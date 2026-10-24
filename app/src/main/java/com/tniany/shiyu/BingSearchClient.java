package com.tniany.shiyu;

import android.text.Html;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/** Keyless Bing RSS search used by character web-search tools. */
final class BingSearchClient {
  private BingSearchClient() {}

  static String search(String query, int limit) throws Exception {
    String clean = query == null ? "" : query.trim();
    if (clean.isEmpty()) throw new IllegalArgumentException("搜索关键词不能为空");
    int count = Math.max(1, Math.min(10, limit));
    String encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8.name());
    URL url = new URL("https://www.bing.com/search?format=rss&setlang=zh-Hans&q=" + encoded);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    try {
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(15_000);
      connection.setReadTimeout(30_000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.5");
      connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.6");
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36 ShiYu/" + BuildConfig.VERSION_NAME);
      int code = connection.getResponseCode();
      if (code < 200 || code >= 300) throw new IllegalStateException("必应搜索 HTTP " + code);
      try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(input, "UTF-8");
        StringBuilder output = new StringBuilder();
        String title = "", link = "", description = "", tag = "";
        boolean inItem = false;
        int found = 0;
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
          if (event == XmlPullParser.START_TAG) {
            tag = parser.getName();
            if ("item".equalsIgnoreCase(tag)) {
              inItem = true;title = "";link = "";description = "";
            }
          } else if (event == XmlPullParser.TEXT && inItem) {
            String text = parser.getText();
            if ("title".equalsIgnoreCase(tag)) title += text;
            else if ("link".equalsIgnoreCase(tag)) link += text;
            else if ("description".equalsIgnoreCase(tag)) description += text;
          } else if (event == XmlPullParser.END_TAG) {
            String end = parser.getName();
            if ("item".equalsIgnoreCase(end) && inItem) {
              inItem = false;
              title = plain(title);
              description = plain(description);
              link = link.trim();
              if (!title.isEmpty() || !description.isEmpty()) {
                found++;
                if (output.length() > 0) output.append('\n');
                output.append(found).append(". ").append(title.isEmpty() ? "必应搜索结果" : title);
                if (!description.isEmpty()) output.append("\n").append(clip(description, 900));
                if (!link.isEmpty()) output.append("\n来源：").append(link);
              }
              if (found >= count) break;
            }
            tag = "";
          }
        }
        if (output.length() == 0) throw new IllegalStateException("必应没有返回可识别的搜索结果");
        return output.toString();
      }
    } finally {
      connection.disconnect();
    }
  }

  private static String plain(String value) {
    if (value == null || value.isEmpty()) return "";
    return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace('\u00a0', ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ")
        .replaceAll("\\n{3,}", "\n\n").trim();
  }

  private static String clip(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }
}
