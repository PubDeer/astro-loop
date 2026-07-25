package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Firer
import com.astroloop.game.util.Vector2
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.Weapon
import kotlin.random.Random

class FlakBarrage : Weapon(
    id = "flak_barrage",
    name = "Flak Barrage",
    description = "Rapid cluster of exploding shells"
) {
    override val baseDamage = 25f
    override val baseCooldown = 0.5f
    override val baseProjectileSpeed = 450f
    override val baseProjectileCount = 5

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

        for (i in 0 until count) {
            // Wide spread with slight random jitter (matches FlakCannon pattern)
            val spreadAngle = (i - (count - 1) / 2f) * 0.15f
            val angle = firer.rotation + spreadAngle + (Random.nextFloat() - 0.5f) * 0.1f
            val direction = Vector2.fromAngle(angle)

            val projectile = projectilePool.obtain()
            projectile.initialize(
                x = firer.position.x + direction.x * firer.radius,
                y = firer.position.y + direction.y * firer.radius,
                vx = direction.x * speed,
                vy = direction.y * speed,
                projectileType = ProjectileType.FLAK,
                projectileDamage = damage,
                projectileLifetime = 1.5f
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.radius = 6f
            projectile.explodeOnDeath = true
            projectile.explosionRadius = 60f * state.areaMultiplier
            projectile.explosionDamage = damage * 0.7f
            projectile.proximityFuse = true
            projectile.color = ShipDefinitions.getEvolutionColor("flak_cannon", state.isCorruptionRun)
        }

        cooldownTimer = getCooldown(state)
    }
}
