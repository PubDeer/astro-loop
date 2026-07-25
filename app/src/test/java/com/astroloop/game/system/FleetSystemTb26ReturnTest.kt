package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Boss
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.Ship
import com.astroloop.game.entity.VisualEffectManager
import com.astroloop.game.util.Vector2
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * TB-26's solo return must support returning to Past Astro (corruption mirror),
 * not just the player (normal run), and arrive() must spawn him on the side of
 * whoever he's protecting — the two runs are exact mirrors of each other.
 */
class FleetSystemTb26ReturnTest {

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    private fun makeFleet(): Triple<FleetSystem, Ship, Boss> {
        val ship = Ship()
        ship.position.set(0f, 0f)
        val boss = Boss()
        boss.position.set(0f, 0f)
        val fleet = FleetSystem(boss, ship, EntityPool({ Projectile() }, 8), VisualEffectManager())
        return Triple(fleet, ship, boss)
    }

    private fun stepUntilArrived(fleet: FleetSystem, state: GameState, targetX: Float, targetY: Float): Boolean {
        var t = 0f
        while (t < 10f) {
            fleet.update(state, 1f / 60f)
            if (dist(fleet.tb26Position.x, fleet.tb26Position.y, targetX, targetY) < 60f) return true
            t += 1f / 60f
        }
        return false
    }

    @Test
    fun `solo return flies to the ship by default`() {
        val (fleet, ship, _) = makeFleet()
        fleet.startTb26Return(1080f, 2400f)
        assertTrue(
            "TB-26 must reach the player's side",
            stepUntilArrived(fleet, GameState(), ship.position.x, ship.position.y)
        )
    }

    @Test
    fun `solo return flies to past astro when given a target`() {
        val (fleet, _, _) = makeFleet()
        val pastAstro = Vector2(600f, 400f)  // frozen well away from the player
        fleet.startTb26Return(1080f, 2400f, pastAstro)
        assertTrue(
            "TB-26 must reach Past Astro, not the player",
            stepUntilArrived(fleet, GameState(), pastAstro.x, pastAstro.y)
        )
        assertTrue(
            "TB-26 must be orbiting Past Astro, far from the player at (0,0)",
            dist(fleet.tb26Position.x, fleet.tb26Position.y, 0f, 0f) > 300f
        )
    }

    @Test
    fun `arrive spawns tb26 behind whoever he is protecting`() {
        val (fleet, _, _) = makeFleet()
        // Corruption-mirror geometry: player IS the boss (both at origin), Past Astro
        // frozen 300px away. Without a target the old code computed atan2(0,0) and
        // spawned TB-26 due east of the player regardless of Past Astro's side.
        val pastAstro = Vector2(0f, -300f)
        fleet.startTb26Return(1080f, 2400f, pastAstro)
        fleet.arrive(GameState(), "ship_white", 1080f, 2400f)

        val spawnDist = FleetSystem.tb26SpawnDistance(1080f, 2400f)
        // Behind Past Astro, away from the boss: along (0,-1) from Past Astro's position.
        val expectedX = pastAstro.x
        val expectedY = pastAstro.y - spawnDist
        assertTrue(
            "TB-26 must warp in from behind Past Astro (away from the boss), " +
                "got (${fleet.tb26Position.x}, ${fleet.tb26Position.y})",
            dist(fleet.tb26Position.x, fleet.tb26Position.y, expectedX, expectedY) < 1f
        )
    }
}
