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
import kotlin.math.PI

class StormCannon : Weapon(
    id = "storm_cannon",
    name = "Storm Cannon",
    description = "Rotating bullet storm — fills space in a persistent spiral"
) {
    override val baseDamage = 18f
    override val baseCooldown = 0.25f
    override val baseProjectileSpeed = 700f
    override val baseProjectileCount = 3

    // Spiral state — persists across shots so the pattern rotates continuously
    private var spiralAngle: Float = 0f
    private val spiralStep = PI.toFloat() / 6f  // 30 degrees per shot

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
        val color = ShipDefinitions.getEvolutionColor("pulse_cannon", state.isCorruptionRun)

        // Fire `count` bullets evenly spread around the current spiral angle
        val sectorAngle = 2f * PI.toFloat() / count
        for (i in 0 until count) {
            val angle = spiralAngle + sectorAngle * i
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
            projectile.color = color
        }

        // Advance spiral — full circle every 12 shots (3 seconds at base cooldown)
        spiralAngle += spiralStep

        cooldownTimer = getCooldown(state)
    }
}
