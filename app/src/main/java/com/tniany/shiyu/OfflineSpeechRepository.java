package com.tniany.shiyu;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class OfflineSpeechRepository {
  static final String SOURCE_URL = "https://github.com/alphacep/vosk-api";
  static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip";
  static final String MODEL_LABEL = "Vosk 中文小模型 0.22";

  interface Progress { void update(int value, String label); }

  private OfflineSpeechRepository() {}

  static File modelDirectory(Context context) {
    return new File(context.getFilesDir(), "offline_speech/vosk-model-small-cn-0.22");
  }

  static boolean isInstalled(Context context) {
    File root = modelDirectory(context);
    return new File(root, "am/final.mdl").isFile() && new File(root, "conf/model.conf").isFile();
  }

  static long installedBytes(Context context) {
    return directoryBytes(modelDirectory(context));
  }

  static void download(Context context, Progress progress) throws Exception {
    File archive = new File(context.getCacheDir(), "vosk_" + UUID.randomUUID() + ".zip");
    File extracted = new File(context.getCacheDir(), "vosk_" + UUID.randomUUID());
    extracted.mkdirs();
    try {
      progress.update(1, "正在连接模型源");
      HttpURLConnection connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
      connection.setConnectTimeout(25_000);
      connection.setReadTimeout(120_000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("User-Agent", "ShiYu-Android/" + BuildConfig.VERSION_NAME);
      int code = connection.getResponseCode();
      if (code < 200 || code >= 300) throw new IllegalStateException("模型服务器 HTTP " + code);
      long expected = Math.max(1, connection.getContentLengthLong());
      long total = 0;
      try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
           FileOutputStream output = new FileOutputStream(archive)) {
        byte[] buffer = new byte[32 * 1024];
        int read;
        while ((read = input.read(buffer)) > 0) {
          total += read;
          if (total > 150L * 1024 * 1024) throw new IllegalStateException("模型压缩包超过 150 MB");
          output.write(buffer, 0, read);
          progress.update((5 + total * 60 / expected > 65 ? 65 : (int) (5 + total * 60 / expected)), "正在下载模型");
        }
      } finally {
        connection.disconnect();
      }
      int files = 0;
      long extractedBytes = 0;
      try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(archive)))) {
        ZipEntry entry;
        byte[] buffer = new byte[32 * 1024];
        while ((entry = zip.getNextEntry()) != null) {
          if (++files > 3000) throw new IllegalStateException("模型文件数量超过安全限制");
          String name = entry.getName().replace('\\', '/');
          if (name.startsWith("/") || name.contains("../")) throw new IllegalStateException("模型包包含不安全路径");
          File target = safeFile(extracted, name);
          if (entry.isDirectory()) target.mkdirs();
          else {
            target.getParentFile().mkdirs();
            try (FileOutputStream output = new FileOutputStream(target)) {
              int read;
              while ((read = zip.read(buffer)) > 0) {
                extractedBytes += read;
                if (extractedBytes > 300L * 1024 * 1024) throw new IllegalStateException("模型解压后超过 300 MB");
                output.write(buffer, 0, read);
              }
            }
          }
          zip.closeEntry();
          progress.update(Math.min(96, 66 + files / 8), "正在校验模型文件");
        }
      }
      File modelRoot = findModelRoot(extracted);
      if (modelRoot == null) throw new IllegalStateException("下载包中没有可用 Vosk 模型");
      File destination = modelDirectory(context);
      deleteRecursively(destination);
      destination.getParentFile().mkdirs();
      copyDirectory(modelRoot, destination);
      if (!isInstalled(context)) throw new IllegalStateException("模型文件不完整");
      progress.update(100, "离线模型已就绪");
    } finally {
      archive.delete();
      deleteRecursively(extracted);
    }
  }

  static void delete(Context context) {
    deleteRecursively(modelDirectory(context));
  }

  private static File findModelRoot(File root) {
    if (new File(root, "am/final.mdl").isFile()) return root;
    File[] children = root.listFiles();
    if (children != null) for (File child : children) if (child.isDirectory()) {
      File found = findModelRoot(child);
      if (found != null) return found;
    }
    return null;
  }

  private static File safeFile(File root, String path) throws Exception {
    File file = new File(root, path).getCanonicalFile();
    if (!file.getPath().startsWith(root.getCanonicalPath() + File.separator))
      throw new IllegalStateException("模型路径越界");
    return file;
  }

  private static void copyDirectory(File source, File target) throws Exception {
    if (source.isDirectory()) {
      target.mkdirs();
      File[] children = source.listFiles();
      if (children != null) for (File child : children) copyDirectory(child, new File(target, child.getName()));
    } else try (java.io.FileInputStream input = new java.io.FileInputStream(source);
                FileOutputStream output = new FileOutputStream(target)) {
      byte[] buffer = new byte[32 * 1024];
      int read;
      while ((read = input.read(buffer)) > 0) output.write(buffer, 0, read);
    }
  }

  private static long directoryBytes(File file) {
    if (!file.exists()) return 0;
    if (file.isFile()) return file.length();
    long total = 0;
    File[] children = file.listFiles();
    if (children != null) for (File child : children) total += directoryBytes(child);
    return total;
  }

  private static void deleteRecursively(File file) {
    if (file == null || !file.exists()) return;
    File[] children = file.listFiles();
    if (children != null) for (File child : children) deleteRecursively(child);
    file.delete();
  }
}
