package com.tniany.shiyu

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal data class MarketSource(
  val id: String,
  val name: String,
  val url: String,
  val updatedAt: String,
  val builtIn: Boolean
)

internal data class MarketTheme(
  val id: String,
  val name: String,
  val summary: String,
  val author: String,
  val version: String,
  val tags: List<String>,
  val palette: List<String>,
  val values: JSONObject,
  val settings: List<MarketSetting>,
  val sourceUrl: String,
  val sourceRef: String
) {
  fun toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("summary", summary)
    .put("author", author)
    .put("version", version)
    .put("tags", JSONArray(tags))
    .put("palette", JSONArray(palette))
    .put("sourceUrl", sourceUrl)
    .put("sourceRef", sourceRef)
    .put("values", JSONObject(values.toString()))
    .put("settings", JSONArray(settings.map(MarketSetting::toJson)))
}

internal data class MarketPlugin(
  val id: String,
  val name: String,
  val summary: String,
  val description: String,
  val author: String,
  val version: String,
  val category: String,
  val icon: String,
  val permissions: List<String>,
  val runtimePrefs: JSONObject,
  val settings: List<MarketSetting>,
  val experimental: Boolean,
  val sourceUrl: String,
  val sourceRef: String
) {
  fun toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("summary", summary)
    .put("description", description)
    .put("author", author)
    .put("version", version)
    .put("category", category)
    .put("icon", icon)
    .put("permissions", JSONArray(permissions))
    .put("runtimePrefs", JSONObject(runtimePrefs.toString()))
    .put("settings", JSONArray(settings.map(MarketSetting::toJson)))
    .put("experimental", experimental)
    .put("sourceUrl", sourceUrl)
    .put("sourceRef", sourceRef)
}

internal data class MarketCatalog(
  val source: MarketSource,
  val version: Int,
  val themes: List<MarketTheme>,
  val plugins: List<MarketPlugin>
)

internal data class MarketImportResult(
  val themes: List<MarketTheme>,
  val plugins: List<MarketPlugin>
)

internal object MarketplaceCatalogCachePolicy {
  private const val REVISION = "catalog-cache-v2"

  fun stamp(builtInRaw: String, versionCode: Int, versionName: String): String {
    require(builtInRaw.isNotBlank()) { "内置市场目录为空" }
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(builtInRaw.toByteArray(StandardCharsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
    return "$REVISION:$versionCode:${versionName.trim()}:$digest"
  }

  fun isCurrent(cachedRaw: String, cachedStamp: String, expectedStamp: String): Boolean =
    cachedRaw.isNotBlank() && cachedStamp.isNotBlank() && cachedStamp == expectedStamp
}

internal object MarketplaceCatalogCodec {
  const val FORMAT = "shiyu_market_catalog"

  fun parse(raw: String, builtIn: Boolean = false): MarketCatalog {
    require(raw.isNotBlank()) { "官方目录为空" }
    val root = JSONObject(raw.removePrefix("\uFEFF"))
    require(root.optString("format") == FORMAT) { "不是时遇市场目录" }
    val source = root.optJSONObject("source") ?: JSONObject()
    val sourceId = source.optString("id").trim()
    require(sourceId.isNotBlank()) { "市场信源缺少 ID" }
    val sourceName = source.optString("name", sourceId).trim().ifBlank { sourceId }

    val themes = mutableListOf<MarketTheme>()
    val themeIds = mutableSetOf<String>()
    root.optJSONArray("themes")?.let { array ->
      for (index in 0 until array.length()) {
        array.optJSONObject(index)?.let { item ->
          parseTheme(item)?.takeIf { themeIds.add(it.id) }?.let(themes::add)
        }
      }
    }

    val plugins = mutableListOf<MarketPlugin>()
    val pluginIds = mutableSetOf<String>()
    root.optJSONArray("plugins")?.let { array ->
      for (index in 0 until array.length()) {
        array.optJSONObject(index)?.let { item ->
          parsePlugin(item)?.takeIf { pluginIds.add(it.id) }?.let(plugins::add)
        }
      }
    }

    return MarketCatalog(
      source = MarketSource(
        id = sourceId,
        name = sourceName,
        url = source.optString("url").trim(),
        updatedAt = source.optString("updatedAt").trim(),
        builtIn = builtIn
      ),
      version = root.optInt("version", 1).coerceAtLeast(1),
      themes = themes,
      plugins = plugins
    )
  }

  fun localImportCandidates(catalog: MarketCatalog): MarketImportResult = MarketImportResult(
    themes = catalog.themes.filterNot { it.id.startsWith("official.") },
    plugins = catalog.plugins.filter { it.id.startsWith("community.") }
  )

  private fun parseTheme(item: JSONObject): MarketTheme? {
    val id = safeIdentifier(item.optString("id"))
    val name = item.optString("name").trim()
    val values = item.optJSONObject("values") ?: return null
    if (id.isEmpty() || name.isEmpty() || values.length() == 0) return null
    return MarketTheme(
      id = id,
      name = name.take(60),
      summary = item.optString("summary").trim().take(300),
      author = item.optString("author", "时遇官方").trim().take(80),
      version = item.optString("version", "1.0.0").trim().take(32),
      tags = strings(item.optJSONArray("tags")).take(8),
      palette = strings(item.optJSONArray("palette"))
        .filter { Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$").matches(it) }
        .take(4),
      values = JSONObject(values.toString()),
      settings = MarketplaceSettingPolicy.parseDefinitions(item.optJSONArray("settings"), null, values),
      sourceUrl = safeGitHubSource(item.optString("sourceUrl")),
      sourceRef = item.optString("sourceRef").trim().take(200)
    )
  }

  private fun parsePlugin(item: JSONObject): MarketPlugin? {
    val id = safeIdentifier(item.optString("id"))
    val name = item.optString("name").trim()
    if (id.isEmpty() || name.isEmpty()) return null
    val runtimeSource = item.optJSONObject("runtimePrefs")
      ?: runtimePrefsForCapabilities(item.optJSONArray("capabilities"))
    val runtimePrefs = MarketplacePluginPolicy.sanitize(id, runtimeSource) ?: return null
    return MarketPlugin(
      id = id,
      name = name.take(60),
      summary = item.optString("summary").trim().take(300),
      description = item.optString("description").trim().take(1000),
      author = item.optString("author", "时遇官方").trim().take(80),
      version = item.optString("version", "1.0.0").trim().take(32),
      category = item.optString("category", "工具").trim().take(30),
      icon = item.optString("icon", "extension").trim().take(40),
      permissions = strings(item.optJSONArray("permissions")).take(16),
      runtimePrefs = runtimePrefs,
      settings = MarketplaceSettingPolicy.parseDefinitions(item.optJSONArray("settings"), id, runtimePrefs),
      experimental = item.optBoolean("experimental", false),
      sourceUrl = safeGitHubSource(item.optString("sourceUrl")),
      sourceRef = item.optString("sourceRef").trim().take(200)
    )
  }

  private fun runtimePrefsForCapabilities(array: JSONArray?): JSONObject? {
    if (array == null || array.length() == 0) return null
    val result = JSONObject()
    for (index in 0 until array.length()) {
      when (array.optString(index).trim()) {
        "time" -> result.put("agent_time_enabled", true)
        "music" -> result.put("agent_music_enabled", true)
        "web_search" -> result.put("web_search_enabled", true)
        "travel_guidance" -> result.put("agent_travel_enabled", true)
      }
    }
    return result.takeIf { it.length() > 0 }
  }

  private fun strings(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return buildList {
      for (index in 0 until array.length()) {
        array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
      }
    }
  }

  private fun safeGitHubSource(value: String): String {
    val clean = value.trim().take(240)
    return clean.takeIf {
      Regex(
        "^https://github\\.com/[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})/[A-Za-z0-9._-]{1,100}$",
        RegexOption.IGNORE_CASE
      ).matches(it) && !it.endsWith(".git", ignoreCase = true)
    }.orEmpty()
  }

  private fun safeIdentifier(value: String): String =
    value.trim().takeIf { Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{2,127}$").matches(it) }.orEmpty()
}

internal class MarketplaceRepository(
  private val context: Context?,
  private val prefs: SharedPreferences
) {
  companion object {
    const val PAGE_SIZE_LIMIT = 2_000_000
    const val OFFICIAL_ASSET = "market/official-catalog.json"

    private const val KEY_CATALOG_CACHE = "market_official_catalog_cache"
    private const val KEY_CATALOG_CACHE_STAMP = "market_official_catalog_cache_stamp"
    private const val KEY_ACTIVE_THEME = "market_active_theme_id"
    private const val KEY_INSTALLED_THEMES = "market_installed_theme_ids"
    private const val KEY_LOCAL_THEMES = "market_local_themes"
    private const val KEY_LOCAL_PLUGINS = "market_local_plugins"
    private const val KEY_ROLLBACK = "market_theme_rollback"
    private const val KEY_INSTALLED_PLUGINS = "market_installed_plugin_ids"
    private const val KEY_ENABLED_PLUGINS = "market_enabled_plugin_ids"
    private const val KEY_PLUGIN_BACKUPS = "market_plugin_runtime_backups"
    private const val KEY_SETTING_OVERRIDES = "market_asset_setting_overrides"

    internal fun runtimePreferenceKey(pluginId: String): String = "plugin_${pluginId.replace(".", "_").replace("-", "_")}_enabled"
  }

  fun builtInCatalog(): MarketCatalog {
    return builtInCatalogPayload().second
  }

  fun cachedOrBuiltInCatalog(): MarketCatalog {
    val (builtInRaw, builtIn) = builtInCatalogPayload()
    val cached = prefs.getString(KEY_CATALOG_CACHE, "").orEmpty()
    val cachedStamp = prefs.getString(KEY_CATALOG_CACHE_STAMP, "").orEmpty()
    val expectedStamp = catalogCacheStamp(builtInRaw)
    if (!MarketplaceCatalogCachePolicy.isCurrent(cached, cachedStamp, expectedStamp)) {
      if (cached.isNotBlank() || cachedStamp.isNotBlank()) clearCatalogCache()
      return builtIn
    }
    return runCatching {
      MarketplaceCatalogCodec.parse(cached).also {
        require(it.source.id == "official") { "缓存目录不是官方源" }
      }
    }.getOrElse {
      clearCatalogCache()
      builtIn
    }
  }

  fun refreshOfficialCatalog(): MarketCatalog {
    val endpoint = BuildConfig.MARKET_CATALOG_URL.trim()
    require(endpoint.startsWith("https://")) { "官方源必须使用 HTTPS" }
    val builtInRaw = builtInCatalogPayload().first
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
      connection.requestMethod = "GET"
      connection.instanceFollowRedirects = false
      connection.connectTimeout = 10_000
      connection.readTimeout = 15_000
      connection.setRequestProperty("Accept", "application/json")
      connection.setRequestProperty("User-Agent", "ShiYu-Android/${BuildConfig.VERSION_NAME}")
      val status = connection.responseCode
      require(status in 200..299) { "官方源请求失败：HTTP $status" }
      val raw = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
        val output = StringBuilder()
        val buffer = CharArray(8192)
        var total = 0
        while (true) {
          val count = reader.read(buffer)
          if (count < 0) break
          total += count
          require(total <= PAGE_SIZE_LIMIT) { "官方源目录超过 2MB 限制" }
          output.append(buffer, 0, count)
        }
        output.toString()
      }
      val catalog = MarketplaceCatalogCodec.parse(raw)
      require(catalog.source.id == "official") { "官方源标识不匹配" }
      prefs.edit()
        .putString(KEY_CATALOG_CACHE, raw)
        .putString(KEY_CATALOG_CACHE_STAMP, catalogCacheStamp(builtInRaw))
        .apply()
      return catalog
    } finally {
      connection.disconnect()
    }
  }

  private fun builtInCatalogPayload(): Pair<String, MarketCatalog> {
    val raw = requireNotNull(context) { "读取内置市场目录需要 Context" }
      .assets.open(OFFICIAL_ASSET).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    return raw to MarketplaceCatalogCodec.parse(raw, builtIn = true)
  }

  private fun catalogCacheStamp(builtInRaw: String): String =
    MarketplaceCatalogCachePolicy.stamp(builtInRaw, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)

  private fun clearCatalogCache() {
    prefs.edit().remove(KEY_CATALOG_CACHE).remove(KEY_CATALOG_CACHE_STAMP).apply()
  }

  fun localThemes(): List<MarketTheme> {
    val values = runCatching { JSONArray(prefs.getString(KEY_LOCAL_THEMES, "[]").orEmpty()) }.getOrElse { JSONArray() }
    return buildList {
      for (index in 0 until values.length()) {
        values.optJSONObject(index)?.let { item ->
          runCatching { MarketplaceCatalogCodec.parse(wrapTheme(item)) }.getOrNull()?.themes?.firstOrNull()?.let(::add)
        }
      }
    }
  }

  fun localPlugins(): List<MarketPlugin> {
    val values = runCatching { JSONArray(prefs.getString(KEY_LOCAL_PLUGINS, "[]").orEmpty()) }.getOrElse { JSONArray() }
    return buildList {
      for (index in 0 until values.length()) {
        values.optJSONObject(index)?.let { item ->
          runCatching { MarketplaceCatalogCodec.parse(wrapPlugin(item)) }
            .getOrNull()?.plugins?.firstOrNull()?.let(::add)
        }
      }
    }
  }

  fun importLocalTheme(raw: String): MarketImportResult = importLocalPackage(raw, includePlugins = false)

  fun importLocalPackage(raw: String): MarketImportResult = importLocalPackage(raw, includePlugins = true)

  private fun importLocalPackage(raw: String, includePlugins: Boolean): MarketImportResult {
    val root = JSONObject(raw.removePrefix("\uFEFF"))
    val format = root.optString("format")
    val manifestKind = root.optString("kind").trim().lowercase()
    val parsed = when {
      format == MarketplaceCatalogCodec.FORMAT -> MarketplaceCatalogCodec.parse(wrapCatalog(root))
      format == "shiyu_market_theme" -> MarketplaceCatalogCodec.parse(wrapTheme(root))
      format == "shiyu_market_plugin" -> MarketplaceCatalogCodec.parse(wrapPlugin(root))
      root.optInt("schema", 0) == 1 && manifestKind == "theme" ->
        MarketplaceCatalogCodec.parse(wrapTheme(root))
      root.optInt("schema", 0) == 1 && manifestKind == "plugin" ->
        MarketplaceCatalogCodec.parse(wrapPlugin(root))
      format == "shiyu_appearance" -> {
        val values = root.optJSONObject("values") ?: error("外观文件缺少 values")
        MarketplaceCatalogCodec.parse(wrapTheme(JSONObject()
          .put("id", "local-" + hash(raw))
          .put("name", root.optString("name", "本地导入主题"))
          .put("summary", root.optString("summary", "从时遇外观 JSON 导入"))
          .put("author", root.optString("author", "本地"))
          .put("version", root.optString("version", "1"))
          .put("tags", JSONArray().put("本地").put("导入"))
          .put("palette", JSONArray())
          .put("values", values)))
      }
      else -> error("请选择时遇市场目录、主题、插件或外观 JSON")
    }

    val imported = MarketplaceCatalogCodec.localImportCandidates(parsed)
    val importedThemes = imported.themes
    val importedPlugins = if (includePlugins) imported.plugins else emptyList()
    require(importedThemes.isNotEmpty() || importedPlugins.isNotEmpty()) {
      if (includePlugins) "文件中没有可导入的自定义主题或 community.* 插件"
      else "文件中没有可导入的自定义主题"
    }

    val nextThemes = (importedThemes + localThemes()).distinctBy { it.id }.take(30)
    saveLocalThemes(nextThemes)
    importedThemes.forEach { addId(KEY_INSTALLED_THEMES, it.id) }

    val nextPlugins = (importedPlugins + localPlugins()).distinctBy { it.id }.take(50)
    saveLocalPlugins(nextPlugins)
    return MarketImportResult(importedThemes, importedPlugins)
  }

  fun deleteLocalPlugin(plugin: MarketPlugin) {
    if (plugin.id in installedPluginIds()) uninstallPlugin(plugin)
    saveLocalPlugins(localPlugins().filterNot { it.id == plugin.id })
  }

  fun deleteLocalTheme(id: String) {
    saveLocalThemes(localThemes().filterNot { it.id == id })
    removeId(KEY_INSTALLED_THEMES, id)
    if (prefs.getString(KEY_ACTIVE_THEME, "") == id) prefs.edit().remove(KEY_ACTIVE_THEME).apply()
  }

  fun activeThemeId(): String = prefs.getString(KEY_ACTIVE_THEME, "").orEmpty()

  fun resolvedTheme(theme: MarketTheme): MarketTheme = theme.copy(
    values = MarketplaceSettingPolicy.resolvedValues(
      theme.settings,
      theme.values,
      settingOverrides(theme.id)
    )
  )

  fun resolvedPlugin(plugin: MarketPlugin): MarketPlugin = plugin.copy(
    runtimePrefs = MarketplaceSettingPolicy.resolvedValues(
      plugin.settings,
      plugin.runtimePrefs,
      settingOverrides(plugin.id)
    )
  )

  fun settingValues(
    assetId: String,
    settings: List<MarketSetting>
  ): Map<String, Any> {
    val stored = settingOverrides(assetId)
    return settings.associate { setting ->
      val value = if (stored?.has(setting.key) == true) {
        MarketplaceSettingPolicy.sanitizeValue(setting, stored.opt(setting.key))
      } else null
      setting.key to (value ?: setting.defaultValue)
    }
  }

  fun saveSettingValues(
    assetId: String,
    settings: List<MarketSetting>,
    values: Map<String, Any>
  ) {
    val normalized = JSONObject()
    settings.forEach { setting ->
      val value = MarketplaceSettingPolicy.sanitizeValue(setting, values[setting.key]) ?: return@forEach
      if (value != setting.defaultValue) normalized.put(setting.key, value)
    }
    val root = settingOverridesRoot()
    if (normalized.length() == 0) root.remove(assetId) else root.put(assetId, normalized)
    prefs.edit().putString(KEY_SETTING_OVERRIDES, root.toString()).apply()
  }

  fun clearSettingValues(assetId: String) {
    val root = settingOverridesRoot()
    if (root.remove(assetId) != null) {
      prefs.edit().putString(KEY_SETTING_OVERRIDES, root.toString()).apply()
    }
  }


  fun installedThemeIds(): Set<String> = ids(KEY_INSTALLED_THEMES)

  fun installTheme(theme: MarketTheme) {
    addId(KEY_INSTALLED_THEMES, theme.id)
  }

  fun markThemeActive(theme: MarketTheme, previousAppearance: JSONObject) {
    prefs.edit()
      .putString(KEY_ROLLBACK, previousAppearance.toString())
      .putString(KEY_ACTIVE_THEME, theme.id)
      .apply()
    addId(KEY_INSTALLED_THEMES, theme.id)
  }

  fun previousAppearance(): JSONObject? =
    prefs.getString(KEY_ROLLBACK, "").orEmpty().takeIf { it.isNotBlank() }?.let {
      runCatching { JSONObject(it) }.getOrNull()
    }

  fun saveAppearanceRollback(previousAppearance: JSONObject) {
    prefs.edit().putString(KEY_ROLLBACK, previousAppearance.toString()).apply()
  }

  fun clearActiveTheme() {
    prefs.edit().remove(KEY_ACTIVE_THEME).apply()
  }

  private fun settingOverridesRoot(): JSONObject =
    runCatching { JSONObject(prefs.getString(KEY_SETTING_OVERRIDES, "{}").orEmpty()) }.getOrElse { JSONObject() }

  private fun settingOverrides(assetId: String): JSONObject? =
    settingOverridesRoot().optJSONObject(assetId)

  fun installedPluginIds(): Set<String> = ids(KEY_INSTALLED_PLUGINS)
  fun enabledPluginIds(): Set<String> = ids(KEY_ENABLED_PLUGINS)

  fun installPlugin(plugin: MarketPlugin) {
    val backups = pluginBackups()
    if (!backups.has(plugin.id)) {
      val original = JSONObject()
      resolvedPlugin(plugin).runtimePrefs.keys().forEach { key ->
        when (val value = prefs.all[key]) {
          is Boolean -> original.put(key, value)
          is Int -> original.put(key, value)
          is Long -> original.put(key, value)
          is Float -> original.put(key, value.toDouble())
          is String -> original.put(key, value)
          else -> original.put(key, JSONObject.NULL)
        }
      }
      backups.put(plugin.id, original)
      prefs.edit().putString(KEY_PLUGIN_BACKUPS, backups.toString()).apply()
    }
    addId(KEY_INSTALLED_PLUGINS, plugin.id)
    setPluginEnabled(plugin, true)
  }

  fun setPluginEnabled(plugin: MarketPlugin, enabled: Boolean) {
    val editor = prefs.edit()
    val resolved = resolvedPlugin(plugin)
    resolved.runtimePrefs.keys().forEach { key ->
      when (val value = resolved.runtimePrefs.opt(key)) {
        is Boolean -> editor.putBoolean(key, if (enabled) value else false)
        is Int -> editor.putInt(key, if (enabled) value else 0)
        is Long -> editor.putLong(key, if (enabled) value else 0L)
        is Double -> editor.putFloat(key, if (enabled) value.toFloat() else 0f)
        is String -> editor.putString(key, if (enabled) value else "")
      }
    }
    editor.putBoolean(runtimePreferenceKey(plugin.id), enabled).apply()
    if (enabled) addId(KEY_ENABLED_PLUGINS, plugin.id) else removeId(KEY_ENABLED_PLUGINS, plugin.id)
  }

  fun uninstallPlugin(plugin: MarketPlugin) {
    val backups = pluginBackups()
    val original = backups.optJSONObject(plugin.id)
    val editor = prefs.edit()
    if (original != null) {
      original.keys().forEach { key ->
        if (original.isNull(key)) {
          editor.remove(key)
        } else when (val value = original.opt(key)) {
          is Boolean -> editor.putBoolean(key, value)
          is Int -> editor.putInt(key, value)
          is Long -> editor.putLong(key, value)
          is Double -> editor.putFloat(key, value.toFloat())
          is String -> editor.putString(key, value)
        }
      }
      backups.remove(plugin.id)
      editor.putString(KEY_PLUGIN_BACKUPS, backups.toString())
    }
    editor.remove(runtimePreferenceKey(plugin.id)).apply()
    clearSettingValues(plugin.id)
    removeId(KEY_INSTALLED_PLUGINS, plugin.id)
    removeId(KEY_ENABLED_PLUGINS, plugin.id)
  }

  private fun pluginBackups(): JSONObject =
    runCatching { JSONObject(prefs.getString(KEY_PLUGIN_BACKUPS, "{}").orEmpty()) }.getOrElse { JSONObject() }

  private fun saveLocalThemes(values: List<MarketTheme>) {
    val array = JSONArray()
    values.forEach { array.put(it.toJson()) }
    prefs.edit().putString(KEY_LOCAL_THEMES, array.toString()).apply()
  }

  private fun saveLocalPlugins(values: List<MarketPlugin>) {
    val array = JSONArray()
    values.forEach { array.put(it.toJson()) }
    prefs.edit().putString(KEY_LOCAL_PLUGINS, array.toString()).apply()
  }

  private fun ids(key: String): Set<String> {
    val array = runCatching { JSONArray(prefs.getString(key, "[]").orEmpty()) }.getOrElse { JSONArray() }
    return buildSet {
      for (index in 0 until array.length()) array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
    }
  }

  private fun addId(key: String, id: String) {
    val values = ids(key).toMutableSet()
    if (values.add(id)) {
      prefs.edit().putString(key, JSONArray(values.toList()).toString()).apply()
    }
  }

  private fun removeId(key: String, id: String) {
    val values = ids(key).toMutableSet()
    if (values.remove(id)) {
      prefs.edit().putString(key, JSONArray(values.toList()).toString()).apply()
    }
  }

  private fun wrapTheme(theme: JSONObject): String = JSONObject()
    .put("format", MarketplaceCatalogCodec.FORMAT)
    .put("version", 1)
    .put("source", JSONObject().put("id", "local").put("name", "本地导入"))
    .put("themes", JSONArray().put(theme))
    .put("plugins", JSONArray())
    .toString()

  private fun wrapPlugin(plugin: JSONObject): String = JSONObject()
    .put("format", MarketplaceCatalogCodec.FORMAT)
    .put("version", 1)
    .put("source", JSONObject().put("id", "local").put("name", "本地导入"))
    .put("themes", JSONArray())
    .put("plugins", JSONArray().put(plugin))
    .toString()

  private fun wrapCatalog(root: JSONObject): String = JSONObject()
    .put("format", MarketplaceCatalogCodec.FORMAT)
    .put("version", 1)
    .put("source", JSONObject().put("id", "local").put("name", "本地导入"))
    .put("themes", root.optJSONArray("themes") ?: JSONArray())
    .put("plugins", root.optJSONArray("plugins") ?: JSONArray())
    .toString()

  private fun hash(raw: String): String {
    return MessageDigest.getInstance("SHA-256")
      .digest(raw.toByteArray(StandardCharsets.UTF_8))
      .take(6)
      .joinToString("") { "%02x".format(it) }
  }
}
