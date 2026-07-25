package com.astroloop.game.core

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BandanaAwardTest {

    private lateinit var p: PersistenceManager

    @Before
    fun setup() {
        p = PersistenceManager(ApplicationProvider.getApplicationContext())
        p.resetAllProgress()
    }

    @Test
    fun belowThresholdAwardsNothing() {
        assertFalse(BandanaAward.maybeAward(p, "pilot_dash", 599f))
        assertFalse(p.hasBandana("pilot_dash"))
        assertNull(p.getPendingBandanaPilot())
    }

    @Test
    fun atThresholdAwardsAndSetsPending() {
        assertTrue(BandanaAward.maybeAward(p, "pilot_dash", 600f))
        assertTrue(p.hasBandana("pilot_dash"))
        assertEquals("pilot_dash", p.getPendingBandanaPilot())
        assertFalse(p.isAwaitingConvergence())
    }

    @Test
    fun alreadyOwnedDoesNotReaward() {
        p.addBandana("pilot_dash")
        assertFalse(BandanaAward.maybeAward(p, "pilot_dash", 900f))
        assertNull(p.getPendingBandanaPilot())
    }

    @Test
    fun twelfthBandanaSetsConvergence() {
        val ids = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
            "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
            "pilot_whiskers", "pilot_unit7", "pilot_havoc"
        )
        for (id in ids) p.addBandana(id)            // 11 already owned
        assertEquals(11, p.getBandanaCount())
        assertTrue(BandanaAward.maybeAward(p, "pilot_astro", 700f))  // the 12th
        assertEquals(12, p.getBandanaCount())
        assertTrue(p.isAwaitingConvergence())
    }
}
