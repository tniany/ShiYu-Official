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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Live2DCoreDownloadService extends Service {
  static final String ACTION_STATE_CHANGED = "com.tniany.shiyu.LIVE2D_CORE_DOWNLOAD_STATE";
  private static final String CHANNEL_ID = "live2d_core_download";
  private static final int NOTIFICATION_ID = 7055;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private SharedPreferences prefs;

  static void start(Context context) {
    Intent intent = new Intent(context, Live2DCoreDownloadService.class);
    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
    else context.startService(intent);
  }

  static boolean running(Context context) {
    return context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE)
        .getBoolean("live2d_core_download_running", false);
  }

  @Override public void onCreate() {
    super.onCreate();
    prefs = getSharedPreferences(MainActivity.PREF, MODE_PRIVATE);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (prefs.getBoolean("live2d_core_download_running", false)) return START_NOT_STICKY;
    update(1, "准备下载", "", true);
    startForeground(NOTIFICATION_ID, notification(1, "准备下载"));
    executor.execute(() -> {
      try {
        Live2DRepository.downloadRuntime(this, (value, label) -> update(value, label, "", true));
        update(100, "Cubism Core 已就绪", "", false);
        DesktopPetService.sync(this);
        notifyComplete("Core 下载并安装完成");
      } catch (Exception error) {
        String detail = error.getMessage() == null ? error.toString() : error.getMessage();
        update(0, "下载失败", detail, false);
        notifyComplete("Core 下载失败：" + detail);
      } finally {
        stopForeground(false);
        stopSelf();
      }
    });
    return START_NOT_STICKY;
  }

  private void update(int progress, String label, String error, boolean running) {
    prefs.edit().putBoolean("live2d_core_download_running", running)
        .putInt("live2d_core_download_progress", progress)
        .putString("live2d_core_download_label", label)
        .putString("live2d_core_download_error", error).apply();
    if (error != null && !error.trim().isEmpty())
      DiagnosticLog.record(this, "live2d_core_download", "error", error);
    if (running) ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
        .notify(NOTIFICATION_ID, notification(progress, label));
    sendBroadcast(new Intent(ACTION_STATE_CHANGED).setPackage(getPackageName()));
  }

  private Notification notification(int progress, String label) {
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Live2D Core 下载", NotificationManager.IMPORTANCE_LOW);
      channel.setShowBadge(false);
      manager.createNotificationChannel(channel);
    }
    PendingIntent pending = PendingIntent.getActivity(this, NOTIFICATION_ID,
        new Intent(this, ComposeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    return builder.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("正在安装 Live2D Core")
        .setContentText(label).setProgress(100, Math.max(0, Math.min(100, progress)), progress <= 1)
        .setContentIntent(pending).setOnlyAlertOnce(true).setOngoing(true).build();
  }

  private void notifyComplete(String text) {
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,
        builder.setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Live2D Core")
            .setContentText(text).setAutoCancel(true).build());
  }

  @Override public void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
  @Override public IBinder onBind(Intent intent) { return null; }
}
