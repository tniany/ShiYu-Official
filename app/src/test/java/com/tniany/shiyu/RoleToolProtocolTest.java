package com.tniany.shiyu;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Test;

public class RoleToolProtocolTest {
  @Test public void nestedJsonAndQuotedBracketsAreParsedAsOneCall() throws Exception {
    String json = "{\"items\":[{\"name\":\"a}]b\",\"nested\":{\"ok\":true}}],\"quote\":\"a\\\"b\"}";
    RoleToolProtocol.Parsed result = RoleToolProtocol.extractMcp("before [调用工具：server/tool|" + json + "] after");
    assertEquals(1, result.calls.size());
    assertEquals("server", result.calls.get(0).serverId);
    assertEquals("tool", result.calls.get(0).toolName);
    assertEquals("a}]b", new JSONObject(result.calls.get(0).arguments)
        .getJSONArray("items").getJSONObject(0).getString("name"));
    assertEquals("before  after", result.text);
  }

  @Test public void everyStreamingPrefixHidesProtocolButKeepsDialogue() {
    String command = "[调用工具：server/tool|{\"q\":\"a]b\",\"values\":[1,2]}]";
    for (int i = 1; i <= command.length(); i++)
      assertEquals("hello", RoleToolProtocol.visibleText("hello " + command.substring(0, i), true));
    assertEquals("hello  again", RoleToolProtocol.visibleText("hello " + command + " again", true));
  }

  @Test public void optionalArgumentsAndFullwidthSeparatorWork() {
    RoleToolProtocol.Parsed result = RoleToolProtocol.extractMcp("[调用工具:server/time] [调用工具：server/search｜{}]");
    assertEquals(2, result.calls.size());
    assertEquals("{}", result.calls.get(0).arguments);
    assertEquals("{}", result.calls.get(1).arguments);
  }

  @Test public void callLimitDoesNotLeakRemainingCommands() {
    RoleToolProtocol.Parsed result = RoleToolProtocol.extractMcp("[调用工具:s/t|{}]".repeat(6));
    assertEquals(4, result.calls.size());
    assertEquals("", result.text);
  }

  @Test public void emptyCodeFenceIsNotVisible() {
    assertEquals("hello", RoleToolProtocol.visibleText("hello\n```json\n[调用工具:s/t|{}]\n```", true));
    assertEquals("hello", RoleToolProtocol.extractMcp("hello\n```json\n[调用工具:s/t|{}]\n```").text);
  }

  @Test public void ordinaryBracketedRoleplayStaysVisible() {
    assertEquals("[smiles] hello", RoleToolProtocol.visibleText("[smiles] hello", true));
    assertEquals("[调用工具箱里的东西]", RoleToolProtocol.visibleText("[调用工具箱里的东西]", true));
  }

  @Test public void finalReplyPreservesSegmentsAndSilenceDecision() {
    assertEquals("one<SEG>two", RoleToolProtocol.visibleText("one<SEG>two", false));
    assertEquals("[NO_REPLY]", RoleToolProtocol.visibleText("[NO_REPLY]", false));
    assertEquals("hello", RoleToolProtocol.visibleText("hello [调用工具:s/t|{", false));
  }
}
