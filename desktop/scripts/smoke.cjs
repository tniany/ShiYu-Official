const { _electron: electron, expect } = require("@playwright/test");
const fs = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");
const http = require("node:http");
const { gzipSync } = require("node:zlib");
const assert = require("node:assert/strict");
const { APP_ORIGIN } = require("../src/security.cjs");

async function smoke() {
  const root = path.resolve(__dirname, "..");
  const output = path.join(root, "test-results");
  await fs.mkdir(output, { recursive: true });
  const profile = await fs.mkdtemp(path.join(os.tmpdir(), "shiyu-desktop-test-"));
  const requests = [];
  const errors = [];
  const server = http.createServer(async (req, res) => {
    const chunks = [];
    for await (const chunk of req) chunks.push(chunk);
    requests.push({ url: req.url, method: req.method, body: Buffer.concat(chunks).toString(), authorization: req.headers.authorization });
    res.setHeader("Content-Type", "application/json");
    if (req.url === "/redirect") { res.statusCode = 302; res.setHeader("Location", "/v1/models"); res.end(); }
    else if (req.url === "/gzip") {
      const bytes = gzipSync(JSON.stringify({ compressed: true }));
      res.setHeader("Content-Encoding", "gzip");
      res.setHeader("Content-Length", bytes.length);
      res.end(bytes);
    }
    else if (req.url === "/slow") setTimeout(() => res.end("{}"), 300);
    else if (req.url === "/v1/models") res.end(JSON.stringify({ data: [{ id: "shiyu-test-model" }] }));
    else if (req.url === "/v1/chat/completions") res.end(JSON.stringify({ choices: [{ message: { role: "assistant", content: "Desktop smoke reply OK." } }] }));
    else { res.statusCode = 404; res.end("{}"); }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  const endpoint = `http://127.0.0.1:${server.address().port}/v1`;
  const executablePath = process.env.SHIYU_TEST_EXE || require("electron");
  const env = { ...process.env, SHIYU_TEST_MODE: "1", SHIYU_TEST_USER_DATA: profile };
  delete env.ELECTRON_RUN_AS_NODE;
  let instance;
  let stage = "launch";
  const launch = async () => {
    const launched = await electron.launch({ executablePath, args: process.env.SHIYU_TEST_EXE ? [] : [root], env, timeout: 60000 });
    launched.process().stderr.on("data", data => errors.push(data.toString()));
    const page = await launched.firstWindow();
    page.on("pageerror", error => errors.push(error.message));
    page.on("console", message => { if (message.type() === "error") errors.push(`${message.text()} ${message.location().url}`); });
    await page.waitForURL(`${APP_ORIGIN}/`);
    await expect(page.locator(".brand-name")).toHaveText("\u65f6\u9047");
    await expect(page.locator("#sidebar-roles .sidebar-role").first()).toBeVisible({ timeout: 15000 });
    return { launched, page };
  };
  try {
    let started = await launch();
    instance = started.launched;
    let page = started.page;
    stage = "sandbox";
    assert.equal(await page.evaluate(() => typeof require), "undefined");
    assert.equal(await page.evaluate(() => typeof process), "undefined");
    assert.equal(await page.evaluate(() => window.shiyuDesktop?.isDesktop), true);
    assert.equal(await page.evaluate(() => ["getState", "check", "download", "install", "onState"].every(name => typeof window.shiyuDesktop?.updates?.[name] === "function")), true);
    assert.equal(await page.locator('script[src^="http"]').count(), 0);
    assert.ok(await page.locator("svg.lucide").count() > 10);
    assert.equal(await page.evaluate(() => [...document.images].filter(img => img.getAttribute("src")?.startsWith("./assets/")).every(img => img.complete && img.naturalWidth > 0)), true);
    await page.screenshot({ path: path.join(output, "desktop-home.png") });

    stage = "navigation";
    for (const view of ["roles", "moments", "world", "rules", "settings", "chat"]) {
      await page.locator(`.primary-nav [data-view="${view}"]`).click();
      await expect(page.locator(`#view-${view}`)).toHaveClass(/active/);
    }

    stage = "model settings";
    await page.locator('.primary-nav [data-view="settings"]').click();
    await page.locator("#api-endpoint").fill(endpoint);
    await page.locator("#api-key").fill("desktop-smoke-only");
    await page.locator('[data-action="fetch-models"]').click();
    await expect(page.locator("#api-model")).toHaveValue("shiyu-test-model", { timeout: 20000 });
    await page.locator('[data-action="save-settings"]').click();
    await page.screenshot({ path: path.join(output, "desktop-settings.png") });
    assert.equal(requests.find(item => item.url === "/v1/models")?.authorization, "Bearer desktop-smoke-only");
    const networkChecks = await page.evaluate(async base => {
      const redirected = await fetch(base.replace("/v1", "/redirect"));
      const data = await redirected.json();
      const compressed = await (await fetch(base.replace("/v1", "/gzip"))).json();
      const controller = new AbortController();
      const pending = fetch(base.replace("/v1", "/slow"), { signal: controller.signal }).catch(error => error.name);
      setTimeout(() => controller.abort(), 30);
      return { redirected: data.data[0].id, aborted: await pending, compressed: compressed.compressed };
    }, endpoint);
    assert.deepEqual(networkChecks, { redirected: "shiyu-test-model", aborted: "AbortError", compressed: true });

    stage = "desktop updates";
    await page.locator('[data-settings-tab="about"]').click();
    await expect(page.locator("#desktop-update-section")).toContainText("在线更新未启用", { timeout: 10000 });
    await expect(page.locator("#desktop-update-current")).toContainText("当前版本");

    stage = "chat";
    await page.locator('.primary-nav [data-view="chat"]').click();
    await page.locator('#chat-welcome [data-action="new-chat"]').click();
    await expect(page.locator("#message-input")).toBeVisible();
    stage = "chat input method";
    const imeResult = await page.evaluate(() => {
      const input = document.querySelector("#message-input");
      input.focus();
      input.value = "候选词";
      const messagesBefore = document.querySelectorAll("#message-list .message-row").length;
      input.dispatchEvent(new CompositionEvent("compositionstart", { bubbles: true, data: "候" }));
      const modernEvent = new KeyboardEvent("keydown", { key: "Enter", code: "Enter", bubbles: true, cancelable: true, isComposing: true });
      const modernDispatched = input.dispatchEvent(modernEvent);
      const legacyEvent = new KeyboardEvent("keydown", { key: "Enter", code: "Enter", bubbles: true, cancelable: true });
      Object.defineProperty(legacyEvent, "keyCode", { value: 229 });
      const legacyDispatched = input.dispatchEvent(legacyEvent);
      input.dispatchEvent(new CompositionEvent("compositionend", { bubbles: true, data: "候选词" }));
      const messagesAfter = document.querySelectorAll("#message-list .message-row").length;
      const value = input.value;
      input.value = "";
      input.dispatchEvent(new Event("input", { bubbles: true }));
      return {
        modernPrevented: modernEvent.defaultPrevented || !modernDispatched,
        legacyPrevented: legacyEvent.defaultPrevented || !legacyDispatched,
        messagesBefore,
        messagesAfter,
        value
      };
    });
    assert.equal(imeResult.modernPrevented, false);
    assert.equal(imeResult.legacyPrevented, false);
    assert.equal(imeResult.messagesAfter, imeResult.messagesBefore);
    assert.equal(imeResult.value, "候选词");
    await page.locator("#message-input").fill("Windows persistence check.");
    await page.locator("#message-input").press("Enter");
    await expect(page.locator("#message-list")).toContainText("Desktop smoke reply OK.", { timeout: 20000 });
    const chatRequest = requests.find(item => item.url === "/v1/chat/completions");
    assert.ok(JSON.parse(chatRequest.body).messages.some(item => JSON.stringify(item.content).includes("Windows persistence check.")));
    await page.screenshot({ path: path.join(output, "desktop-chat.png") });
    stage = "download";
    const backupPath = path.join(output, "smoke-backup.json");
    await instance.evaluate(({ session }, target) => {
      session.fromPartition("persist:shiyu").once("will-download", (_event, item) => item.setSavePath(target));
    }, backupPath);
    await page.locator('.primary-nav [data-view="settings"]').click();
    await page.locator('[data-settings-tab="data"]').click();
    await page.locator('#settings-data .data-action[data-action="export-data"]').click();
    await expect.poll(async () => { try { return JSON.parse(await fs.readFile(backupPath, "utf8")).kind; } catch { return ""; } }).toBe("shiyu-web-backup");
    if (process.env.SHIYU_TEST_LIVE_AUTH === "1") {
      stage = "live auth redirect";
      const authOpened = instance.waitForEvent("window");
      await page.locator('.cloud-account-main[data-action="cloud-login"]').click({ noWaitAfter: true });
      const authPage = await authOpened;
      await authPage.waitForURL(/^https:\/\/liminoldc\.suify\.cn\//, { timeout: 30000, waitUntil: "domcontentloaded" });
      const hasPkceCookies = await instance.evaluate(async ({ session }) => {
        const cookies = await session.fromPartition("persist:shiyu").cookies.get({ domain: "web-shiy.tniay.top" });
        return ["shiyu_square_oauth_state", "shiyu_square_oauth_verifier"].every(name =>
          cookies.some(cookie => cookie.name === name && cookie.httpOnly && cookie.secure));
      });
      assert.equal(hasPkceCookies, true);
      await authPage.close();
      await expect(page.locator("#sidebar-roles .sidebar-role").first()).toBeVisible();
    }
    stage = "narrow layout";
    await instance.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(900, 700));
    await page.locator('.primary-nav [data-view="chat"]').click();
    await page.screenshot({ path: path.join(output, "desktop-compact.png") });
    const layout = await page.evaluate(() => [".app-shell", ".main-shell", ".announcement-banner", ".view-chat", ".chat-panel"].map(selector => {
      const element = document.querySelector(selector);
      return { selector, top: element.getBoundingClientRect().top, height: element.clientHeight, scrollTop: element.scrollTop, scrollHeight: element.scrollHeight };
    }));
    assert.equal(layout[0].scrollTop, 0);
    assert.ok(layout[1].top >= 0);
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
    assert.equal(await page.locator(".conversation-list").evaluate(element => element.scrollWidth <= element.clientWidth), true);
    await instance.close();
    instance = null;

    stage = "restart";
    started = await launch();
    instance = started.launched;
    page = started.page;
    await expect(page.locator("#message-list")).toContainText("Windows persistence check.");
    await expect(page.locator("#message-list")).toContainText("Desktop smoke reply OK.");
    stage = "offline reload";
    await instance.evaluate(({ session }) => session.fromPartition("persist:shiyu").enableNetworkEmulation({ offline: true }));
    await page.reload();
    await expect(page.locator("#message-list")).toContainText("Windows persistence check.");
    assert.ok(await page.locator("svg.lucide").count() > 10);
    await page.screenshot({ path: path.join(output, "desktop-offline.png") });
    await fs.writeFile(path.join(output, "smoke-result.json"), JSON.stringify({
      success: true, executablePath, profile, requests: requests.length, liveAuthRedirectChecked: process.env.SHIYU_TEST_LIVE_AUTH === "1",
      checks: ["sandbox", "desktop update bridge", "local icons", "navigation", "CORS-free model GET", "HTTP redirect", "gzip response", "request cancellation", "chat POST", "JSON export", "900px layout", "restart persistence", "offline reload"],
      errors
    }, null, 2));
    console.log(`PASS: desktop smoke tests. Screenshots: ${output}`);
  } catch (error) {
    console.error(`Smoke failed at: ${stage}`);
    console.error(error.message);
    console.error(errors.slice(-12).join("\n"));
    if (instance) {
      const page = instance.windows()[0];
      if (page) await page.screenshot({ path: path.join(output, "failure.png") }).catch(() => {});
    }
    throw error;
  } finally {
    if (instance) await instance.close();
    await new Promise(resolve => server.close(resolve));
  }
}
smoke().catch(error => { console.error(error); process.exitCode = 1; });
