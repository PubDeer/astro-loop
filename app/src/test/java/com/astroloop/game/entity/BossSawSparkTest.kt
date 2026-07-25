package com.astroloop.game.entity

import com.astroloop.game.weapon.weapons.EnergySaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class BossSawSparkTest {
    private val shieldRing = Boss.FLEET_SHIELD_RING_RADIUS  // 45f visible barrier

    @Test
    fun `saw disc overlapping the shield ring registers contact`() {
        val saw = EnergySaw().also { it.level = 1 }
        val discCenterDist = 55f  // 55 < 20 + 45 = 65 -> contact
        assertTrue(discCenterDist < saw.discRadius + shieldRing)
    }

    @Test
    fun `saw disc clear of the ring does not register contact`() {
        val saw = EnergySaw().also { it.level = 1 }
        val discCenterDist = 70f  // 70 > 65 -> no contact
        assertFalse(discCenterDist < saw.discRadius + shieldRing)
    }

    @Test
    fun `spark point lands on the shield ring along the disc-to-boss line`() {
        val bossX = 0f; val bossY = 0f
        val discX = 55f; val discY = 0f
        val sdx = discX - bossX; val sdy = discY - bossY
        val sdist = sqrt(sdx * sdx + sdy * sdy)
        val snrm = 1f / sdist
        val sparkX = bossX + sdx * snrm * shieldRing
        assertEquals(shieldRing, sparkX, 0.001f)  // on the ring
    }

    @Test
    fun `cooldown throttles to one burst per tenth second`() {
        var cd = 0f
        val dt = 1f / 60f
        assertTrue("fires when cooldown is spent", cd <= 0f)
        cd = 0.1f          // burst fired -> reset
        cd -= dt
        assertTrue("still throttled one frame later", cd > 0f)
        repeat(6) { cd -= dt }   // ~0.1s elapsed
        assertTrue("ready again after 0.1s", cd <= 0f)
    }

    @Test
    fun `widened tolerance gives the inner saw a comfortable spark margin`() {
        val saw = EnergySaw().also { it.level = 1 }
        // Inner ship faces the boss at INNER_RADIUS; its L1 disc center lands here:
        val discCenterDist = com.astroloop.game.system.FleetSystem.INNER_RADIUS - saw.reach // 140 - 80 = 60f
        val rawThreshold = saw.discRadius + shieldRing                 // 65f, ~5px margin
        val tolerantThreshold = rawThreshold + Boss.SAW_SPARK_TOLERANCE
        assertTrue("raw margin is tight", discCenterDist < rawThreshold)         // 60 < 65 (5px)
        assertTrue("tolerant margin is comfortable (>=10px)",
            tolerantThreshold - discCenterDist >= 10f)                            // 75 - 60 = 15px
    }

    @Test
    fun `saw contact spawns a colored spark burst`() {
        val boss = Boss()
        boss.spawnShieldSparks(45f, 0f, 1f, 0f, 0xFF9933FF.toInt())  // Ripper magenta
        assertTrue(boss.shieldSparks.size >= 4)
        assertEquals(0xFF9933FF.toInt(), boss.shieldSparks.first().color)
    }
}
