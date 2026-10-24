package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.StatFs;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

final class LocalModelRepository {
  interface Progress { void update(int percent, String label); }
  private static final int MAX_IMPORTED_MODELS = 32;
  private static final long MAX_IMPORT_BYTES = 16L * 1024L * 1024L * 1024L;

  static final class ModelSpec {
    final String id, kind, name, description, sizeLabel, fileName, url;
    final long expectedBytes;
    final boolean imported;
    ModelSpec(String id, String kind, String name, String description, String sizeLabel,
        String fileName, String url, long expectedBytes) {
      this(id,kind,name,description,sizeLabel,fileName,url,expectedBytes,false);
    }
    ModelSpec(String id, String kind, String name, String description, String sizeLabel,
        String fileName, String url, long expectedBytes, boolean imported) {
      this.id=id;this.kind=kind;this.name=name;this.description=description;this.sizeLabel=sizeLabel;
      this.fileName=fileName;this.url=url;this.expectedBytes=expectedBytes;this.imported=imported;
    }
  }

  static final List<ModelSpec> MODELS = Arrays.asList(
      new ModelSpec("qwen25_05b_q4", "chat", "Qwen2.5 0.5B 中文小模型",
          "GGUF Q4_K_M，适合日常短聊天，建议至少 2 GB 可用内存", "约 500 MB",
          "qwen2.5-0.5b-instruct-q4_k_m.gguf",
          "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf?download=true",
          398_000_000L),
      new ModelSpec("qwen25_15b_q4", "chat", "Qwen2.5 1.5B 中文模型",
          "GGUF Q4_K_M，中文能力更强，建议至少 4 GB 可用内存", "约 1.0 GB",
          "qwen2.5-1.5b-instruct-q4_k_m.gguf",
          "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf?download=true",
          900_000_000L),
      new ModelSpec("qwen25_3b_q4", "chat", "Qwen2.5 3B 中文模型",
          "GGUF Q4_K_M，适合较长对话，建议至少 6 GB 可用内存", "约 2.0 GB",
          "qwen2.5-3b-instruct-q4_k_m.gguf",
          "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf?download=true",
          1_800_000_000L),
      new ModelSpec("qwen25_7b_q4", "chat", "Qwen2.5 7B 中文模型",
          "GGUF Q2_K，效果更完整，建议至少 8 GB 可用内存", "约 3.0 GB",
          "qwen2.5-7b-instruct-q2_k.gguf",
          "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF/resolve/main/qwen2.5-7b-instruct-q2_k.gguf?download=true",
          2_900_000_000L),
      new ModelSpec("qwen3_06b_q4", "chat", "Qwen3 0.6B 中文模型",
          "GGUF Q8_0，体积小，适合低内存设备", "约 640 MB",
          "Qwen3-0.6B-Q8_0.gguf",
          "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q8_0.gguf?download=true",
          600_000_000L),
      new ModelSpec("llama32_1b_q4", "chat", "Llama 3.2 1B Instruct",
          "GGUF Q4_K_M，英文与多语言轻量模型，建议至少 3 GB 可用内存", "约 800 MB",
          "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
          "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf?download=true",
          700_000_000L),
      new ModelSpec("gemma3_1b_q4", "chat", "Google Gemma 3 1B IT",
          "Google 开源移动端模型，GGUF Q4_K_M，适合低内存设备上的多语言对话", "约 806 MB",
          "google_gemma-3-1b-it-Q4_K_M.gguf",
          "https://huggingface.co/bartowski/google_gemma-3-1b-it-GGUF/resolve/main/google_gemma-3-1b-it-Q4_K_M.gguf?download=true",
          806_058_496L),
      new ModelSpec("phi35_mini_q4", "chat", "Phi-3.5 Mini Instruct",
          "GGUF Q4_K_M，推理能力较强，建议至少 6 GB 可用内存", "约 2.2 GB",
          "Phi-3.5-mini-instruct-Q4_K_M.gguf",
          "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf?download=true",
          2_000_000_000L),
      new ModelSpec("sd15_q4", "image", "Stable Diffusion 1.5 本地生图",
          "GGUF Q4_0，可由 App 内置引擎直接生成图片，建议至少 6 GB 可用内存", "约 1.6 GB",
          "stable-diffusion-v1-5-pruned-emaonly-Q4_0.gguf",
          "https://huggingface.co/second-state/stable-diffusion-v1-5-GGUF/resolve/main/stable-diffusion-v1-5-pruned-emaonly-Q4_0.gguf?download=true",
          1_566_768_416L),
      new ModelSpec("sd15_q41", "image", "Stable Diffusion 1.5 Q4_1",
          "GGUF Q4_1，画面细节略高，可由 App 内置引擎直接生成图片", "约 1.6 GB",
          "stable-diffusion-v1-5-pruned-emaonly-Q4_1.gguf",
          "https://huggingface.co/second-state/stable-diffusion-v1-5-GGUF/resolve/main/stable-diffusion-v1-5-pruned-emaonly-Q4_1.gguf?download=true",
          1_591_364_896L),
      new ModelSpec("anything_v5", "image", "Anything V5.0",
          "SD1.5 二次元插画模型，适合角色立绘、动漫场景和风格化画面；建议使用具体英文标签提示词",
          "约 2.0 GB", "anything-v5.safetensors",
          "https://huggingface.co/genai-archive/anything-v5/resolve/main/anything-v5.safetensors?download=true",
          2_132_626_102L),
      new ModelSpec("qteamix", "image", "QteaMix",
          "SD1.5 Q版模型，适合 chibi、可爱角色和卡通插画；提示词加入 chibi 通常更容易保持Q版比例",
          "约 2.4 GB", "QteaMix-fp16.safetensors",
          "https://huggingface.co/chenxluo/QteaMix/resolve/main/QteaMix-fp16.safetensors?download=true",
          2_546_054_463L),
      new ModelSpec("absolute_reality_181", "image", "Absolute Reality 1.8.1",
          "SD1.5 写实模型，适合人物肖像、摄影和写实场景；光线、镜头和材质提示词会明显影响效果",
          "约 2.0 GB", "absolutereality_v181.safetensors",
          "https://huggingface.co/digiplay/AbsoluteReality_v1.8.1/resolve/main/absolutereality_v181.safetensors?download=true",
          2_132_625_432L),
      new ModelSpec("chilloutmix_ni", "image", "ChilloutMix",
          "SD1.5 写实人像模型，适合人物、生活照和摄影风格；移动端优先使用 fp16 文件",
          "约 2.4 GB", "chilloutmix-Ni-ema-fp16.safetensors",
          "https://huggingface.co/swl-models/chilloutmix-ni/resolve/main/chilloutmix-Ni-ema-fp16.safetensors?download=true",
          2_546_054_799L));

  private LocalModelRepository() {}

  static List<ModelSpec> models(Context context) {
    List<ModelSpec> values=new ArrayList<>(MODELS);
    values.addAll(importedModels(context));
    return values;
  }

  static synchronized ModelSpec importModel(Context context,Uri uri,String kind) throws Exception {
    if(uri==null)throw new IllegalArgumentException("请选择要导入的模型文件");
    if(!"chat".equals(kind)&&!"image".equals(kind))throw new IllegalArgumentException("不支持的模型类型");
    List<ModelSpec> imported=importedModels(context);
    if(imported.size()>=MAX_IMPORTED_MODELS)throw new IllegalStateException("最多保存 "+MAX_IMPORTED_MODELS+" 个导入模型，请先删除不用的模型");
    String displayName=queryDisplayName(context,uri);
    if(displayName.isEmpty())displayName="local-model";
    validateExtension(displayName,kind);
    long announcedBytes=querySize(context,uri);
    if(announcedBytes>MAX_IMPORT_BYTES)throw new IllegalArgumentException("模型文件不能超过 "+formatBytes(MAX_IMPORT_BYTES));
    File dir=directory(context);
    if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("无法创建本地模型目录");
    if(announcedBytes>0&&new StatFs(dir.getAbsolutePath()).getAvailableBytes()<announcedBytes)
      throw new IllegalStateException("存储空间不足，需要至少 "+formatBytes(announcedBytes)+" 可用空间");
    String extension="";int dot=displayName.lastIndexOf('.');
    if(dot>=0)extension=displayName.substring(dot).toLowerCase(Locale.ROOT);
    String id="imported_"+UUID.randomUUID().toString();
    String safeBase=displayName.substring(0,dot>=0?dot:displayName.length()).replaceAll("[^a-zA-Z0-9._-]","_");
    if(safeBase.isEmpty())safeBase="local-model";
    if(safeBase.length()>80)safeBase=safeBase.substring(0,80);
    String fileName=safeBase+"-"+id.substring(id.length()-8)+extension;
    File temporary=new File(dir,fileName+".importing");
    File target=new File(dir,fileName);
    long copied=0L;
    try(InputStream input=context.getContentResolver().openInputStream(uri);
        FileOutputStream output=new FileOutputStream(temporary)) {
      if(input==null)throw new java.io.IOException("无法读取所选文件");
      byte[] buffer=new byte[128*1024];int read;
      while((read=input.read(buffer))>0){
        if(copied>MAX_IMPORT_BYTES-read)throw new IllegalArgumentException("模型文件不能超过 "+formatBytes(MAX_IMPORT_BYTES));
        output.write(buffer,0,read);copied+=read;
      }
      output.getFD().sync();
    } catch(Exception error) {
      temporary.delete();throw error;
    }
    if(copied<=0L){temporary.delete();throw new java.io.IOException("所选模型文件为空");}
    if(!temporary.renameTo(target)){temporary.delete();throw new java.io.IOException("无法保存导入的模型文件");}
    ModelSpec spec=new ModelSpec(id,kind,displayName,
        "从本地文件导入 · "+("chat".equals(kind)?"对话模型":"生图模型"),formatBytes(copied),
        fileName,"",copied,true);
    try{
      imported.add(spec);saveImportedModels(context,imported);
    }catch(Exception error){target.delete();throw error;}
    return spec;
  }

  static ModelSpec find(String id) {
    for (ModelSpec value : MODELS) if (value.id.equals(id)) return value;
    return null;
  }

  static ModelSpec findInstalled(Context context,String idOrFileName,String kind) {
    String selected=idOrFileName==null?"":idOrFileName.trim();
    ModelSpec fallback=null;
    for(ModelSpec value:models(context)){
      if(!kind.equals(value.kind)||!installed(context,value))continue;
      if(fallback==null)fallback=value;
      if(selected.equals(value.id)||selected.equals(value.fileName))return value;
    }
    return selected.isEmpty()||"stable-diffusion".equals(selected)?fallback:null;
  }

  static File directory(Context context) { return new File(context.getFilesDir(), "local_models"); }
  static File file(Context context, ModelSpec spec) { return new File(directory(context), new File(spec.fileName).getName()); }
  static File partialFile(Context context, ModelSpec spec) { return new File(directory(context), new File(spec.fileName).getName() + ".part"); }
  static boolean installed(Context context, ModelSpec spec) {
    File value=file(context,spec);return value.isFile() && value.length() >= spec.expectedBytes;
  }
  static long downloadedBytes(Context context, ModelSpec spec) {
    File ready=file(context,spec);if(ready.isFile())return ready.length();
    File partial=partialFile(context,spec);return partial.isFile()?partial.length():0L;
  }

  static void delete(Context context, ModelSpec spec) {
    File model=file(context,spec),partial=partialFile(context,spec);
    if(model.exists()&&!model.delete())throw new IllegalStateException("无法删除本地模型文件");
    if(partial.exists()&&!partial.delete())throw new IllegalStateException("无法删除未完成的模型文件");
    if(spec.imported){
      List<ModelSpec> values=importedModels(context);
      for(int i=values.size()-1;i>=0;i--)if(values.get(i).id.equals(spec.id))values.remove(i);
      saveImportedModels(context,values);
    }
  }

  private static List<ModelSpec> importedModels(Context context) {
    List<ModelSpec> values=new ArrayList<>();
    java.util.HashSet<String> ids=new java.util.HashSet<>(),fileNames=new java.util.HashSet<>();
    SharedPreferences prefs=context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE);
    try{
      JSONArray array=new JSONArray(prefs.getString("imported_local_models","[]"));
      for(int i=0;i<array.length();i++){
        JSONObject value=array.optJSONObject(i);if(value==null)continue;
        String id=value.optString("id"),fileName=new File(value.optString("fileName")).getName(),kind=value.optString("kind");
        if(id.isEmpty()||fileName.isEmpty()||(!"chat".equals(kind)&&!"image".equals(kind)))continue;
        if(!ids.add(id)||!fileNames.add(fileName))continue;
        long bytes=value.optLong("bytes",0L);
        File localFile=new File(directory(context),fileName);if(bytes<=0L&&localFile.isFile())bytes=localFile.length();
        values.add(new ModelSpec(id,kind,value.optString("name",fileName),
            "从本地文件导入 · "+("chat".equals(kind)?"对话模型":"生图模型"),formatBytes(bytes),
            fileName,"",bytes,true));
      }
    }catch(Exception ignored){}
    return values;
  }

  private static void saveImportedModels(Context context,List<ModelSpec> values) {
    try{
      JSONArray array=new JSONArray();
      for(ModelSpec value:values)array.put(new JSONObject().put("id",value.id).put("kind",value.kind)
          .put("name",value.name).put("fileName",value.fileName).put("bytes",value.expectedBytes));
      if(!context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE).edit()
          .putString("imported_local_models",array.toString()).commit())
        throw new IllegalStateException("无法保存本地模型记录");
    }catch(Exception error){throw new IllegalStateException("无法保存本地模型记录",error);}
  }

  private static String queryDisplayName(Context context,Uri uri) {
    try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
      if(cursor!=null&&cursor.moveToFirst()){String value=cursor.getString(0);return value==null?"":value.trim();}
    }catch(Exception ignored){}
    String value=uri.getLastPathSegment();return value==null?"":value.trim();
  }

  private static long querySize(Context context,Uri uri) {
    try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){
      if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))return Math.max(0L,cursor.getLong(0));
    }catch(Exception ignored){}
    return -1L;
  }

  private static void validateExtension(String name,String kind) {
    String lower=name.toLowerCase(Locale.ROOT);
    String[] allowed="chat".equals(kind)
        ?new String[]{".gguf"}
        :new String[]{".safetensors",".ckpt",".gguf"};
    for(String extension:allowed)if(lower.endsWith(extension))return;
    throw new IllegalArgumentException("不支持的模型文件格式");
  }

  static void download(Context context, ModelSpec spec, Progress progress) throws Exception {
    File dir=directory(context);
    if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("无法创建本地模型目录");
    File partial=partialFile(context,spec);long existing=partial.isFile()?partial.length():0L;
    long required=Math.max(256L*1024*1024,spec.expectedBytes-existing);
    StatFs stat=new StatFs(dir.getAbsolutePath());
    if(stat.getAvailableBytes()<required)throw new IllegalStateException("存储空间不足，需要至少 "+formatBytes(required)+" 可用空间");
    HttpURLConnection connection=(HttpURLConnection)new URL(downloadUrl(context,spec)).openConnection();
    connection.setConnectTimeout(20_000);connection.setReadTimeout(120_000);
    connection.setRequestProperty("User-Agent","ShiYu/"+BuildConfig.VERSION_NAME);
    if(existing>0)connection.setRequestProperty("Range","bytes="+existing+"-");
    int code=connection.getResponseCode();
    if(code==416&&existing>=spec.expectedBytes){connection.disconnect();installPartial(context,spec,partial);progress.update(100,"模型已安装");return;}
    boolean append=existing>0&&code==206;
    if(code<200||code>=300)throw new java.io.IOException("模型下载 HTTP "+code);
    if(!append)existing=0L;
    long response=0L;try{response=Long.parseLong(connection.getHeaderField("Content-Length"));}catch(Exception ignored){}
    long total=response>0?existing+response:spec.expectedBytes;
    try(InputStream input=connection.getInputStream();FileOutputStream output=new FileOutputStream(partial,append)) {
      byte[] buffer=new byte[128*1024];int read;long done=existing,lastReport=0;
      while((read=input.read(buffer))>0){output.write(buffer,0,read);done+=read;
        if(done-lastReport>=1024*1024){lastReport=done;int percent=total>0?(int)Math.min(99,done*100/total):1;
          progress.update(percent,"已下载 "+formatBytes(done)+(total>0?" / "+formatBytes(total):""));}}
      output.getFD().sync();
    } finally { connection.disconnect(); }
    long completeBytes=Math.max(spec.expectedBytes,total);
    if(partial.length()<completeBytes)throw new java.io.IOException("下载文件不完整，已保留进度供下次继续");
    installPartial(context,spec,partial);
    progress.update(100,"模型已安装");
  }

  static String downloadUrl(Context context, ModelSpec spec) {
    String mirror=context.getSharedPreferences(MainActivity.PREF,Context.MODE_PRIVATE)
        .getString("local_model_download_mirror","");
    mirror=mirror==null?"":mirror.trim();
    if(mirror.isEmpty())return spec.url;
    if(mirror.contains("{url}"))return mirror.replace("{url}",android.net.Uri.encode(spec.url));
    while(mirror.endsWith("/"))mirror=mirror.substring(0,mirror.length()-1);
    if(spec.url.startsWith("https://huggingface.co/"))
      return mirror+spec.url.substring("https://huggingface.co".length());
    return mirror+"/"+spec.url;
  }

  private static void installPartial(Context context,ModelSpec spec,File partial) throws Exception {
    File target=file(context,spec);if(target.exists()&&!target.delete())throw new java.io.IOException("无法替换旧模型文件");
    if(!partial.renameTo(target))throw new java.io.IOException("无法完成模型文件安装");
  }

  static String formatBytes(long bytes) {
    if(bytes>=1024L*1024*1024)return String.format(java.util.Locale.US,"%.1f GB",bytes/(1024d*1024*1024));
    return String.format(java.util.Locale.US,"%.0f MB",bytes/(1024d*1024));
  }
}
