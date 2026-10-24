package com.tniany.shiyu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public final class WeixinLinkService extends Service {
  private static final String CHANNEL_ID = "weixin_link";
  private static final int NOTIFICATION_ID = 7054;
  private ExecutorService executor;
  private volatile boolean stopped;
  private volatile long monitorGeneration;
  private boolean foregroundReady;
  private String monitorSignature = "";
  private SharedPreferences prefs;

  static void sync(Context context) {
    Context app = context.getApplicationContext() == null ? context : context.getApplicationContext();
    Intent intent = new Intent(app, WeixinLinkService.class);
    if (MainActivity.isDataRescueRequired(app)) {
      app.stopService(intent);
      return;
    }
    try {
      boolean enabled = false;
      for (WeixinLinkClient.Account account : WeixinLinkClient.accounts(app)) if (account.enabled) { enabled = true; break; }
      if (enabled) {
        if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent); else app.startService(intent);
      } else app.stopService(intent);
    } catch (RuntimeException error) {
      recordServiceError(app, error);
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    prefs = getSharedPreferences(MainActivity.PREF, MODE_PRIVATE);
    if (MainActivity.isDataRescueRequired(this)) {
      stopSelf();
      return;
    }
    CrashProtection.markFeatureActive(this, CrashProtection.FEATURE_WEIXIN);
    try {
      startForeground(NOTIFICATION_ID, notification("角色正在等待微信消息"));
      foregroundReady = true;
      prefs.edit().remove("weixin_link_service_error").apply();
      refreshMonitors();
    } catch (RuntimeException error) {
      foregroundReady = false;
      recordServiceError(this, error);
      stopSelf();
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (MainActivity.isDataRescueRequired(this) || !foregroundReady) {
      stopSelf(startId);
      return START_NOT_STICKY;
    }
    try {
      return refreshMonitors() ? START_STICKY : START_NOT_STICKY;
    } catch (RuntimeException error) {
      recordServiceError(this, error);
      stopSelf(startId);
      return START_NOT_STICKY;
    }
  }

  private synchronized boolean refreshMonitors() {
    ArrayList<WeixinLinkClient.Account> enabled = new ArrayList<>();
    for (WeixinLinkClient.Account account : WeixinLinkClient.accounts(this)) if (account.enabled) enabled.add(account);
    if (enabled.isEmpty()) {
      monitorSignature = "";
      monitorGeneration++;
      if (executor != null) { executor.shutdownNow(); executor = null; }
      stopSelf();
      return false;
    }
    enabled.sort((left, right) -> left.id.compareTo(right.id));
    StringBuilder signature = new StringBuilder();
    for (WeixinLinkClient.Account account : enabled) signature.append(account.id).append('\u0000')
        .append(account.cardId).append('\u0000').append(account.ownerId).append('\u0000')
        .append(account.baseUrl).append('\u0000').append(account.chatId).append('\u0001');
    String nextSignature = signature.toString();
    if (nextSignature.equals(monitorSignature) && executor != null && !executor.isShutdown()) return true;

    long generation = ++monitorGeneration;
    ExecutorService previous = executor;
    ExecutorService replacement = Executors.newFixedThreadPool(enabled.size());
    executor = replacement;
    monitorSignature = nextSignature;
    if (previous != null) previous.shutdownNow();
    for (WeixinLinkClient.Account account : enabled)
      replacement.execute(() -> monitor(account.id, generation));
    return true;
  }

  private void monitor(String accountId, long generation) {
    while (!stopped && generation == monitorGeneration) {
      CrashProtection.markFeatureActive(this, CrashProtection.FEATURE_WEIXIN);
      WeixinLinkClient.Account account = WeixinLinkClient.account(this, accountId);
      if (account == null || !account.enabled) return;
      try {
        JSONObject response = WeixinLinkClient.getUpdates(this, account);
        if (stopped || generation != monitorGeneration) return;
        int ret = response.optInt("ret", response.optInt("errcode", 0));
        if (ret != 0) throw new IllegalStateException(response.optString("errmsg", "微信连接错误 " + ret));
        JSONArray messages = response.optJSONArray("msgs");
        if (messages != null) for (int i = 0; i < messages.length(); i++) process(account, messages.optJSONObject(i));
        WeixinLinkClient.saveSync(this, account.id, response.optString("get_updates_buf"));
        updateStatus(account.id, "微信已连接", "");
      } catch (SocketTimeoutException ignored) {
      } catch (Exception error) {
        if (stopped || generation != monitorGeneration) return;
        String detail = message(error);
        String previous = prefs.getString("weixin_link_error_" + account.id, "");
        updateStatus(account.id, "正在重连", detail);
        if (!detail.equals(previous)) DiagnosticLog.record(this, "weixin", "error", detail);
        try { Thread.sleep(3_000L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
      }
    }
  }

  private void process(WeixinLinkClient.Account account, JSONObject message) throws Exception {
    if (message == null || message.optInt("message_type", 1) != 1) return;
    String sender = message.optString("from_user_id");
    if (sender.isEmpty() || !senderAllowed(account, sender)) return;
    String identity = account.id + ":" + message.optString("message_id", message.optString("seq"));
    if (seen(identity)) return;
    String text = textOf(message.optJSONArray("item_list"));
    if (text.isEmpty()) { markSeen(identity); return; }
    String pending = pendingReply(identity);
    if (pending.isEmpty()) {
        DesktopPetReplyEngine.Result result = DesktopPetReplyEngine.reply(this, account.cardId, text,
          "weixin_link", sender, "微信 · " + cardName(account.cardId), account.chatId);
      pending = result.text; savePendingReply(identity, pending);
    }
    if (pending.trim().isEmpty()) {
      savePendingReply(identity, "");
      markSeen(identity);
      return;
    }
    WeixinLinkClient.sendText(this, account, sender, message.optString("context_token"), pending);
    savePendingReply(identity, ""); markSeen(identity);
    prefs.edit().putLong("weixin_link_last_message_at", System.currentTimeMillis()).apply();
  }

  private String cardName(String cardId) {
    try (AppDb db = new AppDb(this)) {
      JSONArray cards = new JSONObject(db.loadState()).optJSONArray("cards");
      if (cards != null) for (int i = 0; i < cards.length(); i++) {
        JSONObject card = cards.optJSONObject(i);
        if (card != null && cardId.equals(card.optString("id"))) return card.optString("name", "角色");
      }
    } catch (Exception ignored) {}
    return "角色";
  }

  private boolean senderAllowed(WeixinLinkClient.Account account, String sender) {
    return prefs.getBoolean("weixin_link_allow_all_private", false) || sender.equals(account.ownerId);
  }

  private String textOf(JSONArray items) {
    if (items == null) return ""; StringBuilder text = new StringBuilder();
    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.optJSONObject(i); if (item == null || item.optInt("type") != 1) continue;
      JSONObject textItem = item.optJSONObject("text_item"); String value = textItem == null ? "" : textItem.optString("text").trim();
      if (!value.isEmpty()) { if (text.length() > 0) text.append('\n'); text.append(value); }
    }
    return text.toString();
  }

  private boolean seen(String id) { return prefs.getStringSet("weixin_link_seen_ids", java.util.Collections.emptySet()).contains(id); }
  private void markSeen(String id) {
    Set<String> values = new HashSet<>(prefs.getStringSet("weixin_link_seen_ids", java.util.Collections.emptySet())); values.add(id);
    while (values.size() > 160) values.remove(values.iterator().next()); prefs.edit().putStringSet("weixin_link_seen_ids", values).apply();
  }
  private String pendingReply(String id) { try { return new JSONObject(prefs.getString("weixin_link_pending_replies", "{}")).optString(id); } catch (Exception ignored) { return ""; } }
  private void savePendingReply(String id, String reply) { try { JSONObject values = new JSONObject(prefs.getString("weixin_link_pending_replies", "{}")); if (reply.isEmpty()) values.remove(id); else values.put(id, reply); prefs.edit().putString("weixin_link_pending_replies", values.toString()).apply(); } catch (Exception ignored) {} }
  private void updateStatus(String id, String status, String error) { prefs.edit().putString("weixin_link_status_" + id, status).putString("weixin_link_error_" + id, error).apply(); sendBroadcast(new Intent(WeixinLinkClient.ACTION_STATE_CHANGED).setPackage(getPackageName())); }
  private Notification notification(String text) {
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) { NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "微信角色连接", NotificationManager.IMPORTANCE_LOW); channel.setShowBadge(false); manager.createNotificationChannel(channel); }
    PendingIntent pending = PendingIntent.getActivity(this, NOTIFICATION_ID, new Intent(this, ComposeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    return builder.setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle("时遇微信角色").setContentText(text).setContentIntent(pending).setOnlyAlertOnce(true).setOngoing(true).build();
  }
  private static void recordServiceError(Context context, Throwable error) {
    if (MainActivity.isDataRescueRequired(context)) return;
    String detail = error == null ? "微信连接服务启动失败" : error.getClass().getSimpleName()
        + (error.getMessage() == null || error.getMessage().isEmpty() ? "" : ": " + error.getMessage());
    if (detail.length() > 500) detail = detail.substring(0, 500);
    DiagnosticLog.record(context, "weixin", "error", detail);
    try {
      context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE).edit()
          .putString("weixin_link_service_error", detail)
          .putString("weixin_link_status_text", "微信后台连接启动失败，请重新打开连接设置").apply();
      context.sendBroadcast(new Intent(WeixinLinkClient.ACTION_STATE_CHANGED).setPackage(context.getPackageName()));
    } catch (RuntimeException ignored) {}
  }
  private static String message(Throwable error) { String value = error.getMessage(); return value == null || value.isEmpty() ? error.getClass().getSimpleName() : value; }
  @Override public synchronized void onDestroy() {
    stopped = true;
    foregroundReady = false;
    monitorGeneration++;
    if (executor != null) { executor.shutdownNow(); executor = null; }
    CrashProtection.clearFeatureActive(this, CrashProtection.FEATURE_WEIXIN);
    super.onDestroy();
  }
  @Override public IBinder onBind(Intent intent) { return null; }
}
