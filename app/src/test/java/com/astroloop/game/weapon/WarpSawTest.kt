package com.astroloop.game.weapon

import com.astroloop.game.entity.EnemyShip
import com.astroloop.game.weapon.weapons.EnergySaw
import com.astroloop.game.weapon.weapons.WarpSaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarpSawTest {

    /** Create an active, non-warping EnemyShip at the given position. */
    private fun activeEnemy(x: Float, y: Float): EnemyShip = EnemyShip().apply {
        position.set(x, y)
        isActive = true
        // spawnTime must exceed warpInDuration (1f) so isWarping == false
        spawnTime = 2f
    }

    @Test
    fun `blade matches the L5 energy saw blade`() {
        val maxSaw = EnergySaw().also { it.level = 5 }
        val warpSaw = WarpSaw()
        assertEquals(maxSaw.discRadius, warpSaw.discRadius, 0.001f)
        assertEquals(maxSaw.reach, warpSaw.reach, 0.001f)
    }

    @Test
    fun `idle blade sits at the energy saw front position`() {
        val warpSaw = WarpSaw()
        val positions = warpSaw.getDiscPositions(0f, 0f, 0f)
        assertEquals(1, positions.size)
        assertEquals(125f, positions[0].first, 0.001f)
        assertEquals(0f, positions[0].second, 0.001f)
    }

    @Test
    fun `blade roams toward a target within leash range`() {
        val warpSaw = WarpSaw()
        val enemy = activeEnemy(300f, 0f)
        warpSaw.updateDiscs(0.1f, listOf(enemy), emptyList(), 0f, 0f, 0f)
        warpSaw.updateDiscs(0.1f, listOf(enemy), emptyList(), 0f, 0f, 0f)
        val pos = warpSaw.getDiscPositions(0f, 0f, 0f)[0]
        assertTrue("blade should have moved toward the enemy", pos.first > 125f)
    }

    @Test
    fun `losing the target begins a warp transit with no blade`() {
        val warpSaw = WarpSaw()
        val enemy = activeEnemy(300f, 0f)
        warpSaw.updateDiscs(0.1f, listOf(enemy), emptyList(), 0f, 0f, 0f)   // acquire → ROAM
        enemy.isActive = false
        warpSaw.updateDiscs(0.1f, emptyList(), emptyList(), 0f, 0f, 0f)     // no target → begin warp
        assertTrue("should be warping", warpSaw.isWarping)
        assertTrue("no blade (no damage) during transit", warpSaw.getDiscPositions(0f, 0f, 0f).isEmpty())
    }

    @Test
    fun `warp resolves to the front after the warp duration`() {
        val warpSaw = WarpSaw()
        val enemy = activeEnemy(300f, 0f)
        warpSaw.updateDiscs(0.1f, listOf(enemy), emptyList(), 0f, 0f, 0f)
        enemy.isActive = false
        warpSaw.updateDiscs(0.1f, emptyList(), emptyList(), 0f, 0f, 0f)     // begin warp
        warpSaw.updateDiscs(0.35f, emptyList(), emptyList(), 0f, 0f, 0f)    // tick past WARP_DURATION (0.3s)
        assertFalse("warp should be finished", warpSaw.isWarping)
        val pos = warpSaw.getDiscPositions(0f, 0f, 0f)
        assertEquals(1, pos.size)
        assertEquals(125f, pos[0].first, 0.001f)
    }

    @Test
    fun `leash break begins a warp transit`() {
        val warpSaw = WarpSaw()
        val enemy = activeEnemy(500f, 0f)
        warpSaw.updateDiscs(0.1f, listOf(enemy), emptyList(), 0f, 0f, 0f)
        warpSaw.updateDiscs(0.1f, listOf(enemy), emptyList(), 0f, 0f, 0f)
        warpSaw.updateDiscs(0.1f, listOf(enemy), emptyList(), 2000f, 0f, 0f)   // ship far → leash break
        assertTrue("leash break should begin the warp", warpSaw.isWarping)
    }
}
