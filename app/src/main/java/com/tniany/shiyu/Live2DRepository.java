package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

final class Live2DRepository {
  static final String CORE_SOURCE_URL = "https://cdn.jsdelivr.net/npm/live2dcubismcore@1.0.2/live2dcubismcore.min.js";
  private static final String MODEL_ID = "desktop_pet_live2d_model_id";
  private static final String MODEL_ENTRY = "desktop_pet_live2d_model_entry";
  private static final String MODEL_NAME = "desktop_pet_live2d_model_name";
  private static final long MAX_ARCHIVE_BYTES = 300L * 1024 * 1024;
  private static final int MAX_FILES = 2500;

  private Live2DRepository() {}

  static final class ModelInfo {
    private final String id;
    private final String name;

    ModelInfo(String id, String name) {
      this.id = id;
      this.name = name;
    }

    String getId() {
      return id;
    }

    String getName() {
      return name;
    }
  }

  static File runtimeFile(Context context) {
    return new File(context.getFilesDir(), "live2d_runtime/live2dcubismcore.min.js");
  }

  private static File legacyModelDirectory(Context context) {
    return new File(context.getFilesDir(), "live2d_model");
  }

  private static File modelsDirectory(Context context) {
    return new File(context.getFilesDir(), "live2d_models");
  }

  private static File modelDirectory(Context context) {
    ensureLegacyModelMigrated(context);
    String id = prefs(context).getString(MODEL_ID, "");
    return id.isEmpty() ? new File(modelsDirectory(context), "missing") : safeFile(modelsDirectory(context), id);
  }

  static boolean hasRuntime(Context context) {
    return runtimeFile(context).isFile() && runtimeFile(context).length() > 64 * 1024;
  }

  static boolean hasModel(Context context) {
    String entry = prefs(context).getString(MODEL_ENTRY, "");
    if (entry.isEmpty()) return false;
    try {
      return safeFile(modelDirectory(context), entry).isFile();
    } catch (IllegalStateException ignored) {
      return false;
    }
  }

  static boolean ready(Context context) {
    return hasRuntime(context) && hasModel(context);
  }

  static String modelEntry(Context context) {
    return prefs(context).getString(MODEL_ENTRY, "");
  }

  static String modelName(Context context) {
    return prefs(context).getString(MODEL_NAME, "未导入模型");
  }

  static String selectedModelId(Context context) {
    ensureSelection(context);
    return prefs(context).getString(MODEL_ID, "");
  }

  static ArrayList<ModelInfo> listModels(Context context) {
    ensureLegacyModelMigrated(context);
    ArrayList<ModelInfo> models = scanModels(context);
    ensureSelection(context, models);
    return models;
  }

  static boolean selectModel(Context context, String id) {
    ModelRecord record = findModel(context, id);
    if (record == null) return false;
    saveSelection(context, record);
    return true;
  }

  static void deleteModel(Context context, String id) {
    ensureLegacyModelMigrated(context);
    File directory = safeFile(modelsDirectory(context), id);
    deleteRecursively(directory);
    ArrayList<ModelInfo> models = scanModels(context);
    String selected = prefs(context).getString(MODEL_ID, "");
    if (id.equals(selected)) {
      clearSelection(context);
      ensureSelection(context, models);
    }
  }

  static void importRuntime(Context context, Uri uri) throws Exception {
    File target = runtimeFile(context);
    File temporary = new File(context.getCacheDir(), "cubism_core_" + UUID.randomUUID() + ".js");
    temporary.getParentFile().mkdirs();
    try (InputStream input = context.getContentResolver().openInputStream(uri);
         FileOutputStream output = new FileOutputStream(temporary)) {
      if (input == null) throw new IllegalStateException("无法读取 Cubism Core 文件");
      byte[] buffer = new byte[16 * 1024];
      int read;
      while ((read = input.read(buffer)) > 0) {
        output.write(buffer, 0, read);
      }
    }
    String prefix = readPrefix(temporary, 96 * 1024);
    if (!prefix.contains("Live2DCubismCore") && !prefix.contains("CubismCore")) {
      temporary.delete();
      throw new IllegalStateException("所选文件不是 live2dcubismcore.min.js");
    }
    target.getParentFile().mkdirs();
    if (target.exists() && !target.delete()) throw new IllegalStateException("无法替换旧 Cubism Core");
    if (!temporary.renameTo(target)) {
      copy(temporary, target);
      temporary.delete();
    }
  }

  interface Progress { void update(int value, String label); }

  static void downloadRuntime(Context context, Progress progress) throws Exception {
    File target = runtimeFile(context);
    File temporary = new File(context.getCacheDir(), "cubism_core_" + UUID.randomUUID() + ".js");
    HttpURLConnection connection = null;
    try {
      progress.update(2, "正在连接下载源");
      connection = (HttpURLConnection) new URL(CORE_SOURCE_URL).openConnection();
      connection.setConnectTimeout(25_000);
      connection.setReadTimeout(120_000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("User-Agent", "ShiYu-Android/" + BuildConfig.VERSION_NAME);
      int code = connection.getResponseCode();
      if (code < 200 || code >= 300) throw new IllegalStateException("Core 下载 HTTP " + code);
      long expected = Math.max(1, connection.getContentLengthLong());
      long total = 0;
      try (InputStream input = new BufferedInputStream(connection.getInputStream());
           FileOutputStream output = new FileOutputStream(temporary)) {
        byte[] buffer = new byte[32 * 1024];
        int read;
        while ((read = input.read(buffer)) > 0) {
          total += read;
          output.write(buffer, 0, read);
          progress.update(Math.min(94, (int) (5 + total * 89 / expected)), "正在下载 Cubism Core");
        }
      }
      validateRuntime(temporary);
      target.getParentFile().mkdirs();
      if (target.exists() && !target.delete()) throw new IllegalStateException("无法替换旧 Cubism Core");
      if (!temporary.renameTo(target)) copy(temporary, target);
      progress.update(100, "Cubism Core 已就绪");
    } finally {
      if (connection != null) connection.disconnect();
      temporary.delete();
    }
  }

  private static void validateRuntime(File file) throws Exception {
    String prefix = readPrefix(file, 96 * 1024);
    if (!prefix.contains("Live2DCubismCore") && !prefix.contains("CubismCore"))
      throw new IllegalStateException("下载内容不是有效的 Cubism Core");
  }

  static void importModelZip(Context context, Uri uri) throws Exception {
    File temporary = new File(context.getCacheDir(), "live2d_model_" + UUID.randomUUID());
    temporary.mkdirs();
    ArrayList<File> candidates = new ArrayList<>();
    long total = 0;
    int count = 0;
    InputStream opened = context.getContentResolver().openInputStream(uri);
    if (opened == null) throw new IllegalStateException("无法读取 Live2D 模型包");
    try (InputStream raw = opened;
         ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
      byte[] buffer = new byte[16 * 1024];
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (++count > MAX_FILES) throw new IllegalStateException("模型包文件数量超过安全限制");
        String name = entry.getName().replace('\\', '/');
        if (name.startsWith("/") || name.contains("../")) throw new IllegalStateException("模型包包含不安全路径");
        File output = safeFile(temporary, name);
        if (entry.isDirectory()) {
          output.mkdirs();
        } else {
          File parent = output.getParentFile();
          if (parent != null) parent.mkdirs();
          try (FileOutputStream file = new FileOutputStream(output)) {
            int read;
            while ((read = zip.read(buffer)) > 0) {
              total += read;
              if (total > MAX_ARCHIVE_BYTES) throw new IllegalStateException("模型包解压后超过 300 MB");
              file.write(buffer, 0, read);
            }
          }
          if (name.toLowerCase().endsWith(".model3.json")) candidates.add(output);
        }
        zip.closeEntry();
      }
    } catch (Exception error) {
      deleteRecursively(temporary);
      throw error;
    }
    if (candidates.isEmpty()) {
      deleteRecursively(temporary);
      throw new IllegalStateException("模型包中没有 .model3.json");
    }
    candidates.sort(Comparator.comparingInt(file -> relative(temporary, file).length()));
    File model = candidates.get(0);
    validateModel(temporary, model);
    ensureLegacyModelMigrated(context);
    File destination = new File(modelsDirectory(context), UUID.randomUUID().toString());
    if (!temporary.renameTo(destination)) {
      copyDirectory(temporary, destination);
      deleteRecursively(temporary);
    }
    ArrayList<File> installedCandidates = new ArrayList<>();
    collectModelCandidates(destination, installedCandidates);
    if (installedCandidates.isEmpty()) throw new IllegalStateException("导入后的模型入口不存在");
    installedCandidates.sort(Comparator.comparingInt(file -> relative(destination, file).length()));
    File installedModel = installedCandidates.get(0);
    validateModel(destination, installedModel);
    String selectedEntry = relative(destination, installedModel);
    String selectedName = installedModel.getName().replaceFirst("(?i)\\.model3\\.json$", "");
    prefs(context).edit()
        .putString(MODEL_ID, destination.getName())
        .putString(MODEL_ENTRY, selectedEntry)
        .putString(MODEL_NAME, selectedName)
        .apply();
  }

  static InputStream openModelFile(Context context, String relativePath) throws Exception {
    File file = safeFile(modelDirectory(context), relativePath);
    if (!file.isFile()) throw new IllegalStateException("模型资源不存在");
    return new FileInputStream(file);
  }

  static void clearModel(Context context) {
    String id = selectedModelId(context);
    if (!id.isEmpty()) deleteModel(context, id);
  }

  private static final class ModelRecord {
    final String id;
    final String name;
    final String entry;

    ModelRecord(String id, String name, String entry) {
      this.id = id;
      this.name = name;
      this.entry = entry;
    }
  }

  private static void ensureLegacyModelMigrated(Context context) {
    File legacy = legacyModelDirectory(context);
    if (!legacy.exists()) return;
    ArrayList<File> candidates = new ArrayList<>();
    collectModelCandidates(legacy, candidates);
    if (candidates.isEmpty()) {
      deleteRecursively(legacy);
      return;
    }
    File library = modelsDirectory(context);
    library.mkdirs();
    File destination = new File(library, "legacy-" + UUID.randomUUID());
    try {
      if (!legacy.renameTo(destination)) {
        copyDirectory(legacy, destination);
        deleteRecursively(legacy);
      }
      ModelRecord record = readModel(destination);
      if (record == null) {
        deleteRecursively(destination);
        clearSelection(context);
      } else {
        saveSelection(context, new ModelRecord(destination.getName(), record.name, record.entry));
      }
    } catch (Exception error) {
      deleteRecursively(destination);
    }
  }

  private static void ensureSelection(Context context) {
    ensureLegacyModelMigrated(context);
    ensureSelection(context, scanModels(context));
  }

  private static void ensureSelection(Context context, ArrayList<ModelInfo> models) {
    String selected = prefs(context).getString(MODEL_ID, "");
    if (!selected.isEmpty() && findModel(context, selected) != null) return;
    if (models.isEmpty()) {
      clearSelection(context);
    } else {
      ModelRecord first = findModel(context, models.get(0).id);
      if (first != null) saveSelection(context, first);
    }
  }

  private static ArrayList<ModelInfo> scanModels(Context context) {
    ArrayList<ModelInfo> result = new ArrayList<>();
    File[] directories = modelsDirectory(context).listFiles(File::isDirectory);
    if (directories == null) return result;
    java.util.Arrays.sort(directories, Comparator.comparing(File::getName));
    for (File directory : directories) {
      ModelRecord record = readModel(directory);
      if (record != null) result.add(new ModelInfo(directory.getName(), record.name));
    }
    return result;
  }

  private static ModelRecord findModel(Context context, String id) {
    if (id == null || id.isEmpty()) return null;
    ensureLegacyModelMigrated(context);
    File directory;
    try {
      directory = safeFile(modelsDirectory(context), id);
    } catch (IllegalStateException error) {
      return null;
    }
    ModelRecord record = readModel(directory);
    return record == null ? null : new ModelRecord(id, record.name, record.entry);
  }

  private static ModelRecord readModel(File directory) {
    try {
      ArrayList<File> candidates = new ArrayList<>();
      collectModelCandidates(directory, candidates);
      if (candidates.isEmpty()) return null;
      candidates.sort(Comparator.comparingInt(file -> relative(directory, file).length()));
      File model = candidates.get(0);
      validateModel(directory, model);
      return new ModelRecord(directory.getName(), model.getName().replaceFirst("(?i)\\.model3\\.json$", ""), relative(directory, model));
    } catch (Exception ignored) {
      return null;
    }
  }

  private static void saveSelection(Context context, ModelRecord record) {
    prefs(context).edit()
        .putString(MODEL_ID, record.id)
        .putString(MODEL_ENTRY, record.entry)
        .putString(MODEL_NAME, record.name)
        .apply();
  }

  private static void clearSelection(Context context) {
    prefs(context).edit().remove(MODEL_ID).remove(MODEL_ENTRY).remove(MODEL_NAME).apply();
  }

  private static void validateModel(File packageRoot, File modelFile) throws Exception {
    JSONObject model = new JSONObject(readPrefix(modelFile, 2 * 1024 * 1024));
    JSONObject references = model.optJSONObject("FileReferences");
    if (references == null) throw new IllegalStateException("model3.json 缺少 FileReferences");
    File modelRoot = modelFile.getParentFile();
    String moc = references.optString("Moc");
    if (moc.isEmpty() || !safeReference(packageRoot, modelRoot, moc).isFile())
      throw new IllegalStateException("模型包缺少 .moc3 文件");
    JSONArray textures = references.optJSONArray("Textures");
    if (textures == null || textures.length() == 0) throw new IllegalStateException("模型包没有纹理图片");
    for (int i = 0; i < textures.length(); i++) {
      if (!safeReference(packageRoot, modelRoot, textures.optString(i)).isFile())
        throw new IllegalStateException("模型纹理文件不完整");
    }
  }

  private static File safeReference(File packageRoot, File modelRoot, String reference) {
    try {
      File file = new File(modelRoot, reference.replace('\\', '/')).getCanonicalFile();
      File root = packageRoot.getCanonicalFile();
      String rootPath = root.getPath() + File.separator;
      if (!file.getPath().startsWith(rootPath) && !file.equals(root))
        throw new IllegalStateException("模型资源路径越出 ZIP 包");
      return file;
    } catch (java.io.IOException error) {
      throw new IllegalStateException("无法解析模型资源路径", error);
    }
  }

  private static File safeFile(File root, String relativePath) {
    try {
      File file = new File(root, relativePath).getCanonicalFile();
      String rootPath = root.getCanonicalPath() + File.separator;
      if (!file.getPath().startsWith(rootPath) && !file.equals(root.getCanonicalFile()))
        throw new IllegalStateException("路径越出模型目录");
      return file;
    } catch (java.io.IOException error) {
      throw new IllegalStateException("无法解析模型路径", error);
    }
  }

  private static void collectModelCandidates(File file, ArrayList<File> candidates) {
    if (file == null || !file.exists()) return;
    if (file.isFile()) {
      if (file.getName().toLowerCase().endsWith(".model3.json")) candidates.add(file);
      return;
    }
    File[] children = file.listFiles();
    if (children != null) for (File child : children) collectModelCandidates(child, candidates);
  }

  private static String relative(File root, File child) {
    return root.toPath().relativize(child.toPath()).toString().replace('\\', '/');
  }

  private static String readPrefix(File file, int limit) throws Exception {
    StringBuilder value = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      char[] buffer = new char[8192];
      int read;
      while (value.length() < limit && (read = reader.read(buffer, 0, Math.min(buffer.length, limit - value.length()))) > 0)
        value.append(buffer, 0, read);
    }
    return value.toString();
  }

  private static void copy(File source, File target) throws Exception {
    try (InputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(target)) {
      byte[] buffer = new byte[16 * 1024];
      int read;
      while ((read = input.read(buffer)) > 0) output.write(buffer, 0, read);
    }
  }

  private static void copyDirectory(File source, File target) throws Exception {
    if (source.isDirectory()) {
      target.mkdirs();
      File[] children = source.listFiles();
      if (children != null) for (File child : children) copyDirectory(child, new File(target, child.getName()));
    } else copy(source, target);
  }

  private static void deleteRecursively(File file) {
    if (file == null || !file.exists()) return;
    File[] children = file.listFiles();
    if (children != null) for (File child : children) deleteRecursively(child);
    file.delete();
  }

  private static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
  }
}
