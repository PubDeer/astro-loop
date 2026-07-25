package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.*
import com.astroloop.game.util.Vector2
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.Weapon

class NeedleGun : Weapon(
    id = "needle_gun",
    name = "Needle Gun",
    description = "Rapid piercing needles"
) {
    override val baseDamage = 5f
    override val baseCooldown = 0.25f
    override val baseProjectileSpeed = 800f
    override val baseProjectileCount = 3

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    override fun getProjectileCount(state: GameState): Int = level + 2 + state.extraProjectiles

    override fun getCooldown(state: GameState): Float {
        return baseCooldown * state.cooldownMultiplier
    }

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
            val spreadAngle = (i - (count - 1) / 2f) * 0.08f  // Tight spread
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
            projectile.weaponId = id
            projectile.piercing = true
            projectile.maxPierces = 3  // Pierces through 3 targets
            projectile.length = 14f
            projectile.width = 2f
            projectile.color = ShipDefinitions.getWeaponColor("needle_gun", state.isCorruptionRun)
        }

        cooldownTimer = getCooldown(state)
    }

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("nano_repair") > 0
    }
    override fun getEvolutionId(): String = "siphon_needles"
    override fun getRequiredPassive(): String = "nano_repair"
}
