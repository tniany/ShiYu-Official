package com.tniany.shiyu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Periodically lets explicitly authorized roles publish a local moment. */
public final class MomentAutoPostWorker extends JobService {
  private static final int JOB_ID = 0x4d4f4d54;

  static void sync(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    Set<String> roles = new HashSet<>(prefs.getStringSet("moments_auto_roles", java.util.Collections.emptySet()));
    roles.addAll(prefs.getStringSet("moments_comment_roles", java.util.Collections.emptySet()));
    JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (roles == null || roles.isEmpty()) {
      scheduler.cancel(JOB_ID);
      return;
    }
    JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, MomentAutoPostWorker.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15 * 60_000L).setPersisted(true).build();
    scheduler.schedule(job);
  }

  @Override public boolean onStartJob(JobParameters params) {
    new Thread(() -> {
      runOnce(getApplicationContext());
      jobFinished(params, false);
    }, "shiyu-moment-auto-post").start();
    return true;
  }

  @Override public boolean onStopJob(JobParameters params) {
    return true;
  }

  static boolean runForeground(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    long before = prefs.getLong("moment_state_revision", 0);
    runOnce(context.getApplicationContext());
    return prefs.getLong("moment_state_revision", 0) != before;
  }

  private static synchronized void runOnce(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    if (prefs.getLong("active_generation_until", 0) > System.currentTimeMillis()) return;
    Set<String> allowed = new HashSet<>(prefs.getStringSet("moments_auto_roles", java.util.Collections.emptySet()));
    Set<String> commenters = new HashSet<>(prefs.getStringSet("moments_comment_roles", java.util.Collections.emptySet()));
    if (allowed.isEmpty() && commenters.isEmpty()) return;
    String endpoint = prefs.getString("endpoint", "").trim();
    String model = prefs.getString("model", "").trim();
    String key = new SecureStore(context).get();
    boolean apiConfigured = !endpoint.isEmpty() && !model.isEmpty() && !key.isEmpty();
    if (apiConfigured) {
      while (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
      if (!endpoint.endsWith("/chat/completions")) endpoint += "/chat/completions";
    }
    AppDb db = new AppDb(context);
    try {
      JSONObject state = new JSONObject(db.loadState());
      JSONArray cards = state.optJSONArray("cards");
      if (cards == null) return;
      long now = System.currentTimeMillis();
      JSONArray existingMoments = new JSONArray(prefs.getString("moments", "[]"));
      if (existingMoments.length() > 0) for (int i = 0; i < cards.length(); i++) {
        JSONObject card = cards.optJSONObject(i);
        if (card == null || !commenters.contains(card.optString("id"))) continue;
        String id = card.optString("id");
        long next = prefs.getLong("moment_comment_next_" + id, 0);
        if (next == 0) { scheduleCommentNext(prefs, id, now); continue; }
        if (now < next) continue;

        int likeChance = Math.max(0, Math.min(100, prefs.getInt("moments_like_probability", 65)));
        int commentChance = Math.max(0, Math.min(100, prefs.getInt("moments_comment_probability", 35)));
        JSONObject currentCommentRoot = new JSONObject(prefs.getString("moment_comments", "{}"));
        JSONObject currentLikeRoot = new JSONObject(prefs.getString("moment_likes", "{}"));
        ArrayList<JSONObject> candidates = new ArrayList<>();
        for (int j = 0; j < Math.min(30, existingMoments.length()); j++) {
          JSONObject candidate = existingMoments.optJSONObject(j);
          if (candidate == null || id.equals(candidate.optString("authorId"))) continue;
          String candidateId = candidate.optString("id");
          if (candidateId.isEmpty()) continue;
          boolean canLike = likeChance > 0 && !hasLike(currentLikeRoot.optJSONArray(candidateId), id);
          boolean canComment = apiConfigured && commentChance > 0
              && !hasComment(currentCommentRoot.optJSONArray(candidateId), id);
          if (canLike || canComment) candidates.add(candidate);
        }
        Collections.shuffle(candidates);
        if (candidates.isEmpty()) { scheduleCommentNext(prefs, id, now); continue; }
        JSONObject target = candidates.get(0);
        String targetId = target.optString("id");
        int viewChance = Math.max(0, Math.min(100, prefs.getInt("moments_view_probability", 80)));
        if (Math.random() * 100 >= viewChance) {
          scheduleCommentNext(prefs, id, now);
          continue;
        }
        JSONArray values = currentCommentRoot.optJSONArray(targetId);
        if (values == null) values = new JSONArray();
        JSONArray likeValues = currentLikeRoot.optJSONArray(targetId);
        if (likeValues == null) likeValues = new JSONArray();
        boolean liked = !hasLike(likeValues, id) && Math.random() * 100 < likeChance;
        boolean commented = apiConfigured && !hasComment(values, id)
            && Math.random() * 100 < commentChance;
        String comment = "";
        if (commented) comment = AiContentSanitizer.comment(generateComment(endpoint, key, model, prefs, card, target, values));
        if (!containsMoment(new JSONArray(prefs.getString("moments", "[]")), targetId)) {
          scheduleCommentNext(prefs, id, now);
          continue;
        }

        // The user can edit moments while the model request is in flight. Merge into the latest
        // preference value so foreground activity never loses a concurrent comment or like.
        JSONObject commentRoot = new JSONObject(prefs.getString("moment_comments", "{}"));
        JSONObject likeRoot = new JSONObject(prefs.getString("moment_likes", "{}"));
        JSONArray latestComments = commentRoot.optJSONArray(targetId);
        if (latestComments == null) latestComments = new JSONArray();
        JSONArray latestLikes = likeRoot.optJSONArray(targetId);
        if (latestLikes == null) latestLikes = new JSONArray();
        boolean savedLike = liked && !hasLike(latestLikes, id);
        boolean savedComment = commented && !comment.isEmpty() && !hasComment(latestComments, id);
        if (savedLike) {
          latestLikes.put(id);
          likeRoot.put(targetId, latestLikes);
        }
        if (savedComment) {
          latestComments.put(new JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("authorId", id)
              .put("authorName", card.optString("name", "角色")).put("content", comment).put("createdAt", now));
          commentRoot.put(targetId, latestComments);
        }
        SharedPreferences.Editor interaction = prefs.edit()
            .putString("moment_last_view_id_" + id, targetId)
            .putLong("moment_last_view_at_" + id, now)
            .remove("moment_auto_last_error");
        if (savedLike) interaction.putString("moment_likes", likeRoot.toString());
        if (savedComment) interaction.putString("moment_comments", commentRoot.toString());
        if (savedLike || savedComment)
          interaction.putLong("moment_state_revision", prefs.getLong("moment_state_revision", 0) + 1);
        interaction.apply();
        scheduleCommentNext(prefs, id, now);
        if (savedComment || savedLike) {
          String action = savedComment && savedLike ? "comment_like" : savedComment ? "comment" : "like";
          String body = savedComment ? comment : "赞了 " + target.optString("authorName", "对方") + " 的动态";
          notifyIfBackground(context, prefs, card.optString("name", "角色"), body, action);
          return;
        }
      }
      if (!apiConfigured) return;
      for (int i = 0; i < cards.length(); i++) {
        JSONObject card = cards.optJSONObject(i);
        if (card == null || !allowed.contains(card.optString("id"))) continue;
        String id = card.optString("id");
        long next = prefs.getLong("moment_auto_next_" + id, 0);
        if (next == 0) {
          scheduleNext(prefs, id, now);
          continue;
        }
        if (now < next) continue;
        String body = AiContentSanitizer.moment(generate(endpoint, key, model, prefs, state, card));
        if (body.isEmpty()) throw new IllegalStateException("模型没有返回可发布的正文");
        JSONArray moments = new JSONArray(prefs.getString("moments", "[]"));
        JSONObject post = new JSONObject().put("id", java.util.UUID.randomUUID().toString())
            .put("authorId", id).put("authorName", card.optString("name", "角色"))
            .put("content", body).put("image", "").put("createdAt", now);
        JSONArray updated = new JSONArray().put(post);
        for (int j = 0; j < Math.min(499, moments.length()); j++) updated.put(moments.opt(j));
        prefs.edit().putString("moments", updated.toString()).remove("moment_auto_last_error")
            .putLong("moment_state_revision", prefs.getLong("moment_state_revision", 0) + 1).apply();
        scheduleNext(prefs, id, now);
        notifyIfBackground(context, prefs, card.optString("name", "角色"), body, "post");
        break;
      }
    } catch (Exception error) {
      prefs.edit().putString("moment_auto_last_error", error.getMessage() == null ? error.toString() : error.getMessage()).apply();
    } finally {
      db.close();
    }
  }

  private static boolean hasLike(JSONArray values, String roleId) {
    if (values == null) return false;
    for (int i = 0; i < values.length(); i++) if (roleId.equals(values.optString(i))) return true;
    return false;
  }

  private static boolean hasComment(JSONArray values, String roleId) {
    if (values == null) return false;
    for (int i = 0; i < values.length(); i++) {
      JSONObject value = values.optJSONObject(i);
      if (value != null && roleId.equals(value.optString("authorId"))) return true;
    }
    return false;
  }

  private static boolean containsMoment(JSONArray moments, String momentId) {
    for (int i = 0; i < moments.length(); i++) {
      JSONObject value = moments.optJSONObject(i);
      if (value != null && momentId.equals(value.optString("id"))) return true;
    }
    return false;
  }

  private static void scheduleNext(SharedPreferences prefs, String id, long now) {
    int legacyMin = Math.max(1, prefs.getInt("moments_auto_min_hours", 4)) * 60;
    int legacyMax = Math.max(legacyMin, prefs.getInt("moments_auto_max_hours", 12) * 60);
    int min = Math.max(1, prefs.getInt("moments_auto_min_minutes", legacyMin));
    int max = Math.max(min, prefs.getInt("moments_auto_max_minutes", legacyMax));
    long minutes = min + (long) (Math.random() * (max - min + 1));
    prefs.edit().putLong("moment_auto_next_" + id, now + minutes * 60_000L).apply();
  }

  private static void scheduleCommentNext(SharedPreferences prefs, String id, long now) {
    int legacyMin = Math.max(1, prefs.getInt("moments_auto_min_hours", 4)) * 60;
    int legacyMax = Math.max(legacyMin, prefs.getInt("moments_auto_max_hours", 12) * 60);
    int min = Math.max(1, prefs.getInt("moments_auto_min_minutes", legacyMin));
    int max = Math.max(min, prefs.getInt("moments_auto_max_minutes", legacyMax));
    long minutes = min + (long) (Math.random() * (max - min + 1));
    prefs.edit().putLong("moment_comment_next_" + id, now + minutes * 60_000L).apply();
  }

  private static String generate(String endpoint, String key, String model, SharedPreferences prefs,
      JSONObject state, JSONObject card) throws Exception {
    return GenerationRetry.run(prefs, () -> generateOnce(endpoint, key, model, prefs, state, card));
  }

  private static String generateOnce(String endpoint, String key, String model, SharedPreferences prefs,
      JSONObject state, JSONObject card) throws Exception {
    JSONArray recent = new JSONArray(prefs.getString("moments", "[]"));
    StringBuilder feed = new StringBuilder();
    for (int i = 0; i < Math.min(8, recent.length()); i++) {
      JSONObject item = recent.optJSONObject(i);
      if (item != null) {
        String content = item.optString("content");
        if (content.isEmpty() && !item.optString("image").isEmpty()) content = "[图片动态]";
        if (!content.isEmpty()) feed.append(item.optString("authorName", "我")).append('：').append(content).append('\n');
      }
    }
    StringBuilder chat = new StringBuilder();
    JSONArray chats = state.optJSONArray("chats");
    JSONObject latestChat = null;
    long latestUpdated = Long.MIN_VALUE;
    if (chats != null) for (int i = 0; i < chats.length(); i++) {
      JSONObject candidate = chats.optJSONObject(i);
      if (candidate == null) continue;
      boolean belongs = card.optString("id").equals(candidate.optString("cardId"));
      JSONArray ids = candidate.optJSONArray("cardIds");
      if (!belongs && ids != null) for (int j = 0; j < ids.length(); j++)
        belongs |= card.optString("id").equals(ids.optString(j));
      if (!belongs) continue;
      long updated = candidate.optLong("updated", 0);
      if (latestChat == null || updated > latestUpdated) {
        latestChat = candidate;
        latestUpdated = updated;
      }
    }
    if (latestChat != null) {
      JSONArray messages = latestChat.optJSONArray("messages");
      if (messages != null) for (int j = Math.max(0, messages.length() - 8); j < messages.length(); j++) {
        JSONObject message = messages.optJSONObject(j);
        if (message != null && !message.optString("body").isEmpty()) chat.append(message.optString("role")).append('：').append(message.optString("body")).append('\n');
      }
    }
    JSONArray messages = new JSONArray().put(new JSONObject().put("role", "system").put("content",
        "你是 " + card.optString("name") + "。" + card.optString("persona")
            + "\n请在合适的时刻以自己的口吻发布一条30到120字的朋友圈。自然表达当下经历或情绪，不解释任务，不加角色名前缀，不编造刚发生但上下文没有依据的重大事件。"))
        .put(new JSONObject().put("role", "user").put("content",
            (chat.length() == 0 ? "" : "最近对话：\n" + chat) + (feed.length() == 0 ? "" : "最近朋友圈：\n" + feed) + "现在发布一条自然的新动态。"));
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    try {
    connection.setRequestMethod("POST");connection.setConnectTimeout(20_000);connection.setReadTimeout(90_000);connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    connection.setRequestProperty("Authorization", "Bearer " + key);
    JSONObject request = new JSONObject().put("model", model).put("stream", false).put("temperature", .9).put("messages", messages);
    try (OutputStream output = connection.getOutputStream()) { output.write(request.toString().getBytes(StandardCharsets.UTF_8)); }
    int code = connection.getResponseCode();
    String raw = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
    if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + (raw.isEmpty() ? "" : "：" + raw));
    return new JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
    } finally {
      connection.disconnect();
    }
  }

  private static String generateComment(String endpoint, String key, String model, SharedPreferences prefs, JSONObject card, JSONObject post, JSONArray existing) throws Exception {
    return GenerationRetry.run(prefs, () -> generateCommentOnce(endpoint, key, model, card, post, existing));
  }

  private static String generateCommentOnce(String endpoint, String key, String model, JSONObject card, JSONObject post, JSONArray existing) throws Exception {
    StringBuilder thread = new StringBuilder();
    for (int i = Math.max(0, existing.length() - 6); i < existing.length(); i++) {
      JSONObject value = existing.optJSONObject(i);
      if (value != null && !value.optString("content").isEmpty())
        thread.append(value.optString("authorName", "其他角色")).append("：").append(value.optString("content")).append('\n');
    }
    String postContent = post.optString("content").trim();
    if (postContent.isEmpty() && !post.optString("image").isEmpty()) postContent = "[图片动态]";
    if (postContent.isEmpty()) postContent = "[无文字动态]";
    JSONArray messages = new JSONArray().put(new JSONObject().put("role", "system").put("content",
        "你是 " + card.optString("name") + "。" + card.optString("persona")
            + "\n请以自己的口吻自然参与同一条朋友圈讨论，8到60字。回应原动态或已有评论，保持你自己的关系和立场，不解释任务，不加角色名前缀，不替其他角色发言。"))
        .put(new JSONObject().put("role", "user").put("content", post.optString("authorName", "对方")
            + " 发布：" + postContent + (thread.length() == 0 ? "" : "\n已有评论：\n" + thread)));
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    try {
    connection.setRequestMethod("POST");connection.setConnectTimeout(20_000);connection.setReadTimeout(90_000);connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");connection.setRequestProperty("Authorization", "Bearer " + key);
    JSONObject request = new JSONObject().put("model", model).put("stream", false).put("temperature", .9).put("messages", messages);
    try (OutputStream output = connection.getOutputStream()) { output.write(request.toString().getBytes(StandardCharsets.UTF_8)); }
    int code = connection.getResponseCode();String raw = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
    if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + (raw.isEmpty() ? "" : "：" + raw));
    return new JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
    } finally {
      connection.disconnect();
    }
  }

  private static String read(InputStream input) throws Exception {
    if (input == null) return "";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder result = new StringBuilder();String line;
      while ((line = reader.readLine()) != null) result.append(line);
      return result.toString();
    }
  }

  private static void notifyIfBackground(Context context, SharedPreferences prefs, String name, String body, String action) {
    if (!prefs.getBoolean("app_in_foreground", false)) notify(context, name, body, action);
  }

  private static void notify(Context context, String name, String body, String action) {
    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel("moment_posts", "朋友圈动态", NotificationManager.IMPORTANCE_DEFAULT));
    Intent open = new Intent(context, ComposeActivity.class).putExtra("open_moments", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pending = PendingIntent.getActivity(context, 5801, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, "moment_posts") : new Notification.Builder(context);
    String title = "comment_like".equals(action) ? name + " 评论并点赞了朋友圈"
        : "comment".equals(action) ? name + " 评论了朋友圈"
        : "like".equals(action) ? name + " 点赞了朋友圈" : name + " 发布了朋友圈";
    builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title)
        .setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(pending).setAutoCancel(true);
    manager.notify((name + body).hashCode(), builder.build());
  }
}
