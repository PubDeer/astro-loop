package com.astroloop.game.system

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.hypot

class CrystalEmitterTest {

    private val layer = SpiralLayer(color = 0xFF88EEFF.toInt(), speed = 500f,
                                    rotSpeed = 0.6f, arms = 1, interval = 0.06f)

    @Test fun emitsOneBulletPerArm() {
        assertEquals(1, CrystalEmitter.emit(layer, 0f, 0f, 0f).size)
        assertEquals(3, CrystalEmitter.emit(layer.copy(arms = 3), 0f, 0f, 0f).size)
    }

    @Test fun bulletsStartAtTheCrystal() {
        val b = CrystalEmitter.emit(layer, 200f, 100f, 0f)
        assertTrue(b.all { it.x == 200f && it.y == 100f })
    }

    @Test fun bulletSpeedIsTheLayerSpeed() {
        val b = CrystalEmitter.emit(layer.copy(arms = 4), 0f, 0f, 1.2f)
        b.forEach { assertEquals(500f, hypot(it.vx, it.vy), 0.01f) }
    }

    @Test fun everyBulletCarriesTheLayerColor() {
        val b = CrystalEmitter.emit(layer.copy(arms = 3, color = 0xFFD07FFF.toInt()), 0f, 0f, 0f)
        assertTrue(b.all { it.color == 0xFFD07FFF.toInt() })
    }

    @Test fun armsAreEvenlySpacedAroundTheCircle() {
        val b = CrystalEmitter.emit(layer.copy(arms = 4), 0f, 0f, 0f)
        val angles = b.map { atan2(it.vy, it.vx).toDouble() }.sorted()
        // 4 arms → 90° apart
        for (i in 1 until angles.size) {
            assertEquals(Math.PI / 2.0, angles[i] - angles[i - 1], 0.01)
        }
    }

    @Test fun theEmitAngleAimsTheFirstArm() {
        val b = CrystalEmitter.emit(layer, 0f, 0f, Math.PI.toFloat() / 2f)
        // angle = PI/2 → straight down (+y)
        assertEquals(0f, b[0].vx, 0.01f)
        assertEquals(500f, b[0].vy, 0.01f)
    }

    @Test fun emitIsPure_sameInputsSameOutput() {
        // No Random, no hidden state: the emitter must be a function of its arguments.
        val a = CrystalEmitter.emit(layer.copy(arms = 3), 5f, 7f, 0.9f)
        val b = CrystalEmitter.emit(layer.copy(arms = 3), 5f, 7f, 0.9f)
        assertEquals(a, b)
    }
}
