package com.astroloop.game.hangar

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.astroloop.game.core.LayoutRect
import com.astroloop.game.data.BandanaDefinitions
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.entity.Boss
import com.astroloop.game.render.FontManager
import com.astroloop.game.render.IconCache
import java.util.concurrent.CopyOnWriteArrayList

class BarPageRenderer(
    private val textPaint: Paint,
    private val costPaint: Paint,
    private val persistence: PersistenceManager
) {
    var screenWidth = 0f
    var screenHeight = 0f
    var walkwayY = 0f
    var ceilingY = 0f
    var content: LayoutRect = LayoutRect(0f, 0f, 0f, 0f)

    val pilotCardRects = CopyOnWriteArrayList<RectF>()
    var codexBookRect = RectF()
    var corrupted = false
    var astroLoop = false
    var dressing: BarDressing = BarDressing.forStage(com.astroloop.game.core.StoryStage.NORMAL)

    private val bitmapPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    private val signPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = FontManager.getBold()
        letterSpacing = 0.15f
    }
    private val signGlowPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = FontManager.getBold()
        letterSpacing = 0.15f
    }
    private val signLinePaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }

    private val cardBgPaint = Paint().apply { style = Paint.Style.FILL }
    private val cardBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val swagWirePaint = Paint().apply { color = 0xFF443322.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
    private val swagBulbPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val swagPath = Path()
    private val stringLightColors = listOf(0xFFFFCC88.toInt())
    private val fairyLightColors = listOf(
        0xFFFF0066.toInt(), 0xFFFFAA00.toInt(), 0xFF00FF88.toInt(),
        0xFF00AAFF.toInt(), 0xFFFF00FF.toInt()
    )

    private val confettiPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val confettiColors = intArrayOf(
        0xFFFF0066.toInt(), 0xFFFFAA00.toInt(), 0xFF00FF88.toInt(), 0xFF00AAFF.toInt(), 0xFFFF00FF.toInt()
    )
    // (x fraction 0..1, yOffset px below the walkway rect, colorIndex) — seeded once.
    // Band is 5..9px below walkwayY: the walkway line (walkwayY..+4, drawn later by
    // HangarRenderer over the bar page) would otherwise occlude confetti placed on it,
    // and chat starts at walkwayY+10, so 5..9 is the clear floor strip.
    private val confettiSpots: List<Triple<Float, Float, Int>> = run {
        val r = java.util.Random(99)
        List(26) { i -> Triple(r.nextFloat(), 5f + r.nextFloat() * 4f, i % confettiColors.size) }
    }


    private val sitterFillPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val sitterLimbPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
    private val sitterBandPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }

    lateinit var drawRoomFrame: (Canvas, Boolean, Boolean, Boolean) -> Unit
    lateinit var drawNPCWalkers: (Canvas, HangarState) -> Unit
    lateinit var drawCharacter: (Canvas, Float, Float, Int, Boolean, Float) -> Unit

    fun draw(canvas: Canvas, state: HangarState, xOffset: Float) {
        canvas.save()
        canvas.translate(-xOffset, 0f)

        drawNormalBar(canvas, state)

        canvas.restore()
    }

    private fun drawNormalBar(canvas: Canvas, state: HangarState) {
        // Room frame: ceiling + solid left wall + archway on right
        drawRoomFrame(canvas, true, true, false)

        // --- Pilot portrait grid (top half, 4 columns x 3 rows) ---
        pilotCardRects.clear()

        val cols = 4
        val rows = 3
        val gridPadding = 12f
        val gridLeft = content.left + gridPadding
        val gridRight = content.right - gridPadding
        val gridTop = content.top + 70f
        val gridBottom = content.top + content.height * 0.52f
        val cardGap = 8f

        val totalGapX = cardGap * (cols - 1)
        val totalGapY = cardGap * (rows - 1)
        val cardWidth = (gridRight - gridLeft - totalGapX) / cols
        val cardHeight = (gridBottom - gridTop - totalGapY) / rows

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val index = row * cols + col
                val pilot = PilotDefinitions.getPilotByIndex(index) ?: continue

                val cx = gridLeft + col * (cardWidth + cardGap)
                val cy = gridTop + row * (cardHeight + cardGap)
                val rect = RectF(cx, cy, cx + cardWidth, cy + cardHeight)
                pilotCardRects.add(rect)

                val isUnlocked = state.isPilotUnlocked(index)
                val isDead = !isUnlocked && state.isPilotDeadInCorruption(index)
                val nextPilotIndex = state.getNextPilotIndex()
                val isNextToRecruit = (index == nextPilotIndex)

                // Dim non-selected cards (selected = 1.0, others lerp to 0.5)
                val cardFade = if (index < state.pilotCardFades.size) state.pilotCardFades[index] else 1f
                val cardAlpha = (cardFade * 255).toInt()

                // Background — always dark, portrait provides the color
                cardBgPaint.color = 0xFF1A1A2E.toInt()
                cardBgPaint.alpha = cardAlpha
                canvas.drawRoundRect(rect, 4f, 4f, cardBgPaint)
                cardBgPaint.alpha = 255

                // Border — unlocked pilots use their pilot color (corruption red in corruption runs)
                val borderColor = if (corrupted) Boss.CORRUPTION_COLOR else pilot.color
                when {
                    isUnlocked -> {
                        cardBorderPaint.color = borderColor
                        cardBorderPaint.alpha = cardAlpha
                        cardBorderPaint.strokeWidth = 1.5f
                    }
                    isNextToRecruit -> {
                        cardBorderPaint.color = borderColor
                        cardBorderPaint.alpha = (180 * cardFade).toInt()
                        cardBorderPaint.strokeWidth = 1f
                    }
                    else -> {
                        cardBorderPaint.color = 0xFF333344.toInt()
                        cardBorderPaint.alpha = cardAlpha
                        cardBorderPaint.strokeWidth = 1f
                    }
                }
                canvas.drawRoundRect(rect, 4f, 4f, cardBorderPaint)
                cardBorderPaint.alpha = 255  // reset

                // Portrait — top 65% of card
                val portraitHeight = cardHeight * 0.65f
                val portraitRect = RectF(rect.left + 2f, rect.top + 2f, rect.right - 2f, rect.top + portraitHeight)

                // Compute flip state here so portrait alpha can reflect it
                val isFlipping = isUnlocked && (index == state.pilotFlipIndex)
                val flipProgress = if (isFlipping) state.pilotFlipProgress else 1f
                val showBack = isFlipping && state.pilotFlipShowBack

                val portraitBitmap = when {
                    isUnlocked && corrupted && pilot.id == "pilot_astro" -> IconCache.getPortraitBoss()
                    isUnlocked && corrupted -> IconCache.getCorruptedPortrait(pilot.id) ?: IconCache.getPortrait(pilot.id)
                    isUnlocked && state.persistence.hasBandana(pilot.id) ->
                        IconCache.getBandanaPortrait(pilot.id) ?: IconCache.getPortrait(pilot.id)
                    isUnlocked -> IconCache.getPortrait(pilot.id)
                    isDead -> null
                    else -> IconCache.getPortraitLocked()
                }
                if (portraitBitmap != null) {
                    val baseAlpha = when {
                        isUnlocked      -> cardAlpha
                        isNextToRecruit -> (80 * cardFade).toInt()
                        else            -> (40 * cardFade).toInt()
                    }
                    bitmapPaint.alpha = when {
                        isFlipping && !showBack -> (baseAlpha * flipProgress).toInt()       // front fading out
                        isFlipping && showBack  -> (baseAlpha * (1f - flipProgress)).toInt() // portrait fading in under dissolving back
                        else -> baseAlpha
                    }
                    canvas.drawBitmap(portraitBitmap, null, portraitRect, bitmapPaint)
                    bitmapPaint.alpha = 255
                }

                // Bottom strip text
                val stripTop = rect.top + portraitHeight
                val stripH = cardHeight - portraitHeight

                if (isUnlocked) {
                    if (showBack) {
                        if (corrupted && pilot.id == "pilot_astro") {
                            // TB-26 is dead — show muted memorial back
                            cardBgPaint.color = 0xFF252538.toInt()
                            cardBgPaint.alpha = (255 * flipProgress).toInt()
                            canvas.drawRoundRect(rect, 4f, 4f, cardBgPaint)
                            cardBgPaint.color = 0xFF1A1A2E.toInt()

                            val tb26Icon = IconCache.getPassiveIcon("tb26")
                            if (tb26Icon != null) {
                                val iconSize = (rect.width() * 0.45f).coerceAtMost(portraitHeight * 0.8f)
                                val iconLeft = rect.centerX() - iconSize / 2
                                val iconTop = rect.top + (portraitHeight - iconSize) / 2
                                val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                                bitmapPaint.alpha = (128 * flipProgress).toInt()  // 50% alpha
                                canvas.drawBitmap(tb26Icon, null, iconRect, bitmapPaint)
                                bitmapPaint.alpha = 255
                            }

                            textPaint.textSize = (cardHeight * 0.13f).coerceIn(12f, 22f)
                            textPaint.color = 0xFF666677.toInt()
                            textPaint.alpha = (255 * flipProgress).toInt()
                            canvas.drawText("TB-26", rect.centerX(), stripTop + stripH * 0.38f, textPaint)

                            textPaint.textSize = (cardHeight * 0.12f).coerceIn(13f, 22f)
                            textPaint.color = 0xFF994444.toInt()
                            canvas.drawText("He's gone.", rect.centerX(), stripTop + stripH * 0.78f, textPaint)
                            textPaint.alpha = 255
                        } else {
                            // Back face — cross-dissolves with portrait at animation end
                            cardBgPaint.color = 0xFF252538.toInt()
                            cardBgPaint.alpha = (255 * flipProgress).toInt()
                            canvas.drawRoundRect(rect, 4f, 4f, cardBgPaint)
                            cardBgPaint.color = 0xFF1A1A2E.toInt()

                            val effectivePassiveId = PassiveDefinitions.getEffectivePassiveId(pilot.startingPassiveId, pilot.id, astroLoop)
                            val passiveDef = PassiveDefinitions.getPassiveDef(effectivePassiveId)

                            // Passive icon — centered at 45% of card width (same proportion as upgrade card)
                            val passiveIcon = IconCache.getPassiveIcon(effectivePassiveId)
                            if (passiveIcon != null) {
                                val iconSize = (rect.width() * 0.45f).coerceAtMost(portraitHeight * 0.8f)
                                val iconLeft = rect.centerX() - iconSize / 2
                                val iconTop = rect.top + (portraitHeight - iconSize) / 2
                                val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                                bitmapPaint.alpha = (255 * flipProgress).toInt()
                                canvas.drawBitmap(passiveIcon, null, iconRect, bitmapPaint)
                                bitmapPaint.alpha = 255
                            }

                            // Passive name (white) — same size as callsign, no wrapping
                            textPaint.textSize = (cardHeight * 0.13f).coerceIn(12f, 22f)
                            textPaint.color = 0xFFFFFFFF.toInt()
                            textPaint.alpha = (255 * flipProgress).toInt()
                            canvas.drawText(
                                PassiveDefinitions.getDisplayName(pilot.startingPassiveId, pilot.id, astroLoop),
                                rect.centerX(), stripTop + stripH * 0.38f, textPaint
                            )

                            // Effect text (amber) — \n = explicit split; otherwise two-line wrap if needed
                            val effectText = passiveDef?.description ?: ""
                            textPaint.textSize = (cardHeight * 0.12f).coerceIn(13f, 22f)
                            textPaint.color = 0xFFFFBB44.toInt()
                            textPaint.alpha = (255 * flipProgress).toInt()
                            val availableWidthEff = rect.width() - 12f
                            val lineH = textPaint.textSize * 1.2f
                            if ('\n' in effectText) {
                                val parts = effectText.split('\n')
                                canvas.drawText(parts[0], rect.centerX(), stripTop + stripH * 0.72f, textPaint)
                                canvas.drawText(parts.getOrElse(1) { "" }, rect.centerX(), stripTop + stripH * 0.72f + lineH, textPaint)
                            } else if (textPaint.measureText(effectText) <= availableWidthEff) {
                                canvas.drawText(effectText, rect.centerX(), stripTop + stripH * 0.78f, textPaint)
                            } else {
                                val words = effectText.split(" ")
                                var splitIndex = 1
                                for (i in 2 until words.size) {
                                    if (textPaint.measureText(words.take(i).joinToString(" ")) > availableWidthEff) {
                                        splitIndex = i - 1
                                        break
                                    }
                                }
                                val line1 = words.take(splitIndex).joinToString(" ")
                                val line2 = words.drop(splitIndex).joinToString(" ")
                                canvas.drawText(line1, rect.centerX(), stripTop + stripH * 0.72f, textPaint)
                                canvas.drawText(line2, rect.centerX(), stripTop + stripH * 0.72f + lineH, textPaint)
                            }
                            textPaint.alpha = 255
                        }
                    } else {
                        // Front face
                        val frontAlpha = if (isFlipping) (cardAlpha * flipProgress).toInt() else cardAlpha

                        textPaint.textSize = (cardHeight * 0.13f).coerceIn(12f, 22f)
                        textPaint.color = 0xFFFFFFFF.toInt()
                        textPaint.alpha = frontAlpha
                        canvas.drawText(pilot.callsign, rect.centerX(), stripTop + stripH * 0.38f, textPaint)
                        textPaint.alpha = 255

                        // Passive name text only (no icon)
                        val nameY = stripTop + stripH * 0.78f
                        textPaint.textSize = (cardHeight * 0.13f).coerceIn(12f, 22f)
                        textPaint.color = 0xFFAABBCC.toInt()
                        textPaint.alpha = frontAlpha
                        // Corruption: Astro's TB-26 passive is gone — strike it through.
                        textPaint.isStrikeThruText = corrupted && pilot.id == "pilot_astro"
                        canvas.drawText(
                            PassiveDefinitions.getDisplayName(pilot.startingPassiveId, pilot.id, astroLoop),
                            rect.centerX(), nameY, textPaint
                        )
                        textPaint.isStrikeThruText = false
                        textPaint.alpha = 255
                    }
                } else if (isNextToRecruit) {
                    textPaint.textSize = (cardHeight * 0.18f).coerceIn(12f, 16f)
                    textPaint.color = 0xFF888888.toInt()
                    textPaint.alpha = cardAlpha
                    canvas.drawText("???", rect.centerX(), stripTop + stripH * 0.55f, textPaint)
                    textPaint.alpha = 255
                }
                // Unknown: no text
            }
        }

        // Bar area with TB-26 bartender, codex book, and neon lamps
        drawBarArea(canvas, state)
        drawNPCWalkers(canvas, state)

        // Chat messages below walkway
        drawChatMessages(canvas, state)
    }

    private fun drawChatMessages(canvas: Canvas, state: HangarState) {
        val lineHeight = 32f
        val chatFontSize = 24f
        val indicatorY = screenHeight * 0.95f
        val chatBottom = indicatorY - 30f
        val chatTop = walkwayY + 10f
        val maxVisible = ((chatBottom - chatTop) / lineHeight).toInt().coerceAtLeast(1)
        val chatLeft = 12f  // gridPadding
        val messages = state.chatMessages.toList()
        val startIndex = (messages.size - maxVisible).coerceAtLeast(0)
        val visibleMessages = messages.subList(startIndex, messages.size)

        textPaint.textAlign = Paint.Align.LEFT
        for ((i, msg) in visibleMessages.withIndex()) {
            val msgY = chatTop + (i + 1) * lineHeight
            textPaint.textSize = chatFontSize
            textPaint.color = msg.color
            val callsignText = "[${msg.speaker}]: "
            canvas.drawText(callsignText, chatLeft + 6f, msgY, textPaint)
            val callsignWidth = textPaint.measureText(callsignText)
            textPaint.color = 0xFFCCCCCC.toInt()
            val textX = chatLeft + 6f + callsignWidth
            val fittedText = truncateToFit(msg.text, textPaint, content.right - 12f - textX)
            canvas.drawText(fittedText, textX, msgY, textPaint)
        }
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = 0xFFFFFFFF.toInt()
    }

    /** Trim a chat line with an ellipsis so it never runs past the chat column. */
    private fun truncateToFit(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        for (i in text.length - 1 downTo 1) {
            val truncated = text.substring(0, i) + "…"
            if (paint.measureText(truncated) <= maxWidth) return truncated
        }
        return "…"
    }

    /**
     * Draws the bar area static elements: shelf, bottles, counter, stools, codex book, neon lamps.
     * @param hasEvolutions Whether codex book should glow with evolution discovery
     */
    private fun drawBarElements(canvas: Canvas, swayMomentum: Float = 0f, hasEvolutions: Boolean = false) {
        val barLeft = 10f
        val barRight = screenWidth - 10f
        val barTop = walkwayY - 25f
        val barBottom = walkwayY + 2f

        // Shelf behind bar (full width) — drawn first as back wall
        val shelfPaint = Paint().apply {
            color = 0xFF2A1A0A.toInt()
            style = Paint.Style.FILL
        }
        val shelfY = barTop - 18f
        canvas.drawRect(barLeft + 5f, shelfY, barRight - 5f, shelfY + 2f, shelfPaint)

        // Bottles on shelf (more bottles across full width)
        val bottlePaint = Paint().apply { style = Paint.Style.FILL }
        val bottleColors = listOf(
            0xFF00AA44.toInt(), 0xFFAA4400.toInt(), 0xFF4444AA.toInt(), 0xFFAA0044.toInt(),
            0xFF44AA88.toInt(), 0xFFAA8800.toInt(), 0xFF8844AA.toInt(), 0xFF00AAAA.toInt()
        )
        val bottleSpacing = (barRight - barLeft - 20f) / (bottleColors.size + 1)
        for ((bi, bColor) in bottleColors.withIndex()) {
            val bx = barLeft + 10f + bottleSpacing * (bi + 1)
            val by = shelfY - 1f
            bottlePaint.color = bColor
            bottlePaint.alpha = 160
            canvas.drawRoundRect(RectF(bx - 2f, by - 8f, bx + 2f, by), 1f, 1f, bottlePaint)
            canvas.drawRect(bx - 1f, by - 11f, bx + 1f, by - 8f, bottlePaint)
        }

        // Counter surface (dark wood-ish)
        val counterPaint = Paint().apply {
            color = 0xFF2A1A0A.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 3f, 3f, counterPaint)

        // Counter edge highlight
        val edgePaint = Paint().apply {
            color = 0xFF885522.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawLine(barLeft, barTop, barRight, barTop, edgePaint)

        // Bar stools in front of counter (above walkway line)
        val stoolPaint = Paint().apply {
            color = 0xFF3A2A1A.toInt()
            style = Paint.Style.FILL
        }
        val stoolLegPaint = Paint().apply {
            color = 0xFF555555.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val stoolCount = 8
        val stoolSpacing = (barRight - barLeft) / (stoolCount + 1)
        for (s in 1..stoolCount) {
            val sx = barLeft + stoolSpacing * s
            val seatY = barBottom - 6f
            canvas.drawRoundRect(RectF(sx - 5f, seatY, sx + 5f, seatY + 3f), 1f, 1f, stoolPaint)
            canvas.drawLine(sx, seatY + 3f, sx, barBottom, stoolLegPaint)
        }

        // Codex book on counter (right side)
        val bookX = barRight - 55f
        val bookY = barTop - 2f
        val bookW = 22f
        val bookH = 15f
        val bookRect = RectF(bookX - bookW / 2f, bookY - bookH, bookX + bookW / 2f, bookY)
        codexBookRect = bookRect

        val bookPaint = Paint().apply { style = Paint.Style.FILL }
        val bookLinePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        if (hasEvolutions) {
            // Discovered evolutions — book glows faintly
            bookPaint.color = 0xFF2A2035.toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookPaint)
            // Glow
            val time = System.currentTimeMillis()
            val glow = 0.4f + 0.3f * kotlin.math.sin(time / 800.0).toFloat()
            bookLinePaint.color = 0xFF8844AA.toInt()
            bookLinePaint.alpha = (glow * 200).toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookLinePaint)
            // Page lines
            bookLinePaint.strokeWidth = 0.5f
            bookLinePaint.alpha = (glow * 120).toInt()
            canvas.drawLine(bookRect.left + 4f, bookRect.top + 4f, bookRect.right - 4f, bookRect.top + 4f, bookLinePaint)
            canvas.drawLine(bookRect.left + 4f, bookRect.top + 7f, bookRect.right - 4f, bookRect.top + 7f, bookLinePaint)
            canvas.drawLine(bookRect.left + 4f, bookRect.top + 10f, bookRect.right - 6f, bookRect.top + 10f, bookLinePaint)
            // Bookmark tab sticking out top
            bookPaint.color = 0xFF8844AA.toInt()
            bookPaint.alpha = (glow * 220).toInt()
            canvas.drawRect(bookRect.right - 6f, bookRect.top - 4f, bookRect.right - 3f, bookRect.top, bookPaint)
            bookPaint.alpha = 255
        } else {
            // No evolutions — dark/closed book
            bookPaint.color = 0xFF1A1A24.toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookPaint)
            bookLinePaint.color = 0xFF333344.toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookLinePaint)
        }

        // Neon lamps across full crew quarters width
        val lampPaint = Paint().apply { style = Paint.Style.FILL }
        val lampColors = listOf(
            0xFFFF0066.toInt(), 0xFF00FF88.toInt(), 0xFFFFAA00.toInt(), 0xFF00AAFF.toInt(),
            0xFFFF00FF.toInt(), 0xFF88FF00.toInt(), 0xFFFF8800.toInt()
        )
        val lampSpacing = (barRight - barLeft) / (lampColors.size + 1)

        val wirePaint = Paint().apply {
            color = 0xFF444444.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        for ((i, lampColor) in lampColors.withIndex()) {
            val swayAmount = (swayMomentum * 0.005f).coerceIn(-5f, 5f)
            val lx = barLeft + lampSpacing * (i + 1) + swayAmount * (i + 1) * 0.3f
            val ly = barTop - 25f

            // Lamp glow
            lampPaint.color = lampColor
            lampPaint.alpha = 40
            canvas.drawCircle(lx, ly, 8f, lampPaint)

            // Lamp bulb
            lampPaint.alpha = 200
            canvas.drawCircle(lx, ly, 2.5f, lampPaint)

            // Wire to ceiling
            val wireTopX = barLeft + lampSpacing * (i + 1)
            canvas.drawLine(lx, ly - 8f, wireTopX, ceilingY + 3f, wirePaint)
        }
    }

    /** Tucked legs, drawn BEFORE the seat so the seat hides the thighs (patron faces the bar). */
    private fun drawSitterLegs(canvas: Canvas, sx: Float, seatY: Float, color: Int) {
        sitterLimbPaint.color = color
        canvas.drawLine(sx - 1.5f, seatY + 1f, sx - 2f, seatY + 6f, sitterLimbPaint)
        canvas.drawLine(sx + 1.5f, seatY + 1f, sx + 2f, seatY + 6f, sitterLimbPaint)
    }

    /** Body on the seat with hands resting on the counter edge (barTop = seatY - 3). */
    private fun drawSitterBody(canvas: Canvas, sx: Float, seatY: Float, color: Int, bandanaColor: Int? = null) {
        sitterFillPaint.color = color
        canvas.drawCircle(sx, seatY - 4f, 5f, sitterFillPaint)
        sitterLimbPaint.color = color
        canvas.drawLine(sx - 4f, seatY - 5f, sx - 6f, seatY - 3f, sitterLimbPaint)
        canvas.drawLine(sx + 4f, seatY - 5f, sx + 6f, seatY - 3f, sitterLimbPaint)

        // Bandana, seen from behind: same chord-fit band as the walkers (head center
        // seatY-4, r=5 → band 2.4 above center) plus the knot's two tails hanging down.
        if (bandanaColor != null) {
            sitterBandPaint.color = bandanaColor
            val bandY = seatY - 6.4f
            canvas.drawLine(sx - 4.1f, bandY, sx + 4.1f, bandY, sitterBandPaint)
            canvas.drawLine(sx - 0.5f, bandY, sx - 2f, bandY + 3.5f, sitterBandPaint)
            canvas.drawLine(sx + 0.5f, bandY, sx + 1.5f, bandY + 3f, sitterBandPaint)
        }
    }

    private fun drawBarArea(canvas: Canvas, state: HangarState) {
        val swipeVelocity = state.swayMomentum
        val hasEvolutions = state.hasDiscoveredEvolutions
        val geom = BarGeometry(walkwayY)
        val barLeft = 10f
        val barRight = screenWidth - 10f
        val barTop = geom.counterTop      // slim slab (was walkwayY - 25f)
        val barBottom = geom.counterBottom

        // Shelf behind bar (full width) — drawn first as back wall
        val shelfPaint = Paint().apply {
            color = 0xFF2A1A0A.toInt()
            style = Paint.Style.FILL
        }
        val shelfY = barTop - 18f
        canvas.drawRect(barLeft + 5f, shelfY, barRight - 5f, shelfY + 2f, shelfPaint)

        // Bottles on shelf (more bottles across full width)
        val bottlePaint = Paint().apply { style = Paint.Style.FILL }
        val bottleColors = listOf(
            0xFF00AA44.toInt(), 0xFFAA4400.toInt(), 0xFF4444AA.toInt(), 0xFFAA0044.toInt(),
            0xFF44AA88.toInt(), 0xFFAA8800.toInt(), 0xFF8844AA.toInt(), 0xFF00AAAA.toInt()
        )
        val bottleSpacing = (barRight - barLeft - 20f) / (bottleColors.size + 1)
        for ((bi, bColor) in bottleColors.withIndex()) {
            val bx = barLeft + 10f + bottleSpacing * (bi + 1)
            val by = shelfY - 1f
            bottlePaint.color = bColor
            bottlePaint.alpha = 160
            canvas.drawRoundRect(RectF(bx - 2f, by - 8f, bx + 2f, by), 1f, 1f, bottlePaint)
            canvas.drawRect(bx - 1f, by - 11f, bx + 1f, by - 8f, bottlePaint)
        }

        // TB-26 behind bar (dynamic position, pacing) — drawn before counter so legs are occluded
        // Skip TB-26 when corrupted (bar is empty)
        if (!corrupted) {
            val tb26X = state.tb26BarX
            val tb26Y = barTop + 2f  // Legs extend to y+5 = barTop+7, hidden behind counter (barTop to barBottom)
            drawCharacter(canvas, tb26X, tb26Y, 0xFF88AACC.toInt(), state.tb26BarMoving, 0f)

            // Cyan eye for TB-26 (droid detail)
            val eyePaint = Paint().apply {
                color = 0xFF00FFFF.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(tb26X + 1.5f, tb26Y - 7f, 1.5f, eyePaint)
        }

        // Counter surface (dark wood-ish) — occludes TB-26's legs
        val counterPaint = Paint().apply {
            color = 0xFF2A1A0A.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 3f, 3f, counterPaint)

        // Counter edge highlight
        val edgePaint = Paint().apply {
            color = 0xFF885522.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawLine(barLeft, barTop, barRight, barTop, edgePaint)

        // Bar stools in front of counter (above walkway line)
        val stoolPaint = Paint().apply {
            color = 0xFF3A2A1A.toInt()
            style = Paint.Style.FILL
        }
        val stoolLegPaint = Paint().apply {
            color = 0xFF555555.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val stoolCount = 8
        val stoolSpacing = (barRight - barLeft) / (stoolCount + 1)
        for (s in 1..stoolCount) {
            val sx = barLeft + stoolSpacing * s
            val seatY = geom.stoolSeatY
            val sitter = if (dressing.seatedCrew)
                state.npcWalkers.firstOrNull { it.seated && it.seatedStool == s } else null
            if (sitter != null) drawSitterLegs(canvas, sx, seatY, sitter.color)
            canvas.drawRoundRect(RectF(sx - 5f, seatY, sx + 5f, seatY + 3f), 1f, 1f, stoolPaint)
            canvas.drawLine(sx - 3f, seatY + 3f, sx - 5f, barBottom, stoolLegPaint)
            canvas.drawLine(sx + 3f, seatY + 3f, sx + 5f, barBottom, stoolLegPaint)
            if (sitter != null) {
                val sitterBandana = PilotDefinitions.getPilotByIndex(sitter.pilotIndex)
                    ?.takeIf { persistence.hasBandana(it.id) }
                    ?.let { BandanaDefinitions.accentColor(it.id) }
                drawSitterBody(canvas, sx, seatY, sitter.color, sitterBandana)
            }
        }

        // Beer sliding along counter surface (skip when corrupted — no TB-26 to slide beers)
        if (state.beerActive && !corrupted) {
            val beerAlpha = (state.beerFadeAlpha * 255).toInt().coerceIn(0, 255)
            val beerPaint = Paint().apply {
                color = 0xFFDDAA44.toInt()
                alpha = beerAlpha
                style = Paint.Style.FILL
            }
            val beerSurfaceY = geom.beerSurfaceY   // was barTop + 2f — the +2 sank the glass into the slab
            canvas.drawRect(state.beerX - 3f, beerSurfaceY - 6f, state.beerX + 3f, beerSurfaceY, beerPaint)
            // Foam top
            val foamPaint = Paint().apply {
                color = 0xFFEEDDBB.toInt()
                alpha = beerAlpha
                style = Paint.Style.FILL
            }
            canvas.drawRect(state.beerX - 3f, beerSurfaceY - 8f, state.beerX + 3f, beerSurfaceY - 6f, foamPaint)
        }

        // Codex book on counter (right side)
        val bookX = barRight - 55f
        val bookY = barTop - 2f
        val bookW = 22f
        val bookH = 15f
        val bookRect = RectF(bookX - bookW / 2f, bookY - bookH, bookX + bookW / 2f, bookY)
        codexBookRect = bookRect

        val bookPaint = Paint().apply { style = Paint.Style.FILL }
        val bookLinePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        if (hasEvolutions) {
            // Discovered evolutions — book glows faintly
            bookPaint.color = 0xFF2A2035.toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookPaint)
            // Glow
            val time = System.currentTimeMillis()
            val glow = 0.4f + 0.3f * kotlin.math.sin(time / 800.0).toFloat()
            bookLinePaint.color = 0xFF8844AA.toInt()
            bookLinePaint.alpha = (glow * 200).toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookLinePaint)
            // Page lines
            bookLinePaint.strokeWidth = 0.5f
            bookLinePaint.alpha = (glow * 120).toInt()
            canvas.drawLine(bookRect.left + 4f, bookRect.top + 4f, bookRect.right - 4f, bookRect.top + 4f, bookLinePaint)
            canvas.drawLine(bookRect.left + 4f, bookRect.top + 7f, bookRect.right - 4f, bookRect.top + 7f, bookLinePaint)
            canvas.drawLine(bookRect.left + 4f, bookRect.top + 10f, bookRect.right - 6f, bookRect.top + 10f, bookLinePaint)
            // Bookmark tab sticking out top
            bookPaint.color = 0xFF8844AA.toInt()
            bookPaint.alpha = (glow * 220).toInt()
            canvas.drawRect(bookRect.right - 6f, bookRect.top - 4f, bookRect.right - 3f, bookRect.top, bookPaint)
            bookPaint.alpha = 255
        } else {
            // No evolutions — dark/closed book
            bookPaint.color = 0xFF1A1A24.toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookPaint)
            bookLinePaint.color = 0xFF333344.toInt()
            canvas.drawRoundRect(bookRect, 2f, 2f, bookLinePaint)
        }

        // Neon lamps across full crew quarters width
        val lampPaint = Paint().apply { style = Paint.Style.FILL }
        val lampColors = dressing.lampColors   // warm / corruption-red / rainbow per stage
        val lampSpacing = (barRight - barLeft) / (lampColors.size + 1)

        val wirePaint = Paint().apply {
            color = 0xFF444444.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        for ((i, lampColor) in lampColors.withIndex()) {
            val swayAmount = (swipeVelocity * 0.005f).coerceIn(-5f, 5f)
            val lx = barLeft + lampSpacing * (i + 1) + swayAmount * (i + 1) * 0.3f
            val ly = barTop - 25f

            // Lamp glow
            lampPaint.color = lampColor
            lampPaint.alpha = 40
            canvas.drawCircle(lx, ly, 8f, lampPaint)

            // Lamp bulb
            lampPaint.alpha = 200
            canvas.drawCircle(lx, ly, 2.5f, lampPaint)

            // Wire to ceiling
            val wireTopX = barLeft + lampSpacing * (i + 1)
            canvas.drawLine(lx, ly - 8f, wireTopX, ceilingY + 3f, wirePaint)
        }

        drawNeonBarSign(canvas, geom)

        if (dressing.stringLights) {
            drawSwagLights(canvas, stringLightColors, dipPx = 7f)  // warm, gentle sag
        }
        if (dressing.fairyLights) {
            drawSwagLights(canvas, fairyLightColors, dipPx = 9f)
        }

        if (dressing.confetti) {
            drawConfetti(canvas)
        }
    }

    /** Static confetti scattered on the floor band just below the walkway line. */
    private fun drawConfetti(canvas: Canvas) {
        for ((xf, yOff, ci) in confettiSpots) {
            confettiPaint.color = confettiColors[ci]
            confettiPaint.alpha = 170
            canvas.drawCircle(20f + xf * (screenWidth - 40f), walkwayY + yOff, 1.1f, confettiPaint)
        }
    }

    /** Two swag arcs across the bar back wall with bulbs. Anchored to the wall, stage-independent. */
    private fun drawSwagLights(canvas: Canvas, colors: List<Int>, dipPx: Float) {
        val left = 40f
        val right = screenWidth - 40f
        val mid = (left + right) / 2f
        drawOneSwag(canvas, left, mid, colors, dipPx)
        drawOneSwag(canvas, mid, right, colors, dipPx)
    }

    /** One swag arc (wire + bulbs) between points a and b. */
    private fun drawOneSwag(canvas: Canvas, a: Float, b: Float, colors: List<Int>, dipPx: Float) {
        val baseY = ceilingY + 5f
        val ctrlX = (a + b) / 2f
        val ctrlY = baseY + dipPx * 2f   // quadratic control for a ~dipPx sag at the middle
        swagPath.reset()
        swagPath.moveTo(a, baseY)
        swagPath.quadTo(ctrlX, ctrlY, b, baseY)
        canvas.drawPath(swagPath, swagWirePaint)
        var t = 0.1f
        var ci = 0
        while (t < 0.95f) {
            val inv = 1f - t
            val bx = inv * inv * a + 2f * inv * t * ctrlX + t * t * b
            val by = inv * inv * baseY + 2f * inv * t * ctrlY + t * t * baseY
            swagBulbPaint.color = colors[ci % colors.size]
            swagBulbPaint.alpha = 210
            canvas.drawCircle(bx, by, 2f, swagBulbPaint)
            t += 0.11f
            ci++
        }
    }

    /** Cyan neon "BAR" glyph, upper-left of the bar wall. STEADY breathes; BLINKING flickers. */
    private fun drawNeonBarSign(canvas: Canvas, geom: BarGeometry) {
        val cyan = 0xFF00FFFF.toInt()
        val x = 62f
        val y = geom.shelfY - 8f          // just above the shelf, clear of the lamp row and grid
        val size = 15f
        val dim = when (dressing.signMode) {
            SignMode.STEADY   -> 0.7f + 0.3f * kotlin.math.sin(System.currentTimeMillis() / 400.0).toFloat()
            SignMode.BLINKING -> SignFlicker.dim(System.currentTimeMillis())
        }.coerceIn(0f, 1f)

        // Glow: two enlarged, low-alpha passes behind the glyph (no BlurMaskFilter).
        signGlowPaint.color = cyan
        signGlowPaint.textSize = size * 1.35f
        signGlowPaint.alpha = (60 * dim).toInt()
        canvas.drawText("BAR", x, y, signGlowPaint)
        signGlowPaint.textSize = size * 1.15f
        signGlowPaint.alpha = (90 * dim).toInt()
        canvas.drawText("BAR", x, y, signGlowPaint)

        // Crisp glyph.
        signPaint.color = cyan
        signPaint.textSize = size
        signPaint.alpha = (230 * dim).toInt()
        canvas.drawText("BAR", x, y, signPaint)

        // Tube underline.
        val w = signPaint.measureText("BAR")
        signLinePaint.color = cyan
        signLinePaint.alpha = (90 * dim).toInt()
        canvas.drawLine(x - w / 2f, y + size * 0.45f, x + w / 2f, y + size * 0.45f, signLinePaint)
    }
}
