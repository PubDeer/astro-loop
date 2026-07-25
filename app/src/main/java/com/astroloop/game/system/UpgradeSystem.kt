package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.PowerUp
import com.astroloop.game.entity.PowerUpType
import kotlin.random.Random

data class UpgradeOption(
    val id: String,
    val isWeapon: Boolean,
    val isEvolution: Boolean = false,
    val baseWeaponId: String? = null,
    val requiredPassiveId: String? = null,
    val isFallback: Boolean = false,  // True for health/gold options when fully upgraded
    val fallbackType: FallbackType? = null
)

enum class FallbackType {
    HEALTH_RESTORE,  // Restore 20% health
    GOLD_BONUS       // Bonus gold
}

class UpgradeSystem(
    private val powerUpPool: EntityPool<PowerUp>
) {
    private var pendingOptions: List<UpgradeOption> = emptyList()

    var unlockedWeaponIds: Set<String> = emptySet()
    var unlockedPassiveIds: Set<String> = emptySet()

    fun generateUpgradeOptions(state: GameState, fromAsteroid: Boolean = false): List<UpgradeOption> {
        val options = mutableListOf<UpgradeOption>()
        val selected = mutableSetOf<String>()

        // Check if fully upgraded - no more regular upgrades to offer
        if (state.isFullyUpgraded()) {
            // DISABLED: fallback upgrades kept for potential future use
            // if (!fromAsteroid) {
            //     options.add(UpgradeOption(id = "fallback_health", isWeapon = false, isFallback = true, fallbackType = FallbackType.HEALTH_RESTORE))
            //     options.add(UpgradeOption(id = "fallback_gold", isWeapon = false, isFallback = true, fallbackType = FallbackType.GOLD_BONUS))
            //     pendingOptions = options
            //     return options
            // }
            pendingOptions = emptyList()
            return emptyList()
        }

        // Passives that can only be picked once
        val oneTimePassives = setOf("phoenix_core", "extra_weapon_slot", "glass_cannon", "duplicator_core", "lucky_star")

        // Collect owned (non-maxed) upgrades for preferential selection
        val ownedWeapons = mutableListOf<UpgradeOption>()
        val ownedPassives = mutableListOf<UpgradeOption>()
        val newWeapons = mutableListOf<UpgradeOption>()
        val newPassives = mutableListOf<UpgradeOption>()

        // Arsenal gate: count distinct weapons owned (level > 0).
        // A weapon at level N may be offered for N+1 with full probability if ownedWeaponCount >= N.
        // If the gate is not met, a soft probability roll gives a reduced chance of inclusion.
        // Weapons that fail the soft roll go into gatedWeapons for use as a fallback.
        val ownedWeaponCount = state.weaponLevels.count { it.value > 0 }
        val gatedWeapons = mutableListOf<UpgradeOption>()

        // Categorize weapons (only if we can add new weapons or already own them)
        for (weaponDef in WeaponDefinitions.getBaseWeapons().filter { it.id in unlockedWeaponIds }) {
            val currentLevel = state.getWeaponLevel(weaponDef.id)
            if (currentLevel < GameConfig.WEAPON_MAX_LEVEL) {
                if (currentLevel > 0) {
                    // Already own this weapon — apply soft arsenal gate before offering level-up.
                    // Hard gate met  → always eligible.
                    // Hard gate missed → soft roll with probability = ownedWeaponCount / currentLevel.
                    val hardGateMet = ownedWeaponCount >= currentLevel
                    val softGatePasses = hardGateMet ||
                        Random.nextFloat() < ownedWeaponCount.toFloat() / currentLevel.toFloat()
                    if (softGatePasses) {
                        ownedWeapons.add(UpgradeOption(weaponDef.id, isWeapon = true))
                    } else {
                        gatedWeapons.add(UpgradeOption(weaponDef.id, isWeapon = true))
                    }
                } else if (state.canAddNewWeapon()) {
                    // New weapon, only offer if we have slots
                    newWeapons.add(UpgradeOption(weaponDef.id, isWeapon = true))
                }
            }
        }

        // Categorize passives (only if we can add new passives or already own them)
        for (passiveDef in PassiveDefinitions.getAllPassives().filter { it.id in unlockedPassiveIds }) {
            val currentStacks = state.getPassiveStacks(passiveDef.id)

            // Skip one-time passives if already owned
            if (passiveDef.id in oneTimePassives && currentStacks > 0) continue

            // Skip tb26 in first upgrade (handled by generateWeaponOnlyOptions anyway)
            // Skip extra_weapon_slot if already using extra weapon slot
            if (passiveDef.id == "extra_weapon_slot" && state.hasExtraWeaponSlot) continue

            // Drone passive exclusion: tb26 is Astro-only, combat_drone is for everyone else
            if (passiveDef.id == "combat_drone" && state.activePilotId == PassiveDefinitions.ASTRO_PILOT_ID) continue
            if (passiveDef.id == "tb26" && state.activePilotId != PassiveDefinitions.ASTRO_PILOT_ID) continue

            if (currentStacks < GameConfig.PASSIVE_MAX_STACKS) {
                if (currentStacks > 0) {
                    // Already own this passive, can add stacks
                    ownedPassives.add(UpgradeOption(passiveDef.id, isWeapon = false))
                } else if (state.canAddNewPassive()) {
                    // New passive, only offer if we have slots
                    newPassives.add(UpgradeOption(passiveDef.id, isWeapon = false))
                }
            }
        }

        val ownedUpgrades = ownedWeapons + ownedPassives
        val newUpgrades = newWeapons + newPassives

        // Select remaining options: 50% chance to offer owned upgrades (if any exist)
        while (options.size < GameConfig.UPGRADE_CHOICES) {
            val candidate: UpgradeOption? = when {
                // 50% chance to pick from owned upgrades if available
                ownedUpgrades.any { !selected.contains(it.id) } && Random.nextFloat() < 0.5f -> {
                    ownedUpgrades.filter { !selected.contains(it.id) }.randomOrNull()
                }
                // Otherwise pick from new upgrades
                newUpgrades.any { !selected.contains(it.id) } -> {
                    newUpgrades.filter { !selected.contains(it.id) }.randomOrNull()
                }
                // Fallback to any remaining owned
                ownedUpgrades.any { !selected.contains(it.id) } -> {
                    ownedUpgrades.filter { !selected.contains(it.id) }.randomOrNull()
                }
                else -> null
            }

            if (candidate != null) {
                options.add(candidate)
                selected.add(candidate.id)
            } else {
                break // No more candidates available
            }
        }

        // DISABLED: fallback upgrades kept for potential future use
        // When fewer than 3 options available, show fewer cards instead of adding fallbacks
        // if (!fromAsteroid) {
        //     while (options.size < GameConfig.UPGRADE_CHOICES) {
        //         if (!selected.contains("fallback_health")) {
        //             options.add(UpgradeOption(id = "fallback_health", isWeapon = false, isFallback = true, fallbackType = FallbackType.HEALTH_RESTORE))
        //             selected.add("fallback_health")
        //         } else if (!selected.contains("fallback_gold")) {
        //             options.add(UpgradeOption(id = "fallback_gold", isWeapon = false, isFallback = true, fallbackType = FallbackType.GOLD_BONUS))
        //             selected.add("fallback_gold")
        //         } else {
        //             break
        //         }
        //     }
        // }

        // Guarantee at least one weapon and one passive when both types are available
        val allWeapons = ownedWeapons + newWeapons
        val allPassives = ownedPassives + newPassives
        if (allWeapons.isNotEmpty() && allPassives.isNotEmpty()) {
            val hasWeapon = options.any { it.isWeapon && !it.isEvolution }
            val hasPassive = options.any { !it.isWeapon && !it.isFallback }

            if (!hasWeapon) {
                // All cards are passives — replace one with a weapon
                val replaceIdx = options.indexOfLast { !it.isWeapon && !it.isFallback }
                if (replaceIdx >= 0) {
                    val candidate = allWeapons.firstOrNull { !selected.contains(it.id) }
                    if (candidate != null) {
                        selected.remove(options[replaceIdx].id)
                        options[replaceIdx] = candidate
                        selected.add(candidate.id)
                    }
                }
            } else if (!hasPassive) {
                // All cards are weapons — replace one with a passive
                val replaceIdx = options.indexOfLast { it.isWeapon && !it.isEvolution }
                if (replaceIdx >= 0) {
                    val candidate = allPassives.firstOrNull { !selected.contains(it.id) }
                    if (candidate != null) {
                        selected.remove(options[replaceIdx].id)
                        options[replaceIdx] = candidate
                        selected.add(candidate.id)
                    }
                }
            }
        }

        // Guarantee at least one new item if any are available and grid has room.
        // newUpgrades was already built respecting canAddNewWeapon/canAddNewPassive,
        // so its non-emptiness is sufficient to confirm room exists.
        if (newUpgrades.isNotEmpty()) {
            val hasNewInOptions = options.any { opt ->
                !opt.isEvolution && !opt.isFallback && newUpgrades.any { it.id == opt.id }
            }
            if (!hasNewInOptions) {
                // Find the last option that is safe to replace (not an evolution, not a fallback)
                val replaceIndex = options.indexOfLast { !it.isEvolution && !it.isFallback }
                if (replaceIndex >= 0) {
                    val newCandidate = newUpgrades.firstOrNull { !selected.contains(it.id) }
                    if (newCandidate != null) {
                        selected.remove(options[replaceIndex].id)
                        options[replaceIndex] = newCandidate
                        selected.add(newCandidate.id)
                    }
                }
            }
        }

        // Last resort — case 1: pool is completely empty.
        if (options.isEmpty() && gatedWeapons.isNotEmpty()) {
            gatedWeapons.shuffle()
            options.addAll(gatedWeapons.take(GameConfig.UPGRADE_CHOICES))
        }

        // Last resort — case 2: restricted pool (e.g. Scout+Medic with only 1 weapon unlocked).
        // When the gated weapon is the ONLY weapon type available and no weapon made it into options,
        // promote it alongside passives so the player is never permanently locked out of their weapon.
        // This fires even when options is non-empty (passives filled the slots).
        val onlyGatedWeaponsLeft = (ownedWeapons + newWeapons).isEmpty() && gatedWeapons.isNotEmpty()
        if (onlyGatedWeaponsLeft && options.none { it.isWeapon && !it.isEvolution }) {
            val promoted = gatedWeapons.shuffled().first()
            if (options.size < GameConfig.UPGRADE_CHOICES) {
                options.add(promoted)
            } else {
                val replaceIdx = options.indexOfLast { !it.isWeapon && !it.isFallback }
                if (replaceIdx >= 0) {
                    selected.remove(options[replaceIdx].id)
                    options[replaceIdx] = promoted
                }
            }
        }

        pendingOptions = options
        return options
    }

    fun getEligibleEvolutions(state: GameState): List<UpgradeOption> {
        if (state.astroLoopMode) {
            if (state.survivalTime < 480f || state.astroLoopEvolutionUsed) return emptyList()
        }
        val evolutions = mutableListOf<UpgradeOption>()
        for ((weaponId, level) in state.weaponLevels) {
            if (level >= GameConfig.WEAPON_MAX_LEVEL) {
                val weaponDef = WeaponDefinitions.getWeaponDef(weaponId) ?: continue
                val requiredPassive = weaponDef.evolutionPassive ?: continue
                val evolvedId = weaponDef.evolutionWeaponId ?: continue
                val hasRequiredPassive = when (requiredPassive) {
                    "tb26" -> state.getPassiveStacks("tb26") > 0 || state.getPassiveStacks("combat_drone") > 0
                    else   -> state.getPassiveStacks(requiredPassive) > 0
                }
                if (hasRequiredPassive && !state.hasEvolution(evolvedId)) {
                    evolutions.add(UpgradeOption(
                        id = evolvedId, isWeapon = true, isEvolution = true,
                        baseWeaponId = weaponId, requiredPassiveId = requiredPassive
                    ))
                }
            }
        }
        return evolutions
    }

    /**
     * Returns the list of owned weapons that currently pass the arsenal gate and are
     * therefore eligible to be offered as a level-up.
     *
     * Gate rule: a weapon at level N may be offered N+1 only if the player owns >= N
     * distinct weapons total. This is a testable pure view over state — it does not
     * affect pending options.
     */
    fun getEligibleLevelUps(state: GameState): List<UpgradeOption> {
        val ownedWeaponCount = state.weaponLevels.count { it.value > 0 }
        val eligible = mutableListOf<UpgradeOption>()
        for (weaponDef in WeaponDefinitions.getBaseWeapons().filter { it.id in unlockedWeaponIds }) {
            val currentLevel = state.getWeaponLevel(weaponDef.id)
            if (currentLevel in 1 until GameConfig.WEAPON_MAX_LEVEL) {
                if (ownedWeaponCount >= currentLevel) {
                    eligible.add(UpgradeOption(weaponDef.id, isWeapon = true))
                }
            }
        }
        return eligible
    }

    fun generateEvolutionOptions(state: GameState): List<UpgradeOption> {
        val eligible = getEligibleEvolutions(state).shuffled()
        val options = eligible.take(3)  // Max 3 shown
        pendingOptions = options
        return options
    }

    fun selectOption(index: Int): UpgradeOption? {
        if (index < 0 || index >= pendingOptions.size) return null
        val selected = pendingOptions[index]
        pendingOptions = emptyList()
        return selected
    }

    fun spawnPowerUp(x: Float, y: Float, state: GameState): PowerUp? {
        // Random drop chance, modified by luck
        val dropChance = GameConfig.POWERUP_DROP_CHANCE * state.dropRateMultiplier * state.getSalvageMultiplier()
        if (Random.nextFloat() > dropChance) return null

        val powerUp = powerUpPool.obtain()

        // Determine type (70% weapon, 30% passive)
        val isWeapon = Random.nextFloat() < 0.7f

        // Pick a random valid item
        val itemId = if (isWeapon) {
            val validWeapons = WeaponDefinitions.getBaseWeapons().filter { it.id in unlockedWeaponIds && state.getWeaponLevel(it.id) < GameConfig.WEAPON_MAX_LEVEL }
            validWeapons.randomOrNull()?.id
        } else {
            val validPassives = PassiveDefinitions.getAllPassives().filter { it.id in unlockedPassiveIds && state.getPassiveStacks(it.id) < GameConfig.PASSIVE_MAX_STACKS
                && !(it.id == "combat_drone" && state.activePilotId == PassiveDefinitions.ASTRO_PILOT_ID)
                && !(it.id == "tb26" && state.activePilotId != PassiveDefinitions.ASTRO_PILOT_ID) }
            validPassives.randomOrNull()?.id
        }

        if (itemId == null) {
            powerUpPool.free(powerUp)
            return null
        }

        powerUp.initialize(
            x = x,
            y = y,
            powerUpType = if (isWeapon) PowerUpType.WEAPON else PowerUpType.PASSIVE,
            id = itemId
        )

        return powerUp
    }

    fun getPendingOptions(): List<UpgradeOption> = pendingOptions

    fun hasPendingOptions(): Boolean = pendingOptions.isNotEmpty()

    fun clearPendingOptions() {
        pendingOptions = emptyList()
    }

    /**
     * Generate weapon-only options for the first upgrade pick.
     * Excludes utility weapons and tb26 (which is now a passive).
     */
    fun generateWeaponOnlyOptions(state: GameState): List<UpgradeOption> {
        val options = mutableListOf<UpgradeOption>()
        val candidates = mutableListOf<UpgradeOption>()

        for (weaponDef in WeaponDefinitions.getBaseWeapons().filter { it.id in unlockedWeaponIds }) {
            candidates.add(UpgradeOption(weaponDef.id, isWeapon = true))
        }

        // Select random unique options
        candidates.shuffle()
        for (candidate in candidates) {
            if (options.size < GameConfig.UPGRADE_CHOICES) {
                options.add(candidate)
            }
        }

        pendingOptions = options
        return options
    }

    /**
     * Get available upgrades that the player can still use (not maxed).
     * Used by enemy drop system to only drop useful upgrades.
     * Respects slot limits.
     */
    fun getAvailableUpgrades(state: GameState): List<UpgradeOption> {
        val available = mutableListOf<UpgradeOption>()
        val oneTimePassives = setOf("phoenix_core", "extra_weapon_slot", "glass_cannon", "duplicator_core", "lucky_star")

        // Weapons that can be leveled up (only if owned or have slots)
        for (weaponDef in WeaponDefinitions.getBaseWeapons().filter { it.id in unlockedWeaponIds }) {
            val currentLevel = state.getWeaponLevel(weaponDef.id)
            if (currentLevel < GameConfig.WEAPON_MAX_LEVEL) {
                if (currentLevel > 0 || state.canAddNewWeapon()) {
                    available.add(UpgradeOption(weaponDef.id, isWeapon = true))
                }
            }
        }

        // Passives that can be stacked (only if owned or have slots)
        for (passiveDef in PassiveDefinitions.getAllPassives().filter { it.id in unlockedPassiveIds }) {
            val currentStacks = state.getPassiveStacks(passiveDef.id)

            // Skip one-time passives if already owned
            if (passiveDef.id in oneTimePassives && currentStacks > 0) continue
            if (passiveDef.id == "extra_weapon_slot" && state.hasExtraWeaponSlot) continue

            // Drone passive exclusion: tb26 is Astro-only, combat_drone is for everyone else
            if (passiveDef.id == "combat_drone" && state.activePilotId == PassiveDefinitions.ASTRO_PILOT_ID) continue
            if (passiveDef.id == "tb26" && state.activePilotId != PassiveDefinitions.ASTRO_PILOT_ID) continue

            if (currentStacks < GameConfig.PASSIVE_MAX_STACKS) {
                if (currentStacks > 0 || state.canAddNewPassive()) {
                    available.add(UpgradeOption(passiveDef.id, isWeapon = false))
                }
            }
        }

        return available
    }

}
