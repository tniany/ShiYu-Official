package com.tniany.shiyu;

import static org.junit.Assert.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ChatRequestMessagesTest {
  @Test public void imageOnlyIsOneNonemptyMultimodalMessage() throws Exception {
    JSONArray messages = new JSONArray();
    ChatRequestMessages.append(messages, "user",
        ChatRequestMessages.content("", "data:image/png;base64,abc"));
    assertEquals(1, messages.length());
    JSONArray content = messages.getJSONObject(0).getJSONArray("content");
    assertEquals(1, content.length());
    assertEquals("image_url", content.getJSONObject(0).getString("type"));
  }

  @Test public void imageCaptionAndAuxiliaryDescriptionAreNotDuplicated() throws Exception {
    JSONArray messages = new JSONArray();
    ChatRequestMessages.append(messages, "user", ChatRequestMessages.content("caption", "https://example.com/a.png"));
    ChatRequestMessages.append(messages, "user", ChatRequestMessages.content("visual description", ""));
    assertEquals(2, messages.length());
    assertEquals(2, messages.getJSONObject(0).getJSONArray("content").length());
    assertEquals("visual description", messages.getJSONObject(1).getString("content"));
  }

  @Test public void nullWhitespaceAndEmptyPartsAreNotSent() throws Exception {
    JSONArray messages = new JSONArray();
    ChatRequestMessages.append(messages, "user", null);
    ChatRequestMessages.append(messages, "user", " \n\t");
    ChatRequestMessages.append(messages, "user", new JSONArray());
    assertEquals(0, messages.length());
  }

  @Test public void eventsAndFailedPlaceholdersDoNotConsumeHistoryWindow() {
    List<MainActivity.Msg> messages = new ArrayList<>();
    MainActivity.Msg user = new MainActivity.Msg("user", "question");
    messages.add(user);
    messages.add(new MainActivity.Msg("assistant", "answer"));
    for (int i = 0; i < 10; i++) {
      MainActivity.Msg event = new MainActivity.Msg("system", "tool");
      event.messageType = "tool_event";
      messages.add(event);
    }
    MainActivity.Msg failed = new MainActivity.Msg("assistant", "error");
    failed.failed = true;
    messages.add(failed);
    MainActivity.Msg hidden = new MainActivity.Msg("user", "blocked");
    hidden.delivered = false;
    messages.add(hidden);
    messages.add(new MainActivity.Msg("assistant", ""));
    List<MainActivity.Msg> recent = ChatRequestMessages.recent(messages, 2);
    assertEquals(2, recent.size());
    assertSame(user, recent.get(0));
  }

  @Test public void immersiveBurstIsKeptEvenWithSmallMessageLimit() {
    List<MainActivity.Msg> messages = List.of(new MainActivity.Msg("assistant", "older"),
        new MainActivity.Msg("user", "one"), new MainActivity.Msg("user", "two"),
        new MainActivity.Msg("user", "three"));
    assertEquals(3, ChatRequestMessages.recent(messages, 0).size());
  }

  @Test public void budgetDropsOlderTurnButKeepsUserAndTrailingInstructions() throws Exception {
    JSONArray messages = new JSONArray();
    ChatRequestMessages.append(messages, "system", "persona");
    ChatRequestMessages.append(messages, "user", "old".repeat(2000));
    ChatRequestMessages.append(messages, "assistant", "old reply");
    ChatRequestMessages.append(messages, "user", "latest question");
    ChatRequestMessages.append(messages, "system", "post history");
    ChatRequestMessages.trimToBudget(messages, 1024);
    assertEquals(3, messages.length());
    assertEquals("latest question", messages.getJSONObject(1).getString("content"));
    assertEquals("post history", messages.getJSONObject(2).getString("content"));
  }

  @Test public void oversizedPersonaFailsWithoutErasingLatestQuestion() throws Exception {
    JSONArray messages = new JSONArray();
    ChatRequestMessages.append(messages, "system", "persona".repeat(2000));
    ChatRequestMessages.append(messages, "user", "latest question");
    ChatRequestMessages.append(messages, "system", "post history");
    assertThrows(IOException.class, () -> ChatRequestMessages.trimToBudget(messages, 1024));
    assertEquals("latest question", messages.getJSONObject(1).getString("content"));
  }

  @Test public void systemOnlyRequestIsRejected() throws Exception {
    JSONArray messages = new JSONArray().put(new JSONObject().put("role", "system").put("content", "persona"));
    assertThrows(IOException.class, () -> ChatRequestMessages.trimToBudget(messages, 32768));
  }

  @Test public void invalidContextIsNotRetried() {
    assertFalse(GenerationRetry.shouldRetry(new ChatRequestMessages.InvalidContextException("invalid")));
  }
}
