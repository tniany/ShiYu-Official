package com.tniany.shiyu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Stores official QQ Bot configurations without placing AppSecret in plain preferences. */
public final class QqBotClient {
  public static final String ACTION_STATE_CHANGED = "com.tniany.shiyu.QQBOT_STATE_CHANGED";
  static final String PREF_ACCOUNTS = "qqbot_accounts";
  private static final int MAX_ACCOUNTS = 8;

  public static final class Account {
    public final String id;
    public final String name;
    public final String appId;
    public final String cardId;
    public final String chatId;
    public final boolean enabled;

    public Account(String id, String name, String appId, String cardId, String chatId,
        boolean enabled) {
      this.id = clean(id);
      this.name = clean(name);
      this.appId = clean(appId);
      this.cardId = clean(cardId);
      this.chatId = clean(chatId);
      this.enabled = enabled;
    }

    Account withEnabled(boolean value) {
      return new Account(id, name, appId, cardId, chatId, value);
    }

    JSONObject json() throws Exception {
      return new JSONObject().put("id", id).put("name", name).put("appId", appId)
          .put("cardId", cardId).put("chatId", chatId).put("enabled", enabled);
    }

    static Account from(JSONObject value) {
      return new Account(value.optString("id"), value.optString("name"),
          value.optString("appId"), value.optString("cardId"), value.optString("chatId"),
          value.optBoolean("enabled", true));
    }

    String signature() {
      return id + '\u0000' + name + '\u0000' + appId + '\u0000' + cardId + '\u0000'
          + chatId + '\u0000' + enabled;
    }
  }

  private QqBotClient() {}

  public static ArrayList<Account> accounts(Context context) {
    ArrayList<Account> result = new ArrayList<>();
    try {
      JSONArray values = new JSONArray(prefs(context).getString(PREF_ACCOUNTS, "[]"));
      for (int i = 0; i < values.length() && result.size() < MAX_ACCOUNTS; i++) {
        JSONObject value = values.optJSONObject(i);
        if (value == null) continue;
        Account account = Account.from(value);
        if (!account.id.isEmpty() && !account.appId.isEmpty()) result.add(account);
      }
    } catch (Exception ignored) {}
    return result;
  }

  public static Account account(Context context, String id) {
    for (Account value : accounts(context)) if (value.id.equals(id)) return value;
    return null;
  }

  public static void saveAccounts(Context context, List<Account> accounts) {
    JSONArray values = new JSONArray();
    try {
      int count = 0;
      for (Account account : accounts) {
        if (account == null || account.id.isEmpty() || account.appId.isEmpty()) continue;
        values.put(account.json());
        if (++count >= MAX_ACCOUNTS) break;
      }
    } catch (Exception error) {
      throw new IllegalArgumentException("QQ Bot 配置无效", error);
    }
    prefs(context).edit().putString(PREF_ACCOUNTS, values.toString()).apply();
    QqBotService.sync(context);
    notifyState(context);
  }

  public static void setEnabled(Context context, String id, boolean enabled) {
    ArrayList<Account> values = accounts(context);
    for (int i = 0; i < values.size(); i++) {
      if (values.get(i).id.equals(id)) {
        values.set(i, values.get(i).withEnabled(enabled));
        prefs(context).edit().putString(statusKey(id), enabled ? "等待连接" : "已暂停")
            .remove(errorKey(id)).apply();
        saveAccounts(context, values);
        return;
      }
    }
  }

  public static void delete(Context context, String id) {
    ArrayList<Account> values = accounts(context);
    for (int i = values.size() - 1; i >= 0; i--)
      if (values.get(i).id.equals(id)) values.remove(i);
    try { new SecureStore(app(context)).putNamed(secretKey(id), ""); }
    catch (Exception ignored) {}
    prefs(context).edit().remove(statusKey(id)).remove(errorKey(id)).apply();
    saveAccounts(context, values);
  }

  public static boolean connected(Context context) {
    for (Account account : accounts(context)) {
      if (account.enabled && "已连接".equals(prefs(context).getString(statusKey(account.id), "")))
        return true;
    }
    return false;
  }

  public static int enabledCount(Context context) {
    int count = 0;
    for (Account account : accounts(context)) if (account.enabled) count++;
    return count;
  }

  public static String secretKey(String id) {
    return "qqbot_secret_" + clean(id);
  }

  static String statusKey(String id) {
    return "qqbot_status_" + clean(id);
  }

  static String errorKey(String id) {
    return "qqbot_error_" + clean(id);
  }

  static void notifyState(Context context) {
    try {
      app(context).sendBroadcast(
          new Intent(ACTION_STATE_CHANGED).setPackage(context.getPackageName()));
    } catch (RuntimeException ignored) {}
  }

  private static SharedPreferences prefs(Context context) {
    return app(context).getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
  }

  private static Context app(Context context) {
    Context value = context.getApplicationContext();
    return value == null ? context : value;
  }

  private static String clean(String value) {
    return value == null ? "" : value.trim();
  }
}
