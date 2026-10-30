const fs = require("node:fs/promises");
const path = require("node:path");
const crypto = require("node:crypto");
const assert = require("node:assert/strict");
const { chromium, expect } = require("@playwright/test");

const expectedManifest = require("../../build/shiyu-official-windows-026/download/windows.json");
const filename = expectedManifest.filename;
const expectedHash = expectedManifest.sha256;
const size = expectedManifest.size;
const version = expectedManifest.version;
const origins = ["https://shiy.tniay.top", "https://web-shiy.tniay.top"];
const get = (url, options = {}) => fetch(url, { signal: AbortSignal.timeout(180000), ...options });

(async () => {
  const root = path.resolve(__dirname, "../..");
  const output = path.join(root, "desktop/test-results/download-page");
  await fs.mkdir(output, { recursive: true });
  const local = await fs.open(path.join(root, "release/windows", filename));
  const report = { version, checks: [], checkedAt: new Date().toISOString() };
  try {
    for (const origin of origins) {
      const page = await get(`${origin}/download/`);
      assert.equal(page.status, 200);
      const html = await page.text();
      assert.match(html, /下载时遇/);
      assert.equal((html.match(/class="platform-card"/g) || []).length, 3);
      const home = await (await get(origin)).text();
      assert.ok(home.includes('href="/download/"'));
      const manifest = await (await get(`${origin}/download/windows.json`)).json();
      assert.equal(manifest.version, version);
      assert.equal(manifest.sha256, expectedHash);
      assert.equal(manifest.size, size);
      const head = await get(`${origin}/downloads/windows/${filename}`, { method: "HEAD" });
      assert.equal(head.status, 200);
      assert.equal(Number(head.headers.get("content-length")), size);
      assert.equal(head.headers.get("accept-ranges"), "bytes");
      assert.ok(head.headers.get("content-disposition").includes(filename));
      const range = await get(`${origin}/downloads/windows/${filename}`, { headers: { Range: "bytes=20971490-20971560" } });
      assert.equal(range.status, 206);
      assert.equal(range.headers.get("content-range"), `bytes 20971490-20971560/${size}`);
      const expected = Buffer.alloc(71);
      await local.read(expected, 0, expected.length, 20971490);
      assert.deepEqual(Buffer.from(await range.arrayBuffer()), expected);
      const suffix = await get(`${origin}/downloads/windows/${filename}`, { headers: { Range: "bytes=-32" } });
      assert.equal(suffix.status, 206);
      const tail = Buffer.alloc(32);
      await local.read(tail, 0, 32, size - 32);
      assert.deepEqual(Buffer.from(await suffix.arrayBuffer()), tail);
      report.checks.push(`${origin}: equal platform page, homepage link, manifest, HEAD, cross-part range and suffix`);
      console.log(`PASS: ${origin} page and resumable download`);
    }
    const redirect = await get(`${origins[0]}/downloads/windows/${filename}`, { redirect: "manual" });
    assert.equal(redirect.status, 302);
    assert.equal(redirect.headers.get("location"), `${origins[1]}/downloads/windows/${filename}`);
    const android = await get(`${origins[0]}/version.json`);
    assert.deepEqual(Buffer.from(await android.arrayBuffer()), await fs.readFile(path.join(root, "build/shiyu-official-094-restored/version.json")));
    const me = await get(`${origins[1]}/auth/me`);
    assert.equal(me.status, 200);
    assert.equal((await me.json()).loggedIn, false);
    for (const endpoint of ["/api/square/cards", "/api/announcements"]) {
      assert.equal((await get(`${origins[1]}${endpoint}`)).status, 200);
    }
    const oldApk = await get(`${origins[1]}/downloads/shiyu-0.9.4.apk`, { method: "HEAD" });
    assert.equal(oldApk.status, 200);
    assert.equal(Number(oldApk.headers.get("content-length")), 86306680);
    report.checks.push("Android manifest unchanged; Android download, Web auth, square and announcements healthy");
    console.log("PASS: existing Android and Web endpoints unchanged");
    const response = await get(`${origins[1]}/downloads/windows/${filename}`, { signal: AbortSignal.timeout(900000) });
    assert.equal(response.status, 200);
    const digest = crypto.createHash("sha256");
    let received = 0;
    let lastProgress = 0;
    for await (const chunk of response.body) {
      digest.update(chunk);
      received += chunk.length;
      if (received - lastProgress >= 25 * 1024 * 1024) {
        lastProgress = received;
        console.log(`Download verified ${Math.floor(received / 1024 / 1024)} MiB / 108.9 MiB`);
      }
    }
    const actual = digest.digest("hex");
    assert.equal(received, size);
    assert.equal(actual, expectedHash);
    report.checks.push(`Full online installer: ${received} bytes; SHA256 ${actual}`);
    console.log("PASS: full online installer SHA-256");
  } finally { await local.close(); }
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    const context = await browser.newContext({ permissions: ["clipboard-read", "clipboard-write"] });
    for (const [index, origin] of origins.entries()) {
      const page = await context.newPage();
      for (const width of [1440, 390]) {
        await page.setViewportSize({ width, height: width === 1440 ? 1000 : 844 });
        await page.goto(`${origin}/download/`, { waitUntil: "domcontentloaded", timeout: 60000 });
        await expect(page.locator("h1")).toHaveText("下载时遇");
        await expect(page.locator(".platform-card")).toHaveCount(3);
        await expect(page.locator("#windows .platform-version")).toHaveText(version);
        assert.equal(await page.locator(".primary").count(), 0);
        assert.ok(await page.locator("svg.lucide").count() > 10);
        assert.equal(await page.evaluate(() => [...document.images].every(img => img.complete && img.naturalWidth > 0)), true);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
        await page.screenshot({ path: path.join(output, `production-${index === 0 ? "official" : "web"}-${width}.png`), fullPage: true });
      }
      await page.locator(".checksum summary").click();
      await page.locator("#copy-checksum").click();
      await expect(page.locator("#copy-status")).toHaveText("校验值已复制");
      await page.close();
      report.checks.push(`${origin}: live equal-platform rendering and checksum copy`);
    }
  } finally { await browser.close(); }
  report.success = true;
  await fs.writeFile(path.join(output, "production-verification.json"), JSON.stringify(report, null, 2));
  console.log("PASS: all production download checks.");
})().catch(error => { console.error(error); process.exitCode = 1; });
