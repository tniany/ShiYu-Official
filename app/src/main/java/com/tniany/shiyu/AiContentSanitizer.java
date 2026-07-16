package com.tniany.shiyu;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import java.util.ArrayList;
import java.util.Locale;

final class AiContentSanitizer {
  private AiContentSanitizer() {}

  static String moment(String source) {
    String value = source == null ? "" : source.trim();
    value = value.replaceAll("(?is)^```(?:html|xml|markdown)?\\s*", "")
        .replaceAll("(?is)\\s*```$", "")
        .replaceAll("(?is)<(?:script|style)[^>]*>.*?</(?:script|style)>", "");
    if (value.indexOf('<') >= 0 && value.indexOf('>') > value.indexOf('<')) {
      Spanned parsed = Build.VERSION.SDK_INT >= 24
          ? Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
          : Html.fromHtml(value);
      value = parsed.toString();
    }
    value = value.replaceAll("(?m)^[ \\t]*(?:assistant|角色|动态)\\s*[：:]\\s*", "")
        .replaceAll("\\[(?:发布朋友圈|发送表情|拍一拍|设置拍一拍)[^\\]]*\\]", "")
        .replace('\u00a0', ' ')
        .trim();

    String[] blocks = value.split("(?:\\r?\\n){2,}");
    ArrayList<String> kept = new ArrayList<>();
    boolean hasCjk = false;
    for (String block : blocks) {
      String cleaned = block.replaceAll("[ \\t]+", " ").replaceAll(" *\\r?\\n *", "\n").trim();
      if (cleaned.isEmpty()) continue;
      String lower = cleaned.toLowerCase(Locale.ROOT);
      if (lower.matches("(?s).*(?:of course|let me know|under \\d+ words|no actions or narrations|system prompt|use spaces instead|ready to chat).*")) break;
      int letters = 0, latin = 0, cjk = 0;
      for (int offset = 0; offset < cleaned.length();) {
        int cp = cleaned.codePointAt(offset);
        if (Character.isLetter(cp)) {
          letters++;
          if (cp < 128) latin++;
          if ((cp >= 0x3400 && cp <= 0x4DBF) || (cp >= 0x4E00 && cp <= 0x9FFF)
              || (cp >= 0xF900 && cp <= 0xFAFF)) cjk++;
        }
        offset += Character.charCount(cp);
      }
      if (hasCjk && letters >= 12 && latin * 100 / Math.max(1, letters) >= 80) break;
      kept.add(cleaned);
      hasCjk |= cjk > 0;
    }
    String result = String.join("\n\n", kept).trim();
    return truncateCodePoints(result, 180);
  }

  static String comment(String source) {
    return truncateCodePoints(moment(source).replaceAll("(?:\\r?\\n)+", " ").trim(), 80);
  }

  private static String truncateCodePoints(String value, int limit) {
    int count = value.codePointCount(0, value.length());
    if (count <= limit) return value;
    return value.substring(0, value.offsetByCodePoints(0, limit)).trim();
  }
}
