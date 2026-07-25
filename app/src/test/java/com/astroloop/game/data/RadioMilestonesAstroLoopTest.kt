package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class RadioMilestonesAstroLoopTest {

    private val allPilots = listOf(
        "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
        "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
        "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
    )

    private val newEvents = listOf("time_10min", "time_12min", "time_14min", "time_16min")

    @Test
    fun `all 12 pilots have lines for each new Astro Loop milestone`() {
        for (event in newEvents) {
            for (pilotId in allPilots) {
                val line = RadioDefinitions.getLine(pilotId, event)
                assertNotNull("$pilotId missing line for $event", line)
            }
        }
    }

    @Test
    fun `all new milestone lines are at most 36 characters`() {
        for (event in newEvents) {
            for (pilotId in allPilots) {
                val seen = mutableSetOf<String>()
                repeat(50) {
                    val line = RadioDefinitions.getLine(pilotId, event)
                    if (line != null) seen.add(line)
                }
                for (line in seen) {
                    assertTrue(
                        "$pilotId $event line too long (${line.length}): '$line'",
                        line.length <= 36
                    )
                }
            }
        }
    }

    @Test
    fun `pilot_astro 16min line contains no TB-26 reference`() {
        val seen = mutableSetOf<String>()
        repeat(50) {
            val line = RadioDefinitions.getLine("pilot_astro", "time_16min")
            if (line != null) seen.add(line)
        }
        assertTrue("pilot_astro time_16min: no lines found", seen.isNotEmpty())
        for (line in seen) {
            assertFalse("pilot_astro time_16min must not mention TB-26: '$line'", "TB-26" in line)
        }
    }
}
