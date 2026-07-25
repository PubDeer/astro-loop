package com.astroloop.game.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BandanaPersistenceTest {

    private lateinit var p: PersistenceManager

    @Before
    fun setup() {
        p = PersistenceManager(ApplicationProvider.getApplicationContext())
        p.resetAllProgress()
    }

    @Test
    fun earnedBandanasRoundTrip() {
        assertEquals(0, p.getBandanaCount())
        assertFalse(p.hasBandana("pilot_dash"))
        p.addBandana("pilot_dash")
        assertTrue(p.hasBandana("pilot_dash"))
        assertEquals(1, p.getBandanaCount())
        p.addBandana("pilot_dash") // idempotent (set semantics)
        assertEquals(1, p.getBandanaCount())
        p.addBandana("pilot_medic")
        assertEquals(2, p.getBandanaCount())
    }

    @Test
    fun pendingBandanaRoundTrip() {
        assertNull(p.getPendingBandanaPilot())
        p.setPendingBandanaPilot("pilot_havoc")
        assertEquals("pilot_havoc", p.getPendingBandanaPilot())
        p.clearPendingBandanaPilot()
        assertNull(p.getPendingBandanaPilot())
    }

    @Test
    fun awaitingConvergenceRoundTrip() {
        assertFalse(p.isAwaitingConvergence())
        p.setAwaitingConvergence(true)
        assertTrue(p.isAwaitingConvergence())
        p.setAwaitingConvergence(false)
        assertFalse(p.isAwaitingConvergence())
    }

    @Test
    fun resetAllProgressClearsBandanaState() {
        p.addBandana("pilot_dash")
        p.setPendingBandanaPilot("pilot_dash")
        p.setAwaitingConvergence(true)
        p.resetAllProgress()
        assertEquals(0, p.getBandanaCount())
        assertNull(p.getPendingBandanaPilot())
        assertFalse(p.isAwaitingConvergence())
    }
}
