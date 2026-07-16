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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public final class YandereMessageService extends Service {
  private static final int NOTIFICATION_ID = 5701;
  private ScheduledExecutorService timer;

  static void sync(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    boolean enabled = prefs.getBoolean("active_replies", false) && hasEnabledChat(context);
    Intent intent = new Intent(context, YandereMessageService.class);
    if (enabled) {
      if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
      else context.startService(intent);
    } else context.stopService(intent);
  }

  private static boolean hasEnabledChat(Context context) {
    try {
      AppDb db = new AppDb(context);
      String state = db.loadState();
      db.close();
      if (state.isEmpty()) return false;
      JSONArray chats = new JSONObject(state).optJSONArray("chats");
      if (chats == null) return false;
      for (int i = 0; i < chats.length(); i++) if (chats.optJSONObject(i).optBoolean("yandereEnabled")) return true;
    } catch (Exception ignored) {}
    return false;
  }

  @Override public void onCreate() {
    super.onCreate();
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26)
      manager.createNotificationChannel(new NotificationChannel("yandere_mode", "病娇模式后台运行", NotificationManager.IMPORTANCE_LOW));
    Intent open = new Intent(this, ComposeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pending = PendingIntent.getActivity(this, NOTIFICATION_ID, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "yandere_mode") : new Notification.Builder(this);
    builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("时遇正在守候消息")
        .setContentText("病娇模式已启用，点击返回时遇").setContentIntent(pending).setOngoing(true);
    startForeground(NOTIFICATION_ID, builder.build());
    timer = Executors.newSingleThreadScheduledExecutor();
    timer.scheduleWithFixedDelay(() -> ActiveReplyWorker.runYandere(this), 1, 1, TimeUnit.MINUTES);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
  @Override public void onDestroy() { if (timer != null) timer.shutdownNow(); super.onDestroy(); }
  @Override public IBinder onBind(Intent intent) { return null; }
}
