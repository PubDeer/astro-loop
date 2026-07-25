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

class SiphonNeedles : Weapon(
    id = "siphon_needles",
    name = "Siphon Needles",
    description = "Fast piercing needles that heal the ship on hit"
) {
    override val baseDamage = 9f
    override val baseCooldown = 0.25f
    override val baseProjectileSpeed = 900f
    override val baseProjectileCount = 7

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
            // Same spread constant as NeedleGun (0.08f) to keep identical arc
            val spreadAngle = (i - (count - 1) / 2f) * 0.08f
            val angle = firer.rotation + spreadAngle
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
            // weaponId set to "siphon_needles" so the hit system can detect and apply healing
            projectile.weaponId = "siphon_needles"
            projectile.piercing = true
            projectile.maxPierces = 1
            projectile.length = 14f
            projectile.width = 2f
            projectile.color = ShipDefinitions.getEvolutionColor("needle_gun", state.isCorruptionRun)
        }

        cooldownTimer = getCooldown(state)
    }
}
