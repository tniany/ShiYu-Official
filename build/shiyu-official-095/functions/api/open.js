const MAX_BODY_BYTES = 4096;
const VERSION_PATTERN = /^[0-9A-Za-z._-]{1,32}$/;

const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status,
  headers: {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "access-control-allow-origin": "*",
  },
});

const text = (value, max) => String(value ?? "").trim().slice(0, max);

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
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST, OPTIONS",
      "access-control-allow-headers": "content-type, x-shiyu-app",
    },
  });
}

export async function onRequestPost({ request, env }) {
  if (!env.DB) return json({ ok: false, error: "database_not_configured" }, 503);

  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength > MAX_BODY_BYTES) return json({ ok: false, error: "payload_too_large" }, 413);

  let body = {};
  try {
    const raw = await request.text();
    if (raw.length > MAX_BODY_BYTES) return json({ ok: false, error: "payload_too_large" }, 413);
    body = raw ? JSON.parse(raw) : {};
  } catch (_) {}

  const event = text(body.event, 32);
  const source = text(request.headers.get("x-shiyu-app"), 32).toLowerCase();
  const isLogin = event === "web_login" || source === "web-auth";
  const isWebVisit = isLogin || event === "web_visit" || source === "web";
  const day = hongKongDay();

  if (isLogin) {
    const userId = text(body.userId, 200);
    if (!userId) return json({ ok: false, error: "missing_user_id" }, 400);

    const timestamp = Date.now();
    const userHash = await hashUserId(userId, env.STATS_KEY);
    await env.DB.batch([
      env.DB.prepare("INSERT INTO counters(name,value) VALUES('web_visits',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
      env.DB.prepare("INSERT INTO daily_web_visits(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
      env.DB.prepare("INSERT INTO counters(name,value) VALUES('web_logins',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
      env.DB.prepare("INSERT INTO daily_web_logins(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
      env.DB.prepare("INSERT INTO web_login_users(user_hash,first_seen_at,last_seen_at) VALUES(?,?,?) ON CONFLICT(user_hash) DO UPDATE SET last_seen_at=excluded.last_seen_at").bind(userHash, timestamp, timestamp),
    ]);

    const people = await env.DB.prepare("SELECT COUNT(*) AS count FROM web_login_users").first("count");
    return json({ ok: true, people: Number(people || 0) });
  }

  if (isWebVisit) {
    await env.DB.batch([
      env.DB.prepare("INSERT INTO counters(name,value) VALUES('web_visits',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
      env.DB.prepare("INSERT INTO daily_web_visits(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
    ]);

    const total = await env.DB.prepare("SELECT value FROM counters WHERE name='web_visits'").first("value");
    return json({ ok: true, total: Number(total || 0) });
  }

  const version = VERSION_PATTERN.test(text(body.version, 32)) ? text(body.version, 32) : "unknown";
  await env.DB.batch([
    env.DB.prepare("INSERT INTO counters(name,value) VALUES('app_opens',1) ON CONFLICT(name) DO UPDATE SET value=value+1"),
    env.DB.prepare("INSERT INTO daily_launches(day,count) VALUES(?,1) ON CONFLICT(day) DO UPDATE SET count=count+1").bind(day),
    env.DB.prepare("INSERT INTO version_launches(version,count) VALUES(?,1) ON CONFLICT(version) DO UPDATE SET count=count+1").bind(version),
  ]);

  const total = await env.DB.prepare("SELECT value FROM counters WHERE name='app_opens'").first("value");
  return json({ ok: true, total: Number(total || 0) });
}

export function onRequestGet() {
  return json({ ok: true, service: "shiyu-open-counter" });
}
