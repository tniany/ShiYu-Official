package com.tniany.shiyu;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.util.function.BooleanSupplier;

final class SseEventReader implements Closeable {
  private final BufferedReader reader;
  private boolean firstLine = true;

  SseEventReader(Reader source) {
    reader = new BufferedReader(source);
  }

  String readEvent(BooleanSupplier active) throws IOException {
    StringBuilder data = new StringBuilder();
    while (active.getAsBoolean() && !Thread.currentThread().isInterrupted()) {
      String line = reader.readLine();
      if (line == null) return eventData(data);
      if (firstLine) {
        firstLine = false;
        if (line.startsWith("\uFEFF")) line = line.substring(1);
      }
      if (line.isEmpty()) {
        if (data.length() > 0) return eventData(data);
        continue;
      }
      if (line.equals("data")) {
        data.append('\n');
      } else if (line.startsWith("data:")) {
        String value = line.substring(5);
        if (value.startsWith(" ")) value = value.substring(1);
        data.append(value).append('\n');
      }
      if (data.length() > 1_048_576) throw new IOException("流式响应单条事件过大");
    }
    throw new InterruptedIOException("stopped");
  }

  private String eventData(StringBuilder data) {
    return data.length() == 0 ? null : data.substring(0, data.length() - 1);
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
