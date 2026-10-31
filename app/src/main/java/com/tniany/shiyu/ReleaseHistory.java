package com.tniany.shiyu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ReleaseHistory {
  public static final class Entry {
    public final String version;
    public final String publishedAt;
    public final List<String> notes;

    Entry(String version,String publishedAt,List<String> notes) {
      this.version=version;this.publishedAt=publishedAt;
      this.notes=Collections.unmodifiableList(new ArrayList<>(notes));
    }

    Entry(String version,String publishedAt,String... notes) {
      this(version,publishedAt,Arrays.asList(notes));
    }
  }

  private static final List<Entry> FALLBACK=Arrays.asList(
      new Entry("0.9.6.1","2026年10月31日",
          "新增“时遇市场”，可以浏览、安装、应用主题与插件。",
          "主题和插件可以提供自己的设置项，并支持从本地导入自定义主题与插件。",
          "玻璃材质默认开启，优化底部导航栏文字、主题应用、回滚和旧缓存淘汰。",
          "修复停止群聊生成、关闭工具与 MCP 后仍继续请求或写入回复的问题。"),
      new Entry("0.9.6","2026年10月31日",
          "新增“时遇市场”，可以浏览、安装、应用主题与插件。",
          "主题和插件可以提供自己的设置项，并支持从本地导入自定义主题。",
          "玻璃材质默认开启，优化底部导航栏文字、主题应用、回滚和旧缓存淘汰。",
          "修复停止群聊生成、关闭工具与 MCP 后仍继续请求或写入回复的问题。"),
      new Entry("0.7.8","2026年8月3日",
          "表情包聊天记录支持长按菜单，可直接删除对应记录。",
          "支持将其他聊天中的角色内容分享给当前角色。",
          "支持分享其他角色消息。",
          "新增发送礼物功能。",
          "生成图片过程中支持点击取消生成。",
          "主动生成图片入口已移动到聊天页的更多功能中。",
          "支持将自己的外观配置导出为 JSON 文件。",
          "支持从 JSON 文件导入并应用外观配置。",
          "支持通过系统分享功能将外观配置发送给其他用户。",
          "可迁移主题、颜色、气泡样式、字号、布局、玻璃效果和刷新率等设置。",
          "新增墨迹天气 API 支持。",
          "配置后，模型可获取天气信息并结合天气自然回复。"));

  private ReleaseHistory() {}

  public static List<Entry> bundled() { return FALLBACK; }

  public static List<Entry> fromJson(String raw) {
    if(raw==null||raw.trim().isEmpty())return Collections.emptyList();
    try {
      String clean=raw.trim();
      JSONArray history=clean.startsWith("[")?new JSONArray(clean)
          :new JSONObject(clean).optJSONArray("release_history");
      if(history==null)return Collections.emptyList();
      ArrayList<Entry> result=new ArrayList<>();
      for(int i=0;i<history.length();i++){
        JSONObject item=history.optJSONObject(i);if(item==null)continue;
        String version=item.optString("version").replaceFirst("^[vV]","").trim();
        if(version.isEmpty())continue;
        ArrayList<String> notes=new ArrayList<>();
        JSONArray values=item.optJSONArray("notes");
        if(values!=null)for(int j=0;j<values.length();j++){
          String note=values.optString(j).trim();if(!note.isEmpty())notes.add(note);
        }
        if(notes.isEmpty())for(String line:item.optString("release_notes").split("\\r?\\n")){
          String note=line.replaceFirst("^[\\s•?*-]+","").trim();if(!note.isEmpty())notes.add(note);
        }
        result.add(new Entry(version,formatDate(item.optString("published_at",item.optString("publishedAt"))),notes));
      }
      result.sort((left,right)->compare(right.version,left.version));
      return Collections.unmodifiableList(result);
    } catch(Exception ignored) { return Collections.emptyList(); }
  }

  /** Selects every release newer than the installed version, newest first. */
  public static String notesForUpdate(JSONObject manifest,String installedVersion,String targetVersion) {
    String fallback=manifest.optString("release_notes","").trim();
    JSONArray history=manifest.optJSONArray("release_history");
    if(history==null||history.length()==0)return fallback;
    ArrayList<JSONObject> selected=new ArrayList<>();
    for(int i=0;i<history.length();i++){
      JSONObject item=history.optJSONObject(i);if(item==null)continue;
      String version=item.optString("version","").replaceFirst("^[vV]","");
      if(version.isEmpty()||compare(version,installedVersion)<=0||compare(version,targetVersion)>0)continue;
      selected.add(item);
    }
    selected.sort((left,right)->compare(right.optString("version",""),left.optString("version","")));
    if(selected.isEmpty())return fallback;
    StringBuilder output=new StringBuilder();
    for(JSONObject item:selected){
      String notes=notes(item);if(notes.isEmpty())continue;
      if(output.length()>0)output.append("\n\n");
      output.append("时遇 ").append(item.optString("version")).append('\n').append(notes);
    }
    return output.length()==0?fallback:output.toString();
  }

  static int compare(String left,String right) {
    String[] a=normalize(left).split("\\."),b=normalize(right).split("\\.");
    for(int i=0;i<Math.max(a.length,b.length);i++){
      int x=i<a.length?part(a[i]):0,y=i<b.length?part(b[i]):0;
      if(x!=y)return Integer.compare(x,y);
    }
    return 0;
  }

  private static String notes(JSONObject item) {
    JSONArray values=item.optJSONArray("notes");
    if(values==null)return item.optString("release_notes","").trim();
    StringBuilder result=new StringBuilder();
    for(int i=0;i<values.length();i++){
      String value=values.optString(i,"").trim();if(value.isEmpty())continue;
      if(result.length()>0)result.append('\n');result.append("• ").append(value);
    }
    return result.toString();
  }

  private static String formatDate(String raw) {
    String value=raw==null?"":raw.trim();
    if(value.matches("\\d{4}-\\d{2}-\\d{2}.*")){
      String[] parts=value.substring(0,10).split("-");
      return parts[0]+"年"+Integer.parseInt(parts[1])+"月"+Integer.parseInt(parts[2])+"日";
    }
    return value;
  }

  private static String normalize(String value) {
    if(value==null)return "0";String normalized=value.trim().replaceFirst("^[vV]","");
    int separator=normalized.indexOf('-');return separator<0?normalized:normalized.substring(0,separator);
  }

  private static int part(String value) {
    try{return Integer.parseInt(value.replaceAll("[^0-9].*$",""));}
    catch(Exception ignored){return 0;}
  }
}
