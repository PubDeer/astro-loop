package com.astroloop.game.core

import org.junit.Assert.*
import org.junit.Test

class BeatClockTest {

    @Test
    fun `beat interval at 120 BPM is 500ms`() {
        val clock = BeatClock(120f)
        assertEquals(500L, clock.beatIntervalMs)
    }

    @Test
    fun `subdivision interval for quarter note is one beat`() {
        val clock = BeatClock(120f)
        assertEquals(500L, clock.subdivisionMs(1f))
    }

    @Test
    fun `subdivision interval for 8th note is half a beat`() {
        val clock = BeatClock(120f)
        assertEquals(250L, clock.subdivisionMs(0.5f))
    }

    @Test
    fun `subdivision interval for 16th note is quarter beat`() {
        val clock = BeatClock(120f)
        assertEquals(125L, clock.subdivisionMs(0.25f))
    }

    @Test
    fun `subdivision interval for half note is two beats`() {
        val clock = BeatClock(120f)
        assertEquals(1000L, clock.subdivisionMs(2f))
    }

    @Test
    fun `delay to next subdivision from start is zero`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        assertEquals(0L, clock.msUntilNextSubdivision(500L, 0L))
    }

    @Test
    fun `delay to next quarter from mid-beat`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        assertEquals(300L, clock.msUntilNextSubdivision(500L, 200L))
    }

    @Test
    fun `delay to next 8th from just after an 8th`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        assertEquals(40L, clock.msUntilNextSubdivision(250L, 210L))
    }

    @Test
    fun `delay is zero when exactly on subdivision`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        assertEquals(0L, clock.msUntilNextSubdivision(500L, 1000L))
    }

    @Test
    fun `delay accounts for start offset`() {
        val clock = BeatClock(120f)
        clock.start(1000L)
        assertEquals(300L, clock.msUntilNextSubdivision(500L, 1200L))
    }

    @Test
    fun `elapsed beats calculated correctly`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        assertEquals(2.0f, clock.elapsedBeats(1000L), 0.001f)
    }

    @Test
    fun `subdivision for weapon cooldown 400ms is 0_8 beats`() {
        assertEquals(0.8f, BeatClock.cooldownToSubdivision(0.4f, 120f), 0.001f)
    }

    @Test
    fun `subdivision for weapon cooldown 200ms is 0_4 beats`() {
        assertEquals(0.4f, BeatClock.cooldownToSubdivision(0.2f, 120f), 0.001f)
    }

    @Test
    fun `subdivision for weapon cooldown 1200ms is 2_4 beats`() {
        assertEquals(2.4f, BeatClock.cooldownToSubdivision(1.2f, 120f), 0.001f)
    }

    @Test
    fun `msUntilNextSubdivision with phase offset snaps to offset position`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // Weapon with 1000ms cooldown and 500ms offset fires at: 500, 1500, 2500...
        assertEquals(500L, clock.msUntilNextSubdivision(1000L, 0L, 500L))
        assertEquals(0L, clock.msUntilNextSubdivision(1000L, 500L, 500L))
        assertEquals(500L, clock.msUntilNextSubdivision(1000L, 1000L, 500L))
        assertEquals(0L, clock.msUntilNextSubdivision(1000L, 1500L, 500L))
    }

    @Test
    fun `msUntilNextSubdivision with zero offset is unchanged`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        assertEquals(0L, clock.msUntilNextSubdivision(500L, 0L, 0L))
        assertEquals(250L, clock.msUntilNextSubdivision(500L, 250L, 0L))
    }

    @Test
    fun `msUntilNextSubdivision with offset before first fire`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // offset=250, cooldown=1000: fires at 250, 1250, 2250...
        assertEquals(250L, clock.msUntilNextSubdivision(1000L, 0L, 250L))
        assertEquals(100L, clock.msUntilNextSubdivision(1000L, 150L, 250L))
        assertEquals(0L, clock.msUntilNextSubdivision(1000L, 250L, 250L))
    }

    // ── gridAnchoredDelayMs: needle-family re-anchor at fire time ──────────

    @Test
    fun `grid anchor from just after a tick waits for the next tick only`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // Fired 6ms after the tick at 1000 (frame quantization): next tick is 1250.
        assertEquals(244L, clock.gridAnchoredDelayMs(250L, 1006L))
    }

    @Test
    fun `grid anchor exactly on a tick schedules a full subdivision`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // This tick's shot just fired — never 0, or the gun would double-fire.
        assertEquals(250L, clock.gridAnchoredDelayMs(250L, 1000L))
    }

    @Test
    fun `grid anchor just before a tick bumps to the following tick`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // A frame hitch pushed the shot to 1240 — 10ms before the tick at 1250.
        // Firing again at 1250 would machine-gun; schedule 1500 instead.
        assertEquals(260L, clock.gridAnchoredDelayMs(250L, 1240L))
    }

    @Test
    fun `grid anchor respects the phase offset`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // offset=100: ticks at 100, 350, 600... fired 5ms after the 350 tick.
        assertEquals(245L, clock.gridAnchoredDelayMs(250L, 355L, 100L))
    }

    @Test
    fun `grid-anchored fire loop hits every tick with none skipped`() {
        val clock = BeatClock(120f)
        clock.start(0L)
        // Simulate the needle gun at ~120fps: every shot lands a few ms after its
        // tick because cooldown expiry is only noticed on a frame boundary. The
        // old cooldown-then-resync scheme skipped every other tick (500ms feel).
        var now = 253L  // first shot, 3ms late
        val shots = mutableListOf<Long>()
        repeat(40) {
            shots += now
            now += clock.gridAnchoredDelayMs(250L, now) + (it % 8 + 1L)  // 1-8ms frame lateness
        }
        shots.zipWithNext { a, b -> b - a }.forEach { interval ->
            assertTrue(
                "interval $interval must be one subdivision ± a frame, never two",
                interval in 240L..262L
            )
        }
    }
}
