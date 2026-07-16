package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

final class BackupManager {
  private static final String KIND = "shiyu-backup";
  private static final Set<String> TRANSIENT_PREFS =
      new java.util.HashSet<>(
          java.util.Arrays.asList(
              "app_in_foreground", "visible_chat_id", "active_generation_until", "background_state_revision"));

  private BackupManager() {}

  static byte[] create(
      Context context, AppDb db, SharedPreferences prefs, SecureStore secure, String password)
      throws Exception {
    String stateText = db.loadState();
    JSONObject state = new JSONObject(stateText.isEmpty() ? "{}" : stateText);
    JSONObject preferences = encodePreferences(prefs);
    JSONObject assets = new JSONObject();
    collectAssets(context, state, preferences, assets);
    JSONObject root =
        new JSONObject()
            .put("kind", KIND)
            .put("format", 1)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("createdAt", System.currentTimeMillis())
            .put("state", state)
            .put("memories", db.exportMemories())
            .put("realtimeLibrary", db.exportRealtimeLibrary())
            .put("preferences", preferences)
            .put("secure", secure.exportPlain())
            .put("assets", assets);
    byte[] raw = root.toString().getBytes(StandardCharsets.UTF_8);
    return password.trim().isEmpty() ? raw : encrypt(raw, password);
  }

  static void restore(
      Context context,
      AppDb db,
      SharedPreferences prefs,
      SecureStore secure,
      byte[] bytes,
      String password)
      throws Exception {
    JSONObject envelope = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    JSONObject root;
    if ("shiyu-backup-encrypted".equals(envelope.optString("kind"))) {
      if (password.isEmpty()) throw new IllegalArgumentException("该备份已加密，请输入备份密码");
      root = new JSONObject(new String(decrypt(envelope, password), StandardCharsets.UTF_8));
    } else {
      root = envelope;
    }
    if (!KIND.equals(root.optString("kind")) || root.optInt("format") != 1)
      throw new IllegalArgumentException("不是受支持的时遇备份文件");
    JSONObject state = root.getJSONObject("state");
    JSONObject preferences = root.optJSONObject("preferences");
    if (preferences == null) preferences = new JSONObject();
    restoreAssets(context, state, preferences, root.optJSONObject("assets"));
    db.saveState(state.toString());
    db.importMemories(root.optJSONArray("memories") == null ? new JSONArray() : root.getJSONArray("memories"));
    db.importRealtimeLibrary(root.optJSONArray("realtimeLibrary") == null ? new JSONArray() : root.getJSONArray("realtimeLibrary"));
    restorePreferences(prefs, preferences);
    secure.importPlain(root.optJSONObject("secure") == null ? new JSONObject() : root.getJSONObject("secure"));
    prefs.edit().putBoolean("app_in_foreground", true).remove("visible_chat_id").remove("active_generation_until").apply();
  }

  private static JSONObject encodePreferences(SharedPreferences prefs) throws Exception {
    JSONObject result = new JSONObject();
    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
      if (TRANSIENT_PREFS.contains(entry.getKey())) continue;
      Object raw = entry.getValue();
      JSONObject value = new JSONObject();
      if (raw instanceof Boolean) value.put("type", "boolean").put("value", raw);
      else if (raw instanceof Integer) value.put("type", "int").put("value", raw);
      else if (raw instanceof Long) value.put("type", "long").put("value", raw);
      else if (raw instanceof Float) value.put("type", "float").put("value", raw);
      else if (raw instanceof Set) value.put("type", "set").put("value", new JSONArray((Set<?>) raw));
      else value.put("type", "string").put("value", String.valueOf(raw));
      result.put(entry.getKey(), value);
    }
    return result;
  }

  private static void restorePreferences(SharedPreferences prefs, JSONObject values) {
    SharedPreferences.Editor editor = prefs.edit().clear();
    java.util.Iterator<String> names = values.keys();
    while (names.hasNext()) {
      String name = names.next();
      JSONObject item = values.optJSONObject(name);
      if (item == null) continue;
      String type = item.optString("type");
      switch (type) {
        case "boolean": editor.putBoolean(name, item.optBoolean("value")); break;
        case "int": editor.putInt(name, item.optInt("value")); break;
        case "long": editor.putLong(name, item.optLong("value")); break;
        case "float": editor.putFloat(name, (float) item.optDouble("value")); break;
        case "set":
          JSONArray a = item.optJSONArray("value");
          Set<String> set = new java.util.HashSet<>();
          if (a != null) for (int i = 0; i < a.length(); i++) set.add(a.optString(i));
          editor.putStringSet(name, set);
          break;
        default: editor.putString(name, item.optString("value"));
      }
    }
    editor.commit();
  }

  private static void collectAssets(
      Context context, JSONObject state, JSONObject preferences, JSONObject assets) {
    JSONArray cards = state.optJSONArray("cards");
    if (cards != null) for (int i = 0; i < cards.length(); i++) addAsset(context, cards.optJSONObject(i), "avatar", assets);
    JSONArray chats = state.optJSONArray("chats");
    if (chats != null) for (int i = 0; i < chats.length(); i++) {
      JSONArray messages = chats.optJSONObject(i) == null ? null : chats.optJSONObject(i).optJSONArray("messages");
      if (messages != null) for (int j = 0; j < messages.length(); j++) addAsset(context, messages.optJSONObject(j), "image", assets);
    }
    JSONObject avatar = preferences.optJSONObject("user_avatar");
    if (avatar != null) addAsset(context, avatar, "value", assets);
    JSONObject chatBackground = preferences.optJSONObject("chat_background_image");
    if (chatBackground != null) addAsset(context, chatBackground, "value", assets);
    JSONObject momentPreference = preferences.optJSONObject("moments");
    if (momentPreference != null) {
      try {
        JSONArray moments = new JSONArray(momentPreference.optString("value", "[]"));
        for (int i = 0; i < moments.length(); i++) addAsset(context, moments.optJSONObject(i), "image", assets);
      } catch (Exception ignored) {}
    }
  }

  private static void addAsset(Context context, JSONObject owner, String field, JSONObject assets) {
    if (owner == null) return;
    String reference = owner.optString(field);
    if (reference.isEmpty() || reference.startsWith("http") || assets.has(reference)) return;
    try (InputStream input = open(context, reference)) {
      if (input == null) return;
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) > 0) output.write(buffer, 0, read);
      String extension = extension(reference);
      assets.put(reference, new JSONObject().put("extension", extension).put("data", Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)));
    } catch (Exception ignored) {
      // Unreadable external files are skipped; the rest of the backup remains usable.
    }
  }

  private static InputStream open(Context context, String reference) throws Exception {
    if (reference.startsWith("content://")) return context.getContentResolver().openInputStream(Uri.parse(reference));
    File file = new File(reference);
    return file.isFile() ? new FileInputStream(file) : null;
  }

  private static void restoreAssets(
      Context context, JSONObject state, JSONObject preferences, JSONObject assets) throws Exception {
    if (assets == null) return;
    File directory = new File(context.getFilesDir(), "restored_assets");
    if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("无法创建备份资源目录");
    Map<String, String> replacements = new HashMap<>();
    java.util.Iterator<String> names = assets.keys();
    while (names.hasNext()) {
      String original = names.next();
      JSONObject item = assets.optJSONObject(original);
      if (item == null) continue;
      File target = new File(directory, UUID.randomUUID() + item.optString("extension", ".bin"));
      try (FileOutputStream output = new FileOutputStream(target)) {
        output.write(Base64.decode(item.getString("data"), Base64.DEFAULT));
      }
      replacements.put(original, target.getAbsolutePath());
    }
    replaceStateReferences(state, replacements);
    JSONObject avatar = preferences.optJSONObject("user_avatar");
    if (avatar != null && replacements.containsKey(avatar.optString("value")))
      avatar.put("value", replacements.get(avatar.optString("value")));
    JSONObject chatBackground = preferences.optJSONObject("chat_background_image");
    if (chatBackground != null && replacements.containsKey(chatBackground.optString("value")))
      chatBackground.put("value", replacements.get(chatBackground.optString("value")));
    JSONObject momentPreference = preferences.optJSONObject("moments");
    if (momentPreference != null) {
      try {
        JSONArray moments = new JSONArray(momentPreference.optString("value", "[]"));
        for (int i = 0; i < moments.length(); i++) replace(moments.optJSONObject(i), "image", replacements);
        momentPreference.put("value", moments.toString());
      } catch (Exception ignored) {}
    }
  }

  private static void replaceStateReferences(JSONObject state, Map<String, String> replacements) throws Exception {
    JSONArray cards = state.optJSONArray("cards");
    if (cards != null) for (int i = 0; i < cards.length(); i++) replace(cards.optJSONObject(i), "avatar", replacements);
    JSONArray chats = state.optJSONArray("chats");
    if (chats != null) for (int i = 0; i < chats.length(); i++) {
      JSONArray messages = chats.optJSONObject(i) == null ? null : chats.optJSONObject(i).optJSONArray("messages");
      if (messages != null) for (int j = 0; j < messages.length(); j++) replace(messages.optJSONObject(j), "image", replacements);
    }
  }

  private static void replace(JSONObject owner, String field, Map<String, String> replacements) throws Exception {
    if (owner != null && replacements.containsKey(owner.optString(field))) owner.put(field, replacements.get(owner.optString(field)));
  }

  private static String extension(String value) {
    String clean = value.split("[?#]", 2)[0];
    int slash = clean.lastIndexOf('/'), dot = clean.lastIndexOf('.');
    String extension = dot > slash && clean.length() - dot <= 8 ? clean.substring(dot) : ".bin";
    return extension.matches("\\.[A-Za-z0-9]{1,7}") ? extension : ".bin";
  }

  private static byte[] encrypt(byte[] raw, String password) throws Exception {
    byte[] salt = new byte[16], iv = new byte[12];
    SecureRandom random = new SecureRandom();
    random.nextBytes(salt);
    random.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt), new GCMParameterSpec(128, iv));
    return new JSONObject()
        .put("kind", "shiyu-backup-encrypted")
        .put("format", 1)
        .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
        .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
        .put("data", Base64.encodeToString(cipher.doFinal(raw), Base64.NO_WRAP))
        .toString()
        .getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] decrypt(JSONObject envelope, String password) throws Exception {
    byte[] salt = Base64.decode(envelope.getString("salt"), Base64.DEFAULT);
    byte[] iv = Base64.decode(envelope.getString("iv"), Base64.DEFAULT);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), new GCMParameterSpec(128, iv));
    try {
      return cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.DEFAULT));
    } catch (Exception error) {
      throw new IllegalArgumentException("备份密码错误或文件已损坏");
    }
  }

  private static SecretKeySpec derive(String password, byte[] salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 150_000, 256);
    return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), "AES");
  }
}
