package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.Firer
import com.astroloop.game.weapon.Weapon
import kotlin.math.cos
import kotlin.math.sin

class EnergySaw : Weapon(
    id = "energy_saw",
    name = "Energy Saw",
    description = "Spinning energy disc that shreds on contact"
) {
    override val baseDamage = 8f
    override val baseCooldown = 0.1f  // Damage tick rate
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 1

    // One blade that grows with level — radius and reach scale together
    val discRadius: Float
        get() = when (level) { 1 -> 20f; 2 -> 28f; 3 -> 36f; 4 -> 45f; else -> 55f }

    val reach: Float
        get() = when (level) { 1 -> 80f; 2 -> 90f; 3 -> 100f; 4 -> 112f; else -> 125f }

    fun getDiscCount(): Int = 1

    fun getTickRate(): Float = baseCooldown

    override fun getDamage(state: GameState): Float {
        // Damage grows only mildly (8 -> 12) against a fixed 0.1s tick rate, so
        // single-target asteroid TTK stays roughly flat — the level-up buys coverage
        val perLevel = when (level) { 1 -> 8f; 2 -> 9f; 3 -> 10f; 4 -> 11f; else -> 12f }
        return perLevel * state.damageMultiplier
    }

    override fun getCooldown(state: GameState): Float = getTickRate()

    override fun getProjectileCount(state: GameState): Int = 1

    // Don't fire projectiles - damage is handled by SawDamageSystem contact check
    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        // No-op: Energy Saw doesn't fire projectiles
    }

    fun getDiscPositions(shipX: Float, shipY: Float, shipRotation: Float): List<Pair<Float, Float>> =
        listOf(Pair(shipX + cos(shipRotation) * reach, shipY + sin(shipRotation) * reach))

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("momentum_drive") > 0
    }

    override fun getEvolutionId(): String? {
        return if (level >= GameConfig.WEAPON_MAX_LEVEL) "warp_saw" else null
    }

    override fun getRequiredPassive(): String? = "momentum_drive"
}
