package com.tniany.shiyu;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import com.llamatik.library.platform.StableDiffusionBridge;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

final class LocalImageEngine {
  private static final Object LOCK = LocalInferenceLock.LOCK;
  private static String loadedPath = "";
  private static boolean loaded;

  private LocalImageEngine() {}

  static boolean npuSupported(Context context) {
    return false;
  }

  static String generate(Context context, File model, String prompt, String negativePrompt,
      int width, int height, int steps, float cfgScale, long seed, int threads) throws Exception {
    return generate(context, model, prompt, negativePrompt, width, height, steps, cfgScale, seed, threads, "cpu");
  }

  static String generate(Context context, File model, String prompt, String negativePrompt,
      int width, int height, int steps, float cfgScale, long seed, int threads, String runtime) throws Exception {
    synchronized (LOCK) {
      if ("npu".equalsIgnoreCase(runtime) && !npuSupported(context))
        throw new IOException("当前设备或安装包不支持 NPU 生图");
      if (Build.VERSION.SDK_INT < 26) throw new IOException("App 内生图需要 Android 8.0 或更高版本");
      boolean arm64=false;for(String abi:Build.SUPPORTED_ABIS)if("arm64-v8a".equals(abi)){arm64=true;break;}
      if(!arm64)throw new IOException("App 内生图仅支持 64 位 ARM 设备");
      if (model == null || !model.isFile() || model.length() <= 0L) throw new IOException("本地生图模型不存在");
      String lower = model.getName().toLowerCase(Locale.ROOT);
      if (!(lower.endsWith(".gguf") || lower.endsWith(".safetensors") || lower.endsWith(".ckpt")))
        throw new IOException("App 内生图仅支持 GGUF、SafeTensors 和 CKPT 模型");

      int safeWidth = normalizeSize(width), safeHeight = normalizeSize(height);
      int safeSteps = Math.max(2, Math.min(40, steps));
      float safeCfg = Math.max(1f, Math.min(20f, cfgScale));
      int safeThreads = Math.max(1, Math.min(8, threads));
      LocalLlamaEngine.release();
      StableDiffusionBridge bridge = StableDiffusionBridge.INSTANCE;
      if (!loaded || !model.getAbsolutePath().equals(loadedPath)) {
        if (loaded) bridge.release();
        loaded = bridge.initModel(model.getAbsolutePath(), safeThreads);
        if (!loaded) throw new IOException("本地生图模型加载失败，可能是格式不兼容或设备内存不足");
        loadedPath = model.getAbsolutePath();
      }
      byte[] rgba = bridge.txt2img(prompt, negativePrompt == null ? "" : negativePrompt,
          safeWidth, safeHeight, safeSteps, safeCfg, seed);
      int expected = safeWidth * safeHeight * 4;
      if (rgba == null || rgba.length != expected) throw new IOException("本地生图失败，模型没有返回有效图片");

      int[] pixels = new int[safeWidth * safeHeight];
      for (int source = 0, target = 0; target < pixels.length; source += 4, target++) {
        int red = rgba[source] & 255, green = rgba[source + 1] & 255;
        int blue = rgba[source + 2] & 255, alpha = rgba[source + 3] & 255;
        pixels[target] = alpha << 24 | red << 16 | green << 8 | blue;
      }
      Bitmap bitmap = Bitmap.createBitmap(pixels, safeWidth, safeHeight, Bitmap.Config.ARGB_8888);
      File target = new File(context.getFilesDir(), "generated_" + UUID.randomUUID() + ".png");
      try (FileOutputStream output = new FileOutputStream(target)) {
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException("无法保存生成的图片");
        output.getFD().sync();
      } finally {
        bitmap.recycle();
      }
      return target.getAbsolutePath();
    }
  }

  static void release() {
    synchronized (LOCK) {
      if (loaded) try { StableDiffusionBridge.INSTANCE.release(); } catch (Throwable ignored) {}
      loaded = false;
      loadedPath = "";
    }
  }

  private static int normalizeSize(int value) {
    int clamped = Math.max(256, Math.min(512, value));
    return Math.max(256, clamped / 64 * 64);
  }
}
