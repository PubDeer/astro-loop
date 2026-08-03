package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.EnemyShip
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Ship
import com.astroloop.game.entity.VisualEffectManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectileAoeDamageNumberTest {

    /** Identity modifier — damage passes through untouched, never crits. */
    private val noModifiers: (Float) -> Pair<Float, Boolean> = { Pair(it, false) }

    private fun system(
        visualEffects: VisualEffectManager,
        modifiers: (Float) -> Pair<Float, Boolean> = noModifiers,
        ship: Ship = Ship(),
        state: GameState = GameState()
    ) = ProjectileEffectsSystem(
        ship = ship,
        state = state,
        collisionSystem = CollisionSystem(),
        visualEffects = visualEffects,
        applyDamageModifiers = modifiers,
        onAsteroidDestroyed = {},
        onEnemyDestroyed = {},
        onPlayerDeath = {}
    )

    private fun airburst() = Projectile().apply {
        position.set(100f, 100f)
        isActive = false
        expiredNaturally = true          // airburst via proximity fuse
        type = ProjectileType.FLAK
        explodeOnDeath = true
        explosionRadius = 60f
        explosionDamage = 30f
        isEnemyProjectile = false
        weaponId = "flak_cannon"
    }

    private fun asteroidNearby() = Asteroid().apply {
        position.set(110f, 100f)
        isActive = true
    }

    @Test
    fun `player flak airburst shows a damage number on asteroids`() {
        val visualEffects = VisualEffectManager()

        system(visualEffects).processExpired(listOf(airburst()), listOf(asteroidNearby()), emptyList())

        assertTrue(
            "asteroid AoE damage must spawn a damage number",
            visualEffects.getDamageNumbers().isNotEmpty()
        )
    }

    /**
     * Regression: explosion damage used to roll crit inline and never consult Momentum Drive,
     * so flak — whose proximity fuse means it lands no direct hits at all — got no momentum
     * bonus on any of its output. Explosion damage must run through the SAME modifier hook as
     * the direct-hit, beam and saw paths.
     */
    @Test
    fun `explosion damage on asteroids routes through the shared damage modifiers`() {
        val visualEffects = VisualEffectManager()
        val tripled: (Float) -> Pair<Float, Boolean> = { Pair(it * 3f, false) }

        system(visualEffects, tripled)
            .processExpired(listOf(airburst()), listOf(asteroidNearby()), emptyList())

        val numbers = visualEffects.getDamageNumbers()
        assertTrue("expected a damage number", numbers.isNotEmpty())
        assertEquals(
            "explosion damage must be modified, not raw explosionDamage",
            90, numbers.first().value
        )
    }

    @Test
    fun `explosion damage on enemies routes through the shared damage modifiers`() {
        val visualEffects = VisualEffectManager()
        val tripled: (Float) -> Pair<Float, Boolean> = { Pair(it * 3f, false) }
        // Enemies off-screen are skipped, so give the camera real dimensions around the blast.
        val ship = Ship().apply { position.set(100f, 100f) }
        val state = GameState().apply { screenWidth = 800f; screenHeight = 600f }
        val enemy = EnemyShip().apply {
            position.set(110f, 100f)
            isActive = true
            health = 1000f
            maxHealth = 1000f
            spawnShieldTimer = 0f   // defaults to 5f — a fresh enemy blocks all damage
        }

        system(visualEffects, tripled, ship, state)
            .processExpired(listOf(airburst()), emptyList(), listOf(enemy))

        assertEquals(
            "enemy took raw explosionDamage instead of the modified value",
            1000f - 90f, enemy.health, 0.01f
        )
    }
}
