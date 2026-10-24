package com.tniany.shiyu;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Writes a best-effort package of data that may still be recoverable after state loading fails. */
final class DataRescueManager {
  private static final long MAX_SINGLE_FILE_BYTES = 256L * 1024 * 1024;
  private static final long MAX_TOTAL_FILE_BYTES = 1024L * 1024 * 1024;
  private static final int MAX_FILE_COUNT = 20_000;
  private static final int MAX_DIRECTORY_DEPTH = 32;
  private static final int MAX_RECORDED_ISSUES = 2_000;
  private static final int MAX_RECORDED_EXCLUSIONS = 200;
  private static final int MAX_FAILURE_LENGTH = 32 * 1024;
  private static final int BUFFER_SIZE = 32 * 1024;

  private static final Set<String> EXCLUDED_DIRECTORIES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  "live2d_runtime",
                  "live2d_model",
                  "live2d_models",
                  "offline_speech",
                  "sticker_packs",
                  "cache",
                  "code_cache",
                  "keystore",
                  "androidkeystore")));

  private DataRescueManager() {}

  static void writeRescuePackage(Context context, OutputStream output, String failure)
      throws IOException {
    if (context == null) throw new IllegalArgumentException("context == null");
    if (output == null) throw new IllegalArgumentException("output == null");

    Context application = context.getApplicationContext();
    if (application == null) application = context;
    RescueState state = new RescueState(safeSubstring(failure, MAX_FAILURE_LENGTH));
    collectCandidates(application, state);
    state.candidates.sort(
        Comparator.comparingInt((Candidate candidate) -> candidate.priority)
            .thenComparing(candidate -> candidate.entryName));

    try (ZipOutputStream zip = new ZipOutputStream(new NonClosingOutputStream(output))) {
      zip.setLevel(Deflater.BEST_SPEED);
      Set<String> usedEntryNames = new HashSet<>();
      for (Candidate candidate : state.candidates) {
        if (!usedEntryNames.add(candidate.entryName.toLowerCase(Locale.ROOT))) {
          state.skip(candidate.entryName, "validate", "duplicate archive path");
          continue;
        }
        writeCandidate(zip, candidate, state);
      }
      writeManifest(zip, state);
    }
  }

  private static void collectCandidates(Context context, RescueState state) {
    File database = context.getDatabasePath("shiyu.db");
    File databaseRoot = database.getParentFile();
    addKnownFile(state, databaseRoot, database, "shiyu.db", 0, true);
    addKnownFile(
        state,
        databaseRoot,
        new File(database.getPath() + "-wal"),
        "shiyu.db-wal",
        1,
        false);
    addKnownFile(
        state,
        databaseRoot,
        new File(database.getPath() + "-shm"),
        "shiyu.db-shm",
        2,
        false);
    addKnownFile(
        state,
        databaseRoot,
        new File(database.getPath() + "-journal"),
        "shiyu.db-journal",
        3,
        false);

    File dataDirectory = new File(context.getApplicationInfo().dataDir);
    File preferencesRoot = new File(dataDirectory, "shared_prefs");
    addKnownFile(
        state,
        preferencesRoot,
        new File(preferencesRoot, "shiyu_v2.xml"),
        "shared_prefs/shiyu_v2.xml",
        5,
        true);

    File filesRoot = context.getFilesDir();
    try {
      File canonicalRoot = filesRoot.getCanonicalFile();
      if (!canonicalRoot.isDirectory()) {
        state.issue("files", "scan", "files directory is unavailable");
        return;
      }
      walkFiles(
          canonicalRoot,
          canonicalRoot,
          0,
          new HashSet<>(),
          state);
    } catch (Exception error) {
      state.issue("files", "scan", safeMessage(error));
    }
  }

  private static void addKnownFile(
      RescueState state,
      File root,
      File file,
      String entryName,
      int priority,
      boolean reportMissing) {
    if (root == null || file == null) {
      if (reportMissing) state.issue(entryName, "scan", "source path is unavailable");
      return;
    }
    try {
      File canonicalRoot = root.getCanonicalFile();
      File canonicalFile = file.getCanonicalFile();
      if (!isStrictDescendant(canonicalRoot, canonicalFile)) {
        state.issue(entryName, "validate", "source path escaped its allowed directory");
        return;
      }
      if (!canonicalFile.exists()) {
        if (reportMissing) state.issue(entryName, "scan", "source file is missing");
        return;
      }
      if (!canonicalFile.isFile()) {
        state.issue(entryName, "validate", "source is not a regular file");
        return;
      }
      state.addCandidate(new Candidate(canonicalRoot, canonicalFile, entryName, priority));
    } catch (Exception error) {
      state.issue(entryName, "scan", safeMessage(error));
    }
  }

  private static void walkFiles(
      File root,
      File directory,
      int depth,
      Set<String> visitedDirectories,
      RescueState state) {
    if (state.scanLimitReached) return;
    if (depth > MAX_DIRECTORY_DEPTH) {
      state.issue(logicalPath(root, directory), "scan", "directory depth limit reached");
      return;
    }
    try {
      File canonicalDirectory = directory.getCanonicalFile();
      if (!isSameOrDescendant(root, canonicalDirectory)) {
        state.issue(logicalPath(root, directory), "validate", "directory escaped files directory");
        return;
      }
      if (!visitedDirectories.add(canonicalDirectory.getPath())) {
        state.issue(logicalPath(root, directory), "validate", "directory loop detected");
        return;
      }
      File[] children = canonicalDirectory.listFiles();
      if (children == null) {
        state.issue(logicalPath(root, directory), "scan", "directory cannot be read");
        return;
      }
      Arrays.sort(
          children,
          (left, right) -> {
            int folded = left.getName().compareToIgnoreCase(right.getName());
            return folded != 0 ? folded : left.getName().compareTo(right.getName());
          });
      for (File child : children) {
        if (state.scanLimitReached) return;
        File canonicalChild;
        try {
          canonicalChild = child.getCanonicalFile();
        } catch (Exception error) {
          state.issue(logicalPath(root, child), "validate", safeMessage(error));
          continue;
        }
        if (!isStrictDescendant(root, canonicalChild)) {
          state.issue(logicalPath(root, child), "validate", "path escaped files directory");
          continue;
        }
        String relative = relativePath(root, canonicalChild);
        if (relative == null || !isSafeRelativePath(relative)) {
          state.issue(logicalPath(root, child), "validate", "unsafe relative path");
          continue;
        }
        if (canonicalChild.isDirectory()) {
          if (isExcludedDirectory(relative)) {
            state.exclude("files/" + relative);
          } else {
            walkFiles(root, canonicalChild, depth + 1, visitedDirectories, state);
          }
        } else if (canonicalChild.isFile()) {
          if (isKeystoreFile(relative)) {
            state.exclude("files/" + relative);
            continue;
          }
          state.addCandidate(
              new Candidate(
                  root,
                  canonicalChild,
                  "files/" + relative,
                  priorityForFilesPath(relative)));
        }
      }
    } catch (Exception error) {
      state.issue(logicalPath(root, directory), "scan", safeMessage(error));
    }
  }

  private static int priorityForFilesPath(String relative) {
    String lower = relative.toLowerCase(Locale.ROOT);
    int separator = lower.indexOf('/');
    String first = separator < 0 ? lower : lower.substring(0, separator);
    String name = separator < 0 ? lower : lower.substring(lower.lastIndexOf('/') + 1);
    if (name.startsWith("avatar_")
        || name.startsWith("generated_")
        || name.startsWith("moment_")
        || name.startsWith("chat_bg_")
        || name.equals("chat_background.img")
        || name.contains("background")) return 10;
    if ("restored_assets".equals(first)) return 20;
    if ("sticker_favorites".equals(first)) return 30;
    return 100;
  }

  private static void writeCandidate(ZipOutputStream zip, Candidate candidate, RescueState state)
      throws IOException {
    File file;
    try {
      file = candidate.file.getCanonicalFile();
      if (!isStrictDescendant(candidate.root, file)
          || !file.isFile()
          || !isSafeEntryName(candidate.entryName)) {
        state.skip(candidate.entryName, "validate", "source path changed or is unsafe");
        return;
      }
    } catch (Exception error) {
      state.skip(candidate.entryName, "validate", safeMessage(error));
      return;
    }

    long sizeAtOpen = file.length();
    if (sizeAtOpen < 0 || sizeAtOpen > MAX_SINGLE_FILE_BYTES) {
      state.skip(candidate.entryName, "limit", "single-file size limit exceeded");
      return;
    }
    if (sizeAtOpen > MAX_TOTAL_FILE_BYTES - state.totalBytes) {
      state.skip(candidate.entryName, "limit", "rescue package data limit exceeded");
      return;
    }

    FileInputStream input;
    try {
      input = new FileInputStream(file);
    } catch (Exception error) {
      state.skip(candidate.entryName, "open", safeMessage(error));
      return;
    }

    long maximum = Math.min(MAX_SINGLE_FILE_BYTES, MAX_TOTAL_FILE_BYTES - state.totalBytes);
    long written = 0;
    boolean complete = true;
    String readIssue = "";
    ZipEntry entry = new ZipEntry(candidate.entryName);
    entry.setTime(Math.max(0, file.lastModified()));
    zip.putNextEntry(entry);
    byte[] buffer = new byte[BUFFER_SIZE];
    try {
      while (written < maximum) {
        int requested = (int) Math.min(buffer.length, maximum - written);
        int read;
        try {
          read = input.read(buffer, 0, requested);
        } catch (IOException error) {
          complete = false;
          readIssue = safeMessage(error);
          break;
        }
        if (read < 0) break;
        if (read == 0) continue;
        zip.write(buffer, 0, read);
        written += read;
      }
      if (complete && written == maximum) {
        try {
          if (input.read() >= 0) {
            complete = false;
            readIssue = "file grew or exceeded an export size limit";
          }
        } catch (IOException error) {
          complete = false;
          readIssue = safeMessage(error);
        }
      }
    } finally {
      try {
        input.close();
      } catch (IOException error) {
        complete = false;
        if (readIssue.isEmpty()) readIssue = safeMessage(error);
      }
      zip.closeEntry();
    }

    state.totalBytes += written;
    state.included.add(
        new IncludedFile(candidate.entryName, sizeAtOpen, written, file.lastModified(), complete));
    if (!complete) state.issue(candidate.entryName, "read", readIssue);
  }

  private static void writeManifest(ZipOutputStream zip, RescueState state) throws IOException {
    byte[] bytes;
    try {
      JSONObject manifest =
          new JSONObject()
              .put("kind", "shiyu-data-rescue")
              .put("format", 1)
              .put("appVersion", BuildConfig.VERSION_NAME)
              .put("createdAt", System.currentTimeMillis())
              .put("failure", state.failure)
              .put(
                  "limits",
                  new JSONObject()
                      .put("singleFileBytes", MAX_SINGLE_FILE_BYTES)
                      .put("totalFileBytes", MAX_TOTAL_FILE_BYTES)
                      .put("fileCount", MAX_FILE_COUNT))
              .put("totalBytes", state.totalBytes)
              .put("includedFileCount", state.included.size())
              .put("skippedFileCount", state.skippedFiles)
              .put("excludedPathCount", state.excludedCount)
              .put("suppressedIssueCount", state.suppressedIssues)
              .put("scanLimitReached", state.scanLimitReached);

      JSONArray included = new JSONArray();
      for (IncludedFile file : state.included) {
        included.put(
            new JSONObject()
                .put("path", file.path)
                .put("sizeAtOpen", file.sizeAtOpen)
                .put("bytesWritten", file.bytesWritten)
                .put("lastModified", file.lastModified)
                .put("complete", file.complete));
      }
      manifest.put("files", included);

      JSONArray excluded = new JSONArray();
      for (String path : state.excludedPaths) excluded.put(path);
      manifest.put("excludedPaths", excluded);

      JSONArray issues = new JSONArray();
      for (Issue issue : state.issues) {
        issues.put(
            new JSONObject()
                .put("path", issue.path)
                .put("stage", issue.stage)
                .put("message", issue.message));
      }
      manifest.put("issues", issues);
      bytes = manifest.toString(2).getBytes(StandardCharsets.UTF_8);
    } catch (Exception error) {
      throw new IOException("Unable to create rescue manifest", error);
    }

    ZipEntry entry = new ZipEntry("manifest.json");
    entry.setTime(System.currentTimeMillis());
    zip.putNextEntry(entry);
    zip.write(bytes);
    zip.closeEntry();
  }

  private static boolean isExcludedDirectory(String relative) {
    String[] segments = relative.toLowerCase(Locale.ROOT).split("/");
    for (String segment : segments) {
      if (EXCLUDED_DIRECTORIES.contains(segment)) return true;
    }
    return false;
  }

  private static boolean isKeystoreFile(String relative) {
    String lower = relative.toLowerCase(Locale.ROOT);
    int slash = lower.lastIndexOf('/');
    String name = slash < 0 ? lower : lower.substring(slash + 1);
    return name.endsWith(".jks")
        || name.endsWith(".keystore")
        || name.endsWith(".p12")
        || name.endsWith(".pfx")
        || name.equals("keystore")
        || name.startsWith("keystore.");
  }

  private static boolean isSameOrDescendant(File root, File candidate) {
    String rootPath = root.getPath();
    String candidatePath = candidate.getPath();
    return candidatePath.equals(rootPath)
        || candidatePath.startsWith(rootPath + File.separator);
  }

  private static boolean isStrictDescendant(File root, File candidate) {
    return candidate.getPath().startsWith(root.getPath() + File.separator);
  }

  private static String relativePath(File root, File candidate) {
    String rootPath = root.getPath();
    String candidatePath = candidate.getPath();
    if (!candidatePath.startsWith(rootPath + File.separator)) return null;
    return candidatePath
        .substring(rootPath.length() + 1)
        .replace(File.separatorChar, '/')
        .replace('\\', '/');
  }

  private static boolean isSafeRelativePath(String path) {
    if (path.isEmpty() || path.length() > 1000 || path.startsWith("/") || path.contains("\\"))
      return false;
    String[] segments = path.split("/", -1);
    for (String segment : segments) {
      if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) return false;
      for (int i = 0; i < segment.length(); i++) {
        char value = segment.charAt(i);
        if (value == 0 || value < 0x20 || value == 0x7f) return false;
      }
    }
    return true;
  }

  private static boolean isSafeEntryName(String path) {
    return isSafeRelativePath(path) && !path.equalsIgnoreCase("manifest.json");
  }

  private static String logicalPath(File root, File file) {
    try {
      String relative = relativePath(root.getCanonicalFile(), file.getCanonicalFile());
      return relative == null || relative.isEmpty() ? "files" : "files/" + relative;
    } catch (Exception ignored) {
      return "files/unknown";
    }
  }

  private static String safeMessage(Throwable error) {
    if (error == null) return "unknown error";
    String message = error.getMessage();
    String value =
        error.getClass().getSimpleName()
            + (message == null || message.trim().isEmpty() ? "" : ": " + message.trim());
    return safeSubstring(value, 1000);
  }

  private static String safeSubstring(String value, int maximum) {
    if (value == null) return "";
    return value.length() <= maximum ? value : value.substring(0, maximum) + "...";
  }

  private static final class Candidate {
    final File root;
    final File file;
    final String entryName;
    final int priority;

    Candidate(File root, File file, String entryName, int priority) {
      this.root = root;
      this.file = file;
      this.entryName = entryName;
      this.priority = priority;
    }
  }

  private static final class IncludedFile {
    final String path;
    final long sizeAtOpen;
    final long bytesWritten;
    final long lastModified;
    final boolean complete;

    IncludedFile(
        String path,
        long sizeAtOpen,
        long bytesWritten,
        long lastModified,
        boolean complete) {
      this.path = path;
      this.sizeAtOpen = sizeAtOpen;
      this.bytesWritten = bytesWritten;
      this.lastModified = lastModified;
      this.complete = complete;
    }
  }

  private static final class Issue {
    final String path;
    final String stage;
    final String message;

    Issue(String path, String stage, String message) {
      this.path = safeSubstring(path, 1000);
      this.stage = safeSubstring(stage, 100);
      this.message = safeSubstring(message, 1000);
    }
  }

  private static final class RescueState {
    final String failure;
    final List<Candidate> candidates = new ArrayList<>();
    final List<IncludedFile> included = new ArrayList<>();
    final List<Issue> issues = new ArrayList<>();
    final List<String> excludedPaths = new ArrayList<>();
    long totalBytes;
    int skippedFiles;
    int excludedCount;
    int suppressedIssues;
    boolean scanLimitReached;

    RescueState(String failure) {
      this.failure = failure;
    }

    void addCandidate(Candidate candidate) {
      if (candidates.size() >= MAX_FILE_COUNT) {
        scanLimitReached = true;
        issue("files", "limit", "file count limit reached");
        return;
      }
      candidates.add(candidate);
    }

    void issue(String path, String stage, String message) {
      if (issues.size() < MAX_RECORDED_ISSUES) {
        issues.add(new Issue(path, stage, message));
      } else {
        suppressedIssues++;
      }
    }

    void skip(String path, String stage, String message) {
      skippedFiles++;
      issue(path, stage, message);
    }

    void exclude(String path) {
      excludedCount++;
      if (excludedPaths.size() < MAX_RECORDED_EXCLUSIONS) {
        excludedPaths.add(safeSubstring(path, 1000));
      }
    }
  }

  private static final class NonClosingOutputStream extends FilterOutputStream {
    NonClosingOutputStream(OutputStream output) {
      super(output);
    }

    @Override
    public void close() throws IOException {
      flush();
    }
  }
}
