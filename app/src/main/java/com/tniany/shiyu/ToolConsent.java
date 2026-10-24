package com.tniany.shiyu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class ToolConsent {
  private ToolConsent() {}

  static boolean confirm(Context context, String service, String tool, String arguments) {
    if (!(context instanceof Activity) || Looper.myLooper() == Looper.getMainLooper()) return false;
    Activity activity = (Activity) context;
    if (!foreground(activity)) return false;
    CountDownLatch answer = new CountDownLatch(1);
    AtomicBoolean approved = new AtomicBoolean(false);
    AtomicBoolean expired = new AtomicBoolean(false);
    AtomicReference<AlertDialog> dialog = new AtomicReference<>();
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(() -> {
      if (expired.get() || !foreground(activity)) { answer.countDown(); return; }
      String details = arguments == null ? "{}" : arguments;
      if (details.length() > 2000) details = details.substring(0, 2000) + "…";
      AlertDialog prompt = new AlertDialog.Builder(activity)
          .setTitle("允许这次工具调用？")
          .setMessage(service + "\n工具：" + tool + "\n参数：\n" + details
              + "\n\n参数会发送给该服务。若涉及叫车、取消订单或费用，请核对地点、时间和金额；不确定时请拒绝。")
          .setPositiveButton("确认本次调用", (ignored, which) -> { approved.set(true); answer.countDown(); })
          .setNegativeButton("拒绝", (ignored, which) -> answer.countDown())
          .setOnCancelListener(ignored -> answer.countDown()).create();
      dialog.set(prompt);
      prompt.show();
    });
    try {
      long deadline = System.currentTimeMillis() + 90_000L;
      while (System.currentTimeMillis() < deadline && foreground(activity)) {
        if (answer.await(1, TimeUnit.SECONDS)) return approved.get();
      }
      return false;
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    } finally {
      expired.set(true);
      handler.post(() -> { if (dialog.get() != null) dialog.get().dismiss(); });
    }
  }

  private static boolean foreground(Activity activity) {
    return !activity.isFinishing() && !activity.isDestroyed()
        && activity.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE)
            .getBoolean("app_in_foreground", false);
  }
}
