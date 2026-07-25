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

class ClusterBomb : Weapon(
    id = "cluster_bomb",
    name = "Cluster Bomb",
    description = "Slow bomb that splits into bomblets"
) {
    override val baseDamage = 60f
    override val baseCooldown = 2f
    override val beatPhaseOffsetMs: Long = 1000L
    override val baseProjectileSpeed = 200f
    override val baseProjectileCount = 1

    override fun getDamage(state: GameState): Float {
        return baseDamage * state.damageMultiplier
    }

    override fun getProjectileCount(state: GameState): Int {
        return baseProjectileCount + state.extraProjectiles
    }

    override fun getProjectileSpeed(state: GameState): Float {
        return baseProjectileSpeed * state.projectileSpeedMultiplier
    }

    fun getBombletCount(): Int = level + 1

    private fun getExplosionRadius(state: GameState): Float =
        80f * state.areaMultiplier

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
        val explosionRadius = getExplosionRadius(state)

        for (i in 0 until count) {
            val offsetAngle = (i - (count - 1) / 2f) * 0.2f
            val angle = firer.rotation + offsetAngle
            val direction = Vector2.fromAngle(angle)

            val projectile = projectilePool.obtain()
            projectile.initialize(
                x = firer.position.x + direction.x * firer.radius,
                y = firer.position.y + direction.y * firer.radius,
                vx = direction.x * speed,
                vy = direction.y * speed,
                projectileType = ProjectileType.TORPEDO,
                projectileDamage = damage,
                projectileLifetime = 6f
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.radius = 12f

            projectile.explodeOnDeath = true
            projectile.explosionRadius = explosionRadius
            projectile.explosionDamage = damage * 0.3f

            // Bomblet spawning data
            projectile.bombletCount = getBombletCount()
            projectile.bombletDamage = damage * 0.3f
            projectile.bombletExplosionRadius = explosionRadius * 0.6f
            projectile.hasFragments = true  // fragments are core identity, on from L1
            projectile.fragmentDamage = damage * 0.15f

            projectile.color = ShipDefinitions.getWeaponColor("cluster_bomb", state.isCorruptionRun)
        }

        cooldownTimer = getCooldown(state)
    }

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("magnet_field") > 0
    }

    override fun getEvolutionId(): String = "hunter_killer"
    override fun getRequiredPassive(): String = "magnet_field"
}
