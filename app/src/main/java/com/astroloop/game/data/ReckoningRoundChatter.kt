package com.astroloop.game.data

import kotlin.random.Random

/**
 * Bar conversations between reckoning rounds. TOBAR counts Astro's walk-outs as "rounds" —
 * a bar pun (rounds of drinks / rounds of a fight) that is born as a joke, hardens into
 * ritual, gets heavy, falters at seventeen, and breaks at twenty. Past twenty he has lost
 * count for good and lostCountPool cycles (no immediate repeats — the caller persists the
 * returned pool index).
 *
 * AUTHORING RULES (enforced by ReckoningRoundChatterTest):
 *  - TOBAR and ASTRO only. TOBAR IS HUMAN — never write machine self-references for him.
 *  - Every conversation ends on an ASTRO line that is the retry button: he says it, the
 *    player does it.
 *  - Lines <= 58 chars (bar chatter column budget).
 *  - Numbers are words and match the round ordinal. The counter counts walk-outs, so the
 *    baked-in numbers stay truthful even when a round produced no bar visit (app kill).
 */
object ReckoningRoundChatter {

    const val ROUND_COUNT = 20

    // rounds[0] = round 1 … rounds[19] = round 20.
    val rounds: List<List<Pair<String, String>>> = listOf(
        // R1 — the original loss script, verbatim ("still awake" mirrors the win's "gone quiet")
        listOf(
            "TOBAR" to "You're back early.",
            "TOBAR" to "Something out there's still awake, isn't it.",
            "ASTRO" to "…I'm going back out."
        ),
        // R2 — the pun is born
        listOf(
            "TOBAR" to "Twice in one night, then.",
            "ASTRO" to "Just pour.",
            "TOBAR" to "One round, coming up.",
            "ASTRO" to "…Yeah. One more round. I'm going back out."
        ),
        // R3
        listOf(
            "TOBAR" to "Round three.",
            "TOBAR" to "I'm keeping the glass where you left it.",
            "ASTRO" to "Good. Going back out."
        ),
        // R4
        listOf(
            "TOBAR" to "Four.",
            "ASTRO" to "You're counting.",
            "TOBAR" to "One of us should.",
            "ASTRO" to "…Back out."
        ),
        // R5
        listOf(
            "TOBAR" to "Round five. You're favoring your left side.",
            "ASTRO" to "The ship's fine.",
            "TOBAR" to "I didn't ask about the ship.",
            "ASTRO" to "I'm going back out."
        ),
        // R6
        listOf(
            "TOBAR" to "Six. I've started a tab.",
            "ASTRO" to "For the drinks?",
            "TOBAR" to "For the rounds.",
            "ASTRO" to "Close it later. I'm going out."
        ),
        // R7
        listOf(
            "TOBAR" to "Seven. Lucky number, they say.",
            "ASTRO" to "Who says?",
            "TOBAR" to "Pilots. Mostly before round eight.",
            "ASTRO" to "…Going back out."
        ),
        // R8
        listOf(
            "TOBAR" to "Round eight.",
            "TOBAR" to "The regulars think you're stuck on a high score.",
            "ASTRO" to "Let them think it.",
            "ASTRO" to "Back out."
        ),
        // R9
        listOf(
            "TOBAR" to "Nine. Whatever's out there drinks free, apparently.",
            "ASTRO" to "It doesn't drink.",
            "TOBAR" to "Then it's got no business in my count.",
            "ASTRO" to "I'm going back out."
        ),
        // R10
        listOf(
            "TOBAR" to "Round ten. Double digits.",
            "ASTRO" to "Don't make it a thing.",
            "TOBAR" to "It became a thing at six.",
            "ASTRO" to "Going back out."
        ),
        // R11
        listOf(
            "TOBAR" to "Eleven.",
            "TOBAR" to "The bar never closes. But you should.",
            "ASTRO" to "After this round.",
            "ASTRO" to "Back out."
        ),
        // R12
        listOf(
            "TOBAR" to "Twelve. I cleaned your seat. You never use it.",
            "ASTRO" to "I sit down when it's done.",
            "TOBAR" to "I'll hold you to that.",
            "ASTRO" to "Going out."
        ),
        // R13
        listOf(
            "TOBAR" to "Thirteen. I'd skip this one, superstitiously speaking.",
            "ASTRO" to "You're not superstitious.",
            "TOBAR" to "I pour drinks for pilots. I caught it from them.",
            "ASTRO" to "…I'm going back out."
        ),
        // R14
        listOf(
            "TOBAR" to "Fourteen.",
            "ASTRO" to "You're quieter about it now.",
            "TOBAR" to "The number's getting heavy.",
            "ASTRO" to "Then don't lift it. Back out."
        ),
        // R15
        listOf(
            "TOBAR" to "Fifteen rounds.",
            "TOBAR" to "Whatever it is, it's patient.",
            "ASTRO" to "So am I.",
            "ASTRO" to "Going back out."
        ),
        // R16
        listOf(
            "TOBAR" to "Sixteen. I stopped polishing the glass.",
            "ASTRO" to "Why?",
            "TOBAR" to "You're never here long enough to use it.",
            "ASTRO" to "…I'm going back out."
        ),
        // R17 — the count first falters
        listOf(
            "TOBAR" to "Seventeen. Or eighteen.",
            "ASTRO" to "Seventeen.",
            "TOBAR" to "…I used to be sure.",
            "ASTRO" to "Back out."
        ),
        // R18 — TOBAR's warmest line; echoes his "Welcome back"
        listOf(
            "TOBAR" to "Eighteen.",
            "TOBAR" to "Come back messy, come back angry. Just come back.",
            "ASTRO" to "I always do.",
            "ASTRO" to "Back out."
        ),
        // R19
        listOf(
            "TOBAR" to "I want to say nineteen.",
            "ASTRO" to "You want to?",
            "TOBAR" to "The marks disagree with each other now.",
            "ASTRO" to "…One more round. Going out."
        ),
        // R20 — the count breaks
        listOf(
            "TOBAR" to "I've lost count.",
            "ASTRO" to "You? You don't lose count.",
            "TOBAR" to "No. I don't.",
            "ASTRO" to "…Going back out."
        )
    )

    // Rounds 21+: TOBAR has lost count for good. P4 loops back to round 1's wording —
    // even the fallback state is cyclical.
    val lostCountPool: List<List<Pair<String, String>>> = listOf(
        listOf(
            "TOBAR" to "Back early.",
            "TOBAR" to "Some round or other.",
            "ASTRO" to "The last one, maybe.",
            "ASTRO" to "Going back out."
        ),
        listOf(
            "TOBAR" to "I stopped counting. It helps.",
            "ASTRO" to "Does it?",
            "TOBAR" to "No.",
            "ASTRO" to "…Back out."
        ),
        listOf(
            "TOBAR" to "The glass is where you left it.",
            "ASTRO" to "Keep it there.",
            "ASTRO" to "I'm going back out."
        ),
        listOf(
            "TOBAR" to "You're back. It's still awake.",
            "ASTRO" to "Not for long.",
            "ASTRO" to "Going back out."
        )
    )

    /**
     * Conversation for the Nth round's loss return. Rounds 1..ROUND_COUNT map to the
     * authored arc (round <= 0 clamps to round 1); later rounds draw from lostCountPool,
     * never repeating lastPoolIndex (-1 = nothing to avoid).
     * Returns (conversation, poolIndexUsed) — poolIndexUsed is -1 for authored rounds.
     */
    fun forRound(round: Int, lastPoolIndex: Int, random: Random = Random.Default):
            Pair<List<Pair<String, String>>, Int> {
        if (round <= ROUND_COUNT) return rounds[(round - 1).coerceAtLeast(0)] to -1
        val eligible = lostCountPool.indices.filter { it != lastPoolIndex }
        val pick = eligible[random.nextInt(eligible.size)]
        return lostCountPool[pick] to pick
    }
}
