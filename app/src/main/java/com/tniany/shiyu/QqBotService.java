package com.tniany.shiyu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONArray;
import org.json.JSONObject;

/** Maintains one official QQ Gateway connection for every enabled Bot configuration. */
public final class QqBotService extends Service {
  private static final String CHANNEL_ID = "qqbot_link";
  private static final int NOTIFICATION_ID = 7055;
  private static final String TOKEN_URL = "https://bots.qq.com/app/getAppAccessToken";
  private static final String API_BASE = "https://api.sgroup.qq.com";
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final Pattern IPV4 = Pattern.compile(
      "(?<![0-9])(?:25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])"
          + "(?:\\.(?:25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])){3}(?![0-9])");
  private final Map<String, BotSession> sessions = new HashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
  private final ExecutorService workers = Executors.newFixedThreadPool(4);
  private final Set<String> processing = Collections.newSetFromMap(
      new ConcurrentHashMap<String, Boolean>());
  private OkHttpClient http;
  private SharedPreferences prefs;
  private volatile boolean destroyed;
  private boolean foregroundReady;

  static void sync(Context context) {
    Context app = app(context);
    Intent intent = new Intent(app, QqBotService.class);
    if (MainActivity.isDataRescueRequired(app)) {
      app.stopService(intent);
      return;
    }
    boolean enabled = QqBotClient.enabledCount(app) > 0;
    try {
      if (enabled) {
        if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent);
        else app.startService(intent);
      } else {
        app.stopService(intent);
      }
    } catch (RuntimeException error) {
      recordServiceError(app, error);
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    prefs = getSharedPreferences(MainActivity.PREF, MODE_PRIVATE);
    if (MainActivity.isDataRescueRequired(this)) {
      stopSelf();
      return;
    }
    http = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS).writeTimeout(35, TimeUnit.SECONDS)
        .pingInterval(0, TimeUnit.SECONDS).build();
    CrashProtection.markFeatureActive(this, CrashProtection.FEATURE_QQBOT);
    try {
      startForeground(NOTIFICATION_ID, notification("正在启动 QQ Bot 连接"));
      foregroundReady = true;
      prefs.edit().remove("qqbot_service_error").apply();
      refreshSessions();
    } catch (RuntimeException error) {
      recordServiceError(this, error);
      stopSelf();
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (MainActivity.isDataRescueRequired(this) || !foregroundReady || destroyed) {
      stopSelf(startId);
      return START_NOT_STICKY;
    }
    try {
      return refreshSessions() ? START_STICKY : START_NOT_STICKY;
    } catch (RuntimeException error) {
      recordServiceError(this, error);
      stopSelf(startId);
      return START_NOT_STICKY;
    }
  }

  private synchronized boolean refreshSessions() {
    if (destroyed) return false;
    ArrayList<QqBotClient.Account> enabled = new ArrayList<>();
    for (QqBotClient.Account account : QqBotClient.accounts(this))
      if (account.enabled) enabled.add(account);
    Set<String> keep = new HashSet<>();
    for (QqBotClient.Account account : enabled) {
      keep.add(account.id);
      BotSession current = sessions.get(account.id);
      if (current != null && account.signature().equals(current.signature)) continue;
      if (current != null) current.stop();
      BotSession replacement = new BotSession(account);
      sessions.put(account.id, replacement);
      replacement.start();
    }
    ArrayList<String> removed = new ArrayList<>();
    for (String id : sessions.keySet()) if (!keep.contains(id)) removed.add(id);
    for (String id : removed) {
      BotSession session = sessions.remove(id);
      if (session != null) session.stop();
      setStatus(id, "已暂停", "");
    }
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (manager != null) manager.notify(NOTIFICATION_ID,
        notification(enabled.size() + " 个 QQ Bot 正在运行"));
    if (enabled.isEmpty()) {
      stopSelf();
      return false;
    }
    return true;
  }

  private final class BotSession {
    final QqBotClient.Account account;
    final String signature;
    final AtomicBoolean connecting = new AtomicBoolean();
    final AtomicInteger connectionGeneration = new AtomicInteger();
    final AtomicInteger messageSequence = new AtomicInteger();
    volatile boolean stopped;
    volatile boolean reconnectBlocked;
    volatile WebSocket socket;
    volatile String token = "";
    volatile String sessionId = "";
    volatile String resumeUrl = "";
    volatile long lastSequence;
    volatile int reconnectAttempts;
    ScheduledFuture<?> heartbeat;
    ScheduledFuture<?> reconnect;

    BotSession(QqBotClient.Account account) {
      this.account = account;
      signature = account.signature();
    }

    void start() {
      scheduleConnect(0);
    }

    synchronized void scheduleConnect(long delayMs) {
      if (unavailable()) return;
      ScheduledFuture<?> previous = reconnect;
      if (previous != null) previous.cancel(false);
      try {
        reconnect = scheduler.schedule(this::connect, Math.max(0, delayMs), TimeUnit.MILLISECONDS);
      } catch (RejectedExecutionException error) {
        if (!stopped && !destroyed) {
          reconnectBlocked = true;
          setStatus(account.id, "连接已停止", "QQ Bot 后台连接任务已关闭，请关闭后重新开启此 Bot");
          recordServiceError(QqBotService.this, error);
        }
      }
    }

    void connect() {
      final int attemptGeneration;
      synchronized (this) {
        if (unavailable() || socket != null || !connecting.compareAndSet(false, true)) return;
        attemptGeneration = connectionGeneration.incrementAndGet();
      }
      setStatus(account.id, reconnectAttempts == 0 ? "正在连接" : "正在重连", "");
      Runnable task = () -> {
        try {
          String appSecret = new SecureStore(QqBotService.this)
              .getNamed(QqBotClient.secretKey(account.id));
          if (account.appId.isEmpty() || appSecret.isEmpty() || account.cardId.isEmpty())
            throw new PermanentConnectionException("配置错误", "AppID、AppSecret 和角色不能为空");
          String accessToken = fetchToken(account.appId, appSecret);
          String gateway = fetchGateway(accessToken);
          final String target;
          synchronized (this) {
            if (!isCurrent(attemptGeneration)) return;
            target = resumeUrl.isEmpty() ? gateway : resumeUrl;
            token = accessToken;
          }
          WebSocket created = http.newWebSocket(new Request.Builder().url(target).build(),
              new GatewayListener(attemptGeneration));
          synchronized (this) {
            if (isCurrent(attemptGeneration)) socket = created;
            else closeSocket(created, 1000, "stale");
          }
        } catch (PermanentConnectionException error) {
          blockReconnect(attemptGeneration, error);
        } catch (Exception error) {
          failAndReconnect(attemptGeneration, error);
        } finally {
          connecting.set(false);
        }
      };
      try {
        workers.execute(task);
      } catch (RejectedExecutionException error) {
        connecting.set(false);
        if (isCurrent(attemptGeneration)) {
          blockReconnect(attemptGeneration, new PermanentConnectionException("连接已停止",
              "QQ Bot 后台连接任务已关闭，请关闭后重新开启此 Bot"));
          recordServiceError(QqBotService.this, error);
        }
      }
    }

    private boolean unavailable() {
      return stopped || reconnectBlocked || destroyed;
    }

    private boolean isCurrent(int generation) {
      return !unavailable() && connectionGeneration.get() == generation;
    }

    private synchronized boolean ownsSocket(int generation, WebSocket webSocket) {
      return isCurrent(generation) && (socket == null || socket == webSocket);
    }

    private void onSocketOpen(int generation, WebSocket webSocket) {
      synchronized (this) {
        if (!isCurrent(generation)) {
          closeSocket(webSocket, 1000, "stale");
          return;
        }
        socket = webSocket;
      }
      setStatus(account.id, "正在鉴权", "");
    }

    private void onSocketMessage(int generation, WebSocket webSocket, String text) {
      if (!ownsSocket(generation, webSocket)) return;
      try {
        JSONObject frame = new JSONObject(text);
        int op = frame.optInt("op", -1);
        if (frame.has("s") && !frame.isNull("s")) lastSequence = frame.optLong("s", lastSequence);
        switch (op) {
          case 10:
            long interval = 45_000L;
            JSONObject hello = frame.optJSONObject("d");
            if (hello != null) interval = hello.optLong("heartbeat_interval", interval);
            startHeartbeat(generation, Math.max(5_000L, Math.min(120_000L, interval)));
            if (!sessionId.isEmpty()) sendResume(webSocket); else sendIdentify(webSocket);
            break;
          case 0:
            handleDispatch(generation, frame.optString("t"), frame.optJSONObject("d"));
            break;
          case 7:
            reconnectNow(generation);
            break;
          case 9:
            if (!frame.optBoolean("d", false)) {
              sessionId = "";
              resumeUrl = "";
              lastSequence = 0;
            }
            reconnectNow(generation);
            break;
          case 11:
          default:
            break;
        }
      } catch (Exception error) {
        if (isCurrent(generation)) setStatus(account.id, "协议消息异常", compact(error));
      }
    }

    private final class GatewayListener extends WebSocketListener {
      private final int generation;

      GatewayListener(int generation) {
        this.generation = generation;
      }

      @Override public void onOpen(WebSocket webSocket, Response response) {
        onSocketOpen(generation, webSocket);
      }

      @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
        onSocketMessage(generation, webSocket, bytes.utf8());
      }

      @Override public void onMessage(WebSocket webSocket, String text) {
        onSocketMessage(generation, webSocket, text);
      }

      @Override public void onClosing(WebSocket webSocket, int code, String reason) {
        closeSocket(webSocket, code, reason);
      }

      @Override public void onClosed(WebSocket webSocket, int code, String reason) {
        if (ownsSocket(generation, webSocket)) reconnectNow(generation);
      }

      @Override public void onFailure(WebSocket webSocket, Throwable error, Response response) {
        int code = response == null ? -1 : response.code();
        String raw = responseBody(response);
        if (response != null) {
          try { response.close(); } catch (RuntimeException ignored) {}
        }
        if (!ownsSocket(generation, webSocket)) return;
        if (isIpWhitelistError(raw)) {
          blockReconnect(generation, ipWhitelistException(raw));
          return;
        }
        Throwable detail = raw.isEmpty() ? error : new IOException(
            "Gateway WebSocket HTTP " + code + ": " + shorten(raw), error);
        failAndReconnect(generation, detail);
      }
    }

    private void handleDispatch(int generation, String type, JSONObject data) {
      if (data == null || !isCurrent(generation)) return;
      if ("READY".equals(type)) {
        sessionId = data.optString("session_id", sessionId);
        resumeUrl = data.optString("resume_gateway_url", resumeUrl);
        reconnectAttempts = 0;
        if (isCurrent(generation)) setStatus(account.id, "已连接", "");
      } else if ("RESUMED".equals(type)) {
        reconnectAttempts = 0;
        if (isCurrent(generation)) setStatus(account.id, "已连接", "");
      } else if ("C2C_MESSAGE_CREATE".equals(type)) {
        dispatchMessage(account, data, false);
      } else if ("GROUP_AT_MESSAGE_CREATE".equals(type)) {
        dispatchMessage(account, data, true);
      }
    }

    private void closeSocket(WebSocket webSocket, int code, String reason) {
      if (webSocket == null) return;
      try {
        webSocket.close(code, reason);
      } catch (RuntimeException error) {
        if (!unavailable()) recordServiceError(QqBotService.this, error);
      }
    }

    private void sendIdentify(WebSocket webSocket) throws Exception {
      JSONObject data = new JSONObject().put("token", "QQBot " + token)
          .put("intents", 1 << 25).put("shard", new JSONArray().put(0).put(1))
          .put("properties", new JSONObject().put("$os", "android")
              .put("$browser", "shiyu").put("$device", "shiyu"));
      if (!webSocket.send(new JSONObject().put("op", 2).put("d", data).toString()))
        throw new IOException("发送鉴权消息失败");
    }

    private void sendResume(WebSocket webSocket) throws Exception {
      JSONObject data = new JSONObject().put("token", "QQBot " + token)
          .put("session_id", sessionId).put("seq", lastSequence);
      if (!webSocket.send(new JSONObject().put("op", 6).put("d", data).toString()))
        throw new IOException("恢复 Gateway 会话失败");
    }

    synchronized void startHeartbeat(int generation, long intervalMs) {
      if (!isCurrent(generation)) return;
      if (heartbeat != null) heartbeat.cancel(false);
      try {
        heartbeat = scheduler.scheduleWithFixedDelay(() -> {
          WebSocket active = socket;
          if (!isCurrent(generation) || active == null) return;
          try {
            CrashProtection.markFeatureActive(QqBotService.this, CrashProtection.FEATURE_QQBOT);
            JSONObject frame = new JSONObject().put("op", 1)
                .put("d", lastSequence == 0 ? JSONObject.NULL : lastSequence);
            if (!active.send(frame.toString()))
              failAndReconnect(generation, new IOException("发送 Gateway 心跳失败"));
          } catch (Exception error) {
            failAndReconnect(generation, error);
          }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
      } catch (RejectedExecutionException error) {
        if (isCurrent(generation)) {
          blockReconnect(generation, new PermanentConnectionException("连接已停止",
              "QQ Bot 后台心跳任务已关闭，请关闭后重新开启此 Bot"));
          recordServiceError(QqBotService.this, error);
        }
      }
    }

    synchronized void reconnectNow(int generation) {
      if (!isCurrent(generation)) return;
      connectionGeneration.incrementAndGet();
      WebSocket active = socket;
      socket = null;
      closeSocket(active, 1001, "reconnect");
      if (heartbeat != null) heartbeat.cancel(false);
      heartbeat = null;
      if (unavailable()) return;
      long delay = Math.min(60_000L, 1_000L << Math.min(6, reconnectAttempts++));
      scheduleConnect(delay);
    }

    synchronized void failAndReconnect(int generation, Throwable error) {
      if (!isCurrent(generation)) return;
      setStatus(account.id, "正在重连", compact(error));
      recordServiceError(QqBotService.this, error);
      reconnectNow(generation);
    }

    synchronized void blockReconnect(int generation, PermanentConnectionException error) {
      if (!isCurrent(generation)) return;
      reconnectBlocked = true;
      connectionGeneration.incrementAndGet();
      if (heartbeat != null) heartbeat.cancel(false);
      if (reconnect != null) reconnect.cancel(false);
      heartbeat = null;
      reconnect = null;
      WebSocket active = socket;
      socket = null;
      token = "";
      closeSocket(active, 1000, "configuration required");
      setStatus(account.id, error.status, error.getMessage());
      recordServiceError(QqBotService.this, error);
    }

    synchronized void stop() {
      if (stopped) return;
      stopped = true;
      connectionGeneration.incrementAndGet();
      if (heartbeat != null) heartbeat.cancel(false);
      if (reconnect != null) reconnect.cancel(false);
      heartbeat = null;
      reconnect = null;
      WebSocket active = socket;
      socket = null;
      token = "";
      closeSocket(active, 1000, "disabled");
    }
  }

  private String fetchToken(String appId, String secret) throws Exception {
    JSONObject payload = new JSONObject().put("appId", appId).put("clientSecret", secret);
    Request request = new Request.Builder().url(TOKEN_URL)
        .post(RequestBody.create(payload.toString(), JSON)).build();
    try (Response response = http.newCall(request).execute()) {
      String raw = response.body() == null ? "" : response.body().string();
      if (!response.isSuccessful()) {
        if (isIpWhitelistError(raw)) throw ipWhitelistException(raw);
        if (response.code() == 400 || response.code() == 401 || response.code() == 403)
          throw new PermanentConnectionException("配置错误", "AppID 或 AppSecret 无效（HTTP "
              + response.code() + "）");
        throw new IOException("Token HTTP " + response.code() + ": " + shorten(raw));
      }
      String token = new JSONObject(raw).optString("access_token").trim();
      if (token.isEmpty()) throw new IOException("Token 接口未返回 access_token");
      return token;
    }
  }

  private String fetchGateway(String token) throws Exception {
    Request request = new Request.Builder().url(API_BASE + "/gateway")
        .header("Authorization", "QQBot " + token).get().build();
    try (Response response = http.newCall(request).execute()) {
      String raw = response.body() == null ? "" : response.body().string();
      if (!response.isSuccessful()) {
        if (isIpWhitelistError(raw)) throw ipWhitelistException(raw);
        throw new IOException("Gateway HTTP " + response.code() + ": " + shorten(raw));
      }
      String url = new JSONObject(raw).optString("url").trim();
      if (!url.startsWith("wss://")) throw new IOException("Gateway 未返回有效 WebSocket 地址");
      return url;
    }
  }

  private void dispatchMessage(QqBotClient.Account account, JSONObject data, boolean group) {
    String messageId = data.optString("id").trim();
    if (messageId.isEmpty()) return;
    String identity = account.id + ':' + messageId;
    if (seen(account.id, messageId) || !processing.add(identity)) return;
    Runnable task = () -> {
      try {
        processMessage(account, data, group, messageId);
      } catch (PermanentConnectionException ignored) {
        // The session already exposes the actionable configuration error.
      } catch (Exception error) {
        setStatus(account.id, "消息处理失败", compact(error));
        recordServiceError(this, error);
      } finally {
        processing.remove(identity);
      }
    };
    try {
      workers.execute(task);
    } catch (RejectedExecutionException error) {
      processing.remove(identity);
      if (!destroyed) recordServiceError(this, error);
    }
  }

  private void processMessage(QqBotClient.Account snapshot, JSONObject data, boolean group,
      String messageId) throws Exception {
    QqBotClient.Account account = QqBotClient.account(this, snapshot.id);
    if (account == null || !account.enabled || !account.signature().equals(snapshot.signature())) return;
    String content = data.optString("content").replaceAll("<@!?[^>]+>", "").trim();
    if (content.isEmpty()) {
      markSeen(account.id, messageId);
      return;
    }
    JSONObject author = data.optJSONObject("author");
    String openId = group ? data.optString("group_openid")
        : author == null ? "" : author.optString("user_openid");
    if (openId.isEmpty() && author != null) openId = author.optString("member_openid");
    if (openId.isEmpty()) throw new IOException("QQ 消息缺少接收方 OpenID");
    String peer = account.id + (group ? ":group:" : ":user:") + openId;
    String title = group ? "QQ 群聊 · " + account.name : "QQ · " + account.name;

    DesktopPetReplyEngine.Result result = null;
    Exception last = null;
    for (int attempt = 0; attempt < 4 && result == null; attempt++) {
      try {
        result = DesktopPetReplyEngine.reply(this, account.cardId, content, "qqbot", peer,
            title, account.chatId);
      } catch (Exception error) {
        last = error;
        if (attempt == 3 || !compact(error).contains("其他回复正在生成")) break;
        Thread.sleep(1_500L * (attempt + 1));
      }
    }
    if (result == null) throw last == null ? new IOException("角色回复失败") : last;
    if (result.text.trim().isEmpty()) {
      markSeen(account.id, messageId);
      setStatus(account.id, "已连接", "");
      return;
    }
    Exception sendError = null;
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        sendReply(account.id, group, openId, messageId, result.text);
        sendError = null;
        break;
      } catch (Exception error) {
        sendError = error;
        if (error instanceof PermanentConnectionException) break;
        if (attempt < 2) Thread.sleep(1_200L * (attempt + 1));
      }
    }
    if (sendError != null) throw sendError;
    markSeen(account.id, messageId);
    prefs.edit().putLong("qqbot_last_message_at", System.currentTimeMillis()).apply();
    setStatus(account.id, "已连接", "");
  }

  private void sendReply(String accountId, boolean group, String openId, String messageId,
      String content) throws Exception {
    BotSession session;
    synchronized (this) { session = sessions.get(accountId); }
    if (session == null || session.token.isEmpty()) throw new IOException("QQ Bot 尚未连接");
    String route = group ? "/v2/groups/" : "/v2/users/";
    String url = API_BASE + route + Uri.encode(openId) + "/messages";
    JSONObject payload = new JSONObject().put("content", content).put("msg_type", 0)
        .put("msg_id", messageId).put("msg_seq", session.messageSequence.incrementAndGet());
    Request request = new Request.Builder().url(url)
        .header("Authorization", "QQBot " + session.token)
        .post(RequestBody.create(payload.toString(), JSON)).build();
    try (Response response = http.newCall(request).execute()) {
      String raw = response.body() == null ? "" : response.body().string();
      if (!response.isSuccessful()) {
        if (isIpWhitelistError(raw)) {
          PermanentConnectionException error = ipWhitelistException(raw);
          session.blockReconnect(session.connectionGeneration.get(), error);
          throw error;
        }
        throw new IOException("发送消息 HTTP " + response.code() + ": " + shorten(raw));
      }
    }
  }

  private boolean seen(String accountId, String messageId) {
    return prefs.getStringSet("qqbot_seen_" + accountId, Collections.emptySet()).contains(messageId);
  }

  private void markSeen(String accountId, String messageId) {
    String key = "qqbot_seen_" + accountId;
    Set<String> values = new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
    values.add(messageId);
    while (values.size() > 256) values.remove(values.iterator().next());
    prefs.edit().putStringSet(key, values).apply();
  }

  private void setStatus(String id, String status, String error) {
    if (destroyed || prefs == null) return;
    prefs.edit().putString(QqBotClient.statusKey(id), status)
        .putString(QqBotClient.errorKey(id), shorten(error)).apply();
    QqBotClient.notifyState(this);
  }

  private Notification notification(String text) {
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26 && manager != null) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "QQ Bot 角色连接",
          NotificationManager.IMPORTANCE_LOW);
      channel.setShowBadge(false);
      manager.createNotificationChannel(channel);
    }
    PendingIntent pending = PendingIntent.getActivity(this, NOTIFICATION_ID,
        new Intent(this, ComposeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
    return builder.setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle("时遇 QQ Bot")
        .setContentText(text).setContentIntent(pending).setOnlyAlertOnce(true).setOngoing(true)
        .build();
  }

  private static void recordServiceError(Context context, Throwable error) {
    if (error instanceof InterruptedException || MainActivity.isDataRescueRequired(context)) return;
    String detail = compact(error);
    DiagnosticLog.record(context, "qqbot", "error", detail);
    try {
      app(context).getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE).edit()
          .putString("qqbot_service_error", detail).apply();
      QqBotClient.notifyState(context);
    } catch (RuntimeException ignored) {}
  }

  private static String compact(Throwable error) {
    if (error == null) return "QQ Bot 后台服务异常";
    String message = error.getMessage();
    String value = error.getClass().getSimpleName()
        + (message == null || message.trim().isEmpty() ? "" : ": " + message.trim());
    return shorten(value);
  }

  private static String shorten(String value) {
    if (value == null) return "";
    String clean = value.replace('\r', ' ').replace('\n', ' ').trim();
    return clean.length() <= 600 ? clean : clean.substring(0, 600);
  }

  private static String responseBody(Response response) {
    if (response == null || response.body() == null) return "";
    try {
      return response.body().string();
    } catch (Exception ignored) {
      return "";
    }
  }

  private static boolean isIpWhitelistError(String raw) {
    if (raw == null || raw.trim().isEmpty()) return false;
    try {
      JSONObject value = new JSONObject(raw);
      if (value.optLong("code", -1L) == 11298L
          || value.optLong("err_code", -1L) == 40023002L) return true;
    } catch (Exception ignored) {}
    String lower = raw.toLowerCase(Locale.ROOT);
    boolean mentionsIp = lower.contains("ip");
    boolean mentionsWhitelist = raw.contains("白名单") || raw.contains("白名單")
        || lower.contains("whitelist") || lower.contains("allowlist")
        || lower.contains("not allowed") || lower.contains("not permitted");
    return mentionsIp && mentionsWhitelist;
  }

  private static PermanentConnectionException ipWhitelistException(String raw) {
    Matcher matcher = IPV4.matcher(raw == null ? "" : raw);
    String ip = matcher.find() ? "（" + matcher.group() + "）" : "";
    return new PermanentConnectionException("IP 白名单未配置",
        "当前出口 IP" + ip + " 未加入 QQ 开放平台 IP 白名单。请前往 q.qq.com 的机器人开发设置，"
            + "将该 IP 加入白名单后，再关闭并重新开启此 Bot");
  }

  private static Context app(Context context) {
    Context value = context.getApplicationContext();
    return value == null ? context : value;
  }

  private void markDisabledSessionsPaused() {
    if (prefs == null) return;
    try {
      SharedPreferences.Editor editor = prefs.edit();
      boolean changed = false;
      for (BotSession session : sessions.values()) {
        QqBotClient.Account current = QqBotClient.account(this, session.account.id);
        if (current != null && !current.enabled) {
          editor.putString(QqBotClient.statusKey(current.id), "已暂停")
              .remove(QqBotClient.errorKey(current.id));
          changed = true;
        }
      }
      if (changed) {
        editor.apply();
        QqBotClient.notifyState(this);
      }
    } catch (RuntimeException ignored) {}
  }

  private static void cleanupHttpAsync(OkHttpClient client) {
    if (client == null) return;
    Thread cleanup = new Thread(() -> {
      try { client.dispatcher().cancelAll(); } catch (Throwable ignored) {}
      try { client.connectionPool().evictAll(); } catch (Throwable ignored) {}
      try { client.dispatcher().executorService().shutdown(); } catch (Throwable ignored) {}
    }, "shiyu-qqbot-http-cleanup");
    try {
      cleanup.setDaemon(true);
      cleanup.start();
    } catch (Throwable ignored) {}
  }

  @Override public synchronized void onDestroy() {
    destroyed = true;
    foregroundReady = false;
    markDisabledSessionsPaused();
    for (BotSession session : sessions.values()) {
      try { session.stop(); } catch (Throwable ignored) {}
    }
    sessions.clear();
    processing.clear();
    try { workers.shutdownNow(); } catch (RuntimeException ignored) {}
    try { scheduler.shutdownNow(); } catch (RuntimeException ignored) {}
    cleanupHttpAsync(http);
    CrashProtection.clearFeatureActive(this, CrashProtection.FEATURE_QQBOT);
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }

  private static final class PermanentConnectionException extends Exception {
    final String status;

    PermanentConnectionException(String status, String message) {
      super(message);
      this.status = status;
    }
  }
}
