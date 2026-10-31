package com.tniany.shiyu

import org.json.JSONArray
import org.json.JSONObject

internal sealed interface MarketSettingRule {
  data object BooleanValue : MarketSettingRule
  data class IntegerValue(val min: Int, val max: Int) : MarketSettingRule
  data object ColorValue : MarketSettingRule
}

internal enum class MarketSettingType {
  SWITCH,
  RANGE,
  SELECT
}

internal data class MarketSettingOption(
  val value: String,
  val label: String
) {
  fun toJson(): JSONObject = JSONObject()
    .put("value", value)
    .put("label", label)

  companion object {
    fun fromJson(item: JSONObject): MarketSettingOption? {
      val value = item.optString("value").trim().uppercase()
      val label = item.optString("label").trim().take(24)
      if (!COLOR.matches(value) || label.isEmpty()) return null
      return MarketSettingOption(value, label)
    }

    private val COLOR = Regex("^#[0-9A-F]{6}(?:[0-9A-F]{2})?$")
  }
}

internal data class MarketSetting(
  val key: String,
  val type: MarketSettingType,
  val label: String,
  val description: String,
  val target: String,
  val defaultValue: Any,
  val min: Int = 0,
  val max: Int = 0,
  val step: Int = 1,
  val options: List<MarketSettingOption> = emptyList()
) {
  fun toJson(): JSONObject {
    val item = JSONObject()
      .put("key", key)
      .put("type", type.name.lowercase())
      .put("label", label)
      .put("description", description)
      .put("target", target)
      .put("default", defaultValue)
    if (type == MarketSettingType.RANGE) {
      item.put("min", min).put("max", max).put("step", step)
    }
    if (type == MarketSettingType.SELECT) {
      val array = JSONArray()
      options.forEach { array.put(it.toJson()) }
      item.put("options", array)
    }
    return item
  }
}

/**
 * Settings are declarative. A definition can only point at a field the app already
 * implements, and the user value is checked again whenever it is restored.
 */
internal object MarketplaceSettingPolicy {
  private val themeRules: Map<String, MarketSettingRule> = mapOf(
    "compose_theme" to MarketSettingRule.IntegerValue(0, 2),
    "compose_theme_style" to MarketSettingRule.IntegerValue(0, 4),
    "refresh_rate" to MarketSettingRule.IntegerValue(0, 165),
    "chat_font_size" to MarketSettingRule.IntegerValue(12, 20),
    "chat_bubble_spacing" to MarketSettingRule.IntegerValue(2, 16),
    "chat_line_height" to MarketSettingRule.IntegerValue(16, 32),
    "chat_user_bubble_opacity" to MarketSettingRule.IntegerValue(10, 100),
    "chat_ai_bubble_opacity" to MarketSettingRule.IntegerValue(10, 100),
    "chat_bubble_max_width" to MarketSettingRule.IntegerValue(240, 360),
    "chat_bubble_vertical_padding" to MarketSettingRule.IntegerValue(6, 20),
    "chat_input_min_height" to MarketSettingRule.IntegerValue(40, 64),
    "chat_background_overlay" to MarketSettingRule.IntegerValue(0, 70),
    "experimental_glass_opacity" to MarketSettingRule.IntegerValue(20, 100),
    "chat_background_color" to MarketSettingRule.ColorValue,
    "chat_user_bubble_color" to MarketSettingRule.ColorValue,
    "chat_ai_bubble_color" to MarketSettingRule.ColorValue,
    "chat_no_bubbles" to MarketSettingRule.BooleanValue,
    "chat_hide_avatars" to MarketSettingRule.BooleanValue,
    "chat_half_screen" to MarketSettingRule.BooleanValue,
    "experimental_input_glass" to MarketSettingRule.BooleanValue,
    "experimental_top_bar_transparent" to MarketSettingRule.BooleanValue,
    "experimental_glass_ui" to MarketSettingRule.BooleanValue
  )

  private val communityBooleanTargets = setOf(
    "agent_time_enabled",
    "agent_music_enabled",
    "web_search_enabled",
    "agent_travel_enabled"
  )

  private val officialGroupBooleanTargets = setOf(
    "default_group_reply_decision",
    "default_group_autonomous",
    "default_group_topics"
  )

  private val keyPattern = Regex("^[a-z][a-z0-9_]{2,31}$")
  private val colorPattern = Regex("^#[0-9A-F]{6}(?:[0-9A-F]{2})?$")

  fun ruleFor(pluginId: String?, target: String, baseline: JSONObject): MarketSettingRule? {
    if (pluginId == null) return themeRules[target]
    if (target !in baseline.keys().asSequence().toSet()) return null
    if (target == "default_group_probability" && pluginId == "official.group-director") {
      return MarketSettingRule.IntegerValue(0, 100)
    }
    if (target in communityBooleanTargets && baseline.opt(target) is Boolean) {
      return MarketSettingRule.BooleanValue
    }
    if (pluginId == "official.group-director" && target in officialGroupBooleanTargets && baseline.opt(target) is Boolean) {
      return MarketSettingRule.BooleanValue
    }
    return null
  }

  fun parseDefinitions(raw: JSONArray?, pluginId: String?, baseline: JSONObject): List<MarketSetting> {
    if (raw == null || raw.length() == 0) return emptyList()
    val definitions = mutableListOf<MarketSetting>()
    val keys = mutableSetOf<String>()
    val targets = mutableSetOf<String>()
    for (index in 0 until raw.length().coerceAtMost(MAX_SETTINGS)) {
      val item = raw.optJSONObject(index) ?: continue
      val definition = parseDefinition(item, pluginId, baseline) ?: continue
      if (!keys.add(definition.key) || !targets.add(definition.target)) continue
      definitions.add(definition)
    }
    return definitions
  }

  fun resolvedValues(
    settings: List<MarketSetting>,
    baseline: JSONObject,
    storedOverrides: JSONObject?
  ): JSONObject {
    val result = JSONObject(baseline.toString())
    settings.forEach { setting ->
      val raw = storedOverrides?.opt(setting.key) ?: return@forEach
      val value = sanitizeValue(setting, raw) ?: return@forEach
      result.put(setting.target, value)
    }
    return result
  }

  fun sanitizeValue(setting: MarketSetting, value: Any?): Any? = when (setting.type) {
    MarketSettingType.SWITCH -> if (value is Boolean) value else null
    MarketSettingType.RANGE -> {
      val number = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        else -> null
      }
      number?.takeIf {
        it in setting.min..setting.max && (it - setting.min) % setting.step == 0
      }
    }
    MarketSettingType.SELECT -> {
      val color = (value as? String)?.trim()?.uppercase()
      color?.takeIf { candidate -> setting.options.any { it.value == candidate } }
    }
  }

  private fun parseDefinition(item: JSONObject, pluginId: String?, baseline: JSONObject): MarketSetting? {
    val key = item.optString("key").trim()
    val label = item.optString("label").trim().take(40)
    val description = item.optString("description").trim().take(180)
    val target = item.optString("target").trim().take(80)
    if (!keyPattern.matches(key) || label.length < 2 || target.isEmpty() || !item.has("default")) return null
    val rule = ruleFor(pluginId, target, baseline) ?: return null
    val rawType = item.optString("type").trim().lowercase()
    return when (rawType) {
      "switch" -> {
        if (rule !is MarketSettingRule.BooleanValue || item.opt("default") !is Boolean) return null
        val defaultValue = item.optBoolean("default")
        if (baseline.opt(target) != defaultValue) return null
        MarketSetting(key, MarketSettingType.SWITCH, label, description, target, defaultValue)
      }
      "range" -> {
        if (rule !is MarketSettingRule.IntegerValue) return null
        if (!item.has("min") || !item.has("max") || !item.has("step")) return null
        val min = item.optInt("min", Int.MIN_VALUE)
        val max = item.optInt("max", Int.MIN_VALUE)
        val step = item.optInt("step", 0)
        if (min == Int.MIN_VALUE || max == Int.MIN_VALUE || min > max || step !in 1..100) return null
        if (min < rule.min || max > rule.max) return null
        if (item.opt("default") !is Int) return null
        val defaultValue = item.optInt("default")
        if (defaultValue !in min..max || defaultValue != baseline.optInt(target, Int.MIN_VALUE)) return null
        MarketSetting(key, MarketSettingType.RANGE, label, description, target, defaultValue, min, max, step)
      }
      "select" -> {
        if (rule !is MarketSettingRule.ColorValue) return null
        val rawOptions = item.optJSONArray("options") ?: return null
        if (rawOptions.length() !in 2..8) return null
        val options = buildList {
          for (optionIndex in 0 until rawOptions.length()) {
            rawOptions.optJSONObject(optionIndex)?.let(::add)
          }
        }.mapNotNull(MarketSettingOption::fromJson)
        if (options.size != rawOptions.length() || options.map { it.value }.distinct().size != options.size) return null
        val defaultValue = item.optString("default").trim().uppercase()
        if (!colorPattern.matches(defaultValue) || options.none { it.value == defaultValue }) return null
        val baselineColor = baseline.optString(target).trim().uppercase()
        if (baselineColor != defaultValue) return null
        MarketSetting(
          key,
          MarketSettingType.SELECT,
          label,
          description,
          target,
          defaultValue,
          options = options
        )
      }
      else -> null
    }
  }

  private const val MAX_SETTINGS = 12
}
