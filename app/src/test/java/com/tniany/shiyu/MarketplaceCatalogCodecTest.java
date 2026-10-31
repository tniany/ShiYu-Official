package com.tniany.shiyu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class MarketplaceCatalogCodecTest {
    @Test
    public void parsesOfficialCatalogAndKeepsKnownPluginsOnly() throws Exception {
        String raw = "{"
            + "\"format\":\"shiyu_market_catalog\","
            + "\"version\":3,"
            + "\"source\":{\"id\":\"official\",\"name\":\"官方\"},"
            + "\"themes\":[{"
            + "\"id\":\"official.test-theme\",\"name\":\"测试主题\",\"author\":\"时遇官方\",\"sourceUrl\":\"https://github.com/tniany/test-theme\",\"sourceRef\":\"main\","
            + "\"palette\":[\"#112233\",\"not-a-color\"],"
            + "\"values\":{\"compose_theme\":2,\"experimental_glass_ui\":true}"
            + "}],"
            + "\"plugins\":[{"
            + "\"id\":\"official.time-tools\",\"name\":\"时间工具\",\"sourceUrl\":\"https://github.com/tniany/time-tools\",\"sourceRef\":\"release\","
            + "\"runtimePrefs\":{\"agent_time_enabled\":true,\"agent_music_enabled\":true}"
            + "},{"
            + "\"id\":\"third.party-plugin\",\"name\":\"第三方\","
            + "\"runtimePrefs\":{\"web_search_enabled\":true}"
            + "}]"
            + "}";

        MarketCatalog catalog = MarketplaceCatalogCodec.INSTANCE.parse(raw, true);

        assertEquals(3, catalog.getVersion());
        assertEquals("official", catalog.getSource().getId());
        assertTrue(catalog.getSource().getBuiltIn());
        assertEquals(1, catalog.getThemes().size());
        assertEquals(1, catalog.getThemes().get(0).getPalette().size());
        assertEquals("https://github.com/tniany/test-theme", catalog.getThemes().get(0).getSourceUrl());
        assertEquals("main", catalog.getThemes().get(0).getSourceRef());
        assertEquals(1, catalog.getPlugins().size());
        MarketPlugin plugin = catalog.getPlugins().get(0);
        assertEquals("official.time-tools", plugin.getId());
        assertEquals("https://github.com/tniany/time-tools", plugin.getSourceUrl());
        assertEquals("release", plugin.getSourceRef());
        assertTrue(plugin.getRuntimePrefs().getBoolean("agent_time_enabled"));
        assertFalse(plugin.getRuntimePrefs().has("agent_music_enabled"));
    }

    @Test
    public void rejectsInvalidIdentifiersAndDuplicateIds() {
        String raw = "{"
            + "\"format\":\"shiyu_market_catalog\","
            + "\"source\":{\"id\":\"official\"},"
            + "\"themes\":["
            + "{\"id\":\"ok.theme-one\",\"name\":\"第一套\",\"values\":{\"compose_theme\":1}},"
            + "{\"id\":\"ok.theme-one\",\"name\":\"重复主题\",\"values\":{\"compose_theme\":2}},"
            + "{\"id\":\"x\",\"name\":\"无效主题\",\"values\":{\"compose_theme\":0}}"
            + "],"
            + "\"plugins\":["
            + "{\"id\":\"official.time-tools\",\"name\":\"时间\",\"runtimePrefs\":{\"agent_time_enabled\":true}},"
            + "{\"id\":\"official.time-tools\",\"name\":\"重复时间\",\"runtimePrefs\":{\"agent_time_enabled\":false}}"
            + "]"
            + "}";

        MarketCatalog catalog = MarketplaceCatalogCodec.INSTANCE.parse(raw, false);

        assertEquals(1, catalog.getThemes().size());
        assertEquals("第一套", catalog.getThemes().get(0).getName());
        assertEquals(1, catalog.getPlugins().size());
        assertEquals("时间", catalog.getPlugins().get(0).getName());
    }

    @Test
    public void rejectsOtherPackageFormats() {
        String raw = "{\"format\":\"shiyu_appearance\",\"source\":{\"id\":\"official\"}}";

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> MarketplaceCatalogCodec.INSTANCE.parse(raw, false)
        );

        assertTrue(error.getMessage().contains("时遇市场目录"));
    }

    @Test
    public void pluginPolicyRejectsUnknownPackagesAndWrongTypes() throws Exception {
        JSONObject runtime = new JSONObject()
            .put("web_search_enabled", "yes")
            .put("agent_music_enabled", true);

        assertNull(MarketplacePluginPolicy.INSTANCE.sanitize("third.party-plugin", runtime));
        assertNull(MarketplacePluginPolicy.INSTANCE.sanitize("official.live-web-search", runtime));

        JSONObject valid = MarketplacePluginPolicy.INSTANCE.sanitize(
            "official.live-web-search",
            new JSONObject().put("web_search_enabled", true).put("agent_music_enabled", false)
        );
        assertNotNull(valid);
        assertTrue(valid.getBoolean("web_search_enabled"));
        assertFalse(valid.has("agent_music_enabled"));
    }

    @Test
    public void localPackagesAcceptCommunityPluginsAndStripUnknownRuntimePrefs() throws Exception {
        String raw = """
            {
              "format": "shiyu_market_catalog",
              "source": {"id": "local"},
              "themes": [
                {"id": "official.keep-out", "name": "官方主题", "values": {"compose_theme": 1}},
                {"id": "community.test-theme", "name": "本地主题", "values": {"compose_theme": 2}}
              ],
              "plugins": [
                {"id": "community.test-tools", "name": "本地工具", "runtimePrefs": {
                  "agent_time_enabled": false,
                  "web_search_enabled": true,
                  "unapproved_runtime_flag": true
                }},
                {"id": "official.time-tools", "name": "官方时间", "runtimePrefs": {"agent_time_enabled": true}}
              ]
            }
            """;

        MarketCatalog parsed = MarketplaceCatalogCodec.INSTANCE.parse(raw, false);
        MarketImportResult imported = MarketplaceCatalogCodec.INSTANCE.localImportCandidates(parsed);

        assertEquals(1, imported.getThemes().size());
        assertEquals("community.test-theme", imported.getThemes().get(0).getId());
        assertEquals(1, imported.getPlugins().size());
        MarketPlugin plugin = imported.getPlugins().get(0);
        assertEquals("community.test-tools", plugin.getId());
        assertFalse(plugin.getRuntimePrefs().getBoolean("agent_time_enabled"));
        assertTrue(plugin.getRuntimePrefs().getBoolean("web_search_enabled"));
        assertFalse(plugin.getRuntimePrefs().has("unapproved_runtime_flag"));
    }
}
