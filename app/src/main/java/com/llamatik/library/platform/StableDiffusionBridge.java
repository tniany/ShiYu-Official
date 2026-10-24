package com.llamatik.library.platform;

/** Minimal Java surface for stable-diffusion.cpp in the bundled JNI library. */
public final class StableDiffusionBridge {
  public static final StableDiffusionBridge INSTANCE = new StableDiffusionBridge();

  static { System.loadLibrary("llama_jni"); }

  private StableDiffusionBridge() {}

  public native boolean initModel(String modelPath, int threads);
  public native byte[] txt2img(String prompt, String negativePrompt, int width, int height,
      int steps, float cfgScale, long seed);
  public native void release();
}
