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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** Generates opted-in role messages while the app is in the background. */
public final class ActiveReplyWorker extends JobService {
  private static final int JOB_ID = 0x53485955;
  private volatile Thread runningThread;

  static void sync(Context context) {
    if (MainActivity.isDataRescueRequired(context)) {
      cancel(context);
      return;
    }
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (!prefs.getBoolean("active_replies", false)) {
      scheduler.cancel(JOB_ID);
      return;
    }
    JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, ActiveReplyWorker.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPeriodic(15 * 60_000L)
        .setPersisted(true)
        .build();
    scheduler.schedule(job);
  }

  static void cancel(Context context) {
    JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (scheduler != null) scheduler.cancel(JOB_ID);
  }

  @Override public boolean onStartJob(JobParameters params) {
    Thread worker = new Thread(() -> {
      Context context = getApplicationContext();
      CrashProtection.markFeatureActive(context, CrashProtection.FEATURE_ACTIVE_REPLIES);
      try {
        runOnce(context, false);
      } finally {
        if (runningThread == Thread.currentThread()) runningThread = null;
        CrashProtection.clearFeatureActive(context, CrashProtection.FEATURE_ACTIVE_REPLIES);
        jobFinished(params, false);
      }
    }, "shiyu-active-reply");
    runningThread = worker;
    worker.start();
    return true;
  }

  @Override public boolean onStopJob(JobParameters params) {
    Thread worker = runningThread;
    runningThread = null;
    if (worker != null) worker.interrupt();
    return !MainActivity.isDataRescueRequired(this);
  }

  static void runYandere(Context context) {
    Context app = context.getApplicationContext();
    CrashProtection.markFeatureActive(app, CrashProtection.FEATURE_ACTIVE_REPLIES);
    try {
      runOnce(app, true);
    } finally {
      CrashProtection.clearFeatureActive(app, CrashProtection.FEATURE_ACTIVE_REPLIES);
    }
  }

  private static synchronized void runOnce(Context context, boolean yandereOnly) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    long writeEpoch = MainActivity.dataWriteEpoch(context);
    if (Thread.currentThread().isInterrupted() || MainActivity.isDataRescueRequired(context)
        || !prefs.getBoolean("active_replies", false) || prefs.getBoolean("app_in_foreground", false)
        || prefs.getLong("active_generation_until", 0) > System.currentTimeMillis())
      return;
    SecureStore secure = new SecureStore(context);
    String key = secure.get();
    String endpoint = prefs.getString("endpoint", "").trim();
    String model = prefs.getString("model", "").trim();
    if (key.isEmpty() || endpoint.isEmpty() || model.isEmpty()) return;
    while (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
    if (!endpoint.endsWith("/chat/completions")) endpoint += "/chat/completions";

    AppDb db = new AppDb(context);
    try {
      String savedState = db.loadState();
      if (savedState.isEmpty()) return;
      JSONObject root = new JSONObject(savedState);
      JSONArray cardJson = root.optJSONArray("cards");
      JSONArray chatJson = root.optJSONArray("chats");
      if (cardJson == null || chatJson == null) return;
      ArrayList<MainActivity.Card> cards = new ArrayList<>();
      for (int i = 0; i < cardJson.length(); i++) cards.add(MainActivity.Card.from(cardJson.getJSONObject(i)));
      long now = System.currentTimeMillis();
      for (int i = 0; i < chatJson.length(); i++) {
        MainActivity.Chat chat = MainActivity.Chat.from(chatJson.getJSONObject(i));
        if (!isDue(chat, now, yandereOnly)) continue;
        MainActivity.Card card = find(cards, chat.activeCardId.isEmpty() ? chat.cardId : chat.activeCardId);
        if (card == null) continue;
        int burstLimit = Math.max(1, Math.min(20, chat.proactiveBurstCount));
        int burst = 1 + (int) (Math.random() * burstLimit);
        String lastReply = "";
        MainActivity.Card lastCard = card;
        for (int part = 0; part < burst; part++) {
          MainActivity.Card speaker = chat.group && chat.groupAutonomous ? randomGroupCard(cards, chat, lastCard.id) : card;
          if (speaker == null) speaker = card;
          MusicReply parsed = resolveMusicReplies(context,
              MainActivity.expandReplyMacros(prefs,
                  generate(context, endpoint, key, model, prefs, db, chat, speaker), speaker), speaker);
          String reply = parsed.text;
          if (Thread.currentThread().isInterrupted()
              || !MainActivity.canWriteData(context, writeEpoch)) return;
          if (!reply.isEmpty()) {
            MainActivity.Msg message = new MainActivity.Msg("assistant", reply);
            message.speakerCardId = speaker.id;
            if (chat.ttsEnabled) {
              if (chat.ttsReplyMode == 1) message.displayMode = 1;
              else if (chat.ttsReplyMode == 2) message.displayMode = 2;
              else if (chat.ttsReplyMode == 3) message.displayMode = Math.random() < .5 ? 1 : 0;
            }
            chat.messages.add(message);
          }
          chat.messages.addAll(parsed.music);
          lastReply = reply.isEmpty() && !parsed.music.isEmpty()
              ? "分享了歌曲《" + parsed.music.get(0).body.substring(0,
                  Math.min(parsed.music.get(0).body.length(), 60)) + "》" : reply;
          lastCard = speaker;
        }
        chat.updated = now;
        chat.lastProactiveAt = now;
        chat.nextProactiveAt = now + randomDelay(chat.proactiveMinMinutes, chat.proactiveMaxMinutes);
        chat.proactiveSequence++;
        chatJson.put(i, chat.json());
        if (Thread.currentThread().isInterrupted()
            || !MainActivity.canWriteData(context, writeEpoch)) return;
        db.saveState(root.put("chats", chatJson).toString());
        long revision = Math.max(now, prefs.getLong("background_state_revision", 0) + 1);
        prefs.edit().putLong("background_state_revision", revision).commit();
        notify(context, chat.id, lastCard.name, lastReply);
        break; // One generated notification per worker pass avoids notification bursts.
      }
      prefs.edit().remove("proactive_last_error").apply();
      return;
    } catch (Exception error) {
      if (MainActivity.canWriteData(context, writeEpoch)) {
        prefs.edit().putString("proactive_last_error", MainActivity.friendlyError(error))
            .putString("last_api_error", MainActivity.errorDetails(error)).apply();
        DiagnosticLog.record(context, "active_reply", "error", MainActivity.diagnosticSummary(error));
      }
      return;
    } finally {
      db.close();
    }
  }

  private static MusicReply resolveMusicReplies(Context context, String source,
      MainActivity.Card speaker) {
    String value = source == null ? "" : source;
    Matcher matcher = Pattern.compile("\\[分享音乐[：:]\\s*([^\\]\\r\\n]{1,300})\\]").matcher(value);
    StringBuffer cleaned = new StringBuffer();ArrayList<MainActivity.Msg> music = new ArrayList<>();
    while (matcher.find()) {
      String[] parts = matcher.group(1).trim().split("\\s*[|｜]\\s*", 3);
      if (parts.length > 0 && music.size() < 3) try {
        JSONObject resolved = NeteaseMusicResolver.resolve(context, parts[0],
            parts.length > 1 ? parts[1] : "");
        MainActivity.Msg message = new MainActivity.Msg("assistant",
            resolved.optString("title") + " · " + resolved.optString("artist", "未知歌手"));
        message.speakerCardId = speaker.id;message.messageType = "music";
        message.meta = resolved.toString();music.add(message);
      } catch (Exception ignored) {}
      matcher.appendReplacement(cleaned, "");
    }
    matcher.appendTail(cleaned);
    return new MusicReply(cleaned.toString().replace("<SEG>", "\n\n").trim(), music);
  }

  private static final class MusicReply {
    final String text;final ArrayList<MainActivity.Msg> music;
    MusicReply(String text, ArrayList<MainActivity.Msg> music) { this.text = text;this.music = music; }
  }

  private static boolean isDue(MainActivity.Chat chat, long now, boolean yandereOnly) {
    if (chat.messages.isEmpty()) return false;
    if (chat.userBlockedRole || chat.roleBlockedUser) return false;
    if (isQuietHours(chat)) return false;
    if (yandereOnly) return chat.yandereEnabled && now - chat.lastProactiveAt >= 60_000L;
    if (!chat.proactiveEnabled || chat.yandereEnabled) return false;
    int allowed = chat.proactiveFollowUps ? Math.min(30, chat.proactiveMaxFollowUps) : 1;
    if (chat.proactiveSequence >= Math.max(1, allowed)) return false;
    if (chat.nextProactiveAt > 0) return now >= chat.nextProactiveAt;
    long base = Math.max(chat.updated, chat.lastProactiveAt);
    return now - base >= Math.max(1, chat.proactiveMinMinutes) * 60_000L;
  }

  private static boolean isQuietHours(MainActivity.Chat chat) {
    if (!chat.proactiveDnd) return false;
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int start = chat.proactiveDndStart, end = chat.proactiveDndEnd;
    return start != end && (start < end ? hour >= start && hour < end : hour >= start || hour < end);
  }

  private static long randomDelay(int minValue, int maxValue) {
    int min = Math.max(1, Math.min(minValue, maxValue));
    int max = Math.max(min, Math.max(minValue, maxValue));
    int minutes = min + (int) (Math.random() * (max - min + 1));
    return minutes * 60_000L;
  }

  private static String generate(Context context, String endpoint, String key, String model, SharedPreferences prefs,
      AppDb db, MainActivity.Chat chat, MainActivity.Card card) throws Exception {
    return GenerationRetry.run(prefs, () -> generateOnce(context, endpoint, key, model, prefs, db, chat, card));
  }

  private static String generateOnce(Context context, String endpoint, String key, String model, SharedPreferences prefs,
      AppDb db, MainActivity.Chat chat, MainActivity.Card card) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    try {
    connection.setRequestMethod("POST");
    connection.setConnectTimeout(20_000);
    connection.setReadTimeout(MainActivity.AI_RESPONSE_TIMEOUT_MS);
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    connection.setRequestProperty("Authorization", "Bearer " + key);
    JSONArray messages = new JSONArray();
    String memories = db.memoryContext(card.id);
    String moments = momentsContext(prefs, card.id);
    String identity = MainActivity.identityFor(prefs,chat).prompt();
    String system = "你是 " + card.name + "。" + card.persona
        + (card.desc.isEmpty() ? "" : "\n角色简介：" + card.desc)
        + (identity.isEmpty() ? "" : "\n用户当前身份设定：" + identity)
        + (memories.isEmpty() ? "" : "\n长期记忆：\n" + memories)
        + (moments.isEmpty() ? "" : "\n用户已授权你查看最近朋友圈：\n" + moments)
        + (chat.summary.isEmpty() ? "" : "\n此前摘要：\n" + chat.summary)
        + "\n当你确实想主动分享歌曲时，可在正文末尾追加 [分享音乐：歌名|歌手]。应用会从网易云搜索真实歌曲、封面和音源；不要编造链接，也不要解释这个内部指令。"
        + screenTimeContext(context, chat);
    messages.put(new JSONObject().put("role", "system").put("content", system));
    int count = prefs.getInt("context", 30);
    for (MainActivity.Msg message : ChatRequestMessages.recent(chat.messages, count)) {
      String role = message.role;
      String content = message.body;
      if (chat.group && "assistant".equals(role) && !message.speakerCardId.isEmpty()
          && !message.speakerCardId.equals(card.id)) {
        role = "user";
        content = "群聊中的另一位成员说：" + content;
      }
      if ((content == null || content.trim().isEmpty()) && !message.image.isEmpty())
        content = "[分享了一张图片，当前请求未包含图片内容]";
      ChatRequestMessages.append(messages, role, content);
    }
    messages.put(new JSONObject().put("role", "user").put("content", chat.group && chat.groupActiveTopics
        ? "这是一个真实群聊。结合自己的关系、时间和最近对话，自然接续讨论或主动开启一个合适的新话题。不要替其他角色发言，不要解释任务，只输出自己的消息正文。"
        : "结合关系、时间和最近对话，主动发来一条自然的新消息。不要解释任务，不要重复上一句话，只输出角色消息正文。"));
    double temperature = Double.longBitsToDouble(prefs.getLong("temp", Double.doubleToLongBits(.85)));
    ChatRequestMessages.trimToBudget(messages, prefs.getInt("model_max_context", 32768));
    JSONObject body = new JSONObject().put("model", model).put("temperature", temperature)
        .put("stream", false).put("messages", messages);
    try (OutputStream output = connection.getOutputStream()) {
      output.write(body.toString().getBytes(StandardCharsets.UTF_8));
    }
    int code = connection.getResponseCode();
    String raw = read(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
    if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + (raw.isEmpty() ? "" : "：" + raw));
    String content = MainActivity.jsonText(new JSONObject(raw).getJSONArray("choices")
        .getJSONObject(0).getJSONObject("message"), "content").trim();
    if (content.isEmpty()) throw new IOException("接口未返回消息正文");
    return content;
    } finally {
      connection.disconnect();
    }
  }

  private static String screenTimeContext(Context context, MainActivity.Chat chat) {
    if (!chat.yandereScreenTimeEnabled) return "";
    android.app.AppOpsManager ops = (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    if (ops == null) return "";
    int mode = Build.VERSION.SDK_INT >= 29
        ? ops.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName())
        : ops.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
    if (mode != android.app.AppOpsManager.MODE_ALLOWED) return "";
    android.app.usage.UsageStatsManager manager = (android.app.usage.UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    if (manager == null) return "";
    long end = System.currentTimeMillis(), start = end - 86_400_000L, total = 0;
    java.util.List<android.app.usage.UsageStats> values = manager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, start, end);
    java.util.HashMap<String, Long> apps = new java.util.HashMap<>();
    if (values != null) for (android.app.usage.UsageStats value : values) {long time=Math.max(0,value.getTotalTimeInForeground());total+=time;if(time>=60_000L)apps.merge(value.getPackageName(),time,Long::sum);}
    long minutes = total / 60_000L;
    String details="";
    if(chat.yandereAppUsageEnabled){java.util.ArrayList<java.util.Map.Entry<String,Long>> sorted=new java.util.ArrayList<>(apps.entrySet());sorted.sort((a,b)->Long.compare(b.getValue(),a.getValue()));java.util.ArrayList<String> parts=new java.util.ArrayList<>();for(java.util.Map.Entry<String,Long> value:sorted){if(parts.size()>=6)break;String label=value.getKey();try{android.content.pm.ApplicationInfo info=context.getPackageManager().getApplicationInfo(value.getKey(),0);label=context.getPackageManager().getApplicationLabel(info).toString();}catch(Exception ignored){}long mins=value.getValue()/60_000L;parts.add(label+" "+(mins>=60?(mins/60+"小时"+(mins%60)+"分钟"):mins+"分钟"));}details=String.join("、",parts);}
    return "\n用户已授权你知道过去24小时屏幕使用总时长约" + (minutes >= 60 ? minutes / 60 + "小时" + minutes % 60 + "分钟" : minutes + "分钟")
        +(details.isEmpty()?"。只可自然关心作息，不得推断具体应用。":"，并另行授权常用应用时长："+details+"。只可自然关心，不要持续监视或作出敏感推断。 ");
  }

  private static String momentsContext(SharedPreferences prefs, String cardId) {
    if (!prefs.getStringSet("moments_view_roles", java.util.Collections.emptySet()).contains(cardId)) return "";
    StringBuilder context = new StringBuilder();
    try {
      JSONArray values = new JSONArray(prefs.getString("moments", "[]"));
      JSONObject comments = new JSONObject(prefs.getString("moment_comments", "{}"));
      JSONObject likes = new JSONObject(prefs.getString("moment_likes", "{}"));
      for (int i = 0; i < Math.min(8, values.length()); i++) {
        JSONObject item = values.optJSONObject(i);
        if (item == null) continue;
        String content = item.optString("content").trim();
        if (content.isEmpty() && !item.optString("image").isEmpty()) content = "[分享了一张图片]";
        if (content.isEmpty()) continue;
        if (context.length() > 0) context.append('\n');
        context.append("- ").append(item.optString("authorName", "我")).append("：").append(content);
        JSONArray likeValues = likes.optJSONArray(item.optString("id"));
        if (likeValues != null && likeValues.length() > 0) context.append("（").append(likeValues.length()).append(" 人点赞）");
        JSONArray commentValues = comments.optJSONArray(item.optString("id"));
        if (commentValues != null) for (int j = 0; j < Math.min(6, commentValues.length()); j++) {
          JSONObject comment = commentValues.optJSONObject(j);
          if (comment != null && !comment.optString("content").trim().isEmpty())
            context.append("\n  评论 · ").append(comment.optString("authorName", "用户")).append("：").append(comment.optString("content").trim());
        }
        if (context.length() >= 2400) break;
      }
    } catch (Exception ignored) {}
    return context.toString();
  }

  private static MainActivity.Card find(ArrayList<MainActivity.Card> cards, String id) {
    for (MainActivity.Card card : cards) if (card.id.equals(id)) return card;
    return null;
  }

  private static MainActivity.Card randomGroupCard(ArrayList<MainActivity.Card> cards, MainActivity.Chat chat, String previousId) {
    ArrayList<MainActivity.Card> candidates = new ArrayList<>();
    for (String id : chat.cardIds) {
      MainActivity.Card value = find(cards, id);
      if (value != null && (!value.id.equals(previousId) || chat.cardIds.size() == 1)) candidates.add(value);
    }
    return candidates.isEmpty() ? find(cards, chat.cardId) : candidates.get((int) (Math.random() * candidates.size()));
  }


  private static String read(InputStream input) throws Exception {
    if (input == null) return "";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) result.append(line);
      return result.toString();
    }
  }

  private static void notify(Context context, String chatId, String name, String body) {
    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
        != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26)
      manager.createNotificationChannel(new NotificationChannel("active_reply", "聊天消息", NotificationManager.IMPORTANCE_DEFAULT));
    Intent intent = new Intent(context, ComposeActivity.class).putExtra("open_chat_id", chatId)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pending = PendingIntent.getActivity(context, chatId.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(context, "active_reply") : new Notification.Builder(context);
    builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(name + " 有新消息")
        .setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body))
        .setContentIntent(pending).setAutoCancel(true);
    manager.notify(chatId.hashCode(), builder.build());
  }
}
