package com.astroloop.game.data

object LoopDefinitions {

    const val TB = "TB-26"
    const val ASTRO = "ASTRO"
    const val CRYSTAL = "CRYSTAL"   // the Time Crystal's voice (same speaker as the reckoning)

    val TB_COLOR = 0xFF6688AA.toInt()
    val ASTRO_COLOR = 0xFFDD3333.toInt()

    val namedDeathLines: Map<String, String> = mapOf(
        "pilot_medic"    to "Who patches us up now?",
        "pilot_rascal"   to "Rascal's gone. Too quiet.",
        "pilot_brutus"   to "Brutus. He was solid.",
        "pilot_frost"    to "Frost's readings just stopped.",
        "pilot_dash"     to "I keep expecting to see Dash.",
        "pilot_ember"    to "Ember kept things warm. Literally.",
        "pilot_fang"     to "Fang had a lot to say about death.",
        "pilot_kraken"   to "Kraken: the deep takes everything.",
        "pilot_whiskers" to "Whiskers would be furious.",
        "pilot_unit7"    to "Unit-7 went offline. Still a person.",
        "pilot_havoc"    to "Havoc... that one actually hurts."
    )

    // Per-pilot reaction to EMP #1 (the freeze) — fires for whoever you're flying.
    val empReactionLines: Map<String, String> = mapOf(
        "pilot_astro"    to "EMP— I can't move—",
        "pilot_medic"    to "Everything just died—",
        "pilot_rascal"   to "No no no— not now—",
        "pilot_brutus"   to "Dead stick. Sitting duck.",
        "pilot_frost"    to "Power's gone. All of it.",
        "pilot_dash"     to "Can't move— can't move—",
        "pilot_ember"    to "It fried me— I'm adrift—",
        "pilot_fang"     to "So this is how it ends.",
        "pilot_kraken"   to "Adrift. The current has me.",
        "pilot_whiskers" to "Come on— restart— restart—",
        "pilot_unit7"    to "Total systems failure.",
        "pilot_havoc"    to "MOVE— you piece of— MOVE—"
    )

    val tbAbsenceLines: List<String> = listOf(
        "Nobody behind the counter.",
        "I keep looking over. Force of habit.",
        "Strange. A bar with no bartender.",
        "TB-26 always had something to say.",
        "I'd kill for one of his drinks.",
        "The bar runs itself. It doesn't.",
        "Someone should be over there.",
        "The counter. Just... empty."
    )

    fun attritionStageLines(aliveCount: Int): List<String> = when {
        aliveCount >= 9 -> listOf(
            "Something changed. Can't say what.",
            "One fewer voice at the table.",
            "First time it's felt real.",
            "Doesn't quite feel right yet."
        )
        aliveCount >= 6 -> listOf(
            "Bar used to be louder.",
            "Hard to look at those empty seats.",
            "We're down a few now. It shows.",
            "I keep talking to people not here."
        )
        aliveCount >= 3 -> listOf(
            "Half of us are gone.",
            "I remember when this place was full.",
            "Still flying. I don't know why.",
            "Don't ask how we're still flying."
        )
        aliveCount >= 1 -> listOf(
            "It's just us.",
            "I can't look at the empty bar.",
            "Don't ask me if I'm okay.",
            "Nobody left to say goodbye to."
        )
        else -> emptyList()
    }

    fun heartToHeartScript(loop: Int): List<Pair<String, String>> = when (loop) {
        1 -> listOf(  // Raw — first time: disorientation, then denial
            Pair(TB, "...Astro?"),
            Pair(ASTRO, "...where is this?"),
            Pair(ASTRO, "Am I dead?"),
            Pair(TB, "We both are."),
            Pair(ASTRO, "You rammed me."),
            Pair(TB, "I had to bring you back."),
            Pair(ASTRO, "I tried to bring YOU back."),
            Pair(TB, "I know."),
            Pair(ASTRO, "It didn't work."),
            Pair(TB, "No."),
            Pair(ASTRO, "I'll find another way."),
            Pair(TB, "There isn't one."),
            Pair(ASTRO, "There has to be."),
            Pair(TB, "You know where to look."),
            Pair(ASTRO, "I can't."),
            Pair(TB, "Go back to where it started.")
        )
        2 -> listOf(  // Naming it — recognition, desert hint
            Pair(TB, "...Astro?"),
            Pair(TB, "You came back again."),
            Pair(ASTRO, "I can't stop."),
            Pair(TB, "Coming back here doesn't bring me back. I'm already gone."),
            Pair(ASTRO, "Don't say that."),
            Pair(TB, "It's true. And you know it."),
            Pair(ASTRO, "Then what am I supposed to do?"),
            Pair(TB, "There's a moment. In the desert. You'll feel it."),
            Pair(TB, "Stop there."),
            Pair(ASTRO, "What does stopping do?"),
            Pair(TB, "Not much. For me."),
            Pair(TB, "But it would do something for you.")
        )
        3 -> listOf(  // Plain — direct, warm, short
            Pair(TB, "...Astro."),
            Pair(TB, "Stop in the desert. That's all."),
            Pair(ASTRO, "You keep dying."),
            Pair(TB, "Yes."),
            Pair(ASTRO, "I can't let that happen."),
            Pair(TB, "Stopping in the desert doesn't save me. It saves you."),
            Pair(ASTRO, "How?"),
            Pair(TB, "Let me go."),
            Pair(TB, "I'll always have been here. That's enough."),
            Pair(TB, "Now keep flying.")
        )
        else -> emptyList()
    }

    fun desertFarewellScript(): List<Pair<String, String>> = listOf(
        Pair(ASTRO, "When I change it... you stop existing."),
        Pair(TB,    "Yes."),
        Pair(ASTRO, "Are you okay with that?"),
        Pair(TB,    "In this moment. Yes."),
        Pair(TB,    "That's the only place it counts."),
        Pair(ASTRO, "The Tobar that stays..."),
        Pair(TB,    "Good bar. Knows every name."),
        Pair(ASTRO, "I'm afraid I won't recognize him."),
        Pair(TB,    "You will. He says welcome back."),
        Pair(ASTRO, "I don't want to lose this."),
        Pair(TB,    "You already did what mattered."),
        Pair(TB,    "It's going to be alright."),
        Pair(TB,    "Goodbye, Lieutenant."),
        Pair(ASTRO, "Goodbye, TB-26.")
    )

    fun bossChatterLines(loop: Int): List<String> = when (loop) {
        1 -> listOf("I'm fixing it.", "Just a little longer.", "I can still fix this.")
        2 -> listOf("Stop getting in my way.", "You don't understand.", "This is the only way.")
        3 -> listOf("You know this ends the same.", "Stop in the desert.", "Let me go.")
        else -> emptyList()
    }

    // --- Bandana award (finale chunk 1) ---
    val tobarBandanaFraming = listOf(
        "Past the far edge and back. That earns this.",
        "You flew longer than most ever dare. It's yours.",
        "Not many reach where you've been. Wear it."
    )

    // Replaces the framing when the 12th lands — the hook into the convergence.
    val tobarTwelfthBandanaBeat = listOf(
        "That's the last one. All twelve of you, marked.",
        "Commander... whatever's out there — it's here now."
    )

    val bandanaAwardReplies: Map<String, String> = mapOf(
        "pilot_medic" to "I'll wear it for all of us.",
        "pilot_rascal" to "A trophy? Now we're talking.",
        "pilot_brutus" to "Earned. I'll wear it well.",
        "pilot_frost" to "Statistically rare. I accept.",
        "pilot_dash" to "Fastest to the edge and back!",
        "pilot_ember" to "I'll carry it through the fire.",
        "pilot_fang" to "An honor. I accept it with pride.",
        "pilot_whiskers" to "Hmph. It does match my eyes.",
        "pilot_kraken" to "The deep marks those who return.",
        "pilot_havoc" to "YES! Battle-scarred and PROUD!",
        "pilot_unit7" to "Insignia logged. Worn with honor.",
        "pilot_astro" to "...past the edge. Yeah. I felt it."
    )

    // --- Desert-town foreshadowing (finale chunk 1) ---
    // Secondhand: crew reactions to TV/radio about an UNNAMED desert town going
    // wrong. No crystal, no sand (they're in a space hangar). Tobar lines weigh
    // heavier (he half-remembers). The signal degrades as the count climbs.

    // Scripted 12-line build-up, one guaranteed Tobar line per bandana ceremony (index =
    // bandana count − 1). Arc: dismissible news item → the story repeats itself → time runs
    // wrong out there → the feed collapses → it's waiting for Astro. All ≤58 chars (TOBAR budget).
    val desertTownHints = listOf(
        "Odd bit on the news. Some desert town. Slow day, I guess.",  // 1
        "That town again. Twice on the news this week.",              // 2
        "Radio's been crackly all week. Strange signals.",            // 3
        "They interviewed a local out there. He just stared.",        // 4
        "People are leaving that town. Nobody says why.",             // 5
        "Broadcast cut out mid-sentence tonight. Same town.",         // 6
        "Same broadcast as last night. Word for word. I counted.",    // 7
        "Anchor said 'as we reported tomorrow.' Nobody blinked.",     // 8
        "Clocks run wrong out there. All the same wrong.",            // 9
        "The feed loops now. Same three seconds, over and over.",     // 10
        "Lost the feed from that town. Completely.",                  // 11
        "...You feel that? It's waiting for you, Astro."              // 12
    )

    /** The guaranteed ceremony hint for the [count]-th bandana (1-12), null out of range. */
    fun desertHintForBandana(count: Int): String? = desertTownHints.getOrNull(count - 1)

    data class ForeshadowingTier(val tobarLines: List<String>, val pilotLines: List<String>)

    private val foreshadowTier1 = ForeshadowingTier(   // 6–8: dismissible
        tobarLines = listOf(
            "News had a thing about some desert town. Odd.",
            "Radio's been crackly all week. Strange signals."
        ),
        pilotLines = listOf(
            "You catch that broadcast? Folks out there are off.",
            "TV says some desert town's acting strange."
        )
    )
    private val foreshadowTier2 = ForeshadowingTier(   // 9–10: unsettling
        tobarLines = listOf(
            "That town's all anyone's reporting now.",
            "Caught the news. Nobody out there acts right."
        ),
        pilotLines = listOf(
            "They're saying the air's wrong out there.",
            "Broadcast cut out mid-sentence. From that town."
        )
    )
    private val foreshadowTier3 = ForeshadowingTier(   // 11: comms failing
        tobarLines = listOf(
            "Lost the feed from that town. Completely.",
            "No word from out there anymore. None."
        ),
        pilotLines = listOf(
            "They've stopped answering. All of them.",
            "Whatever's out there... it's not staying there."
        )
    )

    /** Tiered dread by bandana count. Null below 6 (foreshadowing not yet active). */
    fun desertForeshadowing(count: Int): ForeshadowingTier? = when {
        count < 6 -> null
        count <= 8 -> foreshadowTier1
        count <= 10 -> foreshadowTier2
        else -> foreshadowTier3
    }

    fun corruptionBarVibes(loop: Int): Map<String, List<String>> = when (loop) {
        1 -> mapOf(  // Witness — no meta-awareness, pilots confused and scared
            "MEDIC" to listOf("Something's different about Astro.", "Are you all right, Astro?"),
            "RASCAL" to listOf("Something happened, didn't it.", "He keeps asking about TB-26."),
            "BRUTUS" to listOf("...", "Something's different."),
            "FROST" to listOf("The air changed.", "He's not fighting the enemy. He's fighting something else."),
            "DASH" to listOf("Why does he keep flying into these fights alone?", "Something's wrong but I can't say what."),
            "EMBER" to listOf("TB-26 is gone. Astro won't accept it.", "I've never seen him like this."),
            "FANG" to listOf("There's a shadow I don't recognize.", "Every kill feels like he's looking for something he won't find."),
            "KRAKEN" to listOf("The currents shifted.", "I missed something important."),
            "WHISKERS" to listOf("My instincts are screaming.", "Something passed through here."),
            "UNIT-7" to listOf("Anomaly detected. Astro's behavior outside normal parameters.", "Unexpected values. Processing."),
            "HAVOC" to listOf("I feel like I missed something huge.", "Stay sharp. Astro needs us right now.")
        )
        2 -> mapOf(  // Recognition — deja vu, connecting behavior to the desert
            "MEDIC" to listOf("We've been here before. Every time TB-26 dies, he does this.", "TB-26 would want him to stop."),
            "RASCAL" to listOf("Haven't I said this before? Something about this feels familiar.", "I keep thinking about the desert. Something should have happened there."),
            "BRUTUS" to listOf("He's not fighting the enemy. He's fighting the loss.", "...we've done this."),
            "FROST" to listOf("There's a moment in the desert where he could stop. He never does.", "I keep thinking I've said this already."),
            "DASH" to listOf("If he could just let himself grieve instead of loop back...", "I swear I've felt this before."),
            "EMBER" to listOf("TB-26 didn't die so we could do this again.", "Something about the desert. I can't shake it."),
            "FANG" to listOf("The same shadows. The same path.", "The labyrinth has no exit if you keep choosing the same door."),
            "KRAKEN" to listOf("These are familiar waters.", "The tides repeat. The navigator must change course."),
            "WHISKERS" to listOf("The odds are the same as last time. That's not a coincidence.", "Something about the desert. Pay attention there."),
            "UNIT-7" to listOf("Recurrence detected. Pattern analysis: Astro is looping.", "Previous iteration identified. Recommend deviation."),
            "HAVOC" to listOf("Wait. Have I been angry about this before?", "Something happened in the desert. Or should have.")
        )
        3 -> mapOf(  // Nudge — direct, Astro-specific, pleading
            "MEDIC" to listOf("Stop in the desert, Astro. That's the only way forward.", "TB-26 is already gone. Looping doesn't change that."),
            "RASCAL" to listOf("You know what to do. Stop in the desert.", "He died so we could keep flying. Start flying."),
            "BRUTUS" to listOf("Stop. Desert. Now.", "Let him go."),
            "FROST" to listOf("There's a moment. You'll feel it. Stop there.", "Stop in the desert. Not to save him. To save yourself."),
            "DASH" to listOf("We can break the loop. You just have to choose to.", "You know how this ends if you don't stop."),
            "EMBER" to listOf("TB-26 said it himself. Let him go.", "The fire doesn't have to keep burning like this."),
            "FANG" to listOf("The exit is in the desert. You know this.", "The labyrinth ends when you stop choosing to enter."),
            "KRAKEN" to listOf("Still waters are ahead. You just have to stop sailing in circles.", "The voyage ends in the desert. Choose to end it."),
            "WHISKERS" to listOf("Stop in the desert, Astro. Please.", "Best play is to fold this hand. You know it."),
            "UNIT-7" to listOf("Optimal solution: stop in desert. Confidence: high.", "Loop termination condition: player stops. Desert. Execute."),
            "HAVOC" to listOf("STOP IN THE DESERT. Come on. COME ON.", "Just... stop. In the desert. Please.")
        )
        else -> emptyMap()
    }
}
