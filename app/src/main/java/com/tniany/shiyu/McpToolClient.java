package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Minimal MCP Streamable HTTP client used by character tools. */
final class McpToolClient {
  static final String SERVERS_KEY = "mcp_servers";
  private static final String PROTOCOL_VERSION = "2025-03-26";
  private static volatile String cachedSignature = "";
  private static volatile String cachedPrompt = "";
  private static volatile long cachedAt;

  static final class Server {
    final String id, name, endpoint;
    final boolean enabled;
    Server(String id, String name, String endpoint, boolean enabled) {
      this.id = id;this.name = name;this.endpoint = endpoint;this.enabled = enabled;
    }
  }

  static final class Call {
    final String serverId, toolName, arguments;
    Call(String serverId, String toolName, String arguments) {
      this.serverId = serverId;this.toolName = toolName;this.arguments = arguments;
    }
  }

  private static final class Response {
    final JSONObject body;
    final String sessionId;
    Response(JSONObject body, String sessionId) { this.body = body;this.sessionId = sessionId; }
  }

  private McpToolClient() {}

  static ArrayList<Server> servers(Context context) {
    ArrayList<Server> result = new ArrayList<>();
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    try {
      JSONArray values = new JSONArray(prefs.getString(SERVERS_KEY, "[]"));
      for (int i = 0; i < values.length(); i++) {
        JSONObject value = values.optJSONObject(i);
        if (value == null) continue;
        String id = value.optString("id"), endpoint = normalizeEndpoint(value.optString("endpoint"));
        if (!id.isEmpty() && !endpoint.isEmpty()) result.add(new Server(id,
            value.optString("name", "MCP"), endpoint, value.optBoolean("enabled", true)));
      }
    } catch (Exception ignored) {}
    return result;
  }

  static void saveServers(Context context, ArrayList<Server> servers) {
    JSONArray values = new JSONArray();
    for (Server server : servers) {
      try { values.put(new JSONObject().put("id", server.id).put("name", server.name)
          .put("endpoint", normalizeEndpoint(server.endpoint)).put("enabled", server.enabled)); }
      catch (Exception ignored) {}
    }
    context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE).edit()
        .putString(SERVERS_KEY, values.toString()).apply();
    cachedSignature = "";cachedPrompt = "";cachedAt = 0L;
  }

  static void deleteServer(Context context, String id) {
    ArrayList<Server> values = servers(context);
    for (int i = values.size() - 1; i >= 0; i--)
      if (values.get(i).id.equals(id)) values.remove(i);
    saveServers(context, values);
    try { new SecureStore(context).putNamed(tokenKey(id), ""); } catch (Exception ignored) {}
  }

  static String promptContext(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    if (!prefs.getBoolean("character_tools_enabled", true)) return "";
    ArrayList<Server> enabled = new ArrayList<>();
    for (Server server : servers(context)) if (server.enabled) enabled.add(server);
    String builtin = BuiltinAgentTools.prompt(context);
    if (enabled.isEmpty()) return builtin;
    String signature = prefs.getString(SERVERS_KEY, "");
    long now = System.currentTimeMillis();
    if (signature.equals(cachedSignature) && now - cachedAt < 5 * 60_000L) return builtin + "\n" + cachedPrompt;
    StringBuilder result = new StringBuilder();
    SecureStore secure = new SecureStore(context);
    for (Server server : enabled) {
      try {
        JSONArray tools = listTools(server, secure.getNamed(tokenKey(server.id)));
        for (int i = 0; i < Math.min(50, tools.length()); i++) {
          JSONObject tool = tools.optJSONObject(i);
          if (tool == null || tool.optString("name").isEmpty()) continue;
          if (result.length() > 0) result.append('\n');
          result.append("- ").append(server.id).append('/').append(tool.optString("name"));
          String description = tool.optString("description").replace('\n', ' ').trim();
          if (!description.isEmpty()) result.append("：").append(description);
          JSONObject schema = tool.optJSONObject("inputSchema");
          if (schema != null) result.append("；参数 schema=").append(schema);
          if (result.length() > 12_000) break;
        }
      } catch (Exception error) {
        DiagnosticLog.record(context, "mcp", "error",
            "tools/list " + server.id + " | " + MainActivity.diagnosticSummary(error));
        if (result.length() > 0) result.append('\n');
        result.append("- ").append(server.id).append("：当前无法读取工具列表");
      }
    }
    cachedSignature = signature;cachedPrompt = result.toString();cachedAt = now;
    return builtin + "\n" + cachedPrompt;
  }

  static String execute(Context context, Call call) {
    if (!context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE)
        .getBoolean("character_tools_enabled", true)) return "角色工具已关闭";
    if (BuiltinAgentTools.SERVER.equals(call.serverId)) {
      try { return BuiltinAgentTools.execute(context, call); }
      catch (Exception error) { return "内置工具失败：" + error.getMessage(); }
    }
    Server target = null;
    for (Server server : servers(context)) if (server.enabled && server.id.equals(call.serverId)) {
      target = server;break;
    }
    if (target == null) return "MCP server is disabled or missing: " + call.serverId;
    try {
      JSONObject arguments = call.arguments == null || call.arguments.trim().isEmpty()
          ? new JSONObject() : new JSONObject(call.arguments);
      Response session = initialize(target, new SecureStore(context).getNamed(tokenKey(target.id)));
      JSONObject request = new JSONObject().put("jsonrpc", "2.0").put("id", UUID.randomUUID().toString())
          .put("method", "tools/call").put("params", new JSONObject().put("name", call.toolName)
              .put("arguments", arguments));
      Response response = post(target.endpoint, request,
          new SecureStore(context).getNamed(tokenKey(target.id)), session.sessionId, true);
      JSONObject error = response.body.optJSONObject("error");
      String value = error == null ? String.valueOf(response.body.opt("result"))
          : "MCP error " + error.optInt("code") + ": " + error.optString("message");
      if (error != null)
        DiagnosticLog.record(context, "mcp", "error",
            "tools/call " + target.id + " | JSON-RPC " + error.optInt("code"));
      return value.length() > 8_000 ? value.substring(0, 8_000) : value;
    } catch (Exception error) {
      DiagnosticLog.record(context, "mcp", "error",
          "tools/call " + target.id + " | " + MainActivity.diagnosticSummary(error));
      String message = error.getMessage();
      return "MCP call failed: " + (message == null || message.isEmpty()
          ? error.getClass().getSimpleName() : message);
    }
  }

  static JSONArray test(Context context, Server server, String token) throws Exception {
    validateEndpoint(server.endpoint);
    return listTools(server, token);
  }

  static String validatedEndpoint(String value) throws Exception {
    String endpoint = normalizeEndpoint(value);
    URI uri = new URI(endpoint);
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
        || uri.getUserInfo() != null || uri.getFragment() != null)
      throw new IllegalArgumentException("MCP endpoint must be a valid HTTPS URL");
    return endpoint;
  }

  static String tokenKey(String id) { return "mcp_token_" + id; }

  private static JSONArray listTools(Server server, String token) throws Exception {
    validateEndpoint(server.endpoint);
    Response session = initialize(server, token);
    JSONObject request = new JSONObject().put("jsonrpc", "2.0").put("id", UUID.randomUUID().toString())
        .put("method", "tools/list").put("params", new JSONObject());
    Response response = post(server.endpoint, request, token, session.sessionId, true);
    JSONObject error = response.body.optJSONObject("error");
    if (error != null) throw new IllegalStateException(error.optString("message", "tools/list failed"));
    JSONObject result = response.body.optJSONObject("result");
    JSONArray tools = result == null ? null : result.optJSONArray("tools");
    return tools == null ? new JSONArray() : tools;
  }

  private static Response initialize(Server server, String token) throws Exception {
    JSONObject request = new JSONObject().put("jsonrpc", "2.0").put("id", UUID.randomUUID().toString())
        .put("method", "initialize").put("params", new JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("capabilities", new JSONObject())
            .put("clientInfo", new JSONObject().put("name", "ShiYu Android")
                .put("version", BuildConfig.VERSION_NAME)));
    Response initialized = post(server.endpoint, request, token, "", true);
    JSONObject error = initialized.body.optJSONObject("error");
    if (error != null) throw new IllegalStateException(error.optString("message", "initialize failed"));
    try {
      post(server.endpoint, new JSONObject().put("jsonrpc", "2.0")
          .put("method", "notifications/initialized"), token, initialized.sessionId, false);
    } catch (Exception ignored) {}
    return initialized;
  }

  private static Response post(String endpoint, JSONObject body, String token, String sessionId,
      boolean responseRequired) throws Exception {
    URI address = new URI(endpoint);
    boolean didi = "mcp.didichuxing.com".equalsIgnoreCase(address.getHost())
        && ("/mcp-servers".equals(address.getPath()) || "/mcp-servers-sandbox".equals(address.getPath()));
    if (didi && address.getQuery() == null) {
      if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("请先填写滴滴 MCP Key");
      endpoint += "?key=" + java.net.URLEncoder.encode(token.trim(), "UTF-8");
    }
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    try {
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod("POST");connection.setConnectTimeout(20_000);connection.setReadTimeout(60_000);connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      connection.setRequestProperty("Accept", "application/json, text/event-stream");
      connection.setRequestProperty("MCP-Protocol-Version", PROTOCOL_VERSION);
      if (!didi && token != null && !token.trim().isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token.trim());
      if (sessionId != null && !sessionId.isEmpty()) connection.setRequestProperty("Mcp-Session-Id", sessionId);
      try (OutputStream output = connection.getOutputStream()) {
        output.write(body.toString().getBytes(StandardCharsets.UTF_8));
      }
      int code = connection.getResponseCode();
      InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
      if (code < 200 || code >= 300) {
        if (stream != null) stream.close();
        throw new IllegalStateException("MCP HTTP " + code);
      }
      JSONObject value = new JSONObject();
      if (responseRequired && connection.getContentType() != null
          && connection.getContentType().contains("text/event-stream")) {
        try (SseEventReader events = new SseEventReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
          String event;
          while ((event = events.readEvent(() -> true)) != null) {
            JSONObject candidate = new JSONObject(event);
            if (candidate.has("result") || candidate.has("error")) { value = candidate; break; }
          }
        }
        if (!value.has("result") && !value.has("error")) throw new IllegalStateException("MCP 未返回调用结果");
      } else if (responseRequired) value = parseResponse(read(stream));
      else if (stream != null) stream.close();
      return new Response(value, connection.getHeaderField("Mcp-Session-Id") == null
          ? sessionId : connection.getHeaderField("Mcp-Session-Id"));
    } finally { connection.disconnect(); }
  }

  private static JSONObject parseResponse(String raw) throws Exception {
    String value = raw == null ? "" : raw.trim();
    if (value.startsWith("{")) return new JSONObject(value);
    for (String line : value.split("[\\r\\n]+")) {
      if (!line.startsWith("data:")) continue;
      String data = line.substring(5).trim();
      if (data.startsWith("{")) return new JSONObject(data);
    }
    throw new IllegalStateException("MCP server returned no JSON-RPC response");
  }

  private static String read(InputStream input) throws Exception {
    if (input == null) return "";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder out = new StringBuilder();String line;
      while ((line = reader.readLine()) != null) {
        out.append(line).append('\n');
        if (out.length() > 1_048_576) throw new IllegalStateException("MCP 响应过大");
      }
      return out.toString();
    }
  }

  private static String normalizeEndpoint(String value) {
    return value == null ? "" : value.trim();
  }

  private static void validateEndpoint(String endpoint) throws Exception {
    validatedEndpoint(endpoint);
  }
}
