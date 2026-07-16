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

/** Optional foreground service that keeps background notification delivery more reliable. */
public final class NotificationKeepAliveService extends Service {
  private static final String CHANNEL_ID = "notification_keep_alive";
  private static final int NOTIFICATION_ID = 5702;

  static void sync(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    Intent intent = new Intent(context, NotificationKeepAliveService.class);
    if (prefs.getBoolean("notification_keep_alive", false)) {
      try {
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
      } catch (RuntimeException ignored) {
        // Some vendors reject background starts; the next foreground launch retries.
      }
    } else {
      context.stopService(intent);
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel = new NotificationChannel(
          CHANNEL_ID, "通知保活", NotificationManager.IMPORTANCE_LOW);
      channel.setDescription("提高后台生成完成与角色主动消息的送达稳定性");
      channel.setShowBadge(false);
      manager.createNotificationChannel(channel);
    }
    Intent open = new Intent(this, ComposeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pending = PendingIntent.getActivity(this, NOTIFICATION_ID, open,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("时遇通知服务正在运行")
        .setContentText("后台回复和主动消息会尽量及时送达")
        .setContentIntent(pending).setOngoing(true).setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_SERVICE);
    startForeground(NOTIFICATION_ID, builder.build());
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }
}
