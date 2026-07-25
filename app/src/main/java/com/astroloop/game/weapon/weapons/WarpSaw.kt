package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.Firer
import com.astroloop.game.entity.EnemyShip
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.weapon.Weapon
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class WarpSaw : Weapon(
    id = "warp_saw",
    name = "Warp Saw",
    description = "The blade detaches and hunts on its own"
) {
    override val baseDamage = 15f
    override val baseCooldown = 0.1f
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 1

    // Identical to the L5 Energy Saw blade — the evolution reads as the blade detaching
    val discRadius: Float = 55f
    val reach: Float = 125f
    val leashRange: Float = 600f
    val roamSpeed: Float = 450f

    enum class Phase { IDLE, ROAM, WARP }

    var phase: Phase = Phase.IDLE
        private set
    var target: Entity? = null
        private set
    var currentX: Float = 0f
        private set
    var currentY: Float = 0f
        private set

    companion object { const val WARP_DURATION = 0.3f }

    // Chrono warp transit: the blade dashes out of (warpFromX, warpFromY) and warps in at
    // the ship front over WARP_DURATION. No blade / no damage while in transit.
    var warpFromX: Float = 0f
        private set
    var warpFromY: Float = 0f
        private set
    private var warpTimer: Float = 0f
    val isWarping: Boolean get() = phase == Phase.WARP
    /** 0 at warp start → 1 at warp end. */
    val warpProgress: Float
        get() = if (WARP_DURATION > 0f) (1f - warpTimer / WARP_DURATION).coerceIn(0f, 1f) else 1f

    fun getDiscCount(): Int = 1

    fun getTickRate(): Float = baseCooldown

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    override fun getCooldown(state: GameState): Float = getTickRate()

    override fun getProjectileCount(state: GameState): Int = 1

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        // No-op: Warp Saw doesn't fire projectiles
    }

    fun getFrontPosition(shipX: Float, shipY: Float, shipRotation: Float): Pair<Float, Float> =
        Pair(shipX + cos(shipRotation) * reach, shipY + sin(shipRotation) * reach)

    fun getDiscPositions(shipX: Float, shipY: Float, shipRotation: Float): List<Pair<Float, Float>> =
        when (phase) {
            Phase.IDLE -> listOf(getFrontPosition(shipX, shipY, shipRotation))
            Phase.ROAM -> listOf(Pair(currentX, currentY))
            Phase.WARP -> emptyList()   // in transit: no blade, no damage
        }

    fun updateDiscs(
        deltaTime: Float,
        enemies: List<EnemyShip>,
        asteroids: List<Asteroid>,
        shipX: Float,
        shipY: Float,
        shipRotation: Float
    ) {
        when (phase) {
            Phase.IDLE -> {
                val front = getFrontPosition(shipX, shipY, shipRotation)
                currentX = front.first
                currentY = front.second
                val found = findTarget(shipX, shipY, enemies, asteroids)
                if (found != null) {
                    phase = Phase.ROAM
                    target = found
                }
            }
            Phase.ROAM -> {
                // Loose leash: too far from the ship -> warp home
                val dxs = currentX - shipX
                val dys = currentY - shipY
                if (dxs * dxs + dys * dys > leashRange * leashRange) {
                    beginWarp()
                    return
                }

                var t = target
                if (t == null || !t.isActive) {
                    t = findTarget(shipX, shipY, enemies, asteroids)
                    if (t == null) {
                        beginWarp()
                        return
                    }
                    target = t
                }

                // Fly through the target — no latch, contact damage handles the grinding
                val dx = t.position.x - currentX
                val dy = t.position.y - currentY
                val dist = sqrt(dx * dx + dy * dy)
                val step = roamSpeed * deltaTime
                if (step >= dist) {
                    currentX = t.position.x
                    currentY = t.position.y
                } else {
                    currentX += (dx / dist) * step
                    currentY += (dy / dist) * step
                }
            }
            Phase.WARP -> {
                warpTimer -= deltaTime
                if (warpTimer <= 0f) {
                    val front = getFrontPosition(shipX, shipY, shipRotation)
                    currentX = front.first
                    currentY = front.second
                    phase = Phase.IDLE
                    target = null
                }
            }
        }
    }

    private fun beginWarp() {
        warpFromX = currentX
        warpFromY = currentY
        warpTimer = WARP_DURATION
        phase = Phase.WARP
        target = null
    }

    private fun findTarget(
        shipX: Float,
        shipY: Float,
        enemies: List<EnemyShip>,
        asteroids: List<Asteroid>
    ): Entity? {
        // Targets must be within leash range of the SHIP; enemies before asteroids
        var bestDist = leashRange
        var best: Entity? = null
        for (enemy in enemies) {
            if (!enemy.isActive || enemy.isWarping) continue
            val dx = enemy.position.x - shipX
            val dy = enemy.position.y - shipY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < bestDist) { bestDist = dist; best = enemy }
        }
        if (best != null) return best
        for (asteroid in asteroids) {
            if (!asteroid.isActive) continue
            val dx = asteroid.position.x - shipX
            val dy = asteroid.position.y - shipY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < bestDist) { bestDist = dist; best = asteroid }
        }
        return best
    }
}
