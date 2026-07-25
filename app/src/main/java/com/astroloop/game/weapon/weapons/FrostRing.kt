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

class FrostRing : Weapon(
    id = "frost_ring",
    name = "Frost Ring",
    description = "Permanent frost ring"
) {
    override val baseDamage = 20f
    override val baseCooldown = 2f
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 18  // 6 inner + 12 outer (approximate, not used directly)

    // Shared orbit center (both rings track ship)
    private var currentOrbitCenter: Vector2? = null

    // Inner ring — clockwise, L5 IonOrbiters equivalent
    private var innerOrbiters = mutableListOf<Projectile>()
    private var innerBaseAngle: Float = 0f
    private val innerOrbitSpeed: Float = 3.5f   // clockwise (+)

    // Outer ring — counter-clockwise
    private var outerOrbiters = mutableListOf<Projectile>()
    private var outerBaseAngle: Float = 0f
    private val outerOrbitSpeed: Float = 2.5f   // counter-clockwise (-)

    private fun getInnerCount(state: GameState): Int = 6 + state.extraProjectiles

    private fun getInnerRadius(state: GameState): Float = 110f * state.areaMultiplier

    private fun getOuterCount(): Int = 12

    private fun getOuterRadius(state: GameState): Float = 225f * state.areaMultiplier

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        innerOrbiters.removeAll { !it.isActive }
        outerOrbiters.removeAll { !it.isActive }

        if (currentOrbitCenter == null) {
            currentOrbitCenter = Vector2()
        }
        currentOrbitCenter!!.set(firer.position)

        val damage = getDamage(state)
        val color = ShipDefinitions.getEvolutionColor("ion_orbiters", state.isCorruptionRun)

        // --- Inner ring ---
        val innerCount = getInnerCount(state)
        val innerRadius = getInnerRadius(state)
        val orbRadius = 10f * state.areaMultiplier

        while (innerOrbiters.size < innerCount) {
            val spawnAngle = if (innerOrbiters.isNotEmpty()) innerOrbiters.last().orbitAngle else innerBaseAngle
            val p = projectilePool.obtain()
            p.initialize(
                x = firer.position.x, y = firer.position.y,
                vx = 0f, vy = 0f,
                projectileType = ProjectileType.ORBITER,
                projectileDamage = damage,
                projectileLifetime = 999f
            )
            p.isEnemyProjectile = firer.isEnemyFirer
            p.weaponId = id
            p.orbitRadius = innerRadius
            p.orbitSpeed = 0f
            p.orbitCenter = currentOrbitCenter
            p.orbitAngle = spawnAngle
            p.radius = orbRadius
            p.piercing = true
            p.maxPierces = 1000
            p.color = color
            p.fadeAlpha = 0f
            innerOrbiters.add(p)
        }

        val innerTotal = innerOrbiters.size
        for ((i, orb) in innerOrbiters.withIndex()) {
            val target = innerBaseAngle + (2f * PI.toFloat() * i / innerTotal)
            orb.orbitAngle = lerpAngle(orb.orbitAngle, target, 0.15f)
            orb.orbitRadius = innerRadius
            orb.damage = damage
        }

        // --- Outer ring ---
        val outerCount = getOuterCount()
        val outerRadius = getOuterRadius(state)

        while (outerOrbiters.size < outerCount) {
            val spawnAngle = if (outerOrbiters.isNotEmpty()) outerOrbiters.last().orbitAngle else outerBaseAngle
            val p = projectilePool.obtain()
            p.initialize(
                x = firer.position.x, y = firer.position.y,
                vx = 0f, vy = 0f,
                projectileType = ProjectileType.ORBITER,
                projectileDamage = damage,
                projectileLifetime = 999f
            )
            p.isEnemyProjectile = firer.isEnemyFirer
            p.weaponId = id
            p.orbitRadius = outerRadius
            p.orbitSpeed = 0f
            p.orbitCenter = currentOrbitCenter
            p.orbitAngle = spawnAngle
            p.radius = orbRadius
            p.piercing = true
            p.maxPierces = 1000
            p.color = color
            p.fadeAlpha = 0f
            outerOrbiters.add(p)
        }

        val outerTotal = outerOrbiters.size
        for ((i, orb) in outerOrbiters.withIndex()) {
            val target = outerBaseAngle + (2f * PI.toFloat() * i / outerTotal)
            orb.orbitAngle = lerpAngle(orb.orbitAngle, target, 0.15f)
            orb.orbitRadius = outerRadius
            orb.damage = damage
        }

        cooldownTimer = getCooldown(state)
    }

    fun updateOrbiters(shipPosition: Vector2, deltaTime: Float) {
        currentOrbitCenter?.set(shipPosition)

        // Inner: clockwise (+), Outer: counter-clockwise (-)
        innerBaseAngle += innerOrbitSpeed * deltaTime
        outerBaseAngle -= outerOrbitSpeed * deltaTime

        innerOrbiters.removeAll { !it.isActive }
        val innerTotal = innerOrbiters.size
        for ((i, orb) in innerOrbiters.withIndex()) {
            val target = innerBaseAngle + (2f * PI.toFloat() * i / innerTotal)
            orb.orbitAngle = lerpAngle(orb.orbitAngle, target, 0.15f)
        }

        outerOrbiters.removeAll { !it.isActive }
        val outerTotal = outerOrbiters.size
        for ((i, orb) in outerOrbiters.withIndex()) {
            val target = outerBaseAngle + (2f * PI.toFloat() * i / outerTotal)
            orb.orbitAngle = lerpAngle(orb.orbitAngle, target, 0.15f)
        }
    }

    fun clearOrbiters() {
        innerOrbiters.forEach { it.isActive = false }
        innerOrbiters.clear()
        outerOrbiters.forEach { it.isActive = false }
        outerOrbiters.clear()
    }

    fun fadeOutOrbiters() {
        innerOrbiters.forEach { it.lifetime = it.age + 0.5f }
        innerOrbiters.clear()
        outerOrbiters.forEach { it.lifetime = it.age + 0.5f }
        outerOrbiters.clear()
        cooldownTimer = Float.MAX_VALUE
    }

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = to - from
        while (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
        while (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
        return from + diff * t
    }
}
