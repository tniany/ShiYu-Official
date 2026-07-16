package com.tniany.shiyu;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.CRC32;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Builds portable Character Card V2/V3 JSON and PNG files without performing any I/O. */
final class CharacterCardExporter {
  private static final byte[] PNG_SIGNATURE =
      new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
  private static final int MAX_AVATAR_BYTES = 32 * 1024 * 1024;
  private static final int MAX_DECODE_DIMENSION = 2048;

  enum Format {
    JSON_V2("application/json", "json", "ccv2"),
    JSON_V3("application/json", "json", "ccv3"),
    PNG_V2("image/png", "png", "ccv2"),
    PNG_V3("image/png", "png", "ccv3");

    final String mimeType;
    final String extension;
    final String nameSuffix;

    Format(String mimeType, String extension, String nameSuffix) {
      this.mimeType = mimeType;
      this.extension = extension;
      this.nameSuffix = nameSuffix;
    }

    boolean isPng() {
      return this == PNG_V2 || this == PNG_V3;
    }

    boolean isV3() {
      return this == JSON_V3 || this == PNG_V3;
    }
  }

  static final class ExportedFile {
    private final byte[] bytes;
    private final String mimeType;
    private final String extension;
    private final String suggestedFileName;
    private final Format format;

    ExportedFile(
        byte[] bytes,
        String mimeType,
        String extension,
        String suggestedFileName,
        Format format) {
      this.bytes = bytes;
      this.mimeType = mimeType;
      this.extension = extension;
      this.suggestedFileName = suggestedFileName;
      this.format = format;
    }

    byte[] getBytes() {
      return bytes;
    }

    String getMimeType() {
      return mimeType;
    }

    String getExtension() {
      return extension;
    }

    String getSuggestedFileName() {
      return suggestedFileName;
    }

    Format getFormat() {
      return format;
    }
  }

  private CharacterCardExporter() {}

  static ExportedFile export(MainActivity.Card card, Format format)
      throws IOException, JSONException {
    return export(card, format, readAvatar(card == null ? null : card.avatar), null);
  }

  /**
   * Avatar bytes can be supplied by the caller for content URIs. When null, a deterministic placeholder
   * is generated so PNG export remains valid even for cards without an avatar.
   */
  static ExportedFile export(
      MainActivity.Card card, Format format, byte[] suppliedAvatar, String suppliedAvatarMime)
      throws IOException, JSONException {
    if (card == null) throw new IllegalArgumentException("card == null");
    if (format == null) throw new IllegalArgumentException("format == null");
    if (suppliedAvatar != null && suppliedAvatar.length > MAX_AVATAR_BYTES) {
      throw new IOException("头像文件过大");
    }

    byte[] cleanAvatar = null;
    String avatarMime = suppliedAvatarMime;
    if (suppliedAvatar != null && suppliedAvatar.length > 0) {
      avatarMime = normalizeImageMime(avatarMime, suppliedAvatar);
      cleanAvatar = isPng(suppliedAvatar) ? stripCardChunks(suppliedAvatar) : suppliedAvatar;
    }

    JSONObject requestedCard;
    byte[] output;
    if (format.isPng()) {
      byte[] pngAvatar = ensurePng(cleanAvatar, card.name);
      JSONObject v2 = buildCard(card, false, null, null);
      JSONObject v3 = buildCard(card, true, null, null);
      requestedCard = format.isV3() ? v3 : v2;
      output = embedCardData(pngAvatar, v2, format.isV3() ? v3 : null);
    } else {
      String dataUri =
          cleanAvatar == null
              ? null
              : "data:"
                  + normalizeImageMime(avatarMime, cleanAvatar)
                  + ";base64,"
                  + Base64.encodeToString(cleanAvatar, Base64.NO_WRAP);
      requestedCard = buildCard(card, format.isV3(), dataUri, imageExtension(avatarMime, cleanAvatar));
      output = requestedCard.toString(2).getBytes(StandardCharsets.UTF_8);
    }

    String baseName = safeFileName(card.name);
    String suggested =
        String.format(
            Locale.ROOT, "%s.%s.%s", baseName, format.nameSuffix, format.extension);
    return new ExportedFile(output, format.mimeType, format.extension, suggested, format);
  }

  private static JSONObject buildCard(
      MainActivity.Card card, boolean v3, String avatarDataUri, String avatarExtension)
      throws JSONException {
    JSONObject data = new JSONObject();
    data.put("name", text(card.name));
    data.put("description", text(card.desc));
    data.put("personality", text(card.persona));
    data.put("scenario", "");
    data.put("first_mes", text(card.greeting));
    data.put("mes_example", text(card.exampleMessages));
    data.put("creator_notes", text(card.creatorNotes));
    data.put("system_prompt", text(card.systemPrompt));
    data.put("post_history_instructions", text(card.postHistoryInstructions));
    data.put("alternate_greetings", jsonArray(card.alternateGreetings));
    data.put("tags", jsonArray(card.tags));
    data.put("creator", text(card.creator));
    data.put("character_version", text(card.characterVersion));

    JSONObject extensions = jsonObject(card.rawExtensions);
    if (!text(card.rawExtensions).isEmpty()
        && extensions.length() == 0
        && !"{}".equals(text(card.rawExtensions).trim())) {
      extensions.put("shiyu_raw_extensions", card.rawExtensions);
    }
    putParsedIfPresent(extensions, "regex_scripts", card.regexRules);
    putParsedIfPresent(extensions, "preset", card.preset);
    if (!text(card.frontend).isEmpty()) extensions.put("frontend_html", card.frontend);
    extensions.put("pat_self", text(card.patSelfText));
    extensions.put("pat_user", text(card.patUserText));

    JSONObject shiyu = extensions.optJSONObject("shiyu");
    String shiyuKey = "shiyu";
    if (shiyu == null) {
      shiyu = new JSONObject();
      if (extensions.has(shiyuKey) && extensions.opt(shiyuKey) != JSONObject.NULL) {
        shiyuKey = "shiyu_export";
        shiyu = extensions.optJSONObject(shiyuKey);
        if (shiyu == null) shiyu = new JSONObject();
      }
    }
    shiyu.put("pat_self", text(card.patSelfText));
    shiyu.put("pat_user", text(card.patUserText));
    if (avatarDataUri != null) shiyu.put("avatar_data_uri", avatarDataUri);
    extensions.put(shiyuKey, shiyu);
    data.put("extensions", extensions);

    if (!text(card.worldBook).isEmpty()) {
      data.put("character_book", parsedValue(card.worldBook));
    }

    if (v3) {
      data.put("group_only_greetings", new JSONArray());
      JSONArray assets = new JSONArray();
      JSONObject icon = new JSONObject();
      icon.put("type", "icon");
      icon.put("name", "main");
      icon.put("ext", avatarExtension == null ? "png" : avatarExtension);
      icon.put("uri", avatarDataUri == null ? "ccdefault:" : avatarDataUri);
      assets.put(icon);
      data.put("assets", assets);
    }

    return new JSONObject()
        .put("spec", v3 ? "chara_card_v3" : "chara_card_v2")
        .put("spec_version", v3 ? "3.0" : "2.0")
        .put("data", data);
  }

  private static void putParsedIfPresent(JSONObject target, String key, String raw)
      throws JSONException {
    if (!text(raw).isEmpty()) target.put(key, parsedValue(raw));
  }

  private static Object parsedValue(String raw) {
    String value = text(raw).trim();
    if (value.isEmpty()) return "";
    try {
      Object parsed = new JSONTokener(value).nextValue();
      return parsed == null ? value : parsed;
    } catch (JSONException ignored) {
      return value;
    }
  }

  private static JSONObject jsonObject(String raw) {
    Object value = parsedValue(raw);
    if (value instanceof JSONObject) {
      try {
        return new JSONObject(value.toString());
      } catch (JSONException ignored) {
        return new JSONObject();
      }
    }
    return new JSONObject();
  }

  private static JSONArray jsonArray(String raw) {
    Object value = parsedValue(raw);
    if (value instanceof JSONArray) {
      try {
        return new JSONArray(value.toString());
      } catch (JSONException ignored) {
        return new JSONArray();
      }
    }
    JSONArray array = new JSONArray();
    String string = text(raw).trim();
    if (!string.isEmpty() && !"[]".equals(string)) {
      for (String item : string.split("[,\\n]")) {
        if (!item.trim().isEmpty()) array.put(item.trim());
      }
    }
    return array;
  }

  private static byte[] readAvatar(String path) throws IOException {
    String value = text(path).trim();
    if (value.isEmpty()) return null;
    if (value.startsWith("file://")) value = value.substring("file://".length());
    File file = new File(value);
    if (!file.isFile()) return null;
    if (file.length() > MAX_AVATAR_BYTES) throw new IOException("头像文件过大");
    try (FileInputStream input = new FileInputStream(file);
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) file.length())) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) != -1) {
        if (count > 0) output.write(buffer, 0, count);
      }
      return output.toByteArray();
    }
  }

  private static byte[] ensurePng(byte[] avatar, String characterName) throws IOException {
    if (avatar == null || avatar.length == 0) return placeholderPng(characterName);
    if (isPng(avatar)) return stripCardChunks(avatar);

    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(avatar, 0, avatar.length, bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("无法读取角色头像");
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 1;
    int largest = Math.max(bounds.outWidth, bounds.outHeight);
    while (largest / options.inSampleSize > MAX_DECODE_DIMENSION) options.inSampleSize *= 2;
    Bitmap bitmap = BitmapFactory.decodeByteArray(avatar, 0, avatar.length, options);
    if (bitmap == null) throw new IOException("无法读取角色头像");
    try {
      return compressPng(bitmap);
    } finally {
      bitmap.recycle();
    }
  }

  private static byte[] placeholderPng(String characterName) throws IOException {
    Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.rgb(240, 235, 246));
    Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
    circle.setColor(Color.rgb(126, 101, 144));
    canvas.drawCircle(256, 256, 176, circle);
    String initial = text(characterName).trim();
    initial = initial.isEmpty() ? "S" : initial.substring(0, 1).toUpperCase(Locale.ROOT);
    Paint letter = new Paint(Paint.ANTI_ALIAS_FLAG);
    letter.setColor(Color.WHITE);
    letter.setTextAlign(Paint.Align.CENTER);
    letter.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    letter.setTextSize(196f);
    Paint.FontMetrics metrics = letter.getFontMetrics();
    float baseline = 256f - (metrics.ascent + metrics.descent) / 2f;
    canvas.drawText(initial, 256f, baseline, letter);
    try {
      return compressPng(bitmap);
    } finally {
      bitmap.recycle();
    }
  }

  private static byte[] compressPng(Bitmap bitmap) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
      throw new IOException("头像转换为 PNG 失败");
    }
    return output.toByteArray();
  }

  private static byte[] embedCardData(byte[] png, JSONObject v2, JSONObject v3)
      throws IOException {
    byte[] clean = stripCardChunks(png);
    ByteArrayOutputStream output = new ByteArrayOutputStream(clean.length + 4096);
    output.write(PNG_SIGNATURE, 0, PNG_SIGNATURE.length);
    int cursor = PNG_SIGNATURE.length;
    boolean wroteMetadata = false;
    while (cursor + 12 <= clean.length) {
      long size = readUnsignedInt(clean, cursor);
      if (size > Integer.MAX_VALUE || cursor + 12L + size > clean.length) {
        throw new IOException("PNG 数据损坏");
      }
      int length = (int) size;
      String type = new String(clean, cursor + 4, 4, StandardCharsets.US_ASCII);
      if ("IEND".equals(type)) {
        writeTextChunk(output, "chara", v2.toString());
        if (v3 != null) writeTextChunk(output, "ccv3", v3.toString());
        wroteMetadata = true;
      }
      output.write(clean, cursor, length + 12);
      cursor += length + 12;
      if ("IEND".equals(type)) break;
    }
    if (!wroteMetadata) throw new IOException("PNG 缺少 IEND 数据块");
    return output.toByteArray();
  }

  private static byte[] stripCardChunks(byte[] png) throws IOException {
    if (!isPng(png)) throw new IOException("头像不是有效的 PNG");
    ByteArrayOutputStream output = new ByteArrayOutputStream(png.length);
    output.write(PNG_SIGNATURE, 0, PNG_SIGNATURE.length);
    int cursor = PNG_SIGNATURE.length;
    boolean sawIend = false;
    while (cursor + 12 <= png.length) {
      long size = readUnsignedInt(png, cursor);
      if (size > Integer.MAX_VALUE || cursor + 12L + size > png.length) {
        throw new IOException("PNG 数据损坏");
      }
      int length = (int) size;
      String type = new String(png, cursor + 4, 4, StandardCharsets.US_ASCII);
      boolean cardChunk = isTextChunk(type) && isCardKeyword(png, cursor + 8, length);
      if (!cardChunk) output.write(png, cursor, length + 12);
      cursor += length + 12;
      if ("IEND".equals(type)) {
        sawIend = true;
        break;
      }
    }
    if (!sawIend) throw new IOException("PNG 缺少 IEND 数据块");
    return output.toByteArray();
  }

  private static boolean isTextChunk(String type) {
    return "tEXt".equals(type) || "iTXt".equals(type) || "zTXt".equals(type);
  }

  private static boolean isCardKeyword(byte[] png, int offset, int length) {
    int end = offset;
    int limit = Math.min(png.length, offset + length);
    while (end < limit && png[end] != 0) end++;
    if (end == limit) return false;
    String keyword = new String(png, offset, end - offset, StandardCharsets.ISO_8859_1);
    return "chara".equalsIgnoreCase(keyword) || "ccv3".equalsIgnoreCase(keyword);
  }

  private static void writeTextChunk(ByteArrayOutputStream output, String keyword, String json) {
    byte[] encoded = Base64.encode(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    byte[] key = keyword.getBytes(StandardCharsets.ISO_8859_1);
    byte[] data = new byte[key.length + 1 + encoded.length];
    System.arraycopy(key, 0, data, 0, key.length);
    System.arraycopy(encoded, 0, data, key.length + 1, encoded.length);
    writeChunk(output, "tEXt", data);
  }

  private static void writeChunk(ByteArrayOutputStream output, String type, byte[] data) {
    byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
    writeInt(output, data.length);
    output.write(typeBytes, 0, typeBytes.length);
    output.write(data, 0, data.length);
    CRC32 crc = new CRC32();
    crc.update(typeBytes);
    crc.update(data);
    writeInt(output, (int) crc.getValue());
  }

  private static long readUnsignedInt(byte[] bytes, int offset) {
    return ((bytes[offset] & 255L) << 24)
        | ((bytes[offset + 1] & 255L) << 16)
        | ((bytes[offset + 2] & 255L) << 8)
        | (bytes[offset + 3] & 255L);
  }

  private static void writeInt(ByteArrayOutputStream output, int value) {
    output.write((value >>> 24) & 255);
    output.write((value >>> 16) & 255);
    output.write((value >>> 8) & 255);
    output.write(value & 255);
  }

  private static boolean isPng(byte[] bytes) {
    return bytes != null
        && bytes.length >= PNG_SIGNATURE.length
        && Arrays.equals(Arrays.copyOf(bytes, PNG_SIGNATURE.length), PNG_SIGNATURE);
  }

  private static String normalizeImageMime(String supplied, byte[] bytes) {
    String mime = text(supplied).trim().toLowerCase(Locale.ROOT);
    if (mime.startsWith("image/")) return mime;
    if (isPng(bytes)) return "image/png";
    if (bytes != null && bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216) {
      return "image/jpeg";
    }
    if (bytes != null
        && bytes.length >= 12
        && bytes[0] == 'R'
        && bytes[1] == 'I'
        && bytes[2] == 'F'
        && bytes[3] == 'F'
        && bytes[8] == 'W'
        && bytes[9] == 'E'
        && bytes[10] == 'B'
        && bytes[11] == 'P') {
      return "image/webp";
    }
    if (bytes != null && bytes.length >= 6) {
      String head = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
      if (head.startsWith("GIF8")) return "image/gif";
    }
    return "application/octet-stream";
  }

  private static String imageExtension(String suppliedMime, byte[] bytes) {
    String mime = normalizeImageMime(suppliedMime, bytes);
    if ("image/jpeg".equals(mime)) return "jpg";
    if ("image/webp".equals(mime)) return "webp";
    if ("image/gif".equals(mime)) return "gif";
    return "png";
  }

  private static String safeFileName(String value) {
    String name = text(value).trim().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
    name = name.replaceAll("[. ]+$", "");
    if (name.isEmpty()) name = "character";
    if (name.length() > 80) name = name.substring(0, 80).trim();
    return name.isEmpty() ? "character" : name;
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}
