import {
  onRequestGet as getOpenUsage,
  onRequestOptions as optionsOpenUsage,
  onRequestPost as postOpenUsage
} from "./functions/api/open.js";
import {
  onRequestGet as getVisits,
  onRequestOptions as optionsVisits,
  onRequestPost as postVisits
} from "./functions/api/visits.js";
import {
  releaseUrl,
  onRequestGet as getDownloads,
  onRequestOptions as optionsDownloads,
  onRequestPost as postDownloads,
  recordSiteDownload
} from "./functions/api/downloads.js";

function methodNotAllowed(context) {
  return new Response(null, {
    status: 405,
    headers: {
      "Allow": "GET, POST, OPTIONS",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "no-store"
    }
  });
}

async function dispatchApi(request, env, handlers) {
  const context = { request, env };
  if (request.method === "OPTIONS") return handlers.options(context);
  if (request.method === "GET") return handlers.get(context);
  if (request.method === "POST") return handlers.post(context);
  return methodNotAllowed(context);
}

export default {
  async fetch(request, env) {
    const pathname = new URL(request.url).pathname;

    // Keep the released-client compatibility route byte-for-byte backed by the
    // canonical Functions implementation. The wrapper only dispatches methods.
    if (pathname === "/api/open" || pathname === "/api/open/") {
      return dispatchApi(request, env, {
        get: getOpenUsage,
        post: postOpenUsage,
        options: optionsOpenUsage
      });
    }

    if (pathname === "/api/visits" || pathname === "/api/visits/") {
      return dispatchApi(request, env, {
        get: getVisits,
        post: postVisits,
        options: optionsVisits
      });
    }

    if (pathname === "/api/downloads" || pathname === "/api/downloads/") {
      return dispatchApi(request, env, {
        get: getDownloads,
        post: postDownloads,
        options: optionsDownloads
      });
    }

    const platformQuery = new URL(request.url).searchParams.get("platform");
    let downloadPlatform = "";
    if (
      pathname === "/download/android" ||
      pathname === "/download/android/" ||
      pathname === "/downloads/shiyu-0.9.6.apk"
    ) {
      downloadPlatform = "android";
    } else if (
      pathname === "/download/windows" ||
      pathname === "/download/windows/" ||
      pathname === "/downloads/windows/Shiyu-0.2.7-win-x64-Setup.exe"
    ) {
      downloadPlatform = "windows";
    } else if (pathname === "/download/latest" || pathname === "/download/latest/") {
      downloadPlatform = platformQuery === "windows" ? "windows" : "android";
    }
    if (downloadPlatform && (request.method === "GET" || request.method === "HEAD")) {
      if (request.method === "GET") {
        try {
          await recordSiteDownload(env, downloadPlatform);
        } catch (error) {
          console.error(`${downloadPlatform} download counter failed`, error);
        }
      }
      return Response.redirect(releaseUrl(), 302);
    }

    if (
      (request.method === "GET" || request.method === "HEAD") &&
      (pathname === "/market/catalog.json" || pathname === "/market/catalog.json/")
    ) {
      try {
        const upstream = await fetch(
          "https://web-shiy.tniay.top/api/market/catalog.json",
          {
            method: request.method,
            headers: { "accept": "application/json" },
            cf: { cacheEverything: true, cacheTtl: 60 }
          }
        );
        if (upstream.ok) return upstream;
      } catch (error) {
        console.error("market catalog proxy failed", error);
      }
      return env.ASSETS.fetch(request);
    }

    return env.ASSETS.fetch(request);
  }
};
