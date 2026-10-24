package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** Persistent, redacted diagnostics used by opt-in feedback reports. */
public final class DiagnosticLog {
  public static final int MAX_ENTRIES = 20;

  private static final String KEY = "diagnostic_log_v1";
  private static final int MAX_SOURCE_CHARS = 80;
  private static final int MAX_LEVEL_CHARS = 24;
  private static final int MAX_MESSAGE_CHARS = 1_200;
  private static final int MAX_STORED_CHARS = 30_000;
  private static final int MAX_SANITIZE_INPUT_CHARS = 16_000;
  private static final Object LOCK = new Object();

  private static final Pattern BEARER = Pattern.compile(
      "(?i)(\\bBearer\\s+)[A-Za-z0-9._~+/=-]+");
  private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
      "(?i)(\\b(?:Proxy-)?Authorization\\s*[:=]\\s*)(?:[A-Za-z][A-Za-z0-9._-]*\\s+)?[^\\s,;]+");
  private static final Pattern KNOWN_TOKEN = Pattern.compile(
      "(?i)\\b(?:sk-[A-Za-z0-9_-]{8,}|gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,})\\b");
  private static final Pattern URL_CREDENTIALS = Pattern.compile(
      "(?i)(https?://[^\\s/:@]+:)[^\\s/@]+(@)");
  private static final Pattern QUERY_SECRET = Pattern.compile(
      "(?i)([?&](?:api[_-]?key|key|token|access[_-]?token|refresh[_-]?token|secret|app[_-]?secret|client[_-]?secret|password|passwd)=)[^&\\s#]+");
  private static final Pattern JSON_SECRET = Pattern.compile(
      "(?i)(\"(?:api[_ -]?key|x-api-key|authorization|access[_ -]?token|refresh[_ -]?token|secret|app[_ -]?secret|appsecret|client[_ -]?secret|password|passwd|cookie)\"\\s*:\\s*\")[^\"]*(\")");
  private static final Pattern LABELED_SECRET = Pattern.compile(
      "(?i)(\\b(?:api[_ -]?key|x-api-key|authorization|access[_ -]?token|refresh[_ -]?token|secret|app[_ -]?secret|appsecret|client[_ -]?secret|password|passwd|cookie)\\b\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\s,;]+)");
  private static final Pattern PRIVATE_KEY = Pattern.compile(
      "(?is)-----BEGIN(?: [A-Z]+)? PRIVATE KEY-----.*?(?:-----END(?: [A-Z]+)? PRIVATE KEY-----|$)");

  private DiagnosticLog() {}

  public static void record(Context context, String source, String level, String message) {
    if (context == null) return;
    String cleanMessage = sanitize(message, MAX_MESSAGE_CHARS);
    if (cleanMessage.isEmpty()) return;
    JSONObject current = new JSONObject();
    try {
      current.put("time", System.currentTimeMillis());
      current.put("source", sanitize(source, MAX_SOURCE_CHARS));
      current.put("level", sanitize(level, MAX_LEVEL_CHARS));
      current.put("message", cleanMessage);
    } catch (Exception ignored) {
      return;
    }

    try {
      synchronized (LOCK) {
      SharedPreferences values = prefs(context);
      JSONArray previous = parse(values.getString(KEY, "[]"));
      JSONArray updated = new JSONArray();
      Set<String> seen = new HashSet<>();
      updated.put(current);
      seen.add(signature(current));
      for (int i = 0; i < previous.length() && updated.length() < MAX_ENTRIES; i++) {
        JSONObject item = normalized(previous.optJSONObject(i));
        if (item == null || !seen.add(signature(item))) continue;
          updated.put(item);
          if (updated.toString().length() > MAX_STORED_CHARS) {
            updated.remove(updated.length() - 1);
            break;
          }
        }
        values.edit().putString(KEY, updated.toString()).commit();
      }
    } catch (RuntimeException ignored) {}
  }

  public static JSONArray recent(Context context) {
    JSONArray result = new JSONArray();
    if (context == null) return result;
    try {
      synchronized (LOCK) {
        JSONArray stored = parse(prefs(context).getString(KEY, "[]"));
        for (int i = 0; i < stored.length() && result.length() < MAX_ENTRIES; i++) {
          JSONObject item = normalized(stored.optJSONObject(i));
          if (item == null) continue;
          result.put(item);
          if (result.toString().length() > MAX_STORED_CHARS) {
            result.remove(result.length() - 1);
            break;
          }
        }
      }
    } catch (RuntimeException ignored) {}
    return result;
  }

  static String sanitize(String value, int maxChars) {
    if (value == null || maxChars <= 0) return "";
    String clean = value.length() <= MAX_SANITIZE_INPUT_CHARS
        ? value : value.substring(0, MAX_SANITIZE_INPUT_CHARS);
    clean = clean.replace("\u0000", "").trim();
    clean = PRIVATE_KEY.matcher(clean).replaceAll("[PRIVATE KEY REDACTED]");
    clean = AUTHORIZATION_HEADER.matcher(clean).replaceAll("$1***");
    clean = BEARER.matcher(clean).replaceAll("$1***");
    clean = KNOWN_TOKEN.matcher(clean).replaceAll("***");
    clean = URL_CREDENTIALS.matcher(clean).replaceAll("$1***$2");
    clean = QUERY_SECRET.matcher(clean).replaceAll("$1***");
    clean = JSON_SECRET.matcher(clean).replaceAll("$1***$2");
    clean = LABELED_SECRET.matcher(clean).replaceAll("$1***");
    return clean.length() <= maxChars ? clean : clean.substring(0, maxChars).trim() + "...";
  }

  private static JSONObject normalized(JSONObject value) {
    if (value == null) return null;
    String message = sanitize(value.optString("message", ""), MAX_MESSAGE_CHARS);
    if (message.isEmpty()) return null;
    try {
      return new JSONObject()
          .put("time", Math.max(0L, value.optLong("time", 0L)))
          .put("source", sanitize(value.optString("source", ""), MAX_SOURCE_CHARS))
          .put("level", sanitize(value.optString("level", ""), MAX_LEVEL_CHARS))
          .put("message", message);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static JSONArray parse(String value) {
    try {
      return new JSONArray(value == null ? "[]" : value);
    } catch (Exception ignored) {
      return new JSONArray();
    }
  }

  private static String signature(JSONObject value) {
    return value.optString("source") + '\n' + value.optString("level") + '\n'
        + value.optString("message");
  }

  private static SharedPreferences prefs(Context context) {
    Context app = context.getApplicationContext();
    return (app == null ? context : app)
        .getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
  }
}
