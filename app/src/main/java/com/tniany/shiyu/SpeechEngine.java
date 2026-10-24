package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

final class SpeechEngine {
  interface Listener { void onState(boolean speaking, String text, String error); }

  private final Context context;
  private final SharedPreferences prefs;
  private final SecureStore secure;
  private final Listener listener;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private TextToSpeech local;
  private MediaPlayer player;
  private boolean localReady;
  private boolean localInitializing;
  private String initializedEngine = "";
  private String pendingLocal = "";
  private int pendingLocalToken;
  private volatile String activeUtteranceId = "";
  private volatile boolean speaking;
  private volatile String currentText = "";
  private volatile int generation;

  SpeechEngine(Context context, SharedPreferences prefs, SecureStore secure, Listener listener) {
    this.context = context.getApplicationContext();
    this.prefs = prefs;
    this.secure = secure;
    this.listener = listener;
  }

  boolean isSpeaking(String text) { return speaking && currentText.equals(clean(text)); }
  boolean isSpeaking() { return speaking; }

  void speak(String raw) {
    String text = clean(raw);
    if (text.isEmpty()) return;
    stopInternal(false);
    int token = ++generation;
    currentText = text;
    state(true, "");
    if ("online".equals(prefs.getString("tts_provider", "local"))) speakOnline(text, token);
    else speakLocal(text, token);
  }

  void stop() {
    stopInternal(true);
  }

  private void stopInternal(boolean notify) {
    generation++;
    pendingLocal = "";
    pendingLocalToken = 0;
    activeUtteranceId = "";
    if (local != null) local.stop();
    if (player != null) {
      try { player.stop(); } catch (Exception ignored) {}
      player.release();
      player = null;
    }
    if (speaking) {
      if (notify) state(false, "");
      else speaking = false;
    }
  }

  void release() {
    stop();
    if (local != null) local.shutdown();
    local = null;
    executor.shutdownNow();
  }

  void resetLocal() {
    pendingLocal = "";
    pendingLocalToken = 0;
    activeUtteranceId = "";
    localReady = false;
    localInitializing = false;
    initializedEngine = "";
    if (local != null) local.shutdown();
    local = null;
  }

  private void speakLocal(String text, int token) {
    String configuredEngine = prefs.getString("tts_engine", "");
    if ("auto_xiaomi".equals(configuredEngine)) {
      configuredEngine = "";
      prefs.edit().putString("tts_engine", "").apply();
    }
    if (localReady && local != null && configuredEngine.equals(initializedEngine)) {
      speakReadyLocal(text, token);
      return;
    }
    if (localInitializing) { pendingLocal = text; pendingLocalToken = token; return; }
    resetLocal();
    pendingLocal = text;
    pendingLocalToken = token;
    initializeLocal(configuredEngine, false);
  }

  private void initializeLocal(String engine, boolean fallbackAttempt) {
    localInitializing = true;
    initializedEngine = engine == null ? "" : engine;
    TextToSpeech.OnInitListener callback = status -> main.post(() -> finishLocalInitialization(status, fallbackAttempt));
    try {
      local = initializedEngine.isEmpty() ? new TextToSpeech(context, callback) : new TextToSpeech(context, callback, initializedEngine);
    } catch (Exception error) {
      finishLocalInitialization(TextToSpeech.ERROR, fallbackAttempt);
    }
  }

  private void finishLocalInitialization(int status, boolean fallbackAttempt) {
    localInitializing = false;
    if (status == TextToSpeech.SUCCESS && local != null) {
      localReady = true;
      local.setOnUtteranceProgressListener(new UtteranceProgressListener() {
        public void onStart(String id) { if (id.equals(activeUtteranceId)) state(true, ""); }
        public void onDone(String id) { if (id.equals(activeUtteranceId)) { activeUtteranceId = ""; state(false, ""); } }
        public void onError(String id) { if (id.equals(activeUtteranceId)) { activeUtteranceId = ""; state(false, "本地语音播放失败"); } }
      });
      local.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
      String value = pendingLocal;
      int token = pendingLocalToken;
      pendingLocal = "";
      pendingLocalToken = 0;
      if (!value.isEmpty() && token == generation) speakReadyLocal(value, token);
      return;
    }
    if (local != null) local.shutdown();
    local = null;
    localReady = false;
    String fallback = firstAvailableEngine(initializedEngine);
    if (!fallbackAttempt && !fallback.isEmpty()) {
      prefs.edit().putString("tts_engine", fallback).apply();
      initializeLocal(fallback, true);
    } else state(false, "未检测到可用的本地语音引擎，请在系统 TTS 设置中安装语音包");
  }

  private void speakReadyLocal(String text, int token) {
    if (token != generation || local == null) return;
    applyLocalSettings();
    String utteranceId = "shiyu-" + token + '-' + UUID.randomUUID();
    activeUtteranceId = utteranceId;
    int result = local.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), utteranceId);
    if (result == TextToSpeech.ERROR) {
      activeUtteranceId = "";
      state(false, "本地语音引擎拒绝播放，请检查语音包");
    }
  }

  private void applyLocalSettings() {
    String tag = prefs.getString("tts_locale", "zh-CN");
    int result = local.setLanguage(Locale.forLanguageTag(tag));
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
      result = local.setLanguage(Locale.CHINESE);
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
      local.setLanguage(Locale.getDefault());
    String voiceName = prefs.getString("tts_local_voice", "");
    if (!voiceName.isEmpty() && local.getVoices() != null)
      for (android.speech.tts.Voice voice : local.getVoices()) if (voice.getName().equals(voiceName)) { local.setVoice(voice); break; }
    local.setSpeechRate(Float.intBitsToFloat(prefs.getInt("tts_rate", Float.floatToIntBits(1f))));
    local.setPitch(Float.intBitsToFloat(prefs.getInt("tts_pitch", Float.floatToIntBits(1f))));
  }

  private String firstAvailableEngine(String excluded) {
    Intent intent = new Intent("android.intent.action.TTS_SERVICE");
    for (ResolveInfo service : context.getPackageManager().queryIntentServices(intent, 0)) {
      String name = service.serviceInfo == null ? "" : service.serviceInfo.packageName;
      if (!name.isEmpty() && !name.equals(excluded)) return name;
    }
    return "";
  }

  private void speakOnline(String text, int token) {
    executor.execute(() -> {
      File audio = null;
      try {
        String endpoint = prefs.getString("tts_endpoint", "https://api.openai.com/v1/audio/speech").trim();
        while (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        if (endpoint.endsWith("/v1")) endpoint += "/audio/speech";
        else if (!endpoint.endsWith("/audio/speech")) endpoint += "/audio/speech";
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(90_000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        String key = secure.getNamed("tts_api_key_encrypted");
        if (!key.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + key);
        JSONObject body = new JSONObject()
            .put("model", prefs.getString("tts_model", "gpt-4o-mini-tts"))
            .put("voice", prefs.getString("tts_voice", "alloy"))
            .put("input", text)
            .put("response_format", "mp3")
            .put("speed", Float.intBitsToFloat(prefs.getInt("tts_rate", Float.floatToIntBits(1f))));
        try (OutputStream output = connection.getOutputStream()) {
          output.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = connection.getResponseCode();
        byte[] bytes = readBytes(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (code < 200 || code >= 300) throw new IllegalStateException("在线语音 HTTP " + code + "：" + new String(bytes, StandardCharsets.UTF_8));
        File dir = new File(context.getCacheDir(), "tts_audio");
        dir.mkdirs();
        audio = new File(dir, "speech_" + System.currentTimeMillis() + ".mp3");
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(audio)) { output.write(bytes); }
        File ready = audio;
        main.post(() -> {
          if (token == generation && speaking) play(ready);
          else ready.delete();
        });
      } catch (Exception error) {
        if (audio != null) audio.delete();
        if (token == generation)
          state(false, error.getMessage() == null ? "在线语音生成失败" : error.getMessage());
      }
    });
  }

  private void play(File file) {
    try {
      player = new MediaPlayer();
      player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
      player.setDataSource(file.getAbsolutePath());
      player.setOnPreparedListener(MediaPlayer::start);
      player.setOnCompletionListener(value -> {
        if (player != value) { value.release(); file.delete(); return; }
        value.release(); player = null; file.delete(); state(false, "");
      });
      player.setOnErrorListener((value, what, extra) -> {
        if (player != value) { value.release(); file.delete(); return true; }
        value.release(); player = null; file.delete(); state(false, "在线语音播放失败"); return true;
      });
      player.prepareAsync();
    } catch (Exception error) {
      file.delete();
      state(false, "在线语音播放失败");
    }
  }

  private void state(boolean value, String error) {
    speaking = value;
    if (!value && error.isEmpty()) currentText = "";
    main.post(() -> listener.onState(value, currentText, error));
  }

  private static String clean(String value) {
    return value.replaceAll("```[\\s\\S]*?```", " ")
        .replaceAll("<[^>]+>", " ").replaceAll("[*_#>`~]", "")
        .replaceAll("\\s+", " ").trim();
  }

  private static byte[] readBytes(InputStream input) throws Exception {
    try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = source.read(buffer)) > 0) output.write(buffer, 0, count);
      return output.toByteArray();
    }
  }
}
