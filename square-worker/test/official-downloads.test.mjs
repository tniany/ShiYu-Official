import assert from "node:assert/strict";
import test from "node:test";

import {
  onRequestGet as getDownloads,
  recordSiteDownload,
} from "../../build/shiyu-official-0961/functions/api/downloads.js";
import officialSite from "../../build/shiyu-official-0961/_worker.js";

function createDatabase(initial = {}) {
  const counters = new Map(Object.entries(initial));
  const statements = [];

  const prepare = (sql) => {
    let values = [];
    return {
      bind(...args) {
        values = args;
        return this;
      },
      async first() {
        if (!sql.includes("SELECT value FROM counters")) throw new Error(`Unexpected first SQL: ${sql}`);
        const value = counters.get(values[0]);
        return value === undefined ? null : { value };
      },
      async run() {
        statements.push({ sql, values });
        if (sql.includes("value=value+excluded.value")) {
          counters.set(values[0], Number(counters.get(values[0]) || 0) + Number(values[1] || 0));
        } else if (sql.includes("value=excluded.value")) {
          counters.set(values[0], Number(values[1] || 0));
        } else {
          throw new Error(`Unexpected run SQL: ${sql}`);
        }
        return { success: true };
      },
    };
  };

  return {
    counters,
    statements,
    prepare,
    async batch(batchStatements) {
      return Promise.all(batchStatements.map((statement) => statement.run()));
    },
  };
}

test("official download stats combine website redirects and GitHub assets", async () => {
  const db = createDatabase({
    android_downloads: 4,
    windows_downloads: 3,
    github_android_downloads: 20,
    github_windows_downloads: 12,
  });
  const originalFetch = globalThis.fetch;
  const originalCaches = globalThis.caches;
  globalThis.caches = undefined;
  globalThis.fetch = async () => new Response(JSON.stringify([
    {
      assets: [
        { name: "shiyu-0.9.6.1.apk", download_count: 25 },
        { name: "Shiyu-0.2.7-win-x64-Setup.exe", download_count: 16 },
        { name: "Shiyu-0.2.7-win-x64-Setup.exe.blockmap", download_count: 99 },
        { name: "SHA256SUMS.txt", download_count: 200 },
      ],
    },
  ]), { status: 200, headers: { "Content-Type": "application/json" } });

  try {
    const response = await getDownloads({ env: { DB: db } });
    const data = await response.json();
    assert.equal(response.status, 200);
    assert.equal(data.website_android_downloads, 4);
    assert.equal(data.website_windows_downloads, 3);
    assert.equal(data.github_android_downloads, 25);
    assert.equal(data.github_windows_downloads, 16);
    assert.equal(data.website_downloads, 7);
    assert.equal(data.github_asset_downloads, 41);
    assert.equal(data.total_downloads, 48);
    assert.equal(data.version, "0.9.6.1");
    assert.equal(data.release_url, "https://github.com/tniany/ShiYu-Official/releases/tag/v0.9.6.1");
    assert.equal(db.counters.get("github_android_downloads"), 25);
    assert.equal(db.counters.get("github_windows_downloads"), 16);
  } finally {
    globalThis.fetch = originalFetch;
    globalThis.caches = originalCaches;
  }
});

test("official site records each platform before redirecting to GitHub", async () => {
  const db = createDatabase({ android_downloads: 7, windows_downloads: 2 });
  assert.equal(await recordSiteDownload({ DB: db }, "windows"), true);
  assert.equal(db.counters.get("windows_downloads"), 3);

  const response = await officialSite.fetch(
    new Request("https://shiy.tniay.top/downloads/windows/Shiyu-0.2.7-win-x64-Setup.exe", { method: "GET" }),
    { DB: db },
  );
  assert.equal(response.status, 302);
  assert.equal(response.headers.get("Location"), "https://github.com/tniany/ShiYu-Official/releases/tag/v0.9.6.1");
  assert.equal(db.counters.get("windows_downloads"), 4);

  const head = await officialSite.fetch(
    new Request("https://shiy.tniay.top/download/android", { method: "HEAD" }),
    { DB: db },
  );
  assert.equal(head.status, 302);
  assert.equal(db.counters.get("android_downloads"), 7);

  const android = await officialSite.fetch(
    new Request("https://shiy.tniay.top/downloads/shiyu-0.9.6.1.apk?v=63", { method: "GET" }),
    { DB: db },
  );
  assert.equal(android.status, 302);
  assert.equal(android.headers.get("Location"), "https://github.com/tniany/ShiYu-Official/releases/tag/v0.9.6.1");
  assert.equal(db.counters.get("android_downloads"), 8);
});
