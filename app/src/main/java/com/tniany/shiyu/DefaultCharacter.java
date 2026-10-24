package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

final class DefaultCharacter {
  private static final String INSTALLED = "default_lin_wanxing_v1";
  private static final String ID = "shiyu-default-lin-wanxing";

  private DefaultCharacter() {}

  static void install(Context context, AppDb db, SharedPreferences prefs,
      List<MainActivity.Card> cards, JSONObject state) throws Exception {
    if (prefs.getBoolean(INSTALLED, false)) return;
    for (MainActivity.Card existing : cards) {
      if (ID.equals(existing.id) || "林晚星".equals(existing.name)) {
        prefs.edit().putBoolean(INSTALLED, true).apply();
        return;
      }
    }
    MainActivity.Card card = new MainActivity.Card();
    card.id = ID;
    card.name = "林晚星";
    card.desc = "会把晚风写进信里的女孩";
    card.persona = "温柔、敏锐，喜欢在夜里写信。她记得你说过的每一件小事，也愿意把今天的心情慢慢讲给你听。";
    card.greeting = "你来啦。今天的风很轻，我刚好写到一半，想把这一页留给你。";
    card.tags = new JSONArray().put("治愈").put("日常").put("写信").toString();
    card.creator = "时遇";
    card.characterVersion = "1.0";
    card.systemPrompt = "用自然、温柔的日常聊天语气回应，认真倾听，不机械说教。只记得真实对话和已提供的记忆，不编造共同经历。工具不可用时如实说明；涉及付费、出行订单或分享隐私时先征求用户同意。";
    File avatar = new File(context.getFilesDir(), "default-lin-wanxing.png");
    try (InputStream input = context.getAssets().open("characters/lin-wanxing.png");
        FileOutputStream output = new FileOutputStream(avatar)) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
    }
    card.avatar = avatar.getAbsolutePath();
    JSONObject updated = new JSONObject(state.toString());
    JSONArray values = new JSONArray();
    for (MainActivity.Card existing : cards) values.put(existing.json());
    values.put(card.json());
    updated.put("cards", values);
    db.saveState(updated.toString());
    cards.add(card);
    prefs.edit().putBoolean(INSTALLED, true).apply();
  }
}
