// Download statistics for the official Android and Windows releases.
// D1 binding: DB (shiyu-usage database), using the existing counters table.
//
// GET  /api/downloads -> official-site redirects + GitHub asset downloads
// POST /api/downloads?platform=android|windows -> increment site downloads
//
// GitHub's unauthenticated API is cached for 15 minutes. The latest aggregate
// is also stored in D1 so the endpoint keeps returning a useful value when the
// GitHub API is temporarily unavailable.

const REPOSITORY = "tniany/ShiYu-Official";
const RELEASE_TAG = "v0.9.6";
const RELEASE_URL = `https://github.com/${REPOSITORY}/releases/tag/${RELEASE_TAG}`;
const GITHUB_RELEASES_URL = `https://api.github.com/repos/${REPOSITORY}/releases?per_page=100`;
const CACHE_URL = "https://shiyu-official-cache.internal/github-releases.json";
const CACHE_TTL_SECONDS = 900;
const COUNTERS = {
  android: {
    website: "android_downloads",
    github: "github_android_downloads",
  },
  windows: {
    website: "windows_downloads",
    github: "github_windows_downloads",
  },
};

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Cache-Control": "no-store",
  "Content-Type": "application/json; charset=utf-8",
};

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: CORS });
}

function database(env) {
  return env && (env.DB || env.USAGE_DB);
}

async function readCounter(db, name) {
  const row = await db.prepare("SELECT value FROM counters WHERE name = ?").bind(name).first();
  return Number((row && row.value) || 0);
}

async function recordCounter(db, name, amount = 1) {
  await db.prepare(
    "INSERT INTO counters(name,value) VALUES(?,?) ON CONFLICT(name) DO UPDATE SET value=value+excluded.value"
  ).bind(name, amount).run();
}

export async function recordSiteDownload(env, platform) {
  const selected = COUNTERS[platform];
  const db = database(env);
  if (!selected || !db) return false;
  await recordCounter(db, selected.website);
  return true;
}

export function releaseUrl() {
  return RELEASE_URL;
}

async function fetchGitHubTotals() {
  const cache = globalThis.caches && caches.default;
  const cacheRequest = new Request(CACHE_URL, { method: "GET" });
  if (cache) {
    const cached = await cache.match(cacheRequest);
    if (cached) {
      const data = await cached.json();
      return {
        android: Number(data.android || 0),
        windows: Number(data.windows || 0),
        cached: true,
        updatedAt: data.updated_at || null,
      };
    }
  }

  const response = await fetch(GITHUB_RELEASES_URL, {
    headers: {
      "Accept": "application/vnd.github+json",
      "X-GitHub-Api-Version": "2022-11-28",
      "User-Agent": "shiyu-official-site",
    },
    cf: { cacheEverything: true, cacheTtl: CACHE_TTL_SECONDS },
  });
  if (!response.ok) throw new Error(`github_releases_${response.status}`);
  const releases = await response.json();
  if (!Array.isArray(releases)) throw new Error("github_releases_invalid");

  let android = 0;
  let windows = 0;
  for (const release of releases) {
    const assets = Array.isArray(release.assets) ? release.assets : [];
    for (const asset of assets) {
      const name = String(asset.name || "");
      const count = Number(asset.download_count || 0);
      if (/[.]apk$/i.test(name)) android += count;
      if (/[.]exe$/i.test(name)) windows += count;
    }
  }
  const payload = {
    android,
    windows,
    total: android + windows,
    updated_at: new Date().toISOString(),
  };
  const cachePayload = JSON.stringify(payload);
  if (cache) {
    await cache.put(cacheRequest, new Response(cachePayload, {
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": `public, max-age=${CACHE_TTL_SECONDS}`,
      },
    }));
  }
  return { android, windows, cached: false, updatedAt: payload.updated_at };
}

export async function onRequestGet(context) {
  const db = database(context.env);
  const day = new Date().toISOString().slice(0, 10);
  if (!db) {
    return jsonResponse({
      ok: false,
      version: "0.9.6",
      release_url: RELEASE_URL,
      website_android_downloads: 0,
      website_windows_downloads: 0,
      github_android_downloads: 0,
      github_windows_downloads: 0,
      website_downloads: 0,
      github_asset_downloads: 0,
      total_downloads: 0,
      error: "db_unavailable",
    });
  }

  const websiteAndroid = await readCounter(db, COUNTERS.android.website);
  const websiteWindows = await readCounter(db, COUNTERS.windows.website);
  let github = null;
  let githubAvailable = false;
  try {
    github = await fetchGitHubTotals();
    githubAvailable = true;
    const androidSnapshot = await readCounter(db, COUNTERS.android.github);
    const windowsSnapshot = await readCounter(db, COUNTERS.windows.github);
    const updates = [];
    if (github.android >= androidSnapshot) {
      updates.push(db.prepare(
        "INSERT INTO counters(name,value) VALUES(?,?) ON CONFLICT(name) DO UPDATE SET value=excluded.value"
      ).bind(COUNTERS.android.github, github.android));
    }
    if (github.windows >= windowsSnapshot) {
      updates.push(db.prepare(
        "INSERT INTO counters(name,value) VALUES(?,?) ON CONFLICT(name) DO UPDATE SET value=excluded.value"
      ).bind(COUNTERS.windows.github, github.windows));
    }
    if (updates.length) await db.batch(updates);
  } catch (error) {
    console.error("github download statistics unavailable", error);
    github = {
      android: await readCounter(db, COUNTERS.android.github),
      windows: await readCounter(db, COUNTERS.windows.github),
      cached: true,
      updatedAt: null,
    };
  }

  const websiteDownloads = websiteAndroid + websiteWindows;
  const githubDownloads = Number(github.android || 0) + Number(github.windows || 0);
  return jsonResponse({
    ok: true,
    version: "0.9.6",
    windows_version: "0.2.7",
    release_url: RELEASE_URL,
    website_android_downloads: websiteAndroid,
    website_windows_downloads: websiteWindows,
    github_android_downloads: Number(github.android || 0),
    github_windows_downloads: Number(github.windows || 0),
    website_downloads: websiteDownloads,
    github_asset_downloads: githubDownloads,
    total_downloads: websiteDownloads + githubDownloads,
    github_available: githubAvailable,
    github_cached: Boolean(github.cached),
    github_updated_at: github.updatedAt,
    day,
  });
}

export async function onRequestPost(context) {
  const platform = new URL(context.request.url).searchParams.get("platform") === "windows"
    ? "windows"
    : "android";
  const counted = await recordSiteDownload(context.env, platform);
  return jsonResponse({ ok: counted, counted, platform }, counted ? 200 : 503);
}

export async function onRequestOptions() {
  return new Response(null, { status: 204, headers: CORS });
}
