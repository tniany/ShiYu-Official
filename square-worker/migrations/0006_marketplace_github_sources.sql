ALTER TABLE market_plugin_submissions ADD COLUMN asset_type TEXT NOT NULL DEFAULT 'plugin';
ALTER TABLE market_plugin_submissions ADD COLUMN source_url TEXT NOT NULL DEFAULT '';
ALTER TABLE market_plugin_submissions ADD COLUMN source_ref TEXT NOT NULL DEFAULT '';
ALTER TABLE market_plugin_submissions ADD COLUMN manifest_sha256 TEXT NOT NULL DEFAULT '';

CREATE TABLE IF NOT EXISTS market_approved_assets (
  asset_type TEXT NOT NULL CHECK(asset_type IN ('plugin','theme')),
  asset_id TEXT NOT NULL,
  submission_id TEXT NOT NULL,
  owner_id TEXT NOT NULL,
  owner_name TEXT NOT NULL DEFAULT '',
  source_url TEXT NOT NULL DEFAULT '',
  source_ref TEXT NOT NULL DEFAULT '',
  manifest_sha256 TEXT NOT NULL DEFAULT '',
  manifest_json TEXT NOT NULL,
  approved_by TEXT NOT NULL,
  approved_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY(asset_type, asset_id)
);

CREATE INDEX IF NOT EXISTS idx_market_approved_asset_created
  ON market_approved_assets(approved_at DESC);
CREATE INDEX IF NOT EXISTS idx_market_approved_asset_owner
  ON market_approved_assets(owner_id, approved_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_market_asset_one_pending
  ON market_plugin_submissions(asset_type, plugin_id)
  WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_market_asset_status
  ON market_plugin_submissions(asset_type, status, created_at DESC);

INSERT OR IGNORE INTO market_approved_assets (
  asset_type,
  asset_id,
  submission_id,
  owner_id,
  owner_name,
  source_url,
  source_ref,
  manifest_sha256,
  manifest_json,
  approved_by,
  approved_at,
  updated_at
)
SELECT
  'plugin',
  plugin_id,
  submission_id,
  owner_id,
  owner_name,
  '',
  '',
  '',
  manifest_json,
  approved_by,
  approved_at,
  updated_at
FROM market_approved_plugins;
