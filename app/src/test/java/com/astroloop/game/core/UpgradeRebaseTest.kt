package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Finder's Fee and Scavenger Rig were rebased so the shop's percentage label tells the truth.
 *
 * The multiplier used to run 0.5 .. 1.0 in +0.10 steps, and the tile said "+10%". That was 10
 * percentage points of a nominal 1.0 the player is never shown, while the real first purchase was
 * +20%. The multiplier now runs 1.0 .. 2.0 in +0.20 steps and the 0.5 lives at the payout site as
 * a named base rate, so "+20%" is exactly what the first purchase delivers.
 *
 * The rebase must be **arithmetically neutral**: base * multiplier has to equal the old multiplier
 * at every level. These tests are the guard on that, since the whole point was to change the label
 * and nothing else.
 */
class UpgradeRebaseTest {

    /** What the multiplier returned before the rebase, for every level. */
    private fun legacyYen(level: Int) = 0.5f + level * 0.10f
    private fun legacySalvage(level: Int) = 0.5f + level * 0.10f

    @Test
    fun `yen payout is unchanged at every level`() {
        val state = GameState()
        for (level in 0..5) {
            state.permanentYenBonusLevel = level
            val effective = GameConfig.YEN_BASE_RATE * state.getYenMultiplier()
            assertEquals(
                "level $level must pay exactly what it paid before the rebase",
                legacyYen(level), effective, 0.0001f
            )
        }
    }

    @Test
    fun `salvage rate is unchanged at every level`() {
        val state = GameState()
        for (level in 0..5) {
            state.permanentSalvageLevel = level
            val effective = GameConfig.SALVAGE_BASE_RATE * state.getSalvageMultiplier()
            assertEquals(
                "level $level must drop exactly what it dropped before the rebase",
                legacySalvage(level), effective, 0.0001f
            )
        }
    }

    @Test
    fun `the first purchase delivers exactly the advertised twenty percent`() {
        // This is the property the rebase exists for: the tile says +20%, so buying level 1 must
        // multiply the player's actual income by 1.20 — not by 1.10, and not by anything else.
        val state = GameState()
        state.permanentYenBonusLevel = 0
        val before = state.getYenMultiplier()
        state.permanentYenBonusLevel = 1
        val after = state.getYenMultiplier()
        assertEquals("first Finder's Fee purchase must be +20%", 1.20f, after / before, 0.0001f)

        state.permanentSalvageLevel = 0
        val sBefore = state.getSalvageMultiplier()
        state.permanentSalvageLevel = 1
        val sAfter = state.getSalvageMultiplier()
        assertEquals("first Scavenger Rig purchase must be +20%", 1.20f, sAfter / sBefore, 0.0001f)
    }

    @Test
    fun `a maxed track doubles the rate`() {
        val state = GameState()
        state.permanentYenBonusLevel = 5
        assertEquals(2.0f, state.getYenMultiplier(), 0.0001f)
        state.permanentSalvageLevel = 5
        assertEquals(2.0f, state.getSalvageMultiplier(), 0.0001f)
    }

    @Test
    fun `an unupgraded multiplier is neutral, so the base rate is the whole story`() {
        val state = GameState()
        assertEquals("a level-0 multiplier must not scale anything", 1f, state.getYenMultiplier(), 0.0001f)
        assertEquals("a level-0 multiplier must not scale anything", 1f, state.getSalvageMultiplier(), 0.0001f)
    }
}
