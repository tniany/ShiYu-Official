package com.tniany.shiyu

import com.llamatik.library.platform.LlamaBridge
import java.io.File

object LocalLlamaEngine {
  private val lock = LocalInferenceLock.LOCK
  private var loadedPath = ""
  private var loadedConfig = ""
  private var loaded = false

  @JvmStatic
  fun complete(
    modelPath: String,
    systemPrompt: String,
    prompt: String,
    contextSize: Int,
    threads: Int,
    temperature: Float,
    maxTokens: Int
  ): String = synchronized(lock) {
    require(android.os.Build.VERSION.SDK_INT >= 26) {
      "App 内模型推理需要 Android 8.0 或更高版本"
    }
    require(android.os.Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) {
      "App 内模型推理仅支持 64 位 ARM 设备"
    }
    val file = File(modelPath)
    require(file.isFile && file.length() > 0L) { "本地模型文件不存在" }
    val safeContext = contextSize.coerceIn(512, 8192)
    val safeThreads = threads.coerceIn(1, 8)
    val key = "$safeContext:$safeThreads"
    val bridge = LlamaBridge.INSTANCE
    LocalImageEngine.release()
    if (loadedPath != file.absolutePath || loadedConfig != key || !loaded) {
      if (loaded) bridge.shutdown()
      bridge.nativeUpdateGenerationParams(
        temperature.coerceIn(0f, 2f), maxTokens.coerceIn(32, 2048), .9f, 40, 1.1f,
        safeContext, safeThreads, true, false, 256, 0
      )
      loaded = bridge.initGenerateModel(file.absolutePath)
      check(loaded) { "本地模型加载失败，可能是模型格式不兼容或设备内存不足" }
      loadedPath = file.absolutePath
      loadedConfig = key
    } else {
      bridge.nativeUpdateGenerationParams(
        temperature.coerceIn(0f, 2f), maxTokens.coerceIn(32, 2048), .9f, 40, 1.1f,
        safeContext, safeThreads, true, false, 256, 0
      )
    }
    bridge.generateWithContext(systemPrompt, "", prompt).trim()
      .ifBlank { error("本地模型返回了空内容") }
  }

  @JvmStatic
  fun release() = synchronized(lock) {
    if (loaded) runCatching { LlamaBridge.INSTANCE.shutdown() }
    loaded = false
    loadedPath = ""
    loadedConfig = ""
  }
}
