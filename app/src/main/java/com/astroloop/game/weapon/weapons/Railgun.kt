package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.*
import com.astroloop.game.util.Vector2
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.Weapon

class Railgun : Weapon(
    id = "railgun",
    name = "Railgun",
    description = "Piercing sniper shot"
) {
    companion object {
        const val PIERCE_COUNT = 10
    }

    override val baseDamage = 80f
    override val baseCooldown = 1.5f
    override val baseProjectileSpeed = 2000f
    override val baseProjectileCount = 1

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    override fun getProjectileCount(state: GameState): Int {
        return 1 + state.extraProjectiles
    }

    override fun getCooldown(state: GameState): Float {
        return baseCooldown * state.cooldownMultiplier  // No fire rate bonus anymore
    }

    fun getPierceCount(): Int = PIERCE_COUNT

    fun getShotRadius(): Float = when (level) { 1 -> 4f; 2 -> 5.5f; 3 -> 7f; 4 -> 8.5f; else -> 10f }

    fun getShotWidth(): Float = when (level) { 1 -> 3f; 2 -> 5f; 3 -> 7f; 4 -> 9f; else -> 11f }

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
            val offsetAngle = (i - (count - 1) / 2f) * 0.15f
            val angle = firer.rotation + offsetAngle
            val direction = Vector2.fromAngle(angle)

            val projectile = projectilePool.obtain()
            projectile.initialize(
                x = firer.position.x + direction.x * firer.radius,
                y = firer.position.y + direction.y * firer.radius,
                vx = direction.x * speed,
                vy = direction.y * speed,
                projectileType = ProjectileType.BULLET,
                projectileDamage = damage,
                projectileLifetime = 3f
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.piercing = true
            projectile.maxPierces = getPierceCount()
            projectile.radius = getShotRadius()
            projectile.length = 20f
            projectile.width = getShotWidth()
            projectile.color = ShipDefinitions.getWeaponColor("railgun", state.isCorruptionRun)
            projectile.ignoresSpawnShield = true
        }

        cooldownTimer = getCooldown(state)
    }

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("glass_cannon") > 0
    }
    override fun getEvolutionId(): String = "oblivion_beam"
    override fun getRequiredPassive(): String = "glass_cannon"
}
