package com.tniany.shiyu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Small persistent task ledger shared by foreground and overlay generation features. */
final class GenerationTaskTracker {
  static final String ACTION_CHANGED = "com.tniany.shiyu.GENERATION_TASKS_CHANGED";
  private static final String KEY = "generation_tasks_v1";
  private static final String REVISION = "generation_tasks_revision";

  static final class Task {
    final String id, type, title, detail, status, sourceId, error;
    final boolean cancellable;
    final long createdAt, updatedAt;

    Task(JSONObject value) {
      id=value.optString("id");type=value.optString("type");title=value.optString("title");
      detail=value.optString("detail");status=value.optString("status");sourceId=value.optString("sourceId");
      error=value.optString("error");cancellable=value.optBoolean("cancellable",false);
      createdAt=value.optLong("createdAt");updatedAt=value.optLong("updatedAt");
    }

    boolean active() { return "active".equals(status) || "queued".equals(status); }
  }

  private GenerationTaskTracker() {}

  static String start(Context context, String type, String title, String detail, String sourceId,
      boolean cancellable) {
    String id=type+":"+UUID.randomUUID();
    try {
      put(context,new JSONObject().put("id",id).put("type",type).put("title",title)
          .put("detail",detail).put("sourceId",sourceId).put("status","active")
          .put("cancellable",cancellable).put("error","").put("createdAt",System.currentTimeMillis())
          .put("updatedAt",System.currentTimeMillis()));
    } catch(Exception ignored) {}
    return id;
  }

  static void update(Context context, String id, String detail) {
    mutate(context,id,value->value.put("detail",detail).put("updatedAt",System.currentTimeMillis()));
  }

  static void finish(Context context, String id, String detail) {
    mutate(context,id,value->value.put("status","success").put("detail",detail)
        .put("updatedAt",System.currentTimeMillis()).put("cancellable",false));
  }

  static void fail(Context context, String id, Throwable error) {
    String finalMessage=MainActivity.friendlyError(error);
    mutate(context,id,value->value.put("status","failed").put("error",finalMessage)
        .put("detail","生成失败").put("updatedAt",System.currentTimeMillis()).put("cancellable",false));
  }

  static void cancel(Context context, String id) {
    mutate(context,id,value->value.put("status","cancelled").put("detail","已停止")
        .put("updatedAt",System.currentTimeMillis()).put("cancellable",false));
  }

  static synchronized ArrayList<Task> list(Context context) {
    SharedPreferences prefs=context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE);
    ArrayList<Task> result=new ArrayList<>();
    JSONArray values=parse(prefs.getString(KEY,"[]"));
    long now=System.currentTimeMillis();boolean changed=false;
    for(int i=0;i<values.length();i++){
      JSONObject value=values.optJSONObject(i);if(value==null)continue;
      if(("active".equals(value.optString("status"))||"queued".equals(value.optString("status")))
          && now-value.optLong("updatedAt",now)>15*60_000L){
        try { value.put("status","failed").put("detail","任务被系统中断").put("error","App 进程或网络请求已结束")
            .put("cancellable",false).put("updatedAt",now);changed=true; } catch(Exception ignored) {}
      }
      result.add(new Task(value));
    }
    if(changed)save(context,values);
    return result;
  }

  static synchronized void clearHistory(Context context) {
    JSONArray current=parse(context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
    JSONArray kept=new JSONArray();
    for(int i=0;i<current.length();i++){JSONObject value=current.optJSONObject(i);if(value!=null&&("active".equals(value.optString("status"))||"queued".equals(value.optString("status"))))kept.put(value);}
    save(context,kept);
  }

  private interface Change { void apply(JSONObject value) throws Exception; }

  private static synchronized void mutate(Context context,String id,Change change){
    SharedPreferences prefs=context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE);
    JSONArray values=parse(prefs.getString(KEY,"[]"));
    for(int i=0;i<values.length();i++){JSONObject value=values.optJSONObject(i);if(value!=null&&id.equals(value.optString("id"))){try{change.apply(value);save(context,values);}catch(Exception ignored){}return;}}
  }

  private static synchronized void put(Context context,JSONObject task){
    SharedPreferences prefs=context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE);
    JSONArray old=parse(prefs.getString(KEY,"[]"));JSONArray values=new JSONArray().put(task);
    for(int i=0;i<Math.min(39,old.length());i++){JSONObject value=old.optJSONObject(i);if(value!=null&&!task.optString("id").equals(value.optString("id")))values.put(value);}
    save(context,values);
  }

  private static JSONArray parse(String value){try{return new JSONArray(value);}catch(Exception ignored){return new JSONArray();}}

  private static void save(Context context,JSONArray values){
    if (MainActivity.isDataRescueRequired(context)) return;
    SharedPreferences prefs=context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE);
    prefs.edit().putString(KEY,values.toString()).putLong(REVISION,System.currentTimeMillis()).apply();
    context.sendBroadcast(new Intent(ACTION_CHANGED).setPackage(context.getPackageName()));
  }
}
