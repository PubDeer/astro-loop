package com.astroloop.game.system

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetSystemShieldHitTest {

    @Test
    fun `hit point lands on the ring along the shooter-to-boss line with outward normal`() {
        // Shooter 200px to the +x of the boss at origin, ring radius 45.
        val hit = FleetSystem.shieldRingHit(fromX = 200f, fromY = 0f, bossX = 0f, bossY = 0f, ringRadius = 45f)
        assertEquals(45f, hit[0], 0.001f)   // x on the ring
        assertEquals(0f, hit[1], 0.001f)    // y on the ring
        assertEquals(1f, hit[2], 0.001f)    // outward normal x
        assertEquals(0f, hit[3], 0.001f)    // outward normal y
    }

    @Test
    fun `hit point is finite when shooter sits on the boss`() {
        val hit = FleetSystem.shieldRingHit(0f, 0f, 0f, 0f, 45f)
        assertEquals(4, hit.size)
        for (v in hit) assertEquals(true, v.isFinite())
    }
}
