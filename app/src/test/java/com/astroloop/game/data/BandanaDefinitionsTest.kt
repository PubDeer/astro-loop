package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class BandanaDefinitionsTest {

    @Test
    fun knownPilotsHaveExpectedAccents() {
        assertEquals(0xFF2CC9B8.toInt(), BandanaDefinitions.accentColor("pilot_medic"))
        assertEquals(0xFFE0C233.toInt(), BandanaDefinitions.accentColor("pilot_fang"))
        assertEquals(0xFF33D6CC.toInt(), BandanaDefinitions.accentColor("pilot_astro"))
    }

    @Test
    fun allTwelvePilotsAreMapped() {
        val ids = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
            "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
            "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
        )
        for (id in ids) {
            // Mapped ids must not fall through to the white default.
            assertNotEquals("Missing accent for $id", 0xFFFFFFFF.toInt(), BandanaDefinitions.accentColor(id))
        }
    }

    @Test
    fun unknownPilotFallsBackToWhite() {
        assertEquals(0xFFFFFFFF.toInt(), BandanaDefinitions.accentColor("pilot_nobody"))
    }
}
