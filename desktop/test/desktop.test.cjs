const test = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const {
  APP_ORIGIN, localAsset, isAppPage, isExternalUrl, isLoginUrl,
  isAuthNavigation, isTrustedCors, safeBounds
} = require("../src/security.cjs");
const { createHandler } = require("../src/protocol.cjs");

test("bundled assets are exact matches, not arbitrary local paths or official-site files", () => {
  assert.equal(localAsset(`${APP_ORIGIN}/?v=1`)[0], "index.html");
  assert.equal(localAsset(`${APP_ORIGIN}/app.js?v=1`)[0], "app.js");
  for (const url of [
    `${APP_ORIGIN}/../package.json`, `${APP_ORIGIN}/assets/%2e%2e%2fpackage.json`,
    `${APP_ORIGIN}/assets/..%5c..%5cmain.cjs`, "file:///C:/Windows/win.ini",
    "https://shiy.tniay.top/", `${APP_ORIGIN}.evil.test/app.js`
  ]) assert.equal(localAsset(url), null, url);
});

test("navigation and OS links only accept the intended URLs", () => {
  assert.equal(isAppPage(`${APP_ORIGIN}/#chat`), true);
  assert.equal(isAppPage(`${APP_ORIGIN}/auth/me`), false);
  assert.equal(isLoginUrl(`${APP_ORIGIN}/auth/login?return_to=/`), true);
  assert.equal(isAuthNavigation("https://liminoldc.suify.cn/oauth/authorize"), true);
  assert.equal(isAuthNavigation("https://x.qq.com/login"), true);
  for (const url of ["javascript:alert(1)", "file:///C:/test.exe", "ms-settings:test", "https://user:pass@example.com"]) {
    assert.equal(isExternalUrl(url), false, url);
    assert.equal(isAuthNavigation(url), false, url);
  }
  assert.equal(isAuthNavigation("https://evilqq.com"), false);
  assert.equal(isAuthNavigation("https://qq.com:444"), false);
});

test("CORS relaxation is restricted to the packaged origin", () => {
  const request = origin => new Request("https://models.example/v1/models", { headers: { origin } });
  assert.equal(isTrustedCors(request(APP_ORIGIN)), true);
  assert.equal(isTrustedCors(request("null")), false);
  assert.equal(isTrustedCors(request("https://evil.test")), false);
  assert.equal(isTrustedCors(new Request(`${APP_ORIGIN}/api/sync`, { headers: { origin: APP_ORIGIN } })), false);
});

test("local UI loads offline and never falls back to a remote HTML app", async () => {
  let fetched = false;
  const handler = createHandler(path.resolve(__dirname, "../renderer"), async () => { fetched = true; throw Error("offline"); });
  const response = await handler(new Request(`${APP_ORIGIN}/`));
  assert.equal(response.status, 200);
  assert.match(await response.text(), /vendor\/lucide.js/);
  assert.match(response.headers.get("content-security-policy"), /object-src 'none'/);
  assert.equal((await handler(new Request(`${APP_ORIGIN}/sw.js`))).status, 404);
  assert.equal(fetched, false);
  assert.equal((await handler(new Request(`${APP_ORIGIN}/api/sync`))).status, 502);
});

test("preflights work without upstream CORS while auth cookies and redirects are passed through", async () => {
  const calls = [];
  const handler = createHandler("", async request => {
    calls.push({ request });
    return new Response(null, { status: 302, headers: { location: "https://liminoldc.suify.cn/oauth/authorize", "set-cookie": "test=one; HttpOnly; Secure" } });
  });
  const preflight = await handler(new Request("https://model.example/v1/models", {
    method: "OPTIONS", headers: { origin: APP_ORIGIN, "access-control-request-headers": "authorization, content-type" }
  }));
  assert.equal(preflight.status, 204);
  assert.equal(calls.length, 0);
  const response = await handler(new Request(`${APP_ORIGIN}/auth/login`));
  assert.equal(response.status, 302);
  assert.match(response.headers.get("set-cookie"), /HttpOnly/);
  assert.equal(calls[0].request.url, `${APP_ORIGIN}/auth/login`);
});

test("API response bodies and error statuses are preserved", async () => {
  const handler = createHandler("", async () => new Response('{"error":"invalid_key"}', { status: 401 }));
  const response = await handler(new Request("https://model.example/v1/models", { headers: { origin: APP_ORIGIN } }));
  assert.equal(response.status, 401);
  assert.equal(response.headers.get("access-control-allow-origin"), APP_ORIGIN);
  assert.deepEqual(await response.json(), { error: "invalid_key" });
});

test("window positions recover from removed monitors and malformed settings", () => {
  const displays = [{ workArea: { x: 0, y: 0, width: 1366, height: 768 } }];
  assert.deepEqual(safeBounds({ x: 3000, y: 1, width: 1280, height: 840 }, displays), { width: 1280, height: 768 });
  assert.deepEqual(safeBounds({ x: 0, y: 0, width: 2000, height: 1000 }, displays), { x: 0, y: 0, width: 1366, height: 768 });
  assert.deepEqual(safeBounds(null, displays), { width: 1280, height: 768 });
});
