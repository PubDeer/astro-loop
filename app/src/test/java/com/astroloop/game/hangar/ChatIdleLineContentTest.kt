// app/src/test/java/com/astroloop/game/hangar/ChatIdleLineContentTest.kt
package com.astroloop.game.hangar

import org.junit.Assert.assertTrue
import org.junit.Test

class ChatIdleLineContentTest {

    /**
     * Idle lines fire in every story stage, so the bartender must be authored as
     * "TB-26" — outside the astro loop Tobar is dead, and inside it the display
     * swap in ChatSystem renders TB-26 as Tobar automatically.
     */
    @Test
    fun `idle lines never hardcode Tobar`() {
        val offenders = ChatSystem().idleLines.flatMap { (speaker, lines) ->
            lines.filter { it.contains("tobar", ignoreCase = true) }
                .map { "$speaker: $it" }
        }
        assertTrue("Author these as TB-26 (astro loop swaps the name): $offenders",
            offenders.isEmpty())
    }
}
