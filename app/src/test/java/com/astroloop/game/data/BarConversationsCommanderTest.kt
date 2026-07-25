package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class BarConversationsCommanderTest {

    private val allPilots = setOf(
        "tb26", "pilot_medic", "pilot_rascal", "pilot_brutus",
        "pilot_frost", "pilot_dash", "pilot_ember", "pilot_fang",
        "pilot_kraken", "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
    )

    @Test
    fun `no bar conversation line addresses the commander`() {
        val flagCombos = listOf(true to true, true to false, false to true, false to false)
        flagCombos.forEach { (arc, loop) ->
            BarConversations.getAvailable(allPilots, arcCompleted = arc, isAstroLoop = loop).forEach { convo ->
                convo.lines.forEach { msg ->
                    assertFalse(
                        "Line '${msg.text}' must not contain 'commander' (arc=$arc loop=$loop)",
                        msg.text.contains("commander", ignoreCase = true)
                    )
                }
            }
        }
    }
}
