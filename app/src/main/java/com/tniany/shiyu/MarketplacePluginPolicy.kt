package com.tniany.shiyu

import org.json.JSONObject

/**
 * Marketplace plugins can only use interfaces already implemented and reviewed by the app.
 * They never ship executable code, and this allowlist is the complete set of runtime keys they may change.
 */
internal object MarketplacePluginPolicy {
  private val allowedDefaults: Map<String, Map<String, Any>> = mapOf(
    "official.time-tools" to mapOf("agent_time_enabled" to true),
    "official.netease-music" to mapOf("agent_music_enabled" to true),
    "official.live-web-search" to mapOf("web_search_enabled" to true),
    "official.travel-assistant" to mapOf("agent_travel_enabled" to true),
    "official.group-director" to mapOf(
      "default_group_reply_decision" to true,
      "default_group_autonomous" to true,
      "default_group_topics" to true,
      "default_group_probability" to 70
    )
  )

  private val communityDefaults: Map<String, Any> = mapOf(
    "agent_time_enabled" to true,
    "agent_music_enabled" to true,
    "web_search_enabled" to true,
    "agent_travel_enabled" to true
  )

  fun sanitize(pluginId: String, source: JSONObject?): JSONObject? {
    val allowed = allowedDefaults[pluginId]
      ?: if (pluginId.startsWith("community.")) communityDefaults else return null
    val result = JSONObject()
    allowed.forEach { (key, default) ->
      val value = source?.opt(key)
      when (default) {
        is Boolean -> if (value is Boolean) result.put(key, value)
        is Int -> if (value is Int) result.put(key, value)
        is Long -> if (value is Long) result.put(key, value)
        is Double -> if (value is Double) result.put(key, value)
        is String -> if (value is String) result.put(key, value)
      }
    }
    return result.takeIf { it.length() > 0 }
  }
}
