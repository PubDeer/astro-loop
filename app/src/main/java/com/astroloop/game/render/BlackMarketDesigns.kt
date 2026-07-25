package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.sin

/**
 * Debug-only black market environment design variants. Each design draws the whole
 * vertical of the store room within `bounds`, transparent (so a starfield shows
 * through). The mini-slot anchor is drawn separately by the debug page so it lands on
 * the same spot in every design. Throwaway iteration fodder — not wired into the store.
 */
object BlackMarketDesigns {
    const val COUNT = 20

    val NAMES = arrayOf(
        "NEON NOIR", "CARGO BAY", "SMUGGLER DEN", "PIPE WORKS", "LANTERN BAZAAR",
        "RIVETED BULKHEAD", "HAZARD DOCK", "CURTAINED ROOM", "SCAFFOLD CATWALK", "VENDING ROW",
        "BACK ALLEY", "FUEL DEPOT", "BROKEN ARCADE", "STREET STALL", "VAULT DOOR",
        "GRAFFITI WALL", "HANGING WARES", "CONTAINER STACK", "NEON GRID", "ASH CHAPEL"
    )

    // --- shared paints ---
    private val stroke = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true }
    private val fill = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val glow = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val text = Paint().apply {
        isAntiAlias = true; textAlign = Paint.Align.CENTER; typeface = FontManager.getBold()
    }

    /** Reset the shared paints to a known baseline so each design starts clean,
     *  regardless of state (color/alpha/strokeWidth/textAlign) left by the previous
     *  design or by a helper like drawWalkway. */
    private fun resetPaints() {
        stroke.style = Paint.Style.STROKE; stroke.color = 0xFFFFFFFF.toInt(); stroke.strokeWidth = 1.5f; stroke.alpha = 255
        fill.style = Paint.Style.FILL; fill.color = 0xFFFFFFFF.toInt(); fill.alpha = 255
        glow.style = Paint.Style.FILL; glow.color = 0xFFFFFFFF.toInt(); glow.alpha = 255
        text.textAlign = Paint.Align.CENTER; text.color = 0xFFFFFFFF.toInt(); text.alpha = 255
    }

    // --- layout helpers ---
    private fun roomTop(b: RectF) = b.top + 26f
    private fun walkwayY(b: RectF) = b.top + b.height() * 0.60f
    private fun floorY(b: RectF) = b.bottom - 8f

    /** Steel walkway line shared by most designs. */
    private fun drawWalkway(canvas: Canvas, b: RectF, color: Int = 0xFF555566.toInt()) {
        stroke.color = color; stroke.strokeWidth = 2f
        canvas.drawLine(b.left, walkwayY(b), b.right, walkwayY(b), stroke)
    }

    /** Mini slot-machine representation, drawn at the fixed walkway-center spot. */
    fun drawMiniSlot(canvas: Canvas, b: RectF) {
        val cx = b.centerX()
        val baseY = walkwayY(b)
        val bodyW = 30f; val bodyH = 38f
        val body = RectF(cx - bodyW / 2f, baseY - bodyH, cx + bodyW / 2f, baseY)
        fill.color = 0xFF15151F.toInt(); canvas.drawRoundRect(body, 3f, 3f, fill)
        stroke.color = 0xFFDDCCFF.toInt(); stroke.strokeWidth = 1.5f
        canvas.drawRoundRect(body, 3f, 3f, stroke)
        // three reel windows
        val time = System.currentTimeMillis()
        for (r in 0 until 3) {
            val rx = body.left + 4f + r * 8f
            val reel = RectF(rx, body.top + 6f, rx + 6f, body.top + 16f)
            fill.color = 0xFF0A0A12.toInt(); canvas.drawRect(reel, fill)
            val blink = 0.5f + 0.5f * sin(time / 300.0 + r).toFloat()
            stroke.color = 0xFFCCAAFF.toInt(); stroke.alpha = (blink * 200).toInt()
            canvas.drawRect(reel, stroke); stroke.alpha = 255
        }
        // base/legs
        stroke.color = 0xFF444455.toInt()
        canvas.drawLine(body.left + 4f, baseY, body.left + 4f, baseY + 4f, stroke)
        canvas.drawLine(body.right - 4f, baseY, body.right - 4f, baseY + 4f, stroke)
    }

    fun render(canvas: Canvas, index: Int, bounds: RectF) {
        resetPaints()
        when (index) {
            0 -> design01_neonNoir(canvas, bounds)
            1 -> design02_cargoBay(canvas, bounds)
            2 -> design03_smugglerDen(canvas, bounds)
            3 -> design04_pipeWorks(canvas, bounds)
            4 -> design05_lanternBazaar(canvas, bounds)
            5 -> design06_rivetedBulkhead(canvas, bounds)
            6 -> design07_hazardDock(canvas, bounds)
            7 -> design08_curtainedRoom(canvas, bounds)
            8 -> design09_scaffoldCatwalk(canvas, bounds)
            9 -> design10_vendingRow(canvas, bounds)
            10 -> design11_backAlley(canvas, bounds)
            11 -> design12_fuelDepot(canvas, bounds)
            12 -> design13_brokenArcade(canvas, bounds)
            13 -> design14_streetStall(canvas, bounds)
            14 -> design15_vaultDoor(canvas, bounds)
            15 -> design16_graffitiWall(canvas, bounds)
            16 -> design17_hangingWares(canvas, bounds)
            17 -> design18_containerStack(canvas, bounds)
            18 -> design19_neonGrid(canvas, bounds)
            19 -> design20_ashChapel(canvas, bounds)
            else -> error("BlackMarketDesigns: no design for index $index")
        }
    }

    // ===== REFERENCE DESIGNS (style + coordinate conventions) =====

    private fun design01_neonNoir(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        drawWalkway(canvas, b)
        // vertical neon strips on left/right walls
        val flick = 0.6f + 0.4f * sin(time / 350.0).toFloat()
        stroke.strokeWidth = 2f
        stroke.color = 0xFFCC44CC.toInt(); stroke.alpha = (flick * 220).toInt()
        canvas.drawLine(b.left + 18f, roomTop(b), b.left + 18f, walkwayY(b), stroke)
        stroke.color = 0xFF44CCFF.toInt(); stroke.alpha = (flick * 220).toInt()
        canvas.drawLine(b.right - 18f, roomTop(b), b.right - 18f, walkwayY(b), stroke)
        stroke.alpha = 255
        // hanging bulb + faint cone
        val cx = b.centerX()
        stroke.color = 0xFF444444.toInt(); stroke.strokeWidth = 1f
        canvas.drawLine(cx, roomTop(b), cx, roomTop(b) + 16f, stroke)
        glow.color = 0xFFDDAA55.toInt(); glow.alpha = (flick * 200).toInt()
        canvas.drawCircle(cx, roomTop(b) + 20f, 3f, glow); glow.alpha = 255
        // crooked neon sign
        canvas.save(); canvas.rotate(-2f, cx, roomTop(b) + 38f)
        text.textSize = 16f; text.color = 0xFFCC4422.toInt(); text.alpha = (flick * 255).toInt()
        canvas.drawText("BLACK MARKET", cx, roomTop(b) + 42f, text); text.alpha = 255
        canvas.restore()
        // floor scuffs
        stroke.color = 0xFF252535.toInt(); stroke.strokeWidth = 2f
        canvas.drawLine(b.left + 30f, floorY(b), b.left + 60f, floorY(b) - 2f, stroke)
        canvas.drawLine(b.right - 70f, floorY(b) - 1f, b.right - 40f, floorY(b) - 3f, stroke)
    }

    private fun design02_cargoBay(canvas: Canvas, b: RectF) {
        drawWalkway(canvas, b)
        stroke.strokeWidth = 1.5f
        // stacked crates on the left
        val crateColor = 0xFF3A3A4A.toInt()
        fun crate(x: Float, y: Float, s: Float) {
            val r = RectF(x, y - s, x + s, y)
            fill.color = 0xFF1A1A28.toInt(); canvas.drawRect(r, fill)
            stroke.color = crateColor; canvas.drawRect(r, stroke)
            canvas.drawLine(r.left, r.top, r.right, r.bottom, stroke)
            canvas.drawLine(r.right, r.top, r.left, r.bottom, stroke)
        }
        crate(b.left + 12f, walkwayY(b), 40f)
        crate(b.left + 16f, walkwayY(b) - 40f, 30f)
        crate(b.left + 54f, walkwayY(b), 28f)
        // shelf rack on the right
        stroke.color = 0xFF3A3830.toInt()
        for (s in 0 until 3) {
            val sy = roomTop(b) + 20f + s * 22f
            canvas.drawLine(b.right - 90f, sy, b.right - 12f, sy, stroke)
        }
        // overhead beam + sign
        stroke.color = 0xFF444455.toInt(); stroke.strokeWidth = 2f
        canvas.drawLine(b.left, roomTop(b) + 6f, b.right, roomTop(b) + 6f, stroke)
        text.textSize = 15f; text.color = 0xFFCCAA66.toInt()
        canvas.drawText("BLACK MARKET", b.centerX(), roomTop(b) + 24f, text)
    }

    // ===== designs 03..10 =====

    private fun design03_smugglerDen(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        // draped tarp — triangular fill over right side
        val tarp = Path().apply {
            moveTo(b.centerX() + 10f, roomTop(b))
            lineTo(b.right, roomTop(b))
            lineTo(b.right, walkwayY(b))
            close()
        }
        fill.color = 0xFF2A2820.toInt(); fill.alpha = 180; canvas.drawPath(tarp, fill); fill.alpha = 255
        stroke.color = 0xFF3A3830.toInt(); stroke.strokeWidth = 1.5f; canvas.drawPath(tarp, stroke)
        // two faint shelf lines on the left
        stroke.color = 0xFF333340.toInt(); stroke.strokeWidth = 1f
        canvas.drawLine(b.left + 8f, roomTop(b) + 30f, b.centerX() - 10f, roomTop(b) + 30f, stroke)
        canvas.drawLine(b.left + 8f, roomTop(b) + 58f, b.centerX() - 10f, roomTop(b) + 58f, stroke)
        // contraband crate bottom-left with pulsing amber glow
        val cx = b.left + 38f; val cy = walkwayY(b) - 20f; val cs = 28f
        val crate = RectF(cx - cs / 2f, cy - cs / 2f, cx + cs / 2f, cy + cs / 2f)
        val pulse = 0.4f + 0.6f * sin(time / 600.0).toFloat()
        glow.color = 0xFFCC8844.toInt(); glow.alpha = (pulse * 120).toInt()
        canvas.drawRect(RectF(crate.left + 3f, crate.top + 3f, crate.right - 3f, crate.bottom - 3f), glow)
        glow.alpha = 255
        stroke.color = 0xFF555544.toInt(); stroke.strokeWidth = 1.5f; canvas.drawRect(crate, stroke)
        // stencil label near roomTop
        text.textSize = 14f; text.color = 0xFFCCCCCC.toInt(); text.alpha = 160
        canvas.drawText("BLACK MARKET", b.centerX(), roomTop(b) + 14f, text); text.alpha = 255
        drawWalkway(canvas, b)
    }

    private fun design04_pipeWorks(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        drawWalkway(canvas, b)
        // 3 horizontal pipes across upper wall with elbow bends
        val pipeColor = 0xFF3A4452.toInt()
        stroke.color = pipeColor; stroke.strokeWidth = 4f
        val pipeYs = floatArrayOf(roomTop(b) + 18f, roomTop(b) + 44f, roomTop(b) + 70f)
        val elbowXs = floatArrayOf(b.left + 70f, b.right - 55f, b.left + 110f)
        val dropLens = floatArrayOf(28f, 32f, 24f)
        for (i in pipeYs.indices) {
            canvas.drawLine(b.left, pipeYs[i], b.right, pipeYs[i], stroke)
            // elbow drop
            canvas.drawLine(elbowXs[i], pipeYs[i], elbowXs[i], pipeYs[i] + dropLens[i], stroke)
        }
        // valve wheel top-right: circle + 4 spokes
        stroke.color = 0xFF556677.toInt(); stroke.strokeWidth = 1.5f
        val vx = b.right - 22f; val vy = roomTop(b) + 18f
        canvas.drawCircle(vx, vy, 10f, stroke)
        for (a in 0 until 4) {
            val rad = Math.PI / 2.0 * a
            canvas.drawLine(vx, vy, vx + (10f * sin(rad)).toFloat(), vy + (10f * sin(rad + Math.PI / 2.0)).toFloat(), stroke)
        }
        // steam puffs near elbow joint (low-alpha white circles)
        val sx = elbowXs[0]; val sy = pipeYs[0] + dropLens[0]
        val steamAlpha = 0.3f + 0.3f * sin(time / 400.0).toFloat()
        glow.color = 0xFFFFFFFF.toInt()
        for (r in 0 until 3) {
            glow.alpha = ((steamAlpha * 80) / (r + 1)).toInt()
            canvas.drawCircle(sx + r * 4f, sy - r * 5f, 4f + r * 2f, glow)
        }
        glow.alpha = 255
    }

    private fun design05_lanternBazaar(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        // sagging wire across the top — quadratic bezier dipping in middle
        val wireY = roomTop(b) + 8f; val dipY = roomTop(b) + 22f
        val wirePath = Path().apply {
            moveTo(b.left, wireY)
            quadTo(b.centerX(), dipY, b.right, wireY)
        }
        stroke.color = 0xFF444444.toInt(); stroke.strokeWidth = 1.5f; canvas.drawPath(wirePath, stroke)
        // cloth awning suggestion — two diagonal lines
        stroke.color = 0xFF3A3840.toInt(); stroke.strokeWidth = 1.5f
        canvas.drawLine(b.left + 20f, roomTop(b) + 30f, b.left + 80f, roomTop(b) + 52f, stroke)
        canvas.drawLine(b.right - 20f, roomTop(b) + 30f, b.right - 80f, roomTop(b) + 52f, stroke)
        // 4 hanging lanterns along the wire
        val lanternXs = floatArrayOf(b.left + 40f, b.left + 110f, b.right - 110f, b.right - 40f)
        for (i in lanternXs.indices) {
            val lx = lanternXs[i]
            // interpolate y on the wire quadratic (approx)
            val t = (lx - b.left) / b.width()
            val ly = wireY * (1 - t) * (1 - t) + dipY * 2f * t * (1 - t) + wireY * t * t
            stroke.color = 0xFF555544.toInt(); stroke.strokeWidth = 1f
            canvas.drawLine(lx, ly, lx, ly + 10f, stroke)
            val pulse = 0.5f + 0.5f * sin(time / 300.0 + i).toFloat()
            glow.color = 0xFFDDAA55.toInt(); glow.alpha = (pulse * 200).toInt()
            canvas.drawCircle(lx, ly + 14f, 5f, glow)
            stroke.color = 0xFF887744.toInt(); canvas.drawCircle(lx, ly + 14f, 5f, stroke)
        }
        glow.alpha = 255
        drawWalkway(canvas, b)
    }

    private fun design06_rivetedBulkhead(canvas: Canvas, b: RectF) {
        // grid of rivet dots
        val cols = 6; val rows = 3
        val wallTop = roomTop(b); val wallBot = walkwayY(b) - 8f
        val colStep = b.width() / (cols + 1)
        val rowStep = (wallBot - wallTop) / (rows + 1)
        fill.color = 0xFF3A3A4A.toInt()
        for (col in 1..cols) {
            for (row in 1..rows) {
                val rx = b.left + col * colStep; val ry = wallTop + row * rowStep
                canvas.drawCircle(rx, ry, 2.5f, fill)
            }
        }
        // central heavy hatch — rounded rect outline + cross handle
        val cx = b.centerX(); val hatchMidY = (wallTop + wallBot) / 2f
        val hatch = RectF(cx - 32f, hatchMidY - 40f, cx + 32f, hatchMidY + 40f)
        stroke.color = 0xFF555566.toInt(); stroke.strokeWidth = 2f
        canvas.drawRoundRect(hatch, 5f, 5f, stroke)
        stroke.strokeWidth = 1.5f
        canvas.drawLine(cx - 14f, hatchMidY, cx + 14f, hatchMidY, stroke)
        canvas.drawLine(cx, hatchMidY - 14f, cx, hatchMidY + 14f, stroke)
        // engraved label
        text.textSize = 13f; text.color = 0xFF666677.toInt()
        canvas.drawText("BLACK MARKET", cx, wallTop + 14f, text)
        drawWalkway(canvas, b)
    }

    private fun design07_hazardDock(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        drawWalkway(canvas, b)
        // yellow/black hazard band just above walkway — diagonal hatch lines
        val bandTop = walkwayY(b) - 18f; val bandBot = walkwayY(b) - 2f
        stroke.color = 0xFFCCAA22.toInt(); stroke.strokeWidth = 2f
        var hx = b.left - 20f
        while (hx < b.right + 20f) {
            canvas.drawLine(hx, bandTop, hx + 18f, bandBot, stroke)
            hx += 14f
        }
        // caution chevron pair center-low (two > shapes)
        val chevX = b.centerX(); val chevY = walkwayY(b) - 36f
        stroke.color = 0xFFCCAA22.toInt(); stroke.strokeWidth = 2f
        for (offset in floatArrayOf(-18f, 18f)) {
            canvas.drawLine(chevX + offset, chevY - 12f, chevX + offset + 10f, chevY, stroke)
            canvas.drawLine(chevX + offset + 10f, chevY, chevX + offset, chevY + 12f, stroke)
        }
        // hanging lamp that sways
        val sway = 12f * sin(time / 800.0).toFloat()
        val lampBaseX = b.centerX() + sway; val lampTopY = roomTop(b) + 8f
        stroke.color = 0xFF444444.toInt(); stroke.strokeWidth = 1f
        canvas.drawLine(b.centerX(), lampTopY, lampBaseX, lampTopY + 30f, stroke)
        glow.color = 0xFFDDCC88.toInt(); glow.alpha = 200; canvas.drawCircle(lampBaseX, lampTopY + 34f, 4f, glow); glow.alpha = 255
        // cool steel wall lines
        stroke.color = 0xFF3A3A4A.toInt(); stroke.strokeWidth = 1f
        canvas.drawLine(b.left, roomTop(b) + 8f, b.right, roomTop(b) + 8f, stroke)
        canvas.drawLine(b.left, roomTop(b) + 28f, b.right, roomTop(b) + 28f, stroke)
    }

    private fun design08_curtainedRoom(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        // two heavy curtains — left and right thirds, series of close vertical strokes
        val curtainColor = 0xFF2A2230.toInt()
        val curtainW = b.width() * 0.30f
        stroke.strokeWidth = 3f
        // left curtain
        var cx2 = b.left
        while (cx2 < b.left + curtainW) {
            val sway = 2f * sin(time / 1200.0 + cx2 * 0.05).toFloat()
            stroke.color = curtainColor
            stroke.alpha = (180 + (40 * sin(time / 900.0 + cx2 * 0.1)).toInt()).coerceIn(140, 220)
            canvas.drawLine(cx2 + sway, roomTop(b), cx2 - sway, walkwayY(b) - 10f, stroke)
            // scalloped bottom: tiny arc suggestion via short diagonal
            stroke.alpha = 120
            canvas.drawLine(cx2, walkwayY(b) - 10f, cx2 + 4f, walkwayY(b), stroke)
            cx2 += 4f
        }
        // right curtain
        cx2 = b.right - curtainW
        while (cx2 < b.right) {
            val sway = 2f * sin(time / 1200.0 + cx2 * 0.05).toFloat()
            stroke.color = curtainColor
            stroke.alpha = (180 + (40 * sin(time / 900.0 + cx2 * 0.1)).toInt()).coerceIn(140, 220)
            canvas.drawLine(cx2 + sway, roomTop(b), cx2 - sway, walkwayY(b) - 10f, stroke)
            stroke.alpha = 120
            canvas.drawLine(cx2, walkwayY(b) - 10f, cx2 + 4f, walkwayY(b), stroke)
            cx2 += 4f
        }
        stroke.alpha = 255
        // dim red sign glow above the center gap
        val flick = 0.6f + 0.4f * sin(time / 450.0).toFloat()
        glow.color = 0xFFCC4422.toInt(); glow.alpha = (flick * 160).toInt()
        canvas.drawCircle(b.centerX(), roomTop(b) + 22f, 28f, glow); glow.alpha = 255
        text.textSize = 13f; text.color = 0xFFCC4422.toInt(); text.alpha = (flick * 220).toInt()
        canvas.drawText("BLACK MARKET", b.centerX(), roomTop(b) + 26f, text); text.alpha = 255
        drawWalkway(canvas, b)
    }

    private fun design09_scaffoldCatwalk(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        // upper catwalk rail
        val catY = roomTop(b) + 30f
        stroke.color = 0xFF444455.toInt(); stroke.strokeWidth = 2f
        canvas.drawLine(b.left, catY, b.right, catY, stroke)
        // 3 vertical support posts with X cross-braces
        val postXs = floatArrayOf(b.left + 40f, b.centerX(), b.right - 40f)
        stroke.strokeWidth = 1.5f
        for (px in postXs) {
            canvas.drawLine(px, catY, px, walkwayY(b), stroke)
        }
        // X braces between posts
        for (i in 0 until postXs.size - 1) {
            val x1 = postXs[i]; val x2 = postXs[i + 1]
            canvas.drawLine(x1, catY + 8f, x2, walkwayY(b) - 8f, stroke)
            canvas.drawLine(x2, catY + 8f, x1, walkwayY(b) - 8f, stroke)
        }
        // ladder on the left: two rails + rungs
        val ladderX = b.left + 14f
        stroke.color = 0xFF3A3A4A.toInt(); stroke.strokeWidth = 1.5f
        canvas.drawLine(ladderX - 5f, catY, ladderX - 5f, walkwayY(b), stroke)
        canvas.drawLine(ladderX + 5f, catY, ladderX + 5f, walkwayY(b), stroke)
        val rungStep = (walkwayY(b) - catY) / 5f
        for (r in 0..4) {
            val ry = catY + r * rungStep
            canvas.drawLine(ladderX - 5f, ry, ladderX + 5f, ry, stroke)
        }
        // small clip-light on catwalk rail
        val flick = 0.7f + 0.3f * sin(time / 280.0).toFloat()
        glow.color = 0xFFAADDFF.toInt(); glow.alpha = (flick * 180).toInt()
        canvas.drawCircle(postXs[1] + 16f, catY - 5f, 4f, glow); glow.alpha = 255
        drawWalkway(canvas, b)
    }

    private fun design10_vendingRow(canvas: Canvas, b: RectF) {
        // three vendor booths along back wall
        val boothW = b.width() * 0.24f; val boothH = 52f
        val boothTop = roomTop(b) + 16f; val boothBot = boothTop + boothH
        val boothCxs = floatArrayOf(b.left + b.width() * 0.18f, b.centerX(), b.right - b.width() * 0.18f)
        val boothColors = intArrayOf(0xFF44CCFF.toInt(), 0xFFDDAA55.toInt(), 0xFFCC44CC.toInt())
        for (i in boothCxs.indices) {
            val bx = boothCxs[i]
            // skip drawing the center booth opaque body (keeps mini-slot area clearer)
            val booth = RectF(bx - boothW / 2f, boothTop, bx + boothW / 2f, boothBot)
            if (i != 1) {
                fill.color = 0xFF1A1A28.toInt(); canvas.drawRoundRect(booth, 4f, 4f, fill)
            }
            stroke.color = 0xFF33333F.toInt(); stroke.strokeWidth = 1.5f
            canvas.drawRoundRect(booth, 4f, 4f, stroke)
            // slanted awning line on top
            stroke.color = 0xFF555566.toInt()
            canvas.drawLine(booth.left - 4f, boothTop - 6f, booth.right + 4f, boothTop, stroke)
            // colored product dot inside
            glow.color = boothColors[i]; glow.alpha = 200
            canvas.drawCircle(bx, boothTop + boothH * 0.45f, 6f, glow); glow.alpha = 255
        }
        // hanging price-tag rectangles
        stroke.color = 0xFF444444.toInt(); stroke.strokeWidth = 1f
        for (tagX in floatArrayOf(b.left + 55f, b.right - 55f)) {
            canvas.drawLine(tagX, roomTop(b) + 4f, tagX, roomTop(b) + 12f, stroke)
            stroke.color = 0xFF555555.toInt()
            canvas.drawRect(tagX - 8f, roomTop(b) + 12f, tagX + 8f, roomTop(b) + 24f, stroke)
            stroke.color = 0xFF444444.toInt()
        }
        drawWalkway(canvas, b)
    }
    private fun design11_backAlley(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        // brick suggestion on left wall: offset rows of short horizontal segments
        stroke.color = 0xFF3A3340.toInt(); stroke.strokeWidth = 2f
        val brickRowH = 10f; val brickW = 18f; val wallRight = b.left + 60f
        var row = 0
        var ry = roomTop(b) + 4f
        while (ry < walkwayY(b) - 4f) {
            val offsetX = if (row % 2 == 0) 0f else brickW * 0.5f
            var bx = b.left + offsetX
            while (bx + brickW < wallRight) {
                canvas.drawLine(bx, ry, bx + brickW - 2f, ry, stroke)
                bx += brickW
            }
            ry += brickRowH; row++
        }
        // fire-escape zigzag upper-right
        val escPath = Path().apply {
            moveTo(b.right - 20f, roomTop(b) + 4f)
            lineTo(b.right - 45f, roomTop(b) + 28f)
            lineTo(b.right - 20f, roomTop(b) + 28f)
            lineTo(b.right - 45f, roomTop(b) + 56f)
        }
        stroke.color = 0xFF444444.toInt(); stroke.strokeWidth = 1.5f; canvas.drawPath(escPath, stroke)
        // platform lines
        canvas.drawLine(b.right - 50f, roomTop(b) + 28f, b.right - 12f, roomTop(b) + 28f, stroke)
        canvas.drawLine(b.right - 50f, roomTop(b) + 56f, b.right - 12f, roomTop(b) + 56f, stroke)
        // puddle on the floor — flattened ellipse
        val puddleX = b.left + 90f; val puddleY = floorY(b) - 3f
        stroke.color = 0xFF334455.toInt(); stroke.strokeWidth = 1f
        canvas.drawOval(RectF(puddleX - 28f, puddleY - 5f, puddleX + 28f, puddleY + 5f), stroke)
        // neon streak reflected in puddle
        val flick = 0.5f + 0.5f * sin(time / 380.0).toFloat()
        fill.color = 0xFFCC4466.toInt(); fill.alpha = (flick * 80).toInt()
        canvas.drawOval(RectF(puddleX - 12f, puddleY - 3f, puddleX + 12f, puddleY + 3f), fill)
        fill.alpha = 255
        // crooked "BLACK MARKET" sign near roomTop
        canvas.save(); canvas.rotate(-3f, b.centerX(), roomTop(b) + 14f)
        text.textSize = 15f; text.color = 0xFFCC4422.toInt()
        canvas.drawText("BLACK MARKET", b.centerX(), roomTop(b) + 18f, text)
        canvas.restore()
        drawWalkway(canvas, b)
    }

    private fun design12_fuelDepot(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        drawWalkway(canvas, b)
        // two upright fuel drums: left and right
        val drumW = 32f; val drumH = 54f
        val drumCxs = floatArrayOf(b.left + 44f, b.right - 44f)
        for (dx in drumCxs) {
            val drumRect = RectF(dx - drumW / 2f, walkwayY(b) - drumH, dx + drumW / 2f, walkwayY(b))
            fill.color = 0xFF2A2A35.toInt(); canvas.drawRoundRect(drumRect, 6f, 6f, fill)
            stroke.color = 0xFF444455.toInt(); stroke.strokeWidth = 1.5f
            canvas.drawRoundRect(drumRect, 6f, 6f, stroke)
            // top ellipse
            canvas.drawOval(RectF(drumRect.left, drumRect.top - 5f, drumRect.right, drumRect.top + 5f), stroke)
            // two horizontal band lines
            val b1Y = drumRect.top + drumH * 0.30f; val b2Y = drumRect.top + drumH * 0.65f
            canvas.drawLine(drumRect.left, b1Y, drumRect.right, b1Y, stroke)
            canvas.drawLine(drumRect.left, b2Y, drumRect.right, b2Y, stroke)
        }
        // FLAMMABLE warning triangle (center-upper)
        val tx = b.centerX(); val ty = roomTop(b) + 40f; val ts = 22f
        stroke.color = 0xFFCCAA22.toInt(); stroke.strokeWidth = 1.5f
        canvas.drawLine(tx, ty - ts, tx - ts * 0.87f, ty + ts * 0.5f, stroke)
        canvas.drawLine(tx - ts * 0.87f, ty + ts * 0.5f, tx + ts * 0.87f, ty + ts * 0.5f, stroke)
        canvas.drawLine(tx + ts * 0.87f, ty + ts * 0.5f, tx, ty - ts, stroke)
        text.textSize = 13f; text.color = 0xFFCCAA22.toInt()
        canvas.drawText("!", tx, ty + 8f, text)
        // hose loop between drums (quadratic path)
        val hosePath = Path().apply {
            moveTo(drumCxs[0] + drumW / 2f, walkwayY(b) - 20f)
            quadTo(b.centerX(), walkwayY(b) + 14f, drumCxs[1] - drumW / 2f, walkwayY(b) - 20f)
        }
        stroke.color = 0xFF334433.toInt(); stroke.strokeWidth = 3f; canvas.drawPath(hosePath, stroke)
        // amber hazard glow above walkway
        val pulse = 0.4f + 0.6f * sin(time / 700.0).toFloat()
        glow.color = 0xFFCC8844.toInt(); glow.alpha = (pulse * 60).toInt()
        canvas.drawRect(b.left, walkwayY(b) - drumH - 8f, b.right, walkwayY(b), glow); glow.alpha = 255
        // label
        text.textSize = 13f; text.color = 0xFFCCCCCC.toInt(); text.alpha = 150
        canvas.drawText("BLACK MARKET", b.centerX(), roomTop(b) + 14f, text); text.alpha = 255
    }

    private fun design13_brokenArcade(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        // dead arcade cabinet on the left
        val cabW = 50f; val cabH = 80f; val cabX = b.left + 14f; val cabY = walkwayY(b)
        val cab = RectF(cabX, cabY - cabH, cabX + cabW, cabY)
        fill.color = 0xFF1A1820.toInt(); canvas.drawRoundRect(cab, 4f, 4f, fill)
        stroke.color = 0xFF3A3A4A.toInt(); stroke.strokeWidth = 1.5f
        canvas.drawRoundRect(cab, 4f, 4f, stroke)
        // CRT screen on the cabinet
        val scrW = 34f; val scrH = 24f
        val scr = RectF(cabX + 8f, cab.top + 12f, cabX + 8f + scrW, cab.top + 12f + scrH)
        fill.color = 0xFF0A0A10.toInt(); canvas.drawRect(scr, fill)
        // scanlines with blink alpha
        val blink = 0.4f + 0.6f * sin(time / 220.0).toFloat()
        stroke.color = 0xFF334433.toInt(); stroke.strokeWidth = 1f; stroke.alpha = (blink * 180).toInt()
        var sl = scr.top + 3f
        while (sl < scr.bottom) { canvas.drawLine(scr.left + 2f, sl, scr.right - 2f, sl, stroke); sl += 4f }
        stroke.alpha = 255
        // "MA  KET" broken neon sign — two drawText calls with gap for dead glyphs
        val neonFlick = 0.3f + 0.7f * sin(time / 160.0).toFloat()
        text.textSize = 14f; text.color = 0xFFCC44CC.toInt(); text.alpha = (neonFlick * 230).toInt()
        val signY = roomTop(b) + 18f
        text.textAlign = Paint.Align.RIGHT
        canvas.drawText("MA", b.centerX() - 4f, signY, text)
        text.textAlign = Paint.Align.LEFT
        canvas.drawText("KET", b.centerX() + 18f, signY, text)
        text.textAlign = Paint.Align.CENTER; text.alpha = 255
        // dead glyph underline suggestion
        stroke.color = 0xFF441144.toInt(); stroke.strokeWidth = 1f; stroke.alpha = 80
        canvas.drawLine(b.centerX() - 4f, signY + 2f, b.centerX() + 16f, signY + 2f, stroke); stroke.alpha = 255
        drawWalkway(canvas, b)
    }

    private fun design14_streetStall(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        drawWalkway(canvas, b)
        // market table slab + 2 legs
        val tableY = walkwayY(b) - 16f; val tableLeft = b.left + 28f; val tableRight = b.right - 28f
        stroke.color = 0xFF5A4A38.toInt(); stroke.strokeWidth = 2.5f
        canvas.drawLine(tableLeft, tableY, tableRight, tableY, stroke)
        stroke.strokeWidth = 1.5f
        canvas.drawLine(tableLeft + 10f, tableY, tableLeft + 10f, walkwayY(b), stroke)
        canvas.drawLine(tableRight - 10f, tableY, tableRight - 10f, walkwayY(b), stroke)
        // slanted tarp roof (two diagonals meeting a ridge)
        val ridgeX = b.centerX(); val ridgeY = roomTop(b) + 30f
        stroke.color = 0xFF3A3020.toInt(); stroke.strokeWidth = 2f
        canvas.drawLine(tableLeft - 6f, roomTop(b) + 60f, ridgeX, ridgeY, stroke)
        canvas.drawLine(tableRight + 6f, roomTop(b) + 60f, ridgeX, ridgeY, stroke)
        canvas.drawLine(tableLeft - 6f, roomTop(b) + 60f, tableRight + 6f, roomTop(b) + 60f, stroke)
        // goods row on the table: triangle, circle, square
        val goodsY = tableY - 8f; val spacing = (tableRight - tableLeft) / 4f
        stroke.color = 0xFF887766.toInt(); stroke.strokeWidth = 1.2f
        // triangle
        val gx1 = tableLeft + spacing
        canvas.drawLine(gx1 - 6f, goodsY + 8f, gx1, goodsY - 2f, stroke)
        canvas.drawLine(gx1, goodsY - 2f, gx1 + 6f, goodsY + 8f, stroke)
        canvas.drawLine(gx1 - 6f, goodsY + 8f, gx1 + 6f, goodsY + 8f, stroke)
        // circle
        val gx2 = tableLeft + spacing * 2f
        canvas.drawCircle(gx2, goodsY + 4f, 6f, stroke)
        // square
        val gx3 = tableLeft + spacing * 3f
        canvas.drawRect(gx3 - 6f, goodsY - 2f, gx3 + 6f, goodsY + 10f, stroke)
        // hanging bulb above with warm glow
        val pulse = 0.6f + 0.4f * sin(time / 500.0).toFloat()
        stroke.color = 0xFF555544.toInt(); stroke.strokeWidth = 1f
        canvas.drawLine(ridgeX, ridgeY, ridgeX, ridgeY + 18f, stroke)
        glow.color = 0xFFDDBB66.toInt(); glow.alpha = (pulse * 200).toInt()
        canvas.drawCircle(ridgeX, ridgeY + 22f, 5f, glow); glow.alpha = 255
        text.textSize = 13f; text.color = 0xFFBBAA88.toInt(); text.alpha = 180
        canvas.drawText("BLACK MARKET", b.centerX(), roomTop(b) + 18f, text); text.alpha = 255
    }

    private fun design15_vaultDoor(canvas: Canvas, b: RectF) {
        val cx = b.centerX()
        val doorCY = roomTop(b) + (walkwayY(b) - roomTop(b)) * 0.50f
        val outerR = 46f; val innerR = 34f
        // back wall panel behind door
        stroke.color = 0xFF333344.toInt(); stroke.strokeWidth = 1f
        canvas.drawRect(cx - outerR - 8f, roomTop(b) + 8f, cx + outerR + 8f, walkwayY(b) - 8f, stroke)
        // concentric circles
        stroke.color = 0xFF3A3A4A.toInt(); stroke.strokeWidth = 3f
        canvas.drawCircle(cx, doorCY, outerR, stroke)
        stroke.strokeWidth = 2f; canvas.drawCircle(cx, doorCY, outerR - 8f, stroke)
        // thin lavender inner ring
        stroke.color = 0xFFCCAAFF.toInt(); stroke.strokeWidth = 1f
        canvas.drawCircle(cx, doorCY, innerR, stroke)
        // radial bolt dots around the rim
        fill.color = 0xFF555566.toInt()
        val boltCount = 8
        for (i in 0 until boltCount) {
            val ang = Math.PI * 2.0 / boltCount * i
            val bx = cx + (outerR - 5f) * sin(ang).toFloat()
            val by = doorCY - (outerR - 5f) * kotlin.math.cos(ang).toFloat()
            canvas.drawCircle(bx, by, 3f, fill)
        }
        // spoke handle lines from center
        stroke.color = 0xFF555566.toInt(); stroke.strokeWidth = 2f
        for (i in 0 until 4) {
            val ang = Math.PI / 4.0 * i
            canvas.drawLine(
                cx + (innerR - 10f) * sin(ang).toFloat(), doorCY - (innerR - 10f) * kotlin.math.cos(ang).toFloat(),
                cx + (innerR - 2f) * sin(ang).toFloat(), doorCY - (innerR - 2f) * kotlin.math.cos(ang).toFloat(), stroke)
            canvas.drawLine(
                cx - (innerR - 10f) * sin(ang).toFloat(), doorCY + (innerR - 10f) * kotlin.math.cos(ang).toFloat(),
                cx - (innerR - 2f) * sin(ang).toFloat(), doorCY + (innerR - 2f) * kotlin.math.cos(ang).toFloat(), stroke)
        }
        // keypad rectangle beside the door
        val kpX = cx + outerR + 16f; val kpY = doorCY - 14f
        fill.color = 0xFF222230.toInt(); canvas.drawRect(kpX, kpY, kpX + 16f, kpY + 28f, fill)
        stroke.color = 0xFF555566.toInt(); stroke.strokeWidth = 1f
        canvas.drawRect(kpX, kpY, kpX + 16f, kpY + 28f, stroke)
        drawWalkway(canvas, b)
    }

    private fun design16_graffitiWall(canvas: Canvas, b: RectF) {
        // spray tag scribble 1: red-pink loop left
        val tag1 = Path().apply {
            moveTo(b.left + 20f, roomTop(b) + 40f)
            lineTo(b.left + 50f, roomTop(b) + 20f)
            lineTo(b.left + 75f, roomTop(b) + 55f)
            lineTo(b.left + 40f, roomTop(b) + 65f)
            lineTo(b.left + 30f, roomTop(b) + 35f)
        }
        stroke.color = 0xFFCC4466.toInt(); stroke.strokeWidth = 3f; stroke.alpha = 120
        canvas.drawPath(tag1, stroke)
        // spray tag 2: teal loop center-right
        val tag2 = Path().apply {
            moveTo(b.centerX() - 10f, roomTop(b) + 25f)
            lineTo(b.centerX() + 40f, roomTop(b) + 15f)
            lineTo(b.centerX() + 60f, roomTop(b) + 55f)
            lineTo(b.centerX() + 10f, roomTop(b) + 60f)
        }
        stroke.color = 0xFF44CCAA.toInt(); stroke.alpha = 170; canvas.drawPath(tag2, stroke)
        // spray tag 3: gold accent right
        val tag3 = Path().apply {
            moveTo(b.right - 80f, roomTop(b) + 30f)
            lineTo(b.right - 30f, roomTop(b) + 20f)
            lineTo(b.right - 20f, roomTop(b) + 60f)
        }
        stroke.color = 0xFFCCAA22.toInt(); stroke.alpha = 200; canvas.drawPath(tag3, stroke)
        stroke.alpha = 255
        // stencil star center-top
        val sx = b.centerX(); val sY = roomTop(b) + 12f; val sr = 10f
        stroke.color = 0xFFCCCCCC.toInt(); stroke.strokeWidth = 1.5f; stroke.alpha = 160
        for (i in 0 until 5) {
            val a1 = Math.PI * 2.0 / 5 * i - Math.PI / 2
            val a2 = a1 + Math.PI / 5
            canvas.drawLine(
                (sx + sr * kotlin.math.cos(a1)).toFloat(), (sY + sr * kotlin.math.sin(a1)).toFloat(),
                (sx + sr * 0.4f * kotlin.math.cos(a2)).toFloat(), (sY + sr * 0.4f * kotlin.math.sin(a2)).toFloat(), stroke)
            val a3 = a2
            val a4 = a3 + Math.PI / 5
            canvas.drawLine(
                (sx + sr * 0.4f * kotlin.math.cos(a3)).toFloat(), (sY + sr * 0.4f * kotlin.math.sin(a3)).toFloat(),
                (sx + sr * kotlin.math.cos(a4)).toFloat(), (sY + sr * kotlin.math.sin(a4)).toFloat(), stroke)
        }
        stroke.alpha = 255
        // drip lines under the tags
        stroke.color = 0xFFCC4466.toInt(); stroke.strokeWidth = 1.5f; stroke.alpha = 100
        for (drip in floatArrayOf(b.left + 35f, b.left + 60f, b.centerX() + 20f, b.right - 40f)) {
            val dripLen = 8f + (drip % 10f)
            canvas.drawLine(drip, roomTop(b) + 65f, drip, roomTop(b) + 65f + dripLen, stroke)
        }
        stroke.alpha = 255
        drawWalkway(canvas, b)
    }

    private fun design17_hangingWares(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        // upper rail line
        val railY = roomTop(b) + 14f
        stroke.color = 0xFF555566.toInt(); stroke.strokeWidth = 2f
        canvas.drawLine(b.left + 8f, railY, b.right - 8f, railY, stroke)
        // hook + ware positions
        val hooksX = floatArrayOf(b.left + 30f, b.left + 70f, b.left + 110f, b.right - 50f)
        val wareTypes = intArrayOf(0, 1, 2, 0) // 0=gear, 1=orb, 2=blade
        for (i in hooksX.indices) {
            val hx = hooksX[i]
            val sway = 5f * sin(time / 900.0 + i * 1.3).toFloat()
            // hook curve suggestion (short line down + small arc)
            stroke.color = 0xFF555566.toInt(); stroke.strokeWidth = 1.5f
            canvas.drawLine(hx, railY, hx + sway * 0.3f, railY + 6f, stroke)
            // chain: stacked short segments
            val chainTop = railY + 6f; val chainLen = 20f
            for (seg in 0 until 4) {
                val sy = chainTop + seg * 5f
                canvas.drawLine(hx + sway * 0.3f, sy, hx + sway, sy + 4f, stroke)
            }
            // ware silhouette
            val wareY = chainTop + chainLen + 8f
            when (wareTypes[i]) {
                0 -> { // gear: circle + tick marks
                    canvas.drawCircle(hx + sway, wareY, 7f, stroke)
                    for (t in 0 until 6) {
                        val ang = Math.PI * 2 / 6 * t
                        canvas.drawLine(
                            (hx + sway + 7f * kotlin.math.cos(ang)).toFloat(), (wareY + 7f * kotlin.math.sin(ang)).toFloat(),
                            (hx + sway + 10f * kotlin.math.cos(ang)).toFloat(), (wareY + 10f * kotlin.math.sin(ang)).toFloat(), stroke)
                    }
                }
                1 -> { // orb: circle
                    stroke.color = 0xFF44CCFF.toInt(); stroke.alpha = 180
                    canvas.drawCircle(hx + sway, wareY, 7f, stroke); stroke.alpha = 255
                    stroke.color = 0xFF555566.toInt()
                }
                2 -> { // blade: triangle
                    canvas.drawLine(hx + sway, wareY - 8f, hx + sway - 5f, wareY + 8f, stroke)
                    canvas.drawLine(hx + sway - 5f, wareY + 8f, hx + sway + 5f, wareY + 8f, stroke)
                    canvas.drawLine(hx + sway + 5f, wareY + 8f, hx + sway, wareY - 8f, stroke)
                }
            }
        }
        // tied-back cloth on the right side
        stroke.color = 0xFF3A3040.toInt(); stroke.strokeWidth = 2f
        val clothPath = Path().apply {
            moveTo(b.right - 22f, railY)
            quadTo(b.right - 10f, railY + 30f, b.right - 22f, railY + 55f)
        }
        canvas.drawPath(clothPath, stroke)
        val clothPath2 = Path().apply {
            moveTo(b.right - 22f, railY + 30f)
            quadTo(b.right - 2f, railY + 36f, b.right - 22f, railY + 42f)
        }
        stroke.strokeWidth = 1f; canvas.drawPath(clothPath2, stroke)
        drawWalkway(canvas, b)
    }

    private fun design18_containerStack(canvas: Canvas, b: RectF) {
        // two stacked shipping containers
        val c1Rect = RectF(b.left + 8f, roomTop(b) + 4f, b.right - 8f, roomTop(b) + 58f)
        val c2Rect = RectF(b.left + 18f, roomTop(b) + 52f, b.right - 18f, roomTop(b) + 100f)
        val containers = listOf(
            Pair(c1Rect, 0xFF1E1E2C.toInt()),
            Pair(c2Rect, 0xFF242436.toInt())
        )
        for ((rect, col) in containers) {
            fill.color = col; canvas.drawRoundRect(rect, 3f, 3f, fill)
            stroke.color = 0xFF3A3A4A.toInt(); stroke.strokeWidth = 1.5f
            canvas.drawRoundRect(rect, 3f, 3f, stroke)
            // corrugation: evenly spaced vertical strokes
            val corrugStep = 14f; var cx2 = rect.left + corrugStep
            stroke.color = 0xFF2A2A3A.toInt(); stroke.strokeWidth = 1f
            while (cx2 < rect.right - corrugStep * 0.5f) {
                canvas.drawLine(cx2, rect.top + 2f, cx2, rect.bottom - 2f, stroke)
                cx2 += corrugStep
            }
        }
        // serial code stencil on container 1
        text.textSize = 9f; text.color = 0xFF666677.toInt()
        canvas.drawText("BM-7741-X", c1Rect.centerX(), c1Rect.top + 14f, text)
        // serial code stencil on container 2
        canvas.drawText("KV-3302-A", c2Rect.centerX(), c2Rect.top + 14f, text)
        // hazard placard on container 2 (small square)
        val placX = c2Rect.right - 22f; val placY = c2Rect.top + 20f
        fill.color = 0xFFCCAA22.toInt(); fill.alpha = 160
        canvas.drawRect(placX, placY, placX + 14f, placY + 14f, fill); fill.alpha = 255
        stroke.color = 0xFF000000.toInt(); stroke.strokeWidth = 1f
        canvas.drawRect(placX, placY, placX + 14f, placY + 14f, stroke)
        drawWalkway(canvas, b)
    }

    private fun design19_neonGrid(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        val vpX = b.centerX(); val vpY = walkwayY(b) - 20f
        // perspective floor grid lines
        stroke.color = 0xFF44CCFF.toInt(); stroke.strokeWidth = 1f; stroke.alpha = 60
        // horizontal rungs
        val rungYs = floatArrayOf(walkwayY(b) - 2f, walkwayY(b) + 8f, walkwayY(b) + 20f)
        for (ry in rungYs) {
            val spread = (ry - vpY) * 0.9f
            canvas.drawLine(vpX - spread, ry, vpX + spread, ry, stroke)
        }
        // radiating verticals (converging to vanishing point)
        val groundSpreads = floatArrayOf(-80f, -50f, -25f, 0f, 25f, 50f, 80f)
        for (gs in groundSpreads) {
            canvas.drawLine(vpX + gs, floorY(b), vpX, vpY, stroke)
        }
        stroke.alpha = 255
        // two vertical neon pylons left & right
        val pylonFlick = 0.7f + 0.3f * sin(time / 400.0).toFloat()
        stroke.color = 0xFF44CCFF.toInt(); stroke.strokeWidth = 2f; stroke.alpha = (pylonFlick * 200).toInt()
        canvas.drawLine(b.left + 20f, roomTop(b), b.left + 20f, walkwayY(b), stroke)
        canvas.drawLine(b.right - 20f, roomTop(b), b.right - 20f, walkwayY(b), stroke)
        stroke.alpha = 255
        // pylon cap dots
        fill.color = 0xFF44CCFF.toInt(); fill.alpha = (pylonFlick * 220).toInt()
        canvas.drawCircle(b.left + 20f, roomTop(b), 3f, fill)
        canvas.drawCircle(b.right - 20f, roomTop(b), 3f, fill)
        fill.alpha = 255
        // "BLACK MARKET" with glow rect behind
        val labelY = roomTop(b) + 18f
        fill.color = 0xFF003344.toInt(); fill.alpha = 160
        canvas.drawRect(b.centerX() - 66f, labelY - 14f, b.centerX() + 66f, labelY + 4f, fill); fill.alpha = 255
        text.textSize = 14f; text.color = 0xFF44CCFF.toInt()
        canvas.drawText("BLACK MARKET", b.centerX(), labelY, text)
        drawWalkway(canvas, b)
    }

    private fun design20_ashChapel(canvas: Canvas, b: RectF) {
        val time = System.currentTimeMillis()
        val cx = b.centerX()
        // pointed-arch window outline on the back wall
        val archLeft = cx - 30f; val archRight = cx + 30f
        val archBottom = walkwayY(b) - 10f; val archTop = roomTop(b) + 20f
        val archPath = Path().apply {
            moveTo(archLeft, archBottom)
            lineTo(archLeft, archTop + 22f)
            quadTo(archLeft, archTop, cx, archTop - 10f)
            quadTo(archRight, archTop, archRight, archTop + 22f)
            lineTo(archRight, archBottom)
        }
        // faint cold-blue fill / light inside the arch
        fill.color = 0xFF112244.toInt(); fill.alpha = 120; canvas.drawPath(archPath, fill); fill.alpha = 255
        stroke.color = 0xFF445577.toInt(); stroke.strokeWidth = 1.5f; canvas.drawPath(archPath, stroke)
        // two simple columns flanking
        val colW = 8f; val colTop = roomTop(b) + 14f; val colBot = walkwayY(b)
        stroke.color = 0xFF3A3A4A.toInt(); stroke.strokeWidth = 1.5f
        canvas.drawRect(archLeft - colW - 6f, colTop, archLeft - 6f, colBot, stroke)
        canvas.drawRect(archRight + 6f, colTop, archRight + colW + 6f, colBot, stroke)
        // drifting ash: several slow-falling low-alpha dots
        glow.color = 0xFFCCCCCC.toInt()
        for (i in 0 until 12) {
            val ashPhase = (time / (1200.0 + i * 80)) % 1.0
            val ashX = b.left + 20f + (i * 23f) % (b.width() - 40f)
            val ashY = roomTop(b) + (ashPhase * (walkwayY(b) - roomTop(b))).toFloat()
            glow.alpha = (50 + i % 3 * 20)
            canvas.drawCircle(ashX, ashY, 1.2f + (i % 3) * 0.4f, glow)
        }
        glow.alpha = 255
        // dim warm candle glow near walkway EDGE (not center)
        val candleX = b.left + 30f; val candleY = walkwayY(b) - 6f
        val candleFlick = 0.5f + 0.5f * sin(time / 350.0).toFloat()
        glow.color = 0xFFCC9944.toInt(); glow.alpha = (candleFlick * 140).toInt()
        canvas.drawCircle(candleX, candleY, 8f, glow); glow.alpha = 255
        fill.color = 0xFFDDCC88.toInt()
        canvas.drawRect(candleX - 2f, candleY - 8f, candleX + 2f, candleY, fill)
        drawWalkway(canvas, b)
    }
}
