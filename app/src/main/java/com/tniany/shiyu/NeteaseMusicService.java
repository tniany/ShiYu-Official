package com.tniany.shiyu;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Foreground music playback that keeps the canonical song URL separate from the full stream URL. */
public final class NeteaseMusicService extends Service {
  static final String ACTION_STATE = "com.tniany.shiyu.MUSIC_STATE";
  static final String ACTION_PLAY = "com.tniany.shiyu.MUSIC_PLAY";
  static final String ACTION_TOGGLE = "com.tniany.shiyu.MUSIC_TOGGLE";
  static final String ACTION_SEEK = "com.tniany.shiyu.MUSIC_SEEK";
  static final String ACTION_STOP = "com.tniany.shiyu.MUSIC_STOP";
  static final String EXTRA_URL = "url";
  static final String EXTRA_STREAM_URL = "streamUrl";
  static final String EXTRA_TITLE = "title";
  static final String EXTRA_ARTIST = "artist";
  static final String EXTRA_ALBUM = "album";
  static final String EXTRA_COVER_URL = "coverUrl";
  static final String EXTRA_SONG_ID = "songId";
  static final String EXTRA_ROLE_ID = "roleId";
  static final String EXTRA_ROLE_NAME = "roleName";
  static final String EXTRA_CHAT_ID = "chatId";
  static final String EXTRA_PLAYBACK_ID = "playbackId";
  static final String EXTRA_STATUS = "status";
  static final String EXTRA_ERROR = "error";
  static final String EXTRA_POSITION = "position";
  static final String EXTRA_DURATION = "duration";
  static final String EXTRA_COMPLETED = "completed";
  static final String EXTRA_SEEK_POSITION = "seekPosition";
  static final String STATUS_IDLE = "idle";
  static final String STATUS_LOADING = "loading";
  static final String STATUS_PLAYING = "playing";
  static final String STATUS_PAUSED = "paused";
  static final String STATUS_ERROR = "error";

  private static final String CHANNEL = "shiyu_music_playback";
  private static final int NOTICE = 7319;

  private MediaPlayer player;
  private String url = "";
  private String streamUrl = "";
  private String title = "";
  private String artist = "";
  private String album = "";
  private String coverUrl = "";
  private String roleId = "";
  private String roleName = "";
  private String chatId = "";
  private String playbackId = "";
  private String status = STATUS_IDLE;
  private String error = "";
  private long songId;
  private int generation;
  private AudioManager audioManager;
  private AudioFocusRequest audioFocusRequest;
  private boolean resumeAfterFocusLoss;
  private final Handler progressHandler = new Handler(Looper.getMainLooper());
  private final Runnable progressTicker = () -> {
    if (player != null && player.isPlaying() && STATUS_PLAYING.equals(status)) publish(STATUS_PLAYING, "");
  };
  private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
    if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
      if (player != null && player.isPlaying()) {
        try { resumeAfterFocusLoss = change != AudioManager.AUDIOFOCUS_LOSS; player.pause(); publish(STATUS_PAUSED, ""); } catch (Exception ignored) { }
      }
    } else if (change == AudioManager.AUDIOFOCUS_GAIN && resumeAfterFocusLoss && player != null && STATUS_PAUSED.equals(status)) {
      try { resumeAfterFocusLoss = false; player.start(); publish(STATUS_PLAYING, ""); } catch (Exception ignored) { }
    }
  };

  static void play(Context context, String url, String title, String artist, String album, long songId,
                   String roleId, String roleName, String coverUrl, String streamUrl,
                   String chatId, String playbackId) {
    Intent intent = new Intent(context, NeteaseMusicService.class).setAction(ACTION_PLAY)
        .putExtra(EXTRA_URL, url).putExtra(EXTRA_STREAM_URL, streamUrl)
        .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_ARTIST, artist).putExtra(EXTRA_ALBUM, album)
        .putExtra(EXTRA_COVER_URL, coverUrl).putExtra(EXTRA_SONG_ID, songId)
        .putExtra(EXTRA_ROLE_ID, roleId).putExtra(EXTRA_ROLE_NAME, roleName)
        .putExtra(EXTRA_CHAT_ID, chatId).putExtra(EXTRA_PLAYBACK_ID, playbackId);
    ContextCompat.startForegroundService(context, intent);
  }

  static void toggle(Context context) {
    ContextCompat.startForegroundService(context, new Intent(context, NeteaseMusicService.class).setAction(ACTION_TOGGLE));
  }

  static void seek(Context context, long positionMs) {
    ContextCompat.startForegroundService(context, new Intent(context, NeteaseMusicService.class).setAction(ACTION_SEEK).putExtra(EXTRA_SEEK_POSITION, Math.max(0L, positionMs)));
  }

  static void stop(Context context) {
    context.stopService(new Intent(context, NeteaseMusicService.class));
  }

  @Override public void onCreate() {
    super.onCreate();
    createChannel();
    audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    String action = intent == null ? "" : intent.getAction();
    if (ACTION_PLAY.equals(action) || ACTION_TOGGLE.equals(action) || ACTION_SEEK.equals(action))
      startForeground(NOTICE, notification());
    if (ACTION_PLAY.equals(action)) startTrack(intent);
    else if (ACTION_TOGGLE.equals(action)) toggleInternal();
    else if (ACTION_SEEK.equals(action)) seekInternal(intent.getLongExtra(EXTRA_SEEK_POSITION, 0L));
    else if (ACTION_STOP.equals(action)) stopInternal(true);
    if (player == null) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
    return START_NOT_STICKY;
  }

  private void startTrack(Intent intent) {
    String requestedId = getSharedPreferences(MainActivity.PREF, MODE_PRIVATE).getString("music_requested_playback_id", "");
    if (!clean(intent.getStringExtra(EXTRA_PLAYBACK_ID), "").equals(requestedId)) return;
    String next = intent.getStringExtra(EXTRA_URL);
    if (next == null || next.trim().isEmpty()) { publish(STATUS_ERROR, "音乐链接为空"); return; }
    String nextChatId = clean(intent.getStringExtra(EXTRA_CHAT_ID), "");
    String nextRoleId = nextChatId.isEmpty() ? "" : clean(intent.getStringExtra(EXTRA_ROLE_ID), "");
    String nextPlaybackId = clean(intent.getStringExtra(EXTRA_PLAYBACK_ID), "");
    if (next.equals(url) && player != null && chatId.equals(nextChatId)
        && roleId.equals(nextRoleId) && !nextPlaybackId.isEmpty() && playbackId.equals(nextPlaybackId)) {
      if (STATUS_PAUSED.equals(status)) toggleInternal();
      return;
    }
    releasePlayer();
    url = next.trim();
    streamUrl = clean(intent.getStringExtra(EXTRA_STREAM_URL), url);
    title = clean(intent.getStringExtra(EXTRA_TITLE), "分享的音乐");
    artist = clean(intent.getStringExtra(EXTRA_ARTIST), "未知歌手");
    album = clean(intent.getStringExtra(EXTRA_ALBUM), "");
    coverUrl = clean(intent.getStringExtra(EXTRA_COVER_URL), "");
    songId = intent.getLongExtra(EXTRA_SONG_ID, 0);
    chatId = nextChatId;
    playbackId = nextPlaybackId;
    roleId = nextRoleId;
    roleName = roleId.isEmpty() ? "" : clean(intent.getStringExtra(EXTRA_ROLE_NAME), "");
    int token = ++generation;
    publish(STATUS_LOADING, "");
    startForeground(NOTICE, notification());
    MediaPlayer nextPlayer = new MediaPlayer();
    player = nextPlayer;
    try {
      if (audioManager != null) {
        if (Build.VERSION.SDK_INT >= 26) {
          audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
              .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
              .setOnAudioFocusChangeListener(focusListener).build();
          audioManager.requestAudioFocus(audioFocusRequest);
        } else audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      }
      nextPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
      nextPlayer.setWakeMode(getApplicationContext(), android.os.PowerManager.PARTIAL_WAKE_LOCK);
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 ShiYu/" + BuildConfig.VERSION_NAME);
      headers.put("Referer", "https://music.163.com/");
      String cookie = new SecureStore(this).getNamed("netease_music_cookie").trim();
      String streamHost = Uri.parse(streamUrl).getHost();
      if (!cookie.isEmpty() && streamHost != null
          && ("music.163.com".equalsIgnoreCase(streamHost)
              || streamHost.toLowerCase(java.util.Locale.ROOT).endsWith(".music.163.com")))
        headers.put("Cookie", cookie);
      nextPlayer.setDataSource(this, Uri.parse(streamUrl), headers);
      nextPlayer.setOnPreparedListener(ready -> {
        if (token != generation || player != ready) { ready.release(); return; }
        try { ready.start(); publish(STATUS_PLAYING, ""); } catch (Exception e) { publish(STATUS_ERROR, message(e)); releasePlayer(); }
      });
      nextPlayer.setOnCompletionListener(done -> {
        if (token == generation && player == done) {
          publish(STATUS_IDLE, "", true); releasePlayer(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
        }
      });
      nextPlayer.setOnErrorListener((failed, what, extra) -> {
        if (token == generation && player == failed) { publish(STATUS_ERROR, "音频源无法播放（" + what + "/" + extra + "）"); releasePlayer(); }
        return true;
      });
      nextPlayer.prepareAsync();
    } catch (Exception e) { publish(STATUS_ERROR, message(e)); releasePlayer(); }
  }

  private void toggleInternal() {
    if (STATUS_LOADING.equals(status)) return;
    resumeAfterFocusLoss = false;
    if (player == null) { if (!url.isEmpty()) publish(STATUS_ERROR, "播放器已释放，请重新选择歌曲"); return; }
    try { if (player.isPlaying()) { player.pause(); publish(STATUS_PAUSED, ""); } else { player.start(); publish(STATUS_PLAYING, ""); } }
    catch (Exception e) { publish(STATUS_ERROR, message(e)); }
  }

  private void seekInternal(long positionMs) {
    if (player == null) return;
    try {
      int duration = Math.max(0, player.getDuration());
      player.seekTo((int) Math.min(Math.max(0L, positionMs), duration));
      publish(player.isPlaying() ? STATUS_PLAYING : STATUS_PAUSED, "");
    } catch (Exception e) { publish(STATUS_ERROR, message(e)); }
  }

  private void stopInternal(boolean stopService) {
    publish(STATUS_IDLE, ""); releasePlayer(); stopForeground(STOP_FOREGROUND_REMOVE); if (stopService) stopSelf();
  }

  private void releasePlayer() {
    resumeAfterFocusLoss = false;
    generation++;
    progressHandler.removeCallbacks(progressTicker);
    MediaPlayer old = player; player = null;
    if (old != null) {
      try { old.setOnPreparedListener(null); old.setOnCompletionListener(null); old.setOnErrorListener(null); old.stop(); } catch (Exception ignored) { }
      try { old.release(); } catch (Exception ignored) { }
    }
    if (audioManager != null) {
      try { if (Build.VERSION.SDK_INT >= 26 && audioFocusRequest != null) audioManager.abandonAudioFocusRequest(audioFocusRequest); else audioManager.abandonAudioFocus(focusListener); } catch (Exception ignored) { }
    }
  }

  private void publish(String nextStatus, String nextError) {
    publish(nextStatus, nextError, false);
  }

  private void publish(String nextStatus, String nextError, boolean completed) {
    status = nextStatus; error = nextError == null ? "" : nextError;
    // A stopped or superseded player must not replace the new session's state.
    if (!playbackId.equals(getSharedPreferences(MainActivity.PREF, MODE_PRIVATE).getString("music_requested_playback_id", ""))) return;
    long position = 0, duration = 0;
    try { if (player != null) { position = Math.max(0, player.getCurrentPosition()); duration = Math.max(0, player.getDuration()); } } catch (Exception ignored) { }
    JSONObject state = new JSONObject();
    try { state.put(EXTRA_STATUS, status).put(EXTRA_ERROR, error).put(EXTRA_URL, url).put(EXTRA_TITLE, title).put(EXTRA_ARTIST, artist).put(EXTRA_ALBUM, album).put(EXTRA_COVER_URL, coverUrl).put(EXTRA_SONG_ID, songId).put(EXTRA_ROLE_ID, roleId).put(EXTRA_ROLE_NAME, roleName).put(EXTRA_CHAT_ID, chatId).put(EXTRA_PLAYBACK_ID, playbackId).put(EXTRA_POSITION, position).put(EXTRA_DURATION, duration); } catch (Exception ignored) { }
    getSharedPreferences(MainActivity.PREF, MODE_PRIVATE).edit().putString("music_playback_state", state.toString()).apply();
    sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName()).putExtra(EXTRA_STATUS, status).putExtra(EXTRA_ERROR, error).putExtra(EXTRA_COMPLETED, completed).putExtra(EXTRA_URL, url).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_ARTIST, artist).putExtra(EXTRA_ALBUM, album).putExtra(EXTRA_COVER_URL, coverUrl).putExtra(EXTRA_SONG_ID, songId).putExtra(EXTRA_ROLE_ID, roleId).putExtra(EXTRA_ROLE_NAME, roleName).putExtra(EXTRA_CHAT_ID, chatId).putExtra(EXTRA_PLAYBACK_ID, playbackId).putExtra(EXTRA_POSITION, position).putExtra(EXTRA_DURATION, duration));
    if (!STATUS_IDLE.equals(status)) { NotificationManager manager = getSystemService(NotificationManager.class); if (manager != null) manager.notify(NOTICE, notification()); }
    progressHandler.removeCallbacks(progressTicker);
    if (STATUS_PLAYING.equals(status) && player != null && player.isPlaying()) progressHandler.postDelayed(progressTicker, 500);
  }

  private Notification notification() {
    Intent open = new Intent(this, ComposeActivity.class).putExtra("open_music", true)
        .putExtra(EXTRA_CHAT_ID, chatId).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent content = PendingIntent.getActivity(this, 21, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    PendingIntent toggle = PendingIntent.getService(this, 22, new Intent(this, NeteaseMusicService.class).setAction(ACTION_TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    PendingIntent stop = PendingIntent.getService(this, 23, new Intent(this, NeteaseMusicService.class).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    boolean playing = STATUS_PLAYING.equals(status);
    String line = chatId.isEmpty() || roleName.isEmpty() ? artist : "和 " + roleName + " 一起听 · " + artist;
    return new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title.isEmpty() ? "时遇音乐" : title).setContentText(line).setContentIntent(content).setOngoing(playing || STATUS_LOADING.equals(status)).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, playing ? "暂停" : "继续", toggle).addAction(android.R.drawable.ic_menu_close_clear_cancel, "结束", stop).build();
  }

  private void createChannel() { if (Build.VERSION.SDK_INT >= 26) { NotificationChannel c = new NotificationChannel(CHANNEL, "音乐播放", NotificationManager.IMPORTANCE_LOW); c.setDescription("时遇后台音乐播放控制"); NotificationManager m = getSystemService(NotificationManager.class); if (m != null) m.createNotificationChannel(c); } }
  private static String clean(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
  private static String message(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
  @Override public void onDestroy() {
    publish(STATUS_IDLE, "");
    releasePlayer();
    stopForeground(STOP_FOREGROUND_REMOVE);
    super.onDestroy();
  }
  @Override public IBinder onBind(Intent intent) { return null; }
}
