const fs = require("node:fs/promises");
const path = require("node:path");
const http = require("node:http");
const assert = require("node:assert/strict");
const { chromium, expect } = require("@playwright/test");

(async () => {
  const root = path.resolve(__dirname, "../..");
  const output = path.join(root, "desktop/test-results/download-page");
  const expected = JSON.parse(await fs.readFile(path.join(root, "build/shiyu-official-windows-026/download/windows.json"), "utf8"));
  await fs.mkdir(output, { recursive: true });
  const mime = { ".html": "text/html", ".css": "text/css", ".js": "text/javascript", ".png": "image/png", ".txt": "text/plain", ".json": "application/json" };
  const server = http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url, "http://localhost");
      const relative = decodeURIComponent(url.pathname);
      const file = path.resolve(root, `.${relative}${relative.endsWith("/") ? "index.html" : ""}`);
      if (!file.startsWith(root + path.sep)) throw Error("Invalid path");
      const bytes = await fs.readFile(file);
      res.setHeader("Content-Type", `${mime[path.extname(file)] || "application/octet-stream"}; charset=utf-8`);
      res.end(bytes);
    } catch { res.writeHead(404); res.end("Not found"); }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  const failures = [];
  try {
    const context = await browser.newContext({ permissions: ["clipboard-read", "clipboard-write"] });
    for (const folder of ["build/shiyu-official-windows-026", "web"]) {
      const page = await context.newPage();
      page.on("pageerror", error => failures.push(error.message));
      for (const width of [1440, 768, 390, 320]) {
        await page.setViewportSize({ width, height: width > 900 ? 1000 : 844 });
        await page.goto(`${origin}/${folder}/download/`);
        await expect(page.locator("h1")).toHaveText("下载时遇");
        await expect(page.locator(".platform-card")).toHaveCount(3);
        await expect(page.locator(".platform-version")).toHaveCount(3);
        await expect(page.locator("#windows .platform-version")).toHaveText(expected.version);
        await expect(page.locator(".product-preview img")).toBeVisible();
        assert.equal(await page.locator("[data-platform-download]").count(), 3);
        assert.equal(await page.locator(".primary").count(), 0, `${folder} still emphasizes one platform`);
        assert.ok(await page.locator("svg.lucide").count() >= 10);
        assert.equal(await page.evaluate(() => [...document.images].every(img => img.complete && img.naturalWidth > 0)), true);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, `${folder} ${width}px overflows`);
        await page.screenshot({ path: path.join(output, `${folder === "web" ? "web" : "official"}-${width}.png`), fullPage: true });
      }
      await page.locator(".checksum summary").click();
      await page.locator("#copy-checksum").click();
      await expect(page.locator("#copy-status")).toHaveText("校验值已复制");
      assert.equal(await page.evaluate(() => navigator.clipboard.readText()), expected.sha256);
      await page.locator(".faq-list summary").first().click();
      await expect(page.locator(".faq-list details").first()).toHaveAttribute("open", "");
      assert.equal(await page.locator("[data-installer]").getAttribute("href"), `/downloads/windows/${expected.filename}`);
      const manifest = await (await page.request.get(`${origin}/${folder}/download/windows.json`)).json();
      assert.equal(manifest.version, expected.version);
      assert.equal(manifest.size, expected.size);
      const [download] = await Promise.all([
        page.waitForEvent("download"),
        page.locator('a[download]').click()
      ]);
      const checkFile = await fs.readFile(await download.path(), "utf8");
      assert.ok(checkFile.startsWith(manifest.sha256));
      await page.close();
    }
    assert.deepEqual(failures, []);
    console.log("PASS: both download pages at 1440, 768, 390 and 320px; equal platform cards, icons, images, links, checksum copy/download and FAQ.");
  } finally {
    await browser.close();
    await new Promise(resolve => server.close(resolve));
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
