package com.tniany.shiyu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Lightweight GIF playback for Android 6 and 7, where AnimatedImageDrawable is unavailable. */
final class LegacyGifView extends View {
  private Movie movie;
  private long startedAt;
  private boolean fit = true;

  LegacyGifView(Context context) { this(context, null); }

  LegacyGifView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
  }

  void setGif(byte[] data, boolean fit) {
    this.fit = fit;
    movie = data == null ? null : Movie.decodeByteArray(data, 0, data.length);
    startedAt = SystemClock.uptimeMillis();
    invalidate();
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (movie == null || movie.width() <= 0 || movie.height() <= 0) return;
    int duration = movie.duration() <= 0 ? 1000 : movie.duration();
    movie.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
    float scaleX = getWidth() / (float) movie.width();
    float scaleY = getHeight() / (float) movie.height();
    float scale = fit ? Math.min(scaleX, scaleY) : Math.max(scaleX, scaleY);
    float left = (getWidth() - movie.width() * scale) / 2f;
    float top = (getHeight() - movie.height() * scale) / 2f;
    canvas.save();
    canvas.translate(left, top);
    canvas.scale(scale, scale);
    movie.draw(canvas, 0, 0);
    canvas.restore();
    postInvalidateOnAnimation();
  }
}
