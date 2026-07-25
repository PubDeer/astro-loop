package com.astroloop.game.data

import com.astroloop.game.render.GhostShipLance
import com.astroloop.game.system.CrystalPhase
import org.junit.Assert.*
import org.junit.Test

class CrystalFightLinesTest {

    @Test fun allReckoningRadioLinesFitTheHudBudget() {
        val limit = 35
        val lines = mutableListOf<String>()
        CrystalFightLines.opening.forEach { (a, b) -> lines += a; lines += b }
        CrystalPhase.values().forEach { p ->
            val (a, b) = CrystalFightLines.taunt(p)
            lines += a
            if (b != null) lines += b
        }
        CrystalFightLines.ghostScript.forEach { lines += it.second }
        lines.forEach {
            assertTrue("\"$it\" is ${it.length} chars (limit $limit)", it.length <= limit)
        }
    }

    @Test fun allBarLinesFitTheChatColumn() {
        val limit = 58
        CrystalFightLines.barChatter.forEach { (s, l) ->
            assertTrue("$s: \"$l\" is ${l.length} chars (limit $limit)", l.length <= limit)
        }
    }

    @Test fun everyPhaseHasATaunt() {
        CrystalPhase.values().forEach { p ->
            assertTrue("${p} needs a part 1", CrystalFightLines.taunt(p).first.isNotBlank())
        }
    }

    @Test fun p5IsASingleScream() {
        // The mask drops — one line, no measured follow-up.
        assertNull(CrystalFightLines.taunt(CrystalPhase.P5).second)
    }

    // NOTE: "Astro never speaks during the fight" is deliberately NOT tested here. taunt() has
    // no speaker field — the speaker is hardcoded to CRYSTAL_CALLSIGN at the GameSurfaceView call
    // site — so any data-level assertion would be vacuous. The invariant is enforced by there
    // being exactly one showScriptedMessage call in updateReckoningFight, and by the playtest
    // checklist. Do not add a fake test here to feel covered.

    @Test fun ghostScriptIsChronological() {
        val times = CrystalFightLines.ghostScript.map { it.third }
        assertEquals(times.sorted(), times)
        assertEquals(0f, times.first(), 0.001f)
    }

    @Test fun astroSpeaksFirstOnlyAfterTheCrystalsOpeningClaim() {
        // "...That's my crew." is his first word in 90 seconds, and it answers the CREW,
        // never the crystal. The crystal must speak first.
        val speakers = CrystalFightLines.ghostScript.map { it.first }
        assertEquals("CRYSTAL", speakers.first())
        assertEquals("ASTRO", speakers[1])
    }

    @Test fun theReleaseLineIsAstrosLast() {
        val last = CrystalFightLines.ghostScript.last()
        assertEquals("ASTRO", last.first)
        assertEquals(CrystalFightLines.GHOST_RELEASE_LINE, last.second)
    }

    @Test fun stayImmediatelyPrecedesTheReleaseLine() {
        // "...You never had to." is elliptical (had to WHAT?) and completes ONLY against the
        // crystal's "STAY" on the line before it. Separating them breaks the climax.
        val s = CrystalFightLines.ghostScript
        val release = s.indexOfFirst { it.second == CrystalFightLines.GHOST_RELEASE_LINE }
        assertTrue(release > 0)
        val before = s[release - 1]
        assertEquals("CRYSTAL", before.first)
        assertTrue("the line before the release must contain STAY", before.second.contains("STAY"))
    }

    @Test fun theCrewAskForReleaseBeforeAstroGrantsIt() {
        val s = CrystalFightLines.ghostScript
        val ask = s.indexOfFirst { it.second.contains("Let go") }
        val release = s.indexOfFirst { it.second == CrystalFightLines.GHOST_RELEASE_LINE }
        assertTrue("the crew must ask before Astro grants", ask in 0 until release)
    }

    @Test fun theCrewRefuteTheCrystalsLie() {
        // The crystal's central claim is "they never lived". The ghosts disprove it by speaking.
        val s = CrystalFightLines.ghostScript
        assertTrue(s.any { it.first == "CRYSTAL" && it.second.contains("never lived") })
        assertTrue(s.any { it.first == "MEDIC" && it.second.contains("We lived") })
    }

    @Test fun ghostSpeakersHaveGhostPortraits() {
        // Every crew voice needs a portrait_ghost_<pilot>.png. Keep this in sync with
        // IconCache.GHOST_IDS or a speaker will fall back to their living portrait.
        val crew = CrystalFightLines.ghostScript.map { it.first }
            .filter { it != "CRYSTAL" && it != "ASTRO" }.toSet()
        assertEquals(setOf("MEDIC", "DASH", "BRUTUS", "WHISKERS"), crew)
    }

    @Test fun theReleaseLineLandsAfterTheGhostsHaveGathered() {
        // GhostShipLance.release() no-ops unless the ring is whole (t >= T_GATHERED). If the
        // release line is ever retimed before that, the lance never fires, enterReckoningFlyHome()
        // is never reached, and the finale HANGS FOREVER. No other test covers this.
        val releaseAt = CrystalFightLines.ghostScript.last().third
        assertTrue("release line at ${releaseAt}s must land after the ring is whole " +
                   "(T_GATHERED=${GhostShipLance.T_GATHERED}s) or the lance never fires",
            releaseAt >= GhostShipLance.T_GATHERED)
    }

    @Test fun brutusRebuttalImmediatelyFollowsTheCrystalsClaim() {
        // "Held. Never had." is as elliptical as the release line — it rebuts CRYSTAL's
        // "I held every one of them." and is meaningless if separated from it.
        val s = CrystalFightLines.ghostScript
        val brutus = s.indexOfFirst { it.second == "Held. Never had." }
        assertTrue(brutus > 0)
        val before = s[brutus - 1]
        assertEquals("CRYSTAL", before.first)
        assertTrue("the line before Brutus's rebuttal must be the crystal's claim",
            before.second.contains("I held"))
    }
}
