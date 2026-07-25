package com.astroloop.game.core

import com.astroloop.game.entity.Entity

enum class GamePhase {
    PLAYING,
    UPGRADE_SELECTION,
    DEATH_PLAY_OUT,
    CRYSTAL_DEATH,
    HEART_TO_HEART,
    DESERT,
    DESERT_FAREWELL,
    TIMELINE_SHIFT,
    WAKE_UP,
    GAME_OVER,
    GAME_BRICKED
}

class GameState {
    var phase: GamePhase = GamePhase.PLAYING
    var survivalTime: Float = 0f
    var bestTime: Float = 0f
    var isPaused: Boolean = false
    var graceTimer: Float = 0.5f
    var debugMenuOpen: Boolean = false
    var difficultyMultiplier: Float = 1f
    var isPostHorrorRun: Boolean = false  // desertCompleted && !hasDesertGoodEnding
    var retreatPhase: Int = 0  // 0=idle, 1=radio+stop_spawns, 2=flying_south, 3=fading
    var retreatTimer: Float = 0f
    var emergencyShieldActive: Boolean = false

    var debugMenuPage: Int = 0  // 0=Weapons/Passives, 1=Evolutions/Resets, 2=Story, 3=Flak Designs
    var flakDesignIndex: Int = 12  // which FlakDesigns index to use for FLAK_EXPLOSION

    // Phase 4 debug display (updated each frame from persistence)
    var debugStoryPhase: Int = 0
    var debugReckoningRounds: Int = 0
    var debugDeadPilotCount: Int = 0
    var debugCrystalUnlocked: Boolean = false
    var debugArcCompleted: Boolean = false
    var debugCrystalBroken: Boolean = false
    var debugStoryLoop: Int = 1
    var debugDesertCompleted: Boolean = false
    var storyLoop: Int = 1
    var debugDesertGoodEnding: Boolean = false
    var debugAstroLoopMode: Boolean = false
    var activePilotHasBandana: Boolean = false  // mirror of persistence.hasBandana(activePilotId), set in initialize()

    // Evolution cap: max 1 evolution per game run
    var hasEvolvedThisGame: Boolean = false

    // Screen dimensions (set by GameSurfaceView)
    var screenWidth: Float = 1080f
    var screenHeight: Float = 1920f

    // Points system
    var score: Int = 0
    var highScore: Int = 0
    var goldCollected: Int = 0  // Gold collected this run

    // Track upgrade counts for ship evolution
    var totalUpgradesCollected: Int = 0

    // Telemetry: per-minute counters (reset each snapshot)
    var telemetryDamageByWeapon = mutableMapOf<String, Float>()
    var telemetryDamageTakenBy = mutableMapOf<String, Float>()
    var telemetryCritsThisMinute = 0
    var telemetryPowerupsCollected = mutableMapOf<String, Int>()
    var telemetryDodges = 0

    // Telemetry: per-run counters (reset on run start)
    var telemetrySnapshotTimer = 0f
    var telemetryYenFromAsteroids = 0
    var telemetryYenFromEnemies = 0
    var telemetryAsteroidsDestroyed = 0
    var telemetryCritsTotal = 0
    var telemetryUpgradeDropsCollected = 0
    var telemetryDiamondsCollected = 0
    var telemetryTotalDamageDealt = 0f
    var telemetryTotalDamageTaken = 0f
    var lastDamageSource = "unknown"
    var telemetryLastOfferedOptions: List<String> = emptyList()

    // Starting loadout (set from hangar selection)
    var startingWeaponId: String = "pulse_cannon"
    var startingPassiveId: String = "nano_repair"

    // Current equipment (LinkedHashMap preserves insertion order for HUD display)
    val weaponLevels = linkedMapOf<String, Int>()
    val passiveStacks = linkedMapOf<String, Int>()
    val evolvedWeapons = mutableSetOf<String>()

    // Shield state
    var baseShields: Float = GameConfig.SHIP_BASE_SHIELDS
    var shieldRegenRate: Float = GameConfig.SHIELD_REGEN_RATE
    var shieldRegenDelay: Float = GameConfig.SHIELD_REGEN_DELAY
    var shieldCapMultiplier: Float = 1f

    // Computed stats from passives
    var damageMultiplier: Float = 1f
    var maxHealthMultiplier: Float = 1f
    var healthRegen: Float = 0f
    var speedMultiplier: Float = 1f
    var maneuverabilityMultiplier: Float = 1f
    var cooldownMultiplier: Float = 1f
    var projectileSpeedMultiplier: Float = 1f
    var areaMultiplier: Float = 1f
    var extraProjectiles: Int = 0
    var pickupRangeMultiplier: Float = 1f
    var dropRateMultiplier: Float = 1f
    var durationMultiplier: Float = 1f
    var evasionChance: Float = 0f
    var extraLives: Int = 0
    var hasExtraWeaponSlot: Boolean = false  // From extra_weapon_slot passive

    // Drone fields
    var hasDrone: Boolean = false            // droneCount > 0
    var droneCount: Int = 0                  // Total drones: tb26 stacks + combat_drone stacks
    var droneEvolved: Boolean = false        // Autonomous Ace / TB-26-X active

    // Active pilot ID (set at game start, used for display name/color)
    var activePilotId: String = "pilot_medic"

    // New passive effect fields
    var momentumDamageBonus: Float = 0f      // Momentum Drive: +8% per stack while moving
    var cryoSlowPercent: Float = 0f          // Cryo Field: flat 50% slow
    var cryoRadiusMultiplier: Float = 1f     // Cryo Field: +25% radius per stack
    // revengeDamageBonus removed — revenge now doubles fire rate, not damage
    var maxShieldCap: Float = Float.MAX_VALUE // Glass Cannon: shield cap
    var shieldRegenDisabled: Boolean = false  // Glass Cannon: disable shield regen
    var hasLuckyStar: Boolean = false         // Lucky Star: auto-pick upgrades

    // Runtime passive state
    var revengeTimer: Float = 0f             // Time remaining on Revenge Protocol boost
    var revengeActive: Boolean = false       // Whether revenge boost is currently active

    // HUD fade alpha (fades out on pause/death)
    var hudFadeAlpha: Float = 1f

    // Lucky Star animation state
    var luckyStarAnimating: Boolean = false
    var luckyStarTimer: Float = 0f
    var luckyStarSelectedIndex: Int = -1
    var luckyStarCurrentHighlight: Int = 0
    var luckyStarBounceCount: Int = 0
    var luckyStarTotalBounces: Int = 0
    var luckyStarNextBounceTime: Float = 0f
    var luckyStarDimming: Boolean = false
    var luckyStarDimTimer: Float = 0f

    // Radio system state
    var radioMessage: String? = null
    var radioSpeaker: String? = null
    var radioColor: Int = 0xFFFFFFFF.toInt()
    var radioTimer: Float = 0f           // Time remaining to display
    var radioFadeTimer: Float = 0f       // Fade-out countdown
    var radioIsCorrupted: Boolean = false // True = corrupted crew portrait
    var radioIsGhost: Boolean = false     // True = show the muted ghost portrait (reckoning climax)
    var radioBoss: Boolean = false        // True = show portrait_boss (corrupted Astro / boss chatter)

    /** Astro Loop only: the active pilot's earned bandana shows on their own radio lines. */
    fun radioBandanaPilotId(): String? =
        activePilotId.takeIf { astroLoopMode && activePilotHasBandana }

    // Radio cooldowns
    var pilotRadioCooldown: Float = 0f   // 10-15s between player pilot lines
    var corruptedRadioCooldown: Float = 0f // 30s between corrupted crew lines
    var lastCorruptedPilotIndex: Int = -1  // Prevent back-to-back same pilot

    // Trigger tracking
    var shieldsWereUp: Boolean = true     // Track shield transitions
    var shieldsDownTriggered: Boolean = false  // Once-per-life shields_down radio
    var firstWeaponPickedUp: Boolean = false
    var firstEnemySpawned: Boolean = false
    var yenMilestoneReached: Boolean = false
    var lastDensitySpikeLevel: Int = 0    // Track difficulty thresholds

    // Near miss cooldown (2 minutes between triggers)
    var nearMissCooldown: Float = 0f

    // Time milestones — bitmask: bit 0=2min, 1=4min, 2=6min, 3=8min, 4–7=10–16min (Astro Loop only)
    var timeMilestonesFired: Int = 0
    // Random offsets ±10 seconds for each milestone
    var timeMilestoneOffsets: FloatArray = FloatArray(8)

    // Evasion tracking for visual effect
    var lastDodgeTime: Float = -10f

    // Track last asteroid upgrade drop time for cooldown
    var lastAsteroidUpgradeDropTime: Float = -1000f

    // Track asteroid upgrades collected (for early game drop scaling)
    var asteroidUpgradesCollected: Int = 0

    // Boss state
    var bossActive: Boolean = false
    var bossSpawned: Boolean = false

    // Desert flashback state
    var desertPhase: Int = 0           // 0=fun, 1=escalation, 2=horror, 3=crystal, 4=good_ending
    var desertTimer: Float = 0f        // Elapsed scene time
    var desertDialogueStep: Int = 0    // Current radio line index
    var desertDialogueTimer: Float = 0f // Timer for next dialogue line
    var desertNoInputTimer: Float = 0f  // Tracks how long since last input (for secret path)
    var desertSecretTriggered: Boolean = false // Player stopped moving
    var desertCrystalPhase: Int = 0    // 0=none, 1=rising, 2=hovering, 3=approaching, 4=flash
    var desertCrystalTimer: Float = 0f
    var desertFadeAlpha: Float = 0f    // For warm fade-out on good ending or crystal flash

    // Phase 4 boss fight sequence
    var bossFightPhase: Int = 0  // 0=not started, 1=survival, 2=drone_sent, 3=fleet_arriving, 4=fleet_arriving_chatter, 5=fleet_attacking, 6=post_victory
    var bossFightTimer: Float = 0f
    var droneDeparted: Boolean = false
    var fleetChatterStep: Int = 0
    var fleetChatterTimer: Float = 0f

    // Boss railgun charge (fleet arrival scene)
    var bossCharging: Boolean = false
    var bossChargeTimer: Float = 0f
    var bossChargeProgress: Float = 0f  // 0.0–1.0, driven by bossChargeTimer
    var bossEmpFired: Boolean = false   // prevents double-fire if timing drifts
    var pastAstroEmpFrozen: Boolean = false  // EMP #1 froze Past Astro during the corruption charge

    var bossChargedShotActive: Boolean = false  // non-Astro kill-shot beam visible
    var bossChargedShotTimer: Float = 0f        // 0→0.4s: drives beam fade-out

    // EMP rush-in (normal run: boss rushes the player before EMP #1 — see BossRush)
    var bossRushPhase: Int = 0    // 0=idle, 1=rushing, 2=braking, 3=arrival beat
    var bossRushTimer: Float = 0f

    // EMP rush-in (corruption run: autopilot takes the player to Past Astro)
    var corruptionRushPhase: Int = 0   // 0=idle, 1=rushing, 2=braking, 3=arrival beat
    var corruptionRushTimer: Float = 0f
    var corruptionRushSpeed: Float = 0f

    // Time Crystal drop (boss fight victory — normal Astro run)
    enum class TimeCrystalPhase { NONE, RISING, HOVERING, FLYING, COLLECTED }
    var timeCrystalPhase: TimeCrystalPhase = TimeCrystalPhase.NONE
    var timeCrystalTimer: Float = 0f
    var timeCrystalX: Float = 0f       // current render position X
    var timeCrystalY: Float = 0f       // current render position Y
    var timeCrystalOriginX: Float = 0f // boss death X (source for flying arc)
    var timeCrystalOriginY: Float = 0f // boss death Y (source for flying arc)
    var crystalApproachPhase: Int = 0      // 0=slow approach, 1=uncertainty beat, 2=final approach
    var crystalHesitationTimer: Float = 0f

    // Corruption run flag (any pilot during corruption phase)
    var isCorruptionRun: Boolean = false

    // Astro Loop mode (endless pure-asteroid run)
    var astroLoopMode: Boolean = false
    var astroLoopEvolutionUsed: Boolean = false

    // Crystal powers (Astro corruption run)
    var hasCrystalPowers: Boolean = false
    var crystalHealthRegen: Float = 10f    // HP per second
    var crystalShieldRegen: Float = 15f    // Shield per second

    // Crystal afterimage dodge (multiple simultaneous afterimages)
    data class CrystalAfterimage(
        var x: Float = 0f,
        var y: Float = 0f,
        var rotation: Float = 0f,
        var timer: Float = 3.0f,
        var targetX: Float = 0f,
        var targetY: Float = 0f,
        var hasTarget: Boolean = false,
        var aiming: Boolean = true,
        var fired: Boolean = false,
        var targetEntity: Entity? = null  // Track entity for live position updates
    )
    val crystalAfterimages = mutableListOf<CrystalAfterimage>()

    // Crystal energy shield tick timer
    var crystalShieldTickTimer: Float = 0f

    // Corruption Astro time speed-up (3x clock after 10 seconds)
    var corruptionTimeMultiplier: Float = 1f
    var corruptionSpeedUpTriggered: Boolean = false

    // Corruption run encounter state machine
    var corruptionEncounterPhase: Int = 0  // 0=field, 2=shield, 4=zap
    var corruptionEncounterTimer: Float = 0f
    var corruptionEncounterIndex: Int = 0  // 0-4 for 5 encounters
    var corruptionSelectedCrewmates: List<Int> = emptyList()  // indices into CREWMATE_ENCOUNTER_ORDER
    var corruptionNextEncounterTime: Float = 0f  // game-time threshold for next encounter
    var corruptionChatterIndex: Int = 0  // index into chatter lines
    var corruptionChatterTimer: Float = 0f  // real-time countdown to next chatter line
    var corruptionInBossArena: Boolean = false  // true when in red boss arena

    // Crystalline zap effect (localized Time Crystal burst on crewmate kill)
    var crystalZapActive: Boolean = false
    var crystalZapTimer: Float = 0f

    // Scripted death (TB-26 ram → fleet kill, used for both boss and player)
    var scriptedDeathActive: Boolean = false
    var scriptedDeathTimer: Float = 0f
    var scriptedDeathRate: Float = 0f  // HP per second to drain
    var playerStunned: Boolean = false  // Player frozen after TB-26 ram

    // Heart-to-heart dialogue state (letter-by-letter rendering)
    var heartToHeartLog: MutableList<Triple<String, String, Int>> = mutableListOf()  // speaker, text, color
    var heartToHeartCharIndex: Int = 0
    var heartToHeartCharTimer: Float = 0f

    // Zap beam effect (crewmate teleport-back in Astro corruption run)
    var zapBeamActive: Boolean = false
    var zapBeamTimer: Float = 0f
    var zapBeamFromX: Float = 0f
    var zapBeamFromY: Float = 0f
    var zapBeamToX: Float = 0f
    var zapBeamToY: Float = 0f
    var zapBeamColor: Int = com.astroloop.game.render.CrystalPalette.MID  // icy cyan

    // Phoenix passive state - tracks if it was used (stays in slot but inactive)
    var phoenixUsed: Boolean = false
    var phoenixShockwaveActive: Boolean = false
    var phoenixShockwaveOriginX: Float = 0f
    var phoenixShockwaveOriginY: Float = 0f
    var phoenixShockwavePrevRadius: Float = 0f
    var phoenixShockwaveRadius: Float = 0f

    // Bandana finale — crystal reckoning (transient; not persisted).
    // LANCE is terminal on a win: the scene fades to black in place and hands off to the hangar.
    enum class ReckoningStage { NONE, OPENING, FIGHT, LANCE }
    var reckoningActive: Boolean = false
    var reckoningStage: ReckoningStage = ReckoningStage.NONE
    var reckoningTimer: Float = 0f
    /** Retry after a failed attempt: skip the opening monologue to its final "Time to close it." line. */
    var reckoningSkipOpening: Boolean = false

    // Loadout lockout during reckoning fight (weapons / passive effects disabled)
    var weaponsDisabled: Boolean = false
    var passivesDisabled: Boolean = false

    // Permanent upgrades (meta-progression, persist across runs)
    // Each level gives: Health +10, Shields +10, Speed +5%, Damage +5%, Fire Rate +5%
    var permanentHealthLevel: Int = 0      // 0-5, +10 HP per level
    var permanentShieldsLevel: Int = 0     // 0-5, +10 shields per level
    var permanentSpeedLevel: Int = 0       // 0-5, +5% speed per level
    var permanentDamageLevel: Int = 0      // 0-5, +5% damage per level
    var permanentCritLevel: Int = 0        // 0-5, +5% crit chance per level
    var permanentYenBonusLevel: Int = 0    // 0-5, +10% yen per level (0.5x base)
    var permanentSalvageLevel: Int = 0    // 0-5, +10% upgrade drops per level
    var permanentMagnetLevel: Int = 0  // 0-5, +15% pickup range & +20% pull speed per level

    fun getPermanentHealthBonus(): Float = permanentHealthLevel * 10f
    fun getPermanentShieldsBonus(): Float = permanentShieldsLevel * 10f
    fun getPermanentSpeedBonus(): Float = permanentSpeedLevel * 0.05f
    fun getPermanentDamageBonus(): Float = permanentDamageLevel * 0.05f
    fun getCritChance(): Float = permanentCritLevel * GameConfig.CRIT_CHANCE_PER_LEVEL
    fun rollCrit(): Boolean {
        val chance = getCritChance()
        return chance > 0f && kotlin.random.Random.nextFloat() < chance
    }
    fun getYenMultiplier(): Float = 0.5f + permanentYenBonusLevel * 0.10f
    fun getAsteroidYenScale(): Float =
        (0.75f + (survivalTime / 300f) * 0.25f).coerceAtMost(1f)
    fun getSalvageMultiplier(): Float = 0.5f + permanentSalvageLevel * 0.10f
    fun getMagnetRangeMultiplier(): Float = 1f + permanentMagnetLevel * 0.15f
    fun getMagnetSpeedMultiplier(): Float = 1f + permanentMagnetLevel * 0.2f

    fun reset() {
        phase = GamePhase.PLAYING
        survivalTime = 0f
        isPaused = false
        isPostHorrorRun = false
        retreatPhase = 0
        retreatTimer = 0f
        emergencyShieldActive = false
        graceTimer = 0.5f
        difficultyMultiplier = 1f
        totalUpgradesCollected = 0
        debugMenuPage = 0
        score = 0
        goldCollected = 0
        lastAsteroidUpgradeDropTime = -1000f
        asteroidUpgradesCollected = 0
        bossActive = false
        bossSpawned = false
        bossFightPhase = 0
        bossFightTimer = 0f
        droneDeparted = false
        fleetChatterStep = 0
        fleetChatterTimer = 0f
        bossCharging = false
        bossChargeTimer = 0f
        bossChargeProgress = 0f
        bossEmpFired = false
        pastAstroEmpFrozen = false
        bossRushPhase = 0
        bossRushTimer = 0f
        corruptionRushPhase = 0
        corruptionRushTimer = 0f
        corruptionRushSpeed = 0f
        bossChargedShotActive = false
        bossChargedShotTimer = 0f
        timeCrystalPhase = TimeCrystalPhase.NONE
        timeCrystalTimer = 0f
        timeCrystalX = 0f
        timeCrystalY = 0f
        timeCrystalOriginX = 0f
        timeCrystalOriginY = 0f
        crystalApproachPhase = 0
        crystalHesitationTimer = 0f
        phoenixUsed = false
        phoenixShockwaveActive = false
        phoenixShockwaveRadius = 0f
        phoenixShockwavePrevRadius = 0f
        isCorruptionRun = false
        astroLoopMode = false
        astroLoopEvolutionUsed = false
        storyLoop = 1  // will be overwritten by GameSurfaceView.initialize()
        hasCrystalPowers = false
        crystalAfterimages.clear()
        crystalShieldTickTimer = 0f
        corruptionTimeMultiplier = 1f
        corruptionSpeedUpTriggered = false
        corruptionEncounterPhase = 0
        corruptionEncounterTimer = 0f
        corruptionEncounterIndex = 0
        corruptionSelectedCrewmates = emptyList()
        corruptionNextEncounterTime = 0f
        corruptionChatterIndex = 0
        corruptionChatterTimer = 0f
        corruptionInBossArena = false
        crystalZapActive = false
        crystalZapTimer = 0f
        scriptedDeathActive = false
        scriptedDeathTimer = 0f
        scriptedDeathRate = 0f
        playerStunned = false
        zapBeamActive = false
        zapBeamTimer = 0f
        heartToHeartLog.clear()
        heartToHeartCharIndex = 0
        heartToHeartCharTimer = 0f
        hasEvolvedThisGame = false

        // Reckoning fight loadout lockout — clear on new run
        weaponsDisabled = false
        passivesDisabled = false
        reckoningSkipOpening = false

        // Desert reset
        desertPhase = 0
        desertTimer = 0f
        desertDialogueStep = 0
        desertDialogueTimer = 0f
        desertNoInputTimer = 0f
        desertSecretTriggered = false
        desertCrystalPhase = 0
        desertCrystalTimer = 0f
        desertFadeAlpha = 0f

        // Reset drone fields
        hasDrone = false
        droneCount = 0
        droneEvolved = false

        // Reset new passive effect fields
        momentumDamageBonus = 0f
        cryoSlowPercent = 0f
        cryoRadiusMultiplier = 1f
        maxShieldCap = Float.MAX_VALUE
        shieldRegenDisabled = false
        hasLuckyStar = false
        revengeTimer = 0f
        revengeActive = false
        hudFadeAlpha = 1f
        luckyStarAnimating = false
        luckyStarTimer = 0f
        luckyStarSelectedIndex = -1

        // Reset radio system state
        radioMessage = null
        radioSpeaker = null
        radioColor = 0xFFFFFFFF.toInt()
        radioTimer = 0f
        radioFadeTimer = 0f
        radioIsCorrupted = false
        radioIsGhost = false
        pilotRadioCooldown = 0f
        corruptedRadioCooldown = 0f
        lastCorruptedPilotIndex = -1
        shieldsWereUp = true
        shieldsDownTriggered = false
        firstWeaponPickedUp = false
        firstEnemySpawned = false
        yenMilestoneReached = false
        lastDensitySpikeLevel = 0
        nearMissCooldown = 0f
        timeMilestonesFired = 0
        timeMilestoneOffsets = FloatArray(8) { (kotlin.random.Random.nextFloat() * 20f) - 10f }

        // Reset shield state
        baseShields = GameConfig.SHIP_BASE_SHIELDS
        shieldRegenRate = GameConfig.SHIELD_REGEN_RATE
        shieldRegenDelay = GameConfig.SHIELD_REGEN_DELAY
        shieldCapMultiplier = 1f

        weaponLevels.clear()
        passiveStacks.clear()
        evolvedWeapons.clear()
        pendingDelayedEffects.clear()
        weaponCooldowns.clear()
        weaponCounters.clear()

        resetTelemetry()

        recalculateStats()
    }

    fun resetTelemetry() {
        telemetryDamageByWeapon.clear()
        telemetryDamageTakenBy.clear()
        telemetryCritsThisMinute = 0
        telemetryPowerupsCollected.clear()
        telemetryDodges = 0
        telemetrySnapshotTimer = 0f
        telemetryYenFromAsteroids = 0
        telemetryYenFromEnemies = 0
        telemetryAsteroidsDestroyed = 0
        telemetryCritsTotal = 0
        telemetryUpgradeDropsCollected = 0
        telemetryDiamondsCollected = 0
        telemetryTotalDamageDealt = 0f
        telemetryTotalDamageTaken = 0f
        lastDamageSource = "unknown"
        telemetryLastOfferedOptions = emptyList()
    }

    fun resetTelemetryMinuteCounters() {
        telemetryDamageByWeapon.clear()
        telemetryDamageTakenBy.clear()
        telemetryCritsThisMinute = 0
        telemetryPowerupsCollected.clear()
        telemetryDodges = 0
    }

    // Point values for destroying entities
    companion object {
        const val POINTS_ASTEROID_SMALL = 10
        const val POINTS_ASTEROID_MEDIUM = 25
        const val POINTS_ASTEROID_LARGE = 50
        const val POINTS_ENEMY_TIER_1 = 100
        const val POINTS_ENEMY_TIER_2 = 250
        const val POINTS_ENEMY_TIER_3 = 400
        const val POINTS_ENEMY_TIER_4 = 750

        /**
         * Returns a GameState with all multipliers at base values.
         * Used by crewmate weapons so they fire at base stats, not player-scaled.
         * The default GameState() constructor already initialises all multipliers
         * to 1.0 and booleans to false — no field overrides needed.
         */
        fun createNeutral(): GameState = GameState()
    }

    fun addWeapon(weaponId: String): Boolean {
        val currentLevel = weaponLevels[weaponId] ?: 0
        if (currentLevel < GameConfig.WEAPON_MAX_LEVEL) {
            weaponLevels[weaponId] = currentLevel + 1
            totalUpgradesCollected++
            return true
        }
        return false
    }

    fun addPassive(passiveId: String): Boolean {
        val resolvedId = if (passiveId == "combat_drone" && activePilotId == "pilot_astro") "tb26" else passiveId
        val currentStacks = passiveStacks[resolvedId] ?: 0
        if (currentStacks < GameConfig.PASSIVE_MAX_STACKS) {
            // Instant-max passives go to full stacks immediately when first picked
            val instantMaxPassives = setOf("glass_cannon", "phoenix_core", "duplicator_core", "extra_weapon_slot", "lucky_star")
            if (instantMaxPassives.contains(resolvedId) && currentStacks == 0) {
                passiveStacks[resolvedId] = GameConfig.PASSIVE_MAX_STACKS
            } else {
                passiveStacks[resolvedId] = currentStacks + 1
            }
            totalUpgradesCollected++
            recalculateStats()
            return true
        }
        return false
    }

    fun hasEvolution(evolutionId: String): Boolean = evolvedWeapons.contains(evolutionId)

    fun addEvolution(evolutionId: String) {
        evolvedWeapons.add(evolutionId)
    }

    fun getWeaponLevel(weaponId: String): Int = weaponLevels[weaponId] ?: 0

    fun getPassiveStacks(passiveId: String): Int = passiveStacks[passiveId] ?: 0

    fun recalculateStats() {
        // Reset to base
        damageMultiplier = 1f
        maxHealthMultiplier = 1f
        healthRegen = 0f
        speedMultiplier = 1f
        maneuverabilityMultiplier = 1f
        cooldownMultiplier = 1f
        projectileSpeedMultiplier = 1f
        areaMultiplier = 1f
        extraProjectiles = 0
        pickupRangeMultiplier = 1f
        dropRateMultiplier = 1f
        durationMultiplier = 1f
        evasionChance = 0f
        extraLives = 0
        hasDrone = false
        droneCount = 0
        hasExtraWeaponSlot = false
        momentumDamageBonus = 0f
        cryoSlowPercent = 0f
        cryoRadiusMultiplier = 1f
        maxShieldCap = Float.MAX_VALUE
        shieldRegenDisabled = false
        hasLuckyStar = false

        // Apply passives
        passiveStacks.forEach { (passiveId, stacks) ->
            when (passiveId) {
                "nano_repair" -> {
                    healthRegen += 0.4f * stacks
                }
                "duplicator_core" -> {
                    // +1 projectile total (instant-max, binary effect)
                    extraProjectiles += 1
                }
                "magnet_field" -> {
                    pickupRangeMultiplier += 0.3f * stacks
                }
                "phoenix_core" -> {
                    extraLives = 1
                }
                "extra_weapon_slot" -> {
                    hasExtraWeaponSlot = true
                }
                "tb26" -> {
                    droneCount += stacks
                    hasDrone = droneCount > 0
                }
                "combat_drone" -> {
                    droneCount += stacks
                    hasDrone = droneCount > 0
                }
                "momentum_drive" -> {
                    momentumDamageBonus = 0.08f * stacks
                }
                "cryo_field" -> {
                    cryoSlowPercent = 0.5f  // Flat 50% slow
                    cryoRadiusMultiplier = 1f + 0.25f * stacks  // +25% radius per stack
                }
                "lucky_star" -> {
                    hasLuckyStar = true
                    dropRateMultiplier += 0.50f  // +50% upgrade drop rate
                }
                "revenge_protocol" -> {
                    // Fire rate burst handled in WeaponSystem; no stat to set here
                    Unit
                }
                "glass_cannon" -> {
                    damageMultiplier += 1.00f  // Flat +100%, not per-stack
                    maxShieldCap = 0f
                    shieldRegenDisabled = true
                }
            }
        }

        // Apply permanent upgrades (meta-progression)
        damageMultiplier += getPermanentDamageBonus()
        speedMultiplier += getPermanentSpeedBonus()
    }

    fun getMaxWeaponSlots(): Int {
        return if (hasExtraWeaponSlot) GameConfig.MAX_WEAPON_SLOTS + 1 else GameConfig.MAX_WEAPON_SLOTS
    }

    fun getMaxPassiveSlots(): Int {
        return if (hasExtraWeaponSlot) GameConfig.PASSIVE_SLOTS_WITH_EXTRA_WEAPON else GameConfig.MAX_PASSIVE_SLOTS
    }

    fun getWeaponCount(): Int = weaponLevels.size

    fun getPassiveCount(): Int = passiveStacks.count { it.key != "extra_weapon_slot" }

    fun canAddNewWeapon(): Boolean = getWeaponCount() < getMaxWeaponSlots()

    fun canAddNewPassive(): Boolean = getPassiveCount() < getMaxPassiveSlots()

    fun isFullyUpgraded(): Boolean {
        // Check if all weapon slots are filled AND maxed
        val weaponsFull = weaponLevels.size >= getMaxWeaponSlots() &&
            weaponLevels.values.all { it >= GameConfig.WEAPON_MAX_LEVEL }

        // Check if all passive slots are filled AND maxed (extra_weapon_slot excluded from count)
        val passivesFull = getPassiveCount() >= getMaxPassiveSlots() &&
            passiveStacks.values.all { it >= GameConfig.PASSIVE_MAX_STACKS }  // extra_weapon_slot is always instant-max, safe to include

        return weaponsFull && passivesFull
    }

    fun formatTime(seconds: Float): String {
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return String.format("%02d:%02d", mins, secs)
    }

    // Delayed zone effects (shell echo, aftershock ring, etc.)
    data class DelayedEffect(
        var timer: Float,
        val x: Float,
        val y: Float,
        val radius: Float,
        val damage: Float,
        val duration: Float,
        val color: Int
    )
    val pendingDelayedEffects = mutableListOf<DelayedEffect>()

    // Per-key cooldown timers (used for zap pulse, etc.)
    val weaponCooldowns = mutableMapOf<String, Float>()

    // Per-key integer counters (used for burst tracking, etc. — not subject to tick-down)
    val weaponCounters: MutableMap<String, Int> = mutableMapOf()

    fun getAsteroidDropChance(): Float {
        val initial  = if (astroLoopMode) GameConfig.ASTRO_LOOP_UPGRADE_DROP_INITIAL  else GameConfig.ASTEROID_UPGRADE_DROP_INITIAL
        val baseline = if (astroLoopMode) GameConfig.ASTRO_LOOP_UPGRADE_DROP_BASELINE else GameConfig.ASTEROID_UPGRADE_DROP_BASELINE
        val reduction = asteroidUpgradesCollected * GameConfig.ASTEROID_UPGRADE_DROP_DECREASE
        val baseChance = (initial - reduction).coerceAtLeast(baseline)
        return baseChance * getSalvageMultiplier()
    }

    fun isEarlyGameDropRate(): Boolean {
        val initial  = if (astroLoopMode) GameConfig.ASTRO_LOOP_UPGRADE_DROP_INITIAL  else GameConfig.ASTEROID_UPGRADE_DROP_INITIAL
        val baseline = if (astroLoopMode) GameConfig.ASTRO_LOOP_UPGRADE_DROP_BASELINE else GameConfig.ASTEROID_UPGRADE_DROP_BASELINE
        val reduction = asteroidUpgradesCollected * GameConfig.ASTEROID_UPGRADE_DROP_DECREASE
        val baseChance = (initial - reduction).coerceAtLeast(baseline)
        return baseChance > baseline
    }
}
