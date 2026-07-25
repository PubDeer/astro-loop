package com.astroloop.game.render

import com.astroloop.game.core.GameConfig
import com.astroloop.game.data.CrystalFightLines
import org.junit.Assert.*
import org.junit.Test

class GhostShipLanceTest {

    private fun started(): GhostShipLance {
        val l = GhostShipLance()
        l.start(0f, 0f)
        return l
    }

    @Test fun ghostsAreTheSameSizeAsTheShip() {
        // 14f read as undersized next to the player ship; ghosts are the crew, not miniatures.
        // Tied to SHIP_BASE_SIZE rather than a literal so this fails if EITHER value drifts —
        // "same size as the ship" is the actual requirement, not "25".
        assertEquals(GameConfig.SHIP_BASE_SIZE, GhostShipLance.GHOST_SIZE, 0.001f)
    }

    @Test fun allTwelveArePresentAsWhiskersSpeaks() {
        // WHISKERS says "Let go, boss. We're ready." — the ring must be full when he speaks for
        // the crew. Read his time from the script rather than hardcoding it, so retiming him
        // can't silently falsify this test's premise.
        val whiskersAt = CrystalFightLines.ghostScript.first { it.first == "WHISKERS" }.third
        assertTrue("gather ${GhostShipLance.T_GATHERED} must complete by ${whiskersAt}s",
            GhostShipLance.T_GATHERED <= whiskersAt)
    }

    @Test fun startsGathering() {
        assertEquals(GhostShipLance.Stage.GATHER, started().stage)
    }

    @Test fun holdsAfterGatheringInsteadOfLancing() {
        // The old build fired the lance on a timer. It must now WAIT for Astro's "Go."
        val l = started()
        repeat(2000) { l.update(1f / 60f) }   // ~33s — way past any old T_LANCE
        assertEquals(GhostShipLance.Stage.HOLD, l.stage)
        assertFalse("nothing may burst before release", l.burstFired)
    }

    @Test fun gatheredReportsRingCompletion() {
        val l = started()
        assertFalse(l.gathered)
        repeat((GhostShipLance.T_GATHERED * 60).toInt() + 2) { l.update(1f / 60f) }
        assertTrue(l.gathered)
    }

    @Test fun releaseStartsTheLance() {
        val l = started()
        repeat(900) { l.update(1f / 60f) }
        l.release()
        assertEquals(GhostShipLance.Stage.RELEASED, l.update(1f / 60f))
    }

    @Test fun burstFiresOnceShortlyAfterRelease() {
        // Ghosts converge through the crystal: RING_RADIUS / LANCE_SPEED ~= 0.055s.
        val l = started()
        repeat(900) { l.update(1f / 60f) }
        l.release()
        assertFalse(l.burstFired)
        repeat(10) { l.update(1f / 60f) }   // ~0.17s
        assertTrue(l.burstFired)
    }

    @Test fun completesAfterTheReleaseHold() {
        val l = started()
        repeat(900) { l.update(1f / 60f) }
        l.release()
        repeat((GhostShipLance.DONE_AFTER_RELEASE * 60).toInt() + 5) { l.update(1f / 60f) }
        assertEquals(GhostShipLance.Stage.DONE, l.stage)
    }

    @Test fun releaseBeforeGatherIsIgnored() {
        // The script cannot fire "Go." early, but a defensive guard keeps the ring from
        // punching out half-formed if timings are ever retuned.
        val l = started()
        l.release()
        l.update(1f / 60f)
        assertEquals(GhostShipLance.Stage.GATHER, l.stage)
    }

    @Test fun ghostsTravelPastTheScreenEdge() {
        // The punch-through must LEAVE the arena, not dissolve in it. At 2200 px/s a ghost
        // clears a 1400px half-diagonal well inside the 2.5s hold.
        val travel = GhostShipLance.LANCE_SPEED * GhostShipLance.DONE_AFTER_RELEASE
        assertTrue("ghosts must clear the screen ($travel px)", travel > 1400f)
    }
}
