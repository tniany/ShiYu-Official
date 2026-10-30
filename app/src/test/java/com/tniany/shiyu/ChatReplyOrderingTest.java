package com.tniany.shiyu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ChatReplyOrderingTest {
  private static MainActivity.Card card(String id, String name) {
    MainActivity.Card value = new MainActivity.Card();
    value.id = id;
    value.name = name;
    return value;
  }

  @Test public void mentionsFollowTheOrderWrittenByTheUser() {
    List<MainActivity.Card> members = List.of(
        card("a", "阿青"), card("b", "小"), card("c", "小明"));
    List<MainActivity.Card> result =
        ChatReplyOrdering.mentioned("@小明 然后 @阿青，最后 @小", members);
    assertEquals(List.of("c", "a", "b"),
        List.of(result.get(0).id, result.get(1).id, result.get(2).id));
  }

  @Test public void longerOverlappingNamesWinAndDuplicateMentionsRunOnce() {
    List<MainActivity.Card> members = List.of(card("short", "小"), card("long", "小明"));
    List<MainActivity.Card> result =
        ChatReplyOrdering.mentioned("@小明 你好，@小明 再问一次", members);
    assertEquals(1, result.size());
    assertEquals("long", result.get(0).id);
  }

  @Test public void emptyConcurrentReplyPlaceholderIsRemoved() {
    ArrayList<MainActivity.Msg> messages = new ArrayList<>();
    messages.add(new MainActivity.Msg("user", "hello"));
    MainActivity.Msg placeholder = new MainActivity.Msg("assistant", "");
    placeholder.loading = false;
    messages.add(placeholder);

    assertTrue(ChatReplyOrdering.removeEmptyPlaceholder(messages, placeholder));
    assertEquals(1, messages.size());
  }

  @Test public void mediaOrFailedPlaceholderIsKept() {
    ArrayList<MainActivity.Msg> messages = new ArrayList<>();
    MainActivity.Msg sticker = new MainActivity.Msg("assistant", "");
    sticker.image = "/tmp/sticker.png";
    messages.add(sticker);
    assertFalse(ChatReplyOrdering.removeEmptyPlaceholder(messages, sticker));

    MainActivity.Msg failed = new MainActivity.Msg("assistant", "");
    failed.failed = true;
    messages.add(failed);
    assertFalse(ChatReplyOrdering.removeEmptyPlaceholder(messages, failed));
  }
}
