package com.tniany.shiyu;

import java.util.Iterator;
import java.util.LinkedHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

final class BackupSelection {
  final boolean roles, chats, memories, diaries, moments, settings, credentials, assets;

  BackupSelection(boolean roles, boolean chats, boolean memories, boolean diaries,
      boolean moments, boolean settings, boolean credentials, boolean assets) {
    this.roles = roles || chats || memories || diaries || moments;
    this.chats = chats;
    this.memories = memories;
    this.diaries = diaries;
    this.moments = moments;
    this.settings = settings;
    this.credentials = credentials;
    this.assets = assets;
  }

  static BackupSelection all() {
    return new BackupSelection(true, true, true, true, true, true, true, true);
  }

  boolean isFull() {
    return roles && chats && memories && diaries && moments && settings && credentials && assets;
  }

  boolean isEmpty() {
    return !roles && !chats && !memories && !diaries && !moments && !settings && !credentials;
  }

  JSONObject json() throws Exception {
    return new JSONObject().put("roles", roles).put("chats", chats).put("memories", memories)
        .put("diaries", diaries).put("moments", moments).put("settings", settings)
        .put("credentials", credentials).put("assets", assets);
  }

  static BackupSelection from(JSONObject value) {
    return new BackupSelection(value.optBoolean("roles"), value.optBoolean("chats"),
        value.optBoolean("memories"), value.optBoolean("diaries"), value.optBoolean("moments"),
        value.optBoolean("settings"), value.optBoolean("credentials"), value.optBoolean("assets"));
  }

  JSONObject state(JSONObject source) throws Exception {
    JSONObject result = new JSONObject();
    if (roles) result.put("cards", source.optJSONArray("cards"));
    if (chats) result.put("chats", source.optJSONArray("chats"));
    if (settings) {
      result.put("commonWorldBook", source.optString("commonWorldBook"));
      result.put("commonRegexRules", source.optString("commonRegexRules"));
    }
    return new JSONObject(result.toString());
  }

  JSONObject preferences(JSONObject source) throws Exception {
    JSONObject result = new JSONObject();
    Iterator<String> names = source.keys();
    while (names.hasNext()) {
      String name = names.next();
      if (name.equals("state") || name.startsWith("video_task") || name.contains("error")
          || name.contains("diagnostic") || name.contains("cache") || name.contains("history")) continue;
      boolean momentData = name.equals("moments") || name.startsWith("moment_");
      boolean chatData = name.startsWith("relationship_") || name.startsWith("chat_")
          || name.startsWith("story_") || name.startsWith("stories") || name.startsWith("role_state_");
      boolean secret = name.contains("api_key") || name.contains("cookie") || name.endsWith("_token");
      if (secret && !credentials) continue;
      if (momentData ? moments : chatData ? chats : settings) result.put(name, source.opt(name));
    }
    return result;
  }

  static JSONArray merge(JSONArray current, JSONArray incoming, String field) {
    LinkedHashMap<String, JSONObject> values = new LinkedHashMap<>();
    for (JSONArray source : new JSONArray[] {current, incoming}) {
      if (source == null) continue;
      for (int index = 0; index < source.length(); index++) {
        JSONObject value = source.optJSONObject(index);
        if (value == null) continue;
        String key = field.equals("memory") ? value.optString("cardId") + "\n" + value.optString("content")
            : value.optString(field);
        if (key.isEmpty()) key = value.toString();
        values.put(key, value);
      }
    }
    return new JSONArray(values.values());
  }

  static JSONObject overlay(JSONObject current, JSONObject incoming) throws Exception {
    JSONObject result = new JSONObject(current == null ? "{}" : current.toString());
    if (incoming != null) {
      Iterator<String> names = incoming.keys();
      while (names.hasNext()) {
        String name = names.next();
        result.put(name, incoming.opt(name));
      }
    }
    return result;
  }
}
