package com.astroloop.game.render

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Radio portrait selection with a bandana pilot (Astro Loop active-pilot rule).
 * Preloads the real asset set so the bandana PNGs resolve to distinct bitmaps.
 */
@RunWith(RobolectricTestRunner::class)
class IconCacheRadioBandanaTest {

    @Before
    fun preload() {
        IconCache.preload(RuntimeEnvironment.getApplication())
    }

    @After
    fun teardown() {
        IconCache.recycle()
    }

    @Test
    fun `bandana pilot gets bandana portrait on own callsign`() {
        val banded = IconCache.getPortraitByCallsign("DASH", isCorrupted = false, bandanaPilotId = "pilot_dash")
        assertNotNull(banded)
        assertSame(IconCache.getBandanaPortrait("pilot_dash"), banded)
        assertNotSame(IconCache.getPortrait("pilot_dash"), banded)
    }

    @Test
    fun `other speakers keep normal portraits`() {
        val medic = IconCache.getPortraitByCallsign("MEDIC", isCorrupted = false, bandanaPilotId = "pilot_dash")
        assertSame(IconCache.getPortrait("pilot_medic"), medic)
        val tb26 = IconCache.getPortraitByCallsign("TB-26", isCorrupted = false, bandanaPilotId = "pilot_dash")
        assertSame(IconCache.getPortraitByCallsign("TB-26"), tb26)
    }

    @Test
    fun `corrupted speaker overrides bandana`() {
        val corrupted = IconCache.getPortraitByCallsign("DASH", isCorrupted = true, bandanaPilotId = "pilot_dash")
        assertSame(IconCache.getCorruptedPortrait("pilot_dash"), corrupted)
    }

    @Test
    fun `no bandana pilot leaves selection unchanged`() {
        val normal = IconCache.getPortraitByCallsign("DASH", isCorrupted = false, bandanaPilotId = null)
        assertSame(IconCache.getPortrait("pilot_dash"), normal)
    }
}
