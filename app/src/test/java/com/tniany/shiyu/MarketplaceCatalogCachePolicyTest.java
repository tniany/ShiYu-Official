package com.tniany.shiyu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarketplaceCatalogCachePolicyTest {
    @Test
    public void stampTracksCatalogAndAppRevision() {
        String first = MarketplaceCatalogCachePolicy.INSTANCE.stamp("{\"version\":1}", 61, "0.9.5");
        String same = MarketplaceCatalogCachePolicy.INSTANCE.stamp("{\"version\":1}", 61, "0.9.5");
        String changedCatalog = MarketplaceCatalogCachePolicy.INSTANCE.stamp("{\"version\":2}", 61, "0.9.5");
        String changedApp = MarketplaceCatalogCachePolicy.INSTANCE.stamp("{\"version\":1}", 62, "0.9.6");

        assertEquals(first, same);
        assertNotEquals(first, changedCatalog);
        assertNotEquals(first, changedApp);
    }

    @Test
    public void onlyStampedNonEmptyCachesAreReusable() {
        String stamp = MarketplaceCatalogCachePolicy.INSTANCE.stamp("{\"version\":1}", 61, "0.9.5");

        assertFalse(MarketplaceCatalogCachePolicy.INSTANCE.isCurrent("", stamp, stamp));
        assertFalse(MarketplaceCatalogCachePolicy.INSTANCE.isCurrent("cached", "", stamp));
        assertFalse(MarketplaceCatalogCachePolicy.INSTANCE.isCurrent("cached", "old", stamp));
        assertTrue(MarketplaceCatalogCachePolicy.INSTANCE.isCurrent("cached", stamp, stamp));
    }
}
