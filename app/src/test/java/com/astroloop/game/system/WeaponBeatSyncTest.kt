package com.astroloop.game.system

import com.astroloop.game.core.BeatClock
import org.junit.Assert.*
import org.junit.Test

class WeaponBeatSyncTest {

    private val clock = BeatClock(150f)

    @Test
    fun `pulse cannon first shot delay at mid-beat`() {
        clock.start(0L)
        val delay = clock.msUntilNextSubdivision(400L, 200L)
        assertEquals(200L, delay)
    }

    @Test
    fun `pulse cannon no delay when on beat`() {
        clock.start(0L)
        val delay = clock.msUntilNextSubdivision(400L, 400L)
        assertEquals(0L, delay)
    }

    @Test
    fun `needle gun first shot delay`() {
        clock.start(0L)
        val delay = clock.msUntilNextSubdivision(200L, 150L)
        assertEquals(50L, delay)
    }

    @Test
    fun `ion orbiters delay with 3200ms subdivision`() {
        clock.start(0L)
        val delay = clock.msUntilNextSubdivision(3200L, 3000L)
        assertEquals(200L, delay)
    }

    @Test
    fun `max delay for quarter note is under 400ms`() {
        clock.start(0L)
        val delay = clock.msUntilNextSubdivision(400L, 1L)
        assertEquals(399L, delay)
    }

    @Test
    fun `delay converts to seconds for cooldown timer`() {
        clock.start(0L)
        val delayMs = clock.msUntilNextSubdivision(400L, 200L)
        val delaySeconds = delayMs / 1000f
        assertEquals(0.2f, delaySeconds, 0.001f)
    }

    // Verifies BeatClock arithmetic at the exact subdivMs values WeaponSystem will compute;
    // WeaponSystem integration is covered by device playtest
    @Test
    fun `needle gun effective subdivision halved during revenge`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // During Revenge, real fire interval is 125ms; 60ms elapsed → next at 65ms
        val delay = clock.msUntilNextSubdivision(125L, 60L)
        assertEquals(65L, delay)
    }

    @Test
    fun `needle gun re-syncs on every shot at base cooldown`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // Fired 1ms late at 251ms; next 250ms subdivision is 249ms away
        val delay = clock.msUntilNextSubdivision(250L, 251L)
        assertEquals(249L, delay)
    }
}
