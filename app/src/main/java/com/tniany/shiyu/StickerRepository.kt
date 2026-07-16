package com.tniany.shiyu

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.zip.ZipInputStream

data class StickerPack(
  val id: String,
  val name: String,
  val description: String,
  val sourceUrl: String,
  val estimatedSize: String
)

object StickerRepository {
  private const val FAVORITES_KEY = "favorite_stickers"
  private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "webp")

  val packs = listOf(
    StickerPack(
      "owo",
      "owo 常用表情",
      "约 292 张常用平台表情，资源来自 shangskr/owo。",
      "https://github.com/shangskr/owo",
      "约 4 MB"
    ),
    StickerPack(
      "chinesebqb",
      "ChineseBQB 精选",
      "从 zhaoolee/ChineseBQB 官方仓库按需获取精选图片，不下载约 3 GB 的完整仓库。",
      "https://github.com/zhaoolee/ChineseBQB",
      "最多 48 张"
    ),
    StickerPack(
      "chinesebqb_emoji",
      "表情符号反应包",
      "从 ChineseBQB 表情符号分类获取常用情绪与反应图片。",
      "https://github.com/zhaoolee/ChineseBQB",
      "最多 60 张"
    ),
    StickerPack(
      "chinesebqb_cat",
      "猫咪日常",
      "来自 ChineseBQB 的猫咪日常分类，适合轻松聊天。",
      "https://github.com/zhaoolee/ChineseBQB",
      "约 14 张"
    ),
    StickerPack(
      "chinesebqb_pat",
      "拍一拍反应",
      "来自 ChineseBQB 的微信拍一拍主题反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "约 9 张"
    ),
    StickerPack(
      "chinesebqb_food",
      "吃货反应",
      "来自 ChineseBQB 的吃饭与投喂主题反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "约 7 张"
    )
  )

  fun all(context: Context): List<String> = buildList {
    addAll(images(File(context.filesDir, "sticker_favorites")))
    addAll(images(File(context.filesDir, "sticker_packs")))
  }.distinct()

  fun favorites(prefs: SharedPreferences): Set<String> =
    prefs.getStringSet(FAVORITES_KEY, emptySet())?.filter { File(it).isFile }?.toSet().orEmpty()

  fun isInstalled(context: Context, packId: String): Boolean =
    images(File(context.filesDir, "sticker_packs/$packId")).isNotEmpty()

  fun count(context: Context, packId: String): Int =
    images(File(context.filesDir, "sticker_packs/$packId")).size

  fun imagesForPack(context: Context, packId: String): List<String> =
    images(File(context.filesDir, "sticker_packs/$packId"))

  fun toggleFavorite(context: Context, prefs: SharedPreferences, reference: String): Boolean {
    val current = favorites(prefs).toMutableSet()
    val local = persist(context, reference) ?: return false
    val added = if (current.contains(local)) { current.remove(local); false } else { current.add(local); true }
    prefs.edit().putStringSet(FAVORITES_KEY, current).apply()
    return added
  }

  fun addFavorite(context: Context, prefs: SharedPreferences, reference: String): String? {
    val local = persist(context, reference) ?: return null
    val current = favorites(prefs).toMutableSet()
    current.add(local)
    prefs.edit().putStringSet(FAVORITES_KEY, current).apply()
    return local
  }

  fun import(context: Context, prefs: SharedPreferences, values: List<Uri>): Int {
    var imported = 0
    values.forEach { uri -> if (addFavorite(context, prefs, uri.toString()) != null) imported++ }
    return imported
  }

  fun deletePack(context: Context, packId: String) {
    File(context.filesDir, "sticker_packs/$packId").deleteRecursively()
  }

  fun download(context: Context, packId: String, progress: (Int, String) -> Unit) {
    val finalDir = File(context.filesDir, "sticker_packs/$packId")
    val tempDir = File(context.cacheDir, "sticker_download_${packId}_${UUID.randomUUID()}")
    tempDir.mkdirs()
    try {
      when (packId) {
        "owo" -> downloadOwo(tempDir, progress)
        "chinesebqb" -> downloadChineseBqb(tempDir, progress)
        "chinesebqb_emoji" -> downloadChineseBqbFolder(tempDir, "023Emoji_表情符号BQB", "emoji", 60, progress)
        "chinesebqb_cat" -> downloadChineseBqbFolder(tempDir, "049CatEveryday_猫咪日常BQB", "cat", 30, progress)
        "chinesebqb_pat" -> downloadChineseBqbFolder(tempDir, "072PaiYiPai_微信拍一拍👋BQB", "pat", 30, progress)
        "chinesebqb_food" -> downloadChineseBqbFolder(tempDir, "033Eat_吃🍔BQB", "food", 30, progress)
        else -> error("未知资源包")
      }
      if (images(tempDir).isEmpty()) error("资源包中没有可用图片")
      finalDir.deleteRecursively()
      finalDir.parentFile?.mkdirs()
      if (!tempDir.renameTo(finalDir)) {
        tempDir.copyRecursively(finalDir, overwrite = true)
        tempDir.deleteRecursively()
      }
      progress(100, "下载完成")
    } catch (error: Exception) {
      tempDir.deleteRecursively()
      throw error
    }
  }

  private fun downloadOwo(target: File, progress: (Int, String) -> Unit) {
    progress(4, "正在连接 GitHub")
    val connection = open("https://codeload.github.com/shangskr/owo/zip/refs/heads/main")
    val length = connection.contentLengthLong.coerceAtLeast(1L)
    var readTotal = 0L
    var count = 0
    ZipInputStream(BufferedInputStream(connection.inputStream)).use { zip ->
      val buffer = ByteArray(16 * 1024)
      while (true) {
        val entry = zip.nextEntry ?: break
        val extension = entry.name.substringAfterLast('.', "").lowercase()
        if (!entry.isDirectory && extension in imageExtensions && count < 500) {
          val safeName = entry.name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._\u4e00-\u9fa5-]"), "_")
          val file = File(target, "${count.toString().padStart(3, '0')}_$safeName")
          FileOutputStream(file).use { output ->
            while (true) {
              val n = zip.read(buffer)
              if (n <= 0) break
              readTotal += n
              if (readTotal > 80L * 1024 * 1024) error("资源包超过 80 MB 安全限制")
              output.write(buffer, 0, n)
            }
          }
          count++
          progress((10 + readTotal * 82 / length).toInt().coerceIn(10, 92), "已获取 $count 张表情")
        }
        zip.closeEntry()
      }
    }
    connection.disconnect()
  }

  private fun downloadChineseBqb(target: File, progress: (Int, String) -> Unit) {
    downloadChineseBqbFolder(target, "000Contribution_贡献🇨🇳BQB", "bqb", 48, progress)
  }

  private fun downloadChineseBqbFolder(target: File, folderName: String, prefix: String, limit: Int, progress: (Int, String) -> Unit) {
    progress(3, "正在连接官方资源包")
    val archiveName = URLEncoder.encode("$folderName.zip", "UTF-8").replace("+", "%20")
    val connection = open("https://github.com/zhaoolee/ChineseBQB/raw/master/$archiveName")
    var count = 0
    var extractedBytes = 0L
    try {
      ZipInputStream(BufferedInputStream(connection.inputStream)).use { zip ->
        val buffer = ByteArray(16 * 1024)
        while (count < limit.coerceIn(1, 80)) {
          val entry = zip.nextEntry ?: break
          val extension = entry.name.substringAfterLast('.', "").lowercase()
          if (!entry.isDirectory && extension in imageExtensions) {
            val file = File(target, "${prefix}_${count.toString().padStart(3, '0')}.$extension")
            FileOutputStream(file).use { output ->
              while (true) {
                val read = zip.read(buffer)
                if (read <= 0) break
                extractedBytes += read
                if (extractedBytes > 80L * 1024 * 1024) error("资源包超过 80 MB 安全限制")
                output.write(buffer, 0, read)
              }
            }
            count++
            progress((5 + count * 90 / limit.coerceIn(1, 80)).coerceIn(5, 95), "已获取 $count 张表情")
          }
          zip.closeEntry()
        }
      }
    } finally {
      connection.disconnect()
    }
    if (count == 0) error("官方资源包暂时没有可用图片")
  }

  private fun persist(context: Context, reference: String): String? {
    val existing = File(reference)
    if (existing.isFile && existing.absolutePath.startsWith(context.filesDir.absolutePath)) return existing.absolutePath
    val directory = File(context.filesDir, "sticker_favorites").apply { mkdirs() }
    val extension = reference.substringBefore('?').substringAfterLast('.', "png").lowercase().takeIf { it in imageExtensions } ?: "png"
    val target = File(directory, "favorite_${UUID.randomUUID()}.$extension")
    return runCatching {
      val input = if (reference.startsWith("content:")) context.contentResolver.openInputStream(Uri.parse(reference)) else existing.takeIf { it.isFile }?.inputStream()
      input?.use { source -> FileOutputStream(target).use { output -> source.copyTo(output) } } ?: error("无法读取图片")
      target.absolutePath
    }.getOrNull()
  }

  private fun images(directory: File): List<String> =
    directory.walkTopDown().filter { it.isFile && it.extension.lowercase() in imageExtensions }.map { it.absolutePath }.toList().sorted()

  private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
    connectTimeout = 20_000
    readTimeout = 60_000
    setRequestProperty("User-Agent", "ShiYu-Android/${BuildConfig.VERSION_NAME}")
    setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream")
    instanceFollowRedirects = true
    val code = responseCode
    if (code !in 200..299) throw IllegalStateException("资源服务器 HTTP $code")
  }
}
