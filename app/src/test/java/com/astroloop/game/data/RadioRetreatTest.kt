package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class RadioRetreatTest {

    private val allPilots = listOf(
        "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
        "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
        "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
    )

    @Test
    fun `all 12 pilots have retreat_home lines`() {
        for (pilotId in allPilots) {
            val lines = (1..10).map {
                RadioDefinitions.getLine(pilotId, "retreat_home")
            }.filterNotNull()
            assertTrue("$pilotId should have retreat_home lines", lines.isNotEmpty())
        }
    }

    @Test
    fun `all retreat_home lines are at most 36 characters`() {
        for (pilotId in allPilots) {
            val seen = mutableSetOf<String>()
            repeat(50) {
                val line = RadioDefinitions.getLine(pilotId, "retreat_home")
                if (line != null) seen.add(line)
            }
            for (line in seen) {
                assertTrue(
                    "$pilotId line too long (${line.length}): '$line'",
                    line.length <= 36
                )
            }
        }
    }
}
