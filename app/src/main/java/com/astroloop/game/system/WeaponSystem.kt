package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.*
import com.astroloop.game.weapon.Weapon
import com.astroloop.game.weapon.WeaponFactory
import com.astroloop.game.weapon.weapons.FrostRing
import com.astroloop.game.weapon.weapons.IonOrbiters
import com.astroloop.game.weapon.weapons.LingeringNova
import com.astroloop.game.weapon.weapons.SpaceMines
import com.astroloop.game.core.SoundManager

class WeaponSystem(
    private val projectilePool: EntityPool<Projectile>
) {
    private val activeWeapons = mutableMapOf<String, Weapon>()

    fun update(
        deltaTime: Float,
        ship: Ship,
        state: GameState,
        targets: List<Entity>,
        asteroids: List<Asteroid> = emptyList()
    ) {
        // Update all weapon cooldowns (Revenge Protocol: 2x fire rate = 2x delta while active)
        activeWeapons.values.forEach { weapon ->
            val isSaw = weapon.id == "energy_saw" || weapon.id == "warp_saw" || weapon.id == "oblivion_beam"
            val effectiveDelta = if (state.revengeActive && !isSaw) deltaTime * 2f else deltaTime
            weapon.update(effectiveDelta)
        }

        // Update orbiters, attached beam positions, and pending mines
        activeWeapons.values.forEach { weapon ->
            when (weapon) {
                is IonOrbiters -> weapon.updateOrbiters(ship.position, deltaTime, state)
                is FrostRing -> weapon.updateOrbiters(ship.position, deltaTime)
                // EnergySaw has no persistent projectile state to update
                is SpaceMines -> weapon.updatePendingMines(deltaTime, projectilePool, state)
                is LingeringNova -> weapon.updatePending(deltaTime, projectilePool, state)
            }
        }

        // Auto-fire all weapons (beat-synced)
        for (weapon in activeWeapons.values) {
            if (weapon.canFire()) {
                // Energy Saw / Warp Saw: no beat sync (continuous damage)
                // Ion Orbiters / Frost Ring: sound on hit, not on fire
                val isSaw = weapon.id == "energy_saw" || weapon.id == "warp_saw" || weapon.id == "oblivion_beam"
                val isOrbiter = weapon.id == "ion_orbiters" || weapon.id == "frost_ring"
                val skipSound = isSaw || isOrbiter

                if (!skipSound && !weapon.beatSynced && SoundManager.beatClock.isRunning) {
                    // First shot: delay to next subdivision
                    // Revenge doubles fire rate for all weapons — halve subdivision so all weapons stay on the beat grid
                    val effectiveCooldownS = if (state.revengeActive) weapon.getCooldown(state) / 2f
                                             else weapon.getCooldown(state)
                    val subdivMs = (effectiveCooldownS * 1000f).toLong()
                    val beatOffset = weapon.beatPhaseOffsetMs + SoundManager.getWeaponBeatOffsetMs(weapon.id)
                    val delayMs = SoundManager.beatClock.msUntilNextSubdivision(
                        subdivMs, System.currentTimeMillis(), beatOffset
                    )
                    weapon.beatSynced = true
                    if (delayMs > 0) {
                        weapon.cooldownTimer = delayMs / 1000f
                        continue
                    }
                }

                weapon.fire(ship, state, projectilePool, targets)
                if (!skipSound) {
                    weapon.beatSynced = true
                    SoundManager.playSFX("sfx_weapon_${weapon.id}", SoundManager.getWeaponSfxVolume(weapon.id))
                }
                // Needle family: re-anchor the next shot to the beat grid at fire time,
                // preventing drift over time. (Resetting beatSynced instead made every
                // cooldown expire a frame past its tick and wait for the following one —
                // halving the fire rate whenever the beat clock was running.)
                val isNeedleFamily = weapon.id == "needle_gun" || weapon.id == "siphon_needles"
                if (isNeedleFamily && SoundManager.beatClock.isRunning) {
                    val effectiveCooldownS = if (state.revengeActive) weapon.getCooldown(state) / 2f
                                             else weapon.getCooldown(state)
                    val subdivMs = (effectiveCooldownS * 1000f).toLong()
                    val beatOffset = weapon.beatPhaseOffsetMs + SoundManager.getWeaponBeatOffsetMs(weapon.id)
                    weapon.cooldownTimer = SoundManager.beatClock.gridAnchoredDelayMs(
                        subdivMs, System.currentTimeMillis(), beatOffset
                    ) / 1000f
                }
            }
        }

        // Note: Evolutions are now presented as choices in UpgradeSystem
        // checkEvolutions(state) - removed auto-evolution

        // Update ship weapon visuals
        ship.updateWeaponVisuals(activeWeapons.keys)
    }

    fun addWeapon(weaponId: String, state: GameState): Boolean {
        val existingWeapon = activeWeapons[weaponId]
        if (existingWeapon != null) {
            // Level up existing weapon
            if (existingWeapon.level < GameConfig.WEAPON_MAX_LEVEL) {
                existingWeapon.onLevelUp()
                state.weaponLevels[weaponId] = existingWeapon.level
                return true
            }
            return false
        }

        // Create new weapon
        val weapon = WeaponFactory.createWeapon(weaponId)
        if (weapon != null) {
            activeWeapons[weaponId] = weapon
            state.weaponLevels[weaponId] = 1
            return true
        }

        return false
    }

    fun updateOrbitersOnly(shipPosition: com.astroloop.game.util.Vector2, deltaTime: Float) {
        activeWeapons.values.forEach { weapon ->
            when (weapon) {
                is IonOrbiters -> weapon.updateOrbiters(shipPosition, deltaTime)
                is FrostRing -> weapon.updateOrbiters(shipPosition, deltaTime)
                else -> {}
            }
        }
    }

    fun removeWeapon(weaponId: String) {
        val weapon = activeWeapons.remove(weaponId)
        // Clean up orbiters/beams if needed
        when (weapon) {
            is IonOrbiters -> weapon.clearOrbiters()
            is FrostRing -> weapon.clearOrbiters()
            // EnergySaw has no persistent state to clear
        }
    }

    fun empHitOrbiters() {
        activeWeapons.values.forEach { weapon ->
            when (weapon) {
                is IonOrbiters -> weapon.fadeOutOrbiters()
                is FrostRing -> weapon.fadeOutOrbiters()
            }
        }
    }

    fun getWeapon(weaponId: String): Weapon? = activeWeapons[weaponId]

    fun getActiveWeaponIds(): Set<String> = activeWeapons.keys.toSet()

    /**
     * Apply an evolution - called when player selects evolution upgrade card.
     */
    fun applyEvolution(baseWeaponId: String, evolvedWeaponId: String, state: GameState) {
        evolveWeapon(baseWeaponId, evolvedWeaponId, state)
    }

    private fun evolveWeapon(baseWeaponId: String, evolvedWeaponId: String, state: GameState) {
        // Remove base weapon
        removeWeapon(baseWeaponId)
        state.weaponLevels.remove(baseWeaponId)

        // Add evolved weapon
        val evolvedWeapon = WeaponFactory.createWeapon(evolvedWeaponId)
        if (evolvedWeapon != null) {
            evolvedWeapon.level = GameConfig.WEAPON_MAX_LEVEL // Start at max level
            activeWeapons[evolvedWeaponId] = evolvedWeapon
            state.weaponLevels[evolvedWeaponId] = evolvedWeapon.level
            state.addEvolution(evolvedWeaponId)
        }
    }

    fun reset() {
        // Clear all orbiters and beams
        activeWeapons.values.forEach { weapon ->
            when (weapon) {
                is IonOrbiters -> weapon.clearOrbiters()
                is FrostRing -> weapon.clearOrbiters()
                // EnergySaw has no persistent state to clear
            }
        }
        activeWeapons.clear()
    }

    /** Reset beat sync on all weapons so they snap to the nearest subdivision on next fire.
     *  Call this when returning from upgrade selection to keep weapons on the beat. */
    fun resetBeatSync() {
        activeWeapons.values.forEach { weapon ->
            weapon.beatSynced = false
            weapon.cooldownTimer = 0f
        }
    }

    fun hasWeapon(weaponId: String): Boolean = activeWeapons.containsKey(weaponId)

    fun getWeaponCount(): Int = activeWeapons.size

    /**
     * Sync activeWeapons with state.weaponLevels (used by debug menu).
     * Creates missing weapons, removes stale ones, and syncs levels.
     */
    fun syncFromState(state: GameState) {
        // Remove weapons no longer in state
        val toRemove = activeWeapons.keys.filter { it !in state.weaponLevels }
        toRemove.forEach { removeWeapon(it) }

        // Add or update weapons from state
        for ((weaponId, targetLevel) in state.weaponLevels) {
            val existing = activeWeapons[weaponId]
            if (existing == null) {
                val weapon = WeaponFactory.createWeapon(weaponId)
                if (weapon != null) {
                    while (weapon.level < targetLevel) weapon.onLevelUp()
                    activeWeapons[weaponId] = weapon
                }
            } else if (existing.level != targetLevel) {
                // Recreate at correct level
                removeWeapon(weaponId)
                val weapon = WeaponFactory.createWeapon(weaponId)
                if (weapon != null) {
                    while (weapon.level < targetLevel) weapon.onLevelUp()
                    activeWeapons[weaponId] = weapon
                }
            }
        }
    }
}
