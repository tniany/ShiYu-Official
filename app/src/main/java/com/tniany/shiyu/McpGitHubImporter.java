package com.tniany.shiyu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Discovers remote MCP declarations without downloading or executing repository code. */
final class McpGitHubImporter {
  static final class Candidate {
    final String name;
    final String endpoint;
    final boolean needsToken;

    Candidate(String name, String endpoint, boolean needsToken) {
      this.name = name;
      this.endpoint = endpoint;
      this.needsToken = needsToken;
    }
  }

  private static final int MAX_REDIRECTS = 3;
  private static final int MAX_CANDIDATES = 20;
  private static final int MAX_CONFIG_BYTES = 256 * 1024;
  private static final int MAX_API_BYTES = 384 * 1024;
  private static final int MAX_TOTAL_BYTES = 1024 * 1024;
  private static final Set<String> ALLOWED_HOSTS =
      new HashSet<>(
          Arrays.asList(
              "github.com",
              "www.github.com",
              "api.github.com",
              "raw.githubusercontent.com"));
  private static final Set<String> ROOT_CONFIGS =
      new HashSet<>(
          Arrays.asList(
              ".mcp.json",
              "mcp.json",
              "server.json",
              "mcp-server.json",
              "claude_desktop_config.json"));
  private static final String[] NESTED_CONFIGS = {
    ".vscode/mcp.json", ".cursor/mcp.json", ".claude/mcp.json"
  };

  private McpGitHubImporter() {}

  static ArrayList<Candidate> discover(String rawUrl) throws Exception {
    Source source = parseSource(rawUrl);
    Budget budget = new Budget(MAX_TOTAL_BYTES);
    CandidateCollector candidates = new CandidateCollector();
    if (source.directUrl != null) {
      try {
        parseConfig(fetchText(source.directUrl, MAX_CONFIG_BYTES, budget), source.repo, candidates);
      } catch (HttpStatusException error) {
        throw error;
      } catch (IOException error) {
        throw error;
      } catch (Exception error) {
        throw new IllegalArgumentException("配置文件不是有效的 MCP JSON 配置");
      }
      if (candidates.isEmpty()) {
        throw new IllegalArgumentException(
            "配置中没有可导入的 HTTPS Streamable HTTP 服务；本地 stdio 和 SSE 服务不受支持");
      }
      return candidates.values();
    }

    discoverRepository(source, budget, candidates);
    return candidates.values();
  }

  private static void discoverRepository(
      Source source, Budget budget, CandidateCollector candidates) throws Exception {
    String apiBase = "https://api.github.com/repos/" + source.owner + "/" + source.repo;
    JSONObject metadata = parseObject(fetchText(apiBase, 64 * 1024, budget));
    String defaultBranch = metadata.optString("default_branch").trim();
    if (defaultBranch.isEmpty()) throw new IOException("GitHub 项目没有可读取的默认分支");
    String ref = URLEncoder.encode(defaultBranch, "UTF-8");
    JSONArray root =
        parseArray(fetchText(apiBase + "/contents?ref=" + ref, MAX_API_BYTES, budget));
    LinkedHashMap<String, String> files = new LinkedHashMap<>();
    Set<String> directories = new HashSet<>();
    for (int i = 0; i < root.length(); i++) {
      JSONObject entry = root.optJSONObject(i);
      if (entry == null) continue;
      String name = entry.optString("name");
      String type = entry.optString("type");
      if ("file".equals(type) && ROOT_CONFIGS.contains(name)) {
        addDownload(files, name, entry.optString("download_url"));
      } else if ("dir".equals(type)) {
        directories.add(name);
      }
    }

    for (String path : NESTED_CONFIGS) {
      String directory = path.substring(0, path.indexOf('/'));
      if (!directories.contains(directory)) continue;
      try {
        JSONObject entry =
            parseObject(
                fetchText(apiBase + "/contents/" + path + "?ref=" + ref, 64 * 1024, budget));
        if ("file".equals(entry.optString("type"))) {
          addDownload(files, path, entry.optString("download_url"));
        }
      } catch (HttpStatusException error) {
        if (error.code != HttpURLConnection.HTTP_NOT_FOUND) throw error;
      }
    }

    if (files.isEmpty()) {
      throw new IllegalArgumentException(
          "未在项目中找到支持的 MCP JSON 配置（.mcp.json、mcp.json、server.json 或 .vscode/mcp.json）");
    }

    int validJsonFiles = 0;
    int readableFiles = 0;
    Exception firstReadError = null;
    for (Map.Entry<String, String> file : files.entrySet()) {
      if (candidates.size() >= MAX_CANDIDATES) break;
      String text;
      try {
        text = fetchText(file.getValue(), MAX_CONFIG_BYTES, budget);
        readableFiles++;
      } catch (InterruptedIOException error) {
        throw error;
      } catch (HttpStatusException error) {
        if (error.code == HttpURLConnection.HTTP_FORBIDDEN || error.code == 429) {
          if (candidates.isEmpty()) throw error;
          break;
        }
        if (firstReadError == null) firstReadError = error;
        continue;
      } catch (Exception error) {
        if (firstReadError == null) firstReadError = error;
        continue;
      }
      try {
        parseConfig(text, source.repo, candidates);
        validJsonFiles++;
      } catch (Exception ignored) {
        // A repository can contain unrelated server.json files; other known files remain usable.
      }
    }
    if (!candidates.isEmpty()) return;
    if (readableFiles == 0 && firstReadError != null) throw firstReadError;
    if (validJsonFiles == 0) throw new IllegalArgumentException("找到候选配置文件，但内容不是有效的 MCP JSON 配置");
    throw new IllegalArgumentException(
        "项目中没有可导入的 HTTPS Streamable HTTP 服务；本地 stdio 和 SSE 服务不受支持");
  }

  private static void parseConfig(
      String raw, String fallbackName, CandidateCollector candidates) throws Exception {
    String clean = raw.startsWith("\uFEFF") ? raw.substring(1) : raw;
    JSONObject root = parseObject(clean);
    parseServerMap(root.optJSONObject("mcpServers"), fallbackName, candidates);
    parseServerMap(root.optJSONObject("servers"), fallbackName, candidates);
    JSONArray remotes = root.optJSONArray("remotes");
    if (remotes != null) {
      String rootName = firstNonEmpty(root.optString("title"), root.optString("name"), fallbackName);
      for (int i = 0; i < remotes.length() && candidates.size() < MAX_CANDIDATES; i++) {
        JSONObject remote = remotes.optJSONObject(i);
        if (remote != null) addServer(remote, rootName, candidates);
      }
    }
  }

  private static void parseServerMap(
      JSONObject servers, String fallbackName, CandidateCollector candidates) {
    if (servers == null) return;
    Iterator<String> names = servers.keys();
    while (names.hasNext() && candidates.size() < MAX_CANDIDATES) {
      String key = names.next();
      JSONObject server = servers.optJSONObject(key);
      if (server == null) continue;
      addServer(server, firstNonEmpty(server.optString("name"), key, fallbackName), candidates);
    }
  }

  private static void addServer(
      JSONObject server, String fallbackName, CandidateCollector candidates) {
    if (!server.optString("command").trim().isEmpty()) return;
    JSONObject transportObject = server.optJSONObject("transport");
    String transport =
        firstNonEmpty(
            stringValue(server.opt("type")),
            transportObject == null ? "" : stringValue(transportObject.opt("type")),
            transportObject == null ? stringValue(server.opt("transport")) : "");
    String normalizedTransport =
        transport.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
    if (!normalizedTransport.isEmpty()
        && !"http".equals(normalizedTransport)
        && !"streamablehttp".equals(normalizedTransport)) return;

    String rawEndpoint =
        firstNonEmpty(
            server.optString("url"),
            server.optString("endpoint"),
            transportObject == null ? "" : transportObject.optString("url"),
            transportObject == null ? "" : transportObject.optString("endpoint"));
    String endpoint = safeEndpoint(rawEndpoint);
    if (endpoint == null) return;
    URI endpointUri;
    try {
      endpointUri = new URI(endpoint);
    } catch (Exception ignored) {
      return;
    }
    String path = endpointUri.getPath();
    if (normalizedTransport.isEmpty() && path != null && path.toLowerCase(Locale.ROOT).endsWith("/sse")) {
      return;
    }

    HeaderInfo headers = inspectHeaders(server.opt("headers"));
    if (transportObject != null) headers = headers.merge(inspectHeaders(transportObject.opt("headers")));
    if (!headers.supported) return;
    boolean needsToken =
        headers.needsToken
            || server.has("token")
            || server.has("bearerToken")
            || server.has("accessToken");
    candidates.add(cleanName(fallbackName), endpoint, needsToken);
  }

  private static HeaderInfo inspectHeaders(Object rawHeaders) {
    if (rawHeaders == null || rawHeaders == JSONObject.NULL) return HeaderInfo.SUPPORTED;
    boolean needsToken = false;
    if (rawHeaders instanceof JSONObject) {
      JSONObject headers = (JSONObject) rawHeaders;
      Iterator<String> names = headers.keys();
      while (names.hasNext()) {
        String name = names.next();
        if (isBuiltInHeader(name)) continue;
        if (!"authorization".equals(name.trim().toLowerCase(Locale.ROOT))) {
          return HeaderInfo.UNSUPPORTED;
        }
        String value = stringValue(headers.opt(name)).trim().toLowerCase(Locale.ROOT);
        if (!value.isEmpty()
            && !value.startsWith("bearer")
            && !containsPlaceholder(value)) return HeaderInfo.UNSUPPORTED;
        needsToken = true;
      }
      return new HeaderInfo(true, needsToken);
    }
    if (rawHeaders instanceof JSONArray) {
      JSONArray headers = (JSONArray) rawHeaders;
      for (int i = 0; i < headers.length(); i++) {
        JSONObject header = headers.optJSONObject(i);
        if (header == null) return HeaderInfo.UNSUPPORTED;
        String name = header.optString("name").trim();
        if (isBuiltInHeader(name)) continue;
        if (!"authorization".equals(name.toLowerCase(Locale.ROOT))) return HeaderInfo.UNSUPPORTED;
        needsToken = true;
      }
      return new HeaderInfo(true, needsToken);
    }
    return HeaderInfo.UNSUPPORTED;
  }

  private static boolean isBuiltInHeader(String rawName) {
    String name = rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);
    return "accept".equals(name)
        || "content-type".equals(name)
        || "mcp-protocol-version".equals(name);
  }

  private static String safeEndpoint(String raw) {
    if (raw == null) return null;
    String value = raw.trim();
    if (value.isEmpty() || value.length() > 500 || containsPlaceholder(value)) return null;
    try {
      URI uri = new URI(value);
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getFragment() != null
          || uri.getPort() == 0
          || uri.getPort() > 65535
          || hasSecretQuery(uri.getRawQuery())) return null;
      String ascii = uri.toASCIIString();
      int separator = ascii.indexOf(':');
      if (separator < 0) return null;
      String result = "https" + ascii.substring(separator);
      return result.length() <= 500 ? result : null;
    } catch (Exception ignored) {
      return null;
    }
  }

  private static boolean hasSecretQuery(String rawQuery) {
    if (rawQuery == null || rawQuery.isEmpty()) return false;
    for (String part : rawQuery.split("&")) {
      String rawName = part.substring(0, part.indexOf('=') < 0 ? part.length() : part.indexOf('='));
      try {
        String name =
            URLDecoder.decode(rawName.replace("+", "%2B"), "UTF-8")
                .toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");
        if (name.contains("token")
            || name.contains("secret")
            || name.contains("password")
            || name.contains("passwd")
            || name.contains("credential")
            || name.contains("authorization")
            || "auth".equals(name)
            || name.endsWith("auth")
            || name.contains("signature")
            || "sig".equals(name)
            || "key".equals(name)
            || name.contains("apikey")
            || name.contains("accesskey")
            || name.contains("privatekey")) return true;
      } catch (Exception ignored) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsPlaceholder(String value) {
    String lower = value.toLowerCase(Locale.ROOT);
    return value.indexOf('{') >= 0
        || value.indexOf('}') >= 0
        || lower.contains("%7b")
        || lower.contains("%7d")
        || lower.contains("%3c")
        || lower.contains("%3e")
        || lower.contains("${")
        || lower.contains("{{")
        || value.indexOf('<') >= 0
        || value.indexOf('>') >= 0
        || value.matches("(?s).*\\$[A-Za-z_][A-Za-z0-9_]*.*");
  }

  private static String cleanName(String raw) {
    String value = raw == null ? "" : raw.replaceAll("[\\p{Cntrl}]", " ").trim();
    value = value.replaceAll("\\s+", " ");
    if (value.isEmpty()) value = "MCP";
    return value.length() <= 80 ? value : value.substring(0, 80);
  }

  private static Source parseSource(String rawUrl) throws Exception {
    if (rawUrl == null || rawUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("请填写 GitHub 项目或配置文件链接");
    }
    String value = rawUrl.trim();
    if (value.startsWith("github.com/") || value.startsWith("www.github.com/")) {
      value = "https://" + value;
    }
    URI uri;
    try {
      uri = new URI(value);
    } catch (Exception error) {
      throw new IllegalArgumentException("GitHub 链接格式不正确");
    }
    String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || !ALLOWED_HOSTS.contains(host)
        || uri.getUserInfo() != null
        || (uri.getPort() != -1 && uri.getPort() != 443)) {
      throw new IllegalArgumentException("仅支持 github.com 或 raw.githubusercontent.com 的 HTTPS 链接");
    }
    ArrayList<String> rawParts = rawPathParts(uri.getRawPath());
    ArrayList<String> parts = decodedPathParts(rawParts);
    if (parts.size() < 2) throw new IllegalArgumentException("GitHub 链接缺少项目名称");
    String owner = checkedRepoPart(parts.get(0), "GitHub 用户名");
    String repo = checkedRepoPart(parts.get(1), "GitHub 项目名");

    if ("raw.githubusercontent.com".equals(host)) {
      if (parts.size() < 4) throw new IllegalArgumentException("GitHub Raw 链接缺少分支或配置文件路径");
      validateFileTail(parts, 2);
      return new Source(owner, repo, "https://raw.githubusercontent.com" + uri.getRawPath());
    }

    if (parts.size() == 2) {
      if (repo.endsWith(".git")) repo = repo.substring(0, repo.length() - 4);
      repo = checkedRepoPart(repo, "GitHub 项目名");
      return new Source(owner, repo, null);
    }
    String kind = parts.get(2).toLowerCase(Locale.ROOT);
    if (("blob".equals(kind) || "raw".equals(kind)) && parts.size() >= 5) {
      validateFileTail(parts, 3);
      StringBuilder tail = new StringBuilder();
      for (int i = 3; i < rawParts.size(); i++) {
        if (tail.length() > 0) tail.append('/');
        tail.append(rawParts.get(i));
      }
      return new Source(owner, repo, "https://github.com/" + owner + "/" + repo + "/raw/" + tail);
    }
    throw new IllegalArgumentException("仅支持 GitHub 项目首页、blob 文件链接或 Raw 文件链接");
  }

  private static ArrayList<String> rawPathParts(String rawPath) {
    ArrayList<String> result = new ArrayList<>();
    if (rawPath == null) return result;
    for (String part : rawPath.split("/")) if (!part.isEmpty()) result.add(part);
    return result;
  }

  private static ArrayList<String> decodedPathParts(ArrayList<String> rawParts) throws Exception {
    ArrayList<String> result = new ArrayList<>();
    for (String raw : rawParts) {
      String lower = raw.toLowerCase(Locale.ROOT);
      if (lower.contains("%2f") || lower.contains("%5c")) {
        throw new IllegalArgumentException("GitHub 链接路径不安全");
      }
      String part = URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8");
      if (part.isEmpty() || ".".equals(part) || "..".equals(part) || part.indexOf('\\') >= 0) {
        throw new IllegalArgumentException("GitHub 链接路径不安全");
      }
      result.add(part);
    }
    return result;
  }

  private static void validateFileTail(ArrayList<String> parts, int start) {
    if (parts.size() - start < 2) throw new IllegalArgumentException("GitHub 链接缺少分支或配置文件路径");
    int length = 0;
    for (int i = start; i < parts.size(); i++) length += parts.get(i).length() + 1;
    if (length > 800) throw new IllegalArgumentException("GitHub 配置文件路径过长");
  }

  private static String checkedRepoPart(String value, String label) {
    if (value == null
        || !value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,99}")
        || ".".equals(value)
        || "..".equals(value)) throw new IllegalArgumentException(label + "格式不正确");
    return value;
  }

  private static void addDownload(Map<String, String> files, String path, String downloadUrl) {
    if (downloadUrl == null || downloadUrl.trim().isEmpty() || files.size() >= 12) return;
    files.put(path, downloadUrl.trim());
  }

  private static String fetchText(String initialUrl, int perResponseLimit, Budget budget)
      throws Exception {
    URL current = checkedFetchUrl(initialUrl);
    for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("GitHub 导入已取消");
      HttpURLConnection connection = (HttpURLConnection) current.openConnection();
      try {
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("User-Agent", "ShiYu-Android/" + BuildConfig.VERSION_NAME);
        connection.setRequestProperty("Accept", "application/vnd.github+json, application/json, text/plain");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        int code = connection.getResponseCode();
        if (code == HttpURLConnection.HTTP_MOVED_PERM
            || code == HttpURLConnection.HTTP_MOVED_TEMP
            || code == HttpURLConnection.HTTP_SEE_OTHER
            || code == 307
            || code == 308) {
          if (redirects == MAX_REDIRECTS) throw new IOException("GitHub 重定向次数过多");
          String location = connection.getHeaderField("Location");
          if (location == null || location.trim().isEmpty()) throw new IOException("GitHub 返回了无效重定向");
          current = checkedFetchUrl(new URL(current, location).toString());
          continue;
        }
        if (code == HttpURLConnection.HTTP_FORBIDDEN || code == 429) {
          throw new HttpStatusException(code, "GitHub 请求次数受限，请稍后重试");
        }
        if (code == HttpURLConnection.HTTP_NOT_FOUND || code == HttpURLConnection.HTTP_UNAUTHORIZED) {
          throw new HttpStatusException(code, "GitHub 项目或配置文件不存在，私有仓库暂不支持");
        }
        if (code < 200 || code >= 300) {
          throw new HttpStatusException(code, "GitHub 请求失败（HTTP " + code + "）");
        }
        int allowed = Math.min(perResponseLimit, budget.remaining);
        if (allowed <= 0) throw new IOException("GitHub 配置总大小超过 1 MB 安全限制");
        long declared = connection.getContentLengthLong();
        if (declared > allowed) throw new IOException("GitHub 返回内容超过安全大小限制");
        byte[] data = readLimited(connection.getInputStream(), allowed);
        budget.consume(data.length);
        return new String(data, StandardCharsets.UTF_8);
      } finally {
        InputStream error = connection.getErrorStream();
        if (error != null) try { error.close(); } catch (Exception ignored) {}
        connection.disconnect();
      }
    }
    throw new IOException("GitHub 重定向次数过多");
  }

  private static URL checkedFetchUrl(String raw) throws Exception {
    URI uri = new URI(raw);
    String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || !ALLOWED_HOSTS.contains(host)
        || uri.getUserInfo() != null
        || (uri.getPort() != -1 && uri.getPort() != 443)) {
      throw new IOException("GitHub 返回了不受信任的下载地址");
    }
    return uri.toURL();
  }

  private static byte[] readLimited(InputStream input, int limit) throws Exception {
    try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int total = 0;
      while (true) {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("GitHub 导入已取消");
        int count = source.read(buffer);
        if (count < 0) break;
        if (count == 0) continue;
        total += count;
        if (total > limit) throw new IOException("GitHub 返回内容超过安全大小限制");
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    }
  }

  private static JSONObject parseObject(String raw) throws Exception {
    String clean = raw == null ? "" : raw.trim();
    if (clean.startsWith("\uFEFF")) clean = clean.substring(1).trim();
    if (!clean.startsWith("{")) throw new IllegalArgumentException("配置文件不是有效的 JSON 对象");
    try {
      return new JSONObject(clean);
    } catch (Exception error) {
      throw new IllegalArgumentException("JSON 数据格式不正确");
    }
  }

  private static JSONArray parseArray(String raw) throws Exception {
    String clean = raw == null ? "" : raw.trim();
    if (!clean.startsWith("[")) throw new IOException("GitHub 项目目录响应格式不正确");
    try {
      return new JSONArray(clean);
    } catch (Exception error) {
      throw new IOException("GitHub 项目目录响应格式不正确");
    }
  }

  private static String stringValue(Object value) {
    return value instanceof String ? ((String) value).trim() : "";
  }

  private static String firstNonEmpty(String... values) {
    for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
    return "";
  }

  private static final class Source {
    final String owner;
    final String repo;
    final String directUrl;

    Source(String owner, String repo, String directUrl) {
      this.owner = owner;
      this.repo = repo;
      this.directUrl = directUrl;
    }
  }

  private static final class Budget {
    int remaining;

    Budget(int remaining) {
      this.remaining = remaining;
    }

    void consume(int count) {
      remaining -= count;
    }
  }

  private static final class CandidateCollector {
    private final LinkedHashMap<String, Candidate> values = new LinkedHashMap<>();

    void add(String name, String endpoint, boolean needsToken) {
      if (values.size() >= MAX_CANDIDATES && !values.containsKey(endpoint)) return;
      Candidate previous = values.get(endpoint);
      if (previous == null) values.put(endpoint, new Candidate(name, endpoint, needsToken));
      else if (needsToken && !previous.needsToken) {
        values.put(endpoint, new Candidate(previous.name, endpoint, true));
      }
    }

    int size() {
      return values.size();
    }

    boolean isEmpty() {
      return values.isEmpty();
    }

    ArrayList<Candidate> values() {
      return new ArrayList<>(values.values());
    }
  }

  private static final class HeaderInfo {
    static final HeaderInfo SUPPORTED = new HeaderInfo(true, false);
    static final HeaderInfo UNSUPPORTED = new HeaderInfo(false, false);
    final boolean supported;
    final boolean needsToken;

    HeaderInfo(boolean supported, boolean needsToken) {
      this.supported = supported;
      this.needsToken = needsToken;
    }

    HeaderInfo merge(HeaderInfo other) {
      return new HeaderInfo(supported && other.supported, needsToken || other.needsToken);
    }
  }

  private static final class HttpStatusException extends IOException {
    final int code;

    HttpStatusException(int code, String message) {
      super(message);
      this.code = code;
    }
  }
}
