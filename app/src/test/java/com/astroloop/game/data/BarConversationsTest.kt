package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class BarConversationsTest {

    private val allPilots = setOf(
        "tb26", "pilot_medic", "pilot_rascal", "pilot_brutus",
        "pilot_frost", "pilot_dash", "pilot_ember", "pilot_fang",
        "pilot_kraken", "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
    )

    @Test
    fun `astro loop mode transforms TB-26 speaker to TB in all conversations`() {
        val convos = BarConversations.getAvailable(allPilots, arcCompleted = true, isAstroLoop = true)
        convos.forEach { convo ->
            convo.lines.forEach { msg ->
                assertFalse(
                    "Speaker '${msg.speaker}' should not be TB-26 in Astro Loop mode",
                    msg.speaker == "TB-26"
                )
            }
        }
    }

    @Test
    fun `astro loop mode removes TB-26 from dialogue text`() {
        val convos = BarConversations.getAvailable(allPilots, arcCompleted = true, isAstroLoop = true)
        convos.forEach { convo ->
            convo.lines.forEach { msg ->
                assertFalse(
                    "Text '${msg.text}' should not contain TB-26 in Astro Loop mode",
                    "TB-26" in msg.text
                )
            }
        }
    }

    @Test
    fun `non-astro-loop mode preserves TB-26 speaker names`() {
        val convos = BarConversations.getAvailable(allPilots, arcCompleted = true, isAstroLoop = false)
        val hasTb26Speaker = convos.any { it.lines.any { msg -> msg.speaker == "TB-26" } }
        assertTrue("Normal mode should have TB-26 as speaker", hasTb26Speaker)
    }

    @Test
    fun `astro loop mode excludes blockedInAstroLoop conversations`() {
        val normal = BarConversations.getAvailable(allPilots, arcCompleted = true, isAstroLoop = false)
        val astroLoop = BarConversations.getAvailable(allPilots, arcCompleted = true, isAstroLoop = true)
        val blockedExistInNormal = normal.any { it.blockedInAstroLoop }
        val blockedExistInAstroLoop = astroLoop.any { it.blockedInAstroLoop }
        assertTrue("Normal mode should include blockedInAstroLoop conversations", blockedExistInNormal)
        assertFalse("Astro Loop mode should exclude blockedInAstroLoop conversations", blockedExistInAstroLoop)
    }

    @Test
    fun `astro loop mode includes requiresAstroLoop conversations`() {
        val astroLoop = BarConversations.getAvailable(allPilots, arcCompleted = true, isAstroLoop = true)
        assertTrue("Astro Loop mode should include requiresAstroLoop conversations",
            astroLoop.any { it.requiresAstroLoop })
    }

    @Test
    fun `non-astro-loop mode excludes requiresAstroLoop conversations`() {
        val normal = BarConversations.getAvailable(allPilots, arcCompleted = true, isAstroLoop = false)
        assertFalse("Normal mode should not include requiresAstroLoop conversations",
            normal.any { it.requiresAstroLoop })
    }

    @Test
    fun `shield discovery conversation exists with 4 lines from Rascal, Unit-7, and Brutus`() {
        val convo = BarConversations.getShieldDiscoveryConversation()
        assertNotNull(convo)
        assertEquals(4, convo!!.lines.size)
        val speakers = convo.lines.map { it.speaker }
        assertTrue(speakers.any { it.contains("RASCAL", ignoreCase = true) })
        assertTrue(speakers.any { it.contains("UNIT", ignoreCase = true) })
        assertTrue(speakers.any { it.contains("BRUTUS", ignoreCase = true) })
    }

    @Test
    fun `astro loop mapping renames the bartender to TOBAR`() {
        val allIds = PilotDefinitions.pilots.map { it.id }.toSet()
        val convos = BarConversations.getAvailable(allIds, arcCompleted = true, isAstroLoop = true)
        val speakers = convos.flatMap { it.lines }.map { it.speaker }.toSet()
        assertTrue("TOBAR must speak in the astro-loop bar", "TOBAR" in speakers)
        assertFalse("robot name must not appear as speaker", "TB-26" in speakers)
        assertFalse("interim TB label must be gone", "TB" in speakers)
        assertFalse("mixed-case Tobar label must be gone", "Tobar" in speakers)
        val texts = convos.flatMap { it.lines }.map { it.text }
        assertTrue("dialogue text must not mention TB-26 in astro loop",
            texts.none { it.contains("TB-26") })
    }

    @Test
    fun `robot-specific bartender jokes never reach the astro loop bar`() {
        val allIds = PilotDefinitions.pilots.map { it.id }.toSet()
        val texts = BarConversations.getAvailable(allIds, arcCompleted = true, isAstroLoop = true)
            .flatMap { it.lines }.map { it.text }
        assertTrue("room-temperature robot joke must be blocked",
            texts.none { it.contains("But you're room temperature") })
        assertTrue("maintenance robot joke must be blocked",
            texts.none { it.contains("last maintenance") })
    }

    @Test
    fun `tobar alternates appear only in astro loop`() {
        val allIds = PilotDefinitions.pilots.map { it.id }.toSet()
        val astroTexts = BarConversations.getAvailable(allIds, arcCompleted = true, isAstroLoop = true)
            .flatMap { it.lines }.map { it.text }
        val normalTexts = BarConversations.getAvailable(allIds, arcCompleted = true, isAstroLoop = false)
            .flatMap { it.lines }.map { it.text }
        assertTrue(astroTexts.any { it.contains("day off") })
        assertTrue(normalTexts.none { it.contains("day off") })
        assertTrue(astroTexts.any { it.contains("Heating costs yen") })
        assertTrue(normalTexts.none { it.contains("Heating costs yen") })
    }
}
