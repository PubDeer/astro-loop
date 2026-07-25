package com.astroloop.game.render

import android.graphics.Canvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ShipRenderer {

    fun drawShip(
        canvas: Canvas,
        shapeRenderer: ShapeRenderer,
        x: Float,
        y: Float,
        rotation: Float,
        size: Float,
        shipColor: Int,
        pilotColor: Int,
        startingWeaponId: String,
        alpha: Float = 1f
    ) {
        shapeRenderer.setColor(shipColor)
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.setStrokeWidth(2f)

        // Base ship shape
        val basePoints = floatArrayOf(
            size, 0f,
            -size * 0.7f, -size * 0.5f,
            -size * 0.4f, 0f,
            -size * 0.7f, size * 0.5f
        )
        // Fill hull black so background stars don't show through outlines
        shapeRenderer.setColor(0xFF000000.toInt())
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.drawPolygon(canvas, x, y, basePoints, rotation, filled = true)
        // Restore ship color for outline
        shapeRenderer.setColor(shipColor)
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.drawPolygon(canvas, x, y, basePoints, rotation)

        // Weapon-specific variations
        when (startingWeaponId) {
            "homing_missiles" -> {
                // Tracer: Missile pods on wingtips
                val podL = rotatePoint(-size * 0.5f, -size * 0.55f, rotation)
                val podR = rotatePoint(-size * 0.5f, size * 0.55f, rotation)
                shapeRenderer.drawCircle(canvas, x + podL.first, y + podL.second, size * 0.1f, false)
                shapeRenderer.drawCircle(canvas, x + podR.first, y + podR.second, size * 0.1f, false)
            }
            "scatter_shot" -> {
                // Shrapnel: Wide barrel
                val p1 = rotatePoint(size * 0.4f, -size * 0.25f, rotation)
                val p2 = rotatePoint(size * 0.4f, size * 0.25f, rotation)
                shapeRenderer.drawLine(canvas, x + p1.first, y + p1.second, x + p2.first, y + p2.second)
            }
            "ion_orbiters" -> {
                // Sentinel: Ring
                shapeRenderer.drawCircle(canvas, x, y, size * 0.25f, false)
            }
            "solar_storm" -> {
                // Tempest: Lightning
                val z1 = rotatePoint(size * 0.1f, -size * 0.15f, rotation)
                val z2 = rotatePoint(-size * 0.1f, size * 0.05f, rotation)
                val z3 = rotatePoint(size * 0.1f, size * 0.15f, rotation)
                shapeRenderer.drawLine(canvas, x + z1.first, y + z1.second, x + z2.first, y + z2.second)
                shapeRenderer.drawLine(canvas, x + z2.first, y + z2.second, x + z3.first, y + z3.second)
            }
            "space_mines" -> {
                // Trap: Hatches
                val h1 = rotatePoint(-size * 0.5f, -size * 0.25f, rotation)
                val h2 = rotatePoint(-size * 0.5f, size * 0.25f, rotation)
                shapeRenderer.drawCircle(canvas, x + h1.first, y + h1.second, size * 0.08f, false)
                shapeRenderer.drawCircle(canvas, x + h2.first, y + h2.second, size * 0.08f, false)
            }
            "flak_cannon" -> {
                // Devastator: Barrel
                val b1 = rotatePoint(size * 0.1f, 0f, rotation)
                val b2 = rotatePoint(-size * 0.3f, 0f, rotation)
                shapeRenderer.setStrokeWidth(4f)
                shapeRenderer.drawLine(canvas, x + b1.first, y + b1.second, x + b2.first, y + b2.second)
                shapeRenderer.setStrokeWidth(2f)
            }
            "needle_gun" -> {
                // Hedgehog: Spines
                shapeRenderer.setStrokeWidth(1f)
                for (i in -1..1) {
                    val s1 = rotatePoint(size * 0.2f, i * size * 0.15f, rotation)
                    val s2 = rotatePoint(-size * 0.2f, i * size * 0.15f, rotation)
                    shapeRenderer.drawLine(canvas, x + s1.first, y + s1.second, x + s2.first, y + s2.second)
                }
                shapeRenderer.setStrokeWidth(2f)
            }
            "nova_blast" -> {
                // Nova: Starburst
                shapeRenderer.setStrokeWidth(1f)
                for (i in 0 until 6) {
                    val angle = rotation + i * PI.toFloat() / 3f
                    val sx = x + cos(angle) * size * 0.15f
                    val sy = y + sin(angle) * size * 0.15f
                    val ex = x + cos(angle) * size * 0.3f
                    val ey = y + sin(angle) * size * 0.3f
                    shapeRenderer.drawLine(canvas, sx, sy, ex, ey)
                }
                shapeRenderer.setStrokeWidth(2f)
            }
            "cluster_bomb" -> {
                // Dreadnought: Launcher
                val nose = rotatePoint(size * 0.7f, 0f, rotation)
                shapeRenderer.drawCircle(canvas, x + nose.first, y + nose.second, size * 0.2f, false)
            }
            "energy_saw" -> {
                // Ripper: Sharp nose
                val tip = rotatePoint(size * 1.3f, 0f, rotation)
                val b1 = rotatePoint(size * 0.8f, -size * 0.1f, rotation)
                val b2 = rotatePoint(size * 0.8f, size * 0.1f, rotation)
                shapeRenderer.drawLine(canvas, x + b1.first, y + b1.second, x + tip.first, y + tip.second)
                shapeRenderer.drawLine(canvas, x + b2.first, y + b2.second, x + tip.first, y + tip.second)
            }
            "railgun" -> {
                // Specter: Rail
                val r1 = rotatePoint(size * 0.8f, 0f, rotation)
                val r2 = rotatePoint(-size * 0.5f, 0f, rotation)
                shapeRenderer.setStrokeWidth(1.5f)
                shapeRenderer.drawLine(canvas, x + r1.first, y + r1.second, x + r2.first, y + r2.second)
                shapeRenderer.setStrokeWidth(2f)
            }
        }

        // Pilot dot (drawn before cockpit so cockpit outline appears on top)
        val cockpitPos = rotatePoint(size * 0.3f, 0f, rotation)
        shapeRenderer.setColor(pilotColor)
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.drawCircle(canvas, x + cockpitPos.first, y + cockpitPos.second, size * 0.09f, true)

        // Cockpit outline
        shapeRenderer.setColor(shipColor)
        shapeRenderer.setAlpha(alpha)
        val cockpitPoints = floatArrayOf(
            size * 0.55f, 0f,
            size * 0.12f, -size * 0.16f,
            size * 0.12f, size * 0.16f
        )
        shapeRenderer.drawPolygon(canvas, x, y, cockpitPoints, rotation)

        shapeRenderer.setAlpha(1f)
    }

    fun drawShipDesign(
        canvas: Canvas,
        shapeRenderer: ShapeRenderer,
        designIndex: Int,
        x: Float, y: Float,
        size: Float,
        shipColor: Int,
        pilotColor: Int,
        startingWeaponId: String,
        rotation: Float = 0f,
        alpha: Float = 1f
    ) {
        if (designIndex == 0) {
            drawShip(canvas, shapeRenderer, x, y, rotation, size, shipColor, pilotColor, startingWeaponId, alpha)
            return
        }

        shapeRenderer.setAlpha(alpha)
        if (designIndex == 7) shapeRenderer.setStrokeWidth(4f) else shapeRenderer.setStrokeWidth(2f)

        val pts = when (designIndex) {
            // === Hull Variations (1–9): same base silhouette, adjusted proportions ===
            1 -> floatArrayOf(size, 0f, -size*.7f, -size*.7f, -size*.4f, 0f, -size*.7f, size*.7f)           // Wider wings
            2 -> floatArrayOf(size*1.4f, 0f, -size*.7f, -size*.5f, -size*.4f, 0f, -size*.7f, size*.5f)      // Extended nose
            3 -> floatArrayOf(size*.7f, 0f, -size*.5f, -size*.65f, -size*.2f, 0f, -size*.5f, size*.65f)     // Stubby
            4 -> floatArrayOf(size*1.2f, 0f, -size*.9f, -size*.35f, -size*.6f, 0f, -size*.9f, size*.35f)    // Elongated
            5 -> floatArrayOf(size, 0f, -size*.9f, -size*.45f, -size*.3f, 0f, -size*.9f, size*.45f)         // Swept wings
            6 -> floatArrayOf(size, 0f, -size*.7f, -size*.5f, 0f, 0f, -size*.7f, size*.5f)                  // Deep V notch
            7 -> floatArrayOf(size, 0f, -size*.7f, -size*.5f, -size*.4f, 0f, -size*.7f, size*.5f)           // Thick stroke (4f above)
            8 -> floatArrayOf(size, 0f, -size*.7f, -size*.7f, -size*.4f, 0f, -size*.5f, size*.35f)          // Asymmetric
            9 -> floatArrayOf(size, 0f, -size*.4f, -size*.5f, -size*.7f, -size*.2f, -size*.7f, size*.2f, -size*.4f, size*.5f)  // Pentagonal flat rear
            // === New Silhouettes (10–19) ===
            10 -> floatArrayOf(size*.8f, 0f, -size*.4f, -size*.9f, -size*.4f, size*.9f)                     // Delta wing
            11 -> floatArrayOf(size*.5f, -size*.4f, size*.5f, size*.4f, -size*.7f, size*.15f, -size*.7f, -size*.15f)   // Wedge
            12 -> floatArrayOf(size*1.2f, 0f, -size*.3f, -size*.18f, -size*.5f, 0f, -size*.3f, size*.18f)  // Slim cruiser
            13 -> floatArrayOf(size*.9f, 0f, size*.1f, -size*.6f, -size*.5f, -size*.1f, -size*.5f, size*.1f, size*.1f, size*.6f)  // Arrowhead with barbs
            14 -> floatArrayOf(size*.3f, -size*.7f, size*.3f, size*.7f, -size*.3f, size*.4f, -size*.7f, size*.2f, -size*.7f, -size*.2f, -size*.3f, -size*.4f)  // Hammerhead
            15 -> floatArrayOf(size*1.3f, 0f, size*.5f, -size*.1f, -size*.7f, -size*.25f, -size*.7f, size*.25f, size*.5f, size*.1f)  // Blade
            16 -> floatArrayOf(size*.8f, 0f, size*.5f, -size*.5f, 0f, -size*.65f, -size*.5f, -size*.5f, -size*.7f, 0f, -size*.5f, size*.5f, 0f, size*.65f, size*.5f, size*.5f)  // Disc/saucer
            17 -> floatArrayOf(size*.7f, 0f, size*.2f, -size*.8f, -size*.6f, -size*.4f, -size*.4f, 0f, -size*.6f, size*.4f, size*.2f, size*.8f)  // Crab
            18 -> floatArrayOf(size*.8f, 0f, size*.2f, -size*.2f, -size*.1f, -size*.7f, -size*.3f, -size*.3f, -size*.7f, -size*.2f, -size*.7f, size*.2f, -size*.3f, size*.3f, -size*.1f, size*.7f, size*.2f, size*.2f)  // Star fighter
            19 -> floatArrayOf(size*.9f, 0f, 0f, -size*.6f, -size*.5f, 0f, 0f, size*.6f)                   // Rhombus
            else -> floatArrayOf(size, 0f, -size*.7f, -size*.5f, -size*.4f, 0f, -size*.7f, size*.5f)
        }

        // Fill black then colored outline
        shapeRenderer.setColor(0xFF000000.toInt())
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.drawPolygon(canvas, x, y, pts, rotation, filled = true)
        shapeRenderer.setColor(shipColor)
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.drawPolygon(canvas, x, y, pts, rotation)

        // Pilot dot + cockpit (same as drawShip)
        val cockpitPos = rotatePoint(size * 0.3f, 0f, rotation)
        shapeRenderer.setColor(pilotColor)
        shapeRenderer.setAlpha(alpha)
        shapeRenderer.drawCircle(canvas, x + cockpitPos.first, y + cockpitPos.second, size * 0.09f, true)
        shapeRenderer.setColor(shipColor)
        shapeRenderer.setAlpha(alpha)
        val cockpitPts = floatArrayOf(size * 0.55f, 0f, size * 0.12f, -size * 0.16f, size * 0.12f, size * 0.16f)
        shapeRenderer.drawPolygon(canvas, x, y, cockpitPts, rotation)
        shapeRenderer.setStrokeWidth(2f)

        shapeRenderer.setAlpha(1f)
    }

    private fun rotatePoint(px: Float, py: Float, angle: Float): Pair<Float, Float> {
        val cos = cos(angle)
        val sin = sin(angle)
        return Pair(px * cos - py * sin, px * sin + py * cos)
    }
}
