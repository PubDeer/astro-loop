package com.astroloop.game.entity

import org.junit.Assert.*
import org.junit.Test

class BossShieldDeflectTest {

    @Test
    fun `shield deflect radius matches the rendered aura ring`() {
        assertEquals(60f, Boss.SHIELD_DEFLECT_RADIUS, 0.001f)
    }

    @Test
    fun `a shot inside the ring but outside the hull deflects`() {
        val projRadius = 3f
        val dist = 45f
        assertFalse(dist < projRadius + Boss.BOSS_SIZE)
        assertTrue(dist < projRadius + Boss.SHIELD_DEFLECT_RADIUS)
    }

    @Test
    fun `spawnShieldSparks adds a colored spark burst`() {
        val boss = Boss()
        boss.spawnShieldSparks(100f, 50f, 1f, 0f, 0xFF00FF00.toInt())
        assertTrue("expected a burst of sparks", boss.shieldSparks.size >= 4)
        assertEquals(0xFF00FF00.toInt(), boss.shieldSparks.first().color)
    }

    @Test
    fun `updateShieldSparks expires sparks past their lifetime`() {
        val boss = Boss()
        boss.spawnShieldSparks(0f, 0f, 1f, 0f, 0xFFFFFFFF.toInt())
        boss.updateShieldSparks(1f)
        assertTrue("sparks should have expired", boss.shieldSparks.isEmpty())
    }
}
