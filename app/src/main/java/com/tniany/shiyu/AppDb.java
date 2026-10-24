package com.tniany.shiyu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

final class AppDb extends SQLiteOpenHelper {
  private static final int STATE_CHUNK_CHARS = 200_000;

  AppDb(Context context) {
      super(context, "shiyu.db", null, 6);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE state_data(id INTEGER PRIMARY KEY CHECK(id=1), json TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE memories(id INTEGER PRIMARY KEY AUTOINCREMENT, card_id TEXT NOT NULL, content"
            + " TEXT NOT NULL, updated INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX memory_card_idx ON memories(card_id)");
    createEmbeddingTable(db);
    createDiaryTable(db);
    createMemoryNodeTable(db);
    createStateChunkTable(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) createEmbeddingTable(db);
    if (oldVersion < 3) createDiaryTable(db);
    if (oldVersion < 4) createMemoryNodeTable(db);
    if (oldVersion < 5) createStateChunkTable(db);
    if (oldVersion < 6) migrateStateChunkTable(db);
  }

  private static void createStateChunkTable(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS state_chunks("
            + "id INTEGER NOT NULL CHECK(id=1), chunk_index INTEGER NOT NULL,"
            + " content TEXT NOT NULL, PRIMARY KEY(id, chunk_index))");
  }

  private static void migrateStateChunkTable(SQLiteDatabase db) {
    try {
      boolean tableExists = false;
      try (var c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='state_chunks'", null)) {
        tableExists = c.moveToFirst();
      }
      if (!tableExists) {
        createStateChunkTable(db);
        return;
      }
      db.execSQL(
          "CREATE TABLE IF NOT EXISTS state_chunks_new("
              + "id INTEGER NOT NULL CHECK(id=1), chunk_index INTEGER NOT NULL,"
              + " content TEXT NOT NULL, PRIMARY KEY(id, chunk_index))");
      db.execSQL(
          "INSERT OR IGNORE INTO state_chunks_new(id, chunk_index, content)"
              + " SELECT id, chunk_index, content FROM state_chunks");
      db.execSQL("DROP TABLE IF EXISTS state_chunks");
      db.execSQL("ALTER TABLE state_chunks_new RENAME TO state_chunks");
    } catch (RuntimeException ignored) {
      try {
        db.execSQL("DROP TABLE IF EXISTS state_chunks_new");
      } catch (RuntimeException ignored2) {
      }
    }
  }

  private static void createEmbeddingTable(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE IF NOT EXISTS realtime_library(id INTEGER PRIMARY KEY AUTOINCREMENT, card_id TEXT NOT NULL, source_key TEXT NOT NULL UNIQUE, content TEXT NOT NULL, vector TEXT NOT NULL, updated INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX IF NOT EXISTS realtime_card_idx ON realtime_library(card_id,updated DESC)");
  }

  private static void createDiaryTable(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE IF NOT EXISTS diaries(id TEXT PRIMARY KEY,owner_type TEXT NOT NULL,owner_id TEXT NOT NULL,title TEXT NOT NULL,content TEXT NOT NULL,shared_with TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX IF NOT EXISTS diary_owner_idx ON diaries(owner_type,owner_id,updated_at DESC)");
  }

  private static void createMemoryNodeTable(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE IF NOT EXISTS memory_nodes(id TEXT PRIMARY KEY,card_id TEXT NOT NULL,type TEXT NOT NULL,title TEXT NOT NULL,detail TEXT NOT NULL DEFAULT '',year INTEGER NOT NULL DEFAULT 0,month INTEGER NOT NULL,day INTEGER NOT NULL,repeat_yearly INTEGER NOT NULL DEFAULT 1,remind_days INTEGER NOT NULL DEFAULT 7,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX IF NOT EXISTS memory_node_card_idx ON memory_nodes(card_id,month,day)");
  }

  void saveState(String json) {
    SQLiteDatabase database = getWritableDatabase();
    database.beginTransaction();
    try {
      // Keep a compact row for small states so older builds can still open the database, and split
      // anything larger across bounded rows. Reading never depends on a single large CursorWindow.
      ContentValues state = new ContentValues();
      state.put("id", 1);
      state.put("json", json.length() <= STATE_CHUNK_CHARS ? json : "");
      database.insertWithOnConflict("state_data", null, state, SQLiteDatabase.CONFLICT_REPLACE);

      database.delete("state_chunks", null, null);
      if (json.length() > STATE_CHUNK_CHARS) {
        for (int offset = 0, index = 0; offset < json.length(); offset += STATE_CHUNK_CHARS, index++) {
          ContentValues chunk = new ContentValues();
          chunk.put("id", 1);
          chunk.put("chunk_index", index);
          chunk.put("content", json.substring(offset, Math.min(json.length(), offset + STATE_CHUNK_CHARS)));
          database.insertOrThrow("state_chunks", null, chunk);
        }
      }
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
  }

  void restoreBackupState(
      String stateJson, JSONArray memories, JSONArray realtimeLibrary, JSONArray diaries,
      JSONArray memoryNodes)
      throws Exception {
    SQLiteDatabase database = getWritableDatabase();
    database.beginTransaction();
    try {
      ContentValues state = new ContentValues();
      state.put("id", 1);
      state.put("json", stateJson.length() <= STATE_CHUNK_CHARS ? stateJson : "");
      if (database.insertWithOnConflict(
          "state_data", null, state, SQLiteDatabase.CONFLICT_REPLACE) < 0)
        throw new IllegalStateException("Unable to restore state data");
      database.delete("state_chunks", null, null);
      if (stateJson.length() > STATE_CHUNK_CHARS) {
        for (int offset = 0, index = 0; offset < stateJson.length(); offset += STATE_CHUNK_CHARS, index++) {
          ContentValues chunk = new ContentValues();
          chunk.put("id", 1);
          chunk.put("chunk_index", index);
          chunk.put("content", stateJson.substring(offset, Math.min(stateJson.length(), offset + STATE_CHUNK_CHARS)));
          database.insertOrThrow("state_chunks", null, chunk);
        }
      }

      if (memories != null) {
        database.delete("memories", null, null);
        for (int i = 0; i < memories.length(); i++) {
          JSONObject item = memories.getJSONObject(i);
          ContentValues value = new ContentValues();
          value.put("card_id", item.getString("cardId"));
          value.put("content", item.optString("content"));
          value.put("updated", item.optLong("updated", System.currentTimeMillis()));
          database.insertOrThrow("memories", null, value);
        }
      }

      if (realtimeLibrary != null) {
        database.delete("realtime_library", null, null);
        for (int i = 0; i < realtimeLibrary.length(); i++) {
          JSONObject item = realtimeLibrary.getJSONObject(i);
          ContentValues value = new ContentValues();
          value.put("card_id", item.getString("cardId"));
          value.put("source_key", item.getString("sourceKey"));
          value.put("content", item.optString("content"));
          value.put("vector", item.getJSONArray("vector").toString());
          value.put("updated", item.optLong("updated", System.currentTimeMillis()));
          database.insertOrThrow("realtime_library", null, value);
        }
      }

      if (diaries != null) {
        database.delete("diaries", null, null);
        for (int i = 0; i < diaries.length(); i++) {
          JSONObject item = diaries.getJSONObject(i);
          ContentValues value = new ContentValues();
          value.put("id", item.getString("id"));
          value.put("owner_type", "character".equals(item.optString("ownerType"))
              ? "character" : "user");
          value.put("owner_id", item.optString("ownerId"));
          value.put("title", item.optString("title"));
          value.put("content", item.optString("content"));
          value.put("shared_with", item.optString("sharedWith"));
          value.put("created_at", item.optLong("createdAt", System.currentTimeMillis()));
          value.put("updated_at", item.optLong("updatedAt", System.currentTimeMillis()));
          database.insertOrThrow("diaries", null, value);
        }
      }

      database.delete("memory_nodes", null, null);
      if (memoryNodes != null) {
        for (int i = 0; i < memoryNodes.length(); i++) {
          JSONObject item = memoryNodes.getJSONObject(i);
          ContentValues value = memoryNodeValues(
              item.getString("id"), item.getString("cardId"), item.optString("type", "shared_event"),
              item.optString("title"), item.optString("detail"), item.optInt("year"),
              item.optInt("month"), item.optInt("day"), item.optBoolean("repeatYearly", true),
              item.optInt("remindDays", 7), item.optLong("createdAt", System.currentTimeMillis()));
          value.put("updated_at", item.optLong("updatedAt", System.currentTimeMillis()));
          database.insertOrThrow("memory_nodes", null, value);
        }
      }
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
  }

  String loadState() {
    SQLiteDatabase database = getReadableDatabase();
    StringBuilder chunks = new StringBuilder();
    try (Cursor c =
        database.rawQuery(
            "SELECT content FROM state_chunks WHERE id=1 ORDER BY chunk_index", null)) {
      while (c.moveToNext()) {
        String part = c.getString(0);
        if (part != null) chunks.append(part);
      }
    }
    if (chunks.length() > 0) return chunks.toString();

    // Legacy databases stored the whole state in one row, which breaks once it exceeds the
    // CursorWindow limit. Read large legacy rows in bounded slices so they can still be opened.
    int length = 0;
    try (Cursor c = database.rawQuery("SELECT length(json) FROM state_data WHERE id=1", null)) {
      if (!c.moveToFirst()) return "";
      length = c.getInt(0);
    }
    if (length <= 0) return "";
    final int chunk = 131_072;
    StringBuilder out = new StringBuilder(length);
    for (int start = 1; start <= length; start += chunk) {
      try (Cursor c =
          database.rawQuery(
              "SELECT substr(json, ?, ?) FROM state_data WHERE id=1",
              new String[] {String.valueOf(start), String.valueOf(chunk)})) {
        if (!c.moveToFirst()) break;
        String part = c.getString(0);
        if (part == null || part.isEmpty()) break;
        out.append(part);
      }
    }
    return out.toString();
  }

  /**
   * Rewrites a legacy oversized single-row state into chunked storage. Callers invoke this once
   * after a successful load; subsequent reads never touch the large row again.
   */
  boolean optimizeLegacyState(String json) {
    if (json == null || json.isEmpty()) return false;
    int stored = 0;
    try (Cursor c =
        getReadableDatabase().rawQuery(
            "SELECT length(json) FROM state_data WHERE id=1", null)) {
      if (c.moveToFirst()) stored = c.getInt(0);
    } catch (RuntimeException ignored) {
      return false;
    }
    if (stored <= STATE_CHUNK_CHARS) return false;
    saveState(json);
    return true;
  }

  /** Kept for diagnostics: reports the stored state size without materialising the whole row. */
  int stateSize() {
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT SUM(length(content)) FROM state_chunks WHERE id=1", null)) {
      if (c.moveToFirst() && !c.isNull(0) && c.getInt(0) > 0) return c.getInt(0);
    } catch (RuntimeException ignored) {
    }
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT length(json) FROM state_data WHERE id=1", null)) {
      return c.moveToFirst() ? c.getInt(0) : 0;
    }
  }

  void verifyReadable() {
    SQLiteDatabase database = getReadableDatabase();
    try (Cursor check = database.rawQuery("PRAGMA quick_check(1)", null)) {
      if (!check.moveToFirst() || !"ok".equalsIgnoreCase(check.getString(0)))
        throw new IllegalStateException("SQLite quick_check failed");
    }
    String[] tables = {
        "state_data", "state_chunks", "memories", "realtime_library", "diaries", "memory_nodes"
    };
    for (String table : tables) {
      try (Cursor ignored = database.rawQuery("SELECT 1 FROM " + table + " LIMIT 1", null)) {
        ignored.moveToFirst();
      }
    }
  }

  long addMemory(String cardId, String content) {
    ContentValues v = new ContentValues();
    v.put("card_id", cardId);
    v.put("content", content);
    v.put("updated", System.currentTimeMillis());
    return getWritableDatabase().insert("memories", null, v);
  }

  void updateMemory(long id, String content) {
    ContentValues v = new ContentValues();
    v.put("content", content);
    v.put("updated", System.currentTimeMillis());
    getWritableDatabase().update("memories", v, "id=?", new String[] {String.valueOf(id)});
  }

  void deleteMemory(long id) {
    getWritableDatabase().delete("memories", "id=?", new String[] {String.valueOf(id)});
  }

  void deleteCardMemories(String cardId) {
    getWritableDatabase().delete("memories", "card_id=?", new String[] {cardId});
    getWritableDatabase().delete("memory_nodes", "card_id=?", new String[] {cardId});
  }

  String addMemoryNode(String cardId, String type, String title, String detail, int year, int month,
      int day, boolean repeatYearly, int remindDays) {
    String id = java.util.UUID.randomUUID().toString();
    getWritableDatabase().insertOrThrow("memory_nodes", null, memoryNodeValues(id, cardId, type,
        title, detail, year, month, day, repeatYearly, remindDays, System.currentTimeMillis()));
    return id;
  }

  void updateMemoryNode(String id, String type, String title, String detail, int year, int month,
      int day, boolean repeatYearly, int remindDays) {
    ContentValues value = memoryNodeValues(id, "", type, title, detail, year, month, day,
        repeatYearly, remindDays, System.currentTimeMillis());
    value.remove("id");value.remove("card_id");value.remove("created_at");
    getWritableDatabase().update("memory_nodes", value, "id=?", new String[] {id});
  }

  void deleteMemoryNode(String id) {
    getWritableDatabase().delete("memory_nodes", "id=?", new String[] {id});
  }

  ArrayList<MemoryNode> memoryNodes(String cardId) {
    ArrayList<MemoryNode> out = new ArrayList<>();
    try (Cursor c = getReadableDatabase().rawQuery(
        "SELECT id,type,title,detail,year,month,day,repeat_yearly,remind_days,created_at,updated_at FROM memory_nodes WHERE card_id=? ORDER BY month,day,created_at",
        new String[] {cardId})) {
      while (c.moveToNext()) out.add(new MemoryNode(c.getString(0), c.getString(1), c.getString(2),
          c.getString(3), c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7) != 0,
          c.getInt(8), c.getLong(9), c.getLong(10)));
    }
    return out;
  }

  private static ContentValues memoryNodeValues(String id, String cardId, String type, String title,
      String detail, int year, int month, int day, boolean repeatYearly, int remindDays, long createdAt) {
    ContentValues value = new ContentValues();long now = System.currentTimeMillis();
    value.put("id", id);value.put("card_id", cardId == null ? "" : cardId);
    value.put("type", normalizeNodeType(type));value.put("title", title == null ? "" : title.trim());
    value.put("detail", detail == null ? "" : detail.trim());value.put("year", Math.max(0, year));
    value.put("month", Math.max(1, Math.min(12, month)));value.put("day", Math.max(1, Math.min(31, day)));
    value.put("repeat_yearly", repeatYearly ? 1 : 0);value.put("remind_days", Math.max(0, Math.min(60, remindDays)));
    value.put("created_at", createdAt > 0 ? createdAt : now);value.put("updated_at", now);
    return value;
  }

  private static String normalizeNodeType(String type) {
    return java.util.Arrays.asList("birthday", "anniversary", "festival", "shared_event").contains(type)
        ? type : "shared_event";
  }

  boolean hasRealtimeEntry(String sourceKey) {
    try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM realtime_library WHERE source_key=? LIMIT 1", new String[] {sourceKey})) {
      return c.moveToFirst();
    }
  }

  void upsertRealtimeEntry(String cardId, String sourceKey, String content, JSONArray vector) {
    ContentValues value = new ContentValues();
    value.put("card_id", cardId);value.put("source_key", sourceKey);value.put("content", content);
    value.put("vector", vector.toString());value.put("updated", System.currentTimeMillis());
    getWritableDatabase().insertWithOnConflict("realtime_library", null, value, SQLiteDatabase.CONFLICT_REPLACE);
  }

  int realtimeCount(String cardId) {
    String sql = cardId == null || cardId.isEmpty() ? "SELECT COUNT(*) FROM realtime_library" : "SELECT COUNT(*) FROM realtime_library WHERE card_id=?";
    String[] args = cardId == null || cardId.isEmpty() ? null : new String[] {cardId};
    try (Cursor c = getReadableDatabase().rawQuery(sql, args)) { return c.moveToFirst() ? c.getInt(0) : 0; }
  }

  ArrayList<RealtimeEntry> realtimeEntries(String cardId, int limit) {
    ArrayList<RealtimeEntry> out = new ArrayList<>();
    String selection = cardId == null || cardId.isEmpty() ? "" : " WHERE card_id=?";
    String[] args = cardId == null || cardId.isEmpty() ? null : new String[] {cardId};
    try (Cursor c = getReadableDatabase().rawQuery("SELECT source_key,card_id,content,vector,updated FROM realtime_library" + selection + " ORDER BY updated DESC LIMIT " + Math.max(1, Math.min(100000, limit)), args)) {
      while (c.moveToNext()) out.add(new RealtimeEntry(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), 0));
    }
    return out;
  }

  ArrayList<RealtimeEntry> searchRealtime(String cardId, JSONArray query, int limit, double minScore, java.util.Set<String> excludedKeys) {
    ArrayList<RealtimeEntry> matches = new ArrayList<>();
    for (RealtimeEntry entry : realtimeEntries(cardId, 500)) {
      if (excludedKeys != null && excludedKeys.contains(entry.sourceKey)) continue;
      try {
        double score = cosine(query, new JSONArray(entry.vector));
        if (score >= minScore) matches.add(new RealtimeEntry(entry.sourceKey, entry.cardId, entry.content, entry.vector, entry.updated, score));
      } catch (Exception ignored) {}
    }
    matches.sort((a,b) -> Double.compare(b.score, a.score));
    return new ArrayList<>(matches.subList(0, Math.min(matches.size(), Math.max(1, limit))));
  }

  private static double cosine(JSONArray a, JSONArray b) {
    int length = Math.min(a.length(), b.length());double dot=0, left=0, right=0;
    for (int i=0;i<length;i++){double x=a.optDouble(i), y=b.optDouble(i);dot+=x*y;left+=x*x;right+=y*y;}
    return left == 0 || right == 0 ? 0 : dot / (Math.sqrt(left) * Math.sqrt(right));
  }

  void clearRealtimeLibrary() { getWritableDatabase().delete("realtime_library", null, null); }
  void deleteCardRealtime(String cardId) { getWritableDatabase().delete("realtime_library", "card_id=?", new String[] {cardId}); }
  void deleteChatRealtime(String chatId) { getWritableDatabase().delete("realtime_library", "source_key LIKE ?", new String[] {chatId + ":%"}); }

  void clearAll() {
    SQLiteDatabase db = getWritableDatabase();
    db.delete("memories", null, null);
    db.delete("realtime_library", null, null);
    db.delete("diaries", null, null);
    db.delete("memory_nodes", null, null);
    db.delete("state_data", null, null);
  }

  String addDiary(String ownerType, String ownerId, String title, String content) {
    String id = java.util.UUID.randomUUID().toString();
    long now = System.currentTimeMillis();
    ContentValues value = new ContentValues();
    value.put("id", id);
    value.put("owner_type", "character".equals(ownerType) ? "character" : "user");
    value.put("owner_id", ownerId == null ? "" : ownerId);
    value.put("title", title == null ? "" : title.trim());
    value.put("content", content == null ? "" : content.trim());
    value.put("shared_with", "");
    value.put("created_at", now);
    value.put("updated_at", now);
    getWritableDatabase().insertOrThrow("diaries", null, value);
    return id;
  }

  void updateDiary(String id, String title, String content) {
    ContentValues value = new ContentValues();
    value.put("title", title == null ? "" : title.trim());
    value.put("content", content == null ? "" : content.trim());
    value.put("updated_at", System.currentTimeMillis());
    getWritableDatabase().update("diaries", value, "id=?", new String[] {id});
  }

  void markDiaryShared(String id, String targetId) {
    Diary entry = diary(id);
    if (entry == null) return;
    java.util.LinkedHashSet<String> targets = new java.util.LinkedHashSet<>();
    if (!entry.sharedWith.isEmpty()) targets.addAll(java.util.Arrays.asList(entry.sharedWith.split(",")));
    if (targetId != null && !targetId.trim().isEmpty()) targets.add(targetId.trim());
    ContentValues value = new ContentValues();
    value.put("shared_with", android.text.TextUtils.join(",", targets));
    value.put("updated_at", System.currentTimeMillis());
    getWritableDatabase().update("diaries", value, "id=?", new String[] {id});
  }

  void deleteDiary(String id) {
    getWritableDatabase().delete("diaries", "id=?", new String[] {id});
  }

  Diary diary(String id) {
    try (Cursor c = getReadableDatabase().rawQuery(
        "SELECT id,owner_type,owner_id,title,content,shared_with,created_at,updated_at FROM diaries WHERE id=? LIMIT 1",
        new String[] {id})) {
      return c.moveToFirst() ? readDiary(c) : null;
    }
  }

  ArrayList<Diary> diaries(String cardId) {
    ArrayList<Diary> out = new ArrayList<>();
    try (Cursor c = getReadableDatabase().rawQuery(
        "SELECT id,owner_type,owner_id,title,content,shared_with,created_at,updated_at FROM diaries WHERE owner_type='user' OR owner_id=? ORDER BY updated_at DESC LIMIT 500",
        new String[] {cardId == null ? "" : cardId})) {
      while (c.moveToNext()) out.add(readDiary(c));
    }
    return out;
  }

  String diaryContext(String cardId) {
    StringBuilder out = new StringBuilder();
    int count = 0;
    for (Diary diary : diaries(cardId)) {
      boolean visible = "character".equals(diary.ownerType) || diary.sharedWith.contains(cardId);
      if (!visible) continue;
      if (out.length() > 0) out.append('\n');
      out.append("- diary_id=").append(diary.id).append(" [")
          .append("user".equals(diary.ownerType) ? "用户" : "角色").append("] ")
          .append(diary.title).append("：").append(diary.content);
      count++;
      if (count >= 8 || out.length() > 4000) break;
    }
    return out.toString();
  }

  JSONArray exportDiaries() throws Exception {
    JSONArray result = new JSONArray();
    try (Cursor c = getReadableDatabase().rawQuery(
        "SELECT id,owner_type,owner_id,title,content,shared_with,created_at,updated_at FROM diaries ORDER BY created_at", null)) {
      while (c.moveToNext()) {
        Diary entry = readDiary(c);
        result.put(new JSONObject().put("id", entry.id).put("ownerType", entry.ownerType)
            .put("ownerId", entry.ownerId).put("title", entry.title).put("content", entry.content)
            .put("sharedWith", entry.sharedWith).put("createdAt", entry.createdAt).put("updatedAt", entry.updatedAt));
      }
    }
    return result;
  }

  void importDiaries(JSONArray values) {
    SQLiteDatabase database = getWritableDatabase();
    database.beginTransaction();
    try {
      database.delete("diaries", null, null);
      for (int i = 0; i < values.length(); i++) {
        JSONObject item = values.optJSONObject(i);
        if (item == null || item.optString("id").isEmpty()) continue;
        ContentValues value = new ContentValues();
        value.put("id", item.optString("id"));
        value.put("owner_type", "character".equals(item.optString("ownerType")) ? "character" : "user");
        value.put("owner_id", item.optString("ownerId"));
        value.put("title", item.optString("title"));
        value.put("content", item.optString("content"));
        value.put("shared_with", item.optString("sharedWith"));
        value.put("created_at", item.optLong("createdAt", System.currentTimeMillis()));
        value.put("updated_at", item.optLong("updatedAt", System.currentTimeMillis()));
        database.insertWithOnConflict("diaries", null, value, SQLiteDatabase.CONFLICT_REPLACE);
      }
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
  }

  private static Diary readDiary(Cursor c) {
    return new Diary(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4),
        c.getString(5), c.getLong(6), c.getLong(7));
  }

  JSONArray exportMemories() throws Exception {
    JSONArray result = new JSONArray();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT card_id,content,updated FROM memories ORDER BY id", null)) {
      while (c.moveToNext()) {
        result.put(
            new JSONObject()
                .put("cardId", c.getString(0))
                .put("content", c.getString(1))
                .put("updated", c.getLong(2)));
      }
    }
    return result;
  }

  JSONArray exportRealtimeLibrary() throws Exception {
    JSONArray result = new JSONArray();
    for (RealtimeEntry entry : realtimeEntries("", 100000)) result.put(new JSONObject().put("sourceKey",entry.sourceKey).put("cardId",entry.cardId).put("content",entry.content).put("vector",new JSONArray(entry.vector)).put("updated",entry.updated));
    return result;
  }

  void importRealtimeLibrary(JSONArray values) {
    clearRealtimeLibrary();
    for(int i=0;i<values.length();i++){JSONObject item=values.optJSONObject(i);if(item==null)continue;JSONArray vector=item.optJSONArray("vector");if(vector!=null&&!item.optString("cardId").isEmpty())upsertRealtimeEntry(item.optString("cardId"),item.optString("sourceKey"),item.optString("content"),vector);}
  }

  void importMemories(JSONArray values) {
    SQLiteDatabase database = getWritableDatabase();
    database.beginTransaction();
    try {
      database.delete("memories", null, null);
      for (int i = 0; i < values.length(); i++) {
        JSONObject item = values.optJSONObject(i);
        if (item == null || item.optString("cardId").isEmpty()) continue;
        ContentValues value = new ContentValues();
        value.put("card_id", item.optString("cardId"));
        value.put("content", item.optString("content"));
        value.put("updated", item.optLong("updated", System.currentTimeMillis()));
        database.insert("memories", null, value);
      }
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
  }

  ArrayList<Memory> memories(String cardId) {
    ArrayList<Memory> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT id,content FROM memories WHERE card_id=? ORDER BY updated DESC",
                new String[] {cardId})) {
      while (c.moveToNext()) out.add(new Memory(c.getLong(0), c.getString(1)));
    }
    return out;
  }

  String memoryContext(String cardId) {
    StringBuilder b = new StringBuilder();
    for (Memory m : memories(cardId)) {
      if (b.length() > 0) b.append('\n');
      b.append("- ").append(m.content);
      if (b.length() > 3000) break;
    }
    for (MemoryNode node : memoryNodes(cardId)) {
      if (b.length() > 0) b.append('\n');
      b.append("- [").append(node.typeLabel()).append("] ").append(node.title)
          .append("：").append(node.dateLabel());
      if (!node.detail.isEmpty()) b.append("；").append(node.detail);
      int days = node.daysUntil();
      if (days == 0) b.append("（就是今天，可结合关系自然纪念）");
      else if (days > 0 && days <= node.remindDays) b.append("（还有").append(days).append("天，可自然提起或准备）");
      else if (days == -1) b.append("（日期已过去，不要当作即将到来）");
      if (b.length() > 5000) break;
    }
    return b.toString();
  }

  JSONArray exportMemoryNodes() throws Exception {
    JSONArray result = new JSONArray();
    try (Cursor c = getReadableDatabase().rawQuery(
        "SELECT id,card_id,type,title,detail,year,month,day,repeat_yearly,remind_days,created_at,updated_at FROM memory_nodes ORDER BY created_at", null)) {
      while (c.moveToNext()) result.put(new JSONObject().put("id", c.getString(0))
          .put("cardId", c.getString(1)).put("type", c.getString(2)).put("title", c.getString(3))
          .put("detail", c.getString(4)).put("year", c.getInt(5)).put("month", c.getInt(6))
          .put("day", c.getInt(7)).put("repeatYearly", c.getInt(8) != 0).put("remindDays", c.getInt(9))
          .put("createdAt", c.getLong(10)).put("updatedAt", c.getLong(11)));
    }
    return result;
  }

  static final class Memory {
    final long id;
    final String content;

    Memory(long id, String content) {
      this.id = id;
      this.content = content;
    }
  }

  static final class MemoryNode {
    final String id, type, title, detail;final int year, month, day, remindDays;
    final boolean repeatYearly;final long createdAt, updatedAt;
    MemoryNode(String id, String type, String title, String detail, int year, int month, int day,
        boolean repeatYearly, int remindDays, long createdAt, long updatedAt) {
      this.id=id;this.type=type;this.title=title;this.detail=detail;this.year=year;this.month=month;
      this.day=day;this.repeatYearly=repeatYearly;this.remindDays=remindDays;
      this.createdAt=createdAt;this.updatedAt=updatedAt;
    }
    String typeLabel() { switch(type){case "birthday":return "生日";case "anniversary":return "纪念日";case "festival":return "节日";default:return "共同经历";} }
    String dateLabel() { return (year > 0 && !repeatYearly ? year + "年" : "每年") + month + "月" + day + "日"; }
    int daysUntil() {
      Calendar now=Calendar.getInstance();zero(now);int targetYear=year;
      if(repeatYearly||targetYear<=0)targetYear=now.get(Calendar.YEAR);
      Calendar target=Calendar.getInstance();target.clear();target.set(targetYear,month-1,day);
      if(repeatYearly&&target.before(now)){target.set(Calendar.YEAR,targetYear+1);}
      long delta=(target.getTimeInMillis()-now.getTimeInMillis())/86_400_000L;
      if(!repeatYearly&&delta<0)return -1;
      return (int)Math.max(0,Math.min(Integer.MAX_VALUE,delta));
    }
    private static void zero(Calendar value){value.set(Calendar.HOUR_OF_DAY,0);value.set(Calendar.MINUTE,0);value.set(Calendar.SECOND,0);value.set(Calendar.MILLISECOND,0);}
  }

  static final class Diary {
    final String id, ownerType, ownerId, title, content, sharedWith;
    final long createdAt, updatedAt;

    Diary(String id, String ownerType, String ownerId, String title, String content,
        String sharedWith, long createdAt, long updatedAt) {
      this.id = id;
      this.ownerType = ownerType;
      this.ownerId = ownerId;
      this.title = title;
      this.content = content;
      this.sharedWith = sharedWith == null ? "" : sharedWith;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }
  }

  static final class RealtimeEntry {
    final String sourceKey, cardId, content, vector;final long updated;final double score;
    RealtimeEntry(String sourceKey,String cardId,String content,String vector,long updated,double score){this.sourceKey=sourceKey;this.cardId=cardId;this.content=content;this.vector=vector;this.updated=updated;this.score=score;}
  }
}
