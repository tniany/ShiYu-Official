package com.tniany.shiyu;

import static org.junit.Assert.*;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;

public class TogetherListeningTest {
  private MainActivity.Msg song(long id, String scene) throws Exception {
    MainActivity.Msg message = new MainActivity.Msg("user", "song");
    message.messageType = "music";
    message.meta = new JSONObject().put("songId", id).put("scene", scene).toString();
    return message;
  }

  @Test public void reenteringSameSongAfterChatDoesNotSendAnotherCard() throws Exception {
    assertFalse(TogetherListening.shouldAnnounce(
        List.of(song(42, "together_listen"), new MainActivity.Msg("user", "hello")), 42));
  }

  @Test public void differentExplicitSongCanBeShared() throws Exception {
    assertTrue(TogetherListening.shouldAnnounce(List.of(song(42, "together_listen")), 43));
  }

  @Test public void ordinarySharesDoNotCountAsTogetherInvites() throws Exception {
    assertTrue(TogetherListening.shouldAnnounce(List.of(song(42, "")), 42));
  }

  @Test public void removedRoleDoesNotReceiveCurrentListeningContext() throws Exception {
    JSONObject state = new JSONObject().put("status", "playing").put("songId", 42)
        .put("title", "song").put("artist", "artist")
        .put("chatId", "chat-a").put("roleId", "role-a");
    assertFalse(TogetherListening.context(state, "chat-a", "role-a").isEmpty());
    assertEquals("", TogetherListening.context(state, "chat-b", "role-a"));
    assertEquals("", TogetherListening.context(state, "chat-a", "role-b"));
    assertEquals("", TogetherListening.context(state, "", "role-a"));
  }

  @Test public void endedOrFailedPlaybackDoesNotClaimToBePlaying() throws Exception {
    for (String status : List.of("idle", "loading", "error")) {
      JSONObject state = new JSONObject().put("status", status).put("songId", 42);
      assertEquals("", TogetherListening.context(state, "chat-a", "role-a"));
    }
  }

  @Test public void legacyStateWithoutOwnerCannotLeakIntoConversation() throws Exception {
    JSONObject state = new JSONObject().put("status", "playing").put("songId", 42);
    assertEquals("", TogetherListening.context(state, "chat-a", "role-a"));
  }
}
