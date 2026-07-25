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
import kotlin.random.Random

class JackpotMines : Weapon(
    id = "jackpot_mines",
    name = "Gambler's Mines",
    description = "Mines with random bonus effects"
) {
    override val baseDamage = 90f
    override val baseCooldown = 2.0f
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 3
    override fun getProjectileCount(state: GameState): Int = 3 + state.extraProjectiles

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        if (!canFire()) return

        val damage = getDamage(state)
        val count = getProjectileCount(state)

        for (i in 0 until count) {
            // Random offset around the ship so mines don't stack exactly on top of each other
            val offsetAngle = Random.nextFloat() * 2f * PI.toFloat()
            val offsetDist = firer.radius + 10f + Random.nextFloat() * 20f
            val offset = Vector2.fromAngle(offsetAngle, offsetDist)

            val projectile = projectilePool.obtain()
            projectile.initialize(
                x = firer.position.x + offset.x,
                y = firer.position.y + offset.y,
                vx = 0f, // Stationary
                vy = 0f,
                projectileType = ProjectileType.MINE,
                projectileDamage = damage,
                projectileLifetime = 30f
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            // weaponId set to "jackpot_mines" so the hit system can trigger random effects on explosion
            projectile.weaponId = "jackpot_mines"
            projectile.radius = 12f
            projectile.explodeOnDeath = true
            projectile.explosionRadius = 80f
            projectile.explosionDamage = damage
            projectile.color = if (state.isCorruptionRun)
                ShipDefinitions.getEvolutionColor("space_mines", true)
            else
                0xFFFF8844.toInt()
        }

        cooldownTimer = getCooldown(state)
    }
}
