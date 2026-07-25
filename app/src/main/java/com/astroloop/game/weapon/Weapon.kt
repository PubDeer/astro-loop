package com.astroloop.game.weapon

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.EntityPools
import com.astroloop.game.entity.Firer

abstract class Weapon(
    val id: String,
    val name: String,
    val description: String
) {
    var level: Int = 1
    var cooldownTimer: Float = 0f
    var beatSynced: Boolean = false
    open val beatPhaseOffsetMs: Long = 0L

    // Base stats (before level scaling)
    abstract val baseDamage: Float
    abstract val baseCooldown: Float
    abstract val baseProjectileSpeed: Float
    abstract val baseProjectileCount: Int

    // Get scaled stats based on level.
    // Levels that add a projectile (3 and 5) get no damage bonus;
    // their multiplier is held flat from the previous level.
    open fun getDamage(state: GameState): Float {
        val damageBonus = when (level) {
            1 -> 1.0f
            2 -> 1.30f   // +30% damage (no projectile added)
            3 -> 1.30f   // flat — adds +1 projectile instead
            4 -> 1.55f   // +25% on top of level-2 bonus, +1 projectile
            5 -> 1.55f   // flat — adds +1 projectile instead
            else -> 1.55f
        }
        return baseDamage * damageBonus * state.damageMultiplier
    }

    open fun getCooldown(state: GameState): Float {
        return baseCooldown * state.cooldownMultiplier
    }

    open fun getProjectileSpeed(state: GameState): Float {
        return baseProjectileSpeed * state.projectileSpeedMultiplier
    }

    open fun getProjectileCount(state: GameState): Int {
        val base = baseProjectileCount + (level - 1) * 3 / 4
        return base + state.extraProjectiles
    }

    open fun getArea(state: GameState): Float {
        return 1f * state.areaMultiplier
    }

    fun update(deltaTime: Float) {
        if (cooldownTimer > 0) {
            cooldownTimer -= deltaTime
        }
    }

    fun canFire(): Boolean = cooldownTimer <= 0

    abstract fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: com.astroloop.game.entity.EntityPool<com.astroloop.game.entity.Projectile>,
        targets: List<com.astroloop.game.entity.Entity>
    )

    open fun onLevelUp() {
        level++
    }

    // For evolutions - override in specific weapons
    open fun canEvolve(state: GameState): Boolean = false
    open fun getEvolutionId(): String? = null
    open fun getRequiredPassive(): String? = null
}
