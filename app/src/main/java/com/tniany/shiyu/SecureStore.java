package com.tniany.shiyu;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

final class SecureStore {
  private static final String ALIAS = "shiyu_api_key";
  private static final String VALUE = "api_key_encrypted";
  private final SharedPreferences prefs;

  SecureStore(Context context) {
    prefs = context.getSharedPreferences("shiyu_secure", Context.MODE_PRIVATE);
  }

  void put(String value) throws Exception {
    putNamed(VALUE, value);
  }

  void putNamed(String name, String value) throws Exception {
    String encoded = encrypt(value, key());
    prefs.edit().putString(name, encoded).apply();
  }

  private static String encrypt(String value, SecretKey secretKey) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
    return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
        + "."
        + Base64.encodeToString(encrypted, Base64.NO_WRAP);
  }

  String get() {
    return getNamed(VALUE);
  }

  String getNamed(String name) {
    try {
      String raw = prefs.getString(name, "");
      if (raw.isEmpty()) return "";
      String[] parts = raw.split("\\.", 2);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key(),
          new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
      return new String(
          cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    } catch (Exception ignored) {
      return "";
    }
  }

  void clear() {
    prefs.edit().clear().apply();
  }

  JSONObject exportPlain() throws Exception {
    JSONObject result = new JSONObject();
    for (String name : prefs.getAll().keySet()) result.put(name, getNamed(name));
    return result;
  }

  void importPlain(JSONObject values) throws Exception {
    SecretKey secretKey = key();
    Map<String, String> encoded = new LinkedHashMap<>();
    java.util.Iterator<String> names = values.keys();
    while (names.hasNext()) {
      String name = names.next();
      encoded.put(name, encrypt(values.optString(name), secretKey));
    }
    SharedPreferences.Editor editor = prefs.edit().clear();
    for (Map.Entry<String, String> entry : encoded.entrySet())
      editor.putString(entry.getKey(), entry.getValue());
    if (!editor.commit()) throw new IllegalStateException("无法写入加密设置");
  }

  private SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (store.containsAlias(ALIAS))
      return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
    KeyGenerator generator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
    generator.init(
        new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build());
    return generator.generateKey();
  }
}
