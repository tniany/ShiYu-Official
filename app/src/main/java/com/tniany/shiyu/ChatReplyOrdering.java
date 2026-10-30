package com.tniany.shiyu;

import java.util.ArrayList;
import java.util.List;

/** Deterministic group-chat mention and reply-slot ordering helpers. */
final class ChatReplyOrdering {
  private ChatReplyOrdering() {}

  static List<MainActivity.Card> mentioned(String body, List<MainActivity.Card> members) {
    ArrayList<MainActivity.Card> ordered = new ArrayList<>();
    if (body == null || body.isEmpty() || members == null || members.isEmpty()) return ordered;
    for (int index = 0; index < body.length(); index++) {
      if (body.charAt(index) != '@') continue;
      MainActivity.Card matched = null;
      for (MainActivity.Card member : members) {
        if (member == null || member.id == null || member.id.isEmpty()
            || member.name == null || member.name.isEmpty()) continue;
        if (!body.startsWith("@" + member.name, index)) continue;
        if (matched == null || member.name.length() > matched.name.length()) matched = member;
      }
      if (matched == null) continue;
      if (!ordered.contains(matched)) ordered.add(matched);
      index += matched.name.length();
    }
    return ordered;
  }

  static boolean removeEmptyPlaceholder(List<MainActivity.Msg> messages,
      MainActivity.Msg placeholder) {
    if (messages == null || placeholder == null || placeholder.failed || placeholder.loading) {
      return false;
    }
    if (!"assistant".equals(placeholder.role)) return false;
    if (!blank(placeholder.body) || !blank(placeholder.image) || !blank(placeholder.file)) {
      return false;
    }
    return messages.remove(placeholder);
  }

  private static boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
