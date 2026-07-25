package com.astroloop.game.render

import org.junit.Assert.*
import org.junit.Test

/**
 * Mirrors the wrapText logic from UpgradeSelectionRenderer.
 * Uses character count instead of Paint.measureText so it runs without Android.
 */
class WrapTextTest {

    // Mirror of UpgradeSelectionRenderer.wrapText (maxWidth = maxChars for testing)
    private fun wrapText(text: String, maxChars: Int): List<String> {
        val lines = mutableListOf<String>()
        for (segment in text.split('\n')) {
            val words = segment.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (testLine.length <= maxChars) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
        }
        return lines
    }

    @Test fun `newline forces line break regardless of width`() {
        assertEquals(
            listOf("Regenerates health", "over time"),
            wrapText("Regenerates health\nover time", 50)
        )
    }

    @Test fun `two newlines produce three lines`() {
        assertEquals(
            listOf("A", "B", "C"),
            wrapText("A\nB\nC", 50)
        )
    }

    @Test fun `no newline word-wraps normally`() {
        assertEquals(
            listOf("hello", "world"),
            wrapText("hello world", 10)
        )
    }

    @Test fun `no newline short string stays single line`() {
        assertEquals(listOf("Regen on enemy kill"), wrapText("Regen on enemy kill", 50))
    }

    @Test fun `newline segment is itself word-wrapped if too long`() {
        assertEquals(
            listOf("hello", "world", "foo"),
            wrapText("hello world\nfoo", 10)
        )
    }

    @Test fun `empty string returns empty list`() {
        assertEquals(emptyList<String>(), wrapText("", 50))
    }
}
