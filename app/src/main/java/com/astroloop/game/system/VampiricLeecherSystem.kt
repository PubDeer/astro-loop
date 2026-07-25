package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.LeechParticle
import com.astroloop.game.entity.Ship
import kotlin.math.sqrt

class VampiricLeecherSystem(
    private val onAsteroidDestroyed: (Asteroid) -> Unit
) {
    val particles = mutableListOf<LeechParticle>()
    private var tickTimer = 0f
    private var spawnTimer = 0f
    private val inRangeAsteroids = HashSet<Asteroid>()

    companion object {
        const val TICK_INTERVAL = 0.2f
        const val LEECH_RANGE = GameConfig.SHIP_BASE_SIZE * 4f  // 100f from asteroid edge
        const val LEECH_PER_STACK = 0.1f
        // Rock asteroids have ±30% shape variance — push start past max spike (1.3×)
        private const val EDGE_SPAWN_MULTIPLIER = 1.35f
        const val PARTICLE_SPEED = 1.2f           // t-units/sec; full gap in ~0.83s
        const val PARTICLE_SPAWN_INTERVAL = 0.2f  // 1 new particle per asteroid per 0.2s → ~4 in flight
    }

    fun update(ship: Ship, asteroids: List<Asteroid>, state: GameState, deltaTime: Float) {
        val stacks = state.passiveStacks["vampiric_core"] ?: 0
        updateParticles(ship, asteroids, stacks, deltaTime)

        tickTimer -= deltaTime
        if (tickTimer <= 0f) {
            tickTimer += TICK_INTERVAL
            if (stacks > 0) tick(ship, asteroids, stacks)
        }
    }

    private fun updateParticles(ship: Ship, asteroids: List<Asteroid>, stacks: Int, deltaTime: Float) {
        if (stacks > 0) {
            spawnTimer -= deltaTime
            val newInRange = HashSet<Asteroid>()

            for (asteroid in asteroids) {
                if (!asteroid.isActive || asteroid.fragmentImmunityTimer > 0f) continue
                val dx = ship.position.x - asteroid.position.x
                val dy = ship.position.y - asteroid.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist - asteroid.radius > LEECH_RANGE) continue

                newInRange.add(asteroid)
                val norm = if (dist > 0f) 1f / dist else 0f
                val ex = asteroid.position.x + dx * norm * asteroid.radius * EDGE_SPAWN_MULTIPLIER
                val ey = asteroid.position.y + dy * norm * asteroid.radius * EDGE_SPAWN_MULTIPLIER

                if (!inRangeAsteroids.contains(asteroid)) {
                    // First frame in range: seed 4 staggered particles so stream looks full immediately
                    for (i in 0..3) particles.add(LeechParticle(ex, ey, i * 0.25f))
                } else if (spawnTimer <= 0f) {
                    // Ongoing: spawn 1 particle per interval to keep stream flowing
                    particles.add(LeechParticle(ex, ey, 0f))
                }
            }

            if (spawnTimer <= 0f) spawnTimer += PARTICLE_SPAWN_INTERVAL
            inRangeAsteroids.clear()
            inRangeAsteroids.addAll(newInRange)
        } else {
            inRangeAsteroids.clear()
        }

        // Advance and expire all particles every frame (after seeding so new particles get their first step)
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.t += PARTICLE_SPEED * deltaTime
            if (p.t >= 1f) iter.remove()
        }
    }

    private fun tick(ship: Ship, asteroids: List<Asteroid>, stacks: Int) {
        if (ship.health >= ship.maxHealth) return

        val leech = LEECH_PER_STACK * stacks
        var totalHeal = 0f

        for (asteroid in asteroids) {
            if (!asteroid.isActive || asteroid.fragmentImmunityTimer > 0f) continue
            val dx = ship.position.x - asteroid.position.x
            val dy = ship.position.y - asteroid.position.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist - asteroid.radius > LEECH_RANGE) continue

            asteroid.health -= leech
            totalHeal += leech

            if (asteroid.health <= 0f) {
                onAsteroidDestroyed(asteroid)
                asteroid.isActive = false
            }
        }

        if (totalHeal > 0f) {
            ship.health = (ship.health + totalHeal).coerceAtMost(ship.maxHealth)
        }
    }

    fun reset() {
        particles.clear()
        inRangeAsteroids.clear()
        tickTimer = 0f
        spawnTimer = 0f
    }
}
