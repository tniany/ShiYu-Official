package com.tniany.shiyu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Generates and persists replies initiated from the desktop pet overlay. */
final class DesktopPetReplyEngine {
  static final String ACTION_STATE_CHANGED = "com.tniany.shiyu.DESKTOP_PET_STATE_CHANGED";

  static final class Result {
    final String chatId;
    final String text;
    final long createdAt;

    Result(String chatId, String text, long createdAt) {
      this.chatId = chatId;
      this.text = text;
      this.createdAt = createdAt;
    }
  }

  private DesktopPetReplyEngine() {}

  static synchronized Result reply(Context context, String cardId, String userText, String messageType)
      throws Exception {
    return reply(context, cardId, userText, messageType, "", "");
  }

  static synchronized Result reply(Context context, String cardId, String userText, String messageType,
      String externalPeer, String externalTitle) throws Exception {
    return reply(context, cardId, userText, messageType, externalPeer, externalTitle, "");
  }

  static synchronized Result reply(Context context, String cardId, String userText, String messageType,
      String externalPeer, String externalTitle, String preferredChatId) throws Exception {
    Context app = context.getApplicationContext();
    SharedPreferences prefs = app.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    long now = System.currentTimeMillis();
    if (prefs.getLong("active_generation_until", 0) > now)
      throw new IllegalStateException("当前有其他回复正在生成，请稍后再试");

    String endpoint = prefs.getString("endpoint", "").trim();
    String model = prefs.getString("model", "").trim();
    String key = new SecureStore(app).get();
    if (endpoint.isEmpty() || model.isEmpty() || key.isEmpty())
      throw new IllegalStateException("请先在时遇中配置模型供应商");
    while (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
    if (!endpoint.endsWith("/chat/completions")) endpoint += "/chat/completions";

    long reservation = now + 120_000L;
    prefs.edit().putLong("active_generation_until", reservation).commit();
    try (AppDb db = new AppDb(app)) {
      String saved = db.loadState();
      if (saved.isEmpty()) throw new IllegalStateException("没有可用的角色数据");
      JSONObject root = new JSONObject(saved);
      JSONArray cardsJson = root.optJSONArray("cards");
      JSONArray chatsJson = root.optJSONArray("chats");
      if (cardsJson == null) throw new IllegalStateException("没有可用的角色数据");
      if (chatsJson == null) chatsJson = new JSONArray();

      MainActivity.Card card = null;
      for (int i = 0; i < cardsJson.length(); i++) {
        JSONObject value = cardsJson.optJSONObject(i);
        if (value != null && cardId.equals(value.optString("id"))) {
          card = MainActivity.Card.from(value);
          break;
        }
      }
      if (card == null) throw new IllegalStateException("桌宠角色已不存在，请重新选择");

      int chatIndex = -1;
      MainActivity.Chat chat = null;
      long newest = Long.MIN_VALUE;
      if (preferredChatId != null && !preferredChatId.isEmpty()) for (int i = 0; i < chatsJson.length(); i++) {
        JSONObject value = chatsJson.optJSONObject(i);
        if (value == null || value.optBoolean("group", false)
            || !preferredChatId.equals(value.optString("id"))
            || !card.id.equals(value.optString("cardId"))) continue;
        chat = MainActivity.Chat.from(value);
        chatIndex = i;
        break;
      }
      for (int i = 0; i < chatsJson.length(); i++) {
        JSONObject value = chatsJson.optJSONObject(i);
        if (value == null || value.optBoolean("group", false)
            || !card.id.equals(value.optString("cardId"))) continue;
        if (chat != null) break;
        if (!externalPeer.isEmpty() && !externalPeer.equals(value.optString("externalPeer"))) continue;
        if (externalPeer.isEmpty() && !value.optString("externalPeer").isEmpty()) continue;
        long updated = value.optLong("updated", 0);
        if (chat == null || updated >= newest) {
          chat = MainActivity.Chat.from(value);
          chatIndex = i;
          newest = updated;
        }
      }
      if (chat == null) {
        chat = createChat(card, prefs);
        if (!externalPeer.isEmpty()) {
          chat.externalSource = "weixin";
          chat.externalPeer = externalPeer;
          chat.title = externalTitle.isEmpty() ? "微信 · " + card.name : externalTitle;
        }
        chatIndex = chatsJson.length();
      }
      if (!externalPeer.isEmpty()) {
        chat.externalSource = "weixin";
        chat.externalPeer = externalPeer;
        if (!externalTitle.isEmpty() && (chat.title == null || chat.title.isEmpty() || chat.title.startsWith("微信"))) chat.title = externalTitle;
      }

      boolean screenContext = "desktop_pet_screen_context".equals(messageType);
      boolean weixinLink = "weixin_link".equals(messageType);
      MainActivity.Msg user = new MainActivity.Msg("user", screenContext ? "分享了当前屏幕" : userText.trim());
      user.messageType = screenContext ? "screen_context_share" : (messageType == null ? "desktop_pet" : messageType);
      chat.messages.add(user);
      MainActivity.Chat targetChat = chat;
      MainActivity.Card targetCard = card;
      String targetEndpoint = endpoint;
      String generated = GenerationRetry.run(prefs,
          () -> generateOnce(app, targetEndpoint, key, model, prefs, db, targetChat, targetCard,
              screenContext ? userText.trim() : "", weixinLink));
      generated = cleanReply(generated);
      if (generated.isEmpty()) throw new IllegalStateException("角色没有返回可显示的内容");

      MainActivity.Msg assistant = new MainActivity.Msg("assistant", generated);
      assistant.speakerCardId = card.id;
      assistant.messageType = "desktop_pet_reply";
      chat.messages.add(assistant);
      chat.updated = assistant.createdAt;
      chat.proactiveSequence = 0;
      chatsJson.put(chatIndex, chat.json());
      root.put("chats", chatsJson);
      db.saveState(root.toString());
      long revision = Math.max(assistant.createdAt, prefs.getLong("background_state_revision", 0) + 1);
      prefs.edit().putLong("background_state_revision", revision)
          .putLong("desktop_pet_last_bubble_at", assistant.createdAt).commit();
      app.sendBroadcast(new Intent(ACTION_STATE_CHANGED).setPackage(app.getPackageName()));
      return new Result(chat.id, generated, assistant.createdAt);
    } finally {
      if (prefs.getLong("active_generation_until", 0) == reservation)
        prefs.edit().remove("active_generation_until").apply();
    }
  }

  private static MainActivity.Chat createChat(MainActivity.Card card, SharedPreferences prefs) {
    MainActivity.Chat chat = new MainActivity.Chat();
    chat.id = UUID.randomUUID().toString();
    chat.cardId = card.id;
    chat.title = "与" + card.name + "的聊天";
    chat.updated = System.currentTimeMillis();
    chat.immersiveMode = prefs.getBoolean("default_private_immersive", false);
    chat.privateReplyDecision = prefs.getBoolean("default_private_reply_decision", false);
    chat.proactiveMinMinutes = prefs.getInt("default_private_proactive_min", 180);
    chat.proactiveMaxMinutes = prefs.getInt("default_private_proactive_max", 180);
    chat.proactiveBurstCount = prefs.getInt("default_private_burst", 1);
    if (!card.greeting.isEmpty()) {
      MainActivity.Msg greeting = new MainActivity.Msg("assistant", card.greeting);
      greeting.speakerCardId = card.id;
      chat.messages.add(greeting);
    }
    return chat;
  }

  private static String generateOnce(Context context, String endpoint, String key, String model,
      SharedPreferences prefs, AppDb db, MainActivity.Chat chat, MainActivity.Card card,
      String transientScreenContext, boolean weixinLink) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    try {
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(20_000);
      connection.setReadTimeout(90_000);
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      connection.setRequestProperty("Authorization", "Bearer " + key);

      JSONArray messages = new JSONArray();
      String identityDescription = prefs.getString("active_identity_description", "").trim();
      String identityName = prefs.getString("active_identity_name", "").trim();
      String identity = identityName.isEmpty() ? identityDescription : identityName + "：" + identityDescription;
      String worldBook = activeWorldBook(
          chat.worldBookOverride ? chat.worldBook : card.worldBook, chat.messages, card, prefs);
      String preset = chat.presetOverride ? chat.preset : card.preset;
      String regex = chat.regexRulesOverride ? chat.regexRules : card.regexRules;
      String memory = db.memoryContext(card.id);
      String system = "你是 " + card.name + "。始终保持角色卡设定的人格、措辞、关系和立场，不得代替用户发言。"
          + card.persona
          + (card.desc.isEmpty() ? "" : "\n角色简介：" + card.desc)
          + (identity.isEmpty() ? "" : "\n用户当前身份设定：" + identity)
          + (memory.isEmpty() ? "" : "\n请自然遵循这些长期记忆：\n" + memory)
          + (chat.summary.isEmpty() ? "" : "\n此前对话摘要：\n" + chat.summary)
          + (worldBook.isEmpty() ? "" : "\n世界书：\n" + worldBook)
          + (regex.isEmpty() ? "" : "\n文本正则规则：\n" + regex)
          + (preset.isEmpty() ? "" : "\n对话预设：\n" + expand(preset, card, prefs))
          + (card.exampleMessages.isEmpty() ? "" : "\n角色示例对话：\n" + expand(card.exampleMessages, card, prefs))
          + realityContext(prefs)
          + (weixinLink
              ? "\n你正在通过微信私聊与用户交谈。像真人微信聊天一样自然回复，可以结合最近聊天，但不要提及连接器、API、模型或系统提示。"
                  + "回复适合微信阅读，通常1到4句、最多300字；不要加角色名前缀，不要解释任务，不要输出内部指令。"
              : "\n你正以桌宠悬浮在用户当前页面旁边。把互动视为真实发生，结合最近聊天自然回应。"
                  + "回复适合弹幕气泡阅读，通常1到2句、最多100字；不要加角色名前缀，不要解释任务，不要输出内部指令。");
      system = expand(system, card, prefs);
      if (!card.systemPrompt.isEmpty()) {
        String override = expand(card.systemPrompt, card, prefs);
        system = override.contains("{{original}}") ? override.replace("{{original}}", system)
            : system + "\n角色卡系统提示：\n" + override;
      }
      messages.put(new JSONObject().put("role", "system").put("content", system));

      int count = Math.max(6, Math.min(40, prefs.getInt("context", 30)));
      for (int i = Math.max(0, chat.messages.size() - count); i < chat.messages.size(); i++) {
        MainActivity.Msg message = chat.messages.get(i);
        if (message.loading || "tavern_frontend".equals(message.messageType)
            || "image_request".equals(message.messageType) || message.body.trim().isEmpty()) continue;
        if ("screen_context_share".equals(message.messageType)
            || "desktop_pet_screen_context".equals(message.messageType)) continue;
        String content = message.body;
        if ("desktop_pet_interaction".equals(message.messageType)) content = "[桌宠互动：" + content + "]";
        else if ("desktop_pet_feed".equals(message.messageType)) content = "[桌宠投喂：" + content + "]";
        messages.put(new JSONObject().put("role", message.role).put("content", content));
      }
      if (!transientScreenContext.isEmpty())
        messages.put(new JSONObject().put("role", "user").put("content", transientScreenContext));
      if (!card.postHistoryInstructions.isEmpty())
        messages.put(new JSONObject().put("role", "system")
            .put("content", expand(card.postHistoryInstructions, card, prefs)));

      double temperature = Double.longBitsToDouble(
          prefs.getLong("temp", Double.doubleToLongBits(.85)));
      JSONObject body = new JSONObject().put("model", model).put("temperature", temperature)
          .put("stream", false).put("messages", messages);
      try (OutputStream output = connection.getOutputStream()) {
        output.write(body.toString().getBytes(StandardCharsets.UTF_8));
      }
      int code = connection.getResponseCode();
      String raw = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
      if (code < 200 || code >= 300)
        throw new IllegalStateException("HTTP " + code + (raw.isEmpty() ? "" : "：" + raw));
      JSONArray choices = new JSONObject(raw).optJSONArray("choices");
      if (choices == null || choices.length() == 0) throw new IllegalStateException("接口未返回内容");
      JSONObject message = choices.optJSONObject(0).optJSONObject("message");
      if (message == null) throw new IllegalStateException("接口未返回消息正文");
      return message.optString("content", "");
    } finally {
      connection.disconnect();
    }
  }

  private static String activeWorldBook(String raw, ArrayList<MainActivity.Msg> messages,
      MainActivity.Card card, SharedPreferences prefs) {
    if (raw == null || raw.trim().isEmpty()) return "";
    try {
      String clean = raw.trim();
      JSONObject root = clean.startsWith("{") ? new JSONObject(clean) : null;
      JSONArray entries = clean.startsWith("[") ? new JSONArray(clean)
          : root == null ? null : root.optJSONArray("entries");
      if (entries == null && root != null) {
        JSONObject map = root.optJSONObject("entries");
        if (map != null) {
          entries = new JSONArray();
          java.util.Iterator<String> keys = map.keys();
          while (keys.hasNext()) {
            JSONObject entry = map.optJSONObject(keys.next());
            if (entry != null) entries.put(entry);
          }
        }
      }
      if (entries == null) return expand(raw, card, prefs);
      int depth = root == null ? 12 : Math.max(1, Math.min(100, root.optInt("scan_depth", 12)));
      StringBuilder recent = new StringBuilder();
      for (int i = Math.max(0, messages.size() - depth); i < messages.size(); i++)
        recent.append(messages.get(i).body).append('\n');
      ArrayList<JSONObject> selected = new ArrayList<>();
      for (int i = 0; i < entries.length(); i++) {
        JSONObject entry = entries.optJSONObject(i);
        if (entry == null || (entry.has("enabled") && !entry.optBoolean("enabled", true))) continue;
        String content = entry.optString("content", entry.optString("text", entry.optString("entry", ""))).trim();
        if (content.isEmpty()) continue;
        boolean include = entry.optBoolean("constant", false);
        String hay = recent.toString().toLowerCase(Locale.ROOT);
        JSONArray keys = entry.optJSONArray("keys");
        if (keys == null) keys = entry.optJSONArray("key");
        if (!include && keys != null) for (int j = 0; j < keys.length(); j++) {
          String key = keys.optString(j).trim().toLowerCase(Locale.ROOT);
          if (!key.isEmpty() && hay.contains(key)) { include = true; break; }
        }
        if (!include) for (String key : entry.optString("keys", entry.optString("key", "")).split("[,，|]")) {
          key = key.trim().toLowerCase(Locale.ROOT);
          if (!key.isEmpty() && hay.contains(key)) { include = true; break; }
        }
        if (include) selected.add(entry);
      }
      selected.sort(Comparator.comparingInt(value -> value.optInt("insertion_order", value.optInt("order", 100))));
      StringBuilder output = new StringBuilder();
      for (JSONObject entry : selected) {
        if (output.length() > 0) output.append('\n');
        output.append(expand(entry.optString("content", entry.optString("text", entry.optString("entry", ""))), card, prefs));
        if (output.length() > 6000) break;
      }
      return output.toString();
    } catch (Exception ignored) {
      return expand(raw, card, prefs);
    }
  }

  private static String realityContext(SharedPreferences prefs) {
    if (!prefs.getBoolean("reality_awareness_enabled", true)) return "";
    Calendar calendar = Calendar.getInstance();
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    String period = hour < 5 ? "深夜" : hour < 9 ? "早晨" : hour < 12 ? "上午"
        : hour < 14 ? "中午" : hour < 18 ? "下午" : hour < 22 ? "晚上" : "深夜";
    String value = new SimpleDateFormat("yyyy年M月d日 EEEE HH:mm", Locale.CHINA).format(calendar.getTime());
    if (prefs.getBoolean("reality_include_timezone", true)) value += "，时区 " + TimeZone.getDefault().getID();
    return "\n当前现实时间：" + value + "（" + period + "）。只在情境需要时自然使用。";
  }

  private static String expand(String value, MainActivity.Card card, SharedPreferences prefs) {
    if (value == null) return "";
    String user = prefs.getString("active_identity_name", prefs.getString("user_name", "用户"));
    return value.replace("{{char}}", card.name).replace("{{user}}", user == null ? "用户" : user);
  }

  private static String cleanReply(String value) {
    if (value == null) return "";
    String clean = value.replace("<SEG>", "\n").replace("<segment>", "\n")
        .replace("[NO_REPLY]", "").trim();
    return clean.length() > 500 ? clean.substring(0, 500).trim() + "…" : clean;
  }

  private static String read(InputStream input) throws Exception {
    if (input == null) return "";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) result.append(line);
      return result.toString();
    }
  }
}
