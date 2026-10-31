package com.tniany.shiyu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarketplaceSettingPolicyTest {
    @Test
    public void parsesReviewedThemeSettingsAndDropsInvalidDefinitions() throws Exception {
        JSONObject baseline = new JSONObject()
            .put("experimental_glass_opacity", 84)
            .put("chat_font_size", 15)
            .put("experimental_glass_ui", true)
            .put("chat_user_bubble_color", "#FF8F4962");

        JSONArray raw = new JSONArray("""
            [
              {"key":"glass_opacity","type":"range","label":"玻璃强度","description":"调整玻璃强度。","target":"experimental_glass_opacity","min":20,"max":100,"step":1,"default":84},
              {"key":"font_size","type":"range","label":"聊天字号","description":"调整聊天字号。","target":"chat_font_size","min":12,"max":20,"step":1,"default":15},
              {"key":"glass_ui","type":"switch","label":"玻璃界面","description":"控制玻璃材质。","target":"experimental_glass_ui","default":true},
              {"key":"user_bubble_color","type":"select","label":"用户气泡颜色","description":"选择气泡颜色。","target":"chat_user_bubble_color","default":"#FF8F4962","options":[{"value":"#FF8F4962","label":"莓红"},{"value":"#FF2C7D72","label":"青绿"}]}
            ]
            """);

        List<MarketSetting> settings = MarketplaceSettingPolicy.INSTANCE.parseDefinitions(raw, null, baseline);

        assertEquals(4, settings.size());
        assertEquals(MarketSettingType.RANGE, settings.get(0).getType());
        assertEquals(84, settings.get(0).getDefaultValue());
        assertEquals(MarketSettingType.SELECT, settings.get(3).getType());
        assertEquals(2, settings.get(3).getOptions().size());

        JSONArray invalid = new JSONArray("""
            [
              {"key":"unknown","type":"range","label":"未知字段","target":"theme.custom_css","min":0,"max":10,"step":1,"default":5},
              {"key":"bad_default","type":"range","label":"错误默认值","target":"chat_font_size","min":12,"max":20,"step":1,"default":14},
              {"key":"script","type":"script","label":"运行脚本","target":"compose_theme","default":2}
            ]
            """);
        assertTrue(MarketplaceSettingPolicy.INSTANCE.parseDefinitions(invalid, null, baseline).isEmpty());
    }

    @Test
    public void duplicateKeysTargetsAndOutOfRangeDefinitionsAreRejected() throws Exception {
        JSONObject baseline = new JSONObject()
            .put("experimental_glass_opacity", 84)
            .put("chat_font_size", 15);

        JSONArray duplicateKey = new JSONArray("""
            [
              {"key":"font_size","type":"range","label":"聊天字号","target":"chat_font_size","min":12,"max":20,"step":1,"default":15},
              {"key":"font_size","type":"range","label":"另一字号","target":"experimental_glass_opacity","min":20,"max":100,"step":1,"default":84}
            ]
            """);
        assertEquals(1, MarketplaceSettingPolicy.INSTANCE.parseDefinitions(duplicateKey, null, baseline).size());

        JSONArray duplicateTarget = new JSONArray("""
            [
              {"key":"font_size","type":"range","label":"聊天字号","target":"chat_font_size","min":12,"max":20,"step":1,"default":15},
              {"key":"font_size_again","type":"range","label":"重复目标","target":"chat_font_size","min":12,"max":20,"step":1,"default":15}
            ]
            """);
        assertEquals(1, MarketplaceSettingPolicy.INSTANCE.parseDefinitions(duplicateTarget, null, baseline).size());

        JSONArray outOfRange = new JSONArray("""
            [{"key":"font_size","type":"range","label":"聊天字号","target":"chat_font_size","min":1,"max":40,"step":1,"default":15}]
            """);
        assertTrue(MarketplaceSettingPolicy.INSTANCE.parseDefinitions(outOfRange, null, baseline).isEmpty());
    }

    @Test
    public void invalidStoredOverridesFallBackToReviewedDefaults() throws Exception {
        JSONObject baseline = new JSONObject()
            .put("experimental_glass_opacity", 84)
            .put("chat_font_size", 15)
            .put("experimental_glass_ui", true);

        JSONArray raw = new JSONArray("""
            [
              {"key":"glass_opacity","type":"range","label":"玻璃强度","target":"experimental_glass_opacity","min":20,"max":100,"step":1,"default":84},
              {"key":"font_size","type":"range","label":"聊天字号","target":"chat_font_size","min":12,"max":20,"step":1,"default":15},
              {"key":"glass_ui","type":"switch","label":"玻璃界面","target":"experimental_glass_ui","default":true}
            ]
            """);
        List<MarketSetting> settings = MarketplaceSettingPolicy.INSTANCE.parseDefinitions(raw, null, baseline);

        JSONObject stored = new JSONObject()
            .put("glass_opacity", 62)
            .put("font_size", 99)
            .put("glass_ui", "false");

        JSONObject resolved = MarketplaceSettingPolicy.INSTANCE.resolvedValues(settings, baseline, stored);

        assertEquals(62, resolved.getInt("experimental_glass_opacity"));
        assertEquals(15, resolved.getInt("chat_font_size"));
        assertTrue(resolved.getBoolean("experimental_glass_ui"));
    }

    @Test
    public void repositoryPersistsOverridesAndClearingRestoresDefaults() throws Exception {
        MemoryPreferences preferences = new MemoryPreferences();
        MarketplaceRepository repository = new MarketplaceRepository(null, preferences);
        JSONObject baseline = new JSONObject()
            .put("experimental_glass_opacity", 84)
            .put("chat_font_size", 15);
        JSONArray raw = new JSONArray("""
            [
              {"key":"glass_opacity","type":"range","label":"玻璃强度","target":"experimental_glass_opacity","min":20,"max":100,"step":1,"default":84},
              {"key":"font_size","type":"range","label":"聊天字号","target":"chat_font_size","min":12,"max":20,"step":1,"default":15}
            ]
            """);
        List<MarketSetting> settings = MarketplaceSettingPolicy.INSTANCE.parseDefinitions(raw, null, baseline);
        Map<String, Object> changed = Map.of("glass_opacity", 61, "font_size", 17);

        repository.saveSettingValues("official.test-theme", settings, changed);
        Map<String, Object> saved = repository.settingValues("official.test-theme", settings);
        assertEquals(61, saved.get("glass_opacity"));
        assertEquals(17, saved.get("font_size"));

        repository.clearSettingValues("official.test-theme");
        Map<String, Object> restored = repository.settingValues("official.test-theme", settings);
        assertEquals(84, restored.get("glass_opacity"));
        assertEquals(15, restored.get("font_size"));
    }

    @Test
    public void themeOnlyImportIgnoresCommunityPlugins() throws Exception {
        MemoryPreferences preferences = new MemoryPreferences();
        MarketplaceRepository repository = new MarketplaceRepository(null, preferences);
        String raw = """
            {
              "format": "shiyu_market_catalog",
              "source": {"id": "local", "name": "本地导入"},
              "themes": [
                {"id": "community.local-theme", "name": "本地主题", "values": {"compose_theme": 2}}
              ],
              "plugins": [
                {"id": "community.local-tools", "name": "本地工具", "runtimePrefs": {"web_search_enabled": true}}
              ]
            }
            """;

        MarketImportResult result = repository.importLocalTheme(raw);

        assertEquals(1, result.getThemes().size());
        assertTrue(result.getPlugins().isEmpty());
        assertEquals(1, repository.localThemes().size());
        assertTrue(repository.localPlugins().isEmpty());
    }

    @Test
    public void pluginSettingsStayInsideApprovedRuntimeTargets() throws Exception {
        JSONObject communityBaseline = new JSONObject()
            .put("agent_music_enabled", true)
            .put("web_search_enabled", true);
        JSONArray communitySettings = new JSONArray("""
            [
              {"key":"music_enabled","type":"switch","label":"允许点歌","target":"agent_music_enabled","default":true},
              {"key":"search_enabled","type":"switch","label":"允许搜索","target":"web_search_enabled","default":true},
              {"key":"travel_enabled","type":"switch","label":"允许出行","target":"agent_travel_enabled","default":true}
            ]
            """);

        List<MarketSetting> parsed = MarketplaceSettingPolicy.INSTANCE.parseDefinitions(
            communitySettings,
            "community.test-tools",
            communityBaseline
        );
        assertEquals(2, parsed.size());

        JSONObject groupBaseline = new JSONObject()
            .put("default_group_reply_decision", true)
            .put("default_group_probability", 70);
        JSONArray groupSettings = new JSONArray("""
            [
              {"key":"reply_decision","type":"switch","label":"自主接话","target":"default_group_reply_decision","default":true},
              {"key":"probability","type":"range","label":"接话概率","target":"default_group_probability","min":0,"max":100,"step":1,"default":70}
            ]
            """);
        List<MarketSetting> official = MarketplaceSettingPolicy.INSTANCE.parseDefinitions(
            groupSettings,
            "official.group-director",
            groupBaseline
        );

        assertEquals(2, official.size());
        assertFalse(MarketplaceSettingPolicy.INSTANCE.parseDefinitions(
            groupSettings,
            "community.test-tools",
            groupBaseline
        ).size() == 2);
    }

    private static final class MemoryPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return Collections.unmodifiableMap(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            if (value instanceof Set<?>) {
                @SuppressWarnings("unchecked")
                Set<String> strings = (Set<String>) value;
                return strings;
            }
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).intValue() : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).longValue() : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).floatValue() : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new MemoryEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        private final class MemoryEditor implements Editor {
            private final Map<String, Object> updates = new HashMap<>();
            private final Set<String> removals = new java.util.HashSet<>();
            private boolean clear;

            @Override
            public Editor putString(String key, String value) {
                updates.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                updates.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                updates.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                updates.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                updates.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                updates.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                removals.add(key);
                return this;
            }

            @Override
            public Editor clear() {
                clear = true;
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                if (clear) values.clear();
                removals.forEach(values::remove);
                values.putAll(updates);
            }
        }
    }
}
