CREATE TABLE IF NOT EXISTS market_plugin_submissions (
  id TEXT PRIMARY KEY,
  owner_id TEXT NOT NULL,
  owner_name TEXT NOT NULL DEFAULT '',
  plugin_id TEXT NOT NULL,
  version TEXT NOT NULL,
  name TEXT NOT NULL,
  summary TEXT NOT NULL DEFAULT '',
  description TEXT NOT NULL DEFAULT '',
  category TEXT NOT NULL DEFAULT '',
  icon TEXT NOT NULL DEFAULT 'extension',
  capabilities_json TEXT NOT NULL DEFAULT '[]',
  manifest_json TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending'
    CHECK(status IN ('pending','approved','rejected','withdrawn','superseded','hidden')),
  reviewer_note TEXT NOT NULL DEFAULT '',
  reviewed_by TEXT,
  reviewed_at INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_market_plugin_one_pending
  ON market_plugin_submissions(plugin_id)
  WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_market_plugin_owner
  ON market_plugin_submissions(owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_market_plugin_status
  ON market_plugin_submissions(status, created_at DESC);

CREATE TABLE IF NOT EXISTS market_approved_plugins (
  plugin_id TEXT PRIMARY KEY,
  submission_id TEXT NOT NULL,
  owner_id TEXT NOT NULL,
  owner_name TEXT NOT NULL DEFAULT '',
  manifest_json TEXT NOT NULL,
  approved_by TEXT NOT NULL,
  approved_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_market_approved_created
  ON market_approved_plugins(approved_at DESC);

CREATE TABLE IF NOT EXISTS market_plugin_review_events (
  id TEXT PRIMARY KEY,
  submission_id TEXT NOT NULL,
  actor_id TEXT NOT NULL,
  action TEXT NOT NULL,
  note TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_market_plugin_review_events
  ON market_plugin_review_events(submission_id, created_at ASC);
