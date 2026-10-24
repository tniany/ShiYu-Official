package com.tniany.shiyu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class VoiceCallOverlayService extends Service {
  public static final String EXTRA_COMMAND = "voice_call_command";
  public static final String ACTION_COMMAND = "com.tniany.shiyu.voice_call.COMMAND";
  public static final String COMMAND_REOPEN = "reopen";
  public static final String COMMAND_MIC = "mic";
  public static final String COMMAND_SPEAKER = "speaker";
  public static final String COMMAND_SCREEN = "screen";
  public static final String COMMAND_END = "end";

  private static final String ACTION_SHOW = "com.tniany.shiyu.voice_call.SHOW";
  private static final String ACTION_STOP = "com.tniany.shiyu.voice_call.STOP";
  private static final String CHANNEL_ID = "voice_call_overlay";
  private static final int NOTIFICATION_ID = 7051;

  private WindowManager windowManager;
  private LinearLayout overlay;
  private WindowManager.LayoutParams params;
  private TextView title;
  private TextView status;
  private ImageButton micButton;
  private ImageButton speakerButton;
  private ImageButton screenButton;
  private boolean micMuted;
  private boolean speakerEnabled;
  private boolean sharing;
  private boolean visualReady;
  private String chatId = "";

  public static void show(Context context, String roleName, String callStatus, boolean muted,
      boolean speaker, boolean screenSharing, boolean canSeeScreen, String activeChatId) {
    if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) return;
    Intent intent = new Intent(context, VoiceCallOverlayService.class)
        .setAction(ACTION_SHOW)
        .putExtra("role_name", roleName)
        .putExtra("call_status", callStatus)
        .putExtra("mic_muted", muted)
        .putExtra("speaker_enabled", speaker)
        .putExtra("screen_sharing", screenSharing)
        .putExtra("visual_ready", canSeeScreen)
        .putExtra("chat_id", activeChatId);
    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
    else context.startService(intent);
  }

  public static void hide(Context context) {
    context.stopService(new Intent(context, VoiceCallOverlayService.class).setAction(ACTION_STOP));
  }

  @Override public void onCreate() {
    super.onCreate();
    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    startForeground(NOTIFICATION_ID, notification());
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      stopSelf();
      return START_NOT_STICKY;
    }
    if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
      stopSelf();
      return START_NOT_STICKY;
    }
    if (intent != null) {
      micMuted = intent.getBooleanExtra("mic_muted", micMuted);
      speakerEnabled = intent.getBooleanExtra("speaker_enabled", speakerEnabled);
      sharing = intent.getBooleanExtra("screen_sharing", sharing);
      visualReady = intent.getBooleanExtra("visual_ready", visualReady);
      chatId = intent.getStringExtra("chat_id") == null ? chatId : intent.getStringExtra("chat_id");
      ensureOverlay();
      title.setText(intent.getStringExtra("role_name") == null ? "AI" : intent.getStringExtra("role_name"));
      status.setText(displayStatus(intent.getStringExtra("call_status")));
      updateButtons();
    }
    return START_NOT_STICKY;
  }

  private String displayStatus(String value) {
    if (sharing && !visualReady) return "\u5c4f\u5e55\u5171\u4eab\u4e2d\uff0c\u9700\u914d\u7f6e\u89c6\u89c9\u6a21\u578b";
    if (sharing) return "\u5c4f\u5e55\u5171\u4eab\u4e2d";
    return value == null || value.trim().isEmpty() ? "\u901a\u8bdd\u4e2d" : value;
  }

  private void ensureOverlay() {
    if (overlay != null) return;
    overlay = new LinearLayout(this);
    overlay.setOrientation(LinearLayout.VERTICAL);
    overlay.setPadding(dp(12), dp(9), dp(10), dp(9));
    GradientDrawable background = new GradientDrawable();
    background.setColor(Color.rgb(37, 38, 36));
    background.setCornerRadius(dp(8));
    background.setStroke(dp(1), Color.argb(90, 255, 255, 255));
    overlay.setBackground(background);
    overlay.setElevation(dp(10));

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);
    title = new TextView(this);
    title.setTextColor(Color.WHITE);
    title.setTextSize(14);
    title.setSingleLine(true);
    title.setMaxWidth(dp(116));
    title.setEllipsize(android.text.TextUtils.TruncateAt.END);
    status = new TextView(this);
    status.setTextColor(Color.rgb(204, 211, 205));
    status.setTextSize(11);
    status.setSingleLine(true);
    status.setEllipsize(android.text.TextUtils.TruncateAt.END);
    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(28), 0.42f);
    LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(0, dp(28), 0.58f);
    header.addView(title, titleParams);
    header.addView(status, statusParams);
    header.setOnClickListener(v -> openActivity(COMMAND_REOPEN));
    overlay.addView(header, new LinearLayout.LayoutParams(dp(292), dp(28)));

    LinearLayout controls = new LinearLayout(this);
    controls.setOrientation(LinearLayout.HORIZONTAL);
    controls.setGravity(Gravity.CENTER);
    micButton = control(android.R.drawable.ic_btn_speak_now, "\u9ea6\u514b\u98ce", COMMAND_MIC);
    speakerButton = control(android.R.drawable.ic_lock_silent_mode_off, "\u626c\u58f0\u5668", COMMAND_SPEAKER);
    screenButton = control(android.R.drawable.ic_menu_view, "\u5171\u4eab\u5c4f\u5e55", COMMAND_SCREEN);
    controls.addView(micButton);
    controls.addView(speakerButton);
    controls.addView(screenButton);
    controls.addView(control(android.R.drawable.ic_menu_call, "\u8fd4\u56de\u901a\u8bdd", COMMAND_REOPEN));
    ImageButton end = control(android.R.drawable.ic_menu_close_clear_cancel, "\u6302\u65ad", COMMAND_END);
    end.setColorFilter(Color.rgb(255, 111, 111));
    controls.addView(end);
    overlay.addView(controls, new LinearLayout.LayoutParams(dp(292), dp(45)));

    params = new WindowManager.LayoutParams(dp(316), dp(91), overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.TOP | Gravity.END;
    params.x = dp(10);
    params.y = dp(88);
    installDrag(header);
    windowManager.addView(overlay, params);
  }

  private ImageButton control(int icon, String description, String command) {
    ImageButton button = new ImageButton(this);
    button.setImageResource(icon);
    button.setColorFilter(Color.WHITE);
    button.setContentDescription(description);
    button.setBackgroundColor(Color.TRANSPARENT);
    button.setPadding(dp(10), dp(10), dp(10), dp(10));
    button.setOnClickListener(v -> openActivity(command));
    button.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(44)));
    return button;
  }

  private void updateButtons() {
    if (micButton == null) return;
    micButton.setAlpha(micMuted ? 0.45f : 1f);
    speakerButton.setAlpha(speakerEnabled ? 1f : 0.45f);
    screenButton.setColorFilter(sharing ? Color.rgb(93, 219, 155) : Color.WHITE);
  }

  private void installDrag(View handle) {
    handle.setOnTouchListener(new View.OnTouchListener() {
      private int startX, startY;
      private float downX, downY;
      private boolean moved;
      @Override public boolean onTouch(View view, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
          startX = params.x; startY = params.y; downX = event.getRawX(); downY = event.getRawY(); moved = false;
          return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
          int dx = Math.round(downX - event.getRawX());
          int dy = Math.round(event.getRawY() - downY);
          moved = moved || Math.abs(dx) + Math.abs(dy) > dp(5);
          params.x = Math.max(0, startX + dx);
          params.y = Math.max(0, startY + dy);
          windowManager.updateViewLayout(overlay, params);
          return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
          if (!moved) openActivity(COMMAND_REOPEN);
          return true;
        }
        return false;
      }
    });
  }

  private void openActivity(String command) {
    if (!COMMAND_REOPEN.equals(command) && !(COMMAND_SCREEN.equals(command) && !sharing)) {
      sendBroadcast(new Intent(ACTION_COMMAND).setPackage(getPackageName()).putExtra(EXTRA_COMMAND, command));
      if (COMMAND_END.equals(command)) stopSelf();
      return;
    }
    Intent open = new Intent(this, ComposeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(EXTRA_COMMAND, command)
        .putExtra("open_chat_id", chatId);
    startActivity(open);
  }

  private Notification notification() {
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "\u8bed\u97f3\u901a\u8bdd", NotificationManager.IMPORTANCE_LOW);
      channel.setShowBadge(false);
      manager.createNotificationChannel(channel);
    }
    Intent open = new Intent(this, ComposeActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(EXTRA_COMMAND, COMMAND_REOPEN);
    PendingIntent pending = PendingIntent.getActivity(this, 7051, open,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    return builder.setSmallIcon(android.R.drawable.ic_menu_call)
        .setContentTitle("\u65f6\u9047\u8bed\u97f3\u901a\u8bdd\u8fdb\u884c\u4e2d")
        .setContentText("\u70b9\u51fb\u8fd4\u56de\u901a\u8bdd")
        .setContentIntent(pending).setOngoing(true).setOnlyAlertOnce(true).build();
  }

  private int overlayType() {
    return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        : WindowManager.LayoutParams.TYPE_PHONE;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  @Override public void onDestroy() {
    if (overlay != null) {
      try { windowManager.removeView(overlay); } catch (Exception ignored) {}
      overlay = null;
    }
    stopForeground(true);
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) { return null; }
}
