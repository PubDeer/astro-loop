package com.astroloop.game.hangar

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.LayoutRect
import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.render.CrystalOrbPath
import com.astroloop.game.render.CrystalPalette
import com.astroloop.game.render.FontManager
import com.astroloop.game.render.IconCache
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class StorePageRenderer(
    private val persistence: PersistenceManager,
    private val textPaint: Paint,
    private val costPaint: Paint
) {
    var screenWidth = 0f

    /** Width of this room in design units. Equals screenWidth below sw600dp. */
    var roomWidth = 0f

    /** Room width with a fallback for the frame before dimensions arrive. */
    private val rw: Float get() = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)

    /**
     * The content column's edges in ROOM-local units — this page draws inside its room's
     * translate, and `content` is a screen-space rect. Above the gate the room is the content
     * column, so these are 0 and roomWidth; below the gate they are `content.left` / `.right`
     * unchanged, which is why nothing moves on a phone.
     */
    private val contentLeftInRoom: Float get() = HangarMetrics.contentXInRoom(content.left, roomWidth, screenWidth)
    private val contentRightInRoom: Float get() = HangarMetrics.contentXInRoom(content.right, roomWidth, screenWidth)

    var screenHeight = 0f
    var walkwayY = 0f
    var ceilingY = 0f
    var content: LayoutRect = LayoutRect(0f, 0f, 0f, 0f)

    val upgradeRects = CopyOnWriteArrayList<RectF>()
    val storeButtonRects = CopyOnWriteArrayList<RectF>()
    var crystalTileRect = RectF()

    private val bitmapPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    private val crystalDescPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = FontManager.getRegular()
    }

    lateinit var drawRoomFrame: (Canvas, Boolean, Boolean, Boolean) -> Unit

    fun draw(canvas: Canvas, state: HangarState, xOffset: Float) {
        canvas.save()
        canvas.translate(-xOffset, 0f)
        // Clip to this room, exactly as the shipyard page does: nothing the shop draws may reach
        // into a neighbouring room. Below the gate the room is the whole screen, so the clip is
        // the screen and nothing is cut — defence in depth, not a layout change.
        canvas.clipRect(0f, 0f, rw, screenHeight)

        // Room frame: ceiling + archway on left + solid right wall
        drawRoomFrame(canvas, true, false, true)
        drawStoreDecoration(canvas)

        upgradeRects.clear()

        // --- 3x3 black market upgrade grid ---
        val cols = 3
        val rows = 3
        val gridPadding = 16f
        // Room-local (see contentLeftInRoom): this page draws inside its room's translate, so a
        // screen-space content.left would push the grid a room-offset to the right of its room.
        val gridLeft = contentLeftInRoom + gridPadding
        val gridRight = contentRightInRoom - gridPadding
        val gridTop = 70f
        val gridBottom = walkwayY - 20f
        val cardGap = 10f

        val totalGapX = cardGap * (cols - 1)
        val totalGapY = cardGap * (rows - 1)
        val maxTileWidth = (gridRight - gridLeft - totalGapX) / cols
        val maxTileHeight = (gridBottom - gridTop - totalGapY) / rows
        val tileSize = maxTileWidth.coerceAtMost(maxTileHeight)

        // Center the grid in the content column, room-local
        val gridWidth = cols * tileSize + totalGapX
        val gridStartX = contentLeftInRoom + (content.width - gridWidth) / 2f
        val gridHeight = rows * tileSize + totalGapY
        val gridStartY = gridTop + (gridBottom - gridTop - gridHeight) / 2f

        val tileBgPaint = Paint().apply {
            color = 0xFF1A1A2E.toInt()
            style = Paint.Style.FILL
        }
        val tileBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        data class UpgradeTile(
            val name: String,
            val id: String?,          // null = NG+ locked
            val effect: String,
            val isNgPlus: Boolean = false
        )

        val tiles = listOf(
            // Row 1 — Survival
            UpgradeTile("Salvage Plate", "health", "+10 max HP"),
            UpgradeTile("Deflector Rig", "shields", "+10 max shield"),
            UpgradeTile("Nitro Boost", "speed", "+5% speed"),
            // Row 2 — Combat
            UpgradeTile("Hot Rounds", "damage", "+5% damage"),
            UpgradeTile("Lucky Rounds", "crit", "+5% crit chance"),
            UpgradeTile("Haul Line", "magnet", "+15% pickup range"),
            // Row 3 — Economy + NG+
            UpgradeTile("Finder's Fee", "yen_bonus", "+20% yen"),
            UpgradeTile("Scavenger Rig", "salvage", "+20% drop rate"),
            UpgradeTile("Emergency Shield", "emergency_shield", "Survive a lethal hit", isNgPlus = true)
        )

        val pipPaint = Paint().apply { style = Paint.Style.FILL }

        for ((index, tile) in tiles.withIndex()) {
            val row = index / cols
            val col = index % cols

            val tx = gridStartX + col * (tileSize + cardGap)
            val ty = gridStartY + row * (tileSize + cardGap)
            val rect = RectF(tx, ty, tx + tileSize, ty + tileSize)

            // Only add purchasable tiles to upgradeRects
            if (tile.isNgPlus) {
                // Crystal tile — auto-unlocks, no purchase needed
                crystalTileRect = RectF(rect)
                if (StoryStateManager.isAstroLoop(persistence)) {
                    // Emergency Shield — auto-equipped; mirrors the Time Crystal tile
                    canvas.drawRoundRect(rect, 6f, 6f, tileBgPaint)
                    tileBorderPaint.color = CrystalPalette.MID   // icy cyan border
                    canvas.drawRoundRect(rect, 6f, 6f, tileBorderPaint)

                    // Icon — top ~33%
                    val shieldBitmap = IconCache.getStoreIcon("emergency_shield")
                    if (shieldBitmap != null) {
                        val iconSize = tileSize * 0.33f
                        val iconLeft = rect.centerX() - iconSize / 2f
                        val iconTop = rect.top + tileSize * 0.05f
                        canvas.drawBitmap(shieldBitmap, null,
                            RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize), bitmapPaint)
                    }

                    // Name
                    textPaint.textSize = (tileSize * 0.17f).coerceIn(14f, 24f)
                    textPaint.color = CrystalPalette.ICE
                    canvas.drawText("Emergency Shield", rect.centerX(), rect.top + tileSize * 0.52f, textPaint)

                    // Description — aligns with the Time Crystal description row
                    crystalDescPaint.textSize = (tileSize * 0.16f).coerceIn(13f, 22f)
                    crystalDescPaint.color = CrystalPalette.DEEP
                    canvas.drawText("Survive a lethal hit", rect.centerX(), rect.top + tileSize * 0.62f, crystalDescPaint)

                    // Auto-equipped — aligns with the EQUIPPED row on the Time Crystal tile
                    costPaint.textSize = (tileSize * 0.16f).coerceIn(13f, 22f)
                    costPaint.color = 0xFF44FF44.toInt()
                    canvas.drawText("EQUIPPED", rect.centerX(), rect.top + tileSize * 0.88f, costPaint)
                    continue
                }
            } else {
                upgradeRects.add(rect)
            }

            // Tile background
            canvas.drawRoundRect(rect, 6f, 6f, tileBgPaint)

            if (tile.isNgPlus) {
                val corrupted = StoryStateManager.isCorrupted(persistence)
                val crystalUnlocked = persistence.isCrystalUnlocked()

                if (!corrupted) {
                    // Not corrupted — mystery tile
                    tileBorderPaint.color = 0xFF444444.toInt()
                    canvas.drawRoundRect(rect, 6f, 6f, tileBorderPaint)
                    textPaint.textSize = (tileSize * 0.25f).coerceIn(16f, 28f)
                    // The same grey as the tile effect/description row ("+10% yen"). The shipped
                    // 0xFF555555 measured 2.29:1 on this background, under the 3:1 floor; this is
                    // 7.06:1. Both mystery branches must stay identical or the tile visibly
                    // shifts when the story state flips.
                    textPaint.color = 0xFFAAAAAA.toInt()
                    canvas.drawText("?", rect.centerX(), rect.centerY() + tileSize * 0.05f, textPaint)
                } else if (!crystalUnlocked || state.awaitingCrystalReveal) {
                    // Corrupted but crystal not unlocked — unknown tile (matches non-corrupted "?" style)
                    tileBorderPaint.color = 0xFF444444.toInt()
                    canvas.drawRoundRect(rect, 6f, 6f, tileBorderPaint)
                    textPaint.textSize = (tileSize * 0.25f).coerceIn(16f, 28f)
                    // The same grey as the tile effect/description row ("+10% yen"). The shipped
                    // 0xFF555555 measured 2.29:1 on this background, under the 3:1 floor; this is
                    // 7.06:1. Both mystery branches must stay identical or the tile visibly
                    // shifts when the story state flips.
                    textPaint.color = 0xFFAAAAAA.toInt()
                    canvas.drawText("?", rect.centerX(), rect.centerY() + tileSize * 0.05f, textPaint)
                } else {
                    // Corrupted + crystal unlocked — show Time Crystal tile (auto-equipped)
                    tileBorderPaint.color = CrystalPalette.MID   // icy cyan border
                    canvas.drawRoundRect(rect, 6f, 6f, tileBorderPaint)

                    // Crystal icon bitmap
                    val crystalBitmap = IconCache.getStoreIcon(null)
                    if (crystalBitmap != null) {
                        val iconSize = tileSize * 0.33f
                        val iconLeft = rect.centerX() - iconSize / 2f
                        val iconTop = rect.top + tileSize * 0.05f
                        canvas.drawBitmap(crystalBitmap, null,
                            RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize), bitmapPaint)
                    }

                    // Name
                    textPaint.textSize = (tileSize * 0.17f).coerceIn(14f, 24f)
                    textPaint.color = CrystalPalette.ICE
                    canvas.drawText("TIME CRYSTAL", rect.centerX(), rect.top + tileSize * 0.52f, textPaint)

                    // Description — aligns with effect text row on regular tiles
                    crystalDescPaint.textSize = (tileSize * 0.16f).coerceIn(13f, 22f)
                    crystalDescPaint.color = CrystalPalette.DEEP
                    canvas.drawText("Astro's fate", rect.centerX(), rect.top + tileSize * 0.62f, crystalDescPaint)

                    // Auto-equipped — aligns with MAX/cost row on regular tiles
                    costPaint.textSize = (tileSize * 0.16f).coerceIn(13f, 22f)
                    costPaint.color = 0xFF44FF44.toInt()
                    canvas.drawText("EQUIPPED", rect.centerX(), rect.top + tileSize * 0.88f, costPaint)
                }
            } else {
                // Border
                tileBorderPaint.color = 0xFFCC8844.toInt()
                canvas.drawRoundRect(rect, 6f, 6f, tileBorderPaint)
                val currentLevel = persistence.getUpgradeLevel(tile.id!!)
                val maxLevel = 5

                // Icon — top ~33% of tile
                val iconBitmap = IconCache.getStoreIcon(tile.id)
                if (iconBitmap != null) {
                    val iconSize = tileSize * 0.33f
                    val iconLeft = rect.centerX() - iconSize / 2f
                    val iconTop = rect.top + tileSize * 0.05f
                    // ITEM 24: always full alpha, no dimming on max
                    bitmapPaint.alpha = 255
                    canvas.drawBitmap(
                        iconBitmap, null,
                        RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize),
                        bitmapPaint
                    )
                    bitmapPaint.alpha = 255
                }

                // Name — ~50% from top (ITEM 25: font size ~21% larger: 0.14 -> 0.17, coerceIn 12/18 -> 14/22)
                textPaint.textSize = (tileSize * 0.17f).coerceIn(14f, 24f)
                // ITEM 24: always white, no gray when maxed
                textPaint.color = 0xFFFFFFFF.toInt()
                canvas.drawText(tile.name, rect.centerX(), rect.top + tileSize * 0.52f, textPaint)

                // Effect text — ~62% from top (ITEM 4: new effect description row)
                val effectTextPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = FontManager.getRegular()
                    textSize = (tileSize * 0.16f).coerceIn(13f, 22f)
                    color = 0xFFAAAAAA.toInt()
                }
                canvas.drawText(tile.effect, rect.centerX(), rect.top + tileSize * 0.62f, effectTextPaint)

                // Level pips — ~73% from top (ITEM 25: pip radius ~20% larger: 0.03->0.036, coerceIn 3/5 -> 3.6/6)
                val pipRadius = (tileSize * 0.036f).coerceIn(3.6f, 6f)
                val pipSpacing = pipRadius * 3.2f
                val pipsWidth = (maxLevel - 1) * pipSpacing
                val pipStartX = rect.centerX() - pipsWidth / 2f
                val pipY = rect.top + tileSize * 0.73f

                for (p in 0 until maxLevel) {
                    val px = pipStartX + p * pipSpacing
                    if (p < currentLevel) {
                        pipPaint.color = 0xFFCC8844.toInt()
                        pipPaint.style = android.graphics.Paint.Style.FILL
                    } else {
                        pipPaint.color = 0xFF333344.toInt()
                        pipPaint.style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawCircle(px, pipY, pipRadius, pipPaint)
                }

                // Cost / MAX — ~88% from top (ITEM 25: font size ~23% larger: 0.13->0.16, coerceIn 11/16 -> 13/20)
                if (currentLevel >= maxLevel) {
                    costPaint.textSize = (tileSize * 0.16f).coerceIn(13f, 22f)
                    costPaint.color = 0xFF44FF44.toInt()
                    canvas.drawText("MAX", rect.centerX(), rect.top + tileSize * 0.88f, costPaint)
                } else {
                    val cost = PersistenceManager.getUpgradeCost(currentLevel)
                    costPaint.textSize = (tileSize * 0.16f).coerceIn(13f, 22f)
                    costPaint.color = 0xFFFFD700.toInt()
                    canvas.drawText(GameConfig.formatYen(cost), rect.centerX(), rect.top + tileSize * 0.88f, costPaint)
                }
            }
        }

        // Miniature slot machine position on store walkway (always at base position).
        // Read side of the walker band: HangarState.getPilotWorldTarget(2) puts the pilot at
        // `2 * roomWidth + margin + 0.1f * walkable`, i.e. exactly here in room-local space.
        // Measured against the ROOM, like the write side — below the gate roomWidth is the
        // screen width, so this is the shipped `pilotScreenWidth` formula unchanged.
        val margin = rw * 0.1f
        val walkable = rw * 0.8f
        val storeTargetX = margin + 0.1f * walkable
        drawMiniSlotMachine(canvas, storeTargetX, walkwayY)

        // Corrupted Astro at slot machine (auto-gambling during corruption)
        if (state.astroAtSlotMachine) {
            val astroX = storeTargetX
            val astroY = walkwayY - 8f
            val time = System.currentTimeMillis()

            // Darkened Astro dot (Astro is red 0xFFDD3333, corrupted = 50% brightness)
            val astroDotPaint = Paint().apply {
                color = StoryStateManager.corruptColor(0xFFDD3333.toInt())
                style = Paint.Style.FILL
            }
            canvas.drawCircle(astroX, astroY - 6f, 5f, astroDotPaint)

            // Shield aura glow when crystal not yet unlocked or during reveal glow phase
            if (!persistence.isCrystalUnlocked() || state.crystalRevealPhase == HangarState.CrystalRevealPhase.GLOW) {
                val pulse = (0.4f + 0.6f * ((sin((time % 10000L) / 1000.0) + 1f) / 2f)).toFloat()
                val glowPaint = Paint().apply {
                    // color set per-ring below (MID outer / ICE inner)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                // Outer ring (shield boundary)
                glowPaint.color = CrystalPalette.MID
                glowPaint.alpha = (pulse * 120).toInt()
                canvas.drawCircle(astroX, astroY - 6f, 10f + pulse * 2f, glowPaint)
                // Inner ring (brighter)
                glowPaint.color = CrystalPalette.ICE
                glowPaint.alpha = (pulse * 200).toInt()
                glowPaint.strokeWidth = 1.5f
                canvas.drawCircle(astroX, astroY - 6f, 7f + pulse * 1f, glowPaint)
            }

            // Orb travel animation: corkscrew from Astro up to the crystal tile
            if (state.crystalRevealPhase == HangarState.CrystalRevealPhase.ORB_TRAVEL) {
                val t = (state.crystalRevealTimer / CrystalOrbPath.TRAVEL_DURATION).coerceIn(0f, 1f)
                val srcX = astroX
                val srcY = astroY - 6f
                val dstX = crystalTileRect.centerX()
                val dstY = crystalTileRect.centerY()

                val (orbX, orbY) = CrystalOrbPath.position(t, srcX, srcY, dstX, dstY)

                val orbPulse = 0.7f + 0.3f * sin(time / 200.0).toFloat()
                val orbPaint = Paint().apply {
                    color = CrystalPalette.MID   // icy cyan
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                // Trail: fading circles along the corkscrew behind the orb
                for (i in 4 downTo 1) {
                    val trailT = (t - i * 0.04f).coerceAtLeast(0f)
                    val (trailX, trailY) = CrystalOrbPath.position(trailT, srcX, srcY, dstX, dstY)
                    orbPaint.alpha = ((1f - i / 5f) * 60).toInt()
                    canvas.drawCircle(trailX, trailY, 5f - i * 0.8f, orbPaint)
                }
                // Outer glow
                orbPaint.alpha = (orbPulse * 100).toInt()
                canvas.drawCircle(orbX, orbY, 8f, orbPaint)
                // Core
                orbPaint.alpha = (orbPulse * 220).toInt()
                canvas.drawCircle(orbX, orbY, 3f, orbPaint)
            }

            // Flash burst on crystal tile when orb arrives
            if (state.crystalRevealPhase == HangarState.CrystalRevealPhase.FLASH) {
                val ft = (state.crystalRevealTimer / CrystalOrbPath.FLASH_DURATION).coerceIn(0f, 1f)
                val flashRadius = 30f * ft
                val flashAlpha = ((1f - ft) * 255).toInt()
                val flashPaint = Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    alpha = flashAlpha
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(crystalTileRect.centerX(), crystalTileRect.centerY(), flashRadius, flashPaint)
            }
        }

        // Slot machine below walkway
        drawSlotMachine(canvas, state)

        // Reset paints
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.restore()
    }

    private fun drawStoreDecoration(canvas: Canvas) {
        val roomTop = ceilingY + 3f
        val roomBottom = walkwayY
        val time = System.currentTimeMillis()

        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val linePaint = Paint().apply {
            color = 0xFF555566.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val accentPaint = Paint().apply { style = Paint.Style.FILL }

        // =============================================
        // LEFT SIDE — Stacked crates (~35% width)
        // =============================================
        val crateLeft = 10f
        val crateRight = rw * 0.32f

        // Large crate (bottom)
        fillPaint.color = 0xFF1A1A28.toInt()
        val bigCrate = RectF(crateLeft, roomBottom - 35f, crateLeft + 45f, roomBottom - 2f)
        canvas.drawRect(bigCrate, fillPaint)
        linePaint.color = 0xFF3A3A4A.toInt()
        linePaint.strokeWidth = 1f
        canvas.drawRect(bigCrate, linePaint)
        // Cross slats
        canvas.drawLine(bigCrate.left, bigCrate.top, bigCrate.right, bigCrate.bottom, linePaint)
        canvas.drawLine(bigCrate.right, bigCrate.top, bigCrate.left, bigCrate.bottom, linePaint)

        // Medium crate (stacked on top, offset)
        fillPaint.color = 0xFF1E1E2C.toInt()
        val medCrate = RectF(crateLeft + 5f, roomBottom - 60f, crateLeft + 40f, roomBottom - 35f)
        canvas.drawRect(medCrate, fillPaint)
        linePaint.color = 0xFF3A3A4A.toInt()
        canvas.drawRect(medCrate, linePaint)

        // Small open crate (right of big one) with interior glow
        fillPaint.color = 0xFF181828.toInt()
        val openCrate = RectF(crateLeft + 50f, roomBottom - 28f, crateRight, roomBottom - 2f)
        canvas.drawRect(openCrate, fillPaint)
        linePaint.color = 0xFF3A3A4A.toInt()
        canvas.drawRect(openCrate, linePaint)
        // Faint glow inside (contraband)
        val glowPulse = 0.4f + 0.3f * sin(time / 1200.0).toFloat()
        accentPaint.color = 0xFFCC8844.toInt()
        accentPaint.alpha = (glowPulse * 40).toInt()
        canvas.drawRect(openCrate.left + 3f, openCrate.top + 3f, openCrate.right - 3f, openCrate.bottom - 3f, accentPaint)

        // Tall crate at far left (background)
        fillPaint.color = 0xFF161626.toInt()
        val tallCrate = RectF(crateLeft, roomTop + 20f, crateLeft + 22f, roomBottom - 35f)
        canvas.drawRect(tallCrate, fillPaint)
        linePaint.color = 0xFF2A2A3A.toInt()
        canvas.drawRect(tallCrate, linePaint)

        // =============================================
        // CENTER — Bare bulb on wire
        // =============================================
        val centerX = rw * 0.5f

        // Wire from ceiling
        linePaint.color = 0xFF444444.toInt()
        linePaint.strokeWidth = 1f
        canvas.drawLine(centerX, roomTop, centerX, roomTop + 18f, linePaint)

        // Bulb — irregular flicker (dying tube effect)
        val flickerSeed = (time / 80).toInt()
        val flickerHash = (flickerSeed * 2654435761L).toInt()
        val flicker = if ((flickerHash % 17) < 2) 0.2f  // Brief dark flickers
            else 0.65f + 0.35f * sin(time / 300.0).toFloat()

        // Bulb shape (small circle)
        accentPaint.color = 0xFFDDAA55.toInt()
        accentPaint.alpha = (flicker * 220).toInt()
        canvas.drawCircle(centerX, roomTop + 22f, 3f, accentPaint)

        // Light cone below bulb
        accentPaint.alpha = (flicker * 15).toInt()
        val coneHalfWidth = 40f
        val coneBottom = roomBottom - 10f
        val path = android.graphics.Path().apply {
            moveTo(centerX - 4f, roomTop + 24f)
            lineTo(centerX - coneHalfWidth, coneBottom)
            lineTo(centerX + coneHalfWidth, coneBottom)
            lineTo(centerX + 4f, roomTop + 24f)
            close()
        }
        canvas.drawPath(path, accentPaint)

        // =============================================
        // Loose cables from ceiling
        // =============================================
        linePaint.color = 0xFF333344.toInt()
        linePaint.strokeWidth = 1f
        val sway1 = sin(time / 2000.0).toFloat() * 3f
        val sway2 = sin(time / 2500.0 + 1.0).toFloat() * 4f

        // Cable 1
        val cable1X = rw * 0.25f
        canvas.drawLine(cable1X, roomTop, cable1X + sway1, roomTop + 25f, linePaint)
        canvas.drawLine(cable1X + sway1, roomTop + 25f, cable1X + sway1 * 1.5f, roomTop + 40f, linePaint)

        // Cable 2
        val cable2X = rw * 0.72f
        canvas.drawLine(cable2X, roomTop, cable2X + sway2, roomTop + 30f, linePaint)
        canvas.drawLine(cable2X + sway2, roomTop + 30f, cable2X + sway2 * 1.3f, roomTop + 50f, linePaint)

        // =============================================
        // RIGHT SIDE — Tarp over shelves
        // =============================================
        val tarpLeft = rw * 0.68f
        val tarpRight = rw - 10f

        // Shelves behind tarp (partially visible)
        fillPaint.color = 0xFF1A1A28.toInt()
        canvas.drawRect(tarpLeft + 5f, roomTop + 15f, tarpRight - 3f, roomTop + 17f, fillPaint)
        canvas.drawRect(tarpLeft + 5f, roomTop + 40f, tarpRight - 3f, roomTop + 42f, fillPaint)

        // Tarp (rough triangular drape)
        fillPaint.color = 0xFF2A2820.toInt()
        fillPaint.alpha = 180
        val tarpPath = android.graphics.Path().apply {
            moveTo(tarpRight - 2f, roomTop + 8f)
            lineTo(tarpRight - 2f, roomBottom - 15f)
            lineTo(tarpLeft + 15f, roomBottom - 8f)
            lineTo(tarpLeft + 8f, roomTop + 30f)
            close()
        }
        canvas.drawPath(tarpPath, fillPaint)
        fillPaint.alpha = 255
        // Tarp edge
        linePaint.color = 0xFF3A3830.toInt()
        linePaint.strokeWidth = 1f
        canvas.drawLine(tarpRight - 2f, roomTop + 8f, tarpLeft + 8f, roomTop + 30f, linePaint)
        canvas.drawLine(tarpLeft + 8f, roomTop + 30f, tarpLeft + 15f, roomBottom - 8f, linePaint)

        // =============================================
        // Sign — "BLACK MARKET"
        // =============================================
        val signX = rw * 0.5f
        val signY = roomTop + 12f
        // BLACK MARKET — crooked, flickering red neon
        canvas.save()
        canvas.rotate(-2f, signX, signY)
        textPaint.textSize = 16f
        // Irregular neon flicker
        val neonFlicker = if ((flickerHash % 23) < 3) 0.3f
            else 0.7f + 0.3f * sin(time / 400.0).toFloat()
        textPaint.color = 0xFFCC4422.toInt()
        textPaint.alpha = (neonFlicker * 255).toInt()
        canvas.drawText("BLACK MARKET", signX, signY, textPaint)
        // Neon glow
        accentPaint.color = 0xFFCC4422.toInt()
        accentPaint.alpha = (neonFlicker * 25).toInt()
        canvas.drawRoundRect(RectF(signX - 60f, signY - 14f, signX + 60f, signY + 5f), 3f, 3f, accentPaint)
        canvas.restore()

        // =============================================
        // Floor details near walkway
        // =============================================
        accentPaint.color = 0xFF333340.toInt()
        accentPaint.alpha = 255
        val floorRandom = Random(42)
        // Scattered bolts
        for (b in 0 until 8) {
            val bx = 20f + floorRandom.nextFloat() * (rw - 40f)
            val by = roomBottom - 4f - floorRandom.nextFloat() * 6f
            canvas.drawCircle(bx, by, 0.8f + floorRandom.nextFloat() * 0.5f, accentPaint)
        }
        // Spilled canister
        fillPaint.color = 0xFF2A2A35.toInt()
        val canX = rw * 0.42f
        canvas.drawRoundRect(RectF(canX, roomBottom - 6f, canX + 8f, roomBottom - 2f), 1f, 1f, fillPaint)
        // Scuff marks
        linePaint.color = 0xFF252535.toInt()
        linePaint.strokeWidth = 2f
        canvas.drawLine(rw * 0.55f, roomBottom - 2f, rw * 0.60f, roomBottom - 3f, linePaint)
        canvas.drawLine(rw * 0.20f, roomBottom - 1f, rw * 0.24f, roomBottom - 3f, linePaint)

        // Reset paint state
        textPaint.alpha = 255
        textPaint.color = 0xFFFFFFFF.toInt()
    }

    // Slot machine symbol IDs
    companion object {
        const val SYM_YEN = 0
        const val SYM_STAR = 1
        const val SYM_DIAMOND = 2
        const val SYM_ROCKET = 3
        const val SYM_BOLT = 4
        const val SYM_WRENCH = 5
        const val SYMBOL_COUNT = 6

        private val SYMBOL_NAMES = arrayOf("yen", "star", "diamond", "rocket", "bolt", "gear")
        fun getSymbolName(index: Int): String = SYMBOL_NAMES.getOrElse(index) { "?" }
    }

    var spinButtonRect = RectF()

    private fun drawMiniSlotMachine(canvas: Canvas, pilotX: Float, walkwayY: Float) {
        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Sized relative to the pilot character (~2x pilot height)
        val machineW = 18f
        val machineH = 32f
        val machineCX = pilotX
        val machineBottom = walkwayY
        val machineTop = machineBottom - machineH
        val left = machineCX - machineW / 2f
        val right = machineCX + machineW / 2f

        // Cabinet body
        fillPaint.color = 0xFF22223A.toInt()
        canvas.drawRoundRect(RectF(left, machineTop, right, machineBottom), 3f, 3f, fillPaint)
        linePaint.color = 0xFF665544.toInt()
        canvas.drawRoundRect(RectF(left, machineTop, right, machineBottom), 3f, 3f, linePaint)

        // Three tiny reel rectangles
        val reelW = machineW * 0.22f
        val reelH = machineH * 0.25f
        val reelY = machineTop + machineH * 0.2f
        val reelGap = (machineW - reelW * 3) / 4f
        fillPaint.color = 0xFF0A0A16.toInt()
        for (r in 0 until 3) {
            val rx = left + reelGap + r * (reelW + reelGap)
            canvas.drawRect(rx, reelY, rx + reelW, reelY + reelH, fillPaint)
        }

        // Faint screen glow below reels (the CRT)
        fillPaint.color = 0xFFCC8844.toInt()
        fillPaint.alpha = 35
        val glowTop = reelY + reelH + machineH * 0.06f
        val glowBottom = glowTop + machineH * 0.15f
        canvas.drawRoundRect(RectF(left + machineW * 0.12f, glowTop, right - machineW * 0.12f, glowBottom), 1f, 1f, fillPaint)
        fillPaint.alpha = 255
    }

    private fun drawSlotMachine(canvas: Canvas, state: HangarState) {
        storeButtonRects.clear()

        val time = System.currentTimeMillis()
        val machineTop = walkwayY + 15f
        val machineBottom = screenHeight * 0.92f
        val machineHeight = machineBottom - machineTop

        // Replicate grid-left calculation so machine aligns with upgrade grid
        val gridCols = 3
        val gridRows = 3
        val gridCardGap = 10f
        val gridPadding = 16f
        val gridAvailWidth = content.width - gridPadding * 2f
        val gridTotalGapX = gridCardGap * (gridCols - 1)
        val gridTotalGapY = gridCardGap * (gridRows - 1)
        val gridMaxTileWidth = (gridAvailWidth - gridTotalGapX) / gridCols
        val gridMaxTileHeight = ((walkwayY - 20f) - 70f - gridTotalGapY) / gridRows
        val gridTileSize = gridMaxTileWidth.coerceAtMost(gridMaxTileHeight)
        val gridTotalWidth = gridCols * gridTileSize + gridTotalGapX
        // Room-local, matching the grid above — the machine is drawn inside the same translate.
        val gridStartX = contentLeftInRoom + (content.width - gridTotalWidth) / 2f

        val machineLeft = gridStartX
        val machineRight = gridStartX + gridTotalWidth  // machine width == grid width, content-centered

        // Right panel (payout table) — scales with the machine, not the full screen,
        // so its proportion stays constant across aspect ratios.
        val rightPanelWidth = gridTotalWidth * 0.15f

        // Left gap for maintenance hatch panel
        val machineWidth = machineRight - machineLeft
        val leftGap = machineWidth * 0.12f
        val firstReelLeft = machineLeft + leftGap

        // Reel area boundaries
        val reelAreaRight = machineRight - rightPanelWidth - 5f
        val reelAreaLeft = firstReelLeft - 10f

        // Three reels
        val reelGap = 8f
        val reelAreaTop = machineTop + 10f
        val reelAreaBottom = machineTop + machineHeight * 0.55f
        val reelTotalWidth = reelAreaRight - 10f - firstReelLeft
        val reelWidth = (reelTotalWidth - reelGap * 2) / 3f
        val reelHeight = reelAreaBottom - reelAreaTop

        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        // Ambient glow behind machine
        fillPaint.color = 0xFFCC8844.toInt()
        fillPaint.alpha = 12
        canvas.drawRoundRect(RectF(machineLeft - 10f, machineTop - 5f, machineRight + 10f, machineBottom + 5f), 10f, 10f, fillPaint)
        fillPaint.alpha = 255

        // Machine body
        fillPaint.color = 0xFF1A1A2E.toInt()
        canvas.drawRoundRect(RectF(machineLeft, machineTop, machineRight, machineBottom), 6f, 6f, fillPaint)
        linePaint.color = 0xFF4A3A2A.toInt()
        canvas.drawRoundRect(RectF(machineLeft, machineTop, machineRight, machineBottom), 6f, 6f, linePaint)

        for (r in 0 until 3) {
            val rx = firstReelLeft + r * (reelWidth + reelGap)
            val ry = reelAreaTop
            val reelRect = RectF(rx, ry, rx + reelWidth, ry + reelHeight)

            // Reel background
            fillPaint.color = 0xFF0E0E1A.toInt()
            canvas.drawRoundRect(reelRect, 4f, 4f, fillPaint)

            // Reel border
            linePaint.color = 0xFF3A3A4A.toInt()
            linePaint.strokeWidth = 1f
            canvas.drawRoundRect(reelRect, 4f, 4f, linePaint)

            // Draw symbol
            val symbolIndex: Int
            val isSpinning = state.isSpinning && time < state.reelStopTimes[r]

            if (isSpinning) {
                // Spinning — cycle through symbols rapidly
                val elapsed = time - (state.reelStopTimes[r] - 1500L) // ~1.5s spin time
                val cycleSpeed = 80L // ms per symbol change
                symbolIndex = ((elapsed / cycleSpeed) % SYMBOL_COUNT).toInt()
            } else if (state.isSpinning || state.spinResultTime > 0) {
                // Stopped or post-spin — show final value
                symbolIndex = state.reelValues[r]
            } else {
                // Idle — show a default face
                symbolIndex = listOf(SYM_STAR, SYM_YEN, SYM_DIAMOND)[r]
            }

            val symCenterX = rx + reelWidth / 2f
            val symCenterY = ry + reelHeight / 2f
            drawSymbol(canvas, symbolIndex, symCenterX, symCenterY, reelHeight * 0.3f, isSpinning)
        }

        // Pay line (horizontal across all reels)
        linePaint.color = 0xFFCC8844.toInt()
        linePaint.strokeWidth = 1f
        linePaint.alpha = 120
        val payLineY = reelAreaTop + reelHeight / 2f
        canvas.drawLine(reelAreaLeft + 5f, payLineY, reelAreaRight - 5f, payLineY, linePaint)
        linePaint.alpha = 255

        // --- Below reels: Result Screen (top) + Spin Button (bottom) ---
        // Three equal gaps: reels→result, result→button, button→machine bottom
        val belowReelsSpace = machineBottom - reelAreaBottom
        val boxHeight = machineHeight * 0.12f
        val reelCenterX = (reelAreaLeft + reelAreaRight) / 2f
        val buttonWidth = (reelAreaRight - reelAreaLeft) * 0.80f
        val totalBoxSpace = boxHeight * 2
        val gap = (belowReelsSpace - totalBoxSpace) / 3f

        val resultTop = reelAreaBottom + gap
        val resultBottom = resultTop + boxHeight
        val resultLeft = reelCenterX - buttonWidth / 2f
        val resultRight = reelCenterX + buttonWidth / 2f
        val resultRect = RectF(resultLeft, resultTop, resultRight, resultBottom)

        val buttonTop = resultBottom + gap
        val buttonBottom = buttonTop + boxHeight
        val buttonRect = RectF(resultLeft, buttonTop, resultRight, buttonBottom)

        spinButtonRect = buttonRect
        storeButtonRects.add(buttonRect)

        // --- CRT Result Screen ---
        // Bezel
        fillPaint.color = 0xFF2A2A35.toInt()
        canvas.drawRoundRect(RectF(resultLeft - 3f, resultTop - 3f, resultRight + 3f, resultBottom + 3f), 5f, 5f, fillPaint)

        // Screen background with amber tint
        fillPaint.color = 0xFF121218.toInt()
        canvas.drawRoundRect(resultRect, 4f, 4f, fillPaint)

        // Scanlines
        linePaint.color = 0xFF000000.toInt()
        linePaint.alpha = 20
        linePaint.strokeWidth = 1f
        var scanY = resultTop + 2f
        while (scanY < resultBottom) {
            canvas.drawLine(resultLeft + 2f, scanY, resultRight - 2f, scanY, linePaint)
            scanY += 3f
        }
        linePaint.alpha = 255

        // Result content
        val resultAge = time - state.spinResultTime
        val fontSize = (boxHeight * 0.38f).coerceIn(12f, 20f)
        val symbolSize = boxHeight * 0.55f

        val resultDuration = when {
            state.spinResultUpgrade != null -> 5000L
            state.spinResultSymbol == SYM_ROCKET -> 4000L
            else -> 3000L
        }
        val fadeDuration = 1000L

        if (state.spinResultTime > 0 && resultAge < resultDuration) {
            val fadeAlpha = if (resultAge > resultDuration - fadeDuration) ((resultDuration - resultAge) / fadeDuration.toFloat()) else 1f

            if (state.spinResultUpgrade != null) {
                // Jackpot — pulsing amber glow + rocket + scrolling text
                val pulse = (0.7f + 0.3f * sin(resultAge * 0.008f).toFloat())
                fillPaint.color = 0xFFFFAA22.toInt()
                fillPaint.alpha = (fadeAlpha * pulse * 40).toInt()
                canvas.drawRoundRect(RectF(resultLeft - 6f, resultTop - 6f, resultRight + 6f, resultBottom + 6f), 8f, 8f, fillPaint)
                fillPaint.alpha = 255

                drawSymbol(canvas, SYM_ROCKET, resultLeft + symbolSize * 0.8f, resultRect.centerY(), symbolSize, false)
                textPaint.textSize = fontSize
                textPaint.color = 0xFFFFAA22.toInt()
                textPaint.alpha = (fadeAlpha * 255).toInt()

                // Scroll from "JACKPOT!" to upgrade name
                val textX = resultRect.centerX() + symbolSize * 0.2f
                val textY = resultRect.centerY() + fontSize * 0.15f
                if (resultAge < 1500L) {
                    canvas.drawText("JACKPOT!", textX, textY, textPaint)
                } else if (resultAge < 2000L) {
                    // Scroll transition — slide up
                    val t = (resultAge - 1500L) / 500f
                    val offset = t * fontSize * 1.2f
                    textPaint.alpha = ((1f - t) * fadeAlpha * 255).toInt()
                    canvas.drawText("JACKPOT!", textX, textY - offset, textPaint)
                    textPaint.alpha = (t * fadeAlpha * 255).toInt()
                    canvas.drawText(state.spinResultUpgrade!!, textX, textY - offset + fontSize * 1.2f, textPaint)
                } else {
                    canvas.drawText(state.spinResultUpgrade!!, textX, textY, textPaint)
                }

                // Full-screen flash
                if (resultAge < 300L) {
                    fillPaint.color = 0xFFFFAA22.toInt()
                    fillPaint.alpha = ((1f - resultAge / 300f) * 30).toInt()
                    canvas.drawRect(0f, 0f, screenWidth, screenHeight, fillPaint)
                    fillPaint.alpha = 255
                }
            } else if (state.spinResultSymbol == SYM_ROCKET) {
                // Jackpot fallback — all upgrades maxed, gives 10k yen, shows rocket (not diamond)
                val pulse = (0.7f + 0.3f * sin(resultAge * 0.008f).toFloat())
                fillPaint.color = 0xFFFFAA22.toInt()
                fillPaint.alpha = (fadeAlpha * pulse * 40).toInt()
                canvas.drawRoundRect(RectF(resultLeft - 6f, resultTop - 6f, resultRight + 6f, resultBottom + 6f), 8f, 8f, fillPaint)
                fillPaint.alpha = 255

                drawSymbol(canvas, SYM_ROCKET, resultLeft + symbolSize * 0.8f, resultRect.centerY(), symbolSize, false)
                textPaint.textSize = fontSize
                textPaint.color = 0xFFFFAA22.toInt()
                textPaint.alpha = (fadeAlpha * 255).toInt()
                val textX = resultRect.centerX() + symbolSize * 0.2f
                val textY = resultRect.centerY() + fontSize * 0.15f
                if (resultAge < 1500L) {
                    canvas.drawText("JACKPOT!", textX, textY, textPaint)
                } else {
                    canvas.drawText("+${state.spinResultYen}\u00A5", textX, textY, textPaint)
                }

                // Full-screen flash
                if (resultAge < 300L) {
                    fillPaint.color = 0xFFFFAA22.toInt()
                    fillPaint.alpha = ((1f - resultAge / 300f) * 30).toInt()
                    canvas.drawRect(0f, 0f, screenWidth, screenHeight, fillPaint)
                    fillPaint.alpha = 255
                }
            } else if (state.spinResultYen >= 2000) {
                // Diamond win — strong cyan glow
                fillPaint.color = 0xFF44DDAA.toInt()
                fillPaint.alpha = (fadeAlpha * 35).toInt()
                canvas.drawRoundRect(RectF(resultLeft - 5f, resultTop - 5f, resultRight + 5f, resultBottom + 5f), 7f, 7f, fillPaint)
                fillPaint.alpha = 255

                drawSymbol(canvas, SYM_DIAMOND, resultLeft + symbolSize * 0.8f, resultRect.centerY(), symbolSize, false)
                textPaint.textSize = fontSize
                textPaint.color = 0xFF44DDAA.toInt()
                textPaint.alpha = (fadeAlpha * 255).toInt()
                canvas.drawText("+${state.spinResultYen}\u00A5", resultRect.centerX() + symbolSize * 0.2f, resultRect.centerY() + fontSize * 0.15f, textPaint)
            } else if (state.spinResultYen >= 700) {
                // Star win — subtle gold glow
                fillPaint.color = 0xFFFFD700.toInt()
                fillPaint.alpha = (fadeAlpha * 20).toInt()
                canvas.drawRoundRect(RectF(resultLeft - 3f, resultTop - 3f, resultRight + 3f, resultBottom + 3f), 6f, 6f, fillPaint)
                fillPaint.alpha = 255

                drawSymbol(canvas, SYM_STAR, resultLeft + symbolSize * 0.8f, resultRect.centerY(), symbolSize, false)
                textPaint.textSize = fontSize
                textPaint.color = 0xFFFFD700.toInt()
                textPaint.alpha = (fadeAlpha * 255).toInt()
                canvas.drawText("+${state.spinResultYen}\u00A5", resultRect.centerX() + symbolSize * 0.2f, resultRect.centerY() + fontSize * 0.15f, textPaint)
            } else if (state.spinResultYen > 0) {
                // Small win (100, 75, 50) — dim gray or white, no glow
                val matchedSymbol = when (state.spinResultYen) {
                    100 -> SYM_YEN
                    75 -> SYM_BOLT
                    else -> SYM_WRENCH
                }
                val textColor = if (state.spinResultYen >= 100) 0xFFFFFFFF.toInt() else 0xFF888888.toInt()

                drawSymbol(canvas, matchedSymbol, resultLeft + symbolSize * 0.8f, resultRect.centerY(), symbolSize, false)
                textPaint.textSize = fontSize
                textPaint.color = textColor
                textPaint.alpha = (fadeAlpha * 255).toInt()
                canvas.drawText("+${state.spinResultYen}\u00A5", resultRect.centerX() + symbolSize * 0.2f, resultRect.centerY() + fontSize * 0.15f, textPaint)
            } else {
                // Mixed — "No luck" dim gray
                textPaint.textSize = fontSize
                textPaint.color = 0xFF555555.toInt()
                textPaint.alpha = (fadeAlpha * 200).toInt()
                canvas.drawText("No luck", resultRect.centerX(), resultRect.centerY() + fontSize * 0.15f, textPaint)
            }
            textPaint.alpha = 255
        } else {
            // Idle state — faint static flicker
            val flicker = kotlin.random.Random.nextFloat()
            if (flicker < 0.03f) {
                fillPaint.color = 0xFF181820.toInt()
                fillPaint.alpha = 40
                val flickerY = resultTop + kotlin.random.Random.nextFloat() * (resultBottom - resultTop)
                canvas.drawRect(resultLeft + 2f, flickerY, resultRight - 2f, flickerY + 2f, fillPaint)
                fillPaint.alpha = 255
            }
        }

        // --- Spin Button ---
        val canSpin = !state.isSpinning && state.actualYen >= 100
        fillPaint.color = if (canSpin) 0xFF2A2A40.toInt() else 0xFF1A1A24.toInt()
        canvas.drawRoundRect(buttonRect, 4f, 4f, fillPaint)
        linePaint.color = if (canSpin) 0xFFCC8844.toInt() else 0xFF333344.toInt()
        linePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(buttonRect, 4f, 4f, linePaint)

        textPaint.textSize = fontSize
        textPaint.color = if (canSpin) 0xFFFFDD88.toInt() else 0xFF555555.toInt()
        val buttonLabel = if (state.actualYen < 100 && !state.isSpinning) "NO \u00A5" else "SPIN \u2014 100\u00A5"
        canvas.drawText(buttonLabel, buttonRect.centerX(), buttonRect.centerY() + fontSize * 0.35f, textPaint)

        // --- Payout table (right panel) ---
        val payoutLeft = machineRight - rightPanelWidth + 4f
        val payoutRight = machineRight - 4f
        val payoutTop = reelAreaTop
        val payoutCenterX = (payoutLeft + payoutRight) / 2f
        val lineH = reelHeight / 7f

        val payoutFontSize = (boxHeight * 0.38f).coerceIn(12f, 20f)

        val payoutTitlePaint = Paint().apply {
            textSize = payoutFontSize
            typeface = FontManager.getRegular()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Title
        payoutTitlePaint.color = 0xFFAA8855.toInt()
        canvas.drawText("PAYOUTS", payoutCenterX, payoutTop + lineH * 0.7f, payoutTitlePaint)

        // Payout lines: 3 small symbols on left, value on right
        val payoutValuePaint = Paint().apply {
            textSize = payoutFontSize
            typeface = FontManager.getRegular()
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val payoutSymbols = listOf(SYM_ROCKET, SYM_DIAMOND, SYM_STAR, SYM_YEN, SYM_BOLT, SYM_WRENCH)
        val payoutValues = listOf("BONUS", "2000", "700", "100", "75", "50")
        // Reserve room for the widest value text ("BONUS" is wider than any number,
        // and wider still under Exo 2) so the 3-symbol cluster can never clip it.
        // The cluster spans 3.6 * symbol size at 1.3x spacing; shrink the symbols
        // only if the panel is too narrow, otherwise keep their normal size.
        val maxValueWidth = payoutValues.maxOf { payoutValuePaint.measureText(it) }
        val symbolsLeft = payoutLeft + 4f
        val symbolsAvail = (payoutRight - 2f - maxValueWidth - 6f) - symbolsLeft
        val payoutSymSize = (lineH * 0.35f).coerceAtMost((symbolsAvail / 3.6f).coerceAtLeast(1f))
        val symbolSpacing = payoutSymSize * 1.3f
        for (i in payoutSymbols.indices) {
            val py = payoutTop + lineH * (i + 1.7f)
            // Draw 3 symbols side by side, starting from left
            val symbolsStartX = symbolsLeft + payoutSymSize * 0.5f
            for (s in 0 until 3) {
                val sx = symbolsStartX + s * symbolSpacing
                drawSymbol(canvas, payoutSymbols[i], sx, py - payoutFontSize * 0.3f, payoutSymSize, false)
            }
            payoutValuePaint.color = 0xFF888877.toInt()
            canvas.drawText(payoutValues[i], payoutRight - 2f, py, payoutValuePaint)
        }

        // --- Maintenance hatch (left panel) — centered in gap between machineLeft and firstReelLeft ---
        val hatchCenterX = (machineLeft + firstReelLeft) / 2f
        val hatchW = (firstReelLeft - machineLeft) - 8f
        val hatchH = machineHeight * 0.30f
        val hatchCenterY = (reelAreaTop + reelAreaBottom) / 2f
        val hatchTopY = hatchCenterY - hatchH / 2f
        val hatchLeft = hatchCenterX - hatchW / 2f
        val hatchR = RectF(hatchLeft, hatchTopY, hatchLeft + hatchW, hatchTopY + hatchH)
        state.hatchRect = hatchR

        // Panel background
        fillPaint.color = 0xFF151525.toInt()
        canvas.drawRoundRect(hatchR, 3f, 3f, fillPaint)

        // Panel border — subtle seams
        linePaint.color = 0xFF2A2A3A.toInt()
        linePaint.strokeWidth = 1f
        canvas.drawRoundRect(hatchR, 3f, 3f, linePaint)

        // Small screws at corners
        fillPaint.color = 0xFF333344.toInt()
        val sr = 2f
        canvas.drawCircle(hatchLeft + 5f, hatchTopY + 5f, sr, fillPaint)
        canvas.drawCircle(hatchLeft + hatchW - 5f, hatchTopY + 5f, sr, fillPaint)
        canvas.drawCircle(hatchLeft + 5f, hatchTopY + hatchH - 5f, sr, fillPaint)
        canvas.drawCircle(hatchLeft + hatchW - 5f, hatchTopY + hatchH - 5f, sr, fillPaint)

        if (state.hatchOpen) {
            // Hatch ajar — draw slightly offset/open
            fillPaint.color = 0xFF0E0E1A.toInt()
            val openRect = RectF(hatchLeft - 2f, hatchTopY + 2f, hatchLeft + hatchW - 4f, hatchTopY + hatchH - 2f)
            canvas.drawRoundRect(openRect, 3f, 3f, fillPaint)

            // Paper peeking out — extends beyond hatch for a hastily-stashed look
            fillPaint.color = 0xFFCCBB99.toInt()
            val paperTopY = hatchTopY + hatchH * 0.3f - 3f
            val paperBottomY = hatchTopY + hatchH * 0.7f
            val paperLeftX = hatchLeft + 3f
            val paperRightX = hatchLeft + hatchW - 6f + 2f

            // Rotate paper ~6 degrees for a crooked, hastily-stashed look
            canvas.save()
            val paperCenterX = (paperLeftX + paperRightX) / 2f
            val paperCenterY = (paperTopY + paperBottomY) / 2f
            canvas.rotate(6f, paperCenterX, paperCenterY)

            canvas.drawRect(paperLeftX, paperTopY, paperRightX, paperBottomY, fillPaint)

            // Paper border
            linePaint.color = 0xFF998866.toInt()
            canvas.drawRect(paperLeftX, paperTopY, paperRightX, paperBottomY, linePaint)

            // Dog-ear fold on top-right corner
            val foldSize = (paperRightX - paperLeftX) * 0.2f
            val foldPath = Path().apply {
                moveTo(paperRightX - foldSize, paperTopY)  // Start of fold on top edge
                lineTo(paperRightX, paperTopY)              // Top-right corner
                lineTo(paperRightX, paperTopY + foldSize)   // Down the right edge
                close()
            }
            // Fold shadow (slightly darker than paper)
            fillPaint.color = 0xFFBBAA88.toInt()
            canvas.drawPath(foldPath, fillPaint)

            // Text hint on paper
            val tinyPaint = Paint().apply {
                textSize = 18f
                typeface = FontManager.getRegular()
                color = 0xFF554433.toInt()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("CODEX", (paperLeftX + paperRightX) / 2f, (paperTopY + paperBottomY) / 2f + 3f, tinyPaint)

            canvas.restore()

            // Store paper rect for touch — expanded to account for rotation
            state.paperRect = RectF(paperLeftX - 5f, paperTopY - 5f, paperRightX + 5f, paperBottomY + 5f)
        } else {
            state.paperRect = null
        }

        // --- Mute toggle buttons (bottom corners of machine body) ---
        val muteButtonRadius = machineHeight * 0.07f
        val muteButtonMargin = muteButtonRadius + 8f

        // spinButtonRect is assigned when the spin button is drawn earlier in this drawSlotMachine pass
        val spinCenterY = spinButtonRect.centerY()

        // Bottom-left: vibration mute
        val vibBtnCx = machineLeft + muteButtonMargin
        val vibBtnCy = spinCenterY
        state.vibrationMuteButtonRect = RectF(
            vibBtnCx - muteButtonRadius * 1.5f, vibBtnCy - muteButtonRadius * 1.5f,
            vibBtnCx + muteButtonRadius * 1.5f, vibBtnCy + muteButtonRadius * 1.5f
        )

        // Bottom-right: audio mute
        val audBtnCx = machineRight - muteButtonMargin
        val audBtnCy = spinCenterY
        state.audioMuteButtonRect = RectF(
            audBtnCx - muteButtonRadius * 1.5f, audBtnCy - muteButtonRadius * 1.5f,
            audBtnCx + muteButtonRadius * 1.5f, audBtnCy + muteButtonRadius * 1.5f
        )

        drawMuteButton(canvas, vibBtnCx, vibBtnCy, muteButtonRadius, state.vibrationMuted, isAudio = false)
        drawMuteButton(canvas, audBtnCx, audBtnCy, muteButtonRadius, state.audioMuted, isAudio = true)

        // Reset paint state
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.alpha = 255
    }

    private fun drawSymbol(canvas: Canvas, symbol: Int, cx: Float, cy: Float, size: Float, spinning: Boolean) {
        val bitmap = IconCache.getSlotSymbol(symbol) ?: return
        val half = size / 2f
        bitmapPaint.alpha = if (spinning) 180 else 255
        canvas.drawBitmap(bitmap, null, RectF(cx - half, cy - half, cx + half, cy + half), bitmapPaint)
        bitmapPaint.alpha = 255
    }

    private fun drawMuteButton(
        canvas: Canvas, cx: Float, cy: Float, radius: Float,
        isMuted: Boolean, isAudio: Boolean
    ) {
        val fillPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE; isAntiAlias = true; strokeWidth = 1.5f
        }

        // Button base (recessed circle)
        fillPaint.color = if (isMuted) 0xFF0E0E1A.toInt() else 0xFF2A2018.toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)

        // Button rim
        linePaint.color = if (isMuted) 0xFF2A2A3A.toInt() else 0xFFCC8844.toInt()
        canvas.drawCircle(cx, cy, radius, linePaint)

        // Icon
        val iconSize = radius * 0.85f
        linePaint.color = if (isMuted) 0xFF444444.toInt() else 0xFFCC8844.toInt()
        linePaint.strokeWidth = 2.5f

        if (isAudio) {
            // Speaker icon
            val left = cx - iconSize * 0.4f
            val right = cx + iconSize * 0.2f
            val top = cy - iconSize * 0.3f
            val bottom = cy + iconSize * 0.3f
            canvas.drawRect(left, top + iconSize * 0.15f, left + iconSize * 0.25f, bottom - iconSize * 0.15f, linePaint)
            val path = Path()
            path.moveTo(left + iconSize * 0.25f, top + iconSize * 0.15f)
            path.lineTo(right, top)
            path.lineTo(right, bottom)
            path.lineTo(left + iconSize * 0.25f, bottom - iconSize * 0.15f)
            canvas.drawPath(path, linePaint)
            if (!isMuted) {
                val waveX = right + iconSize * 0.15f
                canvas.drawArc(
                    RectF(waveX - iconSize * 0.2f, cy - iconSize * 0.25f,
                          waveX + iconSize * 0.2f, cy + iconSize * 0.25f),
                    -45f, 90f, false, linePaint
                )
            }
        } else {
            // Vibration icon: three arcs
            for (i in 0 until 3) {
                val arcRadius = iconSize * (0.2f + i * 0.2f)
                canvas.drawArc(
                    RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius),
                    200f, 140f, false, linePaint
                )
            }
        }

        // Strikethrough when muted
        if (isMuted) {
            linePaint.color = 0xFFAA3333.toInt()
            linePaint.strokeWidth = 3f
            canvas.drawLine(
                cx - radius * 0.6f, cy + radius * 0.6f,
                cx + radius * 0.6f, cy - radius * 0.6f,
                linePaint
            )
        }
    }

}
