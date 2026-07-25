package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.*
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.Weapon

class SolarStorm : Weapon(
    id = "solar_storm",
    name = "Solar Storm",
    description = "Random piercing strikes"
) {
    override val baseDamage = 29f
    override val baseCooldown = 2.0f
    override val beatPhaseOffsetMs: Long = 500L
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 1

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    private fun getTargetCount(): Int = level

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        if (!canFire()) return

        var damage = getDamage(state)

        // Apply passive damage bonuses (same as collision path)
        if (state.momentumDamageBonus > 0f && firer.velocity.lengthSquared() > 100f) {
            damage *= (1f + state.momentumDamageBonus)
        }
        val targetCount = getTargetCount()

        // Pick random targets - only target enemies visible on screen
        val halfW = state.screenWidth / 2f
        val halfH = state.screenHeight / 2f
        val validTargets = targets.filter {
            it.isActive &&
            it.position.x > firer.position.x - halfW && it.position.x < firer.position.x + halfW &&
            it.position.y > firer.position.y - halfH && it.position.y < firer.position.y + halfH &&
            (it !is Asteroid || it.fragmentImmunityTimer <= 0f)
        }.shuffled().take(targetCount)

        val weaponColor = ShipDefinitions.getWeaponColor("solar_storm", state.isCorruptionRun)
        for (target in validTargets) {
            // Lucky Rounds: crit chance per target
            var targetDamage = damage
            val isCrit = state.rollCrit()
            if (isCrit) targetDamage *= GameConfig.CRIT_DAMAGE_MULTIPLIER
            target.takeDamage(targetDamage)

            // Lightning visual ON the target (not at ship)
            val projectile = projectilePool.obtain()
            projectile.initialize(
                x = target.position.x,
                y = target.position.y,
                vx = 0f,
                vy = 0f,
                projectileType = ProjectileType.LIGHTNING,
                projectileDamage = targetDamage,  // Store damage for damage number display (includes crit)
                projectileLifetime = 0.4f  // Slightly longer for dramatic effect
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.color = weaponColor
            projectile.radius = 60f  // Bigger, more dramatic effect
            projectile.bounceCount = 99  // Mark as instant-hit (won't fork or do collision damage)
            projectile.isCrit = isCrit
        }

        cooldownTimer = getCooldown(state)
    }

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("phoenix_core") > 0
    }
    override fun getEvolutionId(): String = "phoenix_flare"
    override fun getRequiredPassive(): String = "phoenix_core"
}
