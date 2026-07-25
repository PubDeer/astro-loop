package com.astroloop.game.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * The reckoning death beat: instead of freezing where it died, the boss glides to the
 * frozen view's centre and plants itself there (stunned) while the player flees off-screen.
 */
class BossCenterApproachTest {

    private fun distTo(boss: Boss, x: Float, y: Float): Float {
        val dx = boss.position.x - x
        val dy = boss.position.y - y
        return sqrt(dx * dx + dy * dy)
    }

    private fun makeBoss(bx: Float, by: Float): Pair<Boss, Ship> {
        val ship = Ship()
        ship.position.set(0f, 0f)   // player elsewhere; target is passed explicitly
        val boss = Boss()
        boss.initialize(bx, by, ship)
        return boss to ship
    }

    @Test
    fun `glides toward the given centre, not the player`() {
        val (boss, _) = makeBoss(500f, 0f)
        val startDist = distTo(boss, 100f, 100f)
        boss.startCenterApproach(100f, 100f)
        repeat(10) { boss.update(1f / 60f) }
        assertTrue("boss should move toward the centre target", distTo(boss, 100f, 100f) < startDist)
        assertFalse("still travelling — not planted yet", boss.isStunned)
    }

    @Test
    fun `arrives at centre and plants (stuns) within a bounded time`() {
        val (boss, _) = makeBoss(500f, 0f)
        boss.startCenterApproach(100f, 100f)
        var t = 0f
        val dt = 1f / 60f
        while (t < 3f && !boss.isStunned) {
            boss.update(dt)
            t += dt
        }
        assertTrue("boss must plant within the retreat window", boss.isStunned)
        assertTrue("planted at the centre target", distTo(boss, 100f, 100f) < 5f)
    }

    @Test
    fun `stays planted at the centre after arriving`() {
        val (boss, _) = makeBoss(400f, 0f)
        boss.startCenterApproach(0f, 0f)
        repeat(600) { boss.update(1f / 60f) }   // long past arrival
        assertTrue(boss.isStunned)
        assertTrue("does not drift off the centre once planted", distTo(boss, 0f, 0f) < 5f)
    }

    @Test
    fun `does not want to fire while centering`() {
        val (boss, _) = makeBoss(500f, 0f)
        boss.railCooldown = 0f   // would otherwise arm immediately
        boss.startCenterApproach(0f, 0f)
        repeat(20) {
            boss.update(1f / 60f)
            assertFalse("no railgun intent during the centre glide", boss.wantsToFire)
        }
    }
}
