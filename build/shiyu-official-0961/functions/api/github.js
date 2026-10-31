// Public GitHub metadata for the official ShiYu website.
// GET /api/github -> repository link and live star count, cached for 15 minutes.

const REPOSITORY = "tniany/ShiYu-Official";
const REPOSITORY_URL = `https://github.com/${REPOSITORY}`;
const GITHUB_API_URL = `https://api.github.com/repos/${REPOSITORY}`;
const CACHE_URL = "https://shiyu-official-cache.internal/github-repository.json";
const CACHE_TTL_SECONDS = 900;

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Cache-Control": "public, max-age=300",
  "Content-Type": "application/json; charset=utf-8",
};

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: CORS });
}

async function repositoryMetadata(env) {
  const cache = globalThis.caches && caches.default;
  const cacheRequest = new Request(CACHE_URL, { method: "GET" });
  if (cache) {
    const cached = await cache.match(cacheRequest);
    if (cached) {
      return { ...(await cached.json()), cached: true };
    }
  }

  const headers = {
    Accept: "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "User-Agent": "shiyu-official-site",
  };
  if (env && env.GITHUB_TOKEN) {
    headers.Authorization = `Bearer ${env.GITHUB_TOKEN}`;
  }
  const response = await fetch(GITHUB_API_URL, {
    headers,
    cf: { cacheEverything: true, cacheTtl: CACHE_TTL_SECONDS },
  });
  if (!response.ok) throw new Error(`github_repository_${response.status}`);
  const repository = await response.json();
  const payload = {
    repository: REPOSITORY_URL,
    stars: Number(repository.stargazers_count || 0),
    updated_at: new Date().toISOString(),
  };
  if (cache) {
    await cache.put(cacheRequest, new Response(JSON.stringify(payload), {
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": `public, max-age=${CACHE_TTL_SECONDS}`,
      },
    }));
  }
  return { ...payload, cached: false };
}

export async function onRequestGet(context) {
  try {
    const metadata = await repositoryMetadata(context.env);
    return jsonResponse({
      ok: true,
      repository: REPOSITORY_URL,
      stars: metadata.stars,
      updated_at: metadata.updated_at,
      cached: Boolean(metadata.cached),
    });
  } catch (error) {
    console.error("github repository metadata unavailable", error);
    return jsonResponse({
      ok: false,
      repository: REPOSITORY_URL,
      stars: null,
      error: "github_unavailable",
    }, 503);
  }
}

export async function onRequestOptions() {
  return new Response(null, { status: 204, headers: CORS });
}
