import assert from "node:assert/strict";
import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "../..");
const openFunctionPath = path.join(root, "build/shiyu-official-095/functions/api/open.js");
const { onRequestGet, onRequestOptions, onRequestPost } = await import(pathToFileURL(openFunctionPath));

function mockDatabase() {
  const statements = [];
  const batches = [];
  return {
    statements,
    batches,
    prepare(sql) {
      const statement = {
        sql,
        args: [],
        bind(...args) {
          this.args = args;
          return this;
        },
        async first() {
          if (sql.includes("COUNT(*)")) return 1;
          return sql.includes("app_opens") ? 42 : 84;
        },
      };
      statements.push(statement);
      return statement;
    },
    async batch(items) {
      batches.push(items);
      return [];
    },
  };
}

async function post(body, headers = {}, contentType = "application/json") {
  const db = mockDatabase();
  const response = await onRequestPost({
    request: new Request("https://shiy.tniay.top/api/open", {
      method: "POST",
      headers: { "content-type": contentType, ...headers },
      body,
    }),
    env: { DB: db, STATS_KEY: "test-secret" },
  });
  return { response, db, json: await response.clone().json() };
}

test("current Android app open payload is counted by version", async () => {
  const result = await post(
    JSON.stringify({ event: "open", version: "0.9.5" }),
    { "x-shiyu-app": "android" }
  );
  assert.equal(result.response.status, 200);
  assert.equal(result.json.ok, true);
  assert.match(result.db.statements[0].sql, /app_opens/);
  assert.equal(result.db.statements[2].args[0], "0.9.5");
});

test("legacy JSON app_open and appVersion aliases remain compatible", async () => {
  const result = await post(
    JSON.stringify({ event: "app-open", appVersion: "0.8.1" }),
    { "x-client": "android-legacy" }
  );
  assert.equal(result.response.status, 200);
  assert.match(result.db.statements[0].sql, /app_opens/);
  assert.equal(result.db.statements[2].args[0], "0.8.1");
});

test("legacy form posts are accepted", async () => {
  const app = await post("type=launch&ver=0.7.2", { "x-app-id": "android-old" }, "application/x-www-form-urlencoded");
  assert.equal(app.response.status, 200);
  assert.match(app.db.statements[0].sql, /app_opens/);
  assert.equal(app.db.statements[2].args[0], "0.7.2");

  const web = await post(
    "action=visit",
    { "user-agent": "Mozilla/5.0 Chrome/126.0" },
    "application/x-www-form-urlencoded"
  );
  assert.equal(web.response.status, 200);
  assert.match(web.db.statements[0].sql, /web_visits/);
});

test("legacy login aliases count even when the user id is missing", async () => {
  const identified = await post(
    JSON.stringify({ event: "login", uid: "legacy-user" }),
    { "x-client": "web-auth" }
  );
  assert.equal(identified.response.status, 200);
  assert.ok(identified.db.statements.some(item => item.sql.includes("web_login_users")));

  const anonymous = await post("event=signin", { "x-client": "web-auth" }, "application/x-www-form-urlencoded");
  assert.equal(anonymous.response.status, 200);
  assert.match(anonymous.db.statements[0].sql, /web_visits/);
  assert.ok(anonymous.db.statements.some(item => item.sql.includes("web_logins")));
  assert.ok(!anonymous.db.statements.some(item => item.sql.includes("INSERT INTO web_login_users")));
});

test("legacy GET query reporting and CORS preflight remain available", async () => {
  const db = mockDatabase();
  const getResponse = await onRequestGet({
    request: new Request("https://shiy.tniay.top/api/open?type=open&v=0.6.1&client=android"),
    env: { DB: db },
  });
  assert.equal(getResponse.status, 200);
  assert.match(db.statements[0].sql, /app_opens/);

  const options = await onRequestOptions();
  assert.equal(options.status, 204);
  assert.match(options.headers.get("access-control-allow-headers"), /x-client/);
  assert.equal(options.headers.get("cache-control"), "no-store");
});

test("oversized legacy requests are rejected without writing statistics", async () => {
  const db = mockDatabase();
  const response = await onRequestPost({
    request: new Request("https://shiy.tniay.top/api/open", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ event: "open", version: "x".repeat(5000) }),
    }),
    env: { DB: db },
  });
  assert.equal(response.status, 413);
  assert.equal(db.statements.length, 0);
});

test("client and deployment reporting contracts cannot silently drift", async () => {
  const canonicalOpen = await readFile(openFunctionPath, "utf8");
  const buildDirs = await readdir(path.join(root, "build"), { withFileTypes: true });
  for (const entry of buildDirs) {
    if (!entry.isDirectory() || !entry.name.startsWith("shiyu-official-")) continue;
    const candidate = path.join(root, "build", entry.name, "functions/api/open.js");
    try {
      assert.equal(await readFile(candidate, "utf8"), canonicalOpen, `${entry.name} has a stale /api/open implementation`);
    } catch (error) {
      if (error.code !== "ENOENT") throw error;
    }
  }
  const [appGradle, mainActivity, webApp, worker, agents] = await Promise.all([
    readFile(path.join(root, "app/build.gradle"), "utf8"),
    readFile(path.join(root, "app/src/main/java/com/tniany/shiyu/MainActivity.java"), "utf8"),
    readFile(path.join(root, "web/app.js"), "utf8"),
    readFile(path.join(root, "square-worker/src/index.ts"), "utf8"),
    readFile(path.join(root, "AGENTS.md"), "utf8"),
  ]);

  assert.match(appGradle, /OPEN_EVENT_URL', '"https:\/\/shiy\.tniay\.top\/api\/open"'/);
  assert.match(mainActivity, /setRequestProperty\("X-ShiYu-App", "android"\)/);
  assert.match(mainActivity, /\\"event\\":\\"open\\"/);
  assert.match(webApp, /WEB_USAGE_ENDPOINT = "https:\/\/shiy\.tniay\.top\/api\/open"/);
  assert.match(webApp, /event: "web_visit"/);
  assert.match(worker, /https:\/\/shiy\.tniay\.top\/api\/open/);
  assert.match(worker, /event: 'web_login'/);
  assert.match(agents, /Usage reporting compatibility contract/);
  assert.match(agents, /node --test square-worker\/test\/usage-open\.test\.mjs/);
});
