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

data class StickerGroup(val id: String, val name: String, val images: List<String>)

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
      "下载 zhaoolee/ChineseBQB 贡献分类中的全部可用图片，不下载约 3 GB 的完整仓库。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_emoji",
      "表情符号反应包",
      "从 ChineseBQB 表情符号分类获取常用情绪与反应图片。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_cat",
      "猫咪日常",
      "来自 ChineseBQB 的猫咪日常分类，适合轻松聊天。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_pat",
      "拍一拍反应",
      "来自 ChineseBQB 的微信拍一拍主题反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_food",
      "吃货反应",
      "来自 ChineseBQB 的吃饭与投喂主题反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_funny",
      "滑稽反应",
      "来自 ChineseBQB 的滑稽情绪与聊天反应分类。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_hamster",
      "仓鼠日常",
      "仓鼠主题的可爱日常与情绪反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_dog",
      "狗狗反应",
      "狗狗主题的聊天、疑问和情绪反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_tom_jerry",
      "猫和老鼠",
      "猫和老鼠主题的经典聊天反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_spongebob",
      "海绵宝宝",
      "海绵宝宝主题的日常聊天与情绪反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_heart",
      "心心表达",
      "适合喜欢、感谢、安慰和亲密表达的爱心主题图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
    ),
    StickerPack(
      "chinesebqb_pingu",
      "企鹅家族",
      "企鹅家族主题的轻松日常与聊天反应图。",
      "https://github.com/zhaoolee/ChineseBQB",
      "完整分类"
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

  fun groups(context: Context, prefs: SharedPreferences): List<StickerGroup> = buildList {
    val favoriteImages = ordered(prefs, "favorites", favorites(prefs).toList().sorted())
    if (favoriteImages.isNotEmpty()) add(StickerGroup("favorites", "收藏", favoriteImages))
    packs.forEach { pack ->
      val values = ordered(prefs, pack.id, imagesForPack(context, pack.id))
      if (values.isNotEmpty()) add(StickerGroup(pack.id, pack.name, values))
    }
    val known = packs.flatMap { imagesForPack(context, it.id) }.toSet()
    val local = images(File(context.filesDir, "sticker_favorites")).filterNot { it in favoriteImages || it in known }
    if (local.isNotEmpty()) add(StickerGroup("local", "本地导入", ordered(prefs, "local", local)))
  }

  fun move(prefs: SharedPreferences, groupId: String, values: List<String>, from: Int, to: Int): List<String> {
    if (from !in values.indices || to !in values.indices || from == to) return values
    val updated = values.toMutableList()
    updated.add(to, updated.removeAt(from))
    saveOrder(prefs, groupId, updated)
    return updated
  }

  fun saveOrder(prefs: SharedPreferences, groupId: String, values: List<String>) {
    prefs.edit().putString("sticker_order_$groupId", values.distinct().joinToString("\n")).apply()
  }

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

  /** 从收藏移除单个表情；文件位于 sticker_favorites/ 时一并删除。 */
  fun removeFavorite(context: Context, prefs: SharedPreferences, reference: String): Boolean {
    val current = favorites(prefs).toMutableSet()
    if (!current.remove(reference)) return false
    prefs.edit().putStringSet(FAVORITES_KEY, current).apply()
    runCatching {
      val favoritesDir = File(context.filesDir, "sticker_favorites").canonicalPath
      val target = File(reference)
      if (target.canonicalPath.startsWith(favoritesDir)) target.delete()
    }
    return true
  }

  fun import(context: Context, prefs: SharedPreferences, values: List<Uri>): Int {
    var imported = 0
    values.forEach { uri -> if (addFavorite(context, prefs, uri.toString()) != null) imported++ }
    return imported
  }

  fun deletePack(context: Context, packId: String) {
    File(context.filesDir, "sticker_packs/$packId").deleteRecursively()
  }

  fun download(context: Context, packId: String, status: (String) -> Unit) {
    val finalDir = File(context.filesDir, "sticker_packs/$packId")
    val tempDir = File(context.cacheDir, "sticker_download_${packId}_${UUID.randomUUID()}")
    tempDir.mkdirs()
    try {
      when (packId) {
        "owo" -> downloadOwo(tempDir, status)
        "chinesebqb" -> downloadChineseBqb(tempDir, status)
        "chinesebqb_emoji" -> downloadChineseBqbFolder(tempDir, "023Emoji_表情符号BQB", "emoji", status)
        "chinesebqb_cat" -> downloadChineseBqbFolder(tempDir, "049CatEveryday_猫咪日常BQB", "cat", status)
        "chinesebqb_pat" -> downloadChineseBqbFolder(tempDir, "072PaiYiPai_微信拍一拍👋BQB", "pat", status)
        "chinesebqb_food" -> downloadChineseBqbFolder(tempDir, "033Eat_吃🍔BQB", "food", status)
        "chinesebqb_funny" -> downloadChineseBqbFolder(tempDir, "001Funny_滑稽大佬😏BQB", "funny", status)
        "chinesebqb_hamster" -> downloadChineseBqbFolder(tempDir, "006Hamster_仓鼠🐹BQB", "hamster", status)
        "chinesebqb_dog" -> downloadChineseBqbFolder(tempDir, "011Dog_狗🐶BQB", "dog", status)
        "chinesebqb_tom_jerry" -> downloadChineseBqbFolder(tempDir, "035TomAndJerry_猫和老鼠BQB", "tom_jerry", status)
        "chinesebqb_spongebob" -> downloadChineseBqbFolder(tempDir, "048SpongeBob_海绵宝宝BQB", "spongebob", status)
        "chinesebqb_heart" -> downloadChineseBqbFolder(tempDir, "055AllHeart_全是心心BQB", "heart", status)
        "chinesebqb_pingu" -> downloadChineseBqbFolder(tempDir, "108_Pingu_企鹅家族_BQB", "pingu", status)
        else -> error("未知资源包")
      }
      if (images(tempDir).isEmpty()) error("资源包中没有可用图片")
      finalDir.deleteRecursively()
      finalDir.parentFile?.mkdirs()
      if (!tempDir.renameTo(finalDir)) {
        tempDir.copyRecursively(finalDir, overwrite = true)
        tempDir.deleteRecursively()
      }
      status("下载完成")
    } catch (error: Exception) {
      tempDir.deleteRecursively()
      throw error
    }
  }

  private fun downloadOwo(target: File, status: (String) -> Unit) {
    status("正在连接 GitHub")
    val connection = open("https://codeload.github.com/shangskr/owo/zip/refs/heads/main")
    var count = 0
    try {
      ZipInputStream(BufferedInputStream(connection.inputStream)).use { zip ->
        val buffer = ByteArray(16 * 1024)
        while (true) {
          val entry = zip.nextEntry ?: break
          val extension = entry.name.substringAfterLast('.', "").lowercase()
          if (!entry.isDirectory && extension in imageExtensions) {
            val safeName = entry.name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._\u4e00-\u9fa5-]"), "_")
            val file = File(target, "${count.toString().padStart(10, '0')}_$safeName")
            FileOutputStream(file).use { output ->
              while (true) {
                val n = zip.read(buffer)
                if (n <= 0) break
                output.write(buffer, 0, n)
              }
            }
            count++
            status("已获取 $count 张表情")
          }
          zip.closeEntry()
        }
      }
    } finally {
      connection.disconnect()
    }
  }

  private fun downloadChineseBqb(target: File, status: (String) -> Unit) {
    downloadChineseBqbFolder(target, "000Contribution_贡献🇨🇳BQB", "bqb", status)
  }

  private fun downloadChineseBqbFolder(target: File, folderName: String, prefix: String, status: (String) -> Unit) {
    status("正在连接官方资源包")
    val archiveName = URLEncoder.encode("$folderName.zip", "UTF-8").replace("+", "%20")
    val connection = open("https://github.com/zhaoolee/ChineseBQB/raw/master/$archiveName")
    var count = 0
    try {
      ZipInputStream(BufferedInputStream(connection.inputStream)).use { zip ->
        val buffer = ByteArray(16 * 1024)
        while (true) {
          val entry = zip.nextEntry ?: break
          val extension = entry.name.substringAfterLast('.', "").lowercase()
          if (!entry.isDirectory && extension in imageExtensions) {
            val file = File(target, "${prefix}_${count.toString().padStart(10, '0')}.$extension")
            FileOutputStream(file).use { output ->
              while (true) {
                val read = zip.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
              }
            }
            count++
            status("已获取 $count 张表情")
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
    val contentExtension=if(reference.startsWith("content:"))when(context.contentResolver.getType(Uri.parse(reference))?.lowercase()){"image/gif"->"gif";"image/webp"->"webp";"image/jpeg"->"jpg";"image/png"->"png";else->null}else null
    val extension = contentExtension?:reference.substringBefore('?').substringAfterLast('.', "png").lowercase().takeIf { it in imageExtensions } ?: "png"
    val target = File(directory, "favorite_${UUID.randomUUID()}.$extension")
    return runCatching {
      val input = if (reference.startsWith("content:")) context.contentResolver.openInputStream(Uri.parse(reference)) else existing.takeIf { it.isFile }?.inputStream()
      input?.use { source -> FileOutputStream(target).use { output -> source.copyTo(output) } } ?: error("无法读取图片")
      target.absolutePath
    }.getOrNull()
  }

  private fun images(directory: File): List<String> =
    directory.walkTopDown().filter { it.isFile && it.extension.lowercase() in imageExtensions }.map { it.absolutePath }.toList().sorted()

  private fun ordered(prefs: SharedPreferences, groupId: String, values: List<String>): List<String> {
    val available = values.distinct()
    val availableSet = available.toSet()
    val stored = prefs.getString("sticker_order_$groupId", "").orEmpty().lineSequence().filter { it in availableSet }.toList()
    return (stored + available.filterNot { it in stored.toSet() }).distinct()
  }

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
