package com.astroloop.game.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Reckoning hold-pattern: the boss is a Touhou stage boss adapted to free flight — a STABLE
 * emitter. It closes hard only beyond the leash (so the fight can't be outrun off-screen),
 * eases back out of point-blank range (bullets must never spawn on top of the player), and
 * otherwise drifts on a slow orbit so the spiral geometry stays readable. No CHARGING lunges,
 * no strafe jitter — the pattern is the fight, not the chase.
 */
class BossHoldPatternTest {

    private fun makeHoldingBoss(bx: Float, by: Float): Pair<Boss, Ship> {
        val ship = Ship()
        ship.position.set(0f, 0f)
        val boss = Boss()
        boss.initialize(bx, by, ship)
        boss.holdPattern = true
        return boss to ship
    }

    private fun dist(boss: Boss, ship: Ship): Float =
        hypot(boss.position.x - ship.position.x, boss.position.y - ship.position.y)

    @Test
    fun `closes on a player beyond the leash`() {
        val (boss, ship) = makeHoldingBoss(Boss.HOLD_LEASH + 800f, 0f)
        repeat(120) { boss.update(1f / 60f) }   // 2s
        assertTrue("boss must close from beyond the leash",
            dist(boss, ship) < Boss.HOLD_LEASH + 500f)
    }

    @Test
    fun `backs out of point-blank range`() {
        val (boss, ship) = makeHoldingBoss(150f, 0f)
        repeat(120) { boss.update(1f / 60f) }
        assertTrue("boss must ease back out toward the standoff — no point-blank emitter",
            dist(boss, ship) > 150f)
    }

    @Test
    fun `drifts slowly inside the band — no lunges`() {
        val (boss, ship) = makeHoldingBoss((Boss.HOLD_STANDOFF + Boss.HOLD_LEASH) / 2f, 0f)
        repeat(120) {
            boss.update(1f / 60f)
            val speed = hypot(boss.velocity.x, boss.velocity.y)
            assertTrue("hold drift must stay slow (was $speed px/s) — the pattern is the fight",
                speed <= Boss.HOLD_DRIFT_SPEED * 1.5f)
        }
        val d = dist(boss, ship)
        assertTrue("drift must keep the boss inside the band (was $d)",
            d > Boss.HOLD_STANDOFF - 60f && d < Boss.HOLD_LEASH + 60f)
    }

    @Test
    fun `never wants to fire while holding`() {
        val (boss, _) = makeHoldingBoss(400f, 0f)
        boss.railCooldown = 0f   // would otherwise arm immediately
        repeat(30) {
            boss.update(1f / 60f)
            assertFalse("railgun stays quiet — the fight system owns all bullets", boss.wantsToFire)
        }
    }

    @Test
    fun `initialize clears a stale hold flag`() {
        val ship = Ship()
        val boss = Boss()
        boss.holdPattern = true
        boss.initialize(0f, 0f, ship)
        assertFalse(boss.holdPattern)
    }
}
