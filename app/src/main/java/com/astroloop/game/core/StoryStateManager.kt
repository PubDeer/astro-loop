package com.astroloop.game.core

import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.entity.Boss

object StoryStateManager {
    // --- stage / loop ---
    fun stage(p: PersistenceManager): StoryStage = StoryStage.fromCode(p.getStoryStageCode())
    fun isCorrupted(p: PersistenceManager): Boolean = stage(p) == StoryStage.CORRUPTION
    fun isAstroLoop(p: PersistenceManager): Boolean = stage(p) == StoryStage.ASTRO_LOOP
    fun loop(p: PersistenceManager): Int = p.getStoryLoop()
    fun hasLoopedBefore(p: PersistenceManager): Boolean = p.getStoryLoop() >= 2

    /** Music set name for the current stage: drives bgm_${set}_hangar / _combat_loop. */
    fun stageMusicSet(p: PersistenceManager): String = when (stage(p)) {
        StoryStage.NORMAL -> "normal"
        StoryStage.CORRUPTION -> "corruption"
        StoryStage.ASTRO_LOOP -> "astroloop"
    }

    fun isAstroCorruptionRun(p: PersistenceManager): Boolean =
        isCorrupted(p) && p.getSelectedPilotId() == "pilot_astro"
    fun isNonAstroCorruptionRun(p: PersistenceManager): Boolean =
        isCorrupted(p) && p.getSelectedPilotId() != "pilot_astro"

    // --- carried over verbatim from StoryPhaseManager ---

    /**
     * The order in which crewmates are encountered as corrupted bosses.
     * Reverse of recruitment order (strongest first, Medic last).
     * Astro is excluded — Astro is always the player.
     */
    val CREWMATE_ENCOUNTER_ORDER = listOf(
        "pilot_havoc",
        "pilot_unit7",
        "pilot_whiskers",
        "pilot_kraken",
        "pilot_fang",
        "pilot_ember",
        "pilot_dash",
        "pilot_frost",
        "pilot_brutus",
        "pilot_rascal",
        "pilot_medic"
    )

    /**
     * Maps each fleet ship to its assigned pilot.
     * 12 pairs (all ships including Specter/Astro). The player's ship is
     * filtered at runtime in FleetSystem.arrive() so no duplicate appears.
     * Ship IDs may not match color names due to 2026-02-10 color swap.
     */
    val FLEET_MAPPING = mapOf(
        "ship_blue" to "pilot_medic",
        "ship_green" to "pilot_rascal",
        "ship_orange" to "pilot_brutus",
        "ship_cyan" to "pilot_frost",
        "ship_lime" to "pilot_dash",
        "ship_yellow" to "pilot_ember",
        "ship_indigo" to "pilot_fang",
        "ship_red" to "pilot_kraken",
        "ship_coral" to "pilot_whiskers",
        "ship_magenta" to "pilot_unit7",
        "ship_purple" to "pilot_havoc",
        "ship_white" to "pilot_astro"
    )

    // --- Query helpers ---

    /** True if this pilot has been defeated as a corrupted boss and is now dead. */
    fun isPilotDead(persistence: PersistenceManager, pilotId: String): Boolean {
        return persistence.getDeadPilots().contains(pilotId)
    }

    /** True if this ship's pilot has been defeated and the ship is destroyed. */
    fun isShipDead(persistence: PersistenceManager, shipId: String): Boolean {
        return persistence.getDeadShips().contains(shipId)
    }

    /** How many pilots are still alive (not in dead_pilots set). */
    fun getAlivePilotCount(persistence: PersistenceManager): Int {
        val dead = persistence.getDeadPilots()
        return CREWMATE_ENCOUNTER_ORDER.count { !dead.contains(it) }
    }

    /** True when all 11 crewmates have been defeated. */
    fun allCrewDead(persistence: PersistenceManager): Boolean {
        return getAlivePilotCount(persistence) == 0
    }

    /** True when the Crystal Ship should become available (all crew dead, not yet purchased). */
    fun shouldUnlockCrystal(persistence: PersistenceManager): Boolean {
        return allCrewDead(persistence) && !persistence.getCrystalPurchased()
    }

    /** Get the ship ID assigned to a given pilot, or null if not in the fleet. */
    fun getShipForPilot(pilotId: String): String? {
        return FLEET_MAPPING.entries.find { it.value == pilotId }?.key
    }

    /** Get the pilot ID assigned to a given ship, or null if not in the fleet. */
    fun getPilotForShip(shipId: String): String? {
        return FLEET_MAPPING[shipId]
    }

    // --- Color utilities ---

    /**
     * Corrupt a color by reducing brightness to 50%.
     * Used for corrupted pilot portraits and UI elements.
     */
    fun corruptColor(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val r = (((color ushr 16) and 0xFF) * 0.5f).toInt()
        val g = (((color ushr 8) and 0xFF) * 0.5f).toInt()
        val b = ((color and 0xFF) * 0.5f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Corrupt a ship color — all corrupted ships are boss-red.
     * Used for corrupted fleet ship rendering.
     */
    @Suppress("UNUSED_PARAMETER")
    fun corruptShipColor(color: Int): Int {
        return Boss.CORRUPTION_COLOR  // All corrupted ships are boss-red
    }
}
