package com.astroloop.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.core.ScreenLayout
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.entity.Ship
import kotlin.math.PI
import kotlin.math.sin

class HUDRenderer {

    // HUD text paints (Exo 2 via FontManager; antialiased)
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = GameConfig.COLOR_HUD
        textSize = 36f
        typeface = FontManager.getRegular()
    }

    private val smallTextPaint = Paint().apply {
        isAntiAlias = true
        color = GameConfig.COLOR_HUD
        textSize = 28f
        typeface = FontManager.getRegular()
    }

    private val goldPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFDD44.toInt()  // Gold color
        textSize = 36f
        typeface = FontManager.getRegular()
    }

    private val healthBarPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val healthBarStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = GameConfig.COLOR_HUD
    }

    // Shield bar paints (blue colour scheme, matching health bar structure)
    private val shieldBarPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val shieldBarStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFF4488FF.toInt()  // Lighter blue border
    }

    private val iconPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFFFFFFFF.toInt()
    }

    private val boxFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0xFF111111.toInt()
    }

    private val boxBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val emptySlotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0xFF444444.toInt()  // Gray outline for empty slots
        pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }

    // Semi-transparent dark fill for empty slots (distinct from occupied 0x111111)
    private val emptySlotFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x40000000.toInt()  // 25 % opaque black
    }

    // "+" drawn in the centre of empty slots
    private val emptySlotPlusPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0xFF555555.toInt()  // Slightly brighter than the dashed border
    }

    // Dark shadow drawn behind the level number for contrast
    private val levelShadowPaint = Paint().apply {
        isAntiAlias = true
        textSize = 20f
        typeface = FontManager.getRegular()
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.RIGHT
    }

    // White level number drawn on top of the shadow
    private val levelTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = 20f
        typeface = FontManager.getRegular()
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.RIGHT
    }

    // Dark shadow for bar text (health/shield values inside bars)
    private val barTextShadowPaint = Paint().apply {
        isAntiAlias = true
        textSize = 16f
        typeface = FontManager.getRegular()
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.CENTER
    }

    // White text drawn on top of the shadow inside bars
    private val barTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = 16f
        typeface = FontManager.getRegular()
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
    }

    // Semi-transparent dark background for radio chatter placeholder
    private val radioChatterBgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x66000000.toInt()  // ~40% opaque black
    }

    // Dim placeholder text for radio chatter
    private val radioChatterTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = 14f
        typeface = FontManager.getRegular()
        color = 0xFF555555.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val radioPortraitPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val radioPortraitStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val bitmapPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    private val radioSpeakerPaint = Paint().apply {
        isAntiAlias = true
        textSize = 18f
        typeface = FontManager.getBold()
        textAlign = Paint.Align.LEFT
    }

    private val radioLinePaint = Paint().apply {
        isAntiAlias = true
        textSize = 24f
        typeface = FontManager.getRegular()
        color = 0xFFCCCCCC.toInt()
        textAlign = Paint.Align.LEFT
    }

    private val radioBoxStrokePaint = Paint().apply {
        color = 0xFF444444.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val radioWavePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0xFF444444.toInt()
    }

    // ITEM 7: Red X overlay paint for Phoenix Core "used" indicator
    private val phoenixUsedPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = 0xB4FFFFFF.toInt()  // White with alpha ~180
    }

    private var layout: ScreenLayout = ScreenLayout.compute(
        GameConfig.DESIGN_WIDTH, GameConfig.DESIGN_HEIGHT
    )

    // Grid layout constants
    private val ICON_SIZE = 48f
    private val ICON_PADDING = 8f
    private val WEAPON_SLOTS = 4
    private val PASSIVE_SLOTS = 4

    fun initialize(layout: ScreenLayout) {
        this.layout = layout
    }

    // Debug paint for alignment grid
    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0x44FF00FF.toInt()  // Semi-transparent magenta
    }

    fun render(canvas: Canvas, state: GameState, ship: Ship, hudAlpha: Float = 1f) {
        if (hudAlpha <= 0f) return

        val saveLayerAlpha = (hudAlpha * 255).toInt().coerceIn(0, 255)
        canvas.saveLayerAlpha(0f, 0f, layout.width, layout.height, saveLayerAlpha)

        // Draw alignment grid in debug mode
        if (state.debugMenuOpen) {
            renderAlignmentGrid(canvas)
        }
        renderTopBar(canvas, state, ship)
        canvas.restore()
    }

    private fun renderAlignmentGrid(canvas: Canvas) {
        // Vertical center line
        canvas.drawLine(layout.width / 2, 0f, layout.width / 2, layout.height, gridPaint)

        // Horizontal thirds
        canvas.drawLine(0f, layout.height / 3, layout.width, layout.height / 3, gridPaint)
        canvas.drawLine(0f, layout.height * 2 / 3, layout.width, layout.height * 2 / 3, gridPaint)

        // Vertical thirds
        canvas.drawLine(layout.width / 3, 0f, layout.width / 3, layout.height, gridPaint)
        canvas.drawLine(layout.width * 2 / 3, 0f, layout.width * 2 / 3, layout.height, gridPaint)

        // HUD alignment guide lines — anchored to the safe area so they track the real
        // HUD origin on cutout devices (the HUD itself is safe-anchored in renderTopBar).
        val padding = 20f
        val guideLeft = layout.safe.left + padding
        val guideTop = layout.safe.top + padding
        gridPaint.color = 0x6600FFFF.toInt()  // Cyan for HUD guides

        // Top padding line
        canvas.drawLine(0f, guideTop, layout.width, guideTop, gridPaint)

        // Icon grid alignment (48px icons + 8px padding)
        val iconGridWidth = 4 * (ICON_SIZE + ICON_PADDING)
        canvas.drawLine(guideLeft, 0f, guideLeft, 150f, gridPaint)
        canvas.drawLine(guideLeft + iconGridWidth, 0f, guideLeft + iconGridWidth, 150f, gridPaint)

        // Center bar alignment
        val barWidth = layout.width * 0.25f
        val barX = (layout.width - barWidth) / 2f
        canvas.drawLine(barX, 0f, barX, 80f, gridPaint)
        canvas.drawLine(barX + barWidth, 0f, barX + barWidth, 80f, gridPaint)

        gridPaint.color = 0x44FF00FF.toInt()  // Reset
    }

    private fun renderTopBar(canvas: Canvas, state: GameState, ship: Ship) {
        val padding = 20f
        val topY = layout.safe.top + padding
        val leftX = layout.safe.left + padding
        val rightX = layout.safe.right - padding

        // =====================================================================
        // ZONE 1 — Upgrade Grid (top-left)
        // Top of the grid icons sits at topY so all three zones share the same
        // vertical origin.
        // =====================================================================
        renderUpgradeGrid(canvas, state, leftX, topY)

        // =====================================================================
        // ZONE 2 — Health + Shield Bars (top row) + Radio Chatter (bottom row)
        // Both bars stacked vertically within the top grid row height.
        // Radio chatter strip occupies the bottom grid row.
        // =====================================================================
        val gridEndX = leftX + WEAPON_SLOTS * (ICON_SIZE + ICON_PADDING) + 12f
        // Reserve space for yen/timer on the right (~140 px)
        val yenAreaWidth = 140f
        val barStartX = gridEndX
        val barWidth = rightX - barStartX - yenAreaWidth
        val barGap = 4f
        val barHeight = (ICON_SIZE - barGap) / 2f  // Two bars + gap fit in one icon row

        // --- Health bar (top of top row) ---
        val hpBarY = topY
        val maxHealth = ship.maxHealth
        val healthPercent = (ship.health / maxHealth).coerceIn(0f, 1f)

        // Bar background
        healthBarPaint.color = 0xFF1a1a1a.toInt()
        canvas.drawRect(barStartX, hpBarY, barStartX + barWidth, hpBarY + barHeight, healthBarPaint)

        // Bar fill (left to right)
        healthBarPaint.color = when {
            healthPercent > 0.5f  -> GameConfig.COLOR_HEALTH_BAR
            healthPercent > 0.25f -> 0xFFFFFF00.toInt()
            else                  -> 0xFFFF0000.toInt()
        }
        canvas.drawRect(barStartX, hpBarY, barStartX + barWidth * healthPercent, hpBarY + barHeight, healthBarPaint)

        // Bar border
        canvas.drawRect(barStartX, hpBarY, barStartX + barWidth, hpBarY + barHeight, healthBarStrokePaint)

        // Numeric value centered inside bar
        val healthText = "${if (ship.health > 0f) ship.health.toInt().coerceAtLeast(1) else 0}/${maxHealth.toInt()}"
        val hpTextX = barStartX + barWidth / 2f
        val hpTextY = hpBarY + barHeight / 2f + barTextPaint.textSize / 3f
        canvas.drawText(healthText, hpTextX + 1f, hpTextY + 1f, barTextShadowPaint)
        canvas.drawText(healthText, hpTextX, hpTextY, barTextPaint)

        // --- Shield bar (bottom of top row) ---
        val shBarY = hpBarY + barHeight + barGap
        if (ship.maxShield > 0f) {
            val shieldPercent = (ship.currentShield / ship.maxShield).coerceIn(0f, 1f)

            // Bar background
            shieldBarPaint.color = 0xFF1a1a2e.toInt()
            canvas.drawRect(barStartX, shBarY, barStartX + barWidth, shBarY + barHeight, shieldBarPaint)

            // Bar fill (left to right)
            shieldBarPaint.color = 0xFF4488FF.toInt()
            canvas.drawRect(barStartX, shBarY, barStartX + barWidth * shieldPercent, shBarY + barHeight, shieldBarPaint)

            // Bar border
            canvas.drawRect(barStartX, shBarY, barStartX + barWidth, shBarY + barHeight, shieldBarStrokePaint)

            // Numeric value centered inside bar
            val shieldText = "${ship.currentShield.toInt()}/${ship.maxShield.toInt()}"
            val shTextX = barStartX + barWidth / 2f
            val shTextY = shBarY + barHeight / 2f + barTextPaint.textSize / 3f
            canvas.drawText(shieldText, shTextX + 1f, shTextY + 1f, barTextShadowPaint)
            canvas.drawText(shieldText, shTextX, shTextY, barTextPaint)
        } else {
            // No shields - draw empty/disabled bar
            shieldBarPaint.color = 0xFF0a0a0a.toInt()
            canvas.drawRect(barStartX, shBarY, barStartX + barWidth, shBarY + barHeight, shieldBarPaint)
            shieldBarStrokePaint.color = 0xFF333333.toInt()
            canvas.drawRect(barStartX, shBarY, barStartX + barWidth, shBarY + barHeight, shieldBarStrokePaint)
            shieldBarStrokePaint.color = 0xFF6688CC.toInt()
        }

        // --- Radio chatter box (bottom grid row) ---
        val chatterY = topY + ICON_SIZE + ICON_PADDING
        val chatterHeight = ICON_SIZE  // Full height of bottom grid row

        // Bordered box (same width as health/shield bars)
        canvas.drawRect(barStartX, chatterY, barStartX + barWidth, chatterY + chatterHeight, radioChatterBgPaint)
        radioBoxStrokePaint.color = 0xFF444444.toInt()
        canvas.drawRect(barStartX, chatterY, barStartX + barWidth, chatterY + chatterHeight, radioBoxStrokePaint)

        val message = state.radioMessage
        if (message != null) {
            // Calculate fade alpha (0.2s quick fade)
            val fadeAlpha = if (state.radioTimer > 0f) {
                1f
            } else {
                (state.radioFadeTimer / 0.2f).coerceIn(0f, 1f)
            }
            val alpha = (fadeAlpha * 255).toInt()

            // Portrait (square, left side)
            val portraitSize = chatterHeight - 8f
            val portraitX = barStartX + 4f
            val portraitY = chatterY + 4f

            val portraitBitmap = radioPortraitBitmap(state)
            if (portraitBitmap != null) {
                bitmapPaint.alpha = alpha
                val destRect = RectF(portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize)
                canvas.drawBitmap(portraitBitmap, null, destRect, bitmapPaint)
                bitmapPaint.alpha = 255
            } else {
                // Fallback for unknown speakers (e.g. "???")
                radioPortraitPaint.color = if (state.radioIsCorrupted) 0xFF663333.toInt() else 0xFF444444.toInt()
                radioPortraitPaint.alpha = alpha
                canvas.drawRect(portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize, radioPortraitPaint)
                radioPortraitStrokePaint.color = if (state.radioIsCorrupted) 0xFF884444.toInt() else 0xFF666666.toInt()
                radioPortraitStrokePaint.alpha = alpha
                canvas.drawRect(portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize, radioPortraitStrokePaint)
            }

            // Message text only (no callsign — portrait identifies speaker)
            val textX = portraitX + portraitSize + 8f
            val lineY = chatterY + chatterHeight / 2f + radioLinePaint.textSize / 3f

            radioLinePaint.alpha = alpha
            val availableWidth = barStartX + barWidth - textX - 4f
            val fittedMessage = truncateToFit(message, radioLinePaint, availableWidth)
            canvas.drawText(fittedMessage, textX, lineY, radioLinePaint)
        } else {
            // Idle state — animated signal wave
            val waveY = chatterY + chatterHeight / 2f
            val waveStartX = barStartX + 16f
            val waveEndX = barStartX + barWidth - 16f
            val waveWidth = waveEndX - waveStartX
            val time = (System.currentTimeMillis() % 100000L) / 1000f

            val maxAmplitude = (chatterHeight / 2f) - 4f
            val segments = 60
            var prevX = waveStartX
            var prevWaveY = waveY
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val x = waveStartX + waveWidth * t
                val envelope = sin(t * PI.toFloat())  // Envelope: peaks in middle
                val y = waveY + maxAmplitude * envelope * sin(t * 10f * 2f * PI.toFloat() + time * 6f)
                canvas.drawLine(prevX, prevWaveY, x, y, radioWavePaint)
                prevX = x
                prevWaveY = y
            }
        }

        // =====================================================================
        // ZONE 3 — Yen & Timer (top-right, right-aligned)
        // Aligned with the vertical centre of the top and bottom grid rows.
        // =====================================================================
        val topRowCenterY = topY + ICON_SIZE / 2f
        val bottomRowCenterY = topY + ICON_SIZE + ICON_PADDING + ICON_SIZE / 2f

        // Yen aligned with top grid row centre
        goldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(GameConfig.formatYen(state.goldCollected), rightX, topRowCenterY + goldPaint.textSize / 3f, goldPaint)

        // Timer aligned with bottom grid row centre
        textPaint.textAlign = Paint.Align.RIGHT
        val timerText = formatTime(state.survivalTime)
        canvas.drawText(timerText, rightX, bottomRowCenterY + textPaint.textSize / 3f, textPaint)
    }

    private fun renderUpgradeGrid(canvas: Canvas, state: GameState, startX: Float, startY: Float) {
        // Always 4x2 grid:
        // Top row: Weapons 1-4
        // Bottom row: Passives 1-3 (or 4) + optional 5th weapon in slot 4

        val weaponIds = state.weaponLevels.keys.toList()
        // Filter out extra_weapon_slot since it's consumed to provide the extra weapon slot
        val passiveIds = state.passiveStacks.keys.filter { it != "extra_weapon_slot" }.toList()
        val hasExtraWeaponSlot = state.hasExtraWeaponSlot

        // Top row - First 4 weapons (slots 0-3)
        for (i in 0 until WEAPON_SLOTS) {
            val x = startX + i * (ICON_SIZE + ICON_PADDING)
            val y = startY

            if (i < weaponIds.size && i < 4) {
                val weaponId = weaponIds[i]
                val level = state.weaponLevels[weaponId] ?: 1
                val borderColor = 0xFFFFFFFF.toInt()  // White border for all weapon slots
                drawUpgradeBox(canvas, x, y, ICON_SIZE, weaponId, true, level, borderColor, state)
            } else {
                // Empty slot
                drawEmptySlot(canvas, x, y, ICON_SIZE)
            }
        }

        // Bottom row - Passives (and 5th weapon in last slot if extra_weapon_slot)
        val effectivePassiveSlots = if (hasExtraWeaponSlot) PASSIVE_SLOTS - 1 else PASSIVE_SLOTS
        for (i in 0 until PASSIVE_SLOTS) {
            val x = startX + i * (ICON_SIZE + ICON_PADDING)
            val y = startY + ICON_SIZE + ICON_PADDING

            if (hasExtraWeaponSlot && i == PASSIVE_SLOTS - 1) {
                // Last slot on bottom row is the 5th weapon slot
                if (weaponIds.size > 4) {
                    // Draw the 5th weapon
                    val weaponId = weaponIds[4]
                    val level = state.weaponLevels[weaponId] ?: 1
                    val borderColor = 0xFFFFFFFF.toInt()  // White border for all weapon slots
                    drawUpgradeBox(canvas, x, y, ICON_SIZE, weaponId, true, level, borderColor, state)
                } else {
                    // Empty 5th weapon slot - special gold border to indicate extra slot
                    drawExtraWeaponSlot(canvas, x, y, ICON_SIZE)
                }
            } else if (i < passiveIds.size) {
                val passiveId = passiveIds[i]
                val stacks = state.passiveStacks[passiveId] ?: 1
                val borderColor = 0xFFFFFFFF.toInt()  // White border for all passive slots
                val displayPassiveId = com.astroloop.game.data.PassiveDefinitions.getEffectivePassiveId(passiveId, state.activePilotId, state.astroLoopMode)
                drawUpgradeBox(canvas, x, y, ICON_SIZE, displayPassiveId, false, stacks, borderColor, state)
            } else if (i < effectivePassiveSlots) {
                // Empty passive slot
                drawEmptySlot(canvas, x, y, ICON_SIZE)
            }
        }
    }

    private fun drawEmptySlot(canvas: Canvas, x: Float, y: Float, size: Float) {
        // Dark semi-transparent background so empty slots read differently from occupied ones
        canvas.drawRect(x, y, x + size, y + size, emptySlotFillPaint)

        // Dashed gray border
        canvas.drawRect(x, y, x + size, y + size, emptySlotPaint)

        // Small "+" in the centre as a tap-to-add hint
        val cx = x + size / 2f
        val cy = y + size / 2f
        val armLen = size * 0.15f  // keeps the + subtle at 48 px icons
        canvas.drawLine(cx - armLen, cy, cx + armLen, cy, emptySlotPlusPaint)
        canvas.drawLine(cx, cy - armLen, cx, cy + armLen, emptySlotPlusPaint)
    }

    private fun drawExtraWeaponSlot(canvas: Canvas, x: Float, y: Float, size: Float) {
        // Dark semi-transparent background
        canvas.drawRect(x, y, x + size, y + size, emptySlotFillPaint)

        // Kraken's teal border to indicate bonus weapon slot
        val goldBorderPaint = Paint().apply {
            color = 0xFF33AAAA.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(x, y, x + size, y + size, goldBorderPaint)

        // Plus icon in center to indicate extra weapon slot
        val armLen = size * 0.2f
        val cx = x + size / 2f
        val cy = y + size / 2f
        val plusPaint = Paint().apply {
            color = 0xFF33AAAA.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawLine(cx - armLen, cy, cx + armLen, cy, plusPaint)
        canvas.drawLine(cx, cy - armLen, cx, cy + armLen, plusPaint)
    }

    private fun formatTime(seconds: Float): String {
        val totalSecs = seconds.toInt()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return if (mins >= 1) {
            "$mins:${secs.toString().padStart(2, '0')}"
        } else {
            "$secs"
        }
    }

    private fun getPassiveColor(passiveId: String, state: GameState): Int {
        if (passiveId == "tb26" || passiveId == "combat_drone") {
            return com.astroloop.game.data.PassiveDefinitions.getDroneColor(passiveId, state.activePilotId, state.astroLoopMode)
        }
        return when (passiveId) {
            // Damage passives: Red
            "duplicator_core", "glass_cannon", "momentum_drive", "revenge_protocol" -> 0xFFFF4444.toInt()
            // Defense passives: Blue
            "nano_repair", "phoenix_core", "vampiric_core" -> 0xFF4488FF.toInt()
            // Utility passives: Yellow
            "magnet_field", "lucky_star", "cryo_field" -> 0xFFFFFF00.toInt()
            // Extra weapon slot: Kraken's colour
            "extra_weapon_slot" -> 0xFF33AAAA.toInt()
            else -> GameConfig.COLOR_HUD
        }
    }

    private fun drawUpgradeBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        id: String,
        isWeapon: Boolean,
        level: Int,
        borderColor: Int,
        state: GameState
    ) {
        // Black filled background
        canvas.drawRect(x, y, x + size, y + size, boxFillPaint)

        // Colored border based on type
        boxBorderPaint.color = borderColor
        canvas.drawRect(x, y, x + size, y + size, boxBorderPaint)

        // White icon inside
        iconPaint.color = 0xFFFFFFFF.toInt()
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = 1.5f
        val iconCenterX = x + size / 2
        val iconCenterY = y + size / 2
        val iconDrawSize = size * 0.6f
        val iconId = if (isWeapon) com.astroloop.game.data.WeaponDefinitions.getWeaponIconId(id, state.activePilotId, state.astroLoopMode) else id
        IconRenderer.drawIcon(canvas, iconId, isWeapon, iconCenterX, iconCenterY, iconDrawSize, iconPaint)

        // --- Level number (bottom-right, inside the box) ---
        // Only show when the level / stack count is greater than zero.
        if (level > 0) {
            val numText = if (level >= GameConfig.WEAPON_MAX_LEVEL) "\u2605" else "$level"
            // Right-aligned: leave a 3 px margin from the right edge of the box.
            val textX = x + size - 3f
            // Bottom of the text sits 3 px above the bottom edge of the box.
            val textY = y + size - 3f

            // Dark 1-px shadow drawn first so the white number sits cleanly on top
            canvas.drawText(numText, textX + 1f, textY + 1f, levelShadowPaint)
            canvas.drawText(numText, textX, textY, levelTextPaint)
        }

        // --- ITEM 7: Phoenix Core "used" indicator ---
        // Draw a red X over the tile when phoenix_core has been consumed this life
        if (!isWeapon && id == "phoenix_core" && state.phoenixUsed) {
            val margin = size * 0.15f
            canvas.drawLine(x + margin, y + margin, x + size - margin, y + size - margin, phoenixUsedPaint)
            canvas.drawLine(x + size - margin, y + margin, x + margin, y + size - margin, phoenixUsedPaint)
        }
    }

    /** Speaker portrait for the radio chatter box; boss lines override, then the
     *  Astro Loop bandana rule (active pilot's own lines only), then the normal set. */
    private fun radioPortraitBitmap(state: GameState): Bitmap? {
        return if (state.radioSpeaker == "BOSS" || (state.radioBoss && state.radioSpeaker == "ASTRO")) {
            IconCache.getPortraitBoss()
        } else {
            IconCache.getPortraitByCallsign(state.radioSpeaker, state.radioIsCorrupted,
                state.radioBandanaPilotId(), state.radioIsGhost)
        }
    }

    fun renderRadioOnly(canvas: Canvas, state: GameState) {
        val padding = 20f
        val topY = layout.safe.top + padding
        val leftX = layout.safe.left + padding
        val rightX = layout.safe.right - padding

        // Replicate the bar position calculations from renderTopBar
        val gridEndX = leftX + WEAPON_SLOTS * (ICON_SIZE + ICON_PADDING) + 12f
        val yenAreaWidth = 140f
        val barStartX = gridEndX
        val barWidth = rightX - barStartX - yenAreaWidth

        // Radio chatter box position (bottom grid row)
        val chatterY = topY + ICON_SIZE + ICON_PADDING
        val chatterHeight = ICON_SIZE

        // Bordered box background
        canvas.drawRect(barStartX, chatterY, barStartX + barWidth, chatterY + chatterHeight, radioChatterBgPaint)
        radioBoxStrokePaint.color = 0xFF444444.toInt()
        canvas.drawRect(barStartX, chatterY, barStartX + barWidth, chatterY + chatterHeight, radioBoxStrokePaint)

        val message = state.radioMessage
        if (message != null) {
            val fadeAlpha = if (state.radioTimer > 0f) {
                1f
            } else {
                (state.radioFadeTimer / 0.2f).coerceIn(0f, 1f)
            }
            val alpha = (fadeAlpha * 255).toInt()

            // Portrait
            val portraitSize = chatterHeight - 8f
            val portraitX = barStartX + 4f
            val portraitY = chatterY + 4f

            val portraitBitmap = radioPortraitBitmap(state)
            if (portraitBitmap != null) {
                bitmapPaint.alpha = alpha
                val destRect = RectF(portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize)
                canvas.drawBitmap(portraitBitmap, null, destRect, bitmapPaint)
                bitmapPaint.alpha = 255
            } else {
                // Fallback for unknown speakers (e.g. "???")
                radioPortraitPaint.color = if (state.radioIsCorrupted) 0xFF663333.toInt() else 0xFF444444.toInt()
                radioPortraitPaint.alpha = alpha
                canvas.drawRect(portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize, radioPortraitPaint)
                radioPortraitStrokePaint.color = if (state.radioIsCorrupted) 0xFF884444.toInt() else 0xFF666666.toInt()
                radioPortraitStrokePaint.alpha = alpha
                canvas.drawRect(portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize, radioPortraitStrokePaint)
            }

            // Message text
            val textX = portraitX + portraitSize + 8f
            val lineY = chatterY + chatterHeight / 2f + radioLinePaint.textSize / 3f

            radioLinePaint.alpha = alpha
            val availableWidth = barStartX + barWidth - textX - 4f
            val fittedMessage = truncateToFit(message, radioLinePaint, availableWidth)
            canvas.drawText(fittedMessage, textX, lineY, radioLinePaint)
        } else {
            // Idle state — animated signal wave
            val waveY = chatterY + chatterHeight / 2f
            val waveStartX = barStartX + 16f
            val waveEndX = barStartX + barWidth - 16f
            val waveWidth = waveEndX - waveStartX
            val time = (System.currentTimeMillis() % 100000L) / 1000f

            val maxAmplitude = (chatterHeight / 2f) - 4f
            val segments = 60
            var prevX = waveStartX
            var prevWaveY = waveY
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val x = waveStartX + waveWidth * t
                val envelope = sin(t * PI.toFloat())
                val y = waveY + maxAmplitude * envelope * sin(t * 10f * 2f * PI.toFloat() + time * 6f)
                canvas.drawLine(prevX, prevWaveY, x, y, radioWavePaint)
                prevX = x
                prevWaveY = y
            }
        }
    }

    private fun truncateToFit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        for (i in text.length - 1 downTo 1) {
            val truncated = text.substring(0, i) + "..."
            if (paint.measureText(truncated) <= maxWidth) return truncated
        }
        return "..."
    }

}
