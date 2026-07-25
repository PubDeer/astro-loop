package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BossChargeTest {

    @Test
    fun `BOSS_CHARGE_DURATION is 62 seconds`() {
        assertEquals(62f, GameConfig.BOSS_CHARGE_DURATION, 0.001f)
    }

    @Test
    fun `BOSS_EMP_CHARGE_THRESHOLD is 45 percent`() {
        assertEquals(0.45f, GameConfig.BOSS_EMP_CHARGE_THRESHOLD, 0.001f)
    }

    @Test
    fun `ease-out progress clamps to 1 when timer exceeds duration`() {
        val timer = 100f
        val raw = (timer / GameConfig.BOSS_CHARGE_DURATION).coerceAtMost(1f)
        val progress = 1f - (1f - raw) * (1f - raw)
        assertEquals(1f, progress, 0.001f)
    }

    @Test
    fun `ease-out progress at half duration is 0_75`() {
        val timer = GameConfig.BOSS_CHARGE_DURATION / 2f
        val raw = (timer / GameConfig.BOSS_CHARGE_DURATION).coerceAtMost(1f)
        val progress = 1f - (1f - raw) * (1f - raw)
        assertEquals(0.75f, progress, 0.001f)
    }

    @Test
    fun `ease-out progress at the ~47_5s ram beat is approximately 95 percent`() {
        // Retimed scene: EMP→fleet ~16.5s + chatter 5s + formation 4s + assault ~22s
        val timer = 47.5f
        val raw = (timer / GameConfig.BOSS_CHARGE_DURATION).coerceAtMost(1f)
        val progress = 1f - (1f - raw) * (1f - raw)
        assertEquals(0.945f, progress, 0.005f)
    }
}
