package com.tniany.shiyu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ChatRequestMessages {
  static final class InvalidContextException extends IOException {
    InvalidContextException(String message) { super(message); }
  }
  private ChatRequestMessages() {}

  static boolean eligible(MainActivity.Msg message) {
    if (message == null || message.loading || message.failed || !message.delivered) return false;
    if ("tool_event".equals(message.messageType) || "tavern_frontend".equals(message.messageType)
        || "image_request".equals(message.messageType)) return false;
    return !blank(message.body) || !blank(message.image) || !blank(message.file);
  }

  static List<MainActivity.Msg> recent(List<MainActivity.Msg> messages, int count) {
    ArrayList<MainActivity.Msg> valid = new ArrayList<>();
    for (MainActivity.Msg message : messages) if (eligible(message)) valid.add(message);
    int start = Math.max(0, valid.size() - Math.max(2, count));
    // Consecutive user messages are one turn, including immersive chat bursts.
    int lastUser = -1;
    for (int i = valid.size() - 1; i >= 0; i--) {
      if ("user".equals(valid.get(i).role)) { lastUser = i; break; }
    }
    if (lastUser >= 0) {
      while (lastUser > 0 && "user".equals(valid.get(lastUser - 1).role)) lastUser--;
      start = Math.min(start, lastUser);
    }
    return new ArrayList<>(valid.subList(start, valid.size()));
  }

  static Object content(String text, String imageUrl) throws JSONException {
    String caption = text == null ? "" : text;
    if (blank(imageUrl)) return caption;
    JSONArray parts = new JSONArray();
    if (!blank(caption)) parts.put(new JSONObject().put("type", "text").put("text", caption));
    return parts.put(new JSONObject().put("type", "image_url")
        .put("image_url", new JSONObject().put("url", imageUrl)));
  }

  static void append(JSONArray messages, String role, Object content) throws JSONException {
    if (hasContent(content)) messages.put(new JSONObject().put("role", role).put("content", content));
  }

  static boolean hasContent(Object content) {
    if (content instanceof String) return !blank((String) content);
    if (!(content instanceof JSONArray)) return false;
    JSONArray parts = (JSONArray) content;
    for (int i = 0; i < parts.length(); i++) {
      JSONObject part = parts.optJSONObject(i);
      if (part == null) continue;
      if ("text".equals(part.optString("type")) && !blank(part.optString("text"))) return true;
      JSONObject image = part.optJSONObject("image_url");
      if ("image_url".equals(part.optString("type")) && image != null
          && !blank(image.optString("url"))) return true;
    }
    return false;
  }

  static void trimToBudget(JSONArray messages, int configuredTokens) throws IOException {
    for (int i = messages.length() - 1; i >= 0; i--) {
      JSONObject message = messages.optJSONObject(i);
      if (message == null || !hasContent(message.opt("content"))) messages.remove(i);
    }
    int latestUser = -1;
    for (int i = messages.length() - 1; i >= 0; i--) {
      if ("user".equals(messages.optJSONObject(i).optString("role"))) { latestUser = i; break; }
    }
    if (latestUser < 0) throw new InvalidContextException("没有可发送的用户消息，请输入内容后重试");
    while (latestUser > 0
        && "user".equals(messages.optJSONObject(latestUser - 1).optString("role"))) latestUser--;
    int maxTokens = Math.max(1024, Math.min(2_000_000, configuredTokens));
    int budget = Math.max(768, maxTokens - Math.max(512, Math.min(8192, maxTokens / 8)));
    // Never sacrifice the current user turn to retain a trailing system instruction.
    while (estimate(messages) > budget) {
      int firstHistory = 0;
      while (firstHistory < latestUser
          && "system".equals(messages.optJSONObject(firstHistory).optString("role"))) firstHistory++;
      if (firstHistory >= latestUser) break;
      int end = firstHistory + 1;
      while (end < latestUser
          && !"user".equals(messages.optJSONObject(end).optString("role"))) end++;
      for (int i = end - 1; i >= firstHistory; i--) messages.remove(i);
      latestUser -= end - firstHistory;
    }
    if (estimate(messages) > budget) {
      throw new InvalidContextException("角色设定与本轮消息超过上下文上限，请提高模型上下文长度或精简设定后重试；本轮消息未被丢弃");
    }
  }

  static int estimate(JSONArray messages) {
    long tokens = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject message = messages.optJSONObject(i);
      if (message == null) continue;
      tokens += 6;
      Object content = message.opt("content");
      if (content instanceof String) tokens += (long) Math.ceil(((String) content).length() / 2.2);
      else if (content instanceof JSONArray) {
        JSONArray parts = (JSONArray) content;
        for (int p = 0; p < parts.length(); p++) {
          JSONObject part = parts.optJSONObject(p);
          if (part == null) continue;
          if ("text".equals(part.optString("type")))
            tokens += (long) Math.ceil(part.optString("text").length() / 2.2);
          else if ("image_url".equals(part.optString("type"))) tokens += 1024;
        }
      }
    }
    return (int) Math.min(Integer.MAX_VALUE, Math.max(1, tokens));
  }

  private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
