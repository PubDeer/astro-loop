package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GameConfigTest {

    @Test
    fun `100k ship price shows no decimal`() {
        assertEquals("100K", GameConfig.formatYen(100_000))
    }

    @Test
    fun `85k ship price shows no decimal`() {
        assertEquals("¥85K", GameConfig.formatYen(85_000))
    }

    @Test
    fun `30k ship price shows no decimal`() {
        assertEquals("¥30K", GameConfig.formatYen(30_000))
    }

    @Test
    fun `non-round K value keeps decimal`() {
        assertEquals("¥12.3K", GameConfig.formatYen(12_300))
    }

    @Test
    fun `small ship price stays as raw number`() {
        assertEquals("¥3000", GameConfig.formatYen(3_000))
    }

    @Test
    fun `5k raw number stays as raw number`() {
        assertEquals("¥5000", GameConfig.formatYen(5_000))
    }

    @Test
    fun `zero stays as raw number`() {
        assertEquals("¥0", GameConfig.formatYen(0))
    }

    @Test
    fun `non-round value above 100k keeps decimal and no yen prefix`() {
        assertEquals("150.5K", GameConfig.formatYen(150_500))
    }

    @Test
    fun `boss charged shot one-shots any player`() {
        assertEquals(true, GameConfig.BOSS_CHARGED_SHOT_DAMAGE >= 10000f)
    }
}
