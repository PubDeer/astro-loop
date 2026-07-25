package com.astroloop.game.system

import com.astroloop.game.entity.Boss
import com.astroloop.game.weapon.weapons.EnergySaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FleetSystemRingTest {
    @Test
    fun `inner ring radius is 140`() {
        assertEquals(140f, FleetSystem.INNER_RADIUS, 0f)
    }

    @Test
    fun `L1 fleet saw clips the shield ring but never the boss hull`() {
        val saw = EnergySaw().also { it.level = 1 }
        // Inner ship faces the boss; its single L1 disc points straight in.
        val leadingEdge = FleetSystem.INNER_RADIUS - saw.reach - saw.discRadius
        val shieldRingRadius = Boss.FLEET_SHIELD_RING_RADIUS  // 45f visible barrier
        // leadingEdge (40f) sits ~5f inside the ring — just enough to graze it for spark feedback
        assertTrue("saw must clear the boss hull (${Boss.BOSS_SIZE}f)", leadingEdge > Boss.BOSS_SIZE)
        assertTrue("saw must reach into the shield ring (${shieldRingRadius}f)", leadingEdge < shieldRingRadius)
    }
}
