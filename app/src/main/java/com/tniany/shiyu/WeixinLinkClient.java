package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Base64;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

final class WeixinLinkClient {
  static final String ACTION_STATE_CHANGED = "com.tniany.shiyu.WEIXIN_LINK_STATE_CHANGED";
  private static final String DEFAULT_BASE = "https://ilinkai.weixin.qq.com";
  private static final String APP_ID = "bot";
  private static final String CLIENT_VERSION = "132102";
  private static final String ACCOUNTS = "weixin_link_accounts";

  static final class Account {
    final String id, cardId, ownerId, baseUrl, chatId;
    final boolean enabled;

    Account(String id, String cardId, String ownerId, String baseUrl, boolean enabled, String chatId) {
      this.id = id; this.cardId = cardId; this.ownerId = ownerId; this.baseUrl = baseUrl; this.enabled = enabled; this.chatId = chatId == null ? "" : chatId;
    }
  }

  static final class LoginResult {
    final String status, message, qrUrl;
    LoginResult(String status, String message, String qrUrl) {
      this.status = status; this.message = message; this.qrUrl = qrUrl;
    }
  }

  private WeixinLinkClient() {}

  static LoginResult startLogin(Context context, String cardId) throws Exception {
    return startLogin(context, cardId, "");
  }

  static LoginResult startLogin(Context context, String cardId, String chatId) throws Exception {
    if (cardId == null || cardId.isEmpty()) throw new IllegalStateException("请先选择角色");
    for (Account account : accounts(context)) if (cardId.equals(account.cardId))
      throw new IllegalStateException("这个角色已经接入微信");
    JSONArray localTokens = new JSONArray();
    WeixinLinkSecureStore secure = new WeixinLinkSecureStore(context);
    for (Account account : accounts(context)) {
      String token = secure.get(tokenKey(account.id));
      if (!token.isEmpty()) localTokens.put(token);
    }
    JSONObject response = post(DEFAULT_BASE, "ilink/bot/get_bot_qrcode?bot_type=3",
        new JSONObject().put("local_token_list", localTokens), "", 20_000);
    String code = response.optString("qrcode"), qrUrl = response.optString("qrcode_img_content");
    if (code.isEmpty() || qrUrl.isEmpty()) throw new IllegalStateException("微信未返回登录二维码");
    prefs(context).edit().putString("weixin_link_login_card_id", cardId)
        .putString("weixin_link_login_qrcode", code).putString("weixin_link_login_qr_url", qrUrl)
        .putString("weixin_link_login_base", DEFAULT_BASE).putString("weixin_link_login_chat_id", chatId == null ? "" : chatId)
        .putString("weixin_link_status", "wait")
        .putString("weixin_link_status_text", "请使用微信扫描二维码").apply();
    notifyState(context);
    return new LoginResult("wait", "请使用微信扫描二维码", qrUrl);
  }

  static LoginResult pollLogin(Context context) throws Exception {
    SharedPreferences prefs = prefs(context);
    String code = prefs.getString("weixin_link_login_qrcode", "");
    String cardId = prefs.getString("weixin_link_login_card_id", "");
    String chatId = prefs.getString("weixin_link_login_chat_id", "");
    String base = prefs.getString("weixin_link_login_base", DEFAULT_BASE);
    if (code.isEmpty() || cardId.isEmpty()) throw new IllegalStateException("请先生成登录二维码");
    String verify = prefs.getString("weixin_link_verify_code", "").trim();
    String endpoint = "ilink/bot/get_qrcode_status?qrcode=" + URLEncoder.encode(code, StandardCharsets.UTF_8.name());
    if (!verify.isEmpty()) endpoint += "&verify_code=" + URLEncoder.encode(verify, StandardCharsets.UTF_8.name());
    JSONObject response = get(base, endpoint, 40_000);
    String status = response.optString("status", "wait"), text;
    if ("scaned".equals(status)) {
      text = "二维码已扫描，请在微信中确认"; prefs.edit().remove("weixin_link_verify_code").apply();
    } else if ("need_verifycode".equals(status)) {
      text = "请输入微信显示的数字验证码";
      if (!verify.isEmpty()) prefs.edit().remove("weixin_link_verify_code").apply();
    } else if ("scaned_but_redirect".equals(status)) {
      String host = response.optString("redirect_host");
      if (!host.isEmpty()) prefs.edit().putString("weixin_link_login_base", "https://" + host).apply();
      text = "正在连接";
    } else if ("confirmed".equals(status)) {
      String token = response.optString("bot_token"), accountId = response.optString("ilink_bot_id");
      if (token.isEmpty() || accountId.isEmpty()) throw new IllegalStateException("微信登录凭据不完整");
      Account account = new Account(accountId, cardId, response.optString("ilink_user_id"),
          normalizeBase(response.optString("baseurl", base)), true, chatId);
      new WeixinLinkSecureStore(context).put(tokenKey(accountId), token);
      saveAccount(context, account);
      updateChatMarkers(context, null, account, accounts(context));
      prefs.edit().remove("weixin_link_login_qrcode").remove("weixin_link_verify_code")
          .remove("weixin_link_login_qr_url").remove("weixin_link_login_card_id").remove("weixin_link_login_chat_id")
          .putString("weixin_link_status", "connected").putString("weixin_link_status_text", "微信已连接").apply();
      WeixinLinkService.sync(context); text = "微信已连接";
    } else if ("expired".equals(status)) text = "二维码已过期，请重新生成";
    else if ("verify_code_blocked".equals(status)) text = "验证码错误次数过多，请重新生成二维码";
    else if ("binded_redirect".equals(status)) text = "此微信已绑定其他连接";
    else text = "等待扫描二维码";
    prefs.edit().putString("weixin_link_status", status).putString("weixin_link_status_text", text).apply();
    notifyState(context);
    return new LoginResult(status, text, prefs.getString("weixin_link_login_qr_url", ""));
  }

  static ArrayList<Account> accounts(Context context) {
    migrateLegacyAccount(context);
    ArrayList<Account> result = new ArrayList<>();
    try {
      JSONArray values = new JSONArray(prefs(context).getString(ACCOUNTS, "[]"));
      for (int i = 0; i < values.length(); i++) {
        JSONObject item = values.optJSONObject(i); if (item == null) continue;
        String id = item.optString("id"), cardId = item.optString("cardId");
        if (!id.isEmpty() && !cardId.isEmpty()) result.add(new Account(id, cardId,
            item.optString("ownerId"), normalizeBase(item.optString("baseUrl", DEFAULT_BASE)), item.optBoolean("enabled", true), item.optString("chatId", "")));
      }
    } catch (Exception ignored) {}
    return result;
  }

  private static void migrateLegacyAccount(Context context) {
    SharedPreferences prefs = prefs(context);
    String id = prefs.getString("weixin_link_account_id", "");
    String cardId = prefs.getString("weixin_link_card_id", "");
    if (id.isEmpty() || cardId.isEmpty()) return;
    try {
      String token = new WeixinLinkSecureStore(context).get();
      if (!token.isEmpty()) new WeixinLinkSecureStore(context).put(tokenKey(id), token);
      JSONArray values = new JSONArray(prefs.getString(ACCOUNTS, "[]"));
      values.put(new JSONObject().put("id", id).put("cardId", cardId)
          .put("ownerId", prefs.getString("weixin_link_owner_id", ""))
          .put("baseUrl", normalizeBase(prefs.getString("weixin_link_base_url", DEFAULT_BASE)))
          .put("enabled", prefs.getBoolean("weixin_link_enabled", true)).put("chatId", ""));
      prefs.edit().putString(ACCOUNTS, values.toString())
          .remove("weixin_link_account_id").remove("weixin_link_card_id")
          .remove("weixin_link_owner_id").remove("weixin_link_base_url")
          .remove("weixin_link_connected").remove("weixin_link_enabled").apply();
      new WeixinLinkSecureStore(context).clear("bot_token");
    } catch (Exception ignored) {}
  }

  static Account account(Context context, String id) {
    for (Account value : accounts(context)) if (value.id.equals(id)) return value;
    return null;
  }

  static void setEnabled(Context context, String id, boolean enabled) {
    Account old = account(context, id); if (old == null) return;
    saveAccount(context, new Account(old.id, old.cardId, old.ownerId, old.baseUrl, enabled, old.chatId));
    WeixinLinkService.sync(context);
  }

  static void setChatId(Context context, String id, String chatId) {
    Account old = account(context, id); if (old == null) return;
    setBinding(context, id, old.cardId, chatId);
  }

  static void setBinding(Context context, String id, String cardId, String chatId) {
    Account old = account(context, id); if (old == null) return;
    if (cardId == null || cardId.isEmpty()) return;
    Account updated = new Account(old.id, cardId, old.ownerId, old.baseUrl, old.enabled, chatId);
    ArrayList<Account> values = accounts(context);
    values.removeIf(value -> value.id.equals(id));
    values.add(updated);
    writeAccounts(context, values);
    updateChatMarkers(context, old, updated, values);
    notifyState(context);
    WeixinLinkService.sync(context);
  }

  static JSONObject getUpdates(Context context, Account account) throws Exception {
    JSONObject body = new JSONObject().put("get_updates_buf", prefs(context).getString(syncKey(account.id), ""))
        .put("base_info", baseInfo());
    return post(account.baseUrl, "ilink/bot/getupdates", body,
        new WeixinLinkSecureStore(context).get(tokenKey(account.id)), 45_000);
  }

  static void saveSync(Context context, String accountId, String value) {
    if (value != null && !value.isEmpty()) prefs(context).edit().putString(syncKey(accountId), value).apply();
  }

  static void sendText(Context context, Account account, String to, String contextToken, String text) throws Exception {
    JSONObject message = new JSONObject().put("from_user_id", "").put("to_user_id", to)
        .put("client_id", "shiyu-weixin-" + UUID.randomUUID()).put("message_type", 2).put("message_state", 2)
        .put("item_list", new JSONArray().put(new JSONObject().put("type", 1)
            .put("text_item", new JSONObject().put("text", text))));
    if (contextToken != null && !contextToken.isEmpty()) message.put("context_token", contextToken);
    JSONObject response = post(account.baseUrl, "ilink/bot/sendmessage",
        new JSONObject().put("msg", message).put("base_info", baseInfo()),
        new WeixinLinkSecureStore(context).get(tokenKey(account.id)), 20_000);
    if (response.optInt("ret", 0) != 0) throw new IllegalStateException(response.optString("errmsg", "微信消息发送失败"));
  }

  static void disconnect(Context context, String accountId) {
    Account old = account(context, accountId);
    ArrayList<Account> values = accounts(context); values.removeIf(value -> value.id.equals(accountId));
    writeAccounts(context, values); new WeixinLinkSecureStore(context).clear(tokenKey(accountId));
    updateChatMarkers(context, old, null, values);
    prefs(context).edit().remove(syncKey(accountId)).apply(); WeixinLinkService.sync(context); notifyState(context);
  }

  static void disconnectCard(Context context, String cardId) {
    ArrayList<Account> values = accounts(context);
    ArrayList<Account> removed = new ArrayList<>();
    values.removeIf(value -> { boolean match = value.cardId.equals(cardId); if (match) removed.add(value); return match; });
    writeAccounts(context, values);
    WeixinLinkSecureStore secure = new WeixinLinkSecureStore(context);
    SharedPreferences.Editor editor = prefs(context).edit();
    for (Account account : removed) { secure.clear(tokenKey(account.id)); editor.remove(syncKey(account.id)); updateChatMarkers(context, account, null, values); }
    editor.apply(); WeixinLinkService.sync(context); notifyState(context);
  }

  static boolean connected(Context context) { return !accounts(context).isEmpty(); }

  static Bitmap qrBitmap(String value, int size) throws Exception {
    BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
    int[] pixels = new int[size * size];
    for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) pixels[y * size + x] = matrix.get(x, y) ? 0xff101010 : 0xffffffff;
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
  }

  private static void saveAccount(Context context, Account account) {
    ArrayList<Account> values = accounts(context); values.removeIf(value -> value.id.equals(account.id)); values.add(account); writeAccounts(context, values);
  }

  private static void writeAccounts(Context context, ArrayList<Account> values) {
    JSONArray output = new JSONArray();
    for (Account value : values) try {
      output.put(new JSONObject().put("id", value.id).put("cardId", value.cardId)
          .put("ownerId", value.ownerId).put("baseUrl", value.baseUrl).put("enabled", value.enabled)
          .put("chatId", value.chatId));
    } catch (Exception ignored) {}
    prefs(context).edit().putString(ACCOUNTS, output.toString()).apply();
  }

  private static void updateChatMarkers(Context context, Account oldAccount, Account newAccount,
      ArrayList<Account> remaining) {
    try (AppDb db = new AppDb(context)) {
      String raw = db.loadState();
      if (raw.isEmpty()) return;
      JSONObject root = new JSONObject(raw);
      JSONArray chats = root.optJSONArray("chats");
      if (chats == null) return;
      boolean changed = false;
      for (int i = 0; i < chats.length(); i++) {
        JSONObject chat = chats.optJSONObject(i); if (chat == null) continue;
        String chatId = chat.optString("id");
        if (oldAccount != null && matchesOldChat(chat, oldAccount)
            && !boundByAnotherAccount(chat, oldAccount.id, remaining)) {
          chat.put("externalSource", "").put("externalPeer", "");
          changed = true;
        }
        if (newAccount != null && !newAccount.chatId.isEmpty()
            && newAccount.chatId.equals(chatId) && newAccount.cardId.equals(chat.optString("cardId"))) {
          chat.put("externalSource", "weixin").put("externalPeer", newAccount.ownerId);
          changed = true;
        }
      }
      if (changed) db.saveState(root.toString());
    } catch (Exception ignored) {}
  }

  private static boolean matchesOldChat(JSONObject chat, Account account) {
    if (!account.chatId.isEmpty()) return account.chatId.equals(chat.optString("id"));
    return "weixin".equals(chat.optString("externalSource"))
        && account.cardId.equals(chat.optString("cardId"))
        && (account.ownerId.isEmpty() || account.ownerId.equals(chat.optString("externalPeer")));
  }

  private static boolean boundByAnotherAccount(JSONObject chat, String excludedId,
      ArrayList<Account> accounts) {
    for (Account account : accounts) {
      if (account.id.equals(excludedId)) continue;
      if (!account.chatId.isEmpty() && account.chatId.equals(chat.optString("id"))) return true;
      if (account.chatId.isEmpty() && "weixin".equals(chat.optString("externalSource"))
          && account.cardId.equals(chat.optString("cardId"))
          && (account.ownerId.isEmpty() || account.ownerId.equals(chat.optString("externalPeer")))) return true;
    }
    return false;
  }

  private static JSONObject baseInfo() throws Exception { return new JSONObject().put("channel_version", "2.4.6").put("bot_agent", "ShiYu/" + BuildConfig.VERSION_NAME); }
  private static String tokenKey(String id) { return "bot_token_" + id; }
  private static String syncKey(String id) { return "weixin_link_sync_" + id; }
  private static JSONObject get(String base, String endpoint, int timeout) throws Exception { return readResponse(open(base, endpoint, "GET", "", timeout)); }
  private static JSONObject post(String base, String endpoint, JSONObject body, String token, int timeout) throws Exception {
    HttpURLConnection connection = open(base, endpoint, "POST", token, timeout); connection.setDoOutput(true);
    try {
      try (OutputStream output = connection.getOutputStream()) { output.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
      return readResponse(connection);
    } finally { connection.disconnect(); }
  }
  private static HttpURLConnection open(String base, String endpoint, String method, String token, int timeout) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(normalizeBase(base) + "/" + endpoint).openConnection();
    connection.setRequestMethod(method); connection.setConnectTimeout(20_000); connection.setReadTimeout(timeout);
    connection.setRequestProperty("iLink-App-Id", APP_ID); connection.setRequestProperty("iLink-App-ClientVersion", CLIENT_VERSION);
    if ("POST".equals(method)) {
      connection.setRequestProperty("Content-Type", "application/json"); connection.setRequestProperty("AuthorizationType", "ilink_bot_token");
      connection.setRequestProperty("X-WECHAT-UIN", randomUin()); if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
    }
    return connection;
  }
  private static JSONObject readResponse(HttpURLConnection connection) throws Exception {
    try {
      int code = connection.getResponseCode(); String raw = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
      if (code < 200 || code >= 300) throw new IllegalStateException("微信连接 HTTP " + code); return raw.isEmpty() ? new JSONObject() : new JSONObject(raw);
    } finally { connection.disconnect(); }
  }
  private static String read(InputStream input) throws Exception {
    if (input == null) return ""; StringBuilder value = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) value.append(line); }
    return value.toString();
  }
  private static String randomUin() { long value = new SecureRandom().nextInt() & 0xffffffffL; return Base64.encodeToString(String.valueOf(value).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP); }
  private static String normalizeBase(String value) { String base = value == null || value.trim().isEmpty() ? DEFAULT_BASE : value.trim(); if (!base.startsWith("http")) base = "https://" + base; while (base.endsWith("/")) base = base.substring(0, base.length() - 1); return base; }
  private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE); }
  private static void notifyState(Context context) { context.sendBroadcast(new android.content.Intent(ACTION_STATE_CHANGED).setPackage(context.getPackageName())); }
}
