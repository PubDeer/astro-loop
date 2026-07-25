package com.astroloop.game.data

import com.astroloop.game.hangar.ChatMessage
import org.junit.Assert.*
import org.junit.Test

class MultiWayBarConversationsTest {

    @Test
    fun `multi-way conversation exposes all participants`() {
        val convo = BarConversation(
            listOf("pilot_medic", "pilot_rascal", "pilot_brutus"),
            listOf(ChatMessage("MEDIC", "x", 0))
        )
        assertEquals(3, convo.participantIds.size)
        assertTrue("pilot_brutus" in convo.participantIds)
    }

    @Test
    fun `two-arg constructor still yields two participants`() {
        val convo = BarConversation(
            "pilot_medic", "pilot_rascal",
            listOf(ChatMessage("MEDIC", "x", 0))
        )
        assertEquals(listOf("pilot_medic", "pilot_rascal"), convo.participantIds)
    }

    private val allIds = PilotDefinitions.pilots.map { it.id }.toSet()

    // Both modes unioned — surfaces every authored entry, incl. TB-26 + TOBAR variants.
    private fun allConversations(): List<BarConversation> =
        BarConversations.getAvailable(allIds, arcCompleted = true, isAstroLoop = false) +
        BarConversations.getAvailable(allIds, arcCompleted = true, isAstroLoop = true)

    private fun multiWay(): List<BarConversation> =
        allConversations().filter { it.participantIds.size >= 3 }

    private val charCaps = mapOf(
        "DASH" to 59, "FANG" to 59,
        "MEDIC" to 58, "FROST" to 58, "UNIT-7" to 58,
        "HAVOC" to 58, "ASTRO" to 58, "TB-26" to 58, "TOBAR" to 58,
        "RASCAL" to 57, "BRUTUS" to 57, "EMBER" to 57, "KRAKEN" to 57,
        "WHISKERS" to 55
    )

    private val callsignToId =
        PilotDefinitions.pilots.associate { it.callsign to it.id } +
        mapOf("TB-26" to "tb26", "TOBAR" to "tb26")

    @Test
    fun `no multi-way conversation exceeds four lines`() {
        multiWay().forEach { convo ->
            assertTrue(
                "Conversation has ${convo.lines.size} lines: ${convo.lines.firstOrNull()?.text}",
                convo.lines.size <= 4
            )
        }
    }

    @Test
    fun `multi-way lines fit the per-speaker char budget`() {
        multiWay().forEach { convo ->
            convo.lines.forEach { msg ->
                val cap = charCaps[msg.speaker] ?: 55
                assertTrue(
                    "${msg.speaker} line too long (${msg.text.length} > $cap): ${msg.text}",
                    msg.text.length <= cap
                )
            }
        }
    }

    @Test
    fun `multi-way participant ids match the speakers`() {
        multiWay().forEach { convo ->
            val speakerIds = convo.lines.map {
                callsignToId[it.speaker] ?: error("unknown speaker ${it.speaker}")
            }.toSet()
            assertEquals(
                "participants mismatch for: ${convo.lines.first().text}",
                convo.participantIds.toSet(), speakerIds
            )
        }
    }

    // Single-mode list so pilot-only convos are counted once (they appear in both modes).
    private fun pilotOnlyMultiWay(): List<BarConversation> =
        BarConversations.getAvailable(allIds, arcCompleted = true, isAstroLoop = false)
            .filter { it.participantIds.size >= 3 && "tb26" !in it.participantIds }

    @Test
    fun `each pilot appears in at least twelve trios and eight quads`() {
        val convos = pilotOnlyMultiWay()
        PilotDefinitions.pilots.forEach { pilot ->
            val trios = convos.count { it.participantIds.size == 3 && pilot.id in it.participantIds }
            val quads = convos.count { it.participantIds.size == 4 && pilot.id in it.participantIds }
            assertTrue("${pilot.callsign} only appears in $trios trios (need >= 12)", trios >= 12)
            assertTrue("${pilot.callsign} only appears in $quads quads (need >= 8)", quads >= 8)
        }
    }

    @Test
    fun `a three-way fires only when all three pilots are unlocked`() {
        val twoOfThree = setOf("pilot_medic", "pilot_rascal")  // missing Brutus
        val withTrio = BarConversations.getAvailable(
            setOf("pilot_medic", "pilot_rascal", "pilot_brutus"),
            arcCompleted = false, isAstroLoop = false
        ).any { it.participantIds.toSet() ==
            setOf("pilot_medic", "pilot_rascal", "pilot_brutus") }
        val withoutTrio = BarConversations.getAvailable(
            twoOfThree, arcCompleted = false, isAstroLoop = false
        ).any { it.participantIds.toSet() ==
            setOf("pilot_medic", "pilot_rascal", "pilot_brutus") }
        assertTrue("Medic+Rascal+Brutus trio must fire when all three unlocked", withTrio)
        assertFalse("Trio must not fire when Brutus is locked", withoutTrio)
    }

    @Test
    fun `a four-way fires only when all four pilots are unlocked`() {
        val firstFour = setOf("pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost")
        val target = firstFour
        val withQuad = BarConversations.getAvailable(firstFour, false, false)
            .any { it.participantIds.toSet() == target }
        val withoutQuad = BarConversations.getAvailable(
            setOf("pilot_medic", "pilot_rascal", "pilot_brutus"), false, false
        ).any { it.participantIds.toSet() == target }
        assertTrue("First-four quad must fire when all four unlocked", withQuad)
        assertFalse("Quad must not fire when Frost is locked", withoutQuad)
    }
}
