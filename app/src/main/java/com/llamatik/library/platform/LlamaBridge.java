package com.llamatik.library.platform;

/** Minimal Java surface for the bundled Llamatik JNI library. */
public final class LlamaBridge {
  public static final LlamaBridge INSTANCE = new LlamaBridge();

  static { System.loadLibrary("llama_jni"); }

  private LlamaBridge() {}

  public native boolean initGenerateModel(String modelPath);
  public native String generateWithContext(String systemPrompt, String contextBlock, String userPrompt);
  public native void nativeUpdateGenerationParams(float temperature, int maxTokens, float topP,
      int topK, float repeatPenalty, int contextLength, int numThreads, boolean useMmap,
      boolean flashAttention, int batchSize, int gpuLayers);
  public native void shutdown();
  public native void nativeCancelGenerate();
}
