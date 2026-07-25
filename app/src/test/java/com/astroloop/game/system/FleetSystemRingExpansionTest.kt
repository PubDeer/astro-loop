package com.astroloop.game.system

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetSystemRingExpansionTest {
    @Test
    fun `inner ring stays at base radius before expansion`() {
        val t = 0f
        val eased = 1f - (1f - t) * (1f - t)
        val result = FleetSystem.INNER_RADIUS + (250f - FleetSystem.INNER_RADIUS) * eased
        assertEquals(FleetSystem.INNER_RADIUS, result, 0.001f)
    }

    @Test
    fun `inner ring reaches 250 at t=1`() {
        val t = 1f
        val eased = 1f - (1f - t) * (1f - t)
        val result = FleetSystem.INNER_RADIUS + (250f - FleetSystem.INNER_RADIUS) * eased
        assertEquals(250f, result, 0.001f)
    }
}
