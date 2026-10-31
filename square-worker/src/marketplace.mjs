const text = (value, max) => String(value ?? '').trim().slice(0, max);

export const MAX_MANIFEST_BYTES = 64 * 1024;
export const MARKET_CAPABILITIES = Object.freeze({
  time: Object.freeze({
    label: '日期与时间',
    permission: '读取本机当前日期、时间和时区',
    runtimePrefs: Object.freeze({ agent_time_enabled: true })
  }),
  music: Object.freeze({
    label: '网易云点歌',
    permission: '向网易云音乐公共接口发送歌曲关键词',
    runtimePrefs: Object.freeze({ agent_music_enabled: true })
  }),
  web_search: Object.freeze({
    label: '联网搜索',
    permission: '将搜索关键词发送至必应或已配置的搜索服务',
    runtimePrefs: Object.freeze({ web_search_enabled: true })
  }),
  travel_guidance: Object.freeze({
    label: '出行引导',
    permission: '调用用户已配置的天气、地图与出行 MCP 工具',
    runtimePrefs: Object.freeze({ agent_travel_enabled: true })
  })
});

const ICONS = new Set(['schedule', 'music', 'search', 'travel', 'extension']);
const COMMUNITY_ID = /^community\.[a-z][a-z0-9-]{2,47}$/;
const VERSION = /^\d+\.\d+\.\d+(?:-[a-z0-9.-]{1,32})?$/;
const COLOR = /^#(?:[0-9a-f]{6}|[0-9a-f]{8})$/i;
const MAX_PERMISSIONS = 8;
const MAX_TAGS = 8;
const MAX_PALETTE = 6;
const MAX_SETTINGS = 12;
const SETTING_KEY = /^[a-z][a-z0-9_]{2,31}$/;
const COMMON_FIELDS = new Set(['schema', 'kind', 'id', 'version', 'name', 'summary', 'description', 'author']);
const PLUGIN_FIELDS = new Set([...COMMON_FIELDS, 'category', 'icon', 'capabilities', 'permissions', 'settings']);
const THEME_FIELDS = new Set([...COMMON_FIELDS, 'tags', 'palette', 'values', 'settings']);

export const THEME_VALUE_RULES = Object.freeze({
  compose_theme: Object.freeze({ type: 'integer', min: 0, max: 2 }),
  compose_theme_style: Object.freeze({ type: 'integer', min: 0, max: 4 }),
  refresh_rate: Object.freeze({ type: 'integer', min: 0, max: 165 }),
  chat_font_size: Object.freeze({ type: 'integer', min: 12, max: 20 }),
  chat_bubble_spacing: Object.freeze({ type: 'integer', min: 2, max: 16 }),
  chat_line_height: Object.freeze({ type: 'integer', min: 16, max: 32 }),
  chat_user_bubble_opacity: Object.freeze({ type: 'integer', min: 10, max: 100 }),
  chat_ai_bubble_opacity: Object.freeze({ type: 'integer', min: 10, max: 100 }),
  chat_bubble_max_width: Object.freeze({ type: 'integer', min: 240, max: 360 }),
  chat_bubble_vertical_padding: Object.freeze({ type: 'integer', min: 6, max: 20 }),
  chat_input_min_height: Object.freeze({ type: 'integer', min: 40, max: 64 }),
  chat_background_overlay: Object.freeze({ type: 'integer', min: 0, max: 70 }),
  experimental_glass_opacity: Object.freeze({ type: 'integer', min: 20, max: 100 }),
  chat_background_color: Object.freeze({ type: 'color' }),
  chat_user_bubble_color: Object.freeze({ type: 'color' }),
  chat_ai_bubble_color: Object.freeze({ type: 'color' }),
  chat_no_bubbles: Object.freeze({ type: 'boolean' }),
  chat_hide_avatars: Object.freeze({ type: 'boolean' }),
  chat_half_screen: Object.freeze({ type: 'boolean' }),
  experimental_input_glass: Object.freeze({ type: 'boolean' }),
  experimental_top_bar_transparent: Object.freeze({ type: 'boolean' }),
  experimental_glass_ui: Object.freeze({ type: 'boolean' })
});

function validationError(field, message) {
  return { ok: false, field, message };
}

function isObject(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function validateCommon(manifest, expectedKind) {
  if (!isObject(manifest)) return validationError('manifest', 'shiyu.json 必须是一个 JSON 对象');
  if (manifest.schema !== 1) return validationError('schema', 'shiyu.json 必须设置 "schema": 1');
  if (manifest.kind !== expectedKind) {
    return validationError('kind', `shiyu.json 的 kind 必须是 "${expectedKind}"`);
  }

  const assetId = text(manifest.id, 128).toLowerCase();
  if (!COMMUNITY_ID.test(assetId)) {
    return validationError('id', '作品 ID 必须使用 community. 开头，例如 community.my-tool');
  }
  const version = text(manifest.version, 32).toLowerCase();
  if (!VERSION.test(version)) {
    return validationError('version', '版本号需使用 1.0.0 或 1.0.0-beta 格式');
  }
  const name = text(manifest.name, 60);
  const summary = text(manifest.summary, 300);
  const description = text(manifest.description, 2000);
  const author = text(manifest.author, 80);
  if (name.length < 2) return validationError('name', '作品名称至少需要 2 个字符');
  if (summary.length < 8) return validationError('summary', '简介至少需要 8 个字符');
  if (description.length < 20) return validationError('description', '完整说明至少需要 20 个字符');
  if (author.length < 2) return validationError('author', '作者名称至少需要 2 个字符');

  return {
    ok: true,
    value: { schema: 1, kind: expectedKind, id: assetId, version, name, summary, description, author }
  };
}

function unknownFields(manifest, allowed) {
  return Object.keys(manifest).filter(field => !allowed.has(field));
}

function settingRuleFromValue(value) {
  if (typeof value === 'boolean') return { type: 'boolean' };
  if (Number.isInteger(value)) return { type: 'integer' };
  if (typeof value === 'string') return { type: 'color' };
  return null;
}

function settingValueFits(value, rule) {
  if (!rule) return false;
  if (rule.type === 'boolean') return typeof value === 'boolean';
  if (rule.type === 'integer') return Number.isInteger(value) && value >= rule.min && value <= rule.max;
  if (rule.type === 'color') return typeof value === 'string' && (value === '' || COLOR.test(value));
  return false;
}

function validateSettings(settings, targetRules, baseline) {
  if (settings === undefined) return { ok: true, value: [] };
  if (!Array.isArray(settings)) return validationError('settings', 'settings 必须是设置项数组');
  if (settings.length > MAX_SETTINGS) {
    return validationError('settings', `最多允许 ${MAX_SETTINGS} 个设置项`);
  }
  const normalized = [];
  const keys = new Set();
  const targets = new Set();
  for (const [index, raw] of settings.entries()) {
    const prefix = `settings[${index}]`;
    if (!isObject(raw)) return validationError('settings', `${prefix} 必须是对象`);
    const allowed = new Set(['key', 'type', 'label', 'description', 'target', 'default']);
    if (raw.type === 'range') ['min', 'max', 'step'].forEach(field => allowed.add(field));
    if (raw.type === 'select') allowed.add('options');
    const unknown = Object.keys(raw).find(field => !allowed.has(field));
    if (unknown) return validationError('settings', `${prefix} 包含未知字段：${unknown}`);
    const key = text(raw.key, 32);
    const label = text(raw.label, 40);
    const description = text(raw.description, 180);
    const target = text(raw.target, 80);
    if (!SETTING_KEY.test(key)) return validationError('settings', `${prefix}.key 格式无效`);
    if (keys.has(key)) return validationError('settings', `设置 key 重复：${key}`);
    if (label.length < 2) return validationError('settings', `${prefix}.label 至少需要 2 个字符`);
    if (targets.has(target)) return validationError('settings', `同一个目标字段只能绑定一个设置项：${target}`);
    const rule = targetRules[target];
    if (!rule) return validationError('settings', `${prefix}.target 不是当前作品可配置的字段`);
    if (!Object.prototype.hasOwnProperty.call(baseline, target)) {
      return validationError('settings', `${prefix}.target 必须同时存在于作品默认值中`);
    }
    if (!Object.prototype.hasOwnProperty.call(raw, 'default')) {
      return validationError('settings', `${prefix}.default 必须与作品当前默认值一致`);
    }
    keys.add(key);
    targets.add(target);
    const base = { key, type: raw.type, label, description, target, default: raw.default };
    if (raw.type === 'switch') {
      if (rule.type !== 'boolean' || typeof raw.default !== 'boolean') {
        return validationError('settings', `${prefix} 的 switch 只能绑定布尔字段`);
      }
      if (raw.default !== baseline[target]) {
        return validationError('settings', `${prefix}.default 必须与作品当前默认值一致`);
      }
      normalized.push(base);
      continue;
    }
    if (raw.type === 'range') {
      if (rule.type !== 'integer') return validationError('settings', `${prefix} 的 range 只能绑定整数字段`);
      const { min, max, step } = raw;
      if (!Number.isInteger(min) || !Number.isInteger(max) || !Number.isInteger(step) || min > max || step < 1 || step > 100) {
        return validationError('settings', `${prefix} 的范围或步长无效`);
      }
      if (!Number.isInteger(raw.default) || raw.default < min || raw.default > max) {
        return validationError('settings', `${prefix}.default 超出设置范围`);
      }
      if (min < rule.min || max > rule.max || !settingValueFits(raw.default, rule)) {
        return validationError('settings', `${prefix} 超出 App 审核允许的范围`);
      }
      if (raw.default !== baseline[target]) {
        return validationError('settings', `${prefix}.default 必须与作品当前默认值一致`);
      }
      normalized.push({ ...base, min, max, step });
      continue;
    }
    if (raw.type === 'select') {
      if (rule.type !== 'color' || !Array.isArray(raw.options) || raw.options.length < 2 || raw.options.length > 8) {
        return validationError('settings', `${prefix} 的 select 需要 2 至 8 个颜色选项`);
      }
      const values = new Set();
      const options = [];
      for (const option of raw.options) {
        if (!isObject(option) || Object.keys(option).some(field => !['value', 'label'].includes(field))) {
          return validationError('settings', `${prefix}.options 格式无效`);
        }
        const value = text(option.value, 9).toUpperCase();
        const optionLabel = text(option.label, 24);
        if (!COLOR.test(value) || optionLabel.length < 1 || values.has(value)) {
          return validationError('settings', `${prefix}.options 包含无效或重复的颜色`);
        }
        values.add(value);
        options.push({ value, label: optionLabel });
      }
      const defaultValue = text(raw.default, 9).toUpperCase();
      if (!values.has(defaultValue)) return validationError('settings', `${prefix}.default 必须包含在 options 中`);
      if (!settingValueFits(defaultValue, rule) || defaultValue !== baseline[target]) {
        return validationError('settings', `${prefix}.default 必须与作品当前默认值一致`);
      }
      normalized.push({ ...base, default: defaultValue, options });
      continue;
    }
    return validationError('settings', `${prefix}.type 只支持 switch、range、select`);
  }
  return { ok: true, value: normalized };
}

function validatePluginManifest(manifest) {
  const common = validateCommon(manifest, 'plugin');
  if (!common.ok) return common;
  const unknown = unknownFields(manifest, PLUGIN_FIELDS);
  if (unknown.length) return validationError(unknown[0], `插件配置包含未知字段：${unknown[0]}`);

  const category = text(manifest.category, 30);
  if (!category) return validationError('category', '请填写插件分类');
  const icon = text(manifest.icon || 'extension', 40).toLowerCase();
  if (!ICONS.has(icon)) return validationError('icon', '插件图标不在 App 已审核的图标范围内');
  if ('runtimePrefs' in manifest) {
    return validationError('runtimePrefs', 'runtimePrefs 由时遇根据 capabilities 自动生成，不能手动填写');
  }

  const requested = Array.isArray(manifest.capabilities) ? manifest.capabilities : [];
  const capabilities = [...new Set(requested.map(value => text(value, 40)).filter(Boolean))];
  if (!capabilities.length) return validationError('capabilities', '至少选择一项 App 已实现并审核的接口');
  if (capabilities.length > 4) return validationError('capabilities', '插件最多声明 4 项 App 已审核接口');
  const unknownCapability = capabilities.find(value => !MARKET_CAPABILITIES[value]);
  if (unknownCapability) return validationError('capabilities', `App 未实现或未审核的接口：${unknownCapability}`);

  const extraPermissions = (Array.isArray(manifest.permissions) ? manifest.permissions : [])
    .map(value => text(value, 120))
    .filter(Boolean);
  if (extraPermissions.length > MAX_PERMISSIONS) {
    return validationError('permissions', `权限说明最多 ${MAX_PERMISSIONS} 条`);
  }

  const runtimePrefs = {};
  const permissions = [];
  for (const capability of capabilities) {
    const definition = MARKET_CAPABILITIES[capability];
    Object.assign(runtimePrefs, definition.runtimePrefs);
    permissions.push(definition.permission);
  }

  const targetRules = Object.fromEntries(
    Object.entries(runtimePrefs).map(([key, value]) => [key, settingRuleFromValue(value)])
  );
  const settings = validateSettings(manifest.settings, targetRules, runtimePrefs);
  if (!settings.ok) return settings;

  return {
    ok: true,
    assetId: common.value.id,
    version: common.value.version,
    manifest: {
      ...common.value,
      category,
      icon,
      permissions: [...new Set([...permissions, ...extraPermissions])],
      runtimePrefs,
      experimental: true,
      capabilities,
      settings: settings.value
    }
  };
}

function validateThemeManifest(manifest) {
  const common = validateCommon(manifest, 'theme');
  if (!common.ok) return common;
  const unknown = unknownFields(manifest, THEME_FIELDS);
  if (unknown.length) return validationError(unknown[0], `主题配置包含未知字段：${unknown[0]}`);

  const tags = [...new Set((Array.isArray(manifest.tags) ? manifest.tags : [])
    .map(value => text(value, 16))
    .filter(Boolean))];
  if (!tags.length) return validationError('tags', '主题至少需要一个标签');
  if (tags.length > MAX_TAGS) return validationError('tags', `主题最多允许 ${MAX_TAGS} 个标签`);

  const palette = (Array.isArray(manifest.palette) ? manifest.palette : [])
    .map(value => text(value, 9))
    .filter(Boolean);
  if (!palette.length) return validationError('palette', '主题至少需要一个调色板颜色');
  if (palette.length > MAX_PALETTE) return validationError('palette', `调色板最多允许 ${MAX_PALETTE} 个颜色`);
  const badPaletteColor = palette.find(color => !COLOR.test(color));
  if (badPaletteColor) return validationError('palette', `颜色必须使用 #RRGGBB 或 #AARRGGBB：${badPaletteColor}`);

  if (!isObject(manifest.values)) return validationError('values', '主题 values 必须是一个对象');
  const entries = Object.entries(manifest.values);
  if (!entries.length) return validationError('values', '主题至少需要一项外观设置');
  const values = {};
  for (const [key, rawValue] of entries) {
    const rule = THEME_VALUE_RULES[key];
    if (!rule) return validationError('values', `主题包含不支持的字段：${key}`);
    if (rule.type === 'integer') {
      if (!Number.isInteger(rawValue) || rawValue < rule.min || rawValue > rule.max) {
        return validationError('values', `${key} 必须是 ${rule.min}-${rule.max} 的整数`);
      }
      values[key] = rawValue;
    } else if (rule.type === 'boolean') {
      if (typeof rawValue !== 'boolean') return validationError('values', `${key} 必须是布尔值`);
      values[key] = rawValue;
    } else {
      if (typeof rawValue !== 'string') return validationError('values', `${key} 必须是颜色字符串`);
      const color = rawValue.trim();
      if (color && !COLOR.test(color)) {
        return validationError('values', `${key} 必须使用 #RRGGBB 或 #AARRGGBB`);
      }
      values[key] = color.toUpperCase();
    }
  }

  const settings = validateSettings(manifest.settings, THEME_VALUE_RULES, values);
  if (!settings.ok) return settings;

  return {
    ok: true,
    assetId: common.value.id,
    version: common.value.version,
    manifest: {
      ...common.value,
      tags,
      palette: palette.map(color => color.toUpperCase()),
      values,
      settings: settings.value
    }
  };
}

export function validateMarketplaceManifest(manifest) {
  if (!isObject(manifest)) return validationError('manifest', 'shiyu.json 必须是一个 JSON 对象');
  if (manifest.kind === 'plugin') return validatePluginManifest(manifest);
  if (manifest.kind === 'theme') return validateThemeManifest(manifest);
  return validationError('kind', 'shiyu.json 的 kind 必须是 "plugin" 或 "theme"');
}

export function validatePluginSubmission(input) {
  if (!isObject(input)) return validationError('body', '提交内容必须是对象');
  return validateMarketplaceManifest({
    schema: 1,
    kind: 'plugin',
    id: input.id,
    version: input.version,
    name: input.name,
    summary: input.summary,
    description: input.description,
    author: input.author,
    category: input.category || '实用工具',
    icon: input.icon || 'extension',
    capabilities: input.capabilities,
    permissions: input.permissions
  });
}

export function parseGitHubRepositoryUrl(value) {
  let url;
  try {
    url = new URL(text(value, 500));
  } catch {
    return validationError('repositoryUrl', '请提供完整的 GitHub 仓库地址');
  }
  if (url.protocol !== 'https:' || url.hostname.toLowerCase() !== 'github.com') {
    return validationError('repositoryUrl', '只接受 https://github.com/owner/repository 形式的公开仓库');
  }
  if (url.username || url.password || url.search || url.hash) {
    return validationError('repositoryUrl', 'GitHub 地址不能包含账号、查询参数或锚点');
  }
  const segments = url.pathname.split('/').filter(Boolean);
  if (segments.length !== 2) {
    return validationError('repositoryUrl', '请填写仓库根地址，不要包含 /tree、/blob 或文件路径');
  }
  const [owner, repository] = segments;
  if (!/^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$/.test(owner) || owner.endsWith('-')) {
    return validationError('repositoryUrl', 'GitHub 用户名格式无效');
  }
  if (!/^[A-Za-z0-9._-]{1,100}$/.test(repository) || repository === '.' || repository === '..' || repository.endsWith('.git')) {
    return validationError('repositoryUrl', 'GitHub 仓库名格式无效');
  }
  return {
    ok: true,
    owner,
    repository,
    canonicalUrl: `https://github.com/${owner}/${repository}`
  };
}

function githubHeaders(token = '') {
  return {
    accept: 'application/vnd.github+json',
    'user-agent': 'shiyu-marketplace-worker',
    'x-github-api-version': '2022-11-28',
    ...(token ? { authorization: `Bearer ${token}` } : {})
  };
}

async function githubJson(url, fetchImpl, token) {
  let response;
  try {
    response = await fetchImpl(url, { headers: githubHeaders(token) });
  } catch {
    throw new Error('GITHUB_UNAVAILABLE');
  }
  if (response.status === 403 || response.status === 429) throw new Error('GITHUB_RATE_LIMIT');
  if (!response.ok) throw new Error(`GITHUB_HTTP_${response.status}`);
  return response.json();
}

function decodeGitHubBase64(encoded) {
  const binary = atob(String(encoded || '').replace(/\s/g, ''));
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  return new TextDecoder('utf-8', { fatal: true }).decode(bytes);
}

async function sha256Hex(value) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0')).join('');
}

export async function readGitHubManifest(repository, fetchImpl = fetch, token = '') {
  const owner = text(repository?.owner, 100);
  const name = text(repository?.name, 100);
  const branch = text(repository?.defaultBranch, 200);
  if (!owner || !name || !branch) return validationError('repository', 'GitHub 仓库缺少默认分支信息');

  let file;
  try {
    const url = `https://api.github.com/repos/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/contents/shiyu.json?ref=${encodeURIComponent(branch)}`;
    file = await githubJson(url, fetchImpl, token);
  } catch (error) {
    if (String(error).includes('GITHUB_RATE_LIMIT')) {
      return validationError('repository', 'GitHub 接口请求过于频繁，请稍后再试');
    }
    if (String(error).includes('GITHUB_HTTP_404')) {
      return validationError('repository', '仓库不存在、不是公开仓库，或默认分支根目录没有 shiyu.json');
    }
    return validationError('repository', '暂时无法读取 GitHub 仓库，请稍后重试');
  }
  if (file?.type !== 'file' || file?.encoding !== 'base64') {
    return validationError('manifest', '仓库根目录的 shiyu.json 不是可读取文件');
  }
  if (Number(file.size || 0) > MAX_MANIFEST_BYTES) {
    return validationError('manifest', 'shiyu.json 不能超过 64KB');
  }

  let manifestText;
  let manifest;
  try {
    manifestText = decodeGitHubBase64(file.content);
    manifest = JSON.parse(manifestText);
  } catch {
    return validationError('manifest', 'shiyu.json 不是 UTF-8 编码的有效 JSON');
  }
  if (new TextEncoder().encode(manifestText).byteLength > MAX_MANIFEST_BYTES) {
    return validationError('manifest', 'shiyu.json 不能超过 64KB');
  }
  const validation = validateMarketplaceManifest(manifest);
  if (!validation.ok) return validation;

  return {
    ok: true,
    manifestText,
    manifestSha256: await sha256Hex(manifestText),
    validation
  };
}

export async function inspectGitHubRepository(repositoryUrl, options = {}) {
  const parsed = parseGitHubRepositoryUrl(repositoryUrl);
  if (!parsed.ok) return parsed;
  const fetchImpl = options.fetchImpl || fetch;
  const token = text(options.token, 1000);
  let repository;
  try {
    repository = await githubJson(
      `https://api.github.com/repos/${encodeURIComponent(parsed.owner)}/${encodeURIComponent(parsed.repository)}`,
      fetchImpl,
      token
    );
  } catch (error) {
    if (String(error).includes('GITHUB_RATE_LIMIT')) {
      return validationError('repository', 'GitHub 接口请求过于频繁，请稍后再试');
    }
    if (String(error).includes('GITHUB_HTTP_404')) {
      return validationError('repository', '仓库不存在或不是公开仓库');
    }
    return validationError('repository', '暂时无法读取 GitHub 仓库，请稍后重试');
  }
  if (repository?.private === true) return validationError('repository', '只接受公开 GitHub 仓库');
  const defaultBranch = text(repository?.default_branch, 200);
  if (!defaultBranch) return validationError('repository', 'GitHub 仓库没有可用的默认分支');

  const result = await readGitHubManifest({
    owner: parsed.owner,
    name: parsed.repository,
    defaultBranch
  }, fetchImpl, token);
  if (!result.ok) return result;

  const fullName = text(repository?.full_name, 200) || `${parsed.owner}/${parsed.repository}`;
  return {
    ok: true,
    repository: {
      fullName,
      canonicalUrl: parsed.canonicalUrl,
      defaultBranch
    },
    manifestText: result.manifestText,
    manifestSha256: result.manifestSha256,
    ...result.validation
  };
}

export function mergePublicCatalog(baseCatalog, approvedRows = [], updatedAt = new Date().toISOString().slice(0, 10)) {
  const catalog = JSON.parse(JSON.stringify(baseCatalog || {}));
  const themes = Array.isArray(catalog.themes) ? catalog.themes : [];
  const plugins = Array.isArray(catalog.plugins) ? catalog.plugins : [];
  const themeIds = new Set(themes.map(theme => String(theme?.id || '')));
  const pluginIds = new Set(plugins.map(plugin => String(plugin?.id || '')));

  for (const row of approvedRows) {
    let manifest;
    try {
      manifest = typeof row?.manifest_json === 'string'
        ? JSON.parse(row.manifest_json)
        : row?.manifest;
    } catch {
      continue;
    }
    if (!isObject(manifest) || !COMMUNITY_ID.test(String(manifest.id || ''))) continue;
    const kind = String(row?.asset_type || manifest.kind || 'plugin');
    const source = row?.source_url ? parseGitHubRepositoryUrl(row.source_url) : null;
    const publicManifest = {
      ...manifest,
      ...(source?.ok ? {
        sourceUrl: source.canonicalUrl,
        sourceRef: text(row?.source_ref, 200)
      } : {})
    };
    if (kind === 'theme') {
      if (themeIds.has(manifest.id)) continue;
      themeIds.add(manifest.id);
      themes.push(publicManifest);
    } else if (kind === 'plugin') {
      if (pluginIds.has(manifest.id)) continue;
      pluginIds.add(manifest.id);
      plugins.push(publicManifest);
    }
  }

  catalog.format = 'shiyu_market_catalog';
  catalog.version = Math.max(2, Number(catalog.version || 1));
  catalog.source = {
    id: 'official',
    name: '时遇官方源',
    url: 'https://shiy.tniay.top/market/catalog.json',
    updatedAt
  };
  catalog.themes = themes;
  catalog.plugins = plugins;
  return catalog;
}

export function reviewTransition(status, action) {
  if (action === 'approve' && status === 'pending') return 'approved';
  if (action === 'reject' && status === 'pending') return 'rejected';
  if (action === 'unpublish' && status === 'approved') return 'hidden';
  return '';
}

export function publicManifestForRow(row) {
  try {
    const manifest = typeof row?.manifest_json === 'string'
      ? JSON.parse(row.manifest_json)
      : row?.manifest;
    return isObject(manifest) ? manifest : null;
  } catch {
    return null;
  }
}
