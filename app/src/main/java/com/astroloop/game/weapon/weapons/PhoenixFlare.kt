package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Firer
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.Weapon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class PhoenixFlare : Weapon(
    id = "phoenix_flare",
    name = "Phoenix Flare",
    description = "Three expanding pulse rings that burn everything in their path"
) {
    override val baseDamage = 50f
    override val baseCooldown = 2.0f
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 8  // projectiles per ring

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        if (!canFire()) return

        val damage = getDamage(state)
        val color = ShipDefinitions.getEvolutionColor("solar_storm", state.isCorruptionRun)
        val ringCount = 8
        val ringSpeeds = floatArrayOf(350f, 650f, 950f)
        val ringLifetimes = floatArrayOf(0.7f, 0.65f, 0.6f)
        val ringRadius = 35f * state.areaMultiplier

        // Fire a pulse ring at each of up to 3 on-screen targets (Solar Storm character: strikes at enemy locations)
        val halfW = state.screenWidth / 2f
        val halfH = state.screenHeight / 2f
        val validTargets = targets.filter {
            it.isActive &&
            it.position.x > firer.position.x - halfW && it.position.x < firer.position.x + halfW &&
            it.position.y > firer.position.y - halfH && it.position.y < firer.position.y + halfH
        }.shuffled().take(3)

        for ((r, target) in validTargets.withIndex()) {
            val speed = ringSpeeds[r] * state.areaMultiplier
            val lifetime = ringLifetimes[r]

            for (i in 0 until ringCount) {
                val angle = (2f * PI.toFloat() * i / ringCount)
                val projectile = projectilePool.obtain()
                projectile.initialize(
                    x = target.position.x,
                    y = target.position.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    projectileType = ProjectileType.PLASMA,
                    projectileDamage = damage,
                    projectileLifetime = lifetime
                )
                projectile.isEnemyProjectile = firer.isEnemyFirer
                projectile.weaponId = id
                projectile.radius = ringRadius
                projectile.piercing = true
                projectile.maxPierces = 20
                projectile.color = color
            }
        }

        cooldownTimer = getCooldown(state)
    }
}
