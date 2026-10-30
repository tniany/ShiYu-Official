const APP_ORIGIN = "https://web-shiy.tniay.top";
const AUTH_ORIGIN = "https://liminoldc.suify.cn";
const APP_ID = "top.tniay.shiyu.desktop";

const ASSETS = new Map([
  ["/", ["index.html", "text/html; charset=utf-8"]],
  ["/index.html", ["index.html", "text/html; charset=utf-8"]],
  ["/app.js", ["app.js", "text/javascript; charset=utf-8"]],
  ["/styles.css", ["styles.css", "text/css; charset=utf-8"]],
  ["/desktop.css", ["desktop.css", "text/css; charset=utf-8"]],
  ["/manifest.webmanifest", ["manifest.webmanifest", "application/manifest+json"]],
  ["/assets/character.png", ["assets/character.png", "image/png"]],
  ["/assets/icon-192.png", ["assets/icon-192.png", "image/png"]],
  ["/assets/icon-512.png", ["assets/icon-512.png", "image/png"]],
  ["/vendor/lucide.js", ["vendor/lucide.js", "text/javascript; charset=utf-8"]]
]);

const CONTENT_SECURITY_POLICY = [
  "default-src 'self'",
  "script-src 'self' https://esm.sh 'wasm-unsafe-eval'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob: https: http://localhost:* http://127.0.0.1:*",
  "font-src 'self' data:",
  "media-src 'self' data: blob: https: http://localhost:* http://127.0.0.1:*",
  "connect-src 'self' https: http://localhost:* http://127.0.0.1:*",
  "worker-src 'self' blob:",
  "frame-src 'self' about:",
  "object-src 'none'",
  "base-uri 'none'",
  "form-action 'self'",
  "frame-ancestors 'none'"
].join("; ");

function parseUrl(value) {
  try { return new URL(value); } catch { return null; }
}

function isAppPage(value) {
  const url = parseUrl(value);
  return !!url && url.origin === APP_ORIGIN && ["/", "/index.html"].includes(url.pathname);
}

function isLoginUrl(value) {
  const url = parseUrl(value);
  return !!url && url.origin === APP_ORIGIN && url.pathname === "/auth/login";
}

function isAuthNavigation(value) {
  const url = parseUrl(value);
  if (!url || url.protocol !== "https:" || url.username || url.password) return false;
  return (url.origin === APP_ORIGIN && (url.pathname.startsWith("/auth/") || isAppPage(value)))
    || url.origin === AUTH_ORIGIN
    || (url.port === "" && (url.hostname === "qq.com" || url.hostname.endsWith(".qq.com")));
}

function isExternalUrl(value) {
  const url = parseUrl(value);
  return !!url && ["https:", "http:"].includes(url.protocol) && !url.username && !url.password;
}

function localAsset(value) {
  const url = parseUrl(value);
  if (!url || url.origin !== APP_ORIGIN) return null;
  // An exact asset map never maps URL input to an arbitrary filesystem path.
  return ASSETS.get(url.pathname) || null;
}

function isTrustedCors(request) {
  const url = parseUrl(request.url);
  return !!url && url.origin !== APP_ORIGIN && isExternalUrl(request.url)
    && request.headers.get("origin") === APP_ORIGIN;
}

function corsHeaders(request) {
  return {
    "access-control-allow-origin": APP_ORIGIN,
    "access-control-allow-methods": "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS",
    "access-control-allow-headers": request.headers.get("access-control-request-headers") || "content-type, authorization",
    "access-control-expose-headers": "*",
    "vary": "Origin"
  };
}

function safeBounds(saved, displays) {
  const primaryArea = displays[0]?.workArea;
  const fallback = { width: Math.min(1280, primaryArea?.width || 1280), height: Math.min(840, primaryArea?.height || 840) };
  if (!saved || !["x", "y", "width", "height"].every(key => Number.isFinite(saved[key]))) return fallback;
  const display = displays.find(({ workArea: a }) =>
    saved.x < a.x + a.width && saved.x + saved.width > a.x + 100
    && saved.y >= a.y && saved.y < a.y + a.height - 100);
  if (!display) return fallback;
  const a = display.workArea;
  const width = Math.min(Math.max(saved.width, 840), a.width);
  const height = Math.min(Math.max(saved.height, 600), a.height);
  return {
    width, height,
    x: Math.max(a.x, Math.min(saved.x, a.x + a.width - width)),
    y: Math.max(a.y, Math.min(saved.y, a.y + a.height - height))
  };
}

module.exports = {
  APP_ORIGIN, APP_ID, CONTENT_SECURITY_POLICY, isAppPage, isLoginUrl,
  isAuthNavigation, isExternalUrl, localAsset, isTrustedCors, corsHeaders, safeBounds
};
