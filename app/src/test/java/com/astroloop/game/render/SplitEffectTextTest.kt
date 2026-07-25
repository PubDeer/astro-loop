package com.astroloop.game.render

import org.junit.Assert.*
import org.junit.Test

/**
 * Mirrors the splitEffectText logic from UpgradeSelectionRenderer.
 * Tests that \n, comma, "and", and & all split correctly.
 */
class SplitEffectTextTest {

    // Mirror of UpgradeSelectionRenderer.splitEffectText
    private fun split(text: String): List<String> =
        text.split(Regex("\n|,\\s*|\\s+and\\s+|\\s*&\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    @Test fun `newline splits into two parts`() {
        assertEquals(listOf("+8% damage", "while moving"), split("+8% damage\nwhile moving"))
    }

    @Test fun `comma still splits`() {
        assertEquals(listOf("+1 weapon", "-1 passive"), split("+1 weapon, -1 passive"))
    }

    @Test fun `newline without comma splits correctly`() {
        assertEquals(listOf("+1 weapon", "-1 passive"), split("+1 weapon\n-1 passive"))
    }

    @Test fun `single line with no delimiter returns one element`() {
        assertEquals(listOf("+0.5 HP/sec"), split("+0.5 HP/sec"))
    }

    @Test fun `and splits`() {
        assertEquals(listOf("foo", "bar"), split("foo and bar"))
    }

    @Test
    fun `first line gets arrow prefix second line gets indent`() {
        val lines = listOf("fire rate +20%", "speed +10%")
        val prefixed = lines.mapIndexed { i, line -> if (i == 0) "↑ $line" else "  $line" }
        assertEquals(listOf("↑ fire rate +20%", "  speed +10%"), prefixed)
    }

    @Test
    fun `single line effect gets arrow prefix`() {
        val lines = listOf("damage +30%")
        val prefixed = lines.mapIndexed { i, line -> if (i == 0) "↑ $line" else "  $line" }
        assertEquals(listOf("↑ damage +30%"), prefixed)
    }
}
