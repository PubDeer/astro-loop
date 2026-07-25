package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Firer
import com.astroloop.game.util.Vector2
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.Weapon
import kotlin.math.PI

class NovaBlast : Weapon(
    id = "nova_blast",
    name = "Nova Blast",
    description = "Periodic AOE explosion"
) {
    override val baseDamage = 40f
    override val baseCooldown = 4f
    override val beatPhaseOffsetMs: Long = 2000L
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 1

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    override fun getCooldown(state: GameState): Float {
        return baseCooldown * state.cooldownMultiplier
    }

    private fun getBlastRadius(state: GameState): Float {
        val base = when (level) {
            1 -> 180f
            2 -> 234f
            3 -> 281f
            4 -> 316f
            else -> 351f
        }
        return base * state.areaMultiplier
    }

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        if (!canFire()) return

        val damage = getDamage(state)
        val blastRadius = getBlastRadius(state)

        // Create expanding ring of damage
        val ringCount = 8
        for (i in 0 until ringCount) {
            val angle = (2 * PI.toFloat() * i / ringCount)
            val projectile = projectilePool.obtain()

            projectile.initialize(
                x = firer.position.x,
                y = firer.position.y,
                vx = kotlin.math.cos(angle) * blastRadius * 3f, // Expand outward
                vy = kotlin.math.sin(angle) * blastRadius * 3f,
                projectileType = ProjectileType.PLASMA,
                projectileDamage = damage,
                projectileLifetime = 0.3f
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.radius = blastRadius / 3
            projectile.piercing = true
            projectile.maxPierces = 1000
            projectile.color = ShipDefinitions.getWeaponColor("nova_blast", state.isCorruptionRun)
            projectile.orbitCenter = firer.position  // Track ship movement
            projectile.beamOrigin = Vector2(firer.position.x, firer.position.y)  // Fire position snapshot
        }

        cooldownTimer = getCooldown(state)
    }

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("revenge_protocol") > 0
    }

    override fun getEvolutionId(): String = "lingering_nova"
    override fun getRequiredPassive(): String = "revenge_protocol"
}
