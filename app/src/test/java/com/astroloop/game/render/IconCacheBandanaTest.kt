package com.astroloop.game.render

import org.junit.Assert.*
import org.junit.Test

class IconCacheBandanaTest {

    @Test
    fun bandanaFilenameStripsPilotPrefixAndSuffixesBandana() {
        assertEquals("portrait_medic_bandana", IconCache.bandanaPortraitFilename("pilot_medic"))
        assertEquals("portrait_unit7_bandana", IconCache.bandanaPortraitFilename("pilot_unit7"))
        assertEquals("portrait_astro_bandana", IconCache.bandanaPortraitFilename("pilot_astro"))
    }

    @Test
    fun bandanaPortraitMissingBeforePreloadReturnsNull() {
        // No preload(): the cache is empty, so the accessor must null-fallback cleanly.
        assertNull(IconCache.getBandanaPortrait("pilot_medic"))
    }
}
