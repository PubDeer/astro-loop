package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class RadioDefinitionsGetLinesTest {

    @Test
    fun `getLines returns the full pool for a known pilot and event`() {
        val pool = RadioDefinitions.getLines("pilot_medic", "shields_down")
        assertNotNull(pool)
        assertEquals(3, pool!!.size)
        assertTrue(pool.contains("Shields are down! Watch yourself!"))
    }

    @Test
    fun `getLines returns null for an unknown event`() {
        assertNull(RadioDefinitions.getLines("pilot_medic", "no_such_event"))
    }

    @Test
    fun `getLines filters out TB-26 lines for astro when requested`() {
        val pool = RadioDefinitions.getLines("pilot_astro", "combat_start", filterTb26 = true)
        assertNotNull(pool)
        pool!!.forEach { line ->
            assertFalse("astro filtered pool must not mention TB-26: '$line'", line.contains("TB-26"))
        }
    }

    // The non-Astro corruption run's 60s crewmate encounter speaks a first_enemy line
    // (the mirror of the normal-run enemy-contact call). Every possible starting pilot
    // must have a pool, or the scripted beat goes silent.
    @Test
    fun `every pilot has first_enemy lines for the corruption encounter mirror`() {
        for (pilot in PilotDefinitions.pilots) {
            val line = RadioDefinitions.getLine(pilot.id, "first_enemy")
            assertNotNull("no first_enemy line for ${pilot.id}", line)
        }
    }
}
