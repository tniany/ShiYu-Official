package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class BackupManager {
  private static final String KIND = "shiyu-backup";
  private static final byte[] STREAM_MAGIC = "SYBK2E".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] STREAM_PLAIN_MAGIC = "SYBK2P".getBytes(StandardCharsets.US_ASCII);
  private static final int STREAM_BUFFER = 64 * 1024;
  private static final int MAX_MANIFEST_BYTES = 128 * 1024 * 1024;
  private static final long MAX_ASSET_BYTES = 512L * 1024L * 1024L;
  private static final Set<String> TRANSIENT_PREFS =
      new java.util.HashSet<>(
          java.util.Arrays.asList(
              "app_in_foreground", "visible_chat_id", "active_generation_until",
              "background_state_revision", MainActivity.DATA_RESCUE_REQUIRED,
              MainActivity.DATA_WRITE_EPOCH));

  private BackupManager() {}

  /** Writes a version-2 backup without retaining the complete archive in application memory. */
  static void write(
      Context context, AppDb db, SharedPreferences prefs, SecureStore secure, String password,
      OutputStream destination) throws Exception {
    write(context, db, prefs, secure, password, destination, BackupSelection.all());
  }

  static void write(
      Context context, AppDb db, SharedPreferences prefs, SecureStore secure, String password,
      OutputStream destination, BackupSelection selection) throws Exception {
    if (selection.isEmpty()) throw new IllegalArgumentException("请至少选择一项备份内容");
    if (destination == null) throw new IllegalArgumentException("系统未提供可写入的文件位置");
    OutputStream payload = destination;
    CipherOutputStream encrypted = null;
    if (password == null || password.trim().isEmpty()) {
      destination.write(STREAM_PLAIN_MAGIC);
    } else {
      byte[] salt = new byte[16], iv = new byte[12];
      SecureRandom random = new SecureRandom();
      random.nextBytes(salt);
      random.nextBytes(iv);
      destination.write(STREAM_MAGIC);
      destination.write(salt);
      destination.write(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt), new GCMParameterSpec(128, iv));
      encrypted = new CipherOutputStream(destination, cipher);
      payload = encrypted;
    }

    try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(payload, STREAM_BUFFER))) {
      String stateText;
      try { stateText = db.loadState(); }
      catch (Exception ignored) { stateText = prefs.getString("state", ""); }
      String rawState = stateText == null ? "" : stateText.replaceFirst("^\\uFEFF", "").trim();
      JSONObject state;
      try { state = new JSONObject(rawState.isEmpty() ? "{}" : rawState); }
      catch (Exception ignored) { state = new JSONObject(); }
      normalizeState(state);
      if (!selection.isFull()) state = selection.state(state);
      JSONObject preferences = selection.isFull() ? encodePreferences(prefs)
          : selection.preferences(encodePreferences(prefs));
      preferences.remove("state");
      JSONObject assetFiles = selection.assets ? writeAssets(zip, context, state, preferences) : new JSONObject();
      if (!selection.assets) clearLocalAssetReferences(state, preferences);
      JSONObject root = new JSONObject()
          .put("kind", KIND)
          .put("format", selection.isFull() ? 2 : 3)
          .put("appVersion", BuildConfig.VERSION_NAME)
          .put("createdAt", System.currentTimeMillis())
          .put("state", state)
          .put("stateRaw", selection.isFull() ? rawState : state.toString())
          .put("memories", selection.memories ? safeExportMemories(db) : null)
          .put("memoryNodes", selection.memories ? safeExportMemoryNodes(db) : null)
          .put("realtimeLibrary", selection.memories ? safeExportRealtimeLibrary(db) : null)
          .put("diaries", selection.diaries ? safeExportDiaries(db) : null)
          .put("preferences", preferences)
          .put("secure", selection.credentials ? secure.exportPlain() : null)
          .put("assetFiles", assetFiles);
      if (!selection.isFull()) root.put("selection", selection.json());
      zip.putNextEntry(new ZipEntry("manifest.json"));
      byte[] manifest = root.toString().getBytes(StandardCharsets.UTF_8);
      zip.write(manifest);
      zip.closeEntry();
      zip.finish();
    }
  }

  static byte[] create(
      Context context, AppDb db, SharedPreferences prefs, SecureStore secure, String password)
      throws Exception {
    String stateText;
    try {
      stateText = db.loadState();
    } catch (Exception ignored) {
      stateText = prefs.getString("state", "");
    }
    JSONObject state;
    String rawState = stateText == null ? "" : stateText.replaceFirst("^\\uFEFF", "").trim();
    try {
      state = new JSONObject(rawState.isEmpty() ? "{}" : rawState);
    } catch (Exception ignored) {
      // A rescue backup must still be exportable when the state JSON itself is damaged.
      state = new JSONObject();
    }
    normalizeState(state);
    JSONObject preferences = encodePreferences(prefs);
    JSONObject assets = new JSONObject();
    collectAssets(context, state, preferences, assets);
    JSONArray memories = safeExportMemories(db);
    JSONArray memoryNodes = safeExportMemoryNodes(db);
    JSONArray realtimeLibrary = safeExportRealtimeLibrary(db);
    JSONArray diaries = safeExportDiaries(db);
    JSONObject root =
        new JSONObject()
            .put("kind", KIND)
            .put("format", 1)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("createdAt", System.currentTimeMillis())
            .put("state", state)
            .put("stateRaw", rawState)
            .put("memories", memories)
            .put("memoryNodes", memoryNodes)
            .put("realtimeLibrary", realtimeLibrary)
            .put("diaries", diaries)
            .put("preferences", preferences)
            .put("secure", secure.exportPlain())
            .put("assets", assets);
    byte[] raw = root.toString().getBytes(StandardCharsets.UTF_8);
    return password.trim().isEmpty() ? raw : encrypt(raw, password);
  }

  private static JSONArray safeExportMemories(AppDb db) {
    try { return db.exportMemories(); } catch (Exception ignored) { return new JSONArray(); }
  }

  private static JSONArray safeExportMemoryNodes(AppDb db) {
    try { return db.exportMemoryNodes(); } catch (Exception ignored) { return new JSONArray(); }
  }

  private static JSONArray safeExportRealtimeLibrary(AppDb db) {
    try { return db.exportRealtimeLibrary(); } catch (Exception ignored) { return new JSONArray(); }
  }

  private static JSONArray safeExportDiaries(AppDb db) {
    try { return db.exportDiaries(); } catch (Exception ignored) { return new JSONArray(); }
  }

  static void restore(
      Context context,
      AppDb db,
      SharedPreferences prefs,
      SecureStore secure,
      byte[] bytes,
      String password)
      throws Exception {
    if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("备份文件为空");
    if (startsWith(bytes, STREAM_MAGIC) || startsWith(bytes, STREAM_PLAIN_MAGIC)) {
      restore(context, db, prefs, secure, new ByteArrayInputStream(bytes), password);
      return;
    }
    String input = new String(bytes, StandardCharsets.UTF_8).replaceFirst("^\\uFEFF", "").trim();
    JSONObject envelope = new JSONObject(input);
    JSONObject root;
    if ("shiyu-backup-encrypted".equals(envelope.optString("kind"))) {
      if (password.isEmpty()) throw new IllegalArgumentException("该备份已加密，请输入备份密码");
      root = new JSONObject(new String(decrypt(envelope, password), StandardCharsets.UTF_8));
    } else {
      root = envelope;
    }
    if (!KIND.equals(root.optString("kind")) || root.optInt("format", 1) != 1)
      throw new IllegalArgumentException("不是受支持的时遇备份文件");
    JSONObject state = root.optJSONObject("state");
    if (state == null && root.optString("state", "").trim().startsWith("{")) {
      state = new JSONObject(root.optString("state"));
    }
    if (state == null) {
      String rawState = root.optString("stateRaw", "").trim();
      if (rawState.startsWith("{")) state = new JSONObject(rawState);
    }
    if (state == null) throw new IllegalArgumentException("备份缺少本地状态数据");
    normalizeState(state);
    JSONObject preferences = optionalObject(root, "preferences");
    JSONObject secureValues = optionalObject(root, "secure");
    JSONObject assets = optionalObject(root, "assets");
    JSONArray memories = optionalArray(root, "memories");
    JSONArray memoryNodes = optionalArray(root, "memoryNodes");
    JSONArray realtimeLibrary = optionalArray(root, "realtimeLibrary");
    JSONArray diaries = optionalArray(root, "diaries");
    memories = filterObjects(memories);
    memoryNodes = filterObjects(memoryNodes);
    realtimeLibrary = filterObjects(realtimeLibrary);
    diaries = filterObjects(diaries);
    validateState(state);
    validateMemories(memories);
    validateMemoryNodes(memoryNodes);
    validateRealtimeLibrary(realtimeLibrary);
    validateDiaries(diaries);
    validatePreferences(preferences);
    long rescueEpoch = MainActivity.beginDataRescue(context);
    boolean restorePreferenceValues = preferences != null;
    if (preferences == null) preferences = new JSONObject();
    restoreAssets(context, state, preferences, assets);
    db.restoreBackupState(state.toString(), memories, realtimeLibrary, diaries, memoryNodes);
    if (restorePreferenceValues) restorePreferences(prefs, preferences, rescueEpoch);
    if (secureValues != null) secure.importPlain(secureValues);
    prefs.edit().putBoolean("app_in_foreground", true).remove("visible_chat_id").remove("active_generation_until").apply();
  }

  static void restore(
      Context context, AppDb db, SharedPreferences prefs, SecureStore secure, InputStream source,
      String password) throws Exception {
    if (source == null) throw new IllegalArgumentException("备份文件为空");
    PushbackInputStream input = new PushbackInputStream(new BufferedInputStream(source, STREAM_BUFFER),
        STREAM_MAGIC.length);
    byte[] prefix = new byte[STREAM_MAGIC.length];
    int count = readAtMost(input, prefix);
    if (count <= 0) throw new IllegalArgumentException("备份文件为空");
    if (count == STREAM_MAGIC.length && Arrays.equals(prefix, STREAM_MAGIC)) {
      if (password == null || password.isEmpty())
        throw new IllegalArgumentException("该备份已加密，请输入备份密码");
      byte[] salt = readExact(input, 16), iv = readExact(input, 12);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), new GCMParameterSpec(128, iv));
      restoreZip(context, db, prefs, secure, new CipherInputStream(input, cipher));
      return;
    }
    if (count == STREAM_PLAIN_MAGIC.length && Arrays.equals(prefix, STREAM_PLAIN_MAGIC)) {
      restoreZip(context, db, prefs, secure, input);
      return;
    }
    input.unread(prefix, 0, count);
    ByteArrayOutputStream legacy = new ByteArrayOutputStream();
    copy(input, legacy, Long.MAX_VALUE);
    restore(context, db, prefs, secure, legacy.toByteArray(), password);
  }

  private static void restoreZip(
      Context context, AppDb db, SharedPreferences prefs, SecureStore secure, InputStream source)
      throws Exception {
    File directory = new File(context.getFilesDir(), "restored_assets");
    if (!directory.exists() && !directory.mkdirs())
      throw new IllegalStateException("无法创建备份资源目录");
    JSONObject manifest = null;
    Map<String, String> restoredFiles = new HashMap<>();
    long totalAssets = 0L;
    try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(source, STREAM_BUFFER))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = safeEntryName(entry.getName());
        if ("manifest.json".equals(name)) {
          ByteArrayOutputStream output = new ByteArrayOutputStream();
          copy(zip, output, MAX_MANIFEST_BYTES);
          manifest = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
        } else if (name.startsWith("assets/")) {
          if (totalAssets >= MAX_ASSET_BYTES) throw new IllegalArgumentException("备份资源过大");
          String extension = extension(name);
          File target = new File(directory, UUID.randomUUID() + extension);
          long written;
          try (FileOutputStream output = new FileOutputStream(target)) {
            written = copy(zip, output, MAX_ASSET_BYTES - totalAssets);
          } catch (Exception error) {
            target.delete();
            throw error;
          }
          totalAssets += written;
          restoredFiles.put(name, target.getAbsolutePath());
        }
        zip.closeEntry();
      }
    } catch (javax.crypto.AEADBadTagException error) {
      throw new IllegalArgumentException("备份密码错误或文件已损坏");
    } catch (java.util.zip.ZipException error) {
      throw new IllegalArgumentException("备份密码错误或文件已损坏");
    }
    if (manifest == null) throw new IllegalArgumentException("备份缺少资料清单");
    restoreVersion2(context, db, prefs, secure, manifest, restoredFiles);
  }

  private static void restoreVersion2(
      Context context, AppDb db, SharedPreferences prefs, SecureStore secure, JSONObject root,
      Map<String, String> restoredFiles) throws Exception {
    if (!KIND.equals(root.optString("kind")) || (root.optInt("format") != 2 && root.optInt("format") != 3))
      throw new IllegalArgumentException("不是受支持的时遇备份文件");
    boolean partial = root.optInt("format") == 3;
    if (partial && root.optJSONObject("selection") == null)
      throw new IllegalArgumentException("备份缺少内容选择清单");
    JSONObject state = root.optJSONObject("state");
    if (state == null) throw new IllegalArgumentException("备份缺少本地状态数据");
    normalizeState(state);
    JSONObject preferences = optionalObject(root, "preferences");
    JSONObject secureValues = optionalObject(root, "secure");
    JSONObject assetFiles = optionalObject(root, "assetFiles");
    JSONArray memories = filterObjects(optionalArray(root, "memories"));
    JSONArray memoryNodes = filterObjects(optionalArray(root, "memoryNodes"));
    JSONArray realtimeLibrary = filterObjects(optionalArray(root, "realtimeLibrary"));
    JSONArray diaries = filterObjects(optionalArray(root, "diaries"));
    validateState(state);
    validateMemories(memories);
    validateMemoryNodes(memoryNodes);
    validateRealtimeLibrary(realtimeLibrary);
    validateDiaries(diaries);
    validatePreferences(preferences);
    if (preferences == null) preferences = new JSONObject();
    applyStreamAssetReferences(state, preferences, assetFiles, restoredFiles);
    if (partial) {
      BackupSelection selection = BackupSelection.from(root.getJSONObject("selection"));
      JSONObject current = new JSONObject(db.loadState().isEmpty() ? "{}" : db.loadState());
      if (selection.roles) current.put("cards", BackupSelection.merge(current.optJSONArray("cards"), state.optJSONArray("cards"), "id"));
      if (selection.chats) current.put("chats", BackupSelection.merge(current.optJSONArray("chats"), state.optJSONArray("chats"), "id"));
      if (selection.settings) {
        current.put("commonWorldBook", state.optString("commonWorldBook"));
        current.put("commonRegexRules", state.optString("commonRegexRules"));
      }
      state = current;
      memories = selection.memories ? BackupSelection.merge(db.exportMemories(), memories, "memory") : null;
      memoryNodes = selection.memories ? BackupSelection.merge(db.exportMemoryNodes(), memoryNodes, "id") : db.exportMemoryNodes();
      realtimeLibrary = selection.memories ? BackupSelection.merge(db.exportRealtimeLibrary(), realtimeLibrary, "sourceKey") : null;
      diaries = selection.diaries ? BackupSelection.merge(db.exportDiaries(), diaries, "id") : null;
      preferences = BackupSelection.overlay(encodePreferences(prefs), selection.preferences(preferences));
      secureValues = selection.credentials && secureValues != null
          ? BackupSelection.overlay(secure.exportPlain(), secureValues) : null;
    }
    long rescueEpoch = MainActivity.beginDataRescue(context);
    db.restoreBackupState(state.toString(), memories, realtimeLibrary, diaries, memoryNodes);
    restorePreferences(prefs, preferences, rescueEpoch);
    if (secureValues != null) secure.importPlain(secureValues);
    prefs.edit().putBoolean("app_in_foreground", true).remove("visible_chat_id")
        .remove("active_generation_until").apply();
  }

  private static JSONObject optionalObject(JSONObject root, String name) {
    if (!root.has(name) || root.isNull(name)) return null;
    JSONObject value = root.optJSONObject(name);
    if (value == null) throw new IllegalArgumentException("备份中的 " + name + " 格式不正确");
    return value;
  }

  private static void clearLocalAssetReferences(JSONObject state, JSONObject preferences) throws Exception {
    JSONArray cards = state.optJSONArray("cards");
    if (cards != null) for (int index = 0; index < cards.length(); index++) clearLocalPath(cards.optJSONObject(index), "avatar");
    JSONArray chats = state.optJSONArray("chats");
    if (chats != null) for (int index = 0; index < chats.length(); index++) {
      JSONObject chat = chats.optJSONObject(index);
      JSONArray messages = chat == null ? null : chat.optJSONArray("messages");
      if (messages != null) for (int message = 0; message < messages.length(); message++) clearLocalPath(messages.optJSONObject(message), "image");
    }
    clearLocalPath(preferences.optJSONObject("user_avatar"), "value");
    clearLocalPath(preferences.optJSONObject("chat_background_image"), "value");
    JSONObject stored = preferences.optJSONObject("moments");
    if (stored != null) {
      JSONArray moments = new JSONArray(stored.optString("value", "[]"));
      for (int index = 0; index < moments.length(); index++) clearLocalPath(moments.optJSONObject(index), "image");
      stored.put("value", moments.toString());
    }
  }

  private static void clearLocalPath(JSONObject owner, String key) throws Exception {
    if (owner == null) return;
    String value = owner.optString(key);
    if (!value.startsWith("https://") && !value.startsWith("http://")) owner.put(key, "");
  }

  private static JSONArray optionalArray(JSONObject root, String name) {
    if (!root.has(name) || root.isNull(name)) return null;
    JSONArray value = root.optJSONArray(name);
    if (value == null) throw new IllegalArgumentException("备份中的 " + name + " 格式不正确");
    return value;
  }

  private static void validateState(JSONObject state) throws Exception {
    JSONArray cards = state.optJSONArray("cards"), chats = state.optJSONArray("chats");
    if (cards == null) state.put("cards", new JSONArray());
    if (chats == null) state.put("chats", new JSONArray());
    cards = state.optJSONArray("cards");
    chats = state.optJSONArray("chats");
    for (int i = 0; i < cards.length(); i++) {
      JSONObject item = cards.optJSONObject(i);
      if (item != null) MainActivity.Card.from(item);
    }
    for (int i = 0; i < chats.length(); i++) {
      JSONObject item = chats.optJSONObject(i);
      if (item != null) MainActivity.Chat.from(item);
    }
  }

  /** Removes non-object entries written by older/corrupted versions without losing good data. */
  private static void normalizeState(JSONObject state) throws Exception {
    if (state == null) return;
    state.put("cards", objectArray(state.optJSONArray("cards"), false));
    state.put("chats", objectArray(state.optJSONArray("chats"), true));
  }

  private static JSONArray objectArray(JSONArray source, boolean sanitizeChat) throws Exception {
    JSONArray result = new JSONArray();
    if (source == null) return result;
    for (int i = 0; i < source.length(); i++) {
      JSONObject value = source.optJSONObject(i);
      if (value == null) {
        String encoded = source.optString(i, "").trim();
        if (encoded.startsWith("{")) {
          try { value = new JSONObject(encoded); } catch (Exception ignored) { }
        }
      }
      if (value == null) continue;
      if (sanitizeChat) {
        JSONArray messages = value.optJSONArray("messages");
        if (messages != null) value.put("messages", objectArray(messages, false));
      }
      result.put(value);
    }
    return result;
  }

  private static JSONArray filterObjects(JSONArray source) {
    if (source == null) return null;
    JSONArray result = new JSONArray();
    for (int i = 0; i < source.length(); i++) {
      JSONObject item = source.optJSONObject(i);
      if (item == null) {
        String encoded = source.optString(i, "").trim();
        if (encoded.startsWith("{")) {
          try { item = new JSONObject(encoded); } catch (Exception ignored) { }
        }
      }
      if (item != null) result.put(item);
    }
    return result;
  }

  private static void validateMemories(JSONArray values) throws Exception {
    if (values == null) return;
    for (int i = 0; i < values.length(); i++) {
      JSONObject item = values.optJSONObject(i);
      if (item == null) continue;
      if (item.optString("cardId").trim().isEmpty())
        throw new IllegalArgumentException("备份中的记忆数据不完整");
    }
  }

  private static void validateRealtimeLibrary(JSONArray values) throws Exception {
    if (values == null) return;
    for (int i = 0; i < values.length(); i++) {
      JSONObject item = values.optJSONObject(i);
      if (item == null) continue;
      if (item.optString("cardId").trim().isEmpty()
          || item.optString("sourceKey").trim().isEmpty()
          || item.optJSONArray("vector") == null)
        throw new IllegalArgumentException("备份中的实时资料库数据不完整");
    }
  }

  private static void validateMemoryNodes(JSONArray values) throws Exception {
    if (values == null) return;
    for (int i = 0; i < values.length(); i++) {
      JSONObject item = values.optJSONObject(i);
      if (item == null) continue;
      if (item.optString("id").trim().isEmpty() || item.optString("cardId").trim().isEmpty()
          || item.optString("title").trim().isEmpty() || item.optInt("month") < 1
          || item.optInt("month") > 12 || item.optInt("day") < 1 || item.optInt("day") > 31)
        throw new IllegalArgumentException("备份中的记忆节点数据不完整");
    }
  }

  private static void validateDiaries(JSONArray values) throws Exception {
    if (values == null) return;
    for (int i = 0; i < values.length(); i++) {
      JSONObject item = values.optJSONObject(i);
      if (item != null && item.optString("id").trim().isEmpty())
        throw new IllegalArgumentException("备份中的日记数据不完整");
    }
  }

  private static void validatePreferences(JSONObject values) {
    if (values == null) return;
    java.util.Iterator<String> names = values.keys();
    while (names.hasNext()) {
      String name = names.next();
      JSONObject item = values.optJSONObject(name);
      if (item == null) {
        Object raw = values.opt(name);
        if (raw == null || raw == JSONObject.NULL || raw instanceof Boolean || raw instanceof Number
            || raw instanceof String || raw instanceof JSONArray) continue;
        throw new IllegalArgumentException("备份中的设置数据格式不正确");
      }
      if (!java.util.Arrays.asList("boolean", "int", "long", "float", "set", "string")
          .contains(item.optString("type")))
        throw new IllegalArgumentException("备份中的设置数据格式不正确");
    }
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
      else if (raw instanceof Set) {
        JSONArray array = new JSONArray();
        for (Object item : (Set<?>) raw) array.put(String.valueOf(item));
        value.put("type", "set").put("value", array);
      }
      else value.put("type", "string").put("value", String.valueOf(raw));
      result.put(entry.getKey(), value);
    }
    return result;
  }

  private static void restorePreferences(
      SharedPreferences prefs, JSONObject values, long rescueEpoch) {
    SharedPreferences.Editor editor = prefs.edit().clear();
    java.util.Iterator<String> names = values.keys();
    while (names.hasNext()) {
      String name = names.next();
      if (TRANSIENT_PREFS.contains(name)) continue;
      JSONObject item = values.optJSONObject(name);
      if (item == null) {
        // Accept legacy backups that stored preference values directly.
        Object raw = values.opt(name);
        if (raw instanceof Boolean) editor.putBoolean(name, (Boolean) raw);
        else if (raw instanceof Integer) editor.putInt(name, (Integer) raw);
        else if (raw instanceof Long) editor.putLong(name, (Long) raw);
        else if (raw instanceof Number) editor.putFloat(name, ((Number) raw).floatValue());
        else if (raw != null && raw != JSONObject.NULL) editor.putString(name, String.valueOf(raw));
        continue;
      }
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
    editor.putBoolean(MainActivity.DATA_RESCUE_REQUIRED, true)
        .putLong(MainActivity.DATA_WRITE_EPOCH, rescueEpoch);
    if (!editor.commit()) throw new IllegalStateException("无法写入备份设置");
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

  private static JSONObject writeAssets(
      ZipOutputStream zip, Context context, JSONObject state, JSONObject preferences)
      throws Exception {
    LinkedHashSet<String> references = new LinkedHashSet<>();
    JSONArray cards = state.optJSONArray("cards");
    if (cards != null) for (int i = 0; i < cards.length(); i++)
      collectReference(cards.optJSONObject(i), "avatar", references);
    JSONArray chats = state.optJSONArray("chats");
    if (chats != null) for (int i = 0; i < chats.length(); i++) {
      JSONObject chat = chats.optJSONObject(i);
      JSONArray messages = chat == null ? null : chat.optJSONArray("messages");
      if (messages != null) for (int j = 0; j < messages.length(); j++)
        collectReference(messages.optJSONObject(j), "image", references);
    }
    collectPreferenceReference(preferences, "user_avatar", references);
    collectPreferenceReference(preferences, "chat_background_image", references);
    JSONObject momentPreference = preferences.optJSONObject("moments");
    if (momentPreference != null) {
      try {
        JSONArray moments = new JSONArray(momentPreference.optString("value", "[]"));
        for (int i = 0; i < moments.length(); i++)
          collectReference(moments.optJSONObject(i), "image", references);
      } catch (Exception ignored) {}
    }

    JSONObject result = new JSONObject();
    int index = 0;
    for (String reference : references) {
      try (InputStream input = open(context, reference)) {
        if (input == null) continue;
        String entryName = "assets/" + (++index) + extension(reference);
        zip.putNextEntry(new ZipEntry(entryName));
        copy(input, zip, MAX_ASSET_BYTES);
        zip.closeEntry();
        result.put(reference, entryName);
      } catch (Exception ignored) {
        // A missing external image must not make chats and settings impossible to back up.
        try { zip.closeEntry(); } catch (Exception ignoredClose) {}
      }
    }
    return result;
  }

  private static void collectReference(
      JSONObject owner, String field, Set<String> references) {
    if (owner == null) return;
    String value = owner.optString(field, "").trim();
    if (!value.isEmpty() && !value.startsWith("http://") && !value.startsWith("https://"))
      references.add(value);
  }

  private static void collectPreferenceReference(
      JSONObject preferences, String name, Set<String> references) {
    JSONObject item = preferences.optJSONObject(name);
    if (item == null) return;
    String value = item.optString("value", "").trim();
    if (!value.isEmpty() && !value.startsWith("http://") && !value.startsWith("https://"))
      references.add(value);
  }

  private static void applyStreamAssetReferences(
      JSONObject state, JSONObject preferences, JSONObject assetFiles,
      Map<String, String> restoredFiles) throws Exception {
    if (assetFiles == null || restoredFiles.isEmpty()) return;
    Map<String, String> replacements = new HashMap<>();
    java.util.Iterator<String> names = assetFiles.keys();
    while (names.hasNext()) {
      String original = names.next();
      String restored = restoredFiles.get(assetFiles.optString(original));
      if (restored != null) replacements.put(original, restored);
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
        for (int i = 0; i < moments.length(); i++)
          replace(moments.optJSONObject(i), "image", replacements);
        momentPreference.put("value", moments.toString());
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

  private static String safeEntryName(String value) {
    String name = value == null ? "" : value.replace('\\', '/');
    if (name.isEmpty() || name.startsWith("/") || name.contains("../") || name.equals(".."))
      throw new IllegalArgumentException("备份包含不安全的文件路径");
    return name;
  }

  private static boolean startsWith(byte[] value, byte[] prefix) {
    if (value == null || value.length < prefix.length) return false;
    for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false;
    return true;
  }

  private static int readAtMost(InputStream input, byte[] value) throws Exception {
    int total = 0;
    while (total < value.length) {
      int read = input.read(value, total, value.length - total);
      if (read < 0) break;
      if (read == 0) continue;
      total += read;
    }
    return total;
  }

  private static byte[] readExact(InputStream input, int size) throws Exception {
    byte[] value = new byte[size];
    if (readAtMost(input, value) != size) throw new IllegalArgumentException("备份文件不完整");
    return value;
  }

  private static long copy(InputStream input, OutputStream output, long limit) throws Exception {
    byte[] buffer = new byte[STREAM_BUFFER];
    long total = 0L;
    while (true) {
      int allowed = limit == Long.MAX_VALUE ? buffer.length
          : (int) Math.min(buffer.length, Math.max(1L, limit - total + 1L));
      int read = input.read(buffer, 0, allowed);
      if (read < 0) break;
      if (read == 0) continue;
      total += read;
      if (total > limit) throw new IllegalArgumentException("备份内容过大");
      output.write(buffer, 0, read);
    }
    return total;
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
