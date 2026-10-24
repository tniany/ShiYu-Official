package com.tniany.shiyu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RoleToolProtocol {
  private static final String[] COMMANDS = {
      "调用工具", "联网搜索", "查询天气", "打开地图", "打开应用", "读链接", "生成图片",
      "写日记", "分享日记", "发送定位", "转账", "分享音乐", "语音通话", "发送红包",
      "拍一拍", "设置拍一拍", "发布朋友圈", "拉黑用户", "解除拉黑用户", "NO_REPLY", "不回复"
  };
  private static final Pattern MCP = Pattern.compile(
      "^调用工具[：:]\\s*([^/|｜\\]\\r\\n]{1,80})/([^|｜\\]\\r\\n]{1,120})(?:[|｜]\\s*(\\{[\\s\\S]*\\}))?\\s*$");

  static final class Call {
    final String serverId, toolName, arguments;
    Call(String serverId, String toolName, String arguments) {
      this.serverId = serverId; this.toolName = toolName; this.arguments = arguments;
    }
  }

  static final class Parsed {
    final String text;
    final List<Call> calls;
    Parsed(String text, List<Call> calls) { this.text = text; this.calls = calls; }
  }

  private RoleToolProtocol() {}

  static Parsed extractMcp(String value) {
    String text = value == null ? "" : value;
    StringBuilder visible = new StringBuilder();
    ArrayList<Call> calls = new ArrayList<>();
    int copied = 0;
    for (int start = 0; start < text.length(); start++) {
      if (text.charAt(start) != '[' || !text.startsWith("调用工具", start + 1)) continue;
      int end = commandEnd(text, start);
      if (end < 0) break;
      Matcher match = MCP.matcher(text.substring(start + 1, end));
      if (!match.matches()) continue;
      visible.append(text, copied, start);
      if (calls.size() < 4) calls.add(new Call(match.group(1).trim(), match.group(2).trim(),
          match.group(3) == null ? "{}" : match.group(3).trim()));
      copied = end + 1;
      start = end;
    }
    visible.append(text, copied, text.length());
    return new Parsed(removeEmptyFences(visible.toString()).trim(), calls);
  }

  static String visibleText(String value, boolean streaming) {
    String text = value == null ? "" : value;
    if (!streaming && ("[NO_REPLY]".equalsIgnoreCase(text.trim()) || "[不回复]".equals(text.trim())))
      return text.trim();
    StringBuilder visible = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) != '[') { visible.append(text.charAt(i)); continue; }
      String tail = text.substring(i + 1);
      boolean command = false;
      for (String name : COMMANDS) {
        boolean namedCommand = tail.startsWith(name) && (tail.length() == name.length()
            || ":：]".indexOf(tail.charAt(name.length())) >= 0);
        if (namedCommand || (streaming && name.startsWith(tail))) {
          command = true; break;
        }
      }
      if (!command) { visible.append('['); continue; }
      int end = commandEnd(text, i);
      if (end < 0) break;
      i = end;
    }
    String result = removeEmptyFences(visible.toString());
    if (streaming) result = result.replaceFirst("(?s)\\s*```(?:json|text)?\\s*$", "");
    return (streaming ? result.replace("<SEG>", "\n\n").replace("<segment>", "\n\n") : result).trim();
  }

  // JSON arrays and brackets inside quoted strings must not terminate an MCP command.
  private static int commandEnd(String text, int start) {
    int objects = 0, arrays = 0;
    boolean quoted = false, escaped = false;
    for (int i = start + 1; i < text.length(); i++) {
      char c = text.charAt(i);
      if (quoted) {
        if (escaped) escaped = false;
        else if (c == '\\') escaped = true;
        else if (c == '"') quoted = false;
        continue;
      }
      if (c == '"' && objects > 0) quoted = true;
      else if (c == '{') objects++;
      else if (c == '}' && objects > 0) objects--;
      else if (c == '[' && objects > 0) arrays++;
      else if (c == ']' && arrays > 0) arrays--;
      else if (c == ']' && objects == 0) return i;
    }
    return -1;
  }

  private static String removeEmptyFences(String value) {
    return value.replaceAll("(?s)```(?:json|text)?\\s*```", "");
  }
}
