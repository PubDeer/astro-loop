package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BossRushTest {

    @Test
    fun `rush speed is at least twice the chaser speed`() {
        // 2×400 = 800 beats the 700 floor and the gap term (300/3 + 350 = 450)
        assertEquals(800f, BossRush.rushSpeed(400f, 300f, 350f), 0.001f)
    }

    @Test
    fun `rush speed never drops below the floor`() {
        // slow chaser, tiny gap: 2×300=600 and 100/3+300≈333 both lose to the 700 floor
        assertEquals(GameConfig.BOSS_RUSH_SPEED_FLOOR, BossRush.rushSpeed(300f, 100f, 300f), 0.001f)
    }

    @Test
    fun `distant target is caught within the max close time`() {
        // 10,000px gap: speed must cover gap/3s on top of the target's own flee speed
        val speed = BossRush.rushSpeed(300f, 10_000f, 300f)
        assertEquals(10_000f / GameConfig.BOSS_RUSH_MAX_CLOSE_TIME + 300f, speed, 0.001f)
        assertTrue(
            "net closing rate must cover the gap in the close-time bound",
            speed - 300f >= 10_000f / GameConfig.BOSS_RUSH_MAX_CLOSE_TIME
        )
    }

    @Test
    fun `arrival triggers at the trigger distance and not before`() {
        assertTrue(BossRush.hasArrived(GameConfig.BOSS_RUSH_TRIGGER_DISTANCE))
        assertTrue(BossRush.hasArrived(150f))
        assertFalse(BossRush.hasArrived(GameConfig.BOSS_RUSH_TRIGGER_DISTANCE + 1f))
    }

    @Test
    fun `ease-in ramps from zero to one over the ease duration`() {
        assertEquals(0f, BossRush.easeIn(0f), 0.001f)
        assertEquals(0.5f, BossRush.easeIn(GameConfig.BOSS_RUSH_EASE_DURATION / 2f), 0.001f)
        assertEquals(1f, BossRush.easeIn(GameConfig.BOSS_RUSH_EASE_DURATION), 0.001f)
        assertEquals(1f, BossRush.easeIn(99f), 0.001f)
    }
}
