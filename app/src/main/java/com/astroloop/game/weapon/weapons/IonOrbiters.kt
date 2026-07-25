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
import kotlin.math.PI

class IonOrbiters : Weapon(
    id = "ion_orbiters",
    name = "Ion Orbiters",
    description = "Orbiting energy spheres"
) {
    override val baseDamage = 19f
    override val baseCooldown = 4.0f
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 2

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    private var activeOrbiters = mutableListOf<Projectile>()
    private var currentOrbitCenter: Vector2? = null
    private var baseOrbitAngle: Float = 0f
    private var currentOrbitSpeed: Float = 3f

    override fun getProjectileCount(state: GameState): Int {
        val base = when (level) {
            1 -> 2; 2 -> 3; 3 -> 4; 4 -> 5; 5 -> 6; else -> 6
        }
        return base + state.extraProjectiles
    }

    private fun getOrbitRadius(state: GameState): Float {
        // Increased radius slightly for better coverage
        val base = when (level) {
            1 -> 70f
            2 -> 85f
            3 -> 100f
            4 -> 105f
            5 -> 110f
            else -> 110f
        }
        return base * state.areaMultiplier
    }

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        // Remove inactive orbiters
        activeOrbiters.removeAll { !it.isActive }

        // Update orbit center reference
        if (currentOrbitCenter == null) {
            currentOrbitCenter = Vector2()
        }
        currentOrbitCenter!!.set(firer.position)

        // Spawn new orbiters if needed
        val targetCount = getProjectileCount(state)
        val damage = getDamage(state)
        val orbitRadius = getOrbitRadius(state)
        currentOrbitSpeed = 3f + level * 0.3f

        while (activeOrbiters.size < targetCount) {
            val projectile = projectilePool.obtain()

            // Spawn new orbiter at last existing orbiter's angle
            val spawnAngle = if (activeOrbiters.isNotEmpty()) {
                activeOrbiters.last().orbitAngle
            } else {
                baseOrbitAngle
            }

            projectile.initialize(
                x = firer.position.x,
                y = firer.position.y,
                vx = 0f,
                vy = 0f,
                projectileType = ProjectileType.ORBITER,
                projectileDamage = damage,
                projectileLifetime = 999f // Persist until weapon changes
            )
            projectile.isEnemyProjectile = firer.isEnemyFirer
            projectile.weaponId = id
            projectile.orbitRadius = orbitRadius
            projectile.orbitSpeed = 0f  // IonOrbiters manages all angles centrally
            projectile.orbitCenter = currentOrbitCenter
            projectile.orbitAngle = spawnAngle
            projectile.radius = 10f * state.areaMultiplier
            projectile.piercing = true
            projectile.maxPierces = 1000
            projectile.color = ShipDefinitions.getWeaponColor("ion_orbiters", state.isCorruptionRun)
            projectile.fadeAlpha = 0f  // Fade in new orbiters

            activeOrbiters.add(projectile)
        }

        // Lerp all angles toward evenly-spaced targets (new orbiters slide into formation)
        val total = activeOrbiters.size
        for ((index, orbiter) in activeOrbiters.withIndex()) {
            val targetAngle = baseOrbitAngle + (2f * PI.toFloat() * index / total)
            orbiter.orbitAngle = lerpAngle(orbiter.orbitAngle, targetAngle, 0.15f)
            orbiter.orbitRadius = orbitRadius
            orbiter.damage = damage
        }

        cooldownTimer = getCooldown(state)
    }

    fun updateOrbiters(shipPosition: Vector2, deltaTime: Float, state: GameState? = null) {
        currentOrbitCenter?.set(shipPosition)

        // Advance the shared base angle (double speed during Revenge Protocol)
        val effectiveSpeed = if (state?.revengeActive == true) currentOrbitSpeed * 2f else currentOrbitSpeed
        baseOrbitAngle += effectiveSpeed * deltaTime

        // Lerp all orbiters toward evenly-spaced targets (smooth redistribution)
        activeOrbiters.removeAll { !it.isActive }
        val total = activeOrbiters.size
        for ((index, orbiter) in activeOrbiters.withIndex()) {
            val targetAngle = baseOrbitAngle + (2f * PI.toFloat() * index / total)
            orbiter.orbitAngle = lerpAngle(orbiter.orbitAngle, targetAngle, 0.15f)
        }
    }

    fun clearOrbiters() {
        activeOrbiters.forEach { it.isActive = false }
        activeOrbiters.clear()
    }

    fun fadeOutOrbiters() {
        activeOrbiters.forEach { it.lifetime = it.age + 0.5f }
        activeOrbiters.clear()
        cooldownTimer = Float.MAX_VALUE
    }

    fun makeOrbitersVisible() {
        for (orb in activeOrbiters) orb.fadeAlpha = 1f
    }

    override fun canEvolve(state: GameState): Boolean {
        return level >= GameConfig.WEAPON_MAX_LEVEL &&
               state.getPassiveStacks("cryo_field") > 0
    }

    override fun getEvolutionId(): String = "frost_ring"
    override fun getRequiredPassive(): String = "cryo_field"

    /** Lerp between angles, taking the shortest path around the circle */
    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = to - from
        // Normalize to [-PI, PI]
        while (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
        while (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
        return from + diff * t
    }
}
