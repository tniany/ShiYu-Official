package com.tniany.shiyu;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** Records fatal process errors and disables a recognized feature before it can crash-loop. */
public final class CrashProtection {
  public static final String PREF_ENABLED = "experimental_crash_protection_enabled";
  public static final String FEATURE_WEIXIN = "weixin_link";
  public static final String FEATURE_QQBOT = "qqbot";
  public static final String FEATURE_DESKTOP_PET = "desktop_pet";
  public static final String FEATURE_SCREEN_CONTEXT = "screen_context";
  public static final String FEATURE_ACTIVE_REPLIES = "active_replies";
  public static final String FEATURE_MOMENT_AUTO = "moment_auto";
  public static final String FEATURE_NOTIFICATION_KEEP_ALIVE = "notification_keep_alive";

  private static final String FEATURE_UNKNOWN = "unknown";
  private static final String KEY_HISTORY = "crash_protection_history";
  private static final String KEY_LAST_CAPTURE_AT = "crash_protection_last_capture_at";
  private static final String KEY_LAST_EXIT_CHECKED = "crash_protection_last_exit_checked";
  private static final String KEY_ACTIVE_FEATURE = "crash_protection_active_feature";
  private static final String KEY_ACTIVE_AT = "crash_protection_active_at";
  private static final String KEY_RECOVERY_PENDING = "crash_protection_recovery_pending";
  private static final String KEY_NOTICE = "crash_protection_notice";
  private static final int MAX_HISTORY = 20;
  private static final long EXIT_MATCH_WINDOW_MS = 60_000L;
  private static final AtomicBoolean INSTALLED = new AtomicBoolean();
  private static final AtomicBoolean HANDLING = new AtomicBoolean();
  private static volatile String lastMarkedFeature = "";
  private static volatile long lastMarkedAt;

  public static final class Record {
    public final long timestamp;
    public final String featureId;
    public final String featureName;
    public final String reason;
    public final String action;
    public final String thread;
    public final String location;
    public final String source;

    private Record(JSONObject value) {
      timestamp = value.optLong("timestamp");
      featureId = value.optString("feature", FEATURE_UNKNOWN);
      featureName = value.optString("featureName", featureName(featureId));
      reason = value.optString("reason", "未知异常");
      action = value.optString("action", "已记录");
      thread = value.optString("thread", "unknown");
      location = value.optString("location", "");
      source = value.optString("source", "java");
    }
  }

  private CrashProtection() {}

  public static void install(Context context) {
    Context app = app(context);
    if (!INSTALLED.compareAndSet(false, true)) return;
    inspectPreviousExit(app);
    enforcePendingRecovery(app);
    Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
      if (HANDLING.compareAndSet(false, true)) {
        try {
          if (isEnabled(app)) capture(app, thread, error);
        } catch (Throwable ignored) {
          // A crash handler must always yield to Android's original process terminator.
        }
      }
      if (previous != null) {
        previous.uncaughtException(thread, error);
      } else {
        Process.killProcess(Process.myPid());
        System.exit(10);
      }
    });
  }

  public static boolean isEnabled(Context context) {
    return prefs(context).getBoolean(PREF_ENABLED, true);
  }

  public static void setEnabled(Context context, boolean enabled) {
    prefs(context).edit().putBoolean(PREF_ENABLED, enabled).apply();
  }

  /** Marks the most recently executing background feature for native-crash recovery. */
  public static void markFeatureActive(Context context, String feature) {
    if (!isEnabled(context) || feature == null || feature.isEmpty()) return;
    long now = System.currentTimeMillis();
    if (feature.equals(lastMarkedFeature) && now - lastMarkedAt < 10_000L) return;
    lastMarkedFeature = feature;
    lastMarkedAt = now;
    prefs(context).edit().putString(KEY_ACTIVE_FEATURE, feature).putLong(KEY_ACTIVE_AT, now).apply();
  }

  public static void clearFeatureActive(Context context, String feature) {
    SharedPreferences values = prefs(context);
    if (!feature.equals(values.getString(KEY_ACTIVE_FEATURE, ""))) return;
    lastMarkedFeature = "";
    lastMarkedAt = 0L;
    values.edit().remove(KEY_ACTIVE_FEATURE).remove(KEY_ACTIVE_AT).apply();
  }

  public static ArrayList<Record> records(Context context) {
    ArrayList<Record> result = new ArrayList<>();
    try {
      JSONArray history = new JSONArray(prefs(context).getString(KEY_HISTORY, "[]"));
      for (int i = 0; i < history.length(); i++) {
        JSONObject item = history.optJSONObject(i);
        if (item != null) result.add(new Record(item));
      }
    } catch (Exception ignored) {}
    return result;
  }

  public static void clearRecords(Context context) {
    prefs(context).edit().remove(KEY_HISTORY).remove(KEY_NOTICE).apply();
  }

  public static String consumeNotice(Context context) {
    SharedPreferences values = prefs(context);
    String notice = values.getString(KEY_NOTICE, "");
    if (!notice.isEmpty()) values.edit().remove(KEY_NOTICE).apply();
    return notice;
  }

  private static void capture(Context context, Thread thread, Throwable error) {
    String feature = detectFeature(error);
    String reason = describe(error);
    String location = appLocation(error);
    record(context, System.currentTimeMillis(), feature, reason,
        thread == null ? "unknown" : clean(thread.getName(), 80), location, "java");
  }

  private static void inspectPreviousExit(Context context) {
    if (Build.VERSION.SDK_INT < 30) return;
    SharedPreferences values = prefs(context);
    try {
      ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      List<ApplicationExitInfo> exits = manager == null ? Collections.emptyList()
          : manager.getHistoricalProcessExitReasons(context.getPackageName(), 0, 1);
      if (exits.isEmpty()) return;
      ApplicationExitInfo exit = exits.get(0);
      long timestamp = exit.getTimestamp();
      if (timestamp <= values.getLong(KEY_LAST_EXIT_CHECKED, 0L)) return;
      values.edit().putLong(KEY_LAST_EXIT_CHECKED, timestamp).commit();
      if (!isEnabled(context)) return;
      int reason = exit.getReason();
      if (reason != ApplicationExitInfo.REASON_CRASH
          && reason != ApplicationExitInfo.REASON_CRASH_NATIVE) return;
      if (Math.abs(timestamp - values.getLong(KEY_LAST_CAPTURE_AT, 0L)) <= 15_000L) return;

      String feature = FEATURE_UNKNOWN;
      int importance = exit.getImportance();
      boolean foreground = importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
          || importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
      long activeAt = values.getLong(KEY_ACTIVE_AT, 0L);
      if (!foreground && Math.abs(timestamp - activeAt) <= EXIT_MATCH_WINDOW_MS)
        feature = values.getString(KEY_ACTIVE_FEATURE, FEATURE_UNKNOWN);
      String type = reason == ApplicationExitInfo.REASON_CRASH_NATIVE ? "原生组件崩溃" : "系统记录的应用崩溃";
      record(context, timestamp, feature, type, "system", "", "system_exit");
    } catch (RuntimeException ignored) {}
  }

  private static void record(Context context, long timestamp, String feature, String reason,
      String thread, String location, String source) {
    SharedPreferences values = prefs(context);
    String resolvedFeature = feature == null || feature.isEmpty() ? FEATURE_UNKNOWN : feature;
    String action = actionFor(resolvedFeature);
    boolean disabled = disableFeature(values, resolvedFeature);
    if (!disabled) action = "未能确定具体功能，已保留当前设置并记录异常";

    try {
      JSONObject item = new JSONObject()
          .put("timestamp", timestamp)
          .put("feature", resolvedFeature)
          .put("featureName", featureName(resolvedFeature))
          .put("reason", clean(reason, 500))
          .put("action", action)
          .put("thread", clean(thread, 80))
          .put("location", clean(location, 240))
          .put("source", source);
      JSONArray previous;
      try { previous = new JSONArray(values.getString(KEY_HISTORY, "[]")); }
      catch (Exception ignored) { previous = new JSONArray(); }
      JSONArray history = new JSONArray().put(item);
      for (int i = 0; i < Math.min(previous.length(), MAX_HISTORY - 1); i++) {
        JSONObject old = previous.optJSONObject(i);
        if (old != null) history.put(old);
      }
      DiagnosticLog.record(context, "crash_protection", "error",
          resolvedFeature + ": " + reason + (location.isEmpty() ? "" : " @ " + location));
      SharedPreferences.Editor editor = values.edit()
          .putString(KEY_HISTORY, history.toString())
          .putLong(KEY_LAST_CAPTURE_AT, timestamp)
          .putString(KEY_NOTICE, disabled
              ? "闪退保护已暂停“" + featureName(resolvedFeature) + "”，详情可在实验性功能中查看"
              : "已记录一次闪退，详情可在实验性功能中查看");
      if (disabled) editor.putString(KEY_RECOVERY_PENDING, resolvedFeature);
      editor.commit();
    } catch (Exception ignored) {}
  }

  private static boolean disableFeature(SharedPreferences values, String feature) {
    SharedPreferences.Editor editor = values.edit();
    boolean disabled = true;
    try {
      switch (feature) {
        case FEATURE_WEIXIN:
          JSONArray accounts = new JSONArray(values.getString("weixin_link_accounts", "[]"));
          for (int i = 0; i < accounts.length(); i++) {
            JSONObject account = accounts.optJSONObject(i);
            if (account != null) account.put("enabled", false);
          }
          editor.putString("weixin_link_accounts", accounts.toString())
              .putString("weixin_link_status_text", "闪退保护已暂停微信连接");
          break;
        case FEATURE_QQBOT:
          JSONArray qqBots = new JSONArray(values.getString("qqbot_accounts", "[]"));
          for (int i = 0; i < qqBots.length(); i++) {
            JSONObject bot = qqBots.optJSONObject(i);
            if (bot != null) bot.put("enabled", false);
          }
          editor.putString("qqbot_accounts", qqBots.toString())
              .putString("qqbot_service_error", "闪退保护已暂停 QQ Bot 连接");
          break;
        case FEATURE_DESKTOP_PET:
          editor.putBoolean("experimental_desktop_pet_enabled", false)
              .putString("desktop_pet_last_error", "闪退保护已暂停桌宠模式");
          break;
        case FEATURE_SCREEN_CONTEXT:
          editor.putBoolean("experimental_screen_context_enabled", false);
          break;
        case FEATURE_ACTIVE_REPLIES:
          editor.putBoolean("active_replies", false);
          break;
        case FEATURE_MOMENT_AUTO:
          Set<String> autoRoles = new HashSet<>(values.getStringSet(
              "moments_auto_roles", Collections.emptySet()));
          Set<String> commentRoles = new HashSet<>(values.getStringSet(
              "moments_comment_roles", Collections.emptySet()));
          if (!autoRoles.isEmpty())
            editor.putStringSet("crash_protection_moment_auto_roles_backup", autoRoles);
          if (!commentRoles.isEmpty())
            editor.putStringSet("crash_protection_moment_comment_roles_backup", commentRoles);
          editor.putStringSet("moments_auto_roles", Collections.emptySet())
              .putStringSet("moments_comment_roles", Collections.emptySet());
          break;
        case FEATURE_NOTIFICATION_KEEP_ALIVE:
          editor.putBoolean("notification_keep_alive", false);
          break;
        default:
          disabled = false;
      }
      if (disabled && !editor.commit()) disabled = false;
    } catch (Exception ignored) {
      disabled = false;
    }
    return disabled;
  }

  private static void enforcePendingRecovery(Context context) {
    SharedPreferences values = prefs(context);
    String feature = values.getString(KEY_RECOVERY_PENDING, "");
    if (feature.isEmpty()) return;
    try {
      switch (feature) {
        case FEATURE_WEIXIN:
          context.stopService(new Intent(context, WeixinLinkService.class));
          break;
        case FEATURE_QQBOT:
          context.stopService(new Intent(context, QqBotService.class));
          break;
        case FEATURE_DESKTOP_PET:
          context.stopService(new Intent(context, DesktopPetService.class));
          break;
        case FEATURE_ACTIVE_REPLIES:
          context.stopService(new Intent(context, YandereMessageService.class));
          ActiveReplyWorker.sync(context);
          break;
        case FEATURE_MOMENT_AUTO:
          MomentAutoPostWorker.sync(context);
          break;
        case FEATURE_NOTIFICATION_KEEP_ALIVE:
          context.stopService(new Intent(context, NotificationKeepAliveService.class));
          break;
        default:
          break;
      }
    } catch (RuntimeException ignored) {
      return;
    }
    values.edit().remove(KEY_RECOVERY_PENDING).apply();
  }

  private static String detectFeature(Throwable error) {
    Throwable current = error;
    while (current != null) {
      for (StackTraceElement frame : current.getStackTrace()) {
        String name = frame.getClassName();
        if (name.contains("ScreenContextAccessibilityService")) return FEATURE_SCREEN_CONTEXT;
        if (name.contains("WeixinLink")) return FEATURE_WEIXIN;
        if (name.contains("QqBot")) return FEATURE_QQBOT;
        if (name.contains("DesktopPet") || name.contains("LegacyGifView")
            || name.contains("Live2DCoreDownloadService")) return FEATURE_DESKTOP_PET;
        if (name.contains("MomentAutoPostWorker")) return FEATURE_MOMENT_AUTO;
        if (name.contains("ActiveReplyWorker") || name.contains("YandereMessageService"))
          return FEATURE_ACTIVE_REPLIES;
        if (name.contains("NotificationKeepAliveService")) return FEATURE_NOTIFICATION_KEEP_ALIVE;
      }
      current = current.getCause();
    }
    return FEATURE_UNKNOWN;
  }

  private static String describe(Throwable error) {
    if (error == null) return "未知异常";
    String message = error.getMessage();
    if ((message == null || message.trim().isEmpty()) && error.getCause() != null)
      message = error.getCause().getMessage();
    return error.getClass().getSimpleName()
        + (message == null || message.trim().isEmpty() ? "" : ": " + message.trim());
  }

  private static String appLocation(Throwable error) {
    Throwable current = error;
    while (current != null) {
      for (StackTraceElement frame : current.getStackTrace()) {
        if (frame.getClassName().startsWith("com.tniany.shiyu."))
          return frame.getClassName().substring("com.tniany.shiyu.".length()) + "."
              + frame.getMethodName() + ":" + frame.getLineNumber();
      }
      current = current.getCause();
    }
    return "";
  }

  private static String featureName(String feature) {
    switch (feature) {
      case FEATURE_WEIXIN: return "微信角色连接";
      case FEATURE_QQBOT: return "QQ Bot 连接";
      case FEATURE_DESKTOP_PET: return "桌宠模式";
      case FEATURE_SCREEN_CONTEXT: return "桌宠屏幕分析";
      case FEATURE_ACTIVE_REPLIES: return "主动消息与病娇模式";
      case FEATURE_MOMENT_AUTO: return "朋友圈自动互动";
      case FEATURE_NOTIFICATION_KEEP_ALIVE: return "通知保活";
      default: return "未识别功能";
    }
  }

  private static String actionFor(String feature) {
    switch (feature) {
      case FEATURE_WEIXIN: return "已暂停全部微信 Bot，账号与绑定信息仍保留";
      case FEATURE_QQBOT: return "已暂停全部 QQ Bot，配置与密钥仍保留";
      case FEATURE_DESKTOP_PET: return "已关闭桌宠模式";
      case FEATURE_SCREEN_CONTEXT: return "已关闭桌宠屏幕分析";
      case FEATURE_ACTIVE_REPLIES: return "已关闭主动消息总开关并停止后台任务";
      case FEATURE_MOMENT_AUTO: return "已停止朋友圈自动发布与自动互动";
      case FEATURE_NOTIFICATION_KEEP_ALIVE: return "已关闭通知保活服务";
      default: return "已记录异常";
    }
  }

  private static String clean(String value, int limit) {
    if (value == null) return "";
    String cleaned = value.replace('\r', ' ').replace('\n', ' ').trim();
    return cleaned.length() <= limit ? cleaned : cleaned.substring(0, limit);
  }

  private static Context app(Context context) {
    Context app = context.getApplicationContext();
    return app == null ? context : app;
  }

  private static SharedPreferences prefs(Context context) {
    return app(context).getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
  }
}
