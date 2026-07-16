package com.tniany.shiyu;

import android.content.SharedPreferences;
import java.io.InterruptedIOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GenerationRetry {
  interface Operation<T> { T run() throws Exception; }

  private GenerationRetry() {}

  static <T> T run(SharedPreferences prefs, Operation<T> operation) throws Exception {
    int retries = prefs.getBoolean("generation_auto_retry", true)
        ? Math.max(1, Math.min(5, prefs.getInt("generation_auto_retry_count", 2))) : 0;
    Exception last = null;
    for (int attempt = 0; attempt <= retries; attempt++) {
      try {
        return operation.run();
      } catch (Exception error) {
        last = error;
        if (attempt >= retries || !shouldRetry(error)) throw error;
        pause(attempt + 1);
      }
    }
    throw last == null ? new IllegalStateException("生成失败") : last;
  }

  static boolean shouldRetry(Exception error) {
    if (error instanceof InterruptedIOException || Thread.currentThread().isInterrupted()) return false;
    String message = error.getMessage() == null ? "" : error.getMessage();
    Matcher status = Pattern.compile("HTTP\\s+(\\d{3})").matcher(message);
    if (!status.find()) return true;
    int code = Integer.parseInt(status.group(1));
    return code == 408 || code == 409 || code == 425 || code == 429 || code >= 500;
  }

  static void pause(int attempt) throws InterruptedIOException {
    try {
      Thread.sleep(Math.min(2500L, 450L * attempt));
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException("stopped");
    }
  }
}
