const fs = require("node:fs/promises");
const path = require("node:path");
const {
  APP_ORIGIN, CONTENT_SECURITY_POLICY, localAsset, isTrustedCors, corsHeaders
} = require("./security.cjs");

function createHandler(rendererRoot, networkFetch) {
  return async request => {
    const asset = localAsset(request.url);
    if (asset) {
      if (!["GET", "HEAD"].includes(request.method)) return new Response(null, { status: 405 });
      try {
        const bytes = await fs.readFile(path.join(rendererRoot, asset[0]));
        return new Response(request.method === "HEAD" ? null : bytes, {
          headers: {
            "content-type": asset[1],
            "cache-control": "no-store",
            "x-content-type-options": "nosniff",
            "content-security-policy": CONTENT_SECURITY_POLICY
          }
        });
      } catch {
        return new Response("Packaged resource missing. Please reinstall Shiyu.", { status: 500 });
      }
    }
    const url = new URL(request.url);
    if (url.origin === APP_ORIGIN && !url.pathname.startsWith("/api/")
      && !url.pathname.startsWith("/auth/") && !url.pathname.startsWith("/downloads/")) {
      return new Response("Not found", { status: 404 });
    }
    const trustedCors = isTrustedCors(request);
    if (trustedCors && request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(request) });
    }
    try {
      // Keep the original session/cookies and redirect chain for OAuth. Only
      // the packaged origin gets CORS access to user-configured model APIs.
      const response = await networkFetch(request);
      if (!trustedCors) return response;
      const headers = new Headers(response.headers);
      for (const [key, value] of Object.entries(corsHeaders(request))) headers.set(key, value);
      return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
    } catch (error) {
      if (request.signal.aborted) throw error;
      console.warn("Network request failed:", url.origin + url.pathname, error.message.split("?")[0]);
      return new Response(JSON.stringify({ message: "Network unavailable. Please check your connection." }), {
        status: 502,
        headers: { "content-type": "application/json", ...(trustedCors ? corsHeaders(request) : {}) }
      });
    }
  };
}

module.exports = { createHandler };
