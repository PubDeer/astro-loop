package com.astroloop.game.hangar

import android.graphics.RectF
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.data.PilotDef
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.PilotUnlockType
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.core.StoryStateManager
import java.util.concurrent.CopyOnWriteArrayList

enum class HangarPhase {
    BROWSING,           // Normal state, scrolling ships/pilots
    LAUNCHING,          // Launch sequence playing
    CODEX               // Viewing codex
}

data class ChatMessage(
    val speaker: String,
    val text: String,
    val color: Int
)

data class WalkerNPC(
    val pilotIndex: Int,
    val color: Int,
    var x: Float,          // Normalized 0..1 within bar walkway
    var targetX: Float,
    var walking: Boolean,
    var idleTimer: Float,  // Seconds until picking new target
    var armRaiseTimer: Float = 0f,
    var seated: Boolean = false,      // parked and sitting at a stool (ASTRO_LOOP)
    var seatedStool: Int = -1,        // stool index 2/3/5/7 when seated, else -1
    var pendingStool: Int = -1        // stool index this walker is en route to, else -1
)

class HangarState(internal val persistence: PersistenceManager) {

    @Volatile var phase: HangarPhase = HangarPhase.BROWSING

    // --- Page navigation ---
    @Volatile var currentPage: Int = 1            // 0=bar, 1=shipyard, 2=store
    @Volatile var pageScrollOffset: Float = 0f    // Pixel offset for smooth page swiping
    @Volatile var pageVelocity: Float = 0f        // For momentum page swiping
    @Volatile var swayMomentum: Float = 0f         // Decaying momentum for lamp sway effect

    // --- Ship selection (shipyard page) ---
    @Volatile var selectedShipIndex: Int = 0
    @Volatile var shipScrollOffset: Float = 0f   // Pixel offset for smooth ship switching

    // --- Drag-to-launch ---
    @Volatile var shipDragY: Float = 0f           // Current Y of ship being dragged
    @Volatile var isDraggingShip: Boolean = false
    @Volatile var shipRestingY: Float = 0f        // Resting position below walkway

    // --- Pilot selection (bar page grid) ---
    @Volatile var selectedPilotIndex: Int = 0
    @Volatile var pendingRecruitPilotIndex: Int = -1
    val pilotCardFades: FloatArray = FloatArray(12) { 1f }  // Per-card alpha (1=full, dim otherwise)
    // Pilot card flip animation (tap selected pilot to reveal passive effect)
    @Volatile var pilotFlipIndex: Int = -1
    @Volatile var pilotFlipTimer: Float = 0f
    @Volatile var pilotFlipProgress: Float = 0f
    @Volatile var pilotFlipShowBack: Boolean = false
    // No scroll needed — tappable grid on bar page

    // --- Pilot walker (world-space pixel coordinates spanning 3 pages) ---
    @Volatile var pilotX: Float = 0f              // World X position in pixels
    @Volatile var pilotTargetX: Float = 0f
    @Volatile var pilotWalking: Boolean = false
    var pilotScreenWidth: Float = 0f              // Set before initialize()
    // Width of one hangar room in design units — also the page stride. Set alongside
    // pilotScreenWidth before initialize(). Equals pilotScreenWidth below sw600dp.
    var roomWidth: Float = 0f

    // --- NPC walkers (bar page) ---
    var npcWalkers: CopyOnWriteArrayList<WalkerNPC> = CopyOnWriteArrayList()
    val pendingNPCAdds: MutableList<WalkerNPC> = java.util.Collections.synchronizedList(mutableListOf())
    val pendingNPCRemoves: MutableList<Int> = java.util.Collections.synchronizedList(mutableListOf())

    // --- Seated crew (bar stools, ASTRO_LOOP only) ---
    private val SEATABLE_STOOLS = listOf(2, 3, 5, 7)

    /** Lowest stool in the seatable set not present in [occupied]; -1 if all taken. */
    fun lowestFreeStool(occupied: Set<Int>): Int =
        SEATABLE_STOOLS.firstOrNull { it !in occupied } ?: -1

    /** Screen x of stool [stool] mapped into the walker's normalized 0..1 band. */
    fun stoolNormalizedX(stool: Int): Float {
        val w = HangarMetrics.effectiveRoomWidth(roomWidth, pilotScreenWidth)
        val stoolScreenX = HangarMetrics.stoolCenterX(w, stool)
        return (stoolScreenX - w * 0.1f) / (w * 0.8f)
    }

    // --- Chat log (bar page) ---
    val chatMessages: CopyOnWriteArrayList<ChatMessage> = CopyOnWriteArrayList()
    @Volatile var chatTimer: Float = 0f

    fun addChatMessage(speaker: String, text: String, color: Int) {
        val recent = chatMessages.takeLast(10)
        if (recent.none { it.text == text }) {
            chatMessages.add(ChatMessage(speaker, text, color))
            while (chatMessages.size > 20) chatMessages.removeAt(0)
        }
    }

    // --- Conversation queue ---
    var activeConversation: List<ChatMessage>? = null
    var conversationLineIndex: Int = 0
    var conversationLineTimer: Float = 0f
    var conversationCooldown: Float = 15f  // Start with 15s delay before first conversation
    // Cooldown applied when the CURRENT conversation finishes. Scripted bursts (the post-run
    // return, reckoning chatter, a recruitment) drop this to a short tail so the bar doesn't
    // go silent for a full CONVERSATION_COOLDOWN right after the scripted lines land.
    // Reset to the default every time a conversation ends.
    var conversationEndCooldown: Float = ChatSystem.CONVERSATION_COOLDOWN

    // --- Discovered evolutions (for codex book in bar) ---
    @Volatile var hasDiscoveredEvolutions: Boolean = false

    // --- Slot machine (store page) ---
    @Volatile var isSpinning: Boolean = false
    var reelValues: IntArray = IntArray(3)          // Target symbol index per reel
    var reelStopTimes: LongArray = LongArray(3)     // Stagger timestamps
    @Volatile var spinResultYen: Int = 0            // Payout amount for display
    @Volatile var spinResultUpgrade: String? = null  // Upgrade name if jackpot
    @Volatile var spinResultSymbol: Int = -1         // Symbol that landed (SYM_* constant)
    @Volatile var spinResultTime: Long = 0          // When result landed (for fade-out)
    @Volatile var reelPhases: FloatArray = FloatArray(3) // Animation scroll offset per reel

    // --- TB-26 bartender bar movement ---
    @Volatile var tb26BarX: Float = 0f
    @Volatile var tb26BarTargetX: Float = 0f
    @Volatile var tb26BarMoving: Boolean = false
    @Volatile var tb26BarPauseTimer: Float = 0f

    // --- Beer sliding ---
    @Volatile var beerActive: Boolean = false
    @Volatile var beerX: Float = 0f
    @Volatile var beerTargetX: Float = 0f
    @Volatile var beerTimer: Float = 0f
    @Volatile var beerInterval: Float = 12f
    @Volatile var beerTargetPilotIndex: Int = -1
    @Volatile var beerFading: Boolean = false
    @Volatile var beerFadeAlpha: Float = 1f

    // --- Astro hints (TB-26 hints after all non-Astro pilots recruited) ---
    @Volatile var astroHintCount: Int = 0
    @Volatile var astroHinted: Boolean = false
    @Volatile var allEvolutionsHinted: Boolean = false

    // --- Pilot hint tracking (guaranteed first show) ---
    var hintShownForPilotIndex: Int = -1

    // --- Corrupted Astro at slot machine ---
    @Volatile var astroAtSlotMachine: Boolean = false
    var astroAutoSpinTimer: Float = 0f
    var astroAutoSpinInterval: Float = 5f + kotlin.random.Random.nextFloat() * 3f

    // --- Crystal reveal animation (store page, after 11th corrupted pilot dies) ---
    enum class CrystalRevealPhase { NONE, GLOW, ORB_TRAVEL, FLASH, DONE }
    @Volatile var crystalRevealPhase = CrystalRevealPhase.NONE
    var crystalRevealTimer = 0f
    @Volatile var awaitingCrystalReveal = false

    // --- Codex secret (maintenance hatch on slot machine) ---
    @Volatile var codexDiscovered: Boolean = false
    @Volatile var codexHintGiven: Boolean = false
    @Volatile var hatchTapCount: Int = 0
    @Volatile var hatchOpen: Boolean = false
    @Volatile var showCodex: Boolean = false
    var hatchRect: RectF? = null
    var paperRect: RectF? = null
    var audioMuted: Boolean = false
    var vibrationMuted: Boolean = false
    var audioMuteButtonRect: RectF? = null
    var vibrationMuteButtonRect: RectF? = null

    // --- Fade from black (corruption death return) ---
    @Volatile var fadeFromBlackTimer: Float = 0f
    @Volatile var glitchTimer: Float = 0f

    // --- Desert farewell → Astro Loop first entry ---
    @Volatile var pendingTbWelcome: Boolean = false

    // --- First-launch intro cinematic ---
    @Volatile var introCinematic: Boolean = false   // True only during the first-ever launch intro
    @Volatile var introTitleTimer: Float = 0f        // Seconds the ASTRO LOOP title has been fading in

    // --- Launch sequence ---
    @Volatile var launchProgress: Float = 0f
    @Volatile var launchPhase: Int = 0

    // --- Yen display (for animation) ---
    @Volatile var displayedYen: Int = 0
    @Volatile var actualYen: Int = 0
    private var yenAnimStartValue: Int = 0
    private var yenAnimTargetValue: Int = 0
    private var yenAnimStartTime: Long = 0L
    private val yenAnimDuration: Float = 3f

    fun updatePilotWalker(deltaTime: Float) {
        val walkWidth = HangarMetrics.effectiveRoomWidth(roomWidth, pilotScreenWidth)
        val baseSpeed = walkWidth * 0.70f  // Pixels per second
        val walkSpeed = when {
            // Intro cinematic: deliberately slow so the walk reads as a cinematic beat (~3.4s)
            introCinematic -> walkWidth * 0.20f
            StoryStateManager.isCorrupted(persistence) -> baseSpeed * 0.5f
            else -> baseSpeed
        }
        if (pilotWalking) {
            val diff = pilotTargetX - pilotX
            if (kotlin.math.abs(diff) < walkSpeed * deltaTime) {
                pilotX = pilotTargetX
                pilotWalking = false
            } else {
                pilotX += if (diff > 0) walkSpeed * deltaTime else -walkSpeed * deltaTime
            }
        }
    }

    fun updateNPCWalkers(deltaTime: Float) {
        // Drain pending changes from UI thread before iterating
        if (pendingNPCAdds.isNotEmpty()) {
            npcWalkers.addAll(pendingNPCAdds)
            pendingNPCAdds.clear()
        }
        if (pendingNPCRemoves.isNotEmpty()) {
            npcWalkers.removeAll { it.pilotIndex in pendingNPCRemoves }
            pendingNPCRemoves.clear()
        }

        val baseNpcSpeed = 0.105f
        val corruptedNow = StoryStateManager.isCorrupted(persistence)
        val seatingNow = BarDressing.forStage(StoryStateManager.stage(persistence)).seatedCrew
        val walkSpeed = if (corruptedNow) baseNpcSpeed * 0.5f else baseNpcSpeed
        for (npc in npcWalkers) {
            if (!seatingNow) {
                // No seating this stage (corruption); clear any stale seated state.
                npc.seated = false; npc.seatedStool = -1; npc.pendingStool = -1
            }
            if (npc.walking) {
                val diff = npc.targetX - npc.x
                if (kotlin.math.abs(diff) < walkSpeed * deltaTime) {
                    npc.x = npc.targetX
                    npc.walking = false
                    npc.idleTimer = kotlin.random.Random.nextFloat() * 3f + 1.5f
                    if (npc.pendingStool >= 0) {           // arrived at a stool → sit
                        npc.seated = true
                        npc.seatedStool = npc.pendingStool
                        npc.pendingStool = -1
                    }
                } else {
                    npc.x += if (diff > 0) walkSpeed * deltaTime else -walkSpeed * deltaTime
                }
            } else {
                npc.idleTimer -= deltaTime
                if (npc.idleTimer <= 0f) {
                    // Choosing a new destination: stand up and clear this walker's claim first.
                    npc.seated = false
                    npc.seatedStool = -1
                    npc.pendingStool = -1
                    val stool = if (seatingNow) {
                        val occupied = npcWalkers.mapNotNull { other ->
                            when {
                                other === npc -> null
                                other.seated -> other.seatedStool
                                other.pendingStool >= 0 -> other.pendingStool
                                else -> null
                            }
                        }.toSet()
                        lowestFreeStool(occupied)
                    } else -1
                    if (stool >= 0) {
                        npc.pendingStool = stool
                        npc.targetX = stoolNormalizedX(stool)
                    } else {
                        npc.targetX = kotlin.random.Random.nextFloat() * 0.8f + 0.1f
                    }
                    npc.walking = true
                }
            }
        }
    }

    /** World X target for each page (pilot stands near the archway connecting to shipyard) */
    fun getPilotWorldTarget(page: Int): Float {
        // Pages tile at roomWidth, so world positions must step by the same stride.
        val stride = HangarMetrics.effectiveRoomWidth(roomWidth, pilotScreenWidth)
        val margin = stride * 0.1f
        val walkable = stride * 0.8f
        return when (page) {
            0 -> margin + 0.9f * walkable                       // Right side of bar
            1 -> stride + margin + 0.5f * walkable              // Center of shipyard
            2 -> {
                val base = 2f * stride + margin + 0.1f * walkable   // Left side of store
                if (astroAtSlotMachine) base - 35f
                else base
            }
            else -> stride + margin + 0.5f * walkable
        }
    }

    fun setPageTarget(page: Int) {
        currentPage = page
        pilotTargetX = getPilotWorldTarget(page)
        if (pilotTargetX != pilotX) {
            pilotWalking = true
        }
    }

    fun updateYenDisplay(deltaTime: Float) {
        if (displayedYen != actualYen) {
            if (yenAnimStartTime == 0L || actualYen != yenAnimTargetValue) {
                // Start new animation (or restart if target changed mid-animation)
                yenAnimStartValue = displayedYen
                yenAnimTargetValue = actualYen
                yenAnimStartTime = System.currentTimeMillis()
            }
            val elapsed = (System.currentTimeMillis() - yenAnimStartTime) / 1000f
            val progress = (elapsed / yenAnimDuration).coerceIn(0f, 1f)
            val totalDiff = actualYen - yenAnimStartValue
            displayedYen = yenAnimStartValue + (totalDiff * progress).toInt()
            if (progress >= 1f) {
                displayedYen = actualYen
                yenAnimStartTime = 0L
            }
        }
    }

    fun initialize() {
        // First-launch intro cinematic is active until the first launch ever commits.
        introCinematic = !persistence.isIntroDone()
        introTitleTimer = 0f

        // Load from persistence
        val savedShipId = persistence.getSelectedShipId()
        selectedShipIndex = ShipDefinitions.ships.indexOfFirst { it.id == savedShipId }.coerceAtLeast(0)

        val savedPilotId = persistence.getSelectedPilotId()
        selectedPilotIndex = PilotDefinitions.pilots.indexOfFirst { it.id == savedPilotId }.coerceAtLeast(0)

        // Corruption state: auto-select Astro+Specter when crystal unlocked and all crew dead
        if (StoryStateManager.isCorrupted(persistence)) {
            if (persistence.isCrystalUnlocked() && StoryStateManager.allCrewDead(persistence)) {
                if (persistence.isAwaitingCrystalReveal()) {
                    // Crystal reveal not yet played — no pilot, Specter on carousel
                    selectedPilotIndex = -1
                    awaitingCrystalReveal = true
                    crystalRevealPhase = CrystalRevealPhase.GLOW
                    val specterIndex = ShipDefinitions.ships.indexOfFirst { it.id == "ship_white" }
                    if (specterIndex >= 0) selectedShipIndex = specterIndex
                } else {
                    val astroIndex = PilotDefinitions.pilots.indexOfFirst { it.id == "pilot_astro" }
                    val specterIndex = ShipDefinitions.ships.indexOfFirst { it.id == "ship_white" }
                    if (astroIndex >= 0) selectedPilotIndex = astroIndex
                    if (specterIndex >= 0) selectedShipIndex = specterIndex
                }
            } else {
                // If currently selected pilot/ship is dead, find first available
                if (!isPilotUnlocked(selectedPilotIndex)) {
                    val firstAvailable = (0 until PilotDefinitions.getPilotCount()).firstOrNull { isPilotUnlocked(it) }
                    if (firstAvailable != null) selectedPilotIndex = firstAvailable
                }
                if (!isShipUnlocked(selectedShipIndex)) {
                    val firstAvailable = (0 until ShipDefinitions.ships.size).firstOrNull { isShipUnlocked(it) }
                    if (firstAvailable != null) selectedShipIndex = firstAvailable
                }
            }
        }

        // Astro Loop Mode: default to Astro + Specter only on first entry;
        // otherwise keep the saved (last-flown) selection loaded above.
        if (StoryStateManager.isAstroLoop(persistence) && persistence.isAstroLoopFirstEntry()) {
            val astroIndex = PilotDefinitions.pilots.indexOfFirst { it.id == "pilot_astro" }
            val specterIndex = ShipDefinitions.ships.indexOfFirst { it.id == "ship_white" }
            if (astroIndex >= 0) selectedPilotIndex = astroIndex
            if (specterIndex >= 0) selectedShipIndex = specterIndex
            // Boot-path twin of resetForReturn's fadeFromWhite branch: if the app was
            // restarted between the timeline shift and the hangar, TB's "Welcome back."
            // must still fire — and the first-entry flag is consumed so it fires once.
            pendingTbWelcome = true
            persistence.clearAstroLoopFirstEntry()
        }

        // Initialize NPC walkers for all unlocked pilots except selected
        rebuildNpcWalkers()

        actualYen = persistence.getYen()
        displayedYen = actualYen

        hasDiscoveredEvolutions = persistence.getDiscoveredEvolutions().isNotEmpty()

        // Initialize TB-26 bartender position at center of counter
        tb26BarX = HangarMetrics.effectiveRoomWidth(roomWidth, pilotScreenWidth) / 2f
        tb26BarTargetX = tb26BarX
        tb26BarMoving = false
        tb26BarPauseTimer = 1f + kotlin.random.Random.nextFloat() * 1.5f

        // Load codex discovery state
        codexDiscovered = persistence.isCodexDiscovered()
        hatchOpen = persistence.isCodexDiscovered()  // Stay open if already discovered
        codexHintGiven = persistence.isCodexHintGiven()

        // Load mute state
        audioMuted = persistence.isAudioMuted()
        vibrationMuted = persistence.isVibrationMuted()

        // Load Astro hint state
        astroHintCount = persistence.getAstroHintCount()
        astroHinted = persistence.isAstroHinted()
        allEvolutionsHinted = persistence.isAllEvolutionsHinted()

        // Astro hangs at slot machine in corruption (before all crew are dead, or during crystal reveal)
        astroAtSlotMachine = StoryStateManager.isCorrupted(persistence) &&
            (!StoryStateManager.allCrewDead(persistence) || awaitingCrystalReveal)

        // Start page: the bar during the intro cinematic, otherwise the shipyard.
        // Forcing the bar here keeps the opening robust whenever initialize() re-runs on
        // a full view rebuild (config change / process death). An ordinary pause/resume
        // does not re-run initialize(), so it leaves an in-progress cinematic in place.
        val startPage = if (introCinematic) 0 else 1
        currentPage = startPage
        pilotX = getPilotWorldTarget(startPage)
        pilotTargetX = pilotX
        pilotWalking = false
    }

    fun getSelectedShip() = ShipDefinitions.getShipByIndex(selectedShipIndex)
    fun getSelectedPilot() = PilotDefinitions.getPilotByIndex(selectedPilotIndex)

    fun isShipUnlocked(index: Int): Boolean {
        val ship = ShipDefinitions.getShipByIndex(index) ?: return false
        if (!persistence.isShipUnlocked(ship.id)) return false
        // Corruption gating: dead ships and Specter (until crystal unlocked)
        if (StoryStateManager.isCorrupted(persistence)) {
            if (ship.id == "ship_white" && !persistence.isCrystalUnlocked()) return false
            if (StoryStateManager.isShipDead(persistence, ship.id)) return false
        }
        return true
    }

    fun canUnlockShip(index: Int): Boolean {
        // First ship is always unlockable (starter ship)
        if (index == 0) return true
        // In corruption, Specter is gated by crystal, not purchase
        val ship = ShipDefinitions.getShipByIndex(index)
        if (ship != null && StoryStateManager.isCorrupted(persistence) && ship.id == "ship_white" && !persistence.isCrystalUnlocked()) return false
        // Can only unlock if previous ship is unlocked (sequential)
        return isShipUnlocked(index - 1)
    }

    fun isPilotUnlocked(index: Int): Boolean {
        val pilot = PilotDefinitions.getPilotByIndex(index) ?: return false
        if (!persistence.isPilotUnlocked(pilot.id)) return false
        // Corruption gating: dead pilots and Astro (until crystal unlocked)
        if (StoryStateManager.isCorrupted(persistence)) {
            if (pilot.id == "pilot_astro" && (!persistence.isCrystalUnlocked() || awaitingCrystalReveal)) return false
            if (StoryStateManager.isPilotDead(persistence, pilot.id)) return false
        }
        return true
    }

    fun isPilotDeadInCorruption(index: Int): Boolean {
        val pilot = PilotDefinitions.getPilotByIndex(index) ?: return false
        if (!StoryStateManager.isCorrupted(persistence)) return false
        if (!persistence.isPilotUnlocked(pilot.id)) return false
        return StoryStateManager.isPilotDead(persistence, pilot.id)
    }

    fun canUnlockPilot(index: Int): Boolean {
        if (index == 0) return true
        if (index != persistence.getNextPilotIndex()) return false
        return isPilotUnlocked(index - 1)
    }

    fun checkPilotUnlockCondition(): Boolean {
        val nextIndex = persistence.getNextPilotIndex()
        if (nextIndex >= PilotDefinitions.getPilotCount()) return false

        val pilot = PilotDefinitions.getPilotByIndex(nextIndex) ?: return false

        // Post-desert: one pilot per run, except Astro keeps ALL_OTHERS + 2-run cooldown
        if (StoryStateManager.hasLoopedBefore(persistence)) {
            if (pilot.unlockType != PilotUnlockType.ALL_OTHERS) {
                return persistence.getRunsSincePilotUnlock() >= 1
            }
        }

        val runsSinceUnlock = persistence.getRunsSincePilotUnlock()
        if (runsSinceUnlock < 2) return false

        if (runsSinceUnlock >= 17
            && pilot.unlockType != PilotUnlockType.ALL_OTHERS
            && pilot.unlockType != PilotUnlockType.JACKPOT) return true
        return when (pilot.unlockType) {
            PilotUnlockType.FREE -> true
            PilotUnlockType.TOTAL_YEN_EARNED -> persistence.getTotalYenEarned() >= pilot.unlockThreshold
            PilotUnlockType.TOTAL_DAMAGE_TAKEN -> persistence.getTotalDamageTaken() >= pilot.unlockThreshold
            PilotUnlockType.SURVIVE_SECONDS -> persistence.getBestSurvivalSeconds() >= pilot.unlockThreshold
            PilotUnlockType.KILL_STREAK -> persistence.getBestKillStreak() >= pilot.unlockThreshold
            PilotUnlockType.TOTAL_DEATHS -> persistence.getTotalDeaths() >= pilot.unlockThreshold
            PilotUnlockType.KILLS_IN_SINGLE_RUN -> persistence.getBestSingleRunKills() >= pilot.unlockThreshold
            PilotUnlockType.WEAPONS_DISCOVERED -> persistence.getWeaponsDiscovered().size >= pilot.unlockThreshold
            PilotUnlockType.EVOLUTIONS_DISCOVERED -> persistence.getDiscoveredEvolutions().size >= pilot.unlockThreshold
            PilotUnlockType.TOTAL_KILLS -> persistence.getTotalKills() >= pilot.unlockThreshold
            PilotUnlockType.JACKPOT -> false  // Unlocked via slot machine jackpot
            PilotUnlockType.CONTINUOUS_FLIGHT_SECONDS -> persistence.getBestContinuousFlightSeconds() >= pilot.unlockThreshold
            PilotUnlockType.ALL_OTHERS -> {
                for (i in 0 until PilotDefinitions.getPilotCount() - 1) {
                    if (!isPilotUnlocked(i)) return false
                }
                for (i in 0 until ShipDefinitions.getShipCount()) {
                    if (!isShipUnlocked(i)) return false
                }
                true
            }
        }
    }

    fun recruitNextPilot(): PilotDef? {
        val nextIndex = persistence.getNextPilotIndex()
        val pilot = PilotDefinitions.getPilotByIndex(nextIndex) ?: return null
        persistence.unlockPilot(pilot.id)
        persistence.setNextPilotIndex(nextIndex + 1)
        persistence.resetRunsSincePilotUnlock()
        return pilot
    }

    fun isWhiskersJackpotEligible(): Boolean {
        val nextIndex = persistence.getNextPilotIndex()
        val pilot = PilotDefinitions.getPilotByIndex(nextIndex) ?: return false
        if (pilot.unlockType != PilotUnlockType.JACKPOT) return false
        return persistence.getRunsSincePilotUnlock() >= 1
    }

    fun shouldShowHints(): Boolean {
        val nextIndex = persistence.getNextPilotIndex()
        if (nextIndex >= PilotDefinitions.getPilotCount()) return false
        return persistence.getRunsSincePilotUnlock() >= 1
    }

    fun getNextPilotIndex(): Int = persistence.getNextPilotIndex()

    fun isReadyToLaunch(): Boolean {
        return isShipUnlocked(selectedShipIndex) && isPilotUnlocked(selectedPilotIndex)
    }

    /**
     * (Re)build the walker roster from currently unlocked pilots, minus the selected
     * one (corruption also filters dead pilots and Astro). Stale pending walker
     * mutations are dropped alongside the list they were meant for. Must be called
     * with selectedPilotIndex already final. Runs at initialize(), on every
     * corruption return (via initCorruptionState) and on every astro-loop return —
     * without the astro-loop rebuild, the empty end-of-corruption roster leaks into
     * the freshly-revived bar and pilots only reappear as the player cycles them.
     */
    fun rebuildNpcWalkers() {
        val newWalkers = mutableListOf<WalkerNPC>()
        val npcRandom = kotlin.random.Random(System.currentTimeMillis())
        val isCorrupted = StoryStateManager.isCorrupted(persistence)
        for (i in 0 until PilotDefinitions.getPilotCount()) {
            if (i != selectedPilotIndex && isPilotUnlocked(i)) {
                val pilot = PilotDefinitions.getPilotByIndex(i) ?: continue
                // Skip dead pilots in corruption state
                if (isCorrupted && StoryStateManager.isPilotDead(persistence, pilot.id)) continue
                // Skip Astro in corruption (he's at the slot machine / unavailable until crystal)
                if (isCorrupted && pilot.id == "pilot_astro") continue
                newWalkers.add(WalkerNPC(
                    pilotIndex = i,
                    color = pilot.color,
                    x = npcRandom.nextFloat() * 0.8f + 0.1f,
                    targetX = npcRandom.nextFloat() * 0.8f + 0.1f,
                    walking = false,
                    idleTimer = npcRandom.nextFloat() * 2.5f + 1f
                ))
            }
        }
        npcWalkers = CopyOnWriteArrayList(newWalkers)
        pendingNPCAdds.clear()
        pendingNPCRemoves.clear()
    }

    /**
     * Reinitialize hangar state for corruption phase.
     * Called on return from the boss-victory run (first time corruption activates)
     * and on subsequent corruption-phase returns.
     * Rebuilds NPC walkers to exclude dead pilots and auto-selects Astro+Specter
     * when the crystal is unlocked and all crew are dead.
     */
    fun initCorruptionState(persistence: PersistenceManager) {
        // Astro hangs at slot machine in corruption (before all crew are dead, or during crystal reveal)
        astroAtSlotMachine = !StoryStateManager.allCrewDead(persistence) || persistence.isAwaitingCrystalReveal()

        // Auto-select Astro+Specter when crystal unlocked and all crew dead
        if (persistence.isCrystalUnlocked() && StoryStateManager.allCrewDead(persistence)) {
            if (persistence.isAwaitingCrystalReveal()) {
                // Crystal reveal not yet played — no pilot, Specter on carousel
                selectedPilotIndex = -1
                awaitingCrystalReveal = true
                crystalRevealPhase = CrystalRevealPhase.GLOW
                val specterIndex = ShipDefinitions.ships.indexOfFirst { it.id == "ship_white" }
                if (specterIndex >= 0) selectedShipIndex = specterIndex
            } else {
                // Reveal already played — select Astro+Specter normally
                val astroIndex = PilotDefinitions.pilots.indexOfFirst { it.id == "pilot_astro" }
                val specterIndex = ShipDefinitions.ships.indexOfFirst { it.id == "ship_white" }
                if (astroIndex >= 0) selectedPilotIndex = astroIndex
                if (specterIndex >= 0) selectedShipIndex = specterIndex
            }
        } else {
            // If currently selected pilot/ship is now dead, find first available
            if (!isPilotUnlocked(selectedPilotIndex)) {
                val firstAvailable = (0 until PilotDefinitions.getPilotCount()).firstOrNull { isPilotUnlocked(it) }
                if (firstAvailable != null) selectedPilotIndex = firstAvailable
            }
            if (!isShipUnlocked(selectedShipIndex)) {
                val firstAvailable = (0 until ShipDefinitions.ships.size).firstOrNull { isShipUnlocked(it) }
                if (firstAvailable != null) selectedShipIndex = firstAvailable
            }
        }

        // After selection is final — dead pilots and Astro filtered out by isPilotUnlocked()
        rebuildNpcWalkers()
    }

    fun saveAstroHintState() {
        persistence.setAstroHintCount(astroHintCount)
        persistence.setAstroHinted(astroHinted)
    }

    fun saveSelection() {
        getSelectedShip()?.let { persistence.setSelectedShipId(it.id) }
        getSelectedPilot()?.let { persistence.setSelectedPilotId(it.id) }
    }
}
