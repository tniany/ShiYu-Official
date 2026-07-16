package com.tniany.shiyu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

final class AppDb extends SQLiteOpenHelper {
  AppDb(Context context) {
    super(context, "shiyu.db", null, 2);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE state_data(id INTEGER PRIMARY KEY CHECK(id=1), json TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE memories(id INTEGER PRIMARY KEY AUTOINCREMENT, card_id TEXT NOT NULL, content"
            + " TEXT NOT NULL, updated INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX memory_card_idx ON memories(card_id)");
    createEmbeddingTable(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) createEmbeddingTable(db);
  }

  private static void createEmbeddingTable(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE IF NOT EXISTS realtime_library(id INTEGER PRIMARY KEY AUTOINCREMENT, card_id TEXT NOT NULL, source_key TEXT NOT NULL UNIQUE, content TEXT NOT NULL, vector TEXT NOT NULL, updated INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX IF NOT EXISTS realtime_card_idx ON realtime_library(card_id,updated DESC)");
  }

  void saveState(String json) {
    ContentValues v = new ContentValues();
    v.put("id", 1);
    v.put("json", json);
    getWritableDatabase()
        .insertWithOnConflict("state_data", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  String loadState() {
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT json FROM state_data WHERE id=1", null)) {
      return c.moveToFirst() ? c.getString(0) : "";
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
    db.delete("state_data", null, null);
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
    return b.toString();
  }

  static final class Memory {
    final long id;
    final String content;

    Memory(long id, String content) {
      this.id = id;
      this.content = content;
    }
  }

  static final class RealtimeEntry {
    final String sourceKey, cardId, content, vector;final long updated;final double score;
    RealtimeEntry(String sourceKey,String cardId,String content,String vector,long updated,double score){this.sourceKey=sourceKey;this.cardId=cardId;this.content=content;this.vector=vector;this.updated=updated;this.score=score;}
  }
}
