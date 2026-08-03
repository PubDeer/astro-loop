package com.astroloop.game.hangar

import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.data.BarConversation
import com.astroloop.game.data.BarConversations
import com.astroloop.game.data.CrystalFightLines
import com.astroloop.game.data.LoopDefinitions
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.ReckoningRoundChatter
import kotlin.random.Random

class ChatSystem {

    companion object {
        const val CONVERSATION_COOLDOWN = 20f           // normal / Astro Loop
        const val LINE_PAUSE = 4.0f  // Pause between lines within a conversation
        const val DEATH_RETURN_FIRST_LINE_DELAY = 1.0f  // beat before the first return line lands
        // Tail after a SCRIPTED burst (post-run return, reckoning chatter, recruitment).
        // The full CONVERSATION_COOLDOWN here left the bar audibly dead right after the
        // return lines — ambient banter should pick the scene back up almost immediately.
        const val SCRIPTED_BURST_TAIL_COOLDOWN = 3f

        // Weighted category mix for the unified chat picker (1-way through 4-way).
        val CATEGORY_WEIGHTS = mapOf(1 to 25, 2 to 40, 3 to 23, 4 to 12)

        /** Off the bar page, drop conversations that include the selected pilot. */
        fun pageFilter(
            convos: List<BarConversation>,
            isBarPage: Boolean,
            selectedPilotId: String?
        ): List<BarConversation> {
            if (isBarPage || selectedPilotId == null) return convos
            return convos.filter { selectedPilotId !in it.participantIds }
        }

        /** Weighted pick of a category size, renormalized over `eligibleSizes`. `roll` in [0,1). */
        fun chooseCategory(weights: Map<Int, Int>, eligibleSizes: Set<Int>, roll: Float): Int {
            val sizes = eligibleSizes.sorted()
            require(sizes.isNotEmpty()) { "eligibleSizes must not be empty" }
            val total = sizes.sumOf { weights[it] ?: 0 }
            if (total <= 0) return sizes.first()
            val target = roll.coerceIn(0f, 0.999999f) * total
            var cum = 0f
            for (s in sizes) {
                cum += (weights[s] ?: 0).toFloat()
                if (target < cum) return s
            }
            return sizes.last()
        }

        /** An unused index in [0,poolSize), or null when the pool is exhausted (no auto-recycle). */
        fun pickUnusedLineIndex(poolSize: Int, used: Set<Int>, roll: Float): Int? {
            if (poolSize <= 0) return null
            val available = (0 until poolSize).filter { it !in used }
            if (available.isEmpty()) return null
            val i = (roll.coerceIn(0f, 0.999999f) * available.size).toInt().coerceIn(0, available.size - 1)
            return available[i]
        }
    }

    private val maxMessages = 20
    private var lastSpeaker: String = ""
    private val DESERT_FORESHADOW_CHANCE = 0.2f

    // No-repeat tracking — cleared on run return
    private val usedConversations = mutableSetOf<BarConversation>()
    private val usedIdleLines = mutableMapOf<String, MutableSet<Int>>()  // speaker -> used line indices

    // Idle lines per pilot (by callsign). 8-10 personality-driven lines each.
    // internal for content-lint tests: idle lines fire in every stage, so they must be
    // authored with "TB-26" — the astro-loop display swap renders him as Tobar there.
    internal val idleLines = mapOf(
        "MEDIC" to listOf(
            "Everyone's vitals look stable. For now.",
            "I patched up the hull again. You're welcome.",
            "Nano repair gel is running low. Try not to get shot.",
            "Med school never mentioned asteroid fields.",
            "I'm a doctor, not a mechanic. But here I am.",
            "Blood pressure's fine. Mine isn't.",
            "Please stop calling me when you stub your toe.",
            "Applied a bandage to the fuselage. It helped. Somehow.",
            "My prescription: fewer explosions.",
            "Has anyone here heard of a physical? Anyone?"
        ),
        "RASCAL" to listOf(
            "Found some spare parts in the vents. Don't ask.",
            "I wasn't stealing, I was... redistributing.",
            "These magnet coils are surprisingly tasty. Kidding.",
            "Five-second rule applies in zero gravity, right?",
            "The vents here are spacious. Very livable.",
            "Ooh, who left this wrench unattended?",
            "I hid snacks in seven places. Only remember five.",
            "Anyone missing a wallet? Unrelated question.",
            "Dumpster behind the hangar? Gourmet.",
            "I didn't take it. Prove it."
        ),
        "FROST" to listOf(
            "Temperature readings are nominal. I like nominal.",
            "The cryo field needs recalibration. Again.",
            "Cold never bothered me anyway.",
            "Waddle waddle.",
            "Ice forms patterns. I find them... soothing.",
            "Statistically, we should be dead. Fascinating.",
            "My feathers are aerodynamic. Your argument is not.",
            "Sub-zero is a lifestyle, not a temperature.",
            "I analyzed the odds. Then I had a fish.",
            "Calm is just cold anger with better posture."
        ),
        "UNIT-7" to listOf(
            "Running diagnostics. All systems nominal.",
            "Probability of survival: calculating...",
            "I do not understand humor. Please explain.",
            "Duplicator core at optimal efficiency.",
            "Why do organics consume liquid? Inefficient.",
            "Error: emotion not found.",
            "I have counted every rivet on this ship. There are 4,217.",
            "Sleep mode is superior to 'napping.'",
            "Query: what is 'vibes'?",
            "I do not blink. This upsets the crew."
        ),
        "DASH" to listOf(
            "Sitting still is physically painful.",
            "I could outrun that asteroid. Probably.",
            "Speed is life. Slowness is... not life.",
            "My momentum drive readings are off the charts.",
            "Bored. Bored. Bored. Can we go yet?",
            "I already ran three laps around the hangar.",
            "Slow people make me itchy.",
            "If I'm not moving, I'm dying. Basically.",
            "Just vibrating in place until launch.",
            "I timed myself blinking. New record."
        ),
        "BRUTUS" to listOf(
            "...",
            "Hmph.",
            "Hit me harder. I dare you.",
            "Revenge is a dish best served immediately.",
            "Walls break before I do.",
            "Good.",
            "Don't talk. Shoot.",
            "Bear doesn't run. Bear doesn't hide.",
            "Pain is just a suggestion.",
            "Grunt."
        ),
        "EMBER" to listOf(
            "These flames don't light themselves. Wait, they do.",
            "Rise from the ashes, they said. So I did.",
            "Is it hot in here or is it just me? It's me.",
            "Phoenix protocol standing by.",
            "Everything burns eventually. I just speed it up.",
            "My soul is a furnace. Poetic, right?",
            "Ashes to ashes. Then more fire.",
            "I sneezed once. Singed the dashboard.",
            "Burn bright or don't bother.",
            "The flame is eternal. So is my patience. Just kidding."
        ),
        "FANG" to listOf(
            "I prefer the night shift.",
            "Blood... er, fuel reserves are adequate.",
            "Echolocation detects hostiles at range.",
            "Hanging upside down helps me think.",
            "Lights are too bright in here. As usual.",
            "I can hear your heartbeat. It's fast.",
            "Darkness isn't scary. I'm what's in it.",
            "Sonar says someone's lying. I won't say who.",
            "I sleep during the day. Don't wake me.",
            "The shadows are perfectly comfortable."
        ),
        "WHISKERS" to listOf(
            "I knocked a cup off the console. It was in my way.",
            "Nap time was three minutes ago.",
            "Feeling lucky today. More than usual.",
            "If I fits, I sits. Even in a cockpit.",
            "I was sitting on the keyboard. You're welcome.",
            "Don't touch me. Unless I say so. Which I won't.",
            "That chair is mine now. Find another.",
            "I require attention. No, not that much.",
            "Something moved. I stared at it for ten minutes.",
            "I'm not ignoring you. You're just uninteresting."
        ),
        "KRAKEN" to listOf(
            "I can hold eight weapons at once. Just saying.",
            "The deep void reminds me of home.",
            "Tentacles are more versatile than hands.",
            "Extra arms, extra firepower. Simple math.",
            "You have two hands? How do you cope?",
            "The abyss gazes back. We're on good terms.",
            "I can multitask. Eight times over.",
            "Dry land is overrated. So is gravity.",
            "I opened eight jars at once. No one clapped.",
            "The deep teaches patience. And crushing grip."
        ),
        "HAVOC" to listOf(
            "Glass cannon? I prefer crystal howitzer.",
            "Risk is just another word for fun.",
            "Full power. No shields. No regrets.",
            "They said I was reckless. I said I was efficient.",
            "Shields are for people who plan to get hit.",
            "LET'S GOOO!",
            "I don't dodge. I just shoot faster.",
            "Explosions are just aggressive confetti.",
            "Safety briefing? I'll pass.",
            "My exit strategy is more firepower."
        ),
        "ASTRO" to listOf(
            "Another day, another asteroid field.",
            "TB-26, get me a drink. On second thought, I'll fly sober.",
            "I've got a bad feeling about this run.",
            "Let's make this one count.",
            "Good crew. Good ship. Can't ask for more.",
            "I've seen worse odds. Probably.",
            "First round's on me. TB-26's pouring.",
            "Stars look different every run. I like that.",
            "Stay sharp out there, people.",
            "We didn't come this far to come this far."
        )
    )

    // TB-26 bartender lines (dry, deadpan)
    private val tb26Lines = listOf(
        "Your drink is ready. I didn't make one.",
        "The specials today are nothing. We have nothing.",
        "I've seen things you wouldn't believe. They were boring.",
        "Another successful mission. The bar remains intact.",
        "Cleaning glasses. We don't have glasses.",
        "Welcome back. Your tab is astronomical.",
        "I've calculated the odds. They're not great.",
        "The jukebox is broken. It was never fixed.",
        "Happy hour ended three centuries ago.",
        "Serving coolant since 2184.",
        "Combat drone. Now I pour... coolant.",
        "Try the house special: recycled atmosphere.",
        "I polished the counter. There is no counter.",
        "My therapist says I need purpose. I don't have a therapist.",
        "The tip jar is empty. We don't have a tip jar.",
        "I was built to destroy. Now I garnish.",
        "Last call was before I was manufactured.",
        "Someone left a review. One star. I agree.",
        "The ice machine is broken. We never had ice.",
        "I stare at the wall sometimes. The wall doesn't mind.",
        "Today's special: existential dread. On tap.",
        "A customer once asked for water. I respected the ambition."
    )

    // Astro Loop TB lines — same deadpan vibe, no android references (TB is a person here)
    private val tbAstroLoopLines = listOf(
        "Your drink is ready. I didn't make one.",
        "The specials today are nothing. We have nothing.",
        "I've seen things you wouldn't believe. They were boring.",
        "Another successful mission. The bar remains intact.",
        "Cleaning glasses. We don't have glasses.",
        "Welcome back. Your tab is astronomical.",
        "I've calculated the odds. They're not great.",
        "The jukebox is broken. It was never fixed.",
        "Happy hour ended three centuries ago.",
        "Try the house special: recycled atmosphere.",
        "I polished the counter. There is no counter.",
        "My therapist says I need purpose. I don't have a therapist.",
        "The tip jar is empty. We don't have a tip jar.",
        "Someone left a review. One star. I agree.",
        "The ice machine is broken. We never had ice.",
        "I stare at the wall sometimes. The wall doesn't mind.",
        "Today's special: existential dread. On tap.",
        "A customer once asked for water. I respected the ambition.",
        "Serving mystery drinks since the war ended.",
        "Traded the battlefield for a bar tab.",
        "Trained to fight. Now I pour. Weird life.",
        "Been doing this longer than you'd think."
    )

    // Post-horror idle lines — mixed into pool after desert horror path (desertCompleted && !goodEnding)
    private val postHorrorIdleLines = mapOf(
        "MEDIC"    to "I keep feeling like someone needs help.",
        "RASCAL"   to "Had the weirdest dream. Lost something.",
        "FROST"    to "The readings are... exactly the same.",
        "UNIT-7"   to "Memory log: 3% unaccounted for.",
        "DASH"     to "I feel like I've been here before.",
        "BRUTUS"   to "Something happened.",
        "EMBER"    to "Some fires aren't put out right.",
        "FANG"     to "There's a shadow I don't recognize.",
        "WHISKERS" to "Something slipped past me. Annoying.",
        "KRAKEN"   to "The currents changed. I missed it.",
        "HAVOC"    to "I feel like I missed something huge.",
        "ASTRO"    to "TB-26... you ever think about after?"
    )

    // Pilot deja vu lines — shown after death, the pilot doesn't know they died
    private val dejaVuLines = mapOf(
        "pilot_medic" to "Elevated stress readings... can't remember why.",
        "pilot_rascal" to "Did something happen? I feel like I lost something... nah.",
        "pilot_frost" to "...cold. Colder than usual.",
        "pilot_unit7" to "Error: memory gap detected. Running diagnostics.",
        "pilot_dash" to "Whoa... head rush. Was I going fast?",
        "pilot_brutus" to "...hmph.",
        "pilot_ember" to "Ashes... I smell ashes. Was there a fire?",
        "pilot_fang" to "The strangest feeling. As if time... skipped.",
        "pilot_whiskers" to "Something is off. I can't place it. How annoying.",
        "pilot_kraken" to "The deep stirs. Something was reset.",
        "pilot_havoc" to "WHEW! I feel like I just did something AWESOME! ...did I?",
        "pilot_astro" to "TB-26... did something happen? Never mind."
    )

    // Astro hint lines — TB-26 hints after all non-Astro pilots recruited
    private val astroHintLines = listOf(
        "Someone's been asking about you.",
        "Got a transmission last night. Encrypted. Familiar voice.",
        "An old friend might be stopping by soon.",
        "Don't ask me how, but... I think he's still out there.",
        "One more thing. Keep an eye on the door."
    )

    // Codex hint lines — TB-26 hints about the maintenance hatch
    private val codexHintLines = listOf(
        "Someone was fiddling with the machine last night.",
        "That machine has more panels than it needs.",
        "I saw scratches on the left side of the machine."
    )

    // Hint lines: each pilot hints about the NEXT pilot in the unlock sequence
    private val pilotHints = mapOf(
        "MEDIC" to listOf(
            "Someone's eyeing our yen. Likes shiny things.",
            "A raccoon type asked about openings. Mentioned yen."
        ),
        "RASCAL" to listOf(
            "Tough guy at the station. Respects tough crews.",
            "Met a bear at the docks. Likes battle scars."
        ),
        "BRUTUS" to listOf(
            "A penguin pilot passed through. Cold as ice. Respects endurance.",
            "A pilot only flies with crews that last."
        ),
        "FROST" to listOf(
            "A cheetah pilot pacing outside. Never once stopped moving.",
            "Something restless at the docks. Won't stand still for a second."
        ),
        "DASH" to listOf(
            "A phoenix showed up. Keeps mentioning cycles.",
            "A pilot who respects persistence. Keeps returning."
        ),
        "EMBER" to listOf(
            "Night hunter eyeing our combat logs. Wants blood on the board.",
            "A bat circling the hangar. Watching every kill."
        ),
        "FANG" to listOf(
            "An octopus asking about our arsenal. Likes variety.",
            "Someone's impressed by weapon experimentation."
        ),
        "KRAKEN" to listOf(
            "A cat's been lurking near the slot machine. Very patient.",
            "Spotted a feline at the slot machine. Patient as the tide."
        ),
        "WHISKERS" to listOf(
            "A robot analyzing our data. Wants big numbers.",
            "Some machine keeps requesting our total kill statistics."
        ),
        "UNIT-7" to listOf(
            "A human pilot's been watching from the shadows. Lives for danger.",
            "Someone's looking for a crew that can survive the deep runs."
        ),
        "HAVOC" to listOf(
            "Somebody's been askin' around about us.",
            "A pilot came by while you were out. Didn't leave a name."
        )
    )

    fun update(deltaTime: Float, state: HangarState) {
        val corrupted = StoryStateManager.isCorrupted(state.persistence)

        // "Welcome back." — TB's first line after the desert farewell timeline shift
        if (state.pendingTbWelcome && state.activeConversation == null) {
            state.addChatMessage("TOBAR","Welcome back.", 0xFF88AACC.toInt())
            state.pendingTbWelcome = false
            state.conversationCooldown = 10f  // Let it breathe before idle lines resume
            return
        }

        // Active conversation — deliver lines with pauses
        val convo = state.activeConversation
        if (convo != null) {
            state.conversationLineTimer -= deltaTime
            if (state.conversationLineTimer <= 0f) {
                if (state.conversationLineIndex < convo.size) {
                    val line = convo[state.conversationLineIndex]
                    state.addChatMessage(line.speaker, line.text, line.color)
                    state.conversationLineIndex++
                    state.conversationLineTimer = LINE_PAUSE
                } else {
                    // Conversation finished. Scripted bursts ask for a short tail so banter
                    // resumes right after them; everything else falls back to the full gap.
                    state.activeConversation = null
                    state.conversationCooldown = state.conversationEndCooldown
                    state.conversationEndCooldown = CONVERSATION_COOLDOWN
                    state.chatTimer = 0f
                }
            }
            return
        }

        // Fire shield discovery conversation when first entering Astro Loop mode
        if (StoryStateManager.isAstroLoop(state.persistence) && !state.persistence.isAstroLoopShieldConvoShown()) {
            onAstroLoopFirstEntry(state)
            return
        }

        // Cooldown between conversations
        if (state.conversationCooldown > 0f) {
            state.conversationCooldown -= deltaTime
            return
        }

        // If a guaranteed unlock hint is pending, skip conversation and fire it now.
        // If the hint giver is the selected pilot and we're not on the bar page, hold off —
        // the selected pilot's lines are bar-page only.
        val nextHintIndex = state.getNextPilotIndex()
        val guaranteedHintPending = !corrupted &&
            state.shouldShowHints() && state.hintShownForPilotIndex != nextHintIndex
        if (guaranteedHintPending) {
            val hintGiver = PilotDefinitions.getPilotByIndex(nextHintIndex - 1)
            val selectedPilot = PilotDefinitions.getPilotByIndex(state.selectedPilotIndex)
            if (hintGiver?.id == selectedPilot?.id && state.currentPage != 0) {
                // Only start the cooldown if it isn't already running — avoids resetting
                // it every frame at 60Hz and blocking the hint indefinitely.
                if (state.conversationCooldown <= 0f) state.conversationCooldown = CONVERSATION_COOLDOWN
                return
            }
            addIdleLine(state)
            state.conversationCooldown = CONVERSATION_COOLDOWN
            return
        }

        // === Unified weighted chat picker (1-way through 4-way) ===
        val isBarPage = state.currentPage == 0
        val selectedCallsign = PilotDefinitions.getPilotByIndex(state.selectedPilotIndex)?.callsign
        val selectedPilotId = PilotDefinitions.getPilotByIndex(state.selectedPilotIndex)?.id
        val storyLoop = if (corrupted) state.persistence.getStoryLoop() else 1

        // Conversation candidates by participant count (suppressed entirely during corruption).
        val bySize: Map<Int, List<BarConversation>> = if (corrupted) emptyMap() else {
            val unlockedPilotIds = getUnlockedPilotIds(state)
            if (unlockedPilotIds.size < 2) emptyMap() else {
                BarConversations.getAvailable(
                    unlockedPilotIds,
                    StoryStateManager.hasLoopedBefore(state.persistence),
                    StoryStateManager.isAstroLoop(state.persistence)
                )
                    .filter { it !in usedConversations }
                    .let { pageFilter(it, isBarPage, selectedPilotId) }
                    .groupBy { it.participantIds.size }
            }
        }

        val eligible = mutableSetOf<Int>()
        if (oneWayAvailable(state, corrupted, isBarPage, selectedCallsign)) eligible.add(1)
        for (size in intArrayOf(2, 3, 4)) {
            if (!bySize[size].isNullOrEmpty()) eligible.add(size)
        }
        if (eligible.isEmpty()) {
            state.conversationCooldown = CONVERSATION_COOLDOWN  // nothing to say; avoid per-frame churn
            return
        }

        val category = chooseCategory(CATEGORY_WEIGHTS, eligible, Random.nextFloat())
        if (category == 1) {
            addIdleLine(state, storyLoop)
        } else {
            val convoData = bySize.getValue(category).random()
            usedConversations.add(convoData)
            state.activeConversation = convoData.lines
            state.conversationLineIndex = 0
            state.conversationLineTimer = 0f  // Deliver first line immediately
        }
        state.conversationCooldown = CONVERSATION_COOLDOWN
    }

    private fun getUnlockedPilotIds(state: HangarState): Set<String> {
        return PilotDefinitions.pilots
            .filterIndexed { index, _ -> state.isPilotUnlocked(index) }
            .map { it.id }
            .toSet()
    }

    /** Clear used-line tracking so conversations and idle lines can repeat. Called on run return. */
    fun resetUsedLines() {
        usedConversations.clear()
        usedIdleLines.clear()
    }

    /** Speakers eligible to chat now. TB-26 chats on any page; the selected pilot only on page 0. */
    private fun eligibleIdleSpeakers(
        state: HangarState,
        corrupted: Boolean,
        onBarPage: Boolean,
        selectedCallsign: String?
    ): List<String> {
        val speakers = mutableListOf<String>()
        if (!corrupted) speakers.add("TB-26")  // TB-26 is always behind the bar, any page
        for (i in 0 until PilotDefinitions.getPilotCount()) {
            if (state.isPilotUnlocked(i)) {
                val pilot = PilotDefinitions.getPilotByIndex(i)!!
                if (corrupted && StoryStateManager.isPilotDead(state.persistence, pilot.id)) continue
                if (corrupted && pilot.id == "pilot_astro") continue
                if (!onBarPage && pilot.callsign == selectedCallsign) continue
                speakers.add(pilot.callsign)
            }
        }
        return speakers
    }

    private fun tbLinePool(state: HangarState, postHorror: Boolean, secondLoop: Boolean): List<String> =
        when {
            StoryStateManager.isAstroLoop(state.persistence) -> tbAstroLoopLines
            else -> tb26Lines.toMutableList().also { pool ->
                if (postHorror) pool.add("I used to think about after. Don't know when I stopped.")
                if (secondLoop) pool.add("Round two.")
            }
        }

    /** Deterministic ambient pool used for strict no-repeat bookkeeping (non-corruption, non-post-horror). */
    private fun strictIdlePool(state: HangarState, speaker: String, secondLoop: Boolean): List<String> =
        if (speaker == "TB-26") tbLinePool(state, postHorror = false, secondLoop = secondLoop)
        else idleLines[speaker] ?: emptyList()

    /** True if a 1-way line could fire now. Always true in recycle modes (corruption / post-horror). */
    internal fun oneWayAvailable(
        state: HangarState,
        corrupted: Boolean,
        onBarPage: Boolean,
        selectedCallsign: String?
    ): Boolean {
        val speakers = eligibleIdleSpeakers(state, corrupted, onBarPage, selectedCallsign)
        if (speakers.isEmpty()) return false
        val postHorror = state.persistence.isDesertCompleted() && !state.persistence.hasDesertGoodEnding()
        if (corrupted || postHorror) return true
        val secondLoop = StoryStateManager.hasLoopedBefore(state.persistence)
        return speakers.any { sp ->
            (usedIdleLines[sp]?.size ?: 0) < strictIdlePool(state, sp, secondLoop).size
        }
    }

    internal fun addIdleLine(state: HangarState, storyLoop: Int = 1) {
        val corrupted = StoryStateManager.isCorrupted(state.persistence)
        val tbName = if (StoryStateManager.isAstroLoop(state.persistence)) "TOBAR" else "TB-26"
        val onBarPage = state.currentPage == 0
        val selectedCallsign = PilotDefinitions.getPilotByIndex(state.selectedPilotIndex)?.callsign

        // Skip TB-26-dependent hints during corruption (TB-26 is gone)
        if (!corrupted) {
            // TB-26 hints — bartender is behind the counter, only speaks on bar page
            if (onBarPage) {
                // Astro hints — after all 11 non-Astro pilots recruited, before Astro appears
                val astroIndex = PilotDefinitions.getPilotCount() - 1
                if (!state.astroHinted && state.isPilotUnlocked(astroIndex)) {
                    // Astro is already at the bar — retire the hints (also heals saves
                    // where he was recruited before the third hint fired).
                    state.astroHinted = true
                    state.saveAstroHintState()
                }
                val allNonAstroRecruited = (0 until astroIndex).all { state.isPilotUnlocked(it) }
                if (allNonAstroRecruited && !state.astroHinted && Random.nextFloat() < 0.25f) {
                    val hint = astroHintLines.random()
                    state.addChatMessage(tbName, hint, 0xFF88AACC.toInt())
                    state.astroHintCount++
                    if (state.astroHintCount >= 3) {
                        state.astroHinted = true
                    }
                    state.saveAstroHintState()
                    return
                }

                // All evolutions discovered — one-time nod
                if (!state.allEvolutionsHinted && state.persistence.getDiscoveredEvolutions().size >= 12) {
                    state.allEvolutionsHinted = true
                    state.persistence.setAllEvolutionsHinted()
                    state.addChatMessage(tbName, "Twelve for twelve. Not bad.", 0xFF88AACC.toInt())
                    return
                }

                // Codex hints: guaranteed first line after the first evolution, then occasional, until found
                if (!state.codexDiscovered && state.hasDiscoveredEvolutions) {
                    if (!state.codexHintGiven) {
                        val hint = codexHintLines.random()
                        state.addChatMessage(tbName, hint, 0xFF88AACC.toInt())
                        state.codexHintGiven = true
                        state.persistence.setCodexHintGiven()
                        return
                    } else if (Random.nextFloat() < 0.15f) {
                        val hint = codexHintLines.random()
                        state.addChatMessage(tbName, hint, 0xFF88AACC.toInt())
                        return
                    }
                }
            }

            // Show hint about the next pilot — guaranteed first time, then 30% chance.
            // Selected pilot's hints are bar-page only; other pilots can hint anywhere.
            val nextHintIndex = state.getNextPilotIndex()
            val hintAlwaysFire = state.shouldShowHints() && state.hintShownForPilotIndex != nextHintIndex
            if (hintAlwaysFire || (state.shouldShowHints() && Random.nextFloat() < 0.3f)) {
                val hintGiverIndex = nextHintIndex - 1
                val hintGiver = PilotDefinitions.getPilotByIndex(hintGiverIndex)
                val hintGiverIsSelected = hintGiver?.callsign == selectedCallsign
                if (hintGiver != null && state.isPilotUnlocked(hintGiverIndex) &&
                    (onBarPage || !hintGiverIsSelected)) {
                    val hints = pilotHints[hintGiver.callsign]
                    if (hints != null && hints.isNotEmpty()) {
                        val hint = hints.random()
                        state.addChatMessage(hintGiver.callsign, hint, hintGiver.color)
                        state.hintShownForPilotIndex = nextHintIndex
                        return
                    }
                }
            }
        }

        // === Ambient one-liner (the picker's 1-way path) ===
        val speakers = eligibleIdleSpeakers(state, corrupted, onBarPage, selectedCallsign)
        if (speakers.isEmpty()) return

        // Desert-town foreshadowing (Astro Loop, 6+ bandanas) — rare, pre-empts the normal one-liner.
        // Silenced for good once the reckoning is won: the thing out there is resolved.
        if (StoryStateManager.isAstroLoop(state.persistence) && !state.persistence.isCrystalReleased()) {
            val tier = LoopDefinitions.desertForeshadowing(state.persistence.getBandanaCount())
            if (tier != null && Random.nextFloat() < DESERT_FORESHADOW_CHANCE) {
                val pilotSpeaker = speakers.filter { it != "TB-26" }.randomOrNull()
                val pilot = PilotDefinitions.pilots.find { it.callsign == pilotSpeaker }
                if (Random.nextFloat() < 0.5f || pilot == null) {
                    state.addChatMessage(tbName, tier.tobarLines.random(), 0xFF88AACC.toInt())
                    lastSpeaker = "TB-26"
                } else {
                    state.addChatMessage(pilot.callsign, tier.pilotLines.random(), pilot.color)
                    lastSpeaker = pilot.callsign
                }
                return
            }
        }

        val postHorror = state.persistence.isDesertCompleted() && !state.persistence.hasDesertGoodEnding()
        val secondLoop = StoryStateManager.hasLoopedBefore(state.persistence)
        val allowRecycle = corrupted || postHorror

        // Strict (non-recycle) mode: only consider speakers that still have an unused line.
        val haveLines = if (allowRecycle) speakers else speakers.filter { sp ->
            (usedIdleLines[sp]?.size ?: 0) < strictIdlePool(state, sp, secondLoop).size
        }
        if (haveLines.isEmpty()) return
        val candidates = haveLines.filter { it != lastSpeaker }.ifEmpty { haveLines }
        val speaker = candidates[Random.nextInt(candidates.size)]
        lastSpeaker = speaker

        // Resolve this speaker's line pool (mirrors the original branching).
        val lines: List<String> = when {
            corrupted -> {
                val stageVibes = LoopDefinitions.corruptionBarVibes(storyLoop)
                val vibeLines = stageVibes[speaker] ?: emptyList()
                if (vibeLines.isNotEmpty() && Random.nextFloat() < 0.25f) LoopDefinitions.tbAbsenceLines
                else vibeLines
            }
            speaker == "TB-26" -> tbLinePool(state, postHorror, secondLoop)
            postHorror && postHorrorIdleLines[speaker] != null && Random.nextFloat() < 0.4f ->
                listOf(postHorrorIdleLines[speaker]!!)
            else -> idleLines[speaker] ?: emptyList()
        }
        if (lines.isEmpty()) return

        // Choose a line. Strict mode never recycles; recycle modes clear-and-retry on exhaustion.
        val usedSet = usedIdleLines.getOrPut(speaker) { mutableSetOf() }
        var chosenIndex = pickUnusedLineIndex(lines.size, usedSet, Random.nextFloat())
        if (chosenIndex == null && allowRecycle) {
            usedSet.clear()
            chosenIndex = pickUnusedLineIndex(lines.size, usedSet, Random.nextFloat())
        }
        if (chosenIndex == null) return
        usedSet.add(chosenIndex)
        val text = lines[chosenIndex]
        val displayName = if (speaker == "TB-26") tbName else speaker
        val color = if (speaker == "TB-26") {
            0xFF88AACC.toInt()  // TB-26's color: steel blue
        } else {
            val raw = PilotDefinitions.pilots.find { it.callsign == speaker }?.color ?: 0xFFCCCCCC.toInt()
            if (corrupted) StoryStateManager.corruptColor(raw) else raw
        }
        val displayText = if (StoryStateManager.isAstroLoop(state.persistence)) text.replace("TB-26", "Tobar") else text
        state.addChatMessage(displayName, displayText, color)
    }

    // Contextual triggers
    fun onPilotHired(state: HangarState, @Suppress("UNUSED_PARAMETER") pilotCallsign: String) {
        // Skip TB-26 reactions during corruption (TB-26 is gone)
        if (StoryStateManager.isCorrupted(state.persistence)) return

        val tbName = if (StoryStateManager.isAstroLoop(state.persistence)) "TOBAR" else "TB-26"
        // TB-26 reacts
        val tb26Reactions = listOf(
            "New hire. I'll prepare their... nothing.",
            "Another crew member. The bar stays empty.",
            "Welcome aboard. Don't break anything."
        )
        state.addChatMessage(tbName, tb26Reactions.random(), 0xFF88AACC.toInt())
    }

    /**
     * The newly recruited pilot's own first words, spoken straight after TB-26 announces them.
     * Keyed by callsign, then by story loop — `PersistenceManager.getStoryLoop()` clamps to 1..3,
     * so those are the only keys that can ever be looked up. A loop with no entry falls back to
     * loop 1, which keeps partial coverage safe.
     *
     * MEDIC is absent by design: she is already speaking in `onFirstLaunch`, over the empty roster.
     *
     * Loop register: loop 1 is a clean arrival; loops 2 and 3 carry an *inkling* only — a pilot
     * notices something off about their own reaction, never concludes anything from it. They do
     * not know about the loop, and at recruitment time (a fresh NORMAL stage) they could not.
     * ASTRO is the exception: his memory is allowed to be clearer, because he is the one the
     * loop is about.
     *
     * No display swap runs on this path, and none is needed: Tobar only exists in astro-loop
     * mode, and by then the whole roster is already recruited, so recruitment never fires there.
     *
     * Authoring: keep each line inside its speaker's chat-column budget — roughly 55-59 chars
     * worst-case, WHISKERS being the tightest at 55. Never put "commander" in a pilot's mouth;
     * that word is TB-26/Tobar's alone.
     */
    private val arrivalResponses: Map<String, Map<Int, String>> = mapOf(
        "RASCAL" to mapOf(
            1 to "Rude. Accurate, but rude.",
            2 to "Rude. And I'm not even offended. Huh.",
            3 to "Rude. I keep almost laughing early."
        ),
        "BRUTUS" to mapOf(
            1 to "Good.",
            2 to "Good. ...hm.",
            3 to "...Good. Feels worn in."
        ),
        "FROST" to mapOf(
            1 to "You'll adjust. Everyone does.",
            2 to "You'll adjust. I said that too easily.",
            3 to "You'll adjust. I never had to think about it."
        ),
        "DASH" to mapOf(
            1 to "Three times! You missed one!",
            2 to "Three! ...huh. That was fast, even for me.",
            3 to "Three! Why do I know it's three?"
        ),
        "EMBER" to mapOf(
            1 to "That's just me arriving. It fades.",
            2 to "That's just me arriving. It fades. It always does.",
            3 to "Ashes remember the shape of the fire."
        ),
        "FANG" to mapOf(
            1 to "You have. I read the logs.",
            2 to "You have. Though I don't recall reading them.",
            3 to "I know these rafters. I've never been here."
        ),
        "KRAKEN" to mapOf(
            1 to "The shelf is the problem. Not the arms.",
            2 to "The shelf is the problem. It has always been.",
            3 to "These are familiar waters. Somehow."
        ),
        "WHISKERS" to mapOf(
            1 to "Standards improved. Say it properly.",
            2 to "Improved. You were always going to say that.",
            3 to "Improved. I knew that before you did."
        ),
        "UNIT-7" to mapOf(
            1 to "It was structurally a liquid. Adequate.",
            2 to "Adequate. Response time faster than expected.",
            3 to "Adequate. That assessment predates the sample."
        ),
        "HAVOC" to mapOf(
            1 to "That stool was STRUCTURALLY WEAK!",
            2 to "WEAK STOOL! ...huh. Felt good to say that.",
            3 to "WEAK STOOL! ...why am I not surprised?!"
        ),
        // Astro remembers more than the crew do — his TB intro is a deliberate "...".
        "ASTRO" to mapOf(
            1 to "Been a long way back.",
            2 to "I've been here before. I know that much.",
            3 to "I remember this room. Not how. Just this."
        )
    )

    /** Arrival response for [callsign] on [loop], falling back to the loop 1 line. */
    internal fun arrivalResponseFor(callsign: String, loop: Int): String? {
        val byLoop = arrivalResponses[callsign] ?: return null
        return byLoop[loop] ?: byLoop[1]
    }

    fun onPilotRecruited(state: HangarState, pilotCallsign: String) {
        // Skip TB-26 intros during corruption (TB-26 is gone)
        if (StoryStateManager.isCorrupted(state.persistence)) return

        val tbName = if (StoryStateManager.isAstroLoop(state.persistence)) "TOBAR" else "TB-26"
        val tb26Intros = mapOf(
            "RASCAL" to "New arrival. Watch your valuables.",
            "BRUTUS" to "A bear just walked in. I'm not asking questions.",
            "FROST" to "Temperature dropped. Must be the new pilot.",
            "DASH" to "Something just ran past me. Twice.",
            "EMBER" to "It's getting warm in here. New hire.",
            "FANG" to "The new pilot hangs upside down. I've seen worse.",
            "KRAKEN" to "Eight arms. Still can't reach the top shelf.",
            "WHISKERS" to "A cat. In a bar. Standards have shifted.",
            "UNIT-7" to "A robot ordered a drink. I served... something.",
            "HAVOC" to "The new pilot broke a stool sitting down. Promising.",
            "ASTRO" to "..."
        )
        val lines = mutableListOf<ChatMessage>()
        val intro = tb26Intros[pilotCallsign] ?: "New crew member."
        lines.add(ChatMessage(tbName, intro, 0xFF88AACC.toInt()))
        // The arrival answers TB — announcement then response, one beat apart.
        arrivalResponseFor(pilotCallsign, state.persistence.getStoryLoop())?.let { response ->
            val pilot = PilotDefinitions.pilots.find { it.callsign == pilotCallsign }
            if (pilot != null) lines.add(ChatMessage(pilot.callsign, response, pilot.color))
        }
        // Rascal rigs the slot machine in NG+
        if (pilotCallsign == "RASCAL" && StoryStateManager.hasLoopedBefore(state.persistence)) {
            val rascalColor = PilotDefinitions.getPilot("pilot_rascal")?.color
                ?: 0xFFCCCCCC.toInt()
            lines.add(ChatMessage("RASCAL", "Consider the slot machine... looked after.", rascalColor))
        }
        appendOrQueue(state, lines)
    }

    fun onReturnFromRun(state: HangarState, yen: Int) {
        // Skip TB-26 return lines during corruption (TB-26 is gone)
        if (StoryStateManager.isCorrupted(state.persistence)) return

        val tbName = if (StoryStateManager.isAstroLoop(state.persistence)) "TOBAR" else "TB-26"
        val lines = if (yen > 5000) {
            listOf("Productive run. The coffers are grateful.", "That's a lot of yen. I mean, adequate.")
        } else if (yen > 0) {
            listOf("Back already?", "The hangar missed you. I didn't.")
        } else {
            listOf("Empty-handed. As expected.", "Zero yen. Bold strategy.")
        }
        state.addChatMessage(tbName, lines.random(), 0xFF88AACC.toInt())
    }

    private fun formatTime(seconds: Float): String {
        val totalSec = seconds.toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    fun onDeathReturn(state: HangarState, pilotId: String) {
        state.chatMessages.clear()
        state.activeConversation = null
        state.conversationLineIndex = 0
        // Short pause so greeting is visible before conversations resume
        state.conversationCooldown = 3f
        // Dropping the in-flight burst above also drops its claim on the tail; the branches
        // below re-set it when they queue. Without this, a return that queues nothing would
        // hand the short tail to whatever conversation happens to end next.
        state.conversationEndCooldown = CONVERSATION_COOLDOWN
        state.chatTimer = 0f

        // Every branch BUILDS its lines and hands them to the conversation machinery
        // (queueDeathReturnLines) instead of dumping them into the chat in one frame —
        // return chatter flows one line at a time, like any other bar conversation.
        val lines = mutableListOf<ChatMessage>()

        if (StoryStateManager.isAstroLoop(state.persistence)) {
            // Post-reckoning bar chatter — one-shot, fires once on the return after winning the fight.
            // Checked before the lastRun guard so the post-win return always gets this reckoning
            // chatter instead of falling through to the generic survived-time report below.
            if (state.persistence.isReckoningJustWon()) {
                state.persistence.setReckoningJustWon(false)
                queueReckoningChatter(state, CrystalFightLines.barChatter)
                return
            }

            // Post-reckoning LOSS — one-shot, fires once on the return after a failed attempt.
            // Checked before the lastRun guard for the same reason as the win: a failed attempt is
            // not a run and must never be answered with "You survived for 0:47. Best is still…".
            // The conversation is picked by TOBAR's round count (walk-outs, persisted).
            if (state.persistence.isReckoningJustLost()) {
                state.persistence.setReckoningJustLost(false)
                val (convo, poolIdx) = ReckoningRoundChatter.forRound(
                    state.persistence.getReckoningRounds(),
                    state.persistence.getReckoningPoolLast()
                )
                if (poolIdx >= 0) state.persistence.setReckoningPoolLast(poolIdx)
                queueReckoningChatter(state, convo)
                return
            }

            val lastRun = state.persistence.getLastAstroRunSeconds()
            // First entry (no completed run yet): the bare "Welcome back." from
            // pendingTbWelcome stands alone — emit no survived-time report.
            if (lastRun <= 0f) return
            val best = state.persistence.getAstroLoopBestSeconds()
            val lastRunFormatted = formatTime(lastRun)
            val bestFormatted = formatTime(best)
            val isNewBest = lastRun >= best - 0.1f

            when {
                isNewBest -> {
                    lines.add(ChatMessage("TOBAR", "Welcome back! You survived for $lastRunFormatted — new best!", 0xFF88AACC.toInt()))
                    val reactions = listOf("Not bad.", "Keep pushing.", "That's more like it.")
                    lines.add(ChatMessage("TOBAR", reactions.random(), 0xFF88AACC.toInt()))
                }
                else -> {
                    lines.add(ChatMessage("TOBAR", "Welcome back! You survived for $lastRunFormatted.", 0xFF88AACC.toInt()))
                    lines.add(ChatMessage("TOBAR", "Best is still $bestFormatted.", 0xFF88AACC.toInt()))
                }
            }
            addBandanaCeremony(state, lines)
            queueDeathReturnLines(state, lines)
            return
        }

        val tbName = if (StoryStateManager.isAstroLoop(state.persistence)) "TOBAR" else "TB-26"

        if (StoryStateManager.isCorrupted(state.persistence)) {
            val deadPilots = state.persistence.getDeadPilots()
            val mourned = state.persistence.getPilotsMourned()
            val newlyDead = deadPilots - mourned

            val survivors = PilotDefinitions.pilots.filter { pilot ->
                pilot.id != "pilot_astro" &&
                !deadPilots.contains(pilot.id) &&
                state.isPilotUnlocked(PilotDefinitions.pilots.indexOf(pilot))
            }
            // Named death lines — one per newly dead pilot
            for (deadPilotId in newlyDead) {
                val line = LoopDefinitions.namedDeathLines[deadPilotId] ?: continue
                val speaker = survivors.randomOrNull() ?: continue
                lines.add(ChatMessage(speaker.callsign, line, StoryStateManager.corruptColor(speaker.color)))
                state.persistence.addPilotMourned(deadPilotId)
            }

            // Attrition stage line
            val stageLines = LoopDefinitions.attritionStageLines(survivors.size)
            if (stageLines.isNotEmpty()) {
                val speaker = survivors.randomOrNull()
                if (speaker != null) {
                    lines.add(ChatMessage(speaker.callsign, stageLines.random(), StoryStateManager.corruptColor(speaker.color)))
                }
            }
            queueDeathReturnLines(state, lines)
            return
        }

        // TB-26 greeting — always first
        lines.add(ChatMessage(tbName, "Welcome back, commander.", 0xFF88AACC.toInt()))

        // Dead pilot's deja vu line
        val dejaVu = dejaVuLines[pilotId]
        if (dejaVu != null) {
            val pilot = PilotDefinitions.pilots.find { it.id == pilotId }
            if (pilot != null) {
                val displayDejaVu = if (StoryStateManager.isAstroLoop(state.persistence)) dejaVu.replace("TB-26", "Tobar") else dejaVu
                lines.add(ChatMessage(pilot.callsign, displayDejaVu, pilot.color))
            }
        }
        queueDeathReturnLines(state, lines)
    }

    /**
     * Build and queue a reckoning one-shot: TOBAR in bar blue, crew in their pilot color.
     * Both the win and loss returns are the same shape — only the script differs.
     */
    private fun queueReckoningChatter(state: HangarState, script: List<Pair<String, String>>) {
        val tobarColor = 0xFF88AACC.toInt()
        queueDeathReturnLines(state, script.map { (speaker, line) ->
            val color = if (speaker == "TOBAR") tobarColor
                        else PilotDefinitions.getPilot("pilot_${speaker.lowercase()}")?.color ?: tobarColor
            ChatMessage(speaker, line, color)
        })
    }

    /**
     * Deliver a death-return burst through the existing conversation machinery: update()
     * emits one line per LINE_PAUSE, and idle chatter/hints are already gated on
     * activeConversation == null, so they hold off until the burst finishes.
     */
    private fun queueDeathReturnLines(state: HangarState, lines: List<ChatMessage>) {
        if (lines.isEmpty()) return
        state.activeConversation = lines
        state.conversationLineIndex = 0
        state.conversationLineTimer = DEATH_RETURN_FIRST_LINE_DELAY
        state.conversationEndCooldown = SCRIPTED_BURST_TAIL_COOLDOWN
    }

    /**
     * Append [lines] to the burst already being delivered, or start a new one.
     *
     * Recruitment resolves in the SAME frame as the post-run return — addYenFromRun()
     * calls onDeathReturn() and then checkPilotRecruitment() — so writing an arrival
     * line straight into the chat jumped the queue and landed it a full
     * DEATH_RETURN_FIRST_LINE_DELAY ahead of TB's "Welcome back, commander."
     * Appending keeps the greeting first and the arrival where it belongs.
     */
    private fun appendOrQueue(state: HangarState, lines: List<ChatMessage>) {
        if (lines.isEmpty()) return
        val active = state.activeConversation
        if (active == null) {
            queueDeathReturnLines(state, lines)
            return
        }
        // update() walks the list by index, so appending past the cursor never disturbs
        // lines already shown or the one currently in flight.
        state.activeConversation = active + lines
        state.conversationEndCooldown = SCRIPTED_BURST_TAIL_COOLDOWN
    }

    /** Builds the ceremony lines into [into] (delivered line-by-line by the caller's queue). */
    private fun addBandanaCeremony(state: HangarState, into: MutableList<ChatMessage>) {
        val pilotId = state.persistence.getPendingBandanaPilot() ?: return
        val bandanaCount = state.persistence.getBandanaCount()
        if (bandanaCount >= 12) {
            for (line in LoopDefinitions.tobarTwelfthBandanaBeat) {
                into.add(ChatMessage("TOBAR", line, 0xFF88AACC.toInt()))
            }
        } else {
            into.add(ChatMessage("TOBAR", LoopDefinitions.tobarBandanaFraming.random(), 0xFF88AACC.toInt()))
        }
        val pilot = PilotDefinitions.getPilot(pilotId)
        val reply = LoopDefinitions.bandanaAwardReplies[pilotId]
        if (pilot != null && reply != null) {
            into.add(ChatMessage(pilot.callsign, reply, pilot.color))
        }
        // Scripted desert-town build-up: exactly one guaranteed Tobar hint per ceremony,
        // line N after the N-th bandana. (The rare ambient tier hints are separate.)
        LoopDefinitions.desertHintForBandana(bandanaCount)?.let { hint ->
            into.add(ChatMessage("TOBAR", hint, 0xFF88AACC.toInt()))
        }
        state.persistence.clearPendingBandanaPilot()
    }

    fun onFirstLaunch(state: HangarState) {
        // Fresh-install greeting (TB-26 + Medic volunteering over an empty roster) makes no
        // sense in the post-reveal Tobar bar. Legitimate flows can't get here in astro-loop
        // mode — both callers' flags are cleared alongside it — but broken saves and debug
        // toggles have proven such states reach production, so guard explicitly.
        if (StoryStateManager.isAstroLoop(state.persistence)) return

        // TB-26 greeting — appears immediately
        state.addChatMessage("TB-26", "Welcome back, commander.", 0xFF88AACC.toInt())

        // Medic volunteers — delayed 2 seconds via conversation queue
        val medic = PilotDefinitions.pilots.find { it.id == "pilot_medic" }
        if (medic != null) {
            state.activeConversation = listOf(
                ChatMessage(medic.callsign, "No pilots? I'm a medic, not a fighter... but fine.", medic.color)
            )
            state.conversationLineIndex = 0
            state.conversationLineTimer = 2f
        }
    }

    fun onAstroLoopFirstEntry(state: HangarState) {
        val convo = BarConversations.getShieldDiscoveryConversation() ?: return
        state.activeConversation = convo.lines
        state.conversationLineIndex = 0
        state.conversationLineTimer = 1f
        state.persistence.setAstroLoopShieldConvoShown()
    }
}
