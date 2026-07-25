package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Ship
import com.astroloop.game.entity.VisualEffectManager
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectileAoeDamageNumberTest {

    @Test
    fun `player flak airburst shows a damage number on asteroids`() {
        val visualEffects = VisualEffectManager()
        val system = ProjectileEffectsSystem(
            ship = Ship(),
            state = GameState(),
            collisionSystem = CollisionSystem(),
            visualEffects = visualEffects,
            onAsteroidDestroyed = {},
            onEnemyDestroyed = {},
            onPlayerDeath = {}
        )
        val asteroid = Asteroid().apply {
            position.set(110f, 100f)
            isActive = true
        }
        val shell = Projectile().apply {
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

        system.processExpired(listOf(shell), listOf(asteroid), emptyList())

        assertTrue(
            "asteroid AoE damage must spawn a damage number",
            visualEffects.getDamageNumbers().isNotEmpty()
        )
    }
}
