package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class DesertDefinitionsTest {

    @Test
    fun `all desert lines are 36 chars or under`() {
        for (line in DesertDefinitions.allLines) {
            assertTrue(
                "Desert line too long (${line.text.length} chars): \"${line.text}\"",
                line.text.length <= 36
            )
        }
    }

    @Test
    fun `phase1 has lines`() {
        assertTrue(DesertDefinitions.phase1Lines.isNotEmpty())
    }

    @Test
    fun `phase2 has lines`() {
        assertTrue(DesertDefinitions.phase2Lines.isNotEmpty())
    }

    @Test
    fun `good ending has lines`() {
        assertTrue(DesertDefinitions.goodEndingLines.isNotEmpty())
    }

    @Test
    fun `horror lines has lines`() {
        assertTrue(DesertDefinitions.horrorLines.isNotEmpty())
    }

    @Test
    fun `crystal lines has lines`() {
        assertTrue(DesertDefinitions.crystalLines.isNotEmpty())
    }

    @Test
    fun `crystal color matches the canonical crystal palette`() {
        assertEquals(com.astroloop.game.render.CrystalPalette.MID, DesertDefinitions.CRYSTAL_COLOR)
    }

    @Test
    fun `phase1 first contact response is 'Copy that Tobar'`() {
        val idx = DesertDefinitions.phase1Lines.indexOfFirst {
            it.trigger == DesertDefinitions.DesertTrigger.FIRST_ENEMIES
        }
        assertTrue("FIRST_ENEMIES line not found", idx >= 0)
        assertTrue("No line after FIRST_ENEMIES", idx + 1 < DesertDefinitions.phase1Lines.size)
        val response = DesertDefinitions.phase1Lines[idx + 1]
        assertEquals(DesertDefinitions.ASTRO, response.speaker)
        assertEquals("Copy that, Tobar.", response.text)
    }

    @Test
    fun `wall-hit line is spoken by Astro`() {
        val line = DesertDefinitions.horrorLines.first { it.text == "Can't push through." }
        assertEquals(DesertDefinitions.ASTRO, line.speaker)
        assertEquals(DesertDefinitions.ASTRO_COLOR, line.color)
    }

    @Test
    fun `you have your orders line interrupts`() {
        val line = DesertDefinitions.horrorLines
            .first { it.text == "You have your orders, Lieutenant." }
        assertTrue("Command line must interrupt", line.interrupt)
    }

    @Test
    fun `non-interrupt lines default to false`() {
        val line = DesertDefinitions.horrorLines.first { it.text == "Can't push through." }
        assertEquals(false, line.interrupt)
    }

    @Test
    fun `command outpost line nudges north and fits the HUD`() {
        val line = DesertDefinitions.phase1Lines.first { it.text.startsWith("Command") }
        assertEquals("Command: target, two clicks north.", line.text)
        assertTrue("Line must nudge north", line.text.contains("north"))
        assertTrue("Line too long: ${line.text.length}", line.text.length <= 36)
    }

    @Test
    fun `readings line waits for visible targets`() {
        val line = DesertDefinitions.phase2Lines.first { it.text == "Lieutenant... these readings." }
        assertEquals(DesertDefinitions.DesertTrigger.AMBIGUOUS_TARGET, line.trigger)
    }

    @Test
    fun `signatures line is gated on visibility not a raw timer`() {
        val line = DesertDefinitions.phase2Lines.first { it.text == "Signatures are light. Too light." }
        assertEquals(DesertDefinitions.DesertTrigger.SIGNATURES_VISIBLE, line.trigger)
    }

    @Test
    fun `desert tank commander label is uppercase like the other speakers`() {
        assertEquals("TOBAR", DesertDefinitions.TB)
    }
}
