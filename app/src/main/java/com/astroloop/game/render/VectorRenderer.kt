package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.astroloop.game.core.BossRush
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.core.StoryStateManager.corruptColor
import com.astroloop.game.data.EnemyType
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.entity.*
import com.astroloop.game.entity.LeechParticle
import com.astroloop.game.entity.ReentryBurn
import com.astroloop.game.system.FleetSystem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class FadingTrail(
    val points: List<Triple<Float, Float, Float>>,  // x, y, age-based alpha at snapshot time
    val trailWidth: Float,
    var fadeTimer: Float = 0.75f
) {
    companion object {
        const val FADE_DURATION = 0.75f
    }
}

class VectorRenderer(
    private val shapeRenderer: ShapeRenderer
) {
    fun renderFadingTrail(canvas: Canvas, trail: FadingTrail) {
        if (trail.points.size < 2) return
        val fadeMul = (trail.fadeTimer / FadingTrail.FADE_DURATION).coerceIn(0f, 1f)
        shapeRenderer.setColor(GameConfig.COLOR_ASTEROID_TRAIL)
        for (i in 0 until trail.points.size - 1) {
            val (x1, y1, alpha1) = trail.points[i]
            val (x2, y2, _) = trail.points[i + 1]
            val alpha = alpha1 * fadeMul
            shapeRenderer.setAlpha(alpha * 0.6f)
            shapeRenderer.setStrokeWidth(trail.trailWidth * alpha)
            shapeRenderer.drawLine(canvas, x1, y1, x2, y2)
        }
        shapeRenderer.setAlpha(1f)
        shapeRenderer.setStrokeWidth(2f)
    }

    fun renderShip(canvas: Canvas, ship: Ship, state: GameState) {
        if (!ship.isActive) return

        // Corruption runs: player ship renders boss-red
        val displayColor = if (state.isCorruptionRun) Boss.CORRUPTION_COLOR else ship.shipColor

        // Crystal afterimages (ghost at aim origin with laser sight — multiple simultaneous)
        for (afterimage in state.crystalAfterimages) {
            val alpha = (afterimage.timer / 3.0f).coerceIn(0f, 1f) * 0.4f
            ShipRenderer.drawShip(
                canvas = canvas,
                shapeRenderer = shapeRenderer,
                x = afterimage.x,
                y = afterimage.y,
                rotation = afterimage.rotation,
                size = ship.radius,
                shipColor = 0xFFFFFFFF.toInt(),
                pilotColor = 0xFFFFFFFF.toInt(),
                startingWeaponId = ship.startingWeaponId,
                alpha = alpha
            )

            // Laser sight line to target (pulsing)
            if (afterimage.hasTarget && afterimage.aiming) {
                val laserTime = (System.currentTimeMillis() % 10000L) / 1000f
                val pulse = (sin(laserTime * 10.0) * 0.3 + 0.7).toFloat()
                val laserAlpha = pulse * (afterimage.timer / 3f).coerceIn(0f, 1f)
                val laserColor = Boss.CORRUPTION_COLOR
                val r = (laserColor shr 16) and 0xFF
                val g = (laserColor shr 8) and 0xFF
                val b = laserColor and 0xFF
                shapeRenderer.setColor(android.graphics.Color.argb((laserAlpha * 255).toInt(), r, g, b))
                shapeRenderer.setStrokeWidth(1f)
                shapeRenderer.drawLine(
                    canvas,
                    afterimage.x, afterimage.y,
                    afterimage.targetX, afterimage.targetY
                )
                shapeRenderer.setStrokeWidth(2f)
                shapeRenderer.setAlpha(1f)
            }
        }

        // Cryo Field visual - faint light-blue circle
        if (state.cryoSlowPercent > 0f && ship.health > 0f) {
            val cryoRadius = 100f * state.cryoRadiusMultiplier  // Base cryo radius scaled
            val pulse = (sin(System.currentTimeMillis() / 500.0) * 0.05 + 0.15).toFloat()

            shapeRenderer.setColor(0xFF88CCFF.toInt())  // Light blue
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(pulse)
            shapeRenderer.drawCircle(canvas, ship.position.x, ship.position.y, cryoRadius, false)
            shapeRenderer.setAlpha(1f)
        }

        // Emergency shield — pulsing retreat aura (same visual as crystal, white-hot ring only)
        if (state.emergencyShieldActive && ship.isActive) {
            val shieldRadius = 60f
            val time = (System.currentTimeMillis() % 10000L) / 1000f
            val pulse = 0.6f + 0.4f * sin(time * 4f).toFloat()

            shapeRenderer.setColor(CrystalPalette.MID)
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(0.2f * pulse)
            shapeRenderer.drawCircle(canvas, ship.position.x, ship.position.y, shieldRadius, false)

            shapeRenderer.setColor(CrystalPalette.ICE)
            shapeRenderer.setStrokeWidth(1.5f)
            shapeRenderer.setAlpha(0.4f * pulse)
            shapeRenderer.drawCircle(canvas, ship.position.x, ship.position.y, shieldRadius * 0.8f, false)

            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // Crystal energy shield — pulsing damage aura
        if (state.hasCrystalPowers && ship.isActive) {
            val shieldRadius = 60f
            val time = (System.currentTimeMillis() % 10000L) / 1000f
            val pulse = 0.6f + 0.4f * sin(time * 4f).toFloat()

            // Outer glow ring (icy cyan)
            shapeRenderer.setColor(CrystalPalette.MID)
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(0.2f * pulse)
            shapeRenderer.drawCircle(canvas, ship.position.x, ship.position.y, shieldRadius, false)

            // Inner bright ring (icy highlight)
            shapeRenderer.setColor(CrystalPalette.ICE)
            shapeRenderer.setStrokeWidth(1.5f)
            shapeRenderer.setAlpha(0.4f * pulse)
            shapeRenderer.drawCircle(canvas, ship.position.x, ship.position.y, shieldRadius * 0.8f, false)

            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // Blink if invulnerable — suppress during retreat (emergency shield aura is the visual indicator)
        if (ship.isInvulnerable && state.retreatPhase == 0) {
            val blink = ((ship.invulnerabilityTimer * 10).toInt() % 2 == 0)
            if (!blink) return
        }

        // Draw thrust effect first (behind ship) — skip when stunned, crystal powers use boss engine glow
        if (!state.playerStunned) {
            if (state.hasCrystalPowers) {
                val time = (System.currentTimeMillis() % 10000L) / 1000f
                val pulse = 0.5f + 0.5f * sin(time * 6f).toFloat()
                val engineX = ship.position.x + cos(ship.rotation + PI.toFloat()) * ship.radius * 0.5f
                val engineY = ship.position.y + sin(ship.rotation + PI.toFloat()) * ship.radius * 0.5f
                shapeRenderer.setColor(0xFFFF4400.toInt())
                shapeRenderer.setAlpha(0.5f + pulse * 0.5f)
                shapeRenderer.drawCircle(canvas, engineX, engineY, ship.radius * 0.15f * (0.8f + pulse * 0.4f))
                shapeRenderer.setAlpha(1f)
            } else {
                val momentumActive = state.momentumDamageBonus > 0f && ship.velocity.lengthSquared() > 100f
                drawThrustEffect(canvas, ship, momentumActive)
            }
        }

        // Shield outline — steady second outline matching ship shape, thickness = shield %
        if (ship.currentShield > 0f && ship.maxShield > 0f) {
            val shieldPercent = (ship.currentShield / ship.maxShield).coerceIn(0f, 1f)
            val shieldFadeIn = (state.survivalTime / 1f).coerceIn(0f, 1f)

            val shieldScale = 1.3f
            val size = ship.radius * shieldScale
            val x = ship.position.x
            val y = ship.position.y
            val r = ship.rotation

            val shieldPoints = floatArrayOf(
                size, 0f,
                -size * 0.7f, -size * 0.5f,
                -size * 0.4f, 0f,
                -size * 0.7f, size * 0.5f
            )

            shapeRenderer.setColor(displayColor)
            shapeRenderer.setStrokeWidth(0.5f + 3.5f * shieldPercent)
            shapeRenderer.setAlpha(0.6f * shieldFadeIn)
            shapeRenderer.drawPolygon(canvas, x, y, shieldPoints, r)

            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // Draw ship with weapon-based variation and pilot dot
        ShipRenderer.drawShip(
            canvas = canvas,
            shapeRenderer = shapeRenderer,
            x = ship.position.x,
            y = ship.position.y,
            rotation = ship.rotation,
            size = ship.radius,
            shipColor = displayColor,
            pilotColor = ship.pilotColor,
            startingWeaponId = ship.startingWeaponId,
            alpha = 1f
        )



        // Health bar below ship (only when recently damaged)
        if (ship.healthBarTimer > 0) {
            val maxHealth = ship.maxHealth
            val healthPercent = (ship.health / maxHealth).coerceIn(0f, 1f)
            val alpha = (ship.healthBarTimer / 0.5f).coerceIn(0f, 1f)  // Fade out in last 0.5s

            val barWidth = ship.radius * 2f
            val barHeight = 4f
            val barY = ship.position.y + ship.radius + 8f

            // Background
            shapeRenderer.setColor(0xFF333333.toInt())
            shapeRenderer.setAlpha(alpha)
            shapeRenderer.drawRect(canvas, ship.position.x - barWidth / 2, barY, barWidth, barHeight, true)

            // Health fill (color-coded)
            shapeRenderer.setColor(when {
                healthPercent > 0.5f -> 0xFF00FF00.toInt()
                healthPercent > 0.25f -> 0xFFFFFF00.toInt()
                else -> 0xFFFF0000.toInt()
            })
            shapeRenderer.setAlpha(alpha)
            shapeRenderer.drawRect(canvas, ship.position.x - barWidth / 2, barY, barWidth * healthPercent, barHeight, true)
            shapeRenderer.setAlpha(1f)
        }
    }

    fun drawCombatDrone(canvas: Canvas, drone: Drone, state: GameState) {
        if (!drone.isActive) return

        val droneSize = if (state.droneEvolved) 20f else 15f
        // Apply dodge offset to visual position
        val droneX = drone.position.x + drone.dodgeOffsetX
        val droneY = drone.position.y + drone.dodgeOffsetY
        val facingAngle = drone.rotation
        val color = drone.themeColor

        val cosF = cos(facingAngle)
        val sinF = sin(facingAngle)

        // Draw thruster flame if active (behind drone) — keep existing logic
        if (drone.thrusterActive) {
            val flameLength = 8f + (kotlin.random.Random.nextFloat() * 4f)
            val backAngle = facingAngle + PI.toFloat()
            val flameBaseX = droneX + cos(backAngle) * droneSize * 0.5f
            val flameBaseY = droneY + sin(backAngle) * droneSize * 0.5f
            val flameTipX = flameBaseX + cos(backAngle) * flameLength
            val flameTipY = flameBaseY + sin(backAngle) * flameLength

            shapeRenderer.setColor(0xFFFF6600.toInt())
            shapeRenderer.setStrokeWidth(4f)
            shapeRenderer.setAlpha(0.7f)
            shapeRenderer.drawLine(canvas, flameBaseX, flameBaseY, flameTipX, flameTipY)

            shapeRenderer.setColor(0xFFFFFF00.toInt())
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(0.9f)
            val innerTipX = flameBaseX + cos(backAngle) * flameLength * 0.7f
            val innerTipY = flameBaseY + sin(backAngle) * flameLength * 0.7f
            shapeRenderer.drawLine(canvas, flameBaseX, flameBaseY, innerTipX, innerTipY)
            shapeRenderer.setAlpha(1f)
        }

        // Draw drone body — use theme color instead of hardcoded 0xFF44AAFF
        shapeRenderer.setColor(color)
        shapeRenderer.setStrokeWidth(2f)

        // Triangle points (dart shape, scaled by droneSize)
        val tipX = droneX + cosF * droneSize
        val tipY = droneY + sinF * droneSize
        val leftX = droneX + cos(facingAngle + 2.4f) * droneSize * 0.7f
        val leftY = droneY + sin(facingAngle + 2.4f) * droneSize * 0.7f
        val rightX = droneX + cos(facingAngle - 2.4f) * droneSize * 0.7f
        val rightY = droneY + sin(facingAngle - 2.4f) * droneSize * 0.7f

        shapeRenderer.drawLine(canvas, tipX, tipY, leftX, leftY)
        shapeRenderer.drawLine(canvas, leftX, leftY, rightX, rightY)
        shapeRenderer.drawLine(canvas, rightX, rightY, tipX, tipY)

        // Draw cockpit dot — slightly lighter than theme color
        val cockpitColor = lightenColor(color)
        shapeRenderer.setColor(cockpitColor)
        shapeRenderer.drawCircle(canvas, droneX + cosF * 2f, droneY + sinF * 2f, 2f, true)

        // Glow effect
        shapeRenderer.setColor(color)
        shapeRenderer.setAlpha(0.3f)
        shapeRenderer.drawCircle(canvas, droneX, droneY, droneSize * 0.8f, true)
        shapeRenderer.setAlpha(1f)

        // Dodge afterimage (when dodging)
        if (drone.dodgeTimer > 0f) {
            val alpha = (drone.dodgeTimer / 0.15f).coerceIn(0f, 1f) * 0.4f
            shapeRenderer.setColor(color)
            shapeRenderer.setAlpha(alpha)
            // Ghost at original position (without dodge offset)
            shapeRenderer.drawCircle(canvas, drone.position.x, drone.position.y, droneSize * 0.6f, true)
            shapeRenderer.setAlpha(1f)
        }

        // Evolution glow trail
        if (state.droneEvolved) {
            // Short fading trail behind the drone
            val backAngle = facingAngle + PI.toFloat()
            for (i in 1..3) {
                val trailAlpha = (0.3f - i * 0.08f).coerceAtLeast(0f)
                val trailDist = i * 8f
                val trailX = droneX + cos(backAngle) * trailDist
                val trailY = droneY + sin(backAngle) * trailDist
                shapeRenderer.setColor(color)
                shapeRenderer.setAlpha(trailAlpha)
                shapeRenderer.drawCircle(canvas, trailX, trailY, droneSize * (0.5f - i * 0.1f), true)
            }
            shapeRenderer.setAlpha(1f)
        }
    }

    fun drawCombatDrones(canvas: Canvas, drones: List<Drone>, state: GameState) {
        for (drone in drones) {
            drawCombatDrone(canvas, drone, state)
        }
    }

    fun drawRevengeProtocolEffect(canvas: Canvas, ship: Ship) {
        // Red pulsing glow around ship
        val pulseTime = System.currentTimeMillis() / 150.0
        val pulse = (sin(pulseTime) * 0.3 + 0.7).toFloat()

        // Outer red glow
        shapeRenderer.setColor(0xFFFF2222.toInt())  // Red
        shapeRenderer.setStrokeWidth(4f)
        shapeRenderer.setAlpha(0.3f * pulse)
        shapeRenderer.drawCircle(canvas, ship.position.x, ship.position.y, ship.radius * 2f)

        // Inner red glow
        shapeRenderer.setStrokeWidth(2f)
        shapeRenderer.setAlpha(0.5f * pulse)
        shapeRenderer.drawCircle(canvas, ship.position.x, ship.position.y, ship.radius * 1.5f)

        shapeRenderer.setAlpha(1f)
    }

    private fun drawThrustEffect(canvas: Canvas, ship: Ship, momentumActive: Boolean = false) {
        val speed = ship.velocity.length()
        if (speed < 20f) return

        val intensity = (speed / 400f).coerceIn(0f, 1f)
        val thrustBackX = -ship.radius * 0.5f
        val size = ship.radius * (0.25f + intensity * 0.75f)
        val color = if (momentumActive) 0xFFFF2200.toInt() else 0xFFFF9933.toInt()

        canvas.save()
        canvas.translate(ship.position.x, ship.position.y)
        canvas.rotate(Math.toDegrees(ship.rotation.toDouble()).toFloat())
        ThrusterDesigns.design01_current(canvas, thrustBackX, 0f, size, shapeRenderer, color)
        canvas.restore()
    }

    private fun rotatePoint(px: Float, py: Float, rotation: Float): Pair<Float, Float> {
        val cos = cos(rotation)
        val sin = sin(rotation)
        return Pair(px * cos - py * sin, px * sin + py * cos)
    }

    private fun lightenColor(color: Int): Int {
        val r = ((color shr 16) and 0xFF).coerceAtMost(200) + 55
        val g = ((color shr 8) and 0xFF).coerceAtMost(200) + 55
        val b = (color and 0xFF).coerceAtMost(200) + 55
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int {
        val r = ((from shr 16 and 0xFF) + ((to shr 16 and 0xFF) - (from shr 16 and 0xFF)) * t).toInt()
        val g = ((from shr 8 and 0xFF) + ((to shr 8 and 0xFF) - (from shr 8 and 0xFF)) * t).toInt()
        val b = ((from and 0xFF) + ((to and 0xFF) - (from and 0xFF)) * t).toInt()
        return (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    fun renderAsteroid(canvas: Canvas, asteroid: Asteroid) {
        if (!asteroid.isActive) return

        // Trail asteroid — draw wake
        if (asteroid.type == AsteroidType.TRAIL && asteroid.trailPoints.size >= 2) {
            val trailWidth = asteroid.getTrailWidth()
            val trailLifetime = asteroid.getTrailLifetime()
            val latestTime = asteroid.trailPoints.last().timestamp

            shapeRenderer.setColor(GameConfig.COLOR_ASTEROID_TRAIL)
            for (i in 0 until asteroid.trailPoints.size - 1) {
                val p1 = asteroid.trailPoints[i]
                val p2 = asteroid.trailPoints[i + 1]
                val age = latestTime - p1.timestamp
                val alpha = (1f - age / trailLifetime).coerceIn(0f, 1f)
                shapeRenderer.setAlpha(alpha * 0.6f)
                shapeRenderer.setStrokeWidth(trailWidth * alpha)
                shapeRenderer.drawLine(canvas, p1.x, p1.y, p2.x, p2.y)
            }
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        shapeRenderer.setColor(asteroid.getColor())
        shapeRenderer.setStrokeWidth(2f)

        if (asteroid.shapePoints.isNotEmpty()) {
            // Fill with black to occlude stars behind asteroid
            shapeRenderer.setColor(0xFF000000.toInt())
            shapeRenderer.drawPolygon(
                canvas,
                asteroid.position.x,
                asteroid.position.y,
                asteroid.shapePoints,
                asteroid.rotation,
                filled = true
            )
            // Restore asteroid color for outline
            shapeRenderer.setColor(asteroid.getColor())
            shapeRenderer.drawPolygon(
                canvas,
                asteroid.position.x,
                asteroid.position.y,
                asteroid.shapePoints,
                asteroid.rotation
            )
        } else {
            // Small or shapeless asteroid — circle is visually identical and cheaper
            shapeRenderer.setColor(0xFF000000.toInt())
            shapeRenderer.drawCircle(canvas, asteroid.position.x, asteroid.position.y, asteroid.radius, true)
            shapeRenderer.setColor(asteroid.getColor())
            shapeRenderer.drawCircle(canvas, asteroid.position.x, asteroid.position.y, asteroid.radius)
        }

        // Volatile asteroids pulse
        if (asteroid.type == AsteroidType.VOLATILE) {
            val pulse = (System.currentTimeMillis() % 500) / 500f
            shapeRenderer.setAlpha(0.3f + pulse * 0.3f)
            shapeRenderer.drawCircle(canvas, asteroid.position.x, asteroid.position.y, asteroid.radius * 1.2f)
            shapeRenderer.setAlpha(1f)
        }

        // Magnetic asteroids have field lines
        if (asteroid.type == AsteroidType.MAGNETIC) {
            shapeRenderer.setStrokeWidth(1f)
            shapeRenderer.setAlpha(0.5f)
            for (i in 0 until 4) {
                val angle = asteroid.rotation + (i * PI / 2).toFloat()
                val innerR = asteroid.radius * 1.1f
                val outerR = asteroid.radius * 1.6f
                shapeRenderer.drawLine(
                    canvas,
                    asteroid.position.x + cos(angle) * innerR,
                    asteroid.position.y + sin(angle) * innerR,
                    asteroid.position.x + cos(angle) * outerR,
                    asteroid.position.y + sin(angle) * outerR
                )
            }
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // Cryo Field icy blue outline
        if (asteroid.cryoAffected) {
            shapeRenderer.setColor(0xFF88CCFF.toInt())  // Icy blue
            shapeRenderer.setStrokeWidth(3f)
            shapeRenderer.setAlpha(150f / 255f)
            shapeRenderer.drawCircle(canvas, asteroid.position.x, asteroid.position.y, asteroid.radius + 4f)
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // Health bar above asteroid (only when damaged)
        if (asteroid.health < asteroid.maxHealth) {
            val healthPercent = (asteroid.health / asteroid.maxHealth).coerceIn(0f, 1f)
            val barWidth = asteroid.radius * 1.5f
            val barHeight = 3f
            val barY = asteroid.position.y - asteroid.radius - 8f

            // Background
            shapeRenderer.setColor(0xFF333333.toInt())
            shapeRenderer.drawRect(canvas, asteroid.position.x - barWidth / 2, barY, barWidth, barHeight, true)

            // Health fill
            shapeRenderer.setColor(
                when {
                    healthPercent > 0.5f -> 0xFF00FF00.toInt()
                    healthPercent > 0.25f -> 0xFFFFFF00.toInt()
                    else -> 0xFFFF0000.toInt()
                }
            )
            shapeRenderer.drawRect(canvas, asteroid.position.x - barWidth / 2, barY, barWidth * healthPercent, barHeight, true)

            // Reset color
            shapeRenderer.setColor(asteroid.getColor())
        }
    }

    fun renderProjectile(canvas: Canvas, projectile: Projectile) {
        if (!projectile.isActive) return

        shapeRenderer.setColor(projectile.color)

        when (projectile.type) {
            ProjectileType.BULLET -> renderBullet(canvas, projectile)
            ProjectileType.MISSILE -> renderMissile(canvas, projectile)
            ProjectileType.BEAM -> renderBeam(canvas, projectile)
            ProjectileType.ORBITER -> renderOrbiter(canvas, projectile)
            ProjectileType.MINE -> renderMine(canvas, projectile)
            ProjectileType.LIGHTNING -> renderLightning(canvas, projectile)
            ProjectileType.TORPEDO -> renderTorpedo(canvas, projectile)
            ProjectileType.FLAK -> renderFlak(canvas, projectile)
            ProjectileType.GRAVITY -> renderGravity(canvas, projectile)
            ProjectileType.PLASMA -> renderPlasma(canvas, projectile)
            ProjectileType.BOMBLET -> renderBomblet(canvas, projectile)
            ProjectileType.FRAGMENT -> renderFragment(canvas, projectile)
            ProjectileType.RECALL_SHOT -> {
                    // Render like a railgun shot but with corruption color
                    if (projectile.isRecalling && !projectile.recallRetargeted) {
                        // Paused (recalling) — draw pulsing glow circle only, no line
                        shapeRenderer.setColor(Boss.CORRUPTION_COLOR)
                        val pulse = 0.5f + 0.5f * sin(projectile.recallPauseTimer * 10f)
                        shapeRenderer.setAlpha(0.3f + pulse * 0.4f)
                        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, 8f)
                        shapeRenderer.setAlpha(1f)
                    } else {
                        // Normal line rendering (moving shot)
                        shapeRenderer.setColor(projectile.color)
                        shapeRenderer.setStrokeWidth(2.5f)
                        val len = 15f
                        val vx = projectile.velocity.x
                        val vy = projectile.velocity.y
                        val speed = sqrt(vx * vx + vy * vy).coerceAtLeast(1f)
                        shapeRenderer.drawLine(
                            canvas,
                            projectile.position.x, projectile.position.y,
                            projectile.position.x - vx / speed * len, projectile.position.y - vy / speed * len
                        )
                    }
                }
        }
    }

    private fun renderBullet(canvas: Canvas, projectile: Projectile) {
        // Railgun recall: pulsing glow while paused at screen edge (matches RECALL_SHOT rendering)
        if (projectile.weaponId == "railgun" && projectile.isRecalling) {
            val pulse = 0.5f + 0.5f * sin(projectile.recallPauseTimer * 10f)
            shapeRenderer.setColor(projectile.color)
            shapeRenderer.setAlpha(0.3f + pulse * 0.4f)
            shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, 8f)
            shapeRenderer.setAlpha(1f)
            return
        }

        shapeRenderer.setStrokeWidth(projectile.width)
        val angle = projectile.velocity.angle()
        val tail = projectile.length
        shapeRenderer.drawLine(
            canvas,
            projectile.position.x,
            projectile.position.y,
            projectile.position.x - cos(angle) * tail,
            projectile.position.y - sin(angle) * tail
        )
    }

    private fun renderMissile(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(2f)
        val angle = projectile.velocity.angle()
        // Missile body
        val points = floatArrayOf(
            8f, 0f,
            -6f, -4f,
            -4f, 0f,
            -6f, 4f
        )
        shapeRenderer.drawPolygon(canvas, projectile.position.x, projectile.position.y, points, angle)
        // Exhaust
        shapeRenderer.setColor(0xFFFF8800.toInt())
        shapeRenderer.drawLine(
            canvas,
            projectile.position.x - cos(angle) * 6,
            projectile.position.y - sin(angle) * 6,
            projectile.position.x - cos(angle) * 12,
            projectile.position.y - sin(angle) * 12
        )
    }

    private fun renderBeam(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(projectile.width)
        shapeRenderer.setAlpha(0.8f)
        // Use beamAngle for attached beams, otherwise calculate from velocity
        val angle = if (projectile.beamOrigin != null) {
            projectile.beamAngle
        } else if (projectile.velocity.lengthSquared() > 0) {
            projectile.velocity.angle()
        } else {
            projectile.beamAngle
        }

        if (projectile.beamOrigin != null) {
            // Generic beam rendering for other beam weapons
            shapeRenderer.drawLine(
                canvas,
                projectile.position.x,
                projectile.position.y,
                projectile.position.x + cos(angle) * projectile.length,
                projectile.position.y + sin(angle) * projectile.length
            )
        } else {
            shapeRenderer.drawLine(
                canvas,
                projectile.position.x - cos(angle) * projectile.length / 2,
                projectile.position.y - sin(angle) * projectile.length / 2,
                projectile.position.x + cos(angle) * projectile.length / 2,
                projectile.position.y + sin(angle) * projectile.length / 2
            )
        }
        shapeRenderer.setAlpha(1f)
    }

    private fun renderOrbiter(canvas: Canvas, projectile: Projectile) {
        // Keep filled orbiters - original great look
        shapeRenderer.setStrokeWidth(2f)
        shapeRenderer.setAlpha(projectile.fadeAlpha)
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, projectile.radius)
        shapeRenderer.setAlpha(0.5f * projectile.fadeAlpha)
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, projectile.radius * 0.5f, true)
        shapeRenderer.setAlpha(1f)
    }

    private fun renderMine(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(2f)
        val r = projectile.radius
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, r)
        // 8 static spikes at fixed 45-degree intervals, each crossing the circle
        // edge: inner end inside the circle, outer end further outside (the outer
        // part is a little longer than the inner part).
        val innerR = r * 0.55f
        val outerR = r * 1.5f
        for (i in 0 until 8) {
            val angle = (i * PI / 4).toFloat()
            shapeRenderer.drawLine(
                canvas,
                projectile.position.x + cos(angle) * innerR,
                projectile.position.y + sin(angle) * innerR,
                projectile.position.x + cos(angle) * outerR,
                projectile.position.y + sin(angle) * outerR
            )
        }
    }

    private fun renderLightning(canvas: Canvas, projectile: Projectile) {
        val cx = projectile.position.x
        val cy = projectile.position.y
        // Fade alpha based on remaining lifetime
        val alpha = (1f - projectile.age / projectile.lifetime).coerceIn(0f, 1f)

        if (projectile.velocity.lengthSquared() < 0.01f) {
            // Radial storm burst (Solar Storm / Phoenix Flare)
            val progress = (projectile.age / projectile.lifetime).coerceIn(0f, 1f)

            // 1. White-hot core — expands then fades
            val coreMaxRadius = 35f
            val coreExpand = if (progress < 0.375f) progress / 0.375f else 1f
            val coreRadius = 8f + (coreMaxRadius - 8f) * coreExpand
            val coreFade = if (progress < 0.375f) 1f else 1f - (progress - 0.375f) / 0.625f
            shapeRenderer.setColor(0xFFFFFFFF.toInt())
            shapeRenderer.setAlpha(alpha * coreFade.coerceIn(0f, 1f) * 0.8f)
            shapeRenderer.drawCircle(canvas, cx, cy, coreRadius, true)

            // 2. Shockwave ring — expanding outline
            val ringRadius = 8f + 72f * progress
            val ringStroke = 4f - 3f * progress
            shapeRenderer.setColor(projectile.color)
            shapeRenderer.setStrokeWidth(ringStroke.coerceAtLeast(0.5f))
            shapeRenderer.setAlpha(alpha * 0.7f)
            shapeRenderer.drawCircle(canvas, cx, cy, ringRadius)

            // 3. Lightning tendrils — jagged bolts radiating outward
            val tendrilCount = 7
            val seed = (cx * 1000f + cy).toLong()
            val rng = kotlin.random.Random(seed)
            val tendrilAngles = FloatArray(tendrilCount) { rng.nextFloat() * (2f * PI).toFloat() }
            val tendrilLengths = FloatArray(tendrilCount) { 40f + rng.nextFloat() * 20f }

            // Tendrils grow outward with progress
            val tendrilGrow = (progress * 3f).coerceIn(0f, 1f)

            for (i in 0 until tendrilCount) {
                val angle = tendrilAngles[i]
                val maxLen = tendrilLengths[i]
                val len = maxLen * tendrilGrow

                // 3 zigzag segments per tendril
                val seg1Frac = 0.33f
                val seg2Frac = 0.66f
                val perpSign = if (i % 2 == 0) 1f else -1f
                val perpAngle = angle + (PI / 2f).toFloat()
                val zigOffset = 6f + rng.nextFloat() * 4f

                val p0x = cx
                val p0y = cy
                val p1x = cx + cos(angle) * len * seg1Frac + cos(perpAngle) * zigOffset * perpSign
                val p1y = cy + sin(angle) * len * seg1Frac + sin(perpAngle) * zigOffset * perpSign
                val p2x = cx + cos(angle) * len * seg2Frac - cos(perpAngle) * zigOffset * perpSign * 0.7f
                val p2y = cy + sin(angle) * len * seg2Frac - sin(perpAngle) * zigOffset * perpSign * 0.7f
                val p3x = cx + cos(angle) * len
                val p3y = cy + sin(angle) * len

                // Glow pass
                shapeRenderer.setColor(projectile.color)
                shapeRenderer.setStrokeWidth(5f)
                shapeRenderer.setAlpha(alpha * 0.4f)
                shapeRenderer.drawLine(canvas, p0x, p0y, p1x, p1y)
                shapeRenderer.drawLine(canvas, p1x, p1y, p2x, p2y)
                shapeRenderer.drawLine(canvas, p2x, p2y, p3x, p3y)

                // Core pass
                shapeRenderer.setColor(0xFFFFFFFF.toInt())
                shapeRenderer.setStrokeWidth(1.5f)
                shapeRenderer.setAlpha(alpha * 0.9f)
                shapeRenderer.drawLine(canvas, p0x, p0y, p1x, p1y)
                shapeRenderer.drawLine(canvas, p1x, p1y, p2x, p2y)
                shapeRenderer.drawLine(canvas, p2x, p2y, p3x, p3y)

                // Fork off second segment
                val forkAngle = angle + perpSign * (PI / 5f).toFloat()
                val forkLen = len * 0.3f
                val fkx = p1x + cos(forkAngle) * forkLen
                val fky = p1y + sin(forkAngle) * forkLen
                shapeRenderer.setColor(projectile.color)
                shapeRenderer.setStrokeWidth(3f)
                shapeRenderer.setAlpha(alpha * 0.3f)
                shapeRenderer.drawLine(canvas, p1x, p1y, fkx, fky)
                shapeRenderer.setColor(0xFFFFFFFF.toInt())
                shapeRenderer.setStrokeWidth(1f)
                shapeRenderer.setAlpha(alpha * 0.7f)
                shapeRenderer.drawLine(canvas, p1x, p1y, fkx, fky)
            }
        } else {
            // Traveling lightning bolt (e.g. chain-lightning along velocity)
            val angle = projectile.velocity.angle()
            val halfLen = projectile.length / 2f
            // Glow pass
            shapeRenderer.setStrokeWidth(5f)
            shapeRenderer.setAlpha(alpha * 0.35f)
            shapeRenderer.drawLine(canvas,
                cx - cos(angle) * halfLen, cy - sin(angle) * halfLen,
                cx + cos(angle) * halfLen, cy + sin(angle) * halfLen)
            // Core jagged segments
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(alpha)
            val segments = 4
            var px = cx - cos(angle) * halfLen
            var py = cy - sin(angle) * halfLen
            for (i in 0 until segments) {
                val t = (i + 1).toFloat() / segments
                val nx = cx - cos(angle) * halfLen + cos(angle) * projectile.length * t
                val ny = cy - sin(angle) * halfLen + sin(angle) * projectile.length * t
                val offsetX = if (i % 2 == 0) 5f else -5f
                val offsetY = if (i % 2 == 0) -5f else 5f
                shapeRenderer.drawLine(canvas, px, py, nx + offsetX, ny + offsetY)
                px = nx + offsetX
                py = ny + offsetY
            }
        }
        shapeRenderer.setAlpha(1f)
    }

    private fun renderTorpedo(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(2f)
        val angle = projectile.velocity.angle()
        // Large torpedo shape
        val points = floatArrayOf(
            12f, 0f,
            -8f, -5f,
            -6f, 0f,
            -8f, 5f
        )
        shapeRenderer.drawPolygon(canvas, projectile.position.x, projectile.position.y, points, angle)
        // Glow
        shapeRenderer.setAlpha(0.4f)
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, projectile.radius * 1.5f)
        shapeRenderer.setAlpha(1f)
    }

    private fun renderFlak(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(2f)
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, projectile.radius)
        // Inner detail
        shapeRenderer.drawCross(canvas, projectile.position.x, projectile.position.y, projectile.radius * 0.5f)
    }

    private fun renderGravity(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(1f)
        shapeRenderer.setAlpha(0.3f)
        // Concentric circles
        for (i in 1..4) {
            val r = projectile.radius * i / 4
            shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, r)
        }
        shapeRenderer.setAlpha(1f)
    }

    private fun renderPlasma(canvas: Canvas, projectile: Projectile) {
        val px = projectile.position.x
        val py = projectile.position.y
        val pr = projectile.radius
        val lifeProgress = (projectile.age / projectile.lifetime).coerceIn(0f, 1f)
        val fadeAlpha = (1f - lifeProgress).coerceIn(0f, 1f)

        if (projectile.weaponId == "phoenix_flare") {
            // Ground fire pool — shimmering napalm with heat distortion
            val time = System.currentTimeMillis()

            // 1. Outer pool glow (deep orange, fading)
            shapeRenderer.setColor(0xFFFF4400.toInt())
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(fadeAlpha * 0.3f)
            shapeRenderer.drawCircle(canvas, px, py, pr * 1.2f, true)

            // 2. Inner pool (cycles orange ↔ yellow for heat shimmer)
            val shimmer = (sin(time / 150.0) * 0.5 + 0.5).toFloat()
            val poolColor = if (shimmer > 0.5f) 0xFFFF6600.toInt() else 0xFFFFAA00.toInt()
            shapeRenderer.setColor(poolColor)
            shapeRenderer.setAlpha(fadeAlpha * 0.5f)
            shapeRenderer.drawCircle(canvas, px, py, pr * 0.8f, true)

            // 3. Bright core
            shapeRenderer.setColor(0xFFFFCC00.toInt())
            shapeRenderer.setAlpha(fadeAlpha * 0.6f)
            shapeRenderer.drawCircle(canvas, px, py, pr * 0.4f, true)

            // 4. Animated ring pulses outward
            val ringCount = 2
            for (i in 0 until ringCount) {
                val ringPhase = ((time / 400.0 + i * 0.5) % 1.0).toFloat()
                val ringRadius = pr * 0.3f + pr * 0.9f * ringPhase
                val ringAlpha = fadeAlpha * 0.4f * (1f - ringPhase)
                shapeRenderer.setColor(0xFFFF6600.toInt())
                shapeRenderer.setStrokeWidth(2f)
                shapeRenderer.setAlpha(ringAlpha)
                shapeRenderer.drawCircle(canvas, px, py, ringRadius)
            }

            // 5. Ember particles drifting upward from pool edge
            val embers = 5
            val seed = (px * 100f + py).toLong()
            val rng = kotlin.random.Random(seed)
            for (i in 0 until embers) {
                val angle = rng.nextFloat() * (2f * PI).toFloat()
                val dist = pr * (0.5f + rng.nextFloat() * 0.5f)
                val drift = (time / 1000.0f + i * 0.7f) % 2f
                val ex = px + cos(angle) * dist
                val ey = py + sin(angle) * dist - drift * 20f  // Float upward
                val emberAlpha = fadeAlpha * (1f - drift / 2f) * 0.7f
                if (emberAlpha > 0.05f) {
                    shapeRenderer.setColor(0xFFFFCC00.toInt())
                    shapeRenderer.setAlpha(emberAlpha)
                    shapeRenderer.drawCircle(canvas, ex, ey, 2f, true)
                }
            }

            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        } else {
            // Default PLASMA rendering for other weapons
            shapeRenderer.setStrokeWidth(3f)
            shapeRenderer.setAlpha(fadeAlpha * 0.6f)
            shapeRenderer.drawCircle(canvas, px, py, pr)
            shapeRenderer.setAlpha(1f)
        }
    }

    private fun renderBomblet(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(2f)
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, projectile.radius, true)
        // Outer glow
        shapeRenderer.setAlpha(0.4f)
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, projectile.radius * 1.5f)
        shapeRenderer.setAlpha(1f)
    }

    private fun renderFragment(canvas: Canvas, projectile: Projectile) {
        shapeRenderer.setStrokeWidth(1.5f)
        shapeRenderer.drawCircle(canvas, projectile.position.x, projectile.position.y, projectile.radius, true)
    }

    fun renderEnemyShip(canvas: Canvas, enemy: EnemyShip, state: GameState) {
        if (!enemy.isActive) return

        // Apply dodge offset for perfect-dodge enemies
        val dodgeOffset = enemy.getDodgeOffset()
        val x = enemy.position.x + dodgeOffset.first
        val y = enemy.position.y + dodgeOffset.second
        val r = enemy.rotation
        val size = enemy.radius

        // Warp-in effect
        if (enemy.isWarping) {
            renderWarpEffect(canvas, enemy)
            return  // Don't render ship while warping
        }

        // Color palette: normal ship colors in corruption (enemies are the uncorrupted crew),
        // corrupted red otherwise
        val normalColor = ShipDefinitions.getShip(enemy.shipId)?.color
        val hullColor = if (state.isCorruptionRun && normalColor != null) normalColor else Boss.CORRUPTION_COLOR
        // Every enemy is a pilot from the time loop — cockpit dot uses their pilot color.
        // Normal runs: full pilot color. Corruption runs: darkened pilot color (same as hangar dots).
        val pilotId = StoryStateManager.FLEET_MAPPING[enemy.shipId]
        val basePilotColor = PilotDefinitions.getPilot(pilotId ?: "")?.color
        val pilotColor = when {
            basePilotColor != null && state.isCorruptionRun -> corruptColor(basePilotColor)
            basePilotColor != null -> basePilotColor
            normalColor != null -> lightenColor(normalColor)
            else -> lightenColor(Boss.CORRUPTION_COLOR)
        }
        val accentColor = pilotColor

        // Alpha for Phantom (Specter) cloak, otherwise fully opaque
        val alpha = if (enemy.type == EnemyType.SPECTER) enemy.cloakAlpha else 1f

        // Dodge afterimage ghost at original position
        if (enemy.dodgeTimer > 0f) {
            val ghostAlpha = (enemy.dodgeTimer / 0.4f) * 0.4f
            ShipRenderer.drawShip(
                canvas = canvas,
                shapeRenderer = shapeRenderer,
                x = enemy.position.x,
                y = enemy.position.y,
                rotation = r,
                size = size,
                shipColor = hullColor,
                pilotColor = accentColor,
                startingWeaponId = getWeaponIdForShip(enemy.shipId),
                alpha = ghostAlpha
            )
        }

        // Thrust effect for enemy ships
        val currentSpeed = enemy.velocity.length()
        if (currentSpeed > 50f) {
            val intensity = (currentSpeed / 400f).coerceIn(0.3f, 1f)
            val thrustBackX = -size * 0.5f
            val thrustSize = size * (0.25f + intensity * 0.75f)
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(Math.toDegrees(r.toDouble()).toFloat())
            shapeRenderer.setAlpha(alpha)
            ThrusterDesigns.design01_current(canvas, thrustBackX, 0f, thrustSize, shapeRenderer)
            shapeRenderer.setAlpha(1f)
            canvas.restore()
        }

        // Draw corrupted ship silhouette using player ship shape
        val weaponId = getWeaponIdForShip(enemy.shipId)
        ShipRenderer.drawShip(
            canvas = canvas,
            shapeRenderer = shapeRenderer,
            x = x,
            y = y,
            rotation = r,
            size = size,
            shipColor = hullColor,
            pilotColor = accentColor,
            startingWeaponId = weaponId,
            alpha = alpha
        )

        // Edge glow for visibility against black space — skip for crewmates (allied ships)
        if (!enemy.isCrewmate) {
            shapeRenderer.setColor(accentColor)
            shapeRenderer.setStrokeWidth(1f)
            shapeRenderer.setAlpha(0.3f * alpha)
            shapeRenderer.drawCircle(canvas, x, y, size + 2f, false)
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // === Type-specific visual overlays ===

        // Phantom (Specter): Targeting line when charging shot
        if (enemy.type == EnemyType.SPECTER && enemy.isChargingShot) {
            val pulse = (sin(System.currentTimeMillis() / 100.0) * 0.3 + 0.7).toFloat()
            shapeRenderer.setColor(0xFFFF2222.toInt())
            shapeRenderer.setStrokeWidth(1f)
            shapeRenderer.setAlpha(pulse * alpha)
            shapeRenderer.drawLine(canvas, x, y, enemy.targetLineX, enemy.targetLineY)
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // === Status overlays ===

        // Cryo Field icy blue outline
        if (enemy.cryoAffected) {
            shapeRenderer.setColor(0xFF88CCFF.toInt())  // Icy blue
            shapeRenderer.setStrokeWidth(3f)
            shapeRenderer.setAlpha(150f / 255f)
            shapeRenderer.drawCircle(canvas, x, y, size + 4f)
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // Health bar above enemy
        val healthPercent = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
        if (healthPercent < 1f) {
            val barWidth = size * 2f
            val barHeight = 4f
            val barY = y - size - 10f

            // Background
            shapeRenderer.setColor(0xFF333333.toInt())
            shapeRenderer.drawRect(canvas, x - barWidth / 2, barY, barWidth, barHeight, true)

            // Health
            shapeRenderer.setColor(
                when {
                    healthPercent > 0.5f -> 0xFF00FF00.toInt()
                    healthPercent > 0.25f -> 0xFFFFFF00.toInt()
                    else -> 0xFFFF0000.toInt()
                }
            )
            shapeRenderer.drawRect(canvas, x - barWidth / 2, barY, barWidth * healthPercent, barHeight, true)
        }

        // Spawn invulnerability shield — red pulsing aura
        if (enemy.isSpawnShielded && !enemy.isCrewmate) {
            val shieldProgress = (enemy.spawnShieldTimer / enemy.spawnShieldDuration).coerceIn(0f, 1f)
            // Pulse faster in last second
            val pulseSpeed = if (enemy.spawnShieldTimer < 1f) 12f else 4f
            val pulse = (0.7f + 0.3f * sin(System.currentTimeMillis() / (1000f / pulseSpeed))).toFloat()
            val shieldAlpha = shieldProgress * 0.4f * pulse

            shapeRenderer.setColor(0xFFFF2233.toInt())
            shapeRenderer.setStrokeWidth(3f)
            shapeRenderer.setAlpha(shieldAlpha)
            shapeRenderer.drawCircle(canvas, x, y, size + 6f)
            // Inner glow
            shapeRenderer.setStrokeWidth(1f)
            shapeRenderer.setAlpha(shieldAlpha * 0.5f)
            shapeRenderer.drawCircle(canvas, x, y, size + 10f)
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }
    }

    private fun getWeaponIdForShip(shipId: String): String {
        return ShipDefinitions.getShip(shipId)?.startingWeaponId ?: "pulse_cannon"
    }

    /** Edge pointer triangle — shows at screen edge pointing toward off-screen target */
    fun renderOffScreenPointer(
        canvas: Canvas, targetX: Float, targetY: Float,
        cameraX: Float, cameraY: Float, screenW: Float, screenH: Float, color: Int
    ) {
        val margin = 40f
        // Check if target is on-screen — no pointer needed
        if (targetX > cameraX + margin && targetX < cameraX + screenW - margin &&
            targetY > cameraY + margin && targetY < cameraY + screenH - margin) {
            return
        }

        // Screen center in world coords
        val cx = cameraX + screenW / 2f
        val cy = cameraY + screenH / 2f

        // Direction from center to target
        val dx = targetX - cx
        val dy = targetY - cy
        if (dx == 0f && dy == 0f) return
        val angle = atan2(dy, dx)

        // Find intersection with screen edge (with margin)
        val halfW = screenW / 2f - margin
        val halfH = screenH / 2f - margin
        val scaleX = if (dx != 0f) halfW / kotlin.math.abs(dx) else Float.MAX_VALUE
        val scaleY = if (dy != 0f) halfH / kotlin.math.abs(dy) else Float.MAX_VALUE
        val scale = minOf(scaleX, scaleY)

        val pointerX = cx + dx * scale
        val pointerY = cy + dy * scale

        // Draw filled triangle pointing toward target
        val triSize = 14f
        val perpAngle = angle + PI.toFloat() / 2f
        val tipX = pointerX + cos(angle) * triSize
        val tipY = pointerY + sin(angle) * triSize
        val baseX1 = pointerX - cos(angle) * triSize * 0.3f + cos(perpAngle) * triSize * 0.5f
        val baseY1 = pointerY - sin(angle) * triSize * 0.3f + sin(perpAngle) * triSize * 0.5f
        val baseX2 = pointerX - cos(angle) * triSize * 0.3f - cos(perpAngle) * triSize * 0.5f
        val baseY2 = pointerY - sin(angle) * triSize * 0.3f - sin(perpAngle) * triSize * 0.5f

        // Pulsing alpha
        val time = (System.currentTimeMillis() % 10000L) / 1000f
        val pulse = 0.6f + 0.4f * sin(time * 4f).toFloat()

        shapeRenderer.setColor(color)
        shapeRenderer.setStrokeWidth(2.5f)
        shapeRenderer.setAlpha(pulse)
        shapeRenderer.drawLine(canvas, tipX, tipY, baseX1, baseY1)
        shapeRenderer.drawLine(canvas, baseX1, baseY1, baseX2, baseY2)
        shapeRenderer.drawLine(canvas, baseX2, baseY2, tipX, tipY)
        shapeRenderer.setAlpha(1f)
        shapeRenderer.setStrokeWidth(2f)
    }

    private fun renderWarpEffect(canvas: Canvas, enemy: EnemyShip) {
        val x = enemy.position.x
        val y = enemy.position.y
        val progress = enemy.spawnTime / enemy.warpInDuration  // 0 to 1

        // Cyan/purple energy rings converging
        shapeRenderer.setStrokeWidth(3f)

        // Outer ring (shrinking)
        val outerRadius = enemy.radius * (3f - progress * 2f)  // 3x to 1x
        shapeRenderer.setColor(0xFF00FFFF.toInt())  // Cyan
        shapeRenderer.setAlpha(1f - progress)
        shapeRenderer.drawCircle(canvas, x, y, outerRadius)

        // Middle ring
        val midRadius = enemy.radius * (2f - progress)  // 2x to 1x
        shapeRenderer.setColor(0xFFAA44FF.toInt())  // Purple
        shapeRenderer.setAlpha(0.8f)
        shapeRenderer.drawCircle(canvas, x, y, midRadius)

        // Inner ring (growing)
        val innerRadius = enemy.radius * progress  // 0 to 1x
        shapeRenderer.setColor(0xFFFFFFFF.toInt())  // White
        shapeRenderer.setAlpha(progress)
        shapeRenderer.drawCircle(canvas, x, y, innerRadius)

        // Converging energy lines
        shapeRenderer.setColor(0xFF00FFFF.toInt())
        shapeRenderer.setAlpha(0.6f)
        shapeRenderer.setStrokeWidth(2f)
        for (i in 0 until 8) {
            val angle = (i * PI / 4 + enemy.spawnTime * 3).toFloat()
            val startDist = outerRadius + 20f
            val endDist = innerRadius
            shapeRenderer.drawLine(
                canvas,
                x + cos(angle) * startDist,
                y + sin(angle) * startDist,
                x + cos(angle) * endDist,
                y + sin(angle) * endDist
            )
        }

        shapeRenderer.setAlpha(1f)
    }

    fun renderShipExplosion(canvas: Canvas, explosion: ShipExplosion) {
        if (!explosion.isActive) return

        // Flash effect at start
        if (explosion.flashIntensity > 0) {
            shapeRenderer.setColor(0xFFFFFFFF.toInt())
            shapeRenderer.setAlpha(explosion.flashIntensity * 0.5f)
            // Draw a large circle for flash
            val debris = explosion.getDebris()
            if (debris.isNotEmpty()) {
                val centerX = debris.map { it.position.x }.average().toFloat()
                val centerY = debris.map { it.position.y }.average().toFloat()
                shapeRenderer.drawCircle(canvas, centerX, centerY, 50f * explosion.flashIntensity, true)
            }
            shapeRenderer.setAlpha(1f)
        }

        // Render debris
        for (debris in explosion.getDebris()) {
            val alpha = (debris.lifetime / 2f).coerceIn(0f, 1f)
            shapeRenderer.setColor(debris.color)
            shapeRenderer.setAlpha(alpha)

            if (debris.size < 5f) {
                // Small particles - just dots
                shapeRenderer.drawCircle(canvas, debris.position.x, debris.position.y, debris.size, true)
            } else {
                // Larger debris - draw as triangular fragments
                shapeRenderer.setStrokeWidth(1.5f)
                val points = floatArrayOf(
                    debris.size, 0f,
                    -debris.size * 0.5f, -debris.size * 0.5f,
                    -debris.size * 0.5f, debris.size * 0.5f
                )
                shapeRenderer.drawPolygon(
                    canvas,
                    debris.position.x,
                    debris.position.y,
                    points,
                    debris.rotation + explosion.timer * debris.rotationSpeed
                )
            }
        }

        shapeRenderer.setAlpha(1f)
        shapeRenderer.setStrokeWidth(2f)
    }

    fun renderPowerUp(canvas: Canvas, powerUp: PowerUp) {
        if (!powerUp.isActive) return

        val scale = powerUp.getPulseScale()
        val size = powerUp.radius * scale

        when (powerUp.type) {
            PowerUpType.SCORE_PICKUP -> {
                // Score pickups: star dust (yellow particles) or credits (cyan diamonds)
                if (powerUp.isFromEnemy) {
                    // Credits: Cyan diamond
                    shapeRenderer.setColor(0xFF00FFFF.toInt())
                    shapeRenderer.setAlpha(1f)
                    shapeRenderer.setStrokeWidth(2f)
                    shapeRenderer.drawDiamond(canvas, powerUp.position.x, powerUp.position.y, size, powerUp.pulsePhase * 0.5f)
                    // Inner sparkle
                    shapeRenderer.setColor(0xFFFFFFFF.toInt())
                    shapeRenderer.drawCircle(canvas, powerUp.position.x, powerUp.position.y, size * 0.2f, true)
                } else {
                    // Star dust: Yellow particles
                    shapeRenderer.setColor(0xFFFFDD44.toInt())
                    shapeRenderer.setAlpha(1f)
                    shapeRenderer.setStrokeWidth(1f)
                    // Small star shape
                    for (i in 0 until 4) {
                        val angle = (i * PI / 2 + powerUp.pulsePhase * 0.5f).toFloat()
                        val innerR = size * 0.3f
                        val outerR = size
                        shapeRenderer.drawLine(
                            canvas,
                            powerUp.position.x + cos(angle) * innerR,
                            powerUp.position.y + sin(angle) * innerR,
                            powerUp.position.x + cos(angle) * outerR,
                            powerUp.position.y + sin(angle) * outerR
                        )
                    }
                }
            }
            PowerUpType.EVOLUTION_DIAMOND -> {
                val px = powerUp.position.x
                val py = powerUp.position.y
                val alpha = powerUp.getFadeAlpha()  // fades out when a rival diamond is collected
                val time = System.currentTimeMillis()
                val glowPulse = (sin(time / 300.0) * 0.3 + 0.7).toFloat()

                // Outer glow ring
                shapeRenderer.setColor(0xFFFFD700.toInt())  // Gold
                shapeRenderer.setStrokeWidth(2f)
                shapeRenderer.setAlpha(alpha * 0.25f * glowPulse)
                shapeRenderer.drawCircle(canvas, px, py, size * 1.6f, false)

                // Gold outer diamond
                shapeRenderer.setColor(0xFFFFD700.toInt())
                shapeRenderer.setAlpha(alpha * 0.9f)
                shapeRenderer.setStrokeWidth(3f)
                shapeRenderer.drawDiamond(canvas, px, py, size, powerUp.pulsePhase * 0.3f)

                // White inner diamond (smaller)
                shapeRenderer.setColor(0xFFFFFFFF.toInt())
                shapeRenderer.setAlpha(alpha * 0.8f)
                shapeRenderer.setStrokeWidth(2f)
                shapeRenderer.drawDiamond(canvas, px, py, size * 0.5f, powerUp.pulsePhase * 0.3f + 0.5f)

                // Center sparkle
                shapeRenderer.setColor(0xFFFFFFFF.toInt())
                shapeRenderer.setAlpha(alpha * glowPulse)
                shapeRenderer.drawCircle(canvas, px, py, size * 0.15f, true)
            }
            else -> {
                // Regular upgrade power-ups
                shapeRenderer.setColor(GameConfig.COLOR_POWERUP)
                shapeRenderer.setAlpha(powerUp.getFadeAlpha())
                shapeRenderer.setStrokeWidth(2f)

                // Diamond shape with inner detail
                shapeRenderer.drawDiamond(canvas, powerUp.position.x, powerUp.position.y, size, powerUp.pulsePhase * 0.5f)

                // Inner icon based on type
                when (powerUp.type) {
                    PowerUpType.WEAPON -> {
                        // Crosshair icon
                        shapeRenderer.drawCross(canvas, powerUp.position.x, powerUp.position.y, size * 0.4f)
                    }
                    PowerUpType.PASSIVE -> {
                        // Plus icon
                        shapeRenderer.setStrokeWidth(3f)
                        shapeRenderer.drawCross(canvas, powerUp.position.x, powerUp.position.y, size * 0.3f)
                    }
                    else -> {}
                }
            }
        }

        shapeRenderer.setAlpha(1f)
    }

    fun renderVisualEffects(canvas: Canvas, effectManager: VisualEffectManager) {
        // Render explosion/hit effects
        for (effect in effectManager.getEffects()) {
            val alpha = 1f - (effect.age / effect.lifetime)

            when (effect.type) {
                EffectType.EXPLOSION -> {
                    // Expanding ring effect
                    shapeRenderer.setColor(effect.color)
                    shapeRenderer.setAlpha(alpha * 0.8f)
                    shapeRenderer.setStrokeWidth(4f * alpha + 1f)
                    shapeRenderer.drawCircle(canvas, effect.x, effect.y, effect.radius)

                    // Inner glow
                    shapeRenderer.setAlpha(alpha * 0.4f)
                    shapeRenderer.drawCircle(canvas, effect.x, effect.y, effect.radius * 0.6f, true)
                }
                EffectType.HIT_FLASH -> {
                    // Brief flash effect
                    shapeRenderer.setColor(effect.color)
                    shapeRenderer.setAlpha(alpha * 0.6f)
                    shapeRenderer.drawCircle(canvas, effect.x, effect.y, effect.radius, true)
                }
                EffectType.PHOENIX_SHOCKWAVE -> {
                    val progress = effect.age / effect.lifetime
                    // Thick bright ring, narrows and fades as it expands
                    shapeRenderer.setColor(effect.color)
                    shapeRenderer.setAlpha((1f - progress) * 0.9f)
                    shapeRenderer.setStrokeWidth(8f * (1f - progress) + 1f)
                    shapeRenderer.drawCircle(canvas, effect.x, effect.y, effect.radius)

                    // Secondary inner ring for depth
                    shapeRenderer.setColor(0xFFFFAA44.toInt())
                    shapeRenderer.setAlpha((1f - progress) * 0.5f)
                    shapeRenderer.setStrokeWidth(3f * (1f - progress) + 0.5f)
                    shapeRenderer.drawCircle(canvas, effect.x, effect.y, effect.radius * 0.75f)
                }
                EffectType.FLAK_EXPLOSION -> {
                    val progress = effect.age / effect.lifetime
                    FlakDesigns.render(canvas, effect.color, effect.x, effect.y, effect.maxRadius, progress)
                }
                EffectType.BOSS_SHOCKWAVE -> {
                    val t = effect.age / effect.lifetime
                    val radius = effect.maxRadius * t
                    val alpha = (1f - t).coerceIn(0f, 1f)
                    shapeRenderer.setColor(effect.color)
                    shapeRenderer.setAlpha(alpha)
                    shapeRenderer.setStrokeWidth(4f)
                    shapeRenderer.drawCircle(canvas, effect.x, effect.y, radius, false)
                    shapeRenderer.setAlpha(alpha * 0.4f)
                    shapeRenderer.drawCircle(canvas, effect.x, effect.y, radius * 0.9f, false)
                    shapeRenderer.setAlpha(1f)
                }
                else -> {}
            }
        }

        shapeRenderer.setAlpha(1f)
        shapeRenderer.setStrokeWidth(2f)
    }

    fun renderSolarTelegraphs(canvas: Canvas, enemies: List<com.astroloop.game.entity.EnemyShip>) {
        for (enemy in enemies) {
            if (!enemy.solarTelegraphActive) continue
            val progress = 1f - (enemy.solarTelegraphTimer / 0.5f).coerceIn(0f, 1f)
            // Pulsing ring that contracts toward the strike point as telegraph counts down
            val maxRadius = 80f
            val minRadius = 20f
            val radius = maxRadius - (maxRadius - minRadius) * progress
            val alpha = 0.4f + 0.6f * (sin(progress * Math.PI.toFloat() * 4f).coerceIn(0f, 1f))
            shapeRenderer.setColor(Boss.CORRUPTION_COLOR)
            shapeRenderer.setAlpha(alpha)
            shapeRenderer.setStrokeWidth(2f + 2f * (1f - progress))
            shapeRenderer.drawCircle(canvas, enemy.solarTelegraphX, enemy.solarTelegraphY, radius)
            // Crosshair lines
            val crossSize = radius * 0.4f
            shapeRenderer.setAlpha(alpha * 0.7f)
            shapeRenderer.setStrokeWidth(1.5f)
            shapeRenderer.drawLine(canvas,
                enemy.solarTelegraphX - crossSize, enemy.solarTelegraphY,
                enemy.solarTelegraphX + crossSize, enemy.solarTelegraphY)
            shapeRenderer.drawLine(canvas,
                enemy.solarTelegraphX, enemy.solarTelegraphY - crossSize,
                enemy.solarTelegraphX, enemy.solarTelegraphY + crossSize)
        }
        shapeRenderer.setAlpha(1f)
    }

    fun renderZapBeam(canvas: Canvas, state: GameState) {
        if (!state.zapBeamActive) return
        val progress = state.zapBeamTimer / 0.5f  // 0.5s total duration
        val alpha = (1f - progress).coerceIn(0f, 1f)

        // Zap beam line from player to crewmate death position
        shapeRenderer.setColor(state.zapBeamColor)
        shapeRenderer.setAlpha(alpha * 0.8f)
        shapeRenderer.setStrokeWidth(3f * alpha + 1f)
        shapeRenderer.drawLine(canvas, state.zapBeamFromX, state.zapBeamFromY,
            state.zapBeamToX, state.zapBeamToY)

        // Secondary thinner line for lightning effect
        shapeRenderer.setAlpha(alpha * 0.4f)
        shapeRenderer.setStrokeWidth(1f)
        val midX = (state.zapBeamFromX + state.zapBeamToX) / 2f
        val midY = (state.zapBeamFromY + state.zapBeamToY) / 2f
        val jitterX = (kotlin.random.Random.nextFloat() - 0.5f) * 20f * alpha
        val jitterY = (kotlin.random.Random.nextFloat() - 0.5f) * 20f * alpha
        shapeRenderer.drawLine(canvas, state.zapBeamFromX, state.zapBeamFromY,
            midX + jitterX, midY + jitterY)
        shapeRenderer.drawLine(canvas, midX + jitterX, midY + jitterY,
            state.zapBeamToX, state.zapBeamToY)

        // Teleport circle at death position (expanding)
        val circleRadius = 30f * progress
        shapeRenderer.setAlpha(alpha * 0.6f)
        shapeRenderer.setStrokeWidth(2f * alpha + 0.5f)
        shapeRenderer.drawCircle(canvas, state.zapBeamToX, state.zapBeamToY, circleRadius)

        // Inner bright flash at target
        shapeRenderer.setColor(0xFFFFFFFF.toInt())
        shapeRenderer.setAlpha(alpha * 0.5f)
        shapeRenderer.drawCircle(canvas, state.zapBeamToX, state.zapBeamToY, circleRadius * 0.4f, true)

        shapeRenderer.setAlpha(1f)
    }

    private val chargeSphereGradientPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Pre-allocated objects for renderSawDiscs — avoids per-disc-per-frame alloc
    private val sawDiscToothPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val sawDiscPath = android.graphics.Path()
    private val sawDiscFlashPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }


    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    private val leechParticlePaint = Paint().apply {
        color = 0xFF8844CC.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val damageTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = 24f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    fun renderDamageNumbers(canvas: Canvas, effectManager: VisualEffectManager) {
        for (dmgNum in effectManager.getDamageNumbers()) {
            val alpha = dmgNum.getAlpha()
            damageTextPaint.color = dmgNum.color
            damageTextPaint.alpha = (alpha * 255).toInt()

            // Scale text based on damage (larger damage = bigger text)
            val scale = when {
                dmgNum.value >= 50 -> 1.5f
                dmgNum.value >= 20 -> 1.2f
                else -> 1f
            }
            val critScale = if (dmgNum.isCrit) 1.5f else 1.0f
            damageTextPaint.textSize = 24f * scale * critScale

            val text = dmgNum.label ?: dmgNum.value.toString()
            canvas.drawText(text, dmgNum.x, dmgNum.y, damageTextPaint)
        }
    }

    fun renderBoss(canvas: Canvas, boss: Boss) {
        if (!boss.isActive) return

        val cx = boss.position.x
        val cy = boss.position.y
        val size = Boss.BOSS_SIZE
        val rot = boss.rotation

        // Draw afterimage first (faded copy at old position)
        if (boss.afterimageActive) {
            val alpha = (boss.afterimageTimer / Boss.AFTERIMAGE_DURATION * 0.4f)
            ShipRenderer.drawShip(
                canvas = canvas,
                shapeRenderer = shapeRenderer,
                x = boss.afterimageX,
                y = boss.afterimageY,
                rotation = boss.afterimageRotation,
                size = size,
                shipColor = Boss.CORRUPTION_COLOR,
                pilotColor = lightenColor(Boss.CORRUPTION_COLOR),
                startingWeaponId = "railgun",
                alpha = alpha
            )
        }

        // Re-entry burn (EMP rush) — ghosts/embers under the hull, bow shock ahead of it
        if (boss.reentryBurn.hasContent() || boss.isRushing) {
            renderReentryBurn(
                canvas, boss.reentryBurn, cx, cy, rot, size,
                Boss.CORRUPTION_COLOR, "railgun",
                intensity = if (boss.isRushing && !boss.rushBraking) BossRush.easeIn(boss.rushTimer) else 0f
            )
        }

        // Draw boss ship (corrupted Specter using normal ship sprite)
        ShipRenderer.drawShip(
            canvas = canvas,
            shapeRenderer = shapeRenderer,
            x = cx,
            y = cy,
            rotation = rot,
            size = size,
            shipColor = Boss.CORRUPTION_COLOR,
            pilotColor = lightenColor(Boss.CORRUPTION_COLOR),
            startingWeaponId = "railgun"
        )

        // Fleet shield visual — pulsing cyan barrier while shielded
        if (boss.shielded) {
            val time = (System.currentTimeMillis() % 10000L) / 1000f
            val shieldPulse = 0.15f + 0.05f * sin(time * 3f)
            shapeRenderer.setColor(CrystalPalette.MID)
            shapeRenderer.setStrokeWidth(1.5f)
            shapeRenderer.setAlpha(shieldPulse)
            shapeRenderer.drawCircle(canvas, cx, cy, Boss.FLEET_SHIELD_RING_RADIUS, false)
            shapeRenderer.setAlpha(shieldPulse * 0.5f)
            shapeRenderer.drawCircle(canvas, cx, cy, Boss.BOSS_SIZE * 1.4f, false)
            shapeRenderer.setAlpha(1f)
        }

        // Shield deflection sparks — normal-fight path (renderBossShieldEffects draws them
        // in the corruption finale; the two phase guards are mutually exclusive, never both)
        renderSawSparks(canvas, boss.shieldSparks)

        // Engine glow
        val pulse = 0.5f + 0.5f * sin(boss.enginePulse)
        val engineX = cx + cos(rot + PI.toFloat()) * size * 0.5f
        val engineY = cy + sin(rot + PI.toFloat()) * size * 0.5f
        shapeRenderer.setColor(0xFFFF4400.toInt())
        shapeRenderer.setAlpha(0.5f + pulse * 0.5f)
        shapeRenderer.drawCircle(canvas, engineX, engineY, size * 0.15f * (0.8f + pulse * 0.4f))
        shapeRenderer.setAlpha(1f)

        // Energy shield aura (mirrors corrupted Astro crystal powers)
        if (!boss.isStunned) {
            val shieldRadius = Boss.SHIELD_DEFLECT_RADIUS  // shots fizzle on this same ring
            val time = (System.currentTimeMillis() % 10000L) / 1000f
            val pulse = 0.6f + 0.4f * sin(time * 4f).toFloat()

            // Outer glow ring (icy cyan)
            shapeRenderer.setColor(CrystalPalette.MID)
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(0.2f * pulse)
            shapeRenderer.drawCircle(canvas, cx, cy, shieldRadius, false)

            // Inner bright ring (icy white-cyan)
            shapeRenderer.setColor(CrystalPalette.ICE)
            shapeRenderer.setStrokeWidth(1.5f)
            shapeRenderer.setAlpha(0.4f * pulse)
            shapeRenderer.drawCircle(canvas, cx, cy, shieldRadius * 0.8f, false)

            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }

        // Health bar (only when vulnerable)
        if (!boss.isInvulnerable) {
            val barWidth = size * 3f
            val barHeight = 4f
            val barY = cy - size - 12f
            val barX = cx - barWidth / 2f

            // Background
            shapeRenderer.setColor(0xFF222222.toInt())
            shapeRenderer.drawRect(canvas, barX, barY, barWidth, barHeight, true)

            // Health fill
            val healthPercent = boss.getHealthPercent()
            shapeRenderer.setColor(Boss.CORRUPTION_COLOR)
            shapeRenderer.drawRect(canvas, barX, barY, barWidth * healthPercent, barHeight, true)
        }
    }

    /**
     * Re-entry burn for a rushing ship: afterimage ghost chain, shed embers, and a
     * bow shock compressed ahead of the nose. [intensity] 0..1 scales the bow shock
     * (0 while braking/stopped — ghosts and embers still fade out on their own).
     */
    fun renderReentryBurn(
        canvas: Canvas,
        burn: ReentryBurn,
        x: Float, y: Float, rotation: Float,
        size: Float,
        shipColor: Int,
        startingWeaponId: String,
        intensity: Float
    ) {
        // Ghost chain (list is oldest-first, so the freshest ghost draws on top)
        for (g in burn.ghosts) {
            val alpha = (1f - g.age / ReentryBurn.GHOST_LIFETIME).coerceIn(0f, 1f) * 0.35f
            ShipRenderer.drawShip(
                canvas = canvas,
                shapeRenderer = shapeRenderer,
                x = g.x, y = g.y,
                rotation = g.rotation,
                size = size,
                shipColor = shipColor,
                pilotColor = lightenColor(shipColor),
                startingWeaponId = startingWeaponId,
                alpha = alpha
            )
        }

        // Ember shed
        renderSawSparks(canvas, burn.sparks)

        // Bow shock — nested arcs piling up ahead of the hull, white-hot → orange → red
        if (intensity > 0.05f) {
            val flicker = 0.8f + 0.2f * Random.nextFloat()
            val noseX = x + cos(rotation) * size * 0.9f
            val noseY = y + sin(rotation) * size * 0.9f
            val arcColors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFAA33.toInt(), Boss.CORRUPTION_COLOR)
            val arcRadii = floatArrayOf(size * 0.9f, size * 1.3f, size * 1.8f)
            val arcAlphas = floatArrayOf(0.9f, 0.6f, 0.35f)
            val sweep = 1.9f  // radians (~110°), centered on the heading
            for (i in arcColors.indices) {
                shapeRenderer.setColor(arcColors[i])
                shapeRenderer.setStrokeWidth(3f - i * 0.75f)
                shapeRenderer.setAlpha(arcAlphas[i] * intensity * flicker)
                shapeRenderer.drawArc(canvas, noseX, noseY, arcRadii[i],
                    rotation - sweep / 2f, sweep)
            }
            shapeRenderer.setAlpha(1f)
            shapeRenderer.setStrokeWidth(2f)
        }
    }

    /** Charge overlay: 1A Spiral·Solid design — beam, spiral tendrils, solid radial-gradient sphere.
     *  Color transitions red→blue-white matching the brainstorm variant selection.
     *  targetX/Y = Astro (normal run) or Past Astro (corruption run). */
    fun renderBossChargeOverlay(
        canvas: Canvas,
        boss: Boss,
        targetX: Float,
        targetY: Float,
        state: GameState
    ) {
        if (!state.bossCharging) return
        val progress = state.bossChargeProgress
        val cx = boss.position.x
        val cy = boss.position.y
        val rot = boss.rotation
        val time = (System.currentTimeMillis() % 10000L) / 1000f

        val barrelTipX = cx + cos(rot) * Boss.BOSS_SIZE * 1.2f
        val barrelTipY = cy + sin(rot) * Boss.BOSS_SIZE * 1.2f

        // redBlue color scheme: rgb(220-120p, 20+160p, 20+235p) → crimson at 0%, light blue at 100%
        val rComp = (220f - 120f * progress).toInt().coerceIn(0, 255)
        val gComp = (20f + 160f * progress).toInt().coerceIn(0, 255)
        val bComp = (20f + 235f * progress).toInt().coerceIn(0, 255)
        val mainColor = (0xFF shl 24) or (rComp shl 16) or (gComp shl 8) or bComp

        // --- Beam: transitions from red to blue-white as charge grows ---
        shapeRenderer.setColor(mainColor)
        shapeRenderer.setStrokeWidth(1.2f)
        shapeRenderer.setAlpha(0.3f + 0.5f * progress)
        shapeRenderer.drawLine(canvas, barrelTipX, barrelTipY, targetX, targetY)
        shapeRenderer.setAlpha(1f)

        // --- Spiral tendrils orbiting barrel tip ---
        val tendrilRadius = 26f + progress * 10f
        val spinRate = -1.9f - progress * 0.6f
        val tendrilAlpha = 0.2f + 0.45f * progress
        shapeRenderer.setColor(mainColor)
        shapeRenderer.setStrokeWidth(1.2f)
        for (i in 0 until 5) {
            val baseAngle = (i.toFloat() / 5f) * 2f * PI.toFloat() + time * spinRate
            val fromX = barrelTipX + cos(baseAngle) * tendrilRadius
            val fromY = barrelTipY + sin(baseAngle) * tendrilRadius
            shapeRenderer.setAlpha(tendrilAlpha * (0.6f + 0.4f * abs(sin(time * 3.2f + i))))
            shapeRenderer.drawLine(canvas, fromX, fromY, barrelTipX, barrelTipY)
        }
        shapeRenderer.setAlpha(1f)

        // --- Solid sphere: radial gradient white-center → color → transparent ---
        if (progress > 0.02f) {
            val sphereRadius = 10f + progress * 40f
            chargeSphereGradientPaint.shader = RadialGradient(
                barrelTipX, barrelTipY, sphereRadius,
                intArrayOf(0xFFFFFFFF.toInt(), mainColor, 0x00000000),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP
            )
            chargeSphereGradientPaint.alpha = (progress * 255).toInt().coerceIn(20, 255)
            canvas.drawCircle(barrelTipX, barrelTipY, sphereRadius, chargeSphereGradientPaint)
        }
        shapeRenderer.setStrokeWidth(2f)
    }

    /** Render the lethal charged-shot beam fired at the non-Astro player.
     *  [t] runs 0→1 over 0.4 s; the beam fades as t increases (brief lethal flash). */
    fun renderBossChargedShot(canvas: Canvas, boss: Boss, targetX: Float, targetY: Float, t: Float) {
        val alpha = (1f - t).coerceIn(0f, 1f)
        // Wide outer ice-blue glow
        shapeRenderer.setColor(CrystalPalette.ICE)
        shapeRenderer.setStrokeWidth(10f + 18f * (1f - t))
        shapeRenderer.setAlpha(alpha * 0.6f)
        shapeRenderer.drawLine(canvas, boss.position.x, boss.position.y, targetX, targetY)
        // Hot white core
        shapeRenderer.setColor(0xFFFFFFFF.toInt())
        shapeRenderer.setStrokeWidth(4f)
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.drawLine(canvas, boss.position.x, boss.position.y, targetX, targetY)
        shapeRenderer.setAlpha(1f)
        shapeRenderer.setStrokeWidth(2f)
    }

    /** Render just the boss shield barrier + ripple effects at the boss position.
     *  Used during corruption finale when the boss entity is a fleet target marker at the player. */
    fun renderBossShieldEffects(canvas: Canvas, boss: Boss) {
        if (!boss.isActive) return
        val cx = boss.position.x
        val cy = boss.position.y

        // Pulsing cyan barrier
        if (boss.shielded) {
            val time = (System.currentTimeMillis() % 10000L) / 1000f
            val shieldPulse = 0.15f + 0.05f * sin(time * 3f)
            shapeRenderer.setColor(CrystalPalette.MID)
            shapeRenderer.setStrokeWidth(1.5f)
            shapeRenderer.setAlpha(shieldPulse)
            shapeRenderer.drawCircle(canvas, cx, cy, Boss.FLEET_SHIELD_RING_RADIUS, false)
            shapeRenderer.setAlpha(shieldPulse * 0.5f)
            shapeRenderer.drawCircle(canvas, cx, cy, Boss.BOSS_SIZE * 1.4f, false)
            shapeRenderer.setAlpha(1f)
        }

        // Shield deflection sparks — corruption-finale path (see renderBoss)
        renderSawSparks(canvas, boss.shieldSparks)
    }

    fun renderSawDiscs(canvas: Canvas, discPositions: List<Pair<Float, Float>>, discRadius: Float, color: Int, alpha: Float = 1f, isWarpSaw: Boolean = false, spinSpeed: Float = 1f) {
        val time = System.currentTimeMillis()
        val period = (1000f / spinSpeed).toLong().coerceAtLeast(1)
        val spinAngle = (time % period) / period.toFloat() * 2 * PI.toFloat()

        for (pos in discPositions) {
            val dx = pos.first
            val dy = pos.second

            // Outer glow
            shapeRenderer.setColor(color)
            shapeRenderer.setAlpha(0.3f * alpha)
            shapeRenderer.setStrokeWidth(4f)
            shapeRenderer.drawCircle(canvas, dx, dy, discRadius + 4f, false)

            // Jagged sawblade (8 teeth)
            shapeRenderer.setColor(color)
            shapeRenderer.setAlpha(0.9f * alpha)
            shapeRenderer.setStrokeWidth(2f)
            val teeth = 8
            val innerR = discRadius * 0.6f
            val outerR = discRadius
            sawDiscPath.reset()
            for (i in 0 until teeth) {
                val angle1 = spinAngle + i * 2 * PI.toFloat() / teeth
                val angle2 = spinAngle + (i + 0.5f) * 2 * PI.toFloat() / teeth
                val ox = dx + cos(angle1) * outerR
                val oy = dy + sin(angle1) * outerR
                val ix = dx + cos(angle2) * innerR
                val iy = dy + sin(angle2) * innerR
                if (i == 0) sawDiscPath.moveTo(ox, oy) else sawDiscPath.lineTo(ox, oy)
                sawDiscPath.lineTo(ix, iy)
            }
            sawDiscPath.close()
            sawDiscToothPaint.color = color
            sawDiscToothPaint.alpha = (0.9f * alpha * 255).toInt()
            canvas.drawPath(sawDiscPath, sawDiscToothPaint)

            // Center bright dot
            shapeRenderer.setColor(0xFFFFFFFF.toInt())
            shapeRenderer.setAlpha(0.8f * alpha)
            shapeRenderer.drawCircle(canvas, dx, dy, discRadius * 0.15f, true)
        }
        shapeRenderer.setAlpha(1f)
        shapeRenderer.setStrokeWidth(2f)
    }

    /**
     * Warp Saw chrono teleport (Red Alert 2 chronoshift feel): over the first half the
     * blade dashes/shrinks out at (fromX,fromY); over the second half it ripples back in
     * at (toX,toY). [progress] 0..1. Nothing snaps — satisfies the no-instant-disappearance rule.
     */
    fun renderWarpSawChrono(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float,
                            progress: Float, discRadius: Float, color: Int, alpha: Float) {
        if (alpha <= 0f) return
        val out = progress < 0.5f
        val half = if (out) progress / 0.5f else (progress - 0.5f) / 0.5f   // 0..1 within the phase
        val cx = if (out) fromX else toX
        val cy = if (out) fromY else toY

        // Chrono shimmer rings: out = expand + fade, in = contract + appear.
        shapeRenderer.setStrokeWidth(3f)
        for (i in 0 until 2) {
            val ringPhase = (half + i * 0.4f).coerceIn(0f, 1f)
            val r = if (out) discRadius * (0.6f + ringPhase * 1.6f)
                    else      discRadius * (2.2f - ringPhase * 1.6f)
            val ringAlpha = if (out) (1f - ringPhase) else ringPhase
            shapeRenderer.setColor(color)
            shapeRenderer.setAlpha(0.5f * ringAlpha * alpha)
            shapeRenderer.drawCircle(canvas, cx, cy, r, false)
        }

        // Blade body: shrinks to nothing on the way out, grows from nothing on the way in.
        val scale = if (out) (1f - half) else half
        if (scale > 0.02f) {
            shapeRenderer.setColor(color)
            shapeRenderer.setAlpha(0.9f * alpha)
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.drawCircle(canvas, cx, cy, discRadius * scale, false)
            shapeRenderer.setColor(0xFFFFFFFF.toInt())
            shapeRenderer.setAlpha(0.8f * alpha)
            shapeRenderer.drawCircle(canvas, cx, cy, discRadius * scale * 0.3f, true)
        }

        // Dash streak between source and dest, brightest mid-warp.
        val streakAlpha = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
        sawDiscFlashPaint.color = color
        sawDiscFlashPaint.alpha = (0.4f * streakAlpha * alpha * 255).toInt()
        canvas.drawLine(fromX, fromY, toX, toY, sawDiscFlashPaint)

        shapeRenderer.setAlpha(1f)
        shapeRenderer.setStrokeWidth(2f)
    }

    fun renderSawSparks(canvas: Canvas, sparks: List<SawSpark>) {
        for (spark in sparks) {
            val alpha = 1f - (spark.age / spark.lifetime)
            shapeRenderer.setColor(spark.color)
            shapeRenderer.setAlpha(alpha)
            shapeRenderer.drawCircle(canvas, spark.x, spark.y, 2f, true)
        }
        shapeRenderer.setAlpha(1f)
    }

    fun renderOblivionBeam(canvas: Canvas, shipX: Float, shipY: Float, rotation: Float, shipRadius: Float, color: Int) {
        val ax = shipX + kotlin.math.cos(rotation) * shipRadius
        val ay = shipY + kotlin.math.sin(rotation) * shipRadius
        val bx = ax + kotlin.math.cos(rotation) * 1600f
        val by = ay + kotlin.math.sin(rotation) * 1600f
        // NOTE: %10000L keeps the float small enough for sin() precision
        val t = (System.currentTimeMillis() % 10000L) / 1000f
        val pulse = 0.85f + 0.15f * kotlin.math.sin(t * 12f)

        beamPaint.color = color
        beamPaint.alpha = (60 * pulse).toInt()
        beamPaint.strokeWidth = 22f
        canvas.drawLine(ax, ay, bx, by, beamPaint)
        beamPaint.alpha = (180 * pulse).toInt()
        beamPaint.strokeWidth = 9f
        canvas.drawLine(ax, ay, bx, by, beamPaint)
        beamPaint.color = 0xFFFFFFFF.toInt()
        beamPaint.alpha = (230 * pulse).toInt()
        beamPaint.strokeWidth = 3f
        canvas.drawLine(ax, ay, bx, by, beamPaint)
    }


    fun renderLeechParticles(canvas: Canvas, particles: List<LeechParticle>, shipX: Float, shipY: Float) {
        if (particles.isEmpty()) return
        for (p in particles) {
            val curX = p.edgeX + (shipX - p.edgeX) * p.t
            val curY = p.edgeY + (shipY - p.edgeY) * p.t
            val prevT = (p.t - 0.1f).coerceAtLeast(0f)
            val prevX = p.edgeX + (shipX - p.edgeX) * prevT
            val prevY = p.edgeY + (shipY - p.edgeY) * prevT
            val alpha = when {
                p.t < 0.1f  -> (p.t / 0.1f * 200f).toInt()
                p.t > 0.85f -> ((1f - p.t) / 0.15f * 200f).toInt()
                else        -> 200
            }.coerceIn(0, 255)
            leechParticlePaint.alpha = alpha
            canvas.drawLine(prevX, prevY, curX, curY, leechParticlePaint)
        }
        leechParticlePaint.alpha = 255
    }

    fun renderUpgradeIndicators(canvas: Canvas, powerUps: List<PowerUp>, ship: Ship) {
        for (powerUp in powerUps) {
            if (!powerUp.isActive) continue
            // Skip score pickups - only show arrows for actual upgrades and evolution diamonds
            if (powerUp.type == PowerUpType.SCORE_PICKUP) continue

            // Calculate direction from ship to upgrade
            val dx = powerUp.position.x - ship.position.x
            val dy = powerUp.position.y - ship.position.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 10f) continue  // Too close, no need for indicator

            // Normalize direction
            val dirX = dx / dist
            val dirY = dy / dist

            // Position arrow around ship at fixed distance (e.g., 60 pixels from ship center)
            val arrowDist = ship.radius + 40f
            val arrowX = ship.position.x + dirX * arrowDist
            val arrowY = ship.position.y + dirY * arrowDist
            val arrowAngle = atan2(dy, dx)

            // Draw triangular arrow pointing toward upgrade
            val arrowColor = if (powerUp.type == PowerUpType.EVOLUTION_DIAMOND) 0xFFFFD700.toInt() else GameConfig.COLOR_POWERUP
            shapeRenderer.setColor(arrowColor)
            shapeRenderer.setStrokeWidth(2f)

            val arrowSize = 12f
            val points = floatArrayOf(
                arrowSize, 0f,      // Tip
                -arrowSize * 0.5f, -arrowSize * 0.5f,  // Left
                -arrowSize * 0.5f, arrowSize * 0.5f    // Right
            )
            shapeRenderer.drawPolygon(canvas, arrowX, arrowY, points, arrowAngle)
        }
    }

    fun renderFleet(canvas: Canvas, fleetSystem: FleetSystem) {
        // Draw TB-26 independently — it may be flying out before fleet arrives
        // Uses same dart/triangle shape as drawCombatDrone for visual consistency
        if (fleetSystem.tb26Active) {
            val bx = fleetSystem.tb26Position.x
            val by = fleetSystem.tb26Position.y
            val bRot = fleetSystem.tb26Rotation
            val bSize = 15f  // Non-evolved drone size
            val chargeT = fleetSystem.tb26ChargeProgress
            val bColor = if (chargeT > 0f) {
                lerpColor(PassiveDefinitions.DRONE_COLOR_TB26, 0xFFFF6600.toInt(), chargeT)
            } else {
                PassiveDefinitions.DRONE_COLOR_TB26
            }

            val cosF = cos(bRot)
            val sinF = sin(bRot)

            // Thruster flame (behind drone)
            val flameLength = (8f + chargeT * 24f) + (kotlin.random.Random.nextFloat() * 4f)
            val backAngle = bRot + PI.toFloat()
            val flameBaseX = bx + cos(backAngle) * bSize * 0.5f
            val flameBaseY = by + sin(backAngle) * bSize * 0.5f
            val flameTipX = flameBaseX + cos(backAngle) * flameLength
            val flameTipY = flameBaseY + sin(backAngle) * flameLength

            shapeRenderer.setColor(0xFFFF6600.toInt())
            shapeRenderer.setStrokeWidth(4f)
            shapeRenderer.setAlpha(0.7f)
            shapeRenderer.drawLine(canvas, flameBaseX, flameBaseY, flameTipX, flameTipY)

            shapeRenderer.setColor(0xFFFFFF00.toInt())
            shapeRenderer.setStrokeWidth(2f)
            shapeRenderer.setAlpha(0.9f)
            val innerTipX = flameBaseX + cos(backAngle) * flameLength * 0.7f
            val innerTipY = flameBaseY + sin(backAngle) * flameLength * 0.7f
            shapeRenderer.drawLine(canvas, flameBaseX, flameBaseY, innerTipX, innerTipY)
            shapeRenderer.setAlpha(1f)

            // Dart triangle
            shapeRenderer.setColor(bColor)
            shapeRenderer.setStrokeWidth(2f)
            val tipX = bx + cosF * bSize
            val tipY = by + sinF * bSize
            val leftX = bx + cos(bRot + 2.4f) * bSize * 0.7f
            val leftY = by + sin(bRot + 2.4f) * bSize * 0.7f
            val rightX = bx + cos(bRot - 2.4f) * bSize * 0.7f
            val rightY = by + sin(bRot - 2.4f) * bSize * 0.7f
            shapeRenderer.drawLine(canvas, tipX, tipY, leftX, leftY)
            shapeRenderer.drawLine(canvas, leftX, leftY, rightX, rightY)
            shapeRenderer.drawLine(canvas, rightX, rightY, tipX, tipY)

            // Cockpit dot
            shapeRenderer.setColor(lightenColor(bColor))
            shapeRenderer.drawCircle(canvas, bx + cosF * 2f, by + sinF * 2f, 2f, true)

            // Glow
            shapeRenderer.setColor(bColor)
            shapeRenderer.setAlpha(0.3f)
            shapeRenderer.drawCircle(canvas, bx, by, bSize * 0.8f, true)
            shapeRenderer.setAlpha(1f)

            // Overdrive aura — pulsing rings when charging up or ramming (arrivalPhase 3)
            if (fleetSystem.tb26Charging || fleetSystem.arrivalPhase == 3) {
                val auraAlphaScale = if (fleetSystem.tb26Charging) fleetSystem.tb26ChargeProgress else 1f
                val t = (System.currentTimeMillis() % 1200L) / 1200f
                for (ring in 0 until 3) {
                    val phase = (t + ring / 3f) % 1f
                    val ringRadius = bSize * (0.8f + phase * 2.5f)
                    val ringAlpha = (1f - phase) * 0.7f
                    shapeRenderer.setColor(0xFFFFFFFF.toInt())
                    shapeRenderer.setAlpha(ringAlpha * auraAlphaScale)
                    shapeRenderer.drawCircle(canvas, bx, by, ringRadius, false)
                }
                shapeRenderer.setAlpha(1f)
            }
        }

        if (!fleetSystem.isActive) return

        val time = System.currentTimeMillis()

        for (fs in fleetSystem.fleetShips) {
            if (!fs.isActive) continue

            // Weapon visuals for close-range ships
            val saw = fs.sawWeapon
            if (saw != null && fleetSystem.arrivalPhase >= 1) {
                val positions = saw.getDiscPositions(fs.position.x, fs.position.y, fs.rotation)
                renderSawDiscs(canvas, positions, saw.discRadius, fs.color, fleetSystem.fleetWeaponAlpha)
            }
            ShipRenderer.drawShip(
                canvas = canvas,
                shapeRenderer = shapeRenderer,
                x = fs.position.x,
                y = fs.position.y,
                rotation = fs.rotation,
                size = GameConfig.SHIP_BASE_SIZE,
                shipColor = fs.color,
                pilotColor = fs.pilotColor,
                startingWeaponId = fs.weaponId,
                alpha = 1f
            )

            // Thruster — only show when ship is actually moving (hide after EMP scatter stops)
            val speedSq = fs.velocity.x * fs.velocity.x + fs.velocity.y * fs.velocity.y
            val isStoppedAfterScatter = fs.empDisorientTimer > 0f && speedSq < 2500f
            if (fleetSystem.arrivalPhase >= 1 && !isStoppedAfterScatter) {
                val fSize = GameConfig.SHIP_BASE_SIZE
                val thrustBackX = -fSize * 0.5f
                val thrustSize = fSize * 0.75f
                canvas.save()
                canvas.translate(fs.position.x, fs.position.y)
                canvas.rotate(Math.toDegrees(fs.rotation.toDouble()).toFloat())
                ThrusterDesigns.design01_current(canvas, thrustBackX, 0f, thrustSize, shapeRenderer)
                canvas.restore()
            }
        }
    }
}
