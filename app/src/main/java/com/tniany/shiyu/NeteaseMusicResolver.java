package com.tniany.shiyu;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

final class NeteaseMusicResolver {
  private NeteaseMusicResolver() {}

  static JSONObject resolve(Context context, String requestedTitle, String requestedArtist)
      throws Exception {
    String title = requestedTitle == null ? "" : requestedTitle.trim();
    String artist = requestedArtist == null ? "" : requestedArtist.trim();
    if (title.isEmpty()) throw new IllegalArgumentException("歌曲名称不能为空");
    String query = title + (artist.isEmpty() ? "" : " " + artist);
    String cookie = new SecureStore(context).getNamed("netease_music_cookie");
    JSONObject root = request("https://music.163.com/api/search/get/web?csrf_token=&s="
        + URLEncoder.encode(query, "UTF-8") + "&type=1&offset=0&total=true&limit=12", cookie);
    JSONArray songs = root.optJSONObject("result") == null ? null
        : root.optJSONObject("result").optJSONArray("songs");
    if (songs == null || songs.length() == 0) throw new IllegalStateException("网易云没有找到这首歌");
    JSONObject best = null;int bestScore = Integer.MIN_VALUE;
    for (int i = 0; i < songs.length(); i++) {
      JSONObject value = songs.optJSONObject(i);if (value == null || value.optLong("id") <= 0) continue;
      String foundTitle = value.optString("name").trim();String foundArtist = artists(value);
      int score = 0;
      if (foundTitle.equalsIgnoreCase(title)) score += 12;
      else if (foundTitle.toLowerCase().contains(title.toLowerCase())) score += 7;
      if (!artist.isEmpty() && foundArtist.toLowerCase().contains(artist.toLowerCase())) score += 10;
      if (score > bestScore) { best = value;bestScore = score; }
    }
    if (best == null) throw new IllegalStateException("网易云没有找到可用歌曲");
    long id = best.optLong("id");JSONObject album = album(best);String cover = album.optString("picUrl").trim();
    if (cover.isEmpty()) {
      JSONArray ids = new JSONArray().put(new JSONObject().put("id", id));
      JSONObject detail = request("https://music.163.com/api/v3/song/detail?c="
          + URLEncoder.encode(ids.toString(), "UTF-8"), cookie);
      JSONObject detailed = detail.optJSONArray("songs") == null ? null
          : detail.optJSONArray("songs").optJSONObject(0);
      if (detailed != null) { best = detailed;album = album(best);cover = album.optString("picUrl").trim(); }
    }
    String foundTitle = best.optString("name", title).trim();String foundArtist = artists(best);
    return new JSONObject().put("title", foundTitle.isEmpty() ? title : foundTitle)
        .put("artist", foundArtist.isEmpty() ? (artist.isEmpty() ? "未知歌手" : artist) : foundArtist)
        .put("album", album.optString("name")).put("songId", id)
        .put("durationMs", best.optLong("duration", best.optLong("dt")))
        .put("coverUrl", cover).put("source", "netease")
        .put("url", "https://music.163.com/song/media/outer/url?id=" + id + ".mp3");
  }

  private static JSONObject album(JSONObject song) {
    JSONObject value = song.optJSONObject("album");
    if (value == null) value = song.optJSONObject("al");
    return value == null ? new JSONObject() : value;
  }

  private static String artists(JSONObject song) {
    JSONArray values = song.optJSONArray("artists");if (values == null) values = song.optJSONArray("ar");
    if (values == null) return "";StringBuilder result = new StringBuilder();
    for (int i = 0; i < values.length(); i++) {
      JSONObject value = values.optJSONObject(i);String name = value == null ? "" : value.optString("name").trim();
      if (name.isEmpty()) continue;if (result.length() > 0) result.append(" / ");result.append(name);
    }
    return result.toString();
  }

  private static JSONObject request(String url, String cookie) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    try {
      connection.setConnectTimeout(15000);connection.setReadTimeout(25000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) ShiYu/Android");
      connection.setRequestProperty("Referer", "https://music.163.com/");
      connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
      if (cookie != null && !cookie.trim().isEmpty()) connection.setRequestProperty("Cookie", cookie.trim());
      int code = connection.getResponseCode();
      if (code < 200 || code >= 300) throw new IllegalStateException("网易云请求 HTTP " + code);
      return new JSONObject(read(connection.getInputStream()));
    } finally { connection.disconnect(); }
  }

  private static String read(InputStream input) throws Exception {
    try (InputStream stream = input;ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];int count;
      while ((count = stream.read(buffer)) >= 0) output.write(buffer, 0, count);
      return output.toString(StandardCharsets.UTF_8.name());
    }
  }
}
