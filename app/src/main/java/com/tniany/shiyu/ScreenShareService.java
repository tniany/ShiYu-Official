package com.tniany.shiyu;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScreenShareService extends Service {
  public static final String ACTION_STATE = "com.tniany.shiyu.screen_share.STATE";
  public static final String ACTION_FRAME = "com.tniany.shiyu.screen_share.FRAME";
  public static final String EXTRA_ACTIVE = "active";
  public static final String EXTRA_PATH = "path";

  private static final String ACTION_START = "com.tniany.shiyu.screen_share.START";
  private static final String ACTION_STOP = "com.tniany.shiyu.screen_share.STOP";
  private static final String EXTRA_RESULT_CODE = "result_code";
  private static final String EXTRA_RESULT_DATA = "result_data";
  private static final String CHANNEL_ID = "voice_screen_share";
  private static final int NOTIFICATION_ID = 7052;
  private static final long FRAME_INTERVAL_MS = 2500L;

  private final AtomicBoolean writing = new AtomicBoolean(false);
  private HandlerThread captureThread;
  private Handler captureHandler;
  private MediaProjection projection;
  private VirtualDisplay virtualDisplay;
  private ImageReader imageReader;
  private long lastFrameAt;

  public static void start(Context context, int resultCode, Intent resultData) {
    Intent intent = new Intent(context, ScreenShareService.class)
        .setAction(ACTION_START)
        .putExtra(EXTRA_RESULT_CODE, resultCode)
        .putExtra(EXTRA_RESULT_DATA, resultData);
    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
    else context.startService(intent);
  }

  public static void stop(Context context) {
    context.stopService(new Intent(context, ScreenShareService.class));
  }

  @Override public void onCreate() {
    super.onCreate();
    captureThread = new HandlerThread("voice-screen-share");
    captureThread.start();
    captureHandler = new Handler(captureThread.getLooper());
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) return START_NOT_STICKY;
    if (ACTION_STOP.equals(intent.getAction())) {
      stopSelf();
      return START_NOT_STICKY;
    }
    if (!ACTION_START.equals(intent.getAction()) || projection != null) return START_NOT_STICKY;
    startForeground(NOTIFICATION_ID, notification());
    Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
    int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
    if (resultData == null || resultCode != Activity.RESULT_OK) {
      stopSelf();
      return START_NOT_STICKY;
    }
    try {
      MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
      projection = manager.getMediaProjection(resultCode, resultData);
      if (projection == null) throw new IllegalStateException("MediaProjection unavailable");
      projection.registerCallback(new MediaProjection.Callback() {
        @Override public void onStop() { stopSelf(); }
      }, captureHandler);
      startCapture();
      broadcastState(true);
    } catch (Exception error) {
      stopSelf();
    }
    return START_NOT_STICKY;
  }

  private void startCapture() {
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    int sourceWidth = Math.max(1, metrics.widthPixels);
    int sourceHeight = Math.max(1, metrics.heightPixels);
    float scale = Math.min(1f, 1080f / Math.max(sourceWidth, sourceHeight));
    int width = Math.max(2, Math.round(sourceWidth * scale));
    int height = Math.max(2, Math.round(sourceHeight * scale));
    imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
    imageReader.setOnImageAvailableListener(this::onImageAvailable, captureHandler);
    virtualDisplay = projection.createVirtualDisplay("ShiYuVoiceScreen", width, height,
        metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        imageReader.getSurface(), null, captureHandler);
  }

  private void onImageAvailable(ImageReader reader) {
    Image image = null;
    try {
      image = reader.acquireLatestImage();
      if (image == null) return;
      long now = android.os.SystemClock.elapsedRealtime();
      if (now - lastFrameAt < FRAME_INTERVAL_MS || !writing.compareAndSet(false, true)) return;
      lastFrameAt = now;
      Image.Plane plane = image.getPlanes()[0];
      int pixelStride = plane.getPixelStride();
      int rowStride = plane.getRowStride();
      int rowPadding = rowStride - pixelStride * image.getWidth();
      Bitmap padded = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride,
          image.getHeight(), Bitmap.Config.ARGB_8888);
      padded.copyPixelsFromBuffer(plane.getBuffer());
      Bitmap frame = Bitmap.createBitmap(padded, 0, 0, image.getWidth(), image.getHeight());
      if (frame != padded) padded.recycle();
      writeFrame(frame);
    } catch (Exception ignored) {
      writing.set(false);
    } finally {
      if (image != null) image.close();
    }
  }

  private void writeFrame(Bitmap frame) {
    try {
      File directory = new File(getFilesDir(), "voice_screen_share");
      if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Cannot create capture directory");
      File temp = new File(directory, "latest.tmp");
      File target = new File(directory, "latest.jpg");
      try (FileOutputStream output = new FileOutputStream(temp)) {
        frame.compress(Bitmap.CompressFormat.JPEG, 76, output);
      }
      if (target.exists()) target.delete();
      if (!temp.renameTo(target)) throw new IllegalStateException("Cannot publish capture");
      Intent update = new Intent(ACTION_FRAME).setPackage(getPackageName()).putExtra(EXTRA_PATH, target.getAbsolutePath());
      sendBroadcast(update);
    } catch (Exception ignored) {
    } finally {
      frame.recycle();
      writing.set(false);
    }
  }

  private Notification notification() {
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "\u5c4f\u5e55\u5171\u4eab", NotificationManager.IMPORTANCE_LOW);
      channel.setShowBadge(false);
      manager.createNotificationChannel(channel);
    }
    Intent stop = new Intent(this, ScreenShareService.class).setAction(ACTION_STOP);
    PendingIntent stopPending = PendingIntent.getService(this, 7052, stop,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    return builder.setSmallIcon(android.R.drawable.ic_menu_view)
        .setContentTitle("\u6b63\u5728\u5171\u4eab\u5c4f\u5e55")
        .setContentText("\u65f6\u9047\u4f1a\u5c06\u5f53\u524d\u753b\u9762\u4ea4\u7ed9\u5df2\u914d\u7f6e\u7684\u89c6\u89c9\u6a21\u578b")
        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "\u505c\u6b62\u5171\u4eab", stopPending)
        .setOngoing(true).setOnlyAlertOnce(true).build();
  }

  private void broadcastState(boolean active) {
    sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName()).putExtra(EXTRA_ACTIVE, active));
  }

  @Override public void onDestroy() {
    broadcastState(false);
    if (imageReader != null) {
      imageReader.setOnImageAvailableListener(null, null);
      imageReader.close();
      imageReader = null;
    }
    if (virtualDisplay != null) {
      virtualDisplay.release();
      virtualDisplay = null;
    }
    if (projection != null) {
      projection.stop();
      projection = null;
    }
    if (captureThread != null) {
      captureThread.quitSafely();
      captureThread = null;
    }
    stopForeground(true);
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) { return null; }
}
