package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RushStateTest {

    @Test
    fun `boss rush machine defaults to idle`() {
        val s = GameState()
        assertEquals(0, s.bossRushPhase)
        assertEquals(0f, s.bossRushTimer, 0f)
    }

    @Test
    fun `reset clears the boss rush machine`() {
        val s = GameState()
        s.bossRushPhase = 2
        s.bossRushTimer = 1.5f
        s.reset()
        assertEquals(0, s.bossRushPhase)
        assertEquals(0f, s.bossRushTimer, 0f)
    }

    @Test
    fun `corruption rush machine defaults to idle`() {
        val s = GameState()
        assertEquals(0, s.corruptionRushPhase)
        assertEquals(0f, s.corruptionRushTimer, 0f)
        assertEquals(0f, s.corruptionRushSpeed, 0f)
    }

    @Test
    fun `reset clears the corruption rush machine`() {
        val s = GameState()
        s.corruptionRushPhase = 3
        s.corruptionRushTimer = 2f
        s.corruptionRushSpeed = 900f
        s.reset()
        assertEquals(0, s.corruptionRushPhase)
        assertEquals(0f, s.corruptionRushTimer, 0f)
        assertEquals(0f, s.corruptionRushSpeed, 0f)
    }
}
