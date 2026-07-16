package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class WeixinLinkSecureStore {
  private static final String ALIAS = "shiyu_weixin_link";
  private static final String VALUE = "bot_token";
  private final SharedPreferences prefs;

  WeixinLinkSecureStore(Context context) {
    prefs = context.getSharedPreferences("shiyu_weixin_link_secure", Context.MODE_PRIVATE);
  }

  void put(String value) throws Exception {
    put(VALUE, value);
  }

  void put(String name, String value) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key());
    byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
    prefs.edit().putString(name,
        Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
            + Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply();
  }

  String get() {
    return get(VALUE);
  }

  String get(String name) {
    try {
      String raw = prefs.getString(name, "");
      if (raw.isEmpty()) return "";
      String[] parts = raw.split("\\.", 2);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key(),
          new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
      return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
          StandardCharsets.UTF_8);
    } catch (Exception ignored) {
      return "";
    }
  }

  void clear() {
    prefs.edit().clear().apply();
  }

  void clear(String name) {
    prefs.edit().remove(name).apply();
  }

  private SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (store.containsAlias(ALIAS))
      return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
    KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
    generator.init(new KeyGenParameterSpec.Builder(ALIAS,
        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
    return generator.generateKey();
  }
}
