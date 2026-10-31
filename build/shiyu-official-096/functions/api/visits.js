// Visitor statistics for the shiyu-official site.
// D1 binding: DB (shiyu-usage database)
// Schema (already present):
//   counters(name TEXT PRIMARY KEY, value INTEGER)
//   daily_web_visits(day TEXT PRIMARY KEY, count INTEGER)
//
// GET  /api/visits                              -> { total, today, day }
// GET  /api/visits?history=1                    -> { total, today, history, day }
// GET  /api/visits?format=shields               -> shields.io endpoint badge (total)
// GET  /api/visits?format=shields&metric=today  -> shields.io endpoint badge (today)
// POST /api/visits                              -> increment + return { total, today, day, counted }
//
// Dedup is handled client-side (sessionStorage) to avoid writing a row per
// visitor into the database. Bots that repost will inflate numbers slightly;
// that's acceptable for a public counter and matches the existing schema intent.

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Cache-Control": "no-store",
  "Content-Type": "application/json; charset=utf-8"
};

function todayKey(now) {
  const d = now instanceof Date ? now : new Date();
  const yyyy = d.getUTCFullYear();
  const mm = String(d.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(d.getUTCDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function jsonResponse(body, status = 200, extra = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, ...extra }
  });
}

async function getTotal(db) {
  const row = await db.prepare("SELECT value FROM counters WHERE name = 'web_visits'").first();
  return Number((row && row.value) || 0);
}

async function getToday(db, day) {
  const row = await db.prepare("SELECT count FROM daily_web_visits WHERE day = ?").bind(day).first();
  return Number((row && row.count) || 0);
}

async function increment(db, day) {
  await db.batch([
    db.prepare("INSERT INTO counters (name, value) VALUES ('web_visits', 1) ON CONFLICT(name) DO UPDATE SET value = value + 1"),
    db.prepare("INSERT INTO daily_web_visits (day, count) VALUES (?, 1) ON CONFLICT(day) DO UPDATE SET count = count + 1").bind(day)
  ]);
}

export async function onRequestPost(context) {
  const { env } = context;
  const db = env && (env.DB || env.USAGE_DB);
  const day = todayKey(new Date());
  if (!db) {
    return jsonResponse({ total: 0, today: 0, counted: false, day, error: "db_unavailable" });
  }
  await increment(db, day);
  const total = await getTotal(db);
  const today = await getToday(db, day);
  return jsonResponse({ total, today, counted: true, day });
}

export async function onRequestGet(context) {
  const { request, env } = context;
  const db = env && (env.DB || env.USAGE_DB);
  const day = todayKey(new Date());
  const params = new URL(request.url).searchParams;

  let total = 0;
  let today = 0;
  if (db) {
    total = await getTotal(db);
    today = await getToday(db, day);
  }

  // shields.io "endpoint" badge: https://shields.io/badges/endpoint-badge
  if (params.get("format") === "shields") {
    const isToday = params.get("metric") === "today";
    return jsonResponse({
      schemaVersion: 1,
      label: isToday ? "今日访问" : "访问量",
      message: (isToday ? today : total).toLocaleString("en-US"),
      color: "b94f6f",
      labelColor: "1c2826"
    });
  }

  if (params.has("history") && db) {
    const res = await db.prepare("SELECT day, count FROM daily_web_visits ORDER BY day DESC LIMIT 60").all();
    const history = (res.results || []).map((r) => ({ day: r.day, count: Number(r.count || 0) }));
    return jsonResponse({ total, today, history, day });
  }
  return jsonResponse({ total, today, day });
}

export async function onRequestOptions() {
  return new Response(null, { status: 204, headers: CORS });
}
