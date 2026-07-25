package com.astroloop.game.entity

import com.astroloop.game.core.BossRush
import com.astroloop.game.core.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class BossRushMotionTest {

    private fun gap(boss: Boss, ship: Ship): Float {
        val dx = boss.position.x - ship.position.x
        val dy = boss.position.y - ship.position.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun makePair(distance: Float): Pair<Boss, Ship> {
        val ship = Ship()
        ship.position.set(0f, 0f)
        val boss = Boss()
        boss.initialize(distance, 0f, ship)
        return boss to ship
    }

    @Test
    fun `rush closes a huge gap to EMP range within the close-time bound`() {
        val (boss, ship) = makePair(10_000f)
        boss.startRush(BossRush.rushSpeed(ship.speed, gap(boss, ship), ship.speed))
        var t = 0f
        val dt = 1f / 60f
        // ease-in costs ~half the ease duration of ground; 1s of headroom is plenty
        while (t < GameConfig.BOSS_RUSH_MAX_CLOSE_TIME + 1f && !BossRush.hasArrived(gap(boss, ship))) {
            boss.update(dt)
            t += dt
        }
        assertTrue("boss must reach EMP range in time", BossRush.hasArrived(gap(boss, ship)))
    }

    @Test
    fun `railgun stays quiet for the whole rush`() {
        val (boss, ship) = makePair(2_000f)
        boss.railCooldown = 0f  // would fire immediately if not rushing
        boss.startRush(BossRush.rushSpeed(ship.speed, gap(boss, ship), ship.speed))
        repeat(120) {
            boss.update(1f / 60f)
            assertFalse("no railgun during the rush", boss.wantsToFire)
        }
    }

    @Test
    fun `brake bleeds speed off within the brake window`() {
        val (boss, ship) = makePair(2_000f)
        boss.startRush(BossRush.rushSpeed(ship.speed, gap(boss, ship), ship.speed))
        repeat(60) { boss.update(1f / 60f) }  // reach full rush speed
        boss.startRushBrake()
        var t = 0f
        while (t < GameConfig.BOSS_RUSH_BRAKE_DURATION) {
            boss.update(1f / 60f)
            t += 1f / 60f
        }
        val speed = sqrt(boss.velocity.x * boss.velocity.x + boss.velocity.y * boss.velocity.y)
        assertTrue("residual speed must be a small fraction after the brake", speed < 150f)
    }

    @Test
    fun `stun ends the rush and reset clears the burn`() {
        val (boss, _) = makePair(1_000f)
        boss.startRush(700f)
        repeat(30) { boss.update(1f / 60f) }
        assertTrue("burn emits during the rush", boss.reentryBurn.hasContent())
        boss.stun()
        assertFalse(boss.isRushing)
        boss.reset()
        assertFalse(boss.reentryBurn.hasContent())
        assertEquals(0f, boss.rushSpeed, 0f)
    }
}
