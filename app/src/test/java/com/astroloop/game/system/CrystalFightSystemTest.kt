package com.astroloop.game.system

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class CrystalFightSystemTest {

    // The stationary-camera density formula understates the real peak by this much, because the
    // despawn radius follows the ship. Measured by simulating the committed LAYERS over 90s with
    // the player fleeing in a straight line at 375 px/s (300 base x 1.25 maxed store upgrade).
    private val FLEEING_PLAYER_FACTOR = 1.34

    // Pool pre-allocation is 200; obtain() falls back to factory(), so overrunning grows the pool
    // ONCE to its high-water mark rather than allocating per frame. 250 is the accepted ceiling.
    private val POOL_CEILING = 250.0

    @Test fun fivePhasesAtEighteenSecondsEach() {
        assertEquals(CrystalPhase.P1, CrystalFightSystem.phaseFor(0f))
        assertEquals(CrystalPhase.P1, CrystalFightSystem.phaseFor(17.9f))
        assertEquals(CrystalPhase.P2, CrystalFightSystem.phaseFor(18f))
        assertEquals(CrystalPhase.P3, CrystalFightSystem.phaseFor(36f))
        assertEquals(CrystalPhase.P4, CrystalFightSystem.phaseFor(54f))
        assertEquals(CrystalPhase.P5, CrystalFightSystem.phaseFor(72f))
        assertEquals(CrystalPhase.P5, CrystalFightSystem.phaseFor(89.9f))
    }

    @Test fun phaseForClampsPastTheEnd() {
        assertEquals(CrystalPhase.P5, CrystalFightSystem.phaseFor(90f))
        assertEquals(CrystalPhase.P5, CrystalFightSystem.phaseFor(500f))
    }

    @Test fun fiveLayersOnePerPhase() {
        assertEquals(5, CrystalFightSystem.LAYERS.size)
        assertEquals(5, CrystalPhase.values().size)
    }

    @Test fun layersAreCumulative() {
        assertEquals(1, CrystalFightSystem.layersFor(CrystalPhase.P1).size)
        assertEquals(3, CrystalFightSystem.layersFor(CrystalPhase.P3).size)
        assertEquals(5, CrystalFightSystem.layersFor(CrystalPhase.P5).size)
        // P5 contains P1's layer unchanged — layers add, they never swap out
        assertEquals(CrystalFightSystem.LAYERS[0], CrystalFightSystem.layersFor(CrystalPhase.P5)[0])
    }

    @Test fun everyLayerOutrunsThePlayerByThirtyPercent() {
        // Max player speed: SHIP_BASE_SPEED 300 x 1.25 (maxed store upgrade) = 375 px/s.
        // If this fails the fight is winnable by flying in a straight line.
        val maxPlayerSpeed = 300f * 1.25f
        for (layer in CrystalFightSystem.LAYERS) {
            assertTrue("layer at ${layer.speed}px/s must beat the player by >=1.3x",
                layer.speed > maxPlayerSpeed * 1.3f)
        }
    }

    @Test fun layerSpeedRampsMonotonically() {
        val speeds = CrystalFightSystem.LAYERS.map { it.speed }
        assertEquals(speeds.sorted(), speeds)
    }

    @Test fun densityBudgetHoldsAtMaxEscalation() {
        // live = (arms / interval) * (DESPAWN 1500 / speed) is a STATIONARY-CAMERA figure.
        // The cull is measured from the camera, and Camera.update() centres it on the SHIP — so
        // bullets travelling with a fleeing player close on the boundary at (speed - playerSpeed)
        // and live far longer. Measured peak over the real 90s with the player fleeing at max
        // speed (375) is ~249, about 1.34x this figure. Budget against the REAL number, not the
        // flattering one: the next person to retune must not trust a stationary-camera estimate.
        val stationary = CrystalFightSystem.layersFor(CrystalPhase.P5).sumOf { l ->
            ((l.arms / l.interval) * (1500f / l.speed)).toDouble()
        }
        val fleeingPeak = stationary * FLEEING_PLAYER_FACTOR
        assertTrue("P5 peaks at $fleeingPeak live bullets when fleeing; ceiling is $POOL_CEILING",
            fleeingPeak <= POOL_CEILING)
    }

    // --- Fairness contract (Touhou rules, adapted to free flight) ---
    // A drip stream is a moving picket line: bullets `speed * interval` apart. Crystal
    // bullets hit the REGULAR ship hitbox, so the line is only passable if the picket
    // spacing clears the full danger diameter with room to react. With counter-rotating
    // arms, crossing a stream is sometimes FORCED (scissor pinches close the sector you're
    // standing in) — so every layer must be threadable or the fight is unfair.

    private val dangerDiameter =
        2f * (com.astroloop.game.core.GameConfig.SHIP_BASE_SIZE + CrystalFightSystem.BULLET_RADIUS)

    @Test fun everyStreamIsThreadable() {
        for (layer in CrystalFightSystem.LAYERS) {
            val spacing = layer.speed * layer.interval
            assertTrue(
                "stream spacing ${spacing}px must be >= 2.5x the ${dangerDiameter}px danger " +
                    "diameter or the stream is a wall (interval ${layer.interval})",
                spacing >= 2.5f * dangerDiameter
            )
        }
    }

    @Test fun crossingWindowBeatsCrossingExposure() {
        // At a fixed point on a stream a bullet passes every `interval` seconds, lethal for
        // dangerDiameter/speed of it. The ship needs dangerDiameter/maxPlayerSpeed to cross
        // the lane. The open window must beat that exposure with margin, or threading a
        // forced pinch is a coin flip instead of a read.
        val maxPlayerSpeed = 300f * 1.25f
        for (layer in CrystalFightSystem.LAYERS) {
            val window = layer.interval - dangerDiameter / layer.speed
            val exposure = dangerDiameter / maxPlayerSpeed
            assertTrue(
                "crossing window ${window}s must be >= 1.2x the ${exposure}s exposure " +
                    "(layer at ${layer.speed}px/s)",
                window >= 1.2f * exposure
            )
        }
    }

    @Test fun bulletDamageIsDecisive() {
        // Owner call (2026-07-16): dodging IS the fight — one clean hit ends a base-hull
        // run (shields soften at most one). Fairness lives in the threading invariants
        // above, not in a mistake budget; this guards accidental softening.
        assertTrue(
            "bullet damage ${CrystalFightSystem.BULLET_DAMAGE} must stay lethal to base hull",
            CrystalFightSystem.BULLET_DAMAGE >= 100f
        )
    }

    @Test fun layersUseTheColdSpectrumInOrder() {
        CrystalFightSystem.LAYERS.forEachIndexed { i, l ->
            assertEquals(com.astroloop.game.render.CrystalPalette.LAYER_COLORS[i], l.color)
        }
    }

    @Test fun mixesRotationDirections() {
        val dirs = CrystalFightSystem.LAYERS.map { it.rotSpeed > 0f }.toSet()
        assertEquals("counter-rotation is the point — needs both directions", 2, dirs.size)
    }

    @Test fun survivesAtNinety() {
        val s = CrystalFightSystem()
        assertFalse(s.survived)
        repeat(90) { s.update(0f, 0f, 1f) }
        assertTrue(s.survived)
        assertEquals(90f, s.elapsedTime, 0.001f)
    }

    @Test fun phaseChangedFiresOncePerTransition() {
        val s = CrystalFightSystem()
        s.update(0f, 0f, 0.5f)
        assertEquals(CrystalPhase.P1, s.phaseChanged)
        s.update(0f, 0f, 0.5f)
        assertNull(s.phaseChanged)
        repeat(33) { s.update(0f, 0f, 0.5f) }   // elapsed 17.5 — still P1
        assertNull(s.phaseChanged)
        s.update(0f, 0f, 0.5f)                  // elapsed 18.0 — crosses to P2
        assertEquals(CrystalPhase.P2, s.phaseChanged)
    }

    @Test fun p1EmitsOnlyTheCyanLayer() {
        val s = CrystalFightSystem()
        val burst = s.update(0f, 0f, 0.01f)
        assertTrue(burst.isNotEmpty())
        assertTrue(burst.all { it.color == CrystalFightSystem.LAYERS[0].color })
    }

    @Test fun p5EmitsAllFiveColorsOverTime() {
        val s = CrystalFightSystem()
        repeat(4320) { s.update(0f, 0f, 1f / 60f) }   // 72s → P5
        val seen = mutableSetOf<Int>()
        repeat(60) { seen += s.update(0f, 0f, 1f / 60f).map { b -> b.color } }
        assertEquals(5, seen.size)
    }

    @Test fun anglesAdvanceSoArmsSweep() {
        // Two emissions from the same layer must NOT share a direction — that was the old
        // bug (random spray). With rotation, consecutive bullets differ by rotSpeed * dt.
        val s = CrystalFightSystem()
        val step = CrystalFightSystem.LAYERS[0].interval
        val first = s.update(0f, 0f, step).first()
        val second = s.update(0f, 0f, step).first()
        val a1 = kotlin.math.atan2(first.vy, first.vx)
        val a2 = kotlin.math.atan2(second.vy, second.vx)
        assertNotEquals(a1, a2)
        assertEquals(hypot(first.vx, first.vy), hypot(second.vx, second.vy), 0.01f)
    }

    @Test fun updateIsDeterministic() {
        // No Random anywhere: two systems fed identical dt produce identical bullets.
        val a = CrystalFightSystem()
        val b = CrystalFightSystem()
        repeat(50) {
            assertEquals(a.update(0f, 0f, 1f / 60f), b.update(0f, 0f, 1f / 60f))
        }
    }

    @Test fun resetClearsElapsedAndAngles() {
        val s = CrystalFightSystem()
        repeat(40) { s.update(0f, 0f, 1f) }
        s.reset()
        assertEquals(0f, s.elapsedTime, 0.001f)
        assertFalse(s.survived)
        val fresh = CrystalFightSystem()
        val step = CrystalFightSystem.LAYERS[0].interval
        assertEquals(fresh.update(0f, 0f, step), s.update(0f, 0f, step))
    }
}
