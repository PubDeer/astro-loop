package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Firer
import com.astroloop.game.util.Vector2
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.weapon.Weapon

class AutonomousAce : Weapon(
    id = "autonomous_ace",
    name = "Autonomous Ace",
    description = "Supercharged homing missiles with drone AI upgrade"
) {
    override val baseDamage = 45f
    override val baseCooldown = 1.0f
    override val baseProjectileSpeed = 500f
    override val baseProjectileCount = 5
    override fun getProjectileCount(state: GameState): Int = 5 + state.extraProjectiles

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

        val sortedTargets = targets
            .filter { it.isActive }
            .sortedBy { firer.position.distanceSquared(it.position) }

        for (i in 0 until count) {
            val offsetAngle = (i - (count - 1) / 2f) * 0.3f
            val angle = firer.rotation + offsetAngle
            val direction = Vector2.fromAngle(angle)

            val projectile = projectilePool.obtain()
            projectile.initialize(
                x = firer.position.x + direction.x * firer.radius,
                y = firer.position.y + direction.y * firer.radius,
                vx = direction.x * speed,
                vy = direction.y * speed,
                projectileType = ProjectileType.MISSILE,
                projectileDamage = damage,
                projectileLifetime = 4f
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.homingStrength = 2.5f
            projectile.target = sortedTargets.getOrNull(i % sortedTargets.size.coerceAtLeast(1))
            projectile.color = if (!state.isCorruptionRun &&
                state.activePilotId == PassiveDefinitions.ASTRO_PILOT_ID && !state.astroLoopMode)
                PassiveDefinitions.DRONE_COLOR_TB26   // TB-26-X: steel blue, matches the drone
            else
                ShipDefinitions.getEvolutionColor("homing_missiles", state.isCorruptionRun)
        }

        cooldownTimer = getCooldown(state)
    }
}
