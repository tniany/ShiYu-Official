package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local tool bridge for the Moji Weather API sold through Aliyun API Market. */
final class MojiWeatherClient {
  static final String DEFAULT_CURRENT_ENDPOINT =
      "https://aliv18.data.moji.com/whapi/json/alicityweather/condition";
  static final String DEFAULT_FORECAST_ENDPOINT =
      "https://aliv18.data.moji.com/whapi/json/alicityweather/forecast15days";
  static final String APP_CODE_KEY = "moji_weather_app_code";

  private MojiWeatherClient() {}

  static boolean enabled(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    return prefs.getBoolean("moji_weather_enabled", false)
        && !prefs.getString("moji_weather_city_id", "").trim().isEmpty();
  }

  static String promptContext(Context context) {
    if (!enabled(context)) return "";
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    String cityName = prefs.getString("moji_weather_city_name", "").trim();
    String cityId = prefs.getString("moji_weather_city_id", "").trim();
    String location = cityName.isEmpty() ? "已配置城市（cityId " + cityId + "）" : cityName;
    return "\n- 墨迹天气：需要知道 " + location
        + " 的实时天气时输出 [查询天气]；询问未来天气或预报时输出 [查询天气：预报]。"
        + "应用会查询后把结果交还给你。没有实际查询结果时不得臆造天气。";
  }

  static String queryConfigured(Context context, boolean forecast) throws Exception {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
    if (!prefs.getBoolean("moji_weather_enabled", false))
      throw new IOException("墨迹天气未开启");
    String cityId = prefs.getString("moji_weather_city_id", "").trim();
    if (cityId.isEmpty()) throw new IOException("未配置墨迹天气城市 ID");
    String appCode = new SecureStore(context).getNamed(APP_CODE_KEY).trim();
    if (appCode.isEmpty()) throw new IOException("未配置墨迹天气 AppCode");
    String endpoint = prefs.getString(
        forecast ? "moji_weather_forecast_endpoint" : "moji_weather_endpoint",
        forecast ? DEFAULT_FORECAST_ENDPOINT : DEFAULT_CURRENT_ENDPOINT).trim();
    validateEndpoint(endpoint);
    JSONObject response = post(endpoint, cityId, appCode);
    return forecast ? formatForecast(response, cityId) : formatCurrent(response, cityId);
  }

  private static JSONObject post(String endpoint, String cityId, String appCode) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    try {
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(20_000);
      connection.setReadTimeout(45_000);
      connection.setDoOutput(true);
      connection.setRequestProperty("Authorization", "APPCODE " + appCode);
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      connection.setRequestProperty("Accept", "application/json");
      String body = "cityId=" + URLEncoder.encode(cityId, "UTF-8");
      try (OutputStream output = connection.getOutputStream()) {
        output.write(body.getBytes(StandardCharsets.UTF_8));
      }
      int status = connection.getResponseCode();
      String raw = read(status >= 200 && status < 300
          ? connection.getInputStream() : connection.getErrorStream());
      if (status < 200 || status >= 300)
        throw new IOException("墨迹天气 HTTP " + status + (raw.isEmpty() ? "" : "：" + clip(raw, 1200)));
      JSONObject value = new JSONObject(raw);
      String code = String.valueOf(value.opt("code"));
      if (!("0".equals(code) || "200".equals(code) || "success".equalsIgnoreCase(code))) {
        String message = value.optString("msg", value.optString("message", "接口返回失败"));
        throw new IOException("墨迹天气返回失败（code " + code + "）：" + message);
      }
      return value;
    } finally {
      connection.disconnect();
    }
  }

  private static String formatCurrent(JSONObject root, String fallbackCityId) {
    JSONObject data = root.optJSONObject("data");
    if (data == null) return "墨迹天气原始结果：" + clip(root.toString(), 5000);
    JSONObject city = data.optJSONObject("city");
    JSONObject condition = data.optJSONObject("condition");
    if (condition == null) return "墨迹天气原始结果：" + clip(data.toString(), 5000);
    String cityName = city == null ? "" : first(city, "name", "secondaryname", "pname");
    String cityId = city == null ? fallbackCityId : city.optString("cityId", fallbackCityId);
    StringBuilder out = new StringBuilder("墨迹天气实况");
    if (!cityName.isEmpty()) out.append(" · ").append(cityName);
    out.append("（cityId ").append(cityId).append("）");
    appendLine(out, "更新时间", first(condition, "updatetime", "updateTime"));
    String weather = first(condition, "condition", "weather");
    String temp = first(condition, "temp", "temperature");
    String feels = first(condition, "realFeel", "feelsLike");
    StringBuilder summary = new StringBuilder(weather);
    if (!temp.isEmpty()) summary.append(summary.length() == 0 ? "" : "，").append(temp).append("℃");
    if (!feels.isEmpty()) summary.append("，体感 ").append(feels).append("℃");
    appendLine(out, "当前", summary.toString());
    appendLine(out, "湿度", suffix(first(condition, "humidity"), "%"));
    String wind = joinNonEmpty(" ", first(condition, "windDir"),
        suffix(first(condition, "windLevel"), "级"), suffix(first(condition, "windSpeed"), "km/h"));
    appendLine(out, "风况", wind);
    appendLine(out, "气压", suffix(first(condition, "pressure"), "hPa"));
    appendLine(out, "紫外线", first(condition, "uvi", "uvIndex"));
    appendLine(out, "日出 / 日落", joinNonEmpty(" / ", first(condition, "sunRise"), first(condition, "sunSet")));
    appendLine(out, "提示", first(condition, "tips", "tip"));
    return out.toString();
  }

  private static String formatForecast(JSONObject root, String fallbackCityId) {
    JSONObject data = root.optJSONObject("data");
    if (data == null) return "墨迹天气预报原始结果：" + clip(root.toString(), 5000);
    JSONObject city = data.optJSONObject("city");
    JSONArray forecast = data.optJSONArray("forecast");
    if (forecast == null) forecast = data.optJSONArray("forecasts");
    if (forecast == null) return "墨迹天气预报原始结果：" + clip(data.toString(), 5000);
    String cityName = city == null ? "" : first(city, "name", "secondaryname", "pname");
    String cityId = city == null ? fallbackCityId : city.optString("cityId", fallbackCityId);
    StringBuilder out = new StringBuilder("墨迹天气预报");
    if (!cityName.isEmpty()) out.append(" · ").append(cityName);
    out.append("（cityId ").append(cityId).append("）");
    for (int i = 0; i < Math.min(7, forecast.length()); i++) {
      JSONObject day = forecast.optJSONObject(i);
      if (day == null) continue;
      String date = first(day, "predictDate", "date");
      String conditionDay = first(day, "conditionDay", "condition", "weatherDay");
      String conditionNight = first(day, "conditionNight", "weatherNight");
      String conditions = conditionNight.isEmpty() || conditionNight.equals(conditionDay)
          ? conditionDay : conditionDay + "转" + conditionNight;
      String temps = joinNonEmpty(" / ", suffix(first(day, "tempDay", "tempMax", "high"), "℃"),
          suffix(first(day, "tempNight", "tempMin", "low"), "℃"));
      String wind = joinNonEmpty(" ", first(day, "windDirDay", "windDir"),
          suffix(first(day, "windLevelDay", "windLevel"), "级"));
      out.append('\n').append("- ").append(date.isEmpty() ? "未来第 " + (i + 1) + " 天" : date)
          .append("：").append(joinNonEmpty("，", conditions, temps, wind));
    }
    return out.toString();
  }

  private static String first(JSONObject value, String... keys) {
    if (value == null) return "";
    for (String key : keys) {
      String result = value.optString(key, "").trim();
      if (!result.isEmpty() && !"null".equalsIgnoreCase(result)) return result;
    }
    return "";
  }

  private static void appendLine(StringBuilder out, String label, String value) {
    if (value == null || value.trim().isEmpty()) return;
    out.append('\n').append(label).append("：").append(value.trim());
  }

  private static String suffix(String value, String suffix) {
    if (value == null || value.trim().isEmpty()) return "";
    String clean = value.trim();
    return clean.endsWith(suffix) ? clean : clean + suffix;
  }

  private static String joinNonEmpty(String separator, String... values) {
    StringBuilder out = new StringBuilder();
    for (String value : values) {
      if (value == null || value.trim().isEmpty()) continue;
      if (out.length() > 0) out.append(separator);
      out.append(value.trim());
    }
    return out.toString();
  }

  private static void validateEndpoint(String endpoint) throws Exception {
    URI uri = new URI(endpoint);
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
        || uri.getUserInfo() != null || uri.getFragment() != null)
      throw new IllegalArgumentException("墨迹天气接口必须是有效的 HTTPS 地址");
  }

  private static String read(InputStream input) throws Exception {
    if (input == null) return "";
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder out = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) out.append(line);
      return out.toString();
    }
  }

  private static String clip(String value, int max) {
    if (value == null) return "";
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }
}
