package com.astroloop.game.data

import android.content.Context
import android.content.SharedPreferences
import com.astroloop.game.core.StoryStage

class PersistenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    init {
        // One-time migration: Rapid Mod → Lucky Rounds
        val oldFireRate = prefs.getInt("upgrade_fire_rate", 0)
        if (oldFireRate > 0 && prefs.getInt("upgrade_crit", 0) == 0) {
            prefs.edit().putInt("upgrade_crit", oldFireRate).putInt("upgrade_fire_rate", 0).apply()
        }
    }

    // Yen
    fun getYen(): Int = prefs.getInt(KEY_YEN, 0)
    fun setYen(amount: Int) = prefs.edit().putInt(KEY_YEN, amount).apply()
    fun addYen(amount: Int) = setYen(getYen() + amount)

    // Ship unlocks
    fun getUnlockedShips(): Set<String> {
        val default = setOf("ship_blue")
        return prefs.getStringSet(KEY_UNLOCKED_SHIPS, default) ?: default
    }
    fun unlockShip(shipId: String) {
        val current = getUnlockedShips().toMutableSet()
        current.add(shipId)
        prefs.edit().putStringSet(KEY_UNLOCKED_SHIPS, current).apply()
    }
    fun isShipUnlocked(shipId: String): Boolean = getUnlockedShips().contains(shipId)

    // Pilot unlocks
    fun getUnlockedPilots(): Set<String> {
        val default = setOf("pilot_medic")
        return prefs.getStringSet(KEY_UNLOCKED_PILOTS, default) ?: default
    }
    fun unlockPilot(pilotId: String) {
        val current = getUnlockedPilots().toMutableSet()
        current.add(pilotId)
        prefs.edit().putStringSet(KEY_UNLOCKED_PILOTS, current).apply()
    }
    fun isPilotUnlocked(pilotId: String): Boolean = getUnlockedPilots().contains(pilotId)

    // Selected ship/pilot
    fun getSelectedShipId(): String = prefs.getString(KEY_SELECTED_SHIP, "ship_blue") ?: "ship_blue"
    fun setSelectedShipId(shipId: String) = prefs.edit().putString(KEY_SELECTED_SHIP, shipId).apply()

    fun getSelectedPilotId(): String = prefs.getString(KEY_SELECTED_PILOT, "pilot_medic") ?: "pilot_medic"
    fun setSelectedPilotId(pilotId: String) = prefs.edit().putString(KEY_SELECTED_PILOT, pilotId).apply()

    // Permanent upgrades (0-5 each)
    fun getUpgradeLevel(upgradeId: String): Int = prefs.getInt("upgrade_$upgradeId", 0)
    fun setUpgradeLevel(upgradeId: String, level: Int) {
        prefs.edit().putInt("upgrade_$upgradeId", level.coerceIn(0, 5)).apply()
    }

    // Discovered evolutions (for codex)
    fun getDiscoveredEvolutions(): Set<String> {
        return migrateWeaponIds(prefs.getStringSet(KEY_DISCOVERED_EVOLUTIONS, emptySet()) ?: emptySet())
    }
    fun discoverEvolution(evolutionId: String) {
        val current = getDiscoveredEvolutions().toMutableSet()
        current.add(evolutionId)
        prefs.edit().putStringSet(KEY_DISCOVERED_EVOLUTIONS, current).apply()
    }

    // Reset all progress
    fun resetAllProgress() {
        val editor = prefs.edit()
        // Reset ships to only first ship unlocked
        editor.putStringSet(KEY_UNLOCKED_SHIPS, setOf("ship_blue"))
        // Reset pilots to only first pilot unlocked
        editor.putStringSet(KEY_UNLOCKED_PILOTS, setOf("pilot_medic"))
        // Reset all permanent upgrades to 0
        for (id in listOf("health", "shields", "speed", "damage", "crit", "yen_bonus", "salvage", "magnet")) {
            editor.putInt("upgrade_$id", 0)
        }
        // Reset discovered evolutions
        editor.putStringSet(KEY_DISCOVERED_EVOLUTIONS, emptySet())
        // Reset selection to defaults
        editor.putString(KEY_SELECTED_SHIP, "ship_blue")
        editor.putString(KEY_SELECTED_PILOT, "pilot_medic")
        // Reset cumulative stats
        editor.remove(KEY_TOTAL_YEN_EARNED)
        editor.remove(KEY_TOTAL_DAMAGE_TAKEN)
        editor.remove(KEY_TOTAL_DEATHS)
        editor.remove(KEY_TOTAL_KILLS)
        editor.putStringSet(KEY_WEAPONS_DISCOVERED, emptySet())
        editor.remove(KEY_BEST_KILL_STREAK)
        editor.remove(KEY_BEST_SINGLE_RUN_KILLS)
        editor.remove(KEY_BEST_SURVIVAL_SECONDS)
        editor.putInt(KEY_NEXT_PILOT_INDEX, 1)
        editor.remove(KEY_RUNS_SINCE_PILOT_UNLOCK)
        // Reset codex discovery
        editor.putBoolean(KEY_CODEX_DISCOVERED, false)
        editor.putBoolean(KEY_CODEX_HINT_GIVEN, false)
        // Reset Astro hints
        editor.remove(KEY_ASTRO_HINT_COUNT)
        editor.remove(KEY_ASTRO_HINTED)
        // Reset first launch so bar intro plays again
        editor.remove("first_launch_complete")
        // Reset intro cinematic so a full wipe replays the first-launch intro
        editor.remove("intro_cinematic_done")
        // Reset story state (new keys written below; legacy keys no longer exist)
        editor.putStringSet("dead_pilots", emptySet())
        editor.putStringSet("pilots_mourned", emptySet())
        editor.putStringSet("dead_ships", emptySet())
        editor.putBoolean("crystal_unlocked", false)
        editor.putBoolean("crystal_purchased", false)
        editor.putBoolean("crystal_broken", false)
        // Bandanas (finale chunk 1)
        editor.putStringSet("earned_bandanas", emptySet())
        editor.remove("pending_bandana_pilot")
        editor.putBoolean("awaiting_convergence", false)
        editor.putBoolean("crystal_released", false)
        editor.putBoolean("reckoning_attempted", false)
        editor.putBoolean("reckoning_just_won", false)
        editor.putBoolean("reckoning_just_lost", false)
        editor.putInt("reckoning_rounds", 0)
        editor.putInt("reckoning_pool_last", -1)
        editor.remove("total_casino_spins")
        // Desert flashback
        editor.remove("desert_completed")
        editor.remove("desert_good_ending")
        editor.remove(KEY_LAST_ASTRO_RUN_SECONDS)
        editor.remove(KEY_ASTRO_LOOP_BEST_SECONDS)
        editor.remove(KEY_FRESH_LOOP_START)
        // Reset new story-state keys (written directly, so mark migrated to skip re-migration)
        editor.putInt("story_stage", 0)
        editor.putInt("story_loop", 1)
        editor.putBoolean("story_state_migrated", true)
        editor.apply()
    }

    // Best time (for stats display)
    fun getBestTime(): Float = prefs.getFloat(KEY_BEST_TIME, 0f)
    fun saveBestTime(time: Float): Boolean {
        if (time > getBestTime()) {
            prefs.edit().putFloat(KEY_BEST_TIME, time).apply()
            return true
        }
        return false
    }

    // Cumulative stats (across all runs)
    fun getTotalYenEarned(): Int = prefs.getInt(KEY_TOTAL_YEN_EARNED, 0)
    fun addTotalYenEarned(amount: Int) = prefs.edit().putInt(KEY_TOTAL_YEN_EARNED, getTotalYenEarned() + amount).apply()

    fun getTotalDamageTaken(): Int = prefs.getInt(KEY_TOTAL_DAMAGE_TAKEN, 0)
    fun addTotalDamageTaken(amount: Int) = prefs.edit().putInt(KEY_TOTAL_DAMAGE_TAKEN, getTotalDamageTaken() + amount).apply()

    fun getTotalDeaths(): Int = prefs.getInt(KEY_TOTAL_DEATHS, 0)
    fun incrementTotalDeaths() = prefs.edit().putInt(KEY_TOTAL_DEATHS, getTotalDeaths() + 1).apply()

    fun getTotalKills(): Int = prefs.getInt(KEY_TOTAL_KILLS, 0)
    fun addTotalKills(amount: Int) = prefs.edit().putInt(KEY_TOTAL_KILLS, getTotalKills() + amount).apply()

    fun getWeaponsDiscovered(): Set<String> =
        migrateWeaponIds(prefs.getStringSet(KEY_WEAPONS_DISCOVERED, emptySet()) ?: emptySet())
    fun discoverWeapon(weaponId: String) {
        val current = getWeaponsDiscovered().toMutableSet()
        current.add(weaponId)
        prefs.edit().putStringSet(KEY_WEAPONS_DISCOVERED, current).apply()
    }

    // Best single-run stats
    fun getBestKillStreak(): Int = prefs.getInt(KEY_BEST_KILL_STREAK, 0)
    fun saveBestKillStreak(streak: Int) {
        if (streak > getBestKillStreak()) prefs.edit().putInt(KEY_BEST_KILL_STREAK, streak).apply()
    }

    fun getBestContinuousFlightSeconds(): Int = prefs.getInt(KEY_BEST_CONTINUOUS_FLIGHT, 0)
    fun saveBestContinuousFlightSeconds(seconds: Int) {
        if (seconds > getBestContinuousFlightSeconds()) {
            prefs.edit().putInt(KEY_BEST_CONTINUOUS_FLIGHT, seconds).apply()
        }
    }

    fun getBestSingleRunKills(): Int = prefs.getInt(KEY_BEST_SINGLE_RUN_KILLS, 0)
    fun saveBestSingleRunKills(kills: Int) {
        if (kills > getBestSingleRunKills()) prefs.edit().putInt(KEY_BEST_SINGLE_RUN_KILLS, kills).apply()
    }

    fun getBestSurvivalSeconds(): Int = prefs.getInt(KEY_BEST_SURVIVAL_SECONDS, 0)
    fun saveBestSurvivalSeconds(seconds: Int) {
        if (seconds > getBestSurvivalSeconds()) prefs.edit().putInt(KEY_BEST_SURVIVAL_SECONDS, seconds).apply()
    }

    // Pilot unlock sequencing
    fun getNextPilotIndex(): Int = prefs.getInt(KEY_NEXT_PILOT_INDEX, 1)
    fun setNextPilotIndex(index: Int) = prefs.edit().putInt(KEY_NEXT_PILOT_INDEX, index).apply()

    fun getRunsSincePilotUnlock(): Int = prefs.getInt(KEY_RUNS_SINCE_PILOT_UNLOCK, 0)
    fun incrementRunsSincePilotUnlock() = prefs.edit().putInt(KEY_RUNS_SINCE_PILOT_UNLOCK, getRunsSincePilotUnlock() + 1).apply()
    fun resetRunsSincePilotUnlock() = prefs.edit().putInt(KEY_RUNS_SINCE_PILOT_UNLOCK, 0).apply()
    fun setRunsSincePilotUnlock(value: Int) = prefs.edit().putInt(KEY_RUNS_SINCE_PILOT_UNLOCK, value).apply()

    // First launch detection
    fun isFirstLaunch(): Boolean = !prefs.contains("first_launch_complete")
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean("first_launch_complete", true).apply()
    }

    // Intro cinematic (first-ever launch only)
    fun isIntroDone(): Boolean = prefs.getBoolean("intro_cinematic_done", false)
    fun setIntroDone() {
        prefs.edit().putBoolean("intro_cinematic_done", true).apply()
    }

    // Codex discovery (secret hatch on slot machine)
    fun isCodexDiscovered(): Boolean = prefs.getBoolean(KEY_CODEX_DISCOVERED, false)
    fun setCodexDiscovered() = prefs.edit().putBoolean(KEY_CODEX_DISCOVERED, true).apply()
    fun isCodexHintGiven(): Boolean = prefs.getBoolean(KEY_CODEX_HINT_GIVEN, false)
    fun setCodexHintGiven() = prefs.edit().putBoolean(KEY_CODEX_HINT_GIVEN, true).apply()

    // Audio/vibration mute settings
    fun isAudioMuted(): Boolean = prefs.getBoolean("audio_muted", false)
    fun setAudioMuted(muted: Boolean) = prefs.edit().putBoolean("audio_muted", muted).apply()

    fun isVibrationMuted(): Boolean = prefs.getBoolean("vibration_muted", false)
    fun setVibrationMuted(muted: Boolean) = prefs.edit().putBoolean("vibration_muted", muted).apply()

    // Astro hint state (TB-26 hints after all non-Astro pilots recruited)
    fun getAstroHintCount(): Int = prefs.getInt(KEY_ASTRO_HINT_COUNT, 0)
    fun setAstroHintCount(count: Int) = prefs.edit().putInt(KEY_ASTRO_HINT_COUNT, count).apply()
    fun isAstroHinted(): Boolean = prefs.getBoolean(KEY_ASTRO_HINTED, false)
    fun setAstroHinted(hinted: Boolean) = prefs.edit().putBoolean(KEY_ASTRO_HINTED, hinted).apply()

    fun isAllEvolutionsHinted(): Boolean = prefs.getBoolean(KEY_ALL_EVOLUTIONS_HINTED, false)
    fun setAllEvolutionsHinted() = prefs.edit().putBoolean(KEY_ALL_EVOLUTIONS_HINTED, true).apply()

    // --- Story Phase ---

    internal fun prefsForTest() = prefs

    fun getStoryStageCode(): Int = prefs.getInt("story_stage", 0)
    fun setStoryStageCode(code: Int) { prefs.edit().putInt("story_stage", code).apply() }

    fun getStoryLoop(): Int = prefs.getInt("story_loop", 1).coerceIn(1, 3)
    fun setStoryLoop(n: Int) { prefs.edit().putInt("story_loop", n.coerceIn(1, 3)).apply() }
    fun incrementStoryLoop() { setStoryLoop(getStoryLoop() + 1) }

    /** One-time fold of the legacy flags into story_stage / story_loop. Guarded + idempotent. */
    fun migrateStoryState() {
        if (prefs.getBoolean("story_state_migrated", false)) return
        val oldAstroLoop = prefs.getBoolean("astro_loop_mode", false)
        val oldPhase = prefs.getInt("story_phase", 0)
        val stage = when {
            oldAstroLoop -> StoryStage.ASTRO_LOOP.code
            oldPhase == 2 -> StoryStage.CORRUPTION.code   // legacy NG_PLUS was a corruption replay
            else -> oldPhase.coerceIn(0, 1)                // 0 or 1 carry over; clamp unexpected values
        }
        val loop = prefs.getInt("narrative_loop", 1).coerceIn(1, 3)
        prefs.edit()
            .putInt("story_stage", stage)
            .putInt("story_loop", loop)
            .putBoolean("story_state_migrated", true)
            .remove("astro_loop_mode")
            .remove("story_phase")
            .remove("narrative_loop")
            .remove("corruption_arc_completed")
            .apply()
    }

    fun getDeadPilots(): Set<String> = prefs.getStringSet("dead_pilots", emptySet()) ?: emptySet()
    fun addDeadPilot(pilotId: String) {
        val current = getDeadPilots().toMutableSet()
        current.add(pilotId)
        prefs.edit().putStringSet("dead_pilots", current).apply()
    }

    fun getPilotsMourned(): Set<String> = prefs.getStringSet("pilots_mourned", emptySet()) ?: emptySet()
    fun addPilotMourned(pilotId: String) {
        val current = getPilotsMourned().toMutableSet()
        current.add(pilotId)
        prefs.edit().putStringSet("pilots_mourned", current).apply()
    }

    fun getDeadShips(): Set<String> = prefs.getStringSet("dead_ships", emptySet()) ?: emptySet()
    fun addDeadShip(shipId: String) {
        val current = getDeadShips().toMutableSet()
        current.add(shipId)
        prefs.edit().putStringSet("dead_ships", current).apply()
    }

    fun clearDeadPilotsAndShips() {
        prefs.edit()
            .putStringSet("dead_pilots", emptySet())
            .putStringSet("dead_ships", emptySet())
            .apply()
    }

    // --- Bandanas (finale chunk 1) ---
    fun getEarnedBandanas(): Set<String> = prefs.getStringSet("earned_bandanas", emptySet()) ?: emptySet()
    fun hasBandana(pilotId: String): Boolean = getEarnedBandanas().contains(pilotId)
    fun addBandana(pilotId: String) {
        val current = getEarnedBandanas().toMutableSet()
        current.add(pilotId)
        prefs.edit().putStringSet("earned_bandanas", current).apply()
    }
    fun getBandanaCount(): Int = getEarnedBandanas().size
    fun clearAllBandanas() { prefs.edit().putStringSet("earned_bandanas", emptySet()).apply() }

    fun getPendingBandanaPilot(): String? =
        prefs.getString("pending_bandana_pilot", null)?.takeIf { it.isNotEmpty() }
    fun setPendingBandanaPilot(pilotId: String) {
        prefs.edit().putString("pending_bandana_pilot", pilotId).apply()
    }
    fun clearPendingBandanaPilot() { prefs.edit().remove("pending_bandana_pilot").apply() }

    fun isAwaitingConvergence(): Boolean = prefs.getBoolean("awaiting_convergence", false)
    fun setAwaitingConvergence(v: Boolean) { prefs.edit().putBoolean("awaiting_convergence", v).apply() }

    fun isCrystalReleased(): Boolean = prefs.getBoolean("crystal_released", false)
    fun setCrystalReleased(v: Boolean) { prefs.edit().putBoolean("crystal_released", v).apply() }

    /** Set the first time the reckoning opening plays; retries after a death skip the monologue. */
    fun isReckoningAttempted(): Boolean = prefs.getBoolean("reckoning_attempted", false)
    fun setReckoningAttempted(v: Boolean) { prefs.edit().putBoolean("reckoning_attempted", v).apply() }

    /** One-shot flag: set when the reckoning fight is won; cleared after the bar chatter fires. */
    fun isReckoningJustWon(): Boolean = prefs.getBoolean("reckoning_just_won", false)
    fun setReckoningJustWon(v: Boolean) { prefs.edit().putBoolean("reckoning_just_won", v).apply() }

    /** One-shot flag: set when the reckoning fight is lost; cleared after the bar chatter fires. */
    fun isReckoningJustLost(): Boolean = prefs.getBoolean("reckoning_just_lost", false)
    fun setReckoningJustLost(v: Boolean) { prefs.edit().putBoolean("reckoning_just_lost", v).apply() }

    /** Rounds of the crystal reckoning: incremented once per walk-out (fight entry).
     *  Counts entries, not outcomes — an app kill mid-fight still burned a round. */
    fun getReckoningRounds(): Int = prefs.getInt("reckoning_rounds", 0)
    fun setReckoningRounds(v: Int) { prefs.edit().putInt("reckoning_rounds", v.coerceAtLeast(0)).apply() }
    fun incrementReckoningRounds() = setReckoningRounds(getReckoningRounds() + 1)

    /** Last lost-count pool conversation shown (index into ReckoningRoundChatter.lostCountPool);
     *  -1 = none yet. Persisted so the pool never repeats across sessions. */
    fun getReckoningPoolLast(): Int = prefs.getInt("reckoning_pool_last", -1)
    fun setReckoningPoolLast(v: Int) { prefs.edit().putInt("reckoning_pool_last", v).apply() }

    fun isCrystalUnlocked(): Boolean = prefs.getBoolean("crystal_unlocked", false)
    fun setCrystalUnlocked(unlocked: Boolean) { prefs.edit().putBoolean("crystal_unlocked", unlocked).apply() }

    fun getCrystalPurchased(): Boolean = prefs.getBoolean("crystal_purchased", false)
    fun setCrystalPurchased(purchased: Boolean) { prefs.edit().putBoolean("crystal_purchased", purchased).apply() }

    fun isAwaitingCrystalReveal(): Boolean = prefs.getBoolean("awaiting_crystal_reveal", false)
    fun setAwaitingCrystalReveal(awaiting: Boolean) { prefs.edit().putBoolean("awaiting_crystal_reveal", awaiting).apply() }

    fun isCrystalBroken(): Boolean = prefs.getBoolean("crystal_broken", false)
    fun setCrystalBroken() { prefs.edit().putBoolean("crystal_broken", true).apply() }
    fun clearCrystalBroken() { prefs.edit().putBoolean("crystal_broken", false).apply() }

    fun isAstroLoopFirstEntry(): Boolean = prefs.getBoolean(KEY_ASTRO_LOOP_FIRST_ENTRY, false)
    fun setAstroLoopFirstEntry() { prefs.edit().putBoolean(KEY_ASTRO_LOOP_FIRST_ENTRY, true).apply() }
    fun clearAstroLoopFirstEntry() { prefs.edit().remove(KEY_ASTRO_LOOP_FIRST_ENTRY).apply() }

    fun isFreshLoopStart(): Boolean = prefs.getBoolean(KEY_FRESH_LOOP_START, false)
    fun setFreshLoopStart() { prefs.edit().putBoolean(KEY_FRESH_LOOP_START, true).apply() }
    fun clearFreshLoopStart() { prefs.edit().remove(KEY_FRESH_LOOP_START).apply() }

    fun isAstroLoopShieldConvoShown(): Boolean = prefs.getBoolean(KEY_ASTRO_LOOP_SHIELD_CONVO, false)
    fun setAstroLoopShieldConvoShown() { prefs.edit().putBoolean(KEY_ASTRO_LOOP_SHIELD_CONVO, true).apply() }

    fun getLastAstroRunSeconds(): Float = prefs.getFloat(KEY_LAST_ASTRO_RUN_SECONDS, 0f)
    fun setLastAstroRunSeconds(seconds: Float) {
        prefs.edit().putFloat(KEY_LAST_ASTRO_RUN_SECONDS, seconds).apply()
    }

    fun getAstroLoopBestSeconds(): Float = prefs.getFloat(KEY_ASTRO_LOOP_BEST_SECONDS, 0f)
    fun updateAstroLoopBestSeconds(seconds: Float): Boolean {
        return if (seconds > getAstroLoopBestSeconds()) {
            prefs.edit().putFloat(KEY_ASTRO_LOOP_BEST_SECONDS, seconds).apply()
            true
        } else {
            false
        }
    }

    // Desert flashback
    fun isDesertCompleted(): Boolean = prefs.getBoolean("desert_completed", false)
    fun setDesertCompleted() = prefs.edit().putBoolean("desert_completed", true).apply()
    fun clearDesertCompleted() = prefs.edit().remove("desert_completed").apply()

    fun hasDesertGoodEnding(): Boolean = prefs.getBoolean("desert_good_ending", false)
    fun setDesertGoodEnding() = prefs.edit().putBoolean("desert_good_ending", true).apply()
    fun clearDesertGoodEnding() = prefs.edit().remove("desert_good_ending").apply()

    /** Saves that entered ASTRO_LOOP before the good ending wrote desert_good_ending:
     *  the stage itself proves the good ending happened, so backfill the flag —
     *  otherwise isPostHorrorRun stays true forever and every astro-loop run uses
     *  the post-horror "loop-aware" radio/bar alternates. Idempotent, runs at boot. */
    fun healDesertGoodEnding() {
        if (getStoryStageCode() == StoryStage.ASTRO_LOOP.code && !hasDesertGoodEnding()) {
            setDesertGoodEnding()
        }
    }

    // Casino spin tracking
    fun getTotalCasinoSpins(): Int = prefs.getInt("total_casino_spins", 0)
    fun incrementCasinoSpins() = prefs.edit().putInt("total_casino_spins", getTotalCasinoSpins() + 1).apply()

    // Reset progression but keep yen — called after desert horror path resolution
    fun resetProgressKeepYen() {
        val editor = prefs.edit()
        editor.putStringSet(KEY_UNLOCKED_SHIPS, setOf("ship_blue"))
        editor.putStringSet(KEY_UNLOCKED_PILOTS, setOf("pilot_medic"))
        for (id in listOf("health", "shields", "speed", "damage", "crit", "yen_bonus", "salvage", "magnet")) {
            editor.putInt("upgrade_$id", 0)
        }
        editor.putString(KEY_SELECTED_SHIP, "ship_blue")
        editor.putString(KEY_SELECTED_PILOT, "pilot_medic")
        editor.putInt(KEY_NEXT_PILOT_INDEX, 1)
        editor.putInt(KEY_RUNS_SINCE_PILOT_UNLOCK, 0)
        editor.remove(KEY_FRESH_LOOP_START)
        editor.apply()
    }

    // Unlock everything (for debug rich reset)
    fun unlockAllShipsAndPilots() {
        val allShips = ShipDefinitions.ships.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_UNLOCKED_SHIPS, allShips).apply()
        val allPilots = PilotDefinitions.pilots.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_UNLOCKED_PILOTS, allPilots).apply()
        setNextPilotIndex(PilotDefinitions.getPilotCount())
    }

    // Gating helpers — derive available weapons/passives from unlocked ships/pilots
    fun getUnlockedWeaponIds(): Set<String> {
        val unlocked = mutableSetOf<String>()
        for (ship in ShipDefinitions.ships) {
            if (isShipUnlocked(ship.id)) {
                unlocked.add(ship.startingWeaponId)
            }
        }
        return unlocked
    }

    fun getUnlockedPassiveIds(): Set<String> {
        val unlocked = mutableSetOf<String>()
        for (pilot in PilotDefinitions.pilots) {
            if (isPilotUnlocked(pilot.id)) {
                unlocked.add(pilot.startingPassiveId)
                // combat_drone unlocks alongside tb26 (Astro's passive) for non-Astro use
                if (pilot.startingPassiveId == "tb26") {
                    unlocked.add("combat_drone")
                }
            }
        }
        return unlocked
    }

    companion object {
        private const val PREFS_NAME = "astrohunt_save"
        private const val KEY_YEN = "yen"
        private const val KEY_UNLOCKED_SHIPS = "unlocked_ships"
        private const val KEY_UNLOCKED_PILOTS = "unlocked_pilots"
        private const val KEY_SELECTED_SHIP = "selected_ship"
        private const val KEY_SELECTED_PILOT = "selected_pilot"
        private const val KEY_DISCOVERED_EVOLUTIONS = "discovered_evolutions"
        private const val KEY_BEST_TIME = "best_time"
        private const val KEY_TOTAL_YEN_EARNED = "total_yen_earned"
        private const val KEY_TOTAL_DAMAGE_TAKEN = "total_damage_taken"
        private const val KEY_TOTAL_DEATHS = "total_deaths"
        private const val KEY_TOTAL_KILLS = "total_kills"
        private const val KEY_WEAPONS_DISCOVERED = "weapons_discovered"
        private const val KEY_BEST_KILL_STREAK = "best_kill_streak"
        private const val KEY_BEST_SINGLE_RUN_KILLS = "best_single_run_kills"
        private const val KEY_BEST_SURVIVAL_SECONDS = "best_survival_seconds"
        private const val KEY_NEXT_PILOT_INDEX = "next_pilot_index"
        private const val KEY_RUNS_SINCE_PILOT_UNLOCK = "runs_since_pilot_unlock"
        private const val KEY_CODEX_DISCOVERED = "codex_discovered"
        private const val KEY_CODEX_HINT_GIVEN = "codex_hint_given"
        private const val KEY_ASTRO_HINT_COUNT = "astro_hint_count"
        private const val KEY_ASTRO_HINTED = "astro_hinted"
        private const val KEY_ALL_EVOLUTIONS_HINTED = "all_evolutions_hinted"
        private const val KEY_BEST_CONTINUOUS_FLIGHT = "best_continuous_flight"
        private const val KEY_ASTRO_LOOP_FIRST_ENTRY = "astro_loop_first_entry"
        private const val KEY_FRESH_LOOP_START = "fresh_loop_start"
        private const val KEY_LAST_ASTRO_RUN_SECONDS = "last_astro_run_seconds"
        private const val KEY_ASTRO_LOOP_BEST_SECONDS = "astro_loop_best_seconds"
        private const val KEY_ASTRO_LOOP_SHIELD_CONVO = "astroloop_shield_conversation_shown"

        val UPGRADE_COSTS = listOf(1000, 2500, 7500, 25000, 50000)

        fun getUpgradeCost(currentLevel: Int): Int {
            return if (currentLevel in 0..4) UPGRADE_COSTS[currentLevel] else Int.MAX_VALUE
        }

        // Weapon id renames — applied when reading persisted discovery sets
        fun migrateWeaponIds(ids: Set<String>): Set<String> =
            ids.map { if (it == "torpedo_storm") "hunter_killer" else it }.toSet()
    }
}
