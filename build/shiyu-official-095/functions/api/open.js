const MAX_BODY_BYTES = 4096;
const VERSION_PATTERN = /^[0-9A-Za-z._-]{1,32}$/;
const LOGIN_EVENTS = new Set(["web_login", "login", "user_login", "signin", "sign_in"]);
const WEB_VISIT_EVENTS = new Set(["web_visit", "visit", "page_view", "pageview", "pv", "web_open", "site_visit"]);
const FIELD_ALIASES = {
  event: ["event", "type", "action", "kind"],
  version: ["version", "appVersion", "app_version", "clientVersion", "client_version", "v", "ver"],
  userId: ["userId", "user_id", "uid", "qqId", "qq_id", "openId", "openid"],
  source: ["source", "client", "platform", "app"],
};

const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status,
  headers: {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "access-control-allow-origin": "*",
  },
});

const text = (value, max) => String(value ?? "").trim().slice(0, max);

function firstText(values, max) {
  for (const value of values) {
    const result = text(value, max);
    if (result) return result;
  }
  return "";
}

function normalizeEvent(value) {
  return text(value, 40).toLowerCase().replace(/[\s-]+/g, "_");
}

function parseObject(raw, contentType) {
  if (!raw.trim()) return {};
  const isJson = contentType.includes("json");
  const isForm = contentType.includes("application/x-www-form-urlencoded");
  if (!isForm) {
    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) return parsed;
      if (typeof parsed === "string") return { version: parsed };
    } catch (_) {
      if (isJson) return {};
    }
  }
  try {
    return Object.fromEntries(new URLSearchParams(raw));
  } catch (_) {
    return {};
  }
}

async function readPayload(request) {
  const contentType = (request.headers.get("content-type") || "").toLowerCase();
  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength > MAX_BODY_BYTES) return null;
  const raw = await request.text();
  if (new TextEncoder().encode(raw).byteLength > MAX_BODY_BYTES) return null;
  const body = parseObject(raw, contentType);
  const url = new URL(request.url);
  for (const aliases of Object.values(FIELD_ALIASES)) {
    for (const name of aliases) {
      if (text(body[name], 200)) continue;
      const query = url.searchParams.get(name);
      if (query) body[name] = query;
    }
  }
  return { body, url };
}

function field(body, aliases, max) {
  return firstText(aliases.map(name => body[name]), max);
}

function classifyEvent(event, source, userAgent) {
  const normalized = normalizeEvent(event);
  const normalizedSource = source.toLowerCase();
  const browserRequest = !normalizedSource.includes("android") &&
    /mozilla|chrome|safari|firefox|edg\//i.test(userAgent);

  if (LOGIN_EVENTS.has(normalized) || normalizedSource === "web-auth" || normalizedSource.includes("login")) {
    return "web_login";
  }
  if (WEB_VISIT_EVENTS.has(normalized) || normalizedSource === "web" || normalizedSource.includes("web-") || browserRequest) {
    return "web_visit";
  }
  return "app_open";
}

function hongKongDay(timestamp = Date.now()) {
  return new Date(timestamp + 8 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

function hex(bytes) {
  return Array.from(new Uint8Array(bytes), byte => byte.toString(16).padStart(2, "0")).join("");
}

async function hashUserId(userId, secret) {
  const encoder = new TextEncoder();
  if (secret) {
    const key = await crypto.subtle.importKey(
      "raw",
      encoder.encode(secret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );
    return hex(await crypto.subtle.sign("HMAC", key, encoder.encode(userId)));
  }
  return hex(await crypto.subtle.digest("SHA-256", encoder.encode(userId)));
}

export async function onRequestOptions() {
  return new Response(null, {
    status: 204,
    headers: {
      "cache-control": "no-store",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type, x-shiyu-app, x-client, x-app-id",
      "access-control-max-age": "86400",
    },
  });
}

async function recordUsage(request, env) {
  if (!env.DB) return json({ ok: false, error: "database_not_configured" }, 503);

  const day = hongKongDay();

  const payload = await readPayload(request);
  if (!payload) return json({ ok: false, error: "payload_too_large" }, 413);
  const { body } = payload;
  const source = firstText([
    request.headers.get("x-shiyu-app"),
    request.headers.get("x-client"),
    request.headers.get("x-app-id"),
    field(body, FIELD_ALIASES.source, 80),
  ], 80);
  const userAgent = request.headers.get("user-agent") || "";
  const event = classifyEvent(field(body, FIELD_ALIASES.event, 40), source, userAgent);
  const userId = field(body, FIELD_ALIASES.userId, 200);

  if (event === "web_login") {
    const timestamp = Date.now();
    const statements = [
      env.DB.prepare("INSERT INTO counters(name,value) VALUES('web_visits',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
      env.DB.prepare("INSERT INTO daily_web_visits(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
      env.DB.prepare("INSERT INTO counters(name,value) VALUES('web_logins',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
      env.DB.prepare("INSERT INTO daily_web_logins(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
    ];
    if (userId) {
      const userHash = await hashUserId(userId, env.STATS_KEY);
      statements.push(
        env.DB.prepare("INSERT INTO web_login_users(user_hash,first_seen_at,last_seen_at) VALUES(?,?,?) ON CONFLICT(user_hash) DO UPDATE SET last_seen_at=excluded.last_seen_at").bind(userHash, timestamp, timestamp)
      );
    }
    await env.DB.batch(statements);

    const people = await env.DB.prepare("SELECT COUNT(*) AS count FROM web_login_users").first("count");
    return json({ ok: true, people: Number(people || 0) });
  }

  if (event === "web_visit") {
    await env.DB.batch([
      env.DB.prepare("INSERT INTO counters(name,value) VALUES('web_visits',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
      env.DB.prepare("INSERT INTO daily_web_visits(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
    ]);

    const total = await env.DB.prepare("SELECT value FROM counters WHERE name='web_visits'").first("value");
    return json({ ok: true, total: Number(total || 0) });
  }

  const rawVersion = field(body, FIELD_ALIASES.version, 32);
  const version = VERSION_PATTERN.test(rawVersion) ? rawVersion : "unknown";
  await env.DB.batch([
    env.DB.prepare("INSERT INTO counters(name,value) VALUES('app_opens',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
    env.DB.prepare("INSERT INTO daily_launches(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
    env.DB.prepare("INSERT INTO version_launches(version,count) VALUES(?,1) ON CONFLICT(version) DO UPDATE SET count=count+1").bind(version),
  ]);

  const total = await env.DB.prepare("SELECT value FROM counters WHERE name='app_opens'").first("value");
  return json({ ok: true, total: Number(total || 0) });
}

export async function onRequestPost({ request, env }) {
  return recordUsage(request, env);
}

export async function onRequestGet({ request, env }) {
  const url = new URL(request.url);
  const hasUsage = Object.values(FIELD_ALIASES).some(aliases => aliases.some(name => url.searchParams.has(name)));
  if (hasUsage) return recordUsage(request, env);
  return json({ ok: true, service: "shiyu-open-counter" });
}
