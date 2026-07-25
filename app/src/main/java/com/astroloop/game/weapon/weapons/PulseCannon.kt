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

class PulseCannon : Weapon(
    id = "pulse_cannon",
    name = "Pulse Cannon",
    description = "Auto-aiming energy bolts"
) {
    override val baseDamage = 15f
    override val baseCooldown = 0.5f
    override val baseProjectileSpeed = 600f
    override val baseProjectileCount = 1

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    override fun getProjectileCount(state: GameState): Int = level + state.extraProjectiles

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        if (!canFire()) return

        val damage = getDamage(state)
        val speed = getProjectileSpeed(state)
        val count = getProjectileCount(state)

        // Aimed mode: fire toward nearest target instead of ship facing direction
        val nearestTarget = targets
            .filter { it.isActive }
            .minByOrNull { firer.position.distanceSquared(it.position) }

        val baseAngle = if (nearestTarget != null) {
            // Aim toward nearest target
            val toTarget = nearestTarget.position - firer.position
            toTarget.angle()
        } else {
            // Fallback to ship facing direction
            firer.rotation
        }

        val spreadAngle = if (count > 1) PI.toFloat() / 12f else 0f // 15 degree spread

        for (i in 0 until count) {
            val angle = if (count == 1) {
                baseAngle
            } else {
                baseAngle + spreadAngle * (i - (count - 1) / 2f)
            }

            val direction = Vector2.fromAngle(angle)
            val projectile = projectilePool.obtain()

            projectile.initialize(
                x = firer.position.x + direction.x * firer.radius,
                y = firer.position.y + direction.y * firer.radius,
                vx = direction.x * speed,
                vy = direction.y * speed,
                projectileType = ProjectileType.BULLET,
                projectileDamage = damage,
                projectileLifetime = 2f
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.color = ShipDefinitions.getWeaponColor("pulse_cannon", state.isCorruptionRun)
        }

        cooldownTimer = getCooldown(state)
    }

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("duplicator_core") > 0
    }

    override fun getEvolutionId(): String = "storm_cannon"
    override fun getRequiredPassive(): String = "duplicator_core"
}
