package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.Ship
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VampiricLeecherSystemTest {

    private lateinit var system: VampiricLeecherSystem
    private lateinit var state: GameState
    private lateinit var ship: Ship

    @Before
    fun setup() {
        system = VampiricLeecherSystem(onAsteroidDestroyed = {})
        state = GameState()
        state.reset()
        ship = Ship().apply {
            position.set(100f, 100f)
            health = 50f
            maxHealth = 100f
            radius = 25f
        }
    }

    private fun asteroidAt(x: Float, y: Float, r: Float = 30f, hp: Float = 50f): Asteroid {
        return Asteroid().apply {
            isActive = true
            position.set(x, y)
            radius = r
            health = hp
            maxHealth = hp
        }
    }

    // Edge dist = 50 - 30 = 20 ≤ 100 → in range
    private fun inRangeAsteroid() = asteroidAt(150f, 100f)

    // Edge dist = 135 - 30 = 105 > 100 → out of range (LEECH_RANGE = 100f)
    private fun outOfRangeAsteroid() = asteroidAt(235f, 100f)

    // ── Damage / heal behavior (unchanged) ──────────────────────────────────

    @Test
    fun `no effect when stacks are zero`() {
        val asteroid = inRangeAsteroid()
        system.update(ship, listOf(asteroid), state, 0.25f)
        assertEquals(50f, ship.health, 0.001f)
        assertEquals(50f, asteroid.health, 0.001f)
    }

    @Test
    fun `no effect when asteroid is out of range`() {
        state.addPassive("vampiric_core")
        val asteroid = outOfRangeAsteroid()
        system.update(ship, listOf(asteroid), state, 0.25f)
        assertEquals(50f, ship.health, 0.001f)
        assertEquals(50f, asteroid.health, 0.001f)
    }

    @Test
    fun `tick heals ship and damages asteroid when in range`() {
        state.addPassive("vampiric_core")  // stacks = 1, leech = 0.1
        val asteroid = inRangeAsteroid()
        system.update(ship, listOf(asteroid), state, 0.21f)  // past 0.2s interval
        assertEquals(50.1f, ship.health, 0.001f)
        assertEquals(49.9f, asteroid.health, 0.001f)
    }

    @Test
    fun `two stacks doubles leech per tick`() {
        state.addPassive("vampiric_core")
        state.addPassive("vampiric_core")  // stacks = 2, leech = 0.2
        val asteroid = inRangeAsteroid()
        system.update(ship, listOf(asteroid), state, 0.21f)
        assertEquals(50.2f, ship.health, 0.001f)
        assertEquals(49.8f, asteroid.health, 0.001f)
    }

    @Test
    fun `multiple asteroids in range all contribute heal`() {
        state.addPassive("vampiric_core")  // leech = 0.1 per asteroid
        val a1 = inRangeAsteroid()
        val a2 = asteroidAt(100f, 150f, r = 30f)  // edge dist = 50-30 = 20 ≤ 100
        system.update(ship, listOf(a1, a2), state, 0.21f)
        assertEquals(50.2f, ship.health, 0.001f)
        assertEquals(49.9f, a1.health, 0.001f)
        assertEquals(49.9f, a2.health, 0.001f)
    }

    @Test
    fun `no leech when ship is at max health`() {
        state.addPassive("vampiric_core")
        ship.health = ship.maxHealth
        val asteroid = inRangeAsteroid()
        system.update(ship, listOf(asteroid), state, 0.21f)
        assertEquals(100f, ship.health, 0.001f)
        assertEquals(50f, asteroid.health, 0.001f)
    }

    @Test
    fun `asteroid killed by leech triggers callback`() {
        var callbackAsteroid: Asteroid? = null
        val sys = VampiricLeecherSystem(onAsteroidDestroyed = { callbackAsteroid = it })
        state.addPassive("vampiric_core")
        val asteroid = inRangeAsteroid().apply { health = 0.05f }
        sys.update(ship, listOf(asteroid), state, 0.21f)
        assertNotNull(callbackAsteroid)
        assertFalse(asteroid.isActive)
    }

    @Test
    fun `no leech on fragments with immunity timer`() {
        state.addPassive("vampiric_core")
        val asteroid = inRangeAsteroid().apply { fragmentImmunityTimer = 0.5f }
        system.update(ship, listOf(asteroid), state, 0.21f)
        assertEquals(50f, asteroid.health, 0.001f)
        assertEquals(50f, ship.health, 0.001f)
    }

    // ── Particle visuals ────────────────────────────────────────────────────

    @Test
    fun `no particles when stacks are zero`() {
        system.update(ship, listOf(inRangeAsteroid()), state, 0.01f)
        assertEquals(0, system.particles.size)
    }

    @Test
    fun `particles seed immediately when asteroid enters range`() {
        state.addPassive("vampiric_core")
        system.update(ship, listOf(inRangeAsteroid()), state, 0.01f)
        // 4 staggered particles seeded on first frame
        assertEquals(4, system.particles.size)
    }

    @Test
    fun `particles seed for each in-range asteroid on first frame`() {
        state.addPassive("vampiric_core")
        val a1 = inRangeAsteroid()
        val a2 = asteroidAt(100f, 150f, r = 30f)
        system.update(ship, listOf(a1, a2), state, 0.01f)
        assertEquals(8, system.particles.size)  // 4 per asteroid
    }

    @Test
    fun `particles advance t each frame`() {
        state.addPassive("vampiric_core")
        system.update(ship, listOf(inRangeAsteroid()), state, 0.01f)  // seed
        val tBefore = system.particles.minOf { it.t }
        system.update(ship, listOf(inRangeAsteroid()), state, 0.1f)
        val tAfter = system.particles.minOf { it.t }
        assertTrue(tAfter > tBefore)
    }

    @Test
    fun `particles expire when t reaches 1`() {
        state.addPassive("vampiric_core")
        system.update(ship, listOf(inRangeAsteroid()), state, 0.01f)  // seed
        assertEquals(4, system.particles.size)
        // Advance far past travel time (1.0 / PARTICLE_SPEED 1.2 = ~0.83s)
        system.update(ship, listOf(), state, 1.0f)
        assertEquals(0, system.particles.size)
    }

    @Test
    fun `reset clears particles and timers`() {
        state.addPassive("vampiric_core")
        system.update(ship, listOf(inRangeAsteroid()), state, 0.21f)
        system.reset()
        assertEquals(0, system.particles.size)
    }

    @Test
    fun `seeded particles have staggered t values`() {
        state.addPassive("vampiric_core")
        system.update(ship, listOf(inRangeAsteroid()), state, 0.01f)
        val tValues = system.particles.map { it.t }.sorted()
        // 4 particles seeded at 0, 0.25, 0.5, 0.75 — then all advanced by PARTICLE_SPEED * 0.01f
        val advance = VampiricLeecherSystem.PARTICLE_SPEED * 0.01f
        assertEquals(4, tValues.size)
        assertEquals(0f + advance, tValues[0], 0.001f)
        assertEquals(0.25f + advance, tValues[1], 0.001f)
        assertEquals(0.5f + advance, tValues[2], 0.001f)
        assertEquals(0.75f + advance, tValues[3], 0.001f)
    }
}
