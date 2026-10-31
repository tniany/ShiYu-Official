import test from "node:test";
import assert from "node:assert/strict";
import {
  MARKET_CAPABILITIES,
  THEME_VALUE_RULES,
  inspectGitHubRepository,
  mergePublicCatalog,
  parseGitHubRepositoryUrl,
  reviewTransition,
  validateMarketplaceManifest,
  validatePluginSubmission
} from "../src/marketplace.mjs";

const pluginManifest = {
  schema: 1,
  kind: "plugin",
  id: "community.city-radio",
  version: "1.0.0",
  name: "城市电台",
  summary: "按所在城市整理适合通勤时收听的公开歌单。",
  description: "调用时遇已实现并审核的音乐搜索与日期时间接口整理通勤歌单，不包含可执行代码或远程脚本。",
  author: "测试开发者",
  category: "娱乐内容",
  icon: "music",
  permissions: ["公开歌单来自第三方音乐服务"],
  capabilities: ["music", "time"],
  settings: [
    {
      key: "music_enabled",
      type: "switch",
      label: "允许点歌",
      description: "关闭后不再调用网易云点歌接口。",
      target: "agent_music_enabled",
      default: true
    }
  ]
};

const themeManifest = {
  schema: 1,
  kind: "theme",
  id: "community.sea-breeze",
  version: "1.0.0",
  name: "海风",
  summary: "低饱和青绿色主题，适合长时间阅读和夜间聊天。",
  description: "使用 App 已审核的外观字段调整聊天界面，不包含脚本、远程资源或可执行代码。",
  author: "测试开发者",
  tags: ["青绿", "玻璃", "夜间"],
  palette: ["#ff12201d", "#68C5A4", "#FF21352F"],
  values: {
    compose_theme: 2,
    experimental_glass_ui: true,
    experimental_glass_opacity: 82,
    chat_background_color: "#ff12201d",
    chat_font_size: 15
  },
  settings: [
    {
      key: "glass_opacity",
      type: "range",
      label: "玻璃强度",
      description: "调整主题玻璃材质的可见程度。",
      target: "experimental_glass_opacity",
      min: 20,
      max: 100,
      step: 1,
      default: 82
    },
    {
      key: "font_size",
      type: "range",
      label: "聊天字号",
      description: "调整聊天正文的字号。",
      target: "chat_font_size",
      min: 12,
      max: 20,
      step: 1,
      default: 15
    },
    {
      key: "glass_ui",
      type: "switch",
      label: "玻璃界面",
      description: "控制主题是否使用玻璃材质。",
      target: "experimental_glass_ui",
      default: true
    },
    {
      key: "user_bubble_color",
      type: "select",
      label: "用户气泡",
      description: "选择用户消息气泡的颜色。",
      target: "chat_background_color",
      default: "#FF12201D",
      options: [
        { value: "#FF12201D", label: "墨青" },
        { value: "#FF68C5A4", label: "海盐" }
      ]
    }
  ]
};

function utf8Base64(value) {
  return Buffer.from(value, "utf8").toString("base64");
}

test("plugin manifests generate fixed app-approved runtime preferences", () => {
  const result = validateMarketplaceManifest(pluginManifest);

  assert.equal(result.ok, true);
  assert.equal(result.assetId, "community.city-radio");
  assert.equal(result.version, "1.0.0");
  assert.deepEqual(result.manifest.runtimePrefs, {
    agent_music_enabled: true,
    agent_time_enabled: true
  });
  assert.equal(result.manifest.experimental, true);
  assert.ok(result.manifest.permissions.includes(MARKET_CAPABILITIES.music.permission));
  assert.ok(result.manifest.permissions.includes("公开歌单来自第三方音乐服务"));
  assert.deepEqual(result.manifest.settings, [{
    key: "music_enabled",
    type: "switch",
    label: "允许点歌",
    description: "关闭后不再调用网易云点歌接口。",
    target: "agent_music_enabled",
    default: true
  }]);
});

test("theme manifests only accept whitelisted values and normalized colors", () => {
  const result = validateMarketplaceManifest(themeManifest);

  assert.equal(result.ok, true);
  assert.equal(result.manifest.kind, "theme");
  assert.deepEqual(result.manifest.palette, ["#FF12201D", "#68C5A4", "#FF21352F"]);
  assert.equal(result.manifest.values.chat_background_color, "#FF12201D");
  assert.equal(THEME_VALUE_RULES.chat_bubble_max_width.max, 360);
  assert.equal(result.manifest.settings.length, 4);
  assert.deepEqual(result.manifest.settings[0], {
    key: "glass_opacity",
    type: "range",
    label: "玻璃强度",
    description: "调整主题玻璃材质的可见程度。",
    target: "experimental_glass_opacity",
    min: 20,
    max: 100,
    step: 1,
    default: 82
  });
  assert.equal(result.manifest.settings[3].default, "#FF12201D");
  assert.deepEqual(result.manifest.settings[3].options, [
    { value: "#FF12201D", label: "墨青" },
    { value: "#FF68C5A4", label: "海盐" }
  ]);
  assert.equal(result.manifest.settings.length, 4);
  assert.deepEqual(result.manifest.settings[0], {
    key: "glass_opacity",
    type: "range",
    label: "玻璃强度",
    description: "调整主题玻璃材质的可见程度。",
    target: "experimental_glass_opacity",
    min: 20,
    max: 100,
    step: 1,
    default: 82
  });
  assert.equal(result.manifest.settings[3].default, "#FF12201D");
  assert.deepEqual(result.manifest.settings[3].options, [
    { value: "#FF12201D", label: "墨青" },
    { value: "#FF68C5A4", label: "海盐" }
  ]);
});

test("schema, kind, ownership format and runtime preference overrides are enforced", () => {
  const missingSchema = validateMarketplaceManifest({ ...pluginManifest, schema: undefined });
  assert.equal(missingSchema.ok, false);
  assert.equal(missingSchema.field, "schema");

  const unknownKind = validateMarketplaceManifest({ ...pluginManifest, kind: "wallpaper" });
  assert.equal(unknownKind.ok, false);
  assert.equal(unknownKind.field, "kind");

  const officialId = validateMarketplaceManifest({ ...pluginManifest, id: "official.city-radio" });
  assert.equal(officialId.ok, false);
  assert.equal(officialId.field, "id");

  const runtimeOverride = validateMarketplaceManifest({
    ...pluginManifest,
    runtimePrefs: { web_search_enabled: true }
  });
  assert.equal(runtimeOverride.ok, false);
  assert.equal(runtimeOverride.field, "runtimePrefs");
});

test("unknown app interfaces and unsupported fields cannot be smuggled through manifests", () => {
  const capability = validateMarketplaceManifest({
    ...pluginManifest,
    capabilities: ["music", "shell.execute"]
  });
  assert.equal(capability.ok, false);
  assert.match(capability.message, /shell\.execute/);

  const field = validateMarketplaceManifest({ ...themeManifest, css: "body{display:none}" });
  assert.equal(field.ok, false);
  assert.equal(field.field, "css");
});

test("theme palette, numeric bounds and boolean types are validated", () => {
  const invalidPalette = validateMarketplaceManifest({
    ...themeManifest,
    palette: ["url(https://example.com/a.png)"]
  });
  assert.equal(invalidPalette.ok, false);
  assert.equal(invalidPalette.field, "palette");

  const invalidRange = validateMarketplaceManifest({
    ...themeManifest,
    values: { ...themeManifest.values, chat_font_size: 44 }
  });
  assert.equal(invalidRange.ok, false);
  assert.equal(invalidRange.field, "values");

  const invalidBoolean = validateMarketplaceManifest({
    ...themeManifest,
    values: { ...themeManifest.values, experimental_input_glass: "yes" }
  });
  assert.equal(invalidBoolean.ok, false);
  assert.equal(invalidBoolean.field, "values");
});

test("declarative settings are bound to reviewed fields and defaults", () => {
  const unknownTarget = validateMarketplaceManifest({
    ...themeManifest,
    settings: [{ ...themeManifest.settings[0], target: "theme.custom_css" }]
  });
  assert.equal(unknownTarget.ok, false);
  assert.equal(unknownTarget.field, "settings");

  const mismatchedDefault = validateMarketplaceManifest({
    ...themeManifest,
    settings: [{ ...themeManifest.settings[0], default: 60 }]
  });
  assert.equal(mismatchedDefault.ok, false);
  assert.match(mismatchedDefault.message, /默认值/);

  const outOfRange = validateMarketplaceManifest({
    ...themeManifest,
    settings: [{ ...themeManifest.settings[0], min: 1, max: 100 }]
  });
  assert.equal(outOfRange.ok, false);

  const executable = validateMarketplaceManifest({
    ...themeManifest,
    settings: [...themeManifest.settings, {
      key: "run_script",
      type: "script",
      label: "运行脚本",
      description: "不允许执行代码。",
      target: "compose_theme",
      default: 2
    }]
  });
  assert.equal(executable.ok, false);
  assert.match(executable.message, /switch、range、select/);
});

test("the legacy plugin helper now produces the same strict manifest", () => {
  const result = validatePluginSubmission(pluginManifest);
  assert.equal(result.ok, true);
  assert.equal(result.manifest.kind, "plugin");
  assert.equal(result.manifest.id, "community.city-radio");
});

test("GitHub repository URLs are canonical and reject path or non-GitHub sources", () => {
  assert.deepEqual(parseGitHubRepositoryUrl("https://github.com/tniany/shiyu"), {
    ok: true,
    owner: "tniany",
    repository: "shiyu",
    canonicalUrl: "https://github.com/tniany/shiyu"
  });
  assert.equal(parseGitHubRepositoryUrl("https://github.com/tniany/shiyu/tree/main").ok, false);
  assert.equal(parseGitHubRepositoryUrl("https://raw.githubusercontent.com/tniany/shiyu/main/shiyu.json").ok, false);
  assert.equal(parseGitHubRepositoryUrl("https://github.com/tniany/shiyu?tab=readme").ok, false);
  assert.equal(parseGitHubRepositoryUrl("https://github.com/tniany/shiyu.git").ok, false);
});

test("GitHub inspection reads the repository default branch root shiyu.json", async () => {
  const seen = [];
  const manifestText = JSON.stringify(themeManifest);
  const fetchImpl = async (url, init) => {
    seen.push({ url: String(url), headers: init.headers });
    if (seen.length === 1) {
      return Response.json({ full_name: "tniany/sea-theme", default_branch: "release", private: false });
    }
    return Response.json({
      type: "file",
      encoding: "base64",
      size: Buffer.byteLength(manifestText),
      content: utf8Base64(manifestText)
    });
  };

  const result = await inspectGitHubRepository("https://github.com/tniany/sea-theme", { fetchImpl });

  assert.equal(result.ok, true);
  assert.equal(result.assetId, "community.sea-breeze");
  assert.equal(result.repository.defaultBranch, "release");
  assert.match(result.manifestSha256, /^[a-f0-9]{64}$/);
  assert.equal(seen.length, 2);
  assert.match(seen[1].url, /\/contents\/shiyu\.json\?ref=release$/);
  assert.equal(seen[1].headers["user-agent"], "shiyu-marketplace-worker");
});

test("GitHub inspection reports a missing root manifest without trusting client data", async () => {
  const fetchImpl = async url => {
    if (String(url).includes("/contents/")) return Response.json({ message: "Not Found" }, { status: 404 });
    return Response.json({ full_name: "tniany/missing", default_branch: "main", private: false });
  };

  const result = await inspectGitHubRepository("https://github.com/tniany/missing", { fetchImpl });

  assert.equal(result.ok, false);
  assert.equal(result.field, "repository");
  assert.match(result.message, /shiyu\.json/);
});

test("the public catalog merges approved plugins and themes while preserving official entries", () => {
  const base = {
    format: "shiyu_market_catalog",
    version: 1,
    themes: [{ id: "official.time-theme", name: "时间主题" }],
    plugins: [{ id: "official.time-tools", name: "日期与时间" }]
  };
  const approved = [
    { asset_type: "plugin", source_url: "https://github.com/tniany/city-radio", source_ref: "main", manifest_json: JSON.stringify(pluginManifest) },
    { asset_type: "theme", source_url: "https://github.com/tniany/sea-theme", source_ref: "release", manifest_json: JSON.stringify(themeManifest) },
    { asset_type: "theme", manifest_json: JSON.stringify({ ...themeManifest, id: "official.fake" }) },
    { asset_type: "plugin", manifest_json: "{broken" }
  ];

  const merged = mergePublicCatalog(base, approved, "2026-10-31");

  assert.equal(merged.version, 2);
  assert.equal(merged.source.updatedAt, "2026-10-31");
  assert.deepEqual(merged.themes.map(theme => theme.id), [
    "official.time-theme",
    "community.sea-breeze"
  ]);
  assert.deepEqual(merged.plugins.map(plugin => plugin.id), [
    "official.time-tools",
    "community.city-radio"
  ]);
  assert.equal(merged.plugins[1].sourceUrl, "https://github.com/tniany/city-radio");
  assert.equal(merged.plugins[1].sourceRef, "main");
  assert.equal(merged.themes[1].sourceUrl, "https://github.com/tniany/sea-theme");
  assert.equal(merged.themes[1].sourceRef, "release");
});

test("only official review actions can move a submission through its lifecycle", () => {
  assert.equal(reviewTransition("pending", "approve"), "approved");
  assert.equal(reviewTransition("pending", "reject"), "rejected");
  assert.equal(reviewTransition("approved", "unpublish"), "hidden");
  assert.equal(reviewTransition("approved", "approve"), "");
  assert.equal(reviewTransition("rejected", "unpublish"), "");
  assert.equal(reviewTransition("hidden", "approve"), "");
});
