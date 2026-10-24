package com.tniany.shiyu;

import java.util.List;
import org.json.JSONObject;

final class TogetherListening {
  private TogetherListening() {}

  static boolean belongsTo(JSONObject state, String chatId, String cardId) {
    return chatId != null && !chatId.isEmpty() && cardId != null && !cardId.isEmpty()
        && chatId.equals(state.optString("chatId")) && cardId.equals(state.optString("roleId"));
  }

  static boolean matchesPlayback(JSONObject state, String playbackId) {
    return playbackId != null && !playbackId.isEmpty()
        && playbackId.equals(state.optString("playbackId"));
  }

  static boolean canAdvance(JSONObject state, String chatId, String cardId,
                            String playbackId, long songId) {
    return belongsTo(state, chatId, cardId) && matchesPlayback(state, playbackId)
        && state.optBoolean("completed") && "idle".equals(state.optString("status"))
        && songId > 0 && songId == state.optLong("songId");
  }

  static String sessionKey(String chatId) {
    if (chatId == null || chatId.isEmpty()) throw new IllegalArgumentException("Missing listening chat");
    return "together_listen_chat_" + chatId;
  }

  static String context(JSONObject state, String chatId, String cardId) {
    String status = state.optString("status");
    if (!belongsTo(state, chatId, cardId) || !("playing".equals(status) || "paused".equals(status))
        || state.optLong("songId") <= 0) return "";
    return "\n当前一起听歌的状态：你和用户正在听《" + state.optString("title") + "》，歌手 "
        + state.optString("artist") + ("paused".equals(status) ? "，已暂停。" : "，播放中。")
        + "这只是场景背景，相关时自然提及，不必每次回复都谈音乐。";
  }

  static boolean shouldAnnounce(List<MainActivity.Msg> messages, long songId) {
    for (int i = messages.size() - 1; i >= 0; i--) {
      MainActivity.Msg message = messages.get(i);
      if (!"music".equals(message.messageType) || !"user".equals(message.role)) continue;
      try {
        JSONObject meta = new JSONObject(message.meta);
        if ("together_listen".equals(meta.optString("scene")))
          return meta.optLong("songId") != songId;
      } catch (Exception ignored) { }
    }
    return true;
  }
}
