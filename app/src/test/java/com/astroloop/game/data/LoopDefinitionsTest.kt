package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class LoopDefinitionsTest {

    @Test
    fun `each loop has a non-empty heartToHeart script`() {
        for (loop in 1..3) {
            val lines = LoopDefinitions.heartToHeartScript(loop)
            assertTrue("Loop $loop should have lines", lines.isNotEmpty())
        }
    }

    @Test
    fun `loop 3 ends with keep flying`() {
        val lines = LoopDefinitions.heartToHeartScript(3)
        val lastTbLine = lines.filter { it.first == "TB-26" }.last()
        assertTrue(lastTbLine.second.contains("flying"))
    }

    @Test
    fun `loops 1 and 2 end with a TB-26 line`() {
        for (loop in 1..2) {
            val lines = LoopDefinitions.heartToHeartScript(loop)
            assertEquals("TB-26", lines.last().first)
        }
    }

    @Test
    fun `invalid loop returns empty list`() {
        assertTrue(LoopDefinitions.heartToHeartScript(0).isEmpty())
        assertTrue(LoopDefinitions.heartToHeartScript(4).isEmpty())
    }

    @Test
    fun `each loop has corruption bar vibes for all 11 pilots`() {
        val pilots = listOf("MEDIC", "RASCAL", "BRUTUS", "FROST", "DASH", "EMBER", "FANG", "KRAKEN", "WHISKERS", "UNIT-7", "HAVOC")
        for (loop in 1..3) {
            val vibes = LoopDefinitions.corruptionBarVibes(loop)
            for (pilot in pilots) {
                assertTrue("Loop $loop pilot $pilot should have lines", vibes[pilot]?.isNotEmpty() == true)
            }
        }
    }

    @Test
    fun `boss chatter lines exist for all loops`() {
        for (loop in 1..3) {
            assertTrue(LoopDefinitions.bossChatterLines(loop).isNotEmpty())
        }
    }

    @Test
    fun `namedDeathLines covers all 11 non-Astro pilots`() {
        val expectedPilots = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost", "pilot_dash",
            "pilot_ember", "pilot_fang", "pilot_kraken", "pilot_whiskers", "pilot_unit7", "pilot_havoc"
        )
        for (pilotId in expectedPilots) {
            assertNotNull("$pilotId should have a named death line",
                LoopDefinitions.namedDeathLines[pilotId])
        }
    }

    @Test
    fun `attritionStageLines returns non-empty for alive counts 1 through 10`() {
        for (count in 1..10) {
            assertTrue("aliveCount=$count should have stage lines",
                LoopDefinitions.attritionStageLines(count).isNotEmpty())
        }
    }

    @Test
    fun `attritionStageLines returns empty for zero alive`() {
        assertTrue(LoopDefinitions.attritionStageLines(0).isEmpty())
    }

    @Test
    fun `tbAbsenceLines is non-empty`() {
        assertTrue(LoopDefinitions.tbAbsenceLines.isNotEmpty())
    }

    @Test
    fun `loop 1 opens on disorientation`() {
        val lines = LoopDefinitions.heartToHeartScript(1)
        assertEquals(LoopDefinitions.TB, lines[0].first)
        assertEquals("...Astro?", lines[0].second)
        assertEquals(LoopDefinitions.ASTRO, lines[1].first)
        assertEquals("...where is this?", lines[1].second)
        assertEquals(LoopDefinitions.ASTRO, lines[2].first)
        assertEquals("Am I dead?", lines[2].second)
    }

    @Test
    fun `all named death lines are 36 chars or fewer`() {
        for ((id, line) in LoopDefinitions.namedDeathLines) {
            assertTrue("$id: '$line' exceeds 36 chars", line.length <= 36)
        }
    }

    @Test
    fun `all attrition stage lines are 36 chars or fewer`() {
        for (count in 1..10) {
            for (line in LoopDefinitions.attritionStageLines(count)) {
                assertTrue("'$line' exceeds 36 chars", line.length <= 36)
            }
        }
    }

    @Test
    fun `all tbAbsenceLines are 36 chars or fewer`() {
        for (line in LoopDefinitions.tbAbsenceLines) {
            assertTrue("'$line' exceeds 36 chars", line.length <= 36)
        }
    }

    @Test
    fun `loop 1 has 16 lines with disorientation arc and correct closing line`() {
        val lines = LoopDefinitions.heartToHeartScript(1)
        assertEquals("Loop 1 must have 16 lines (disorientation arc)", 16, lines.size)
        assertEquals("I tried to bring YOU back.", lines[6].second)
        assertEquals("I can't.", lines[14].second)
        assertEquals("Go back to where it started.", lines[15].second)
        assertEquals(LoopDefinitions.TB, lines[15].first)
    }

    @Test
    fun `desert town hints script all 12 bandanas in order and fit the chat column`() {
        assertEquals(12, LoopDefinitions.desertTownHints.size)
        for ((i, line) in LoopDefinitions.desertTownHints.withIndex()) {
            assertTrue("hint ${i + 1}: '$line' is ${line.length} chars (limit 58)", line.length <= 58)
        }
        for (c in 1..12) {
            assertEquals(LoopDefinitions.desertTownHints[c - 1], LoopDefinitions.desertHintForBandana(c))
        }
        assertNull(LoopDefinitions.desertHintForBandana(0))
        assertNull(LoopDefinitions.desertHintForBandana(13))
    }

    @Test
    fun `empReactionLines covers all 12 pilots and fits the HUD`() {
        val expected = listOf(
            "pilot_astro", "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
            "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken", "pilot_whiskers",
            "pilot_unit7", "pilot_havoc"
        )
        for (id in expected) {
            val line = LoopDefinitions.empReactionLines[id]
            assertNotNull("$id should have an EMP line", line)
            assertTrue("$id EMP line too long: '${line}'", line!!.length <= 36)
        }
    }
}
