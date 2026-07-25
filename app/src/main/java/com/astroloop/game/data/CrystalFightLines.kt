package com.astroloop.game.data

import com.astroloop.game.system.CrystalPhase

object CrystalFightLines {
    // All radio lines <=35 chars (HUD radio budget).

    // Opening monologue (ASTRO): pairs shown at ~3s/10s/17s, part 2 follows +3.5s.
    val opening = listOf(
        "I left it out here." to "Told myself that was the end.",
        "But it never opened." to "All those loops, still in there.",
        "And now it's starting to leak." to "Time to close it."
    )

    /**
     * Phase lures (CRYSTAL): part 1 on the phase change, part 2 +3.5s.
     *
     * The crystal is a LURE, not a taunt. Every line is an invitation to STOP — and stopping
     * is exactly what kills you. It never mocks the running; grief doesn't chase you screaming,
     * it whispers rest, stay, come back. The trap is never stated out loud: the player feels it.
     *
     * ASTRO SPEAKS ZERO LINES ACROSS ALL 90 SECONDS. Answering is engaging, and engaging is
     * stopping. His silence is the refusal — and it makes his first word in the ghost phase land.
     */
    fun taunt(p: CrystalPhase): Pair<String, String?> = when (p) {
        CrystalPhase.P1 -> "You came back." to "They always come back."           // recognition
        CrystalPhase.P2 -> "Pick me up. One more loop." to "Nothing has to end."  // invitation
        CrystalPhase.P3 -> "They're all still in here." to "Every one you lost."  // the offer
        CrystalPhase.P4 -> "We could still begin." to "You and me. Again."        // the bargain
        CrystalPhase.P5 -> "STAY — STAY —" to null                                // desperation
    }

    /** Astro's "Go." — the line whose firing releases the ghosts. */
    const val GHOST_RELEASE_LINE = "…You never had to. Go."

    /**
     * Ghost climax script: (speaker, line, seconds from lance start).
     *
     * The crystal's central lie is "they never lived". This beat is its REFUTATION: it says they
     * never lived, then they speak, then they leave. Astro's silence breaks ONLY for his crew —
     * the crystal never gets a word from him at any point in the fight.
     *
     * LOAD-BEARING ORDERING — do not reorder or re-time:
     *  - "…That's my crew." is Astro's first word in 90 seconds, and it is addressed to the crew.
     *  - GHOST_RELEASE_LINE is elliptical (had to WHAT?) and completes ONLY against the crystal's
     *    "STAY" on the line immediately before it: *you never had to stay*.
     *  - The crew ASK to be released ("Let go, boss.") before Astro grants it.
     *
     * Every pronoun resolves against the line directly before it: the radio HUD shows ONE line at
     * a time with no scrollback, so a pronoun needing earlier context is unreadable.
     */
    val ghostScript: List<Triple<String, String, Float>> = listOf(
        Triple("CRYSTAL",  "They're mine. They never lived.", 0f),
        Triple("ASTRO",    "…That's my crew.",                2.5f),
        Triple("MEDIC",    "We lived, Astro.",                5f),
        Triple("DASH",     "Every loop. All of it.",          7.5f),
        Triple("CRYSTAL",  "I held every one of them.",       10f),
        Triple("BRUTUS",   "Held. Never had.",                12.5f),
        Triple("WHISKERS", "Let go, boss. We're ready.",      15f),
        Triple("CRYSTAL",  "STAY — begin — STAY —",           17.5f),
        Triple("ASTRO",    GHOST_RELEASE_LINE,                20f)
    )

    // <=~58 chars each (bar chatter column budget)

    /**
     * Bar chatter on the WIN. Medic said "We lived, Astro." thirty seconds ago and has no idea —
     * the climax's warmth curdles on the walk home. That gap is the whole story.
     */
    val barChatter = listOf(
        "TOBAR" to "You're back. Something out there's gone quiet.",
        "ASTRO" to "Handled. It won't reach us now.",
        "MEDIC" to "Reach us? …Huh. Feels lighter in here. Good.",
        "ASTRO" to "…Yeah. You'd have liked it out there, Medic.",
        "MEDIC" to "Out where?"
    )
}
