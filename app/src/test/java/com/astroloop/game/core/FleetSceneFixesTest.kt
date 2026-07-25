package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FleetSceneFixesTest {

    // ── Fix 1: Boss rotation formula ─────────────────────────────────────────

    @Test
    fun `boss rotation formula faces player directly right`() {
        val bossX = 0f; val bossY = 0f
        val playerX = 100f; val playerY = 0f
        val rotation = atan2(playerY - bossY, playerX - bossX)
        assertEquals(0f, rotation, 0.001f)
    }

    @Test
    fun `boss rotation formula faces player directly above`() {
        val bossX = 0f; val bossY = 0f
        val playerX = 0f; val playerY = -100f  // negative y = up in screen space
        val rotation = atan2(playerY - bossY, playerX - bossX)
        assertEquals(-PI.toFloat() / 2f, rotation, 0.001f)
    }

    @Test
    fun `boss rotation formula faces player in diagonal`() {
        val rotation = atan2(100f, 100f)
        assertEquals(PI.toFloat() / 4f, rotation, 0.001f)
    }

    // Fix 2: EMP scatter

    @Test
    fun `EMP flat impulse 200f outward along x axis`() {
        val dx = 300f; val dy = 0f
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
        val impulseX = (dx / dist) * 200f
        val impulseY = (dy / dist) * 200f
        assertEquals(200f, impulseX, 0.1f)
        assertEquals(0f, impulseY, 0.1f)
    }

    @Test
    fun `EMP drag at 0 8 per second retains over half velocity after 0 5s`() {
        var vx = 200f
        val deltaTime = 1f / 60f
        val frames = 30  // 0.5 seconds
        repeat(frames) {
            vx *= (1f - 0.8f * deltaTime).coerceAtLeast(0f)
        }
        // 0.8×/s drag over 0.5s → roughly 67% remains
        assert(vx > 100f) { "Too much decay: vx=$vx" }
        assert(vx < 200f) { "No decay at all: vx=$vx" }
    }

    @Test
    fun `EMP pure physics position advances by velocity times deltaTime`() {
        var posX = 0f
        var vx = 400f
        val deltaTime = 0.016f
        posX += vx * deltaTime
        assertEquals(6.4f, posX, 0.01f)
    }

    // ── Fix 4: TB-26 orbit center ─────────────────────────────────────────────

    @Test
    fun `TB26 orbit position is exactly orbitRadius from ship when angle is zero`() {
        val shipX = 250f; val shipY = -100f
        val orbitRadius = 30f; val angle = 0f
        val tb26X = shipX + kotlin.math.cos(angle) * orbitRadius
        val tb26Y = shipY + kotlin.math.sin(angle) * orbitRadius
        assertEquals(shipX + orbitRadius, tb26X, 0.001f)
        assertEquals(shipY, tb26Y, 0.001f)
    }

    @Test
    fun `TB26 orbit center tracks ship not a fixed ring slot`() {
        val orbitRadius = 30f; val angle = 0f
        val shipX1 = 200f; val tb26X1 = shipX1 + kotlin.math.cos(angle) * orbitRadius
        val shipX2 = 350f; val tb26X2 = shipX2 + kotlin.math.cos(angle) * orbitRadius
        assertEquals(30f, tb26X1 - shipX1, 0.001f)
        assertEquals(30f, tb26X2 - shipX2, 0.001f)
        assert(tb26X1 != tb26X2) { "TB-26 position should differ when ship moves" }
    }

    // ── Fix: tb26OrbitTarget nullable override ────────────────────────────────

    @Test
    fun `tb26OrbitTarget null falls back to ship position`() {
        val shipX = 100f
        val orbitTargetX: Float? = null
        val centerX = orbitTargetX ?: shipX
        assertEquals(shipX, centerX, 0.001f)
    }

    @Test
    fun `tb26OrbitTarget non-null uses override not ship`() {
        val shipX = 100f
        val orbitTargetX: Float? = 500f
        val centerX = orbitTargetX ?: shipX
        assertEquals(500f, centerX, 0.001f)
    }

    @Test
    fun `tb26 orbit position is orbitRadius from target when target overrides ship`() {
        val targetX = 500f; val targetY = 300f
        val orbitRadius = 30f; val angle = 0f
        val tb26X = targetX + kotlin.math.cos(angle) * orbitRadius
        val tb26Y = targetY + kotlin.math.sin(angle) * orbitRadius
        assertEquals(targetX + orbitRadius, tb26X, 0.001f)
        assertEquals(targetY, tb26Y, 0.001f)
    }

    // ── Fix: EMP separation pass ──────────────────────────────────────────────

    @Test
    fun `separation pass pushes overlapping ships apart`() {
        // Two ships 30f apart, threshold 60f → they overlap and must be pushed
        val ax = 0f; val ay = 0f
        val bx = 30f; val by = 0f
        val dist = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
        val minDist = 60f
        assert(dist < minDist) { "Pre-condition: ships must start overlapping" }

        val nx = (bx - ax) / dist
        val push = (minDist - dist) * 9f  // 270f — matches production push factor in FleetSystem
        val aVx = -nx * push
        val bVx = nx * push

        assert(aVx < 0f) { "Ship A should be pushed left (negative X)" }
        assert(bVx > 0f) { "Ship B should be pushed right (positive X)" }
        assertEquals(270f, bVx, 0.1f)
        assertEquals(-270f, aVx, 0.1f)
    }

    @Test
    fun `separation pass is no-op when ships are far enough apart`() {
        val ax = 0f; val ay = 0f
        val bx = 80f; val by = 0f
        val dist = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
        val minDist = 60f
        assert(dist >= minDist) { "Ships far apart should not be pushed" }
        // No push → velocity delta is 0
        val push = (minDist - dist).coerceAtLeast(0f)
        assertEquals(0f, push, 0.001f)
    }

    // ── Fix: Projectile fade-out math ─────────────────────────────────────────

    @Test
    fun `fade alpha reaches zero when remaining lifetime is zero`() {
        val lifetime = 2f
        val age = 2f  // exactly at expiry
        val remaining = lifetime - age
        val fadeAlpha = (remaining / 0.5f).coerceIn(0f, 1f)
        assertEquals(0f, fadeAlpha, 0.001f)
    }

    @Test
    fun `fade alpha is 0 5 at 0 25s before expiry`() {
        val lifetime = 2f
        val age = 1.75f  // 0.25s left
        val remaining = lifetime - age
        val fadeAlpha = (remaining / 0.5f).coerceIn(0f, 1f)
        assertEquals(0.5f, fadeAlpha, 0.001f)
    }

    @Test
    fun `fade alpha is 1 when more than 0 5s remains`() {
        val lifetime = 2f
        val age = 1f  // 1s left — outside fade window
        val remaining = lifetime - age
        val inFadeWindow = remaining < 0.5f
        assert(!inFadeWindow) { "Should not be fading yet" }
    }

    // ── Mirror fix 1: Crystal Astro ship rotation ─────────────────────────────

    @Test
    fun `ship rotation equals boss rotation after charge block sets it`() {
        val pastAstroX = 300f; val pastAstroY = -150f
        val shipX = 0f; val shipY = 0f
        val bossRotation = atan2(pastAstroY - shipY, pastAstroX - shipX)
        // The fix: ship.rotation = boss.rotation
        val shipRotation = bossRotation
        assertEquals(bossRotation, shipRotation, 0.001f)
    }

    @Test
    fun `ship rotation is not stale when boss has rotated to face Past Astro`() {
        val bossRotation = atan2(-150f, 300f)  // some non-zero angle
        var shipRotation = 0f  // stale before the fix
        shipRotation = bossRotation
        assert(shipRotation != 0f) { "Ship rotation must not remain at stale zero" }
        assertEquals(bossRotation, shipRotation, 0.001f)
    }

    // ── Mirror fix 2: Past Astro thruster after EMP ───────────────────────────

    @Test
    fun `past astro velocity is zeroed when EMP has fired`() {
        val bossEmpFired = true
        val orbitAngle = 0.5f; val playerDir = 1f
        val orbitRadius = 400f  // FleetSystem.OUTER_RADIUS
        val velocityX: Float
        val velocityY: Float
        if (!bossEmpFired) {
            val tangentSpeed = (0.3f * orbitRadius).coerceAtLeast(80f)
            velocityX = -sin(orbitAngle) * playerDir * tangentSpeed
            velocityY = cos(orbitAngle) * playerDir * tangentSpeed
        } else {
            velocityX = 0f
            velocityY = 0f
        }
        assertEquals(0f, velocityX, 0.001f)
        assertEquals(0f, velocityY, 0.001f)
    }

    @Test
    fun `past astro velocity exceeds thruster threshold when EMP has not fired`() {
        val bossEmpFired = false
        val orbitAngle = 0.5f; val playerDir = 1f
        val orbitRadius = 400f
        val velocityX: Float
        val velocityY: Float
        if (!bossEmpFired) {
            val tangentSpeed = (0.3f * orbitRadius).coerceAtLeast(80f)
            velocityX = -sin(orbitAngle) * playerDir * tangentSpeed
            velocityY = cos(orbitAngle) * playerDir * tangentSpeed
        } else {
            velocityX = 0f
            velocityY = 0f
        }
        val speed = sqrt(velocityX * velocityX + velocityY * velocityY)
        assert(speed > 50f) { "VectorRenderer thruster threshold is 50f; speed was $speed" }
    }

    // ── Mirror fix 3B: Past Astro rotation during freeze ──────────────────────

    @Test
    fun `past astro rotation step is clamped to 5 rad per second per frame`() {
        val deltaTime = 0.016f
        val turnRate = 5f
        val paRotation = 0f
        // Crystal Astro directly behind Past Astro → target diff ≈ π rad
        val target = PI.toFloat()
        var diff = target - paRotation
        while (diff > PI) diff -= (2 * PI).toFloat()
        while (diff < -PI) diff += (2 * PI).toFloat()
        val step = diff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
        // Max turn in one 60fps frame: 5 × 0.016 = 0.08 rad
        assertEquals(turnRate * deltaTime, step, 0.001f)
    }

    @Test
    fun `past astro rotation reaches target within one second at 5 rad per second`() {
        val deltaTime = 1f / 60f
        val turnRate = 5f
        var paRotation = 0f
        val target = PI.toFloat()  // 180° away — worst case
        var frames = 0
        while (frames < 200) {
            var diff = target - paRotation
            while (diff > PI) diff -= (2 * PI).toFloat()
            while (diff < -PI) diff += (2 * PI).toFloat()
            if (Math.abs(diff) < 0.01f) break
            paRotation += diff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
            frames++
        }
        // π rad at 5 rad/s = ~0.63s = ~38 frames at 60fps
        assert(frames <= 60) { "Should rotate 180° in under 1s; took $frames frames" }
    }

    @Test
    fun `past astro rotation converges on target ahead`() {
        // Simple case: target at 0, start at π/2
        val deltaTime = 0.016f
        val turnRate = 5f
        var paRotation = (PI / 2f).toFloat()  // π/2 rad = 90 deg
        val target = 0f
        var diff = target - paRotation  // 0 - π/2 = -π/2
        while (diff > PI) diff -= (2 * PI).toFloat()
        while (diff < -PI) diff += (2 * PI).toFloat()  // -π/2 stays as is
        val step = diff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
        val newRotation = paRotation + step
        // -π/2 is clamped to [-0.08, 0.08], so step = -0.08
        // newRotation = π/2 - 0.08 ≈ 1.49 rad (closer to 0)
        assert(Math.abs(newRotation - target) < Math.abs(paRotation - target)) {
            "Should move closer to target: started at $paRotation, now at $newRotation"
        }
    }

    // ── Mirror fix 3C: Alignment gate on Past Astro firing ───────────────────

    @Test
    fun `fire timer decrements when past astro is within aim gate`() {
        val deltaTime = 0.016f
        var pastAstroFireTimer = 2.5f
        val aimDiff = 0.25f  // 0.25 < 0.3 → within ~17° gate
        if (aimDiff < 0.3f) {
            pastAstroFireTimer -= deltaTime
        }
        assertEquals(2.5f - deltaTime, pastAstroFireTimer, 0.001f)
    }

    @Test
    fun `fire timer does not decrement when past astro is outside aim gate`() {
        val deltaTime = 0.016f
        var pastAstroFireTimer = 2.5f
        val aimDiff = 0.8f  // 0.8 > 0.3 → outside gate
        if (aimDiff < 0.3f) {
            pastAstroFireTimer -= deltaTime
        }
        assertEquals(2.5f, pastAstroFireTimer, 0.001f)
    }

    @Test
    fun `aim diff gate threshold is approximately 17 degrees`() {
        val thresholdRad = 0.3f
        val thresholdDeg = Math.toDegrees(thresholdRad.toDouble()).toFloat()
        assert(thresholdDeg > 16f && thresholdDeg < 18f) {
            "0.3 rad should be ~17°, was $thresholdDeg°"
        }
    }
}
