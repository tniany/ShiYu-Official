const { chromium, expect } = require("@playwright/test");
const path = require("node:path");

(async () => {
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    for (const width of [1280, 390]) {
      const context = await browser.newContext({ viewport: { width, height: 844 } });
      const web = await context.newPage();
      await web.goto("https://web-shiy.tniay.top/", { waitUntil: "domcontentloaded" });
      if (width < 720) await web.locator('.mobile-header [data-action="open-sidebar"]').click();
      const link = web.locator('.sidebar-footer a[href="/download/"]');
      await expect(link).toBeVisible();
      const opened = context.waitForEvent("page");
      await link.click();
      const download = await opened;
      await expect(download.locator("h1")).toHaveText("下载时遇");
      await expect(download.locator(".platform-card")).toHaveCount(3);
      await download.close();
      await web.screenshot({ path: path.resolve(__dirname, `../test-results/download-page/web-entry-${width}.png`) });
      const official = await context.newPage();
      await official.goto("https://shiy.tniay.top/", { waitUntil: "domcontentloaded" });
      await official.locator('.nav-cta[href="/download/"]').click();
      await expect(official.locator("h1")).toHaveText("下载时遇");
      await expect(official.locator(".platform-card")).toHaveCount(3);
      await context.close();
      console.log(`PASS: live homepage download entries at ${width}px`);
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
