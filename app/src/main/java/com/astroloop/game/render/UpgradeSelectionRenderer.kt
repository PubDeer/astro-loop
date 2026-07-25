package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.core.ScreenLayout
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.system.FallbackType
import com.astroloop.game.system.UpgradeOption
import com.astroloop.game.system.UpgradeSystem
import kotlin.math.sin

class UpgradeSelectionRenderer {

    private val titlePaint = Paint().apply {
        isAntiAlias = true
        color = GameConfig.COLOR_HUD
        textSize = 48f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val cardPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = GameConfig.COLOR_HUD
    }

    // ITEM 5: All card borders are now white — single white border paint
    private val whiteBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFFFFFFFF.toInt()
    }

    private val cardFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0xFF111122.toInt()
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = GameConfig.COLOR_HUD
        textSize = 28f  // Increased from 24f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val smallTextPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFAAAAAA.toInt()
        textSize = 22f  // Increased from 18f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val newBadgePaint = Paint().apply {
        isAntiAlias = true
        color = 0xFF00FF00.toInt()
        textSize = 16f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val levelPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFFF00.toInt()
        textSize = 24f  // Increased from 20f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val evolutionPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFF44FF.toInt()  // Purple/magenta for evolution
        textSize = 16f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val evolutionBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFFFF44FF.toInt()
    }

    private val goldBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = 0xFFFFD700.toInt() // Gold
    }

    private val goldFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0xFF1A1A0A.toInt() // Dark gold tint
    }

    private val evolutionHeaderPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFD700.toInt() // Gold
        textSize = 20f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val arrowPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFD700.toInt()
        textSize = 24f
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private val dimOverlayPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0x99000000.toInt()
    }

    private var layout: ScreenLayout = ScreenLayout.compute(GameConfig.DESIGN_WIDTH, GameConfig.DESIGN_HEIGHT)

    val cardRects = mutableListOf<RectF>()

    fun initialize(layout: ScreenLayout) {
        this.layout = layout
    }

    fun render(canvas: Canvas, options: List<UpgradeOption>, state: GameState, upgradeSystem: UpgradeSystem? = null) {
        val content = layout.content
        // Dim background (full screen)
        canvas.drawColor(0xAA000000.toInt())

        // Title
        val title = if (state.luckyStarAnimating) "LUCKY STAR!" else "CHOOSE UPGRADE"
        canvas.drawText(title, content.centerX, content.top + content.height * 0.12f, titlePaint)

        // Calculate card layout - bigger cards with comfortable spacing
        val cardWidth = content.width * 0.30f
        val cardHeight = content.height * 0.42f
        val numCards = options.size
        val spacing = content.width * 0.025f  // Tighter spacing since cards are bigger
        val totalCardsWidth = cardWidth * numCards + spacing * (numCards - 1)
        val startX = content.left + (content.width - totalCardsWidth) / 2
        val cardY = content.top + content.height * 0.22f

        cardRects.clear()

        for ((index, option) in options.withIndex()) {
            val cardX = startX + index * (cardWidth + spacing)
            val rect = RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight)
            cardRects.add(rect)

            when {
                option.isFallback -> {
                    // Render fallback option card (health/gold)
                    renderFallbackCard(canvas, rect, option, state)
                }
                option.isEvolution -> {
                    // Render special evolution card
                    renderEvolutionCard(canvas, rect, option, state)
                }
                else -> {
                    renderCard(canvas, rect, option, state, null)
                }
            }
        }

        // Lucky Star dimming overlay — darken non-highlighted cards
        if (state.luckyStarAnimating) {
            for ((index, rect) in cardRects.withIndex()) {
                if (state.luckyStarDimming) {
                    if (index != state.luckyStarSelectedIndex) {
                        val dimAlpha = (state.luckyStarDimTimer / 0.5f).coerceIn(0f, 1f)
                        dimOverlayPaint.alpha = (dimAlpha * 153).toInt()
                        canvas.drawRect(rect, dimOverlayPaint)
                    }
                } else {
                    if (index != state.luckyStarCurrentHighlight) {
                        dimOverlayPaint.alpha = 0x99
                        canvas.drawRect(rect, dimOverlayPaint)
                    }
                }
            }
        }

    }

    // ITEM 5: Returns white border paint for all non-evolution cards
    private fun getUpgradeBorderPaint(option: UpgradeOption, state: GameState): Paint {
        return whiteBorderPaint
    }

    private fun renderCard(canvas: Canvas, rect: RectF, option: UpgradeOption, state: GameState, evolutionId: String? = null) {
        // Card background
        canvas.drawRect(rect, cardFillPaint)

        // Use special border if this triggers evolution, otherwise use white border
        if (evolutionId != null) {
            canvas.drawRect(rect, evolutionBorderPaint)
        } else {
            canvas.drawRect(rect, getUpgradeBorderPaint(option, state))
        }

        val centerX = rect.centerX()
        val cardPadding = 24f

        // TB-26 stacks 2-5 display as Combat Drone (they add green combat drones, not another TB-26).
        // Astro's Autonomous Ace shows the TB-26-X icon (pilot-aware).
        val displayId = when {
            !option.isWeapon && option.id == "tb26" && state.getPassiveStacks("tb26") >= 1 -> "combat_drone"
            option.isWeapon -> WeaponDefinitions.getWeaponIconId(option.id, state.activePilotId, state.astroLoopMode)
            else -> option.id
        }

        // --- ICON (larger, centered at top) ---
        val iconSize = rect.width() * 0.45f
        val iconY = rect.top + cardPadding + iconSize / 2 + 10f

        cardPaint.style = Paint.Style.STROKE
        IconRenderer.drawIcon(canvas, displayId, option.isWeapon, centerX, iconY, iconSize, cardPaint)

        var y = iconY + iconSize / 2 + 28f

        // --- NAME (larger, prominent) ---
        val name = if (option.isWeapon) {
            WeaponDefinitions.getWeaponDisplayName(option.id)
        } else {
            PassiveDefinitions.getDisplayName(displayId, state.activePilotId, state.astroLoopMode)
        }
        fitTextSize(name, rect.width() - 20f, textPaint, 28f)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(name, centerX, y, textPaint)

        y += 32f

        // --- LEVEL BADGE ---
        val currentLevel = if (option.isWeapon) {
            state.getWeaponLevel(option.id)
        } else {
            state.getPassiveStacks(option.id)
        }
        val maxLevel = if (option.isWeapon) GameConfig.WEAPON_MAX_LEVEL else GameConfig.PASSIVE_MAX_STACKS

        levelPaint.textSize = 24f
        levelPaint.textAlign = Paint.Align.CENTER
        if (currentLevel == 0) {
            newBadgePaint.textSize = 22f
            canvas.drawText("NEW", centerX, y, newBadgePaint)
        } else {
            val nextLevel = currentLevel + 1
            if (nextLevel > maxLevel) {
                levelPaint.color = 0xFFFFD700.toInt() // Gold for MAX
                canvas.drawText("MAX", centerX, y, levelPaint)
                levelPaint.color = 0xFFFFFF00.toInt()
            } else {
                canvas.drawText("Lv.$currentLevel > Lv.$nextLevel", centerX, y, levelPaint)
            }
        }

        y += 36f

        // --- CONTENT: Description (NEW) or Bonus (upgrade) ---
        smallTextPaint.textAlign = Paint.Align.CENTER
        smallTextPaint.textSize = 22f
        smallTextPaint.color = 0xFFCCCCCC.toInt()

        if (currentLevel == 0) {
            // NEW item: Show description
            val description = if (option.isWeapon) {
                WeaponDefinitions.getWeaponDef(option.id)?.description ?: ""
            } else {
                PassiveDefinitions.getPassiveDef(displayId)?.description ?: ""
            }
            val lines = wrapText(description, rect.width() - 24f, smallTextPaint)
            for (line in lines.take(3)) {
                fitTextSize(line, rect.width() - 24f, smallTextPaint, 22f)
                canvas.drawText(line, centerX, y, smallTextPaint)
                y += 34f
            }

        } else {
            // UPGRADE: Just show the bonus (no current stats)
            levelPaint.textSize = 24f
            levelPaint.color = 0xFF44FF44.toInt()

            if (option.isWeapon) {
                val weaponDef = WeaponDefinitions.getWeaponDef(option.id)
                if (weaponDef != null) {
                    val nextLevel = currentLevel + 1
                    if (nextLevel <= maxLevel) {
                        val levelBonus = weaponDef.getLevelDescription(nextLevel)
                        val effectLines = splitEffectText(levelBonus)
                        for ((lineIndex, line) in effectLines.take(3).withIndex()) {
                            val prefix = if (lineIndex == 0) "↑ " else "  "
                            val lineText = "$prefix${line.trim()}"
                            fitTextSize(lineText, rect.width() - 30f, levelPaint, 24f)
                            canvas.drawText(lineText, centerX, y, levelPaint)
                            y += 36f
                        }
                    }
                }
            } else {
                val passiveDef = PassiveDefinitions.getPassiveDef(option.id)
                if (passiveDef != null) {
                    val effect = passiveDef.effectPerStack
                    val effectLines = splitEffectText(effect)
                    for ((lineIndex, line) in effectLines.take(3).withIndex()) {
                        val prefix = if (lineIndex == 0) "↑ " else "  "
                        val lineText = "$prefix${line.trim()}"
                        fitTextSize(lineText, rect.width() - 30f, levelPaint, 24f)
                        canvas.drawText(lineText, centerX, y, levelPaint)
                        y += 36f
                    }
                }
            }
            levelPaint.color = 0xFFFFFF00.toInt()
        }

        // --- EVOLUTION INDICATOR (if applicable, at bottom) ---
        if (evolutionId != null) {
            val evolvedName = WeaponDefinitions.getEvolutionDisplayName(evolutionId, state.activePilotId)

            // Position at bottom of card
            val evolveY = rect.bottom - 50f

            // Small icon
            val smallIconSize = rect.width() * 0.12f
            cardPaint.color = 0xFFFF44FF.toInt()
            IconRenderer.drawIcon(canvas, WeaponDefinitions.getWeaponIconId(evolutionId, state.activePilotId, state.astroLoopMode), true, centerX - 50f, evolveY, smallIconSize, cardPaint)
            cardPaint.color = GameConfig.COLOR_HUD

            // "→ Storm Cannon" next to icon
            evolutionPaint.textSize = 18f
            evolutionPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("→ $evolvedName", centerX - 30f, evolveY + 6f, evolutionPaint)
            evolutionPaint.textAlign = Paint.Align.CENTER
        }
    }

    private fun renderFallbackCard(canvas: Canvas, rect: RectF, option: UpgradeOption, state: GameState) {
        // Background
        canvas.drawRect(rect, cardFillPaint)
        canvas.drawRect(rect, cardPaint)

        val centerX = rect.centerX()
        var y = rect.top + 60f

        // Icon based on fallback type
        val iconSize = rect.width() * 0.3f
        cardPaint.style = Paint.Style.STROKE

        when (option.fallbackType) {
            FallbackType.HEALTH_RESTORE -> {
                // Heart/cross icon
                cardPaint.color = 0xFF00FF00.toInt() // Green
                cardPaint.strokeWidth = 4f
                canvas.drawLine(centerX - iconSize * 0.3f, y, centerX + iconSize * 0.3f, y, cardPaint)
                canvas.drawLine(centerX, y - iconSize * 0.3f, centerX, y + iconSize * 0.3f, cardPaint)
                cardPaint.strokeWidth = 3f
                cardPaint.color = GameConfig.COLOR_HUD

                y += iconSize + 30f

                // Title
                textPaint.textSize = 26f
                canvas.drawText("HEAL", centerX, y, textPaint)
                y += 40f

                // Description
                smallTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Restore 20%", centerX, y, smallTextPaint)
                y += 24f
                canvas.drawText("of max health", centerX, y, smallTextPaint)
            }
            FallbackType.GOLD_BONUS -> {
                // Coin/gold icon
                cardPaint.color = 0xFFFFD700.toInt() // Gold
                canvas.drawCircle(centerX, y, iconSize * 0.35f, cardPaint)
                textPaint.textSize = 20f
                textPaint.color = 0xFFFFD700.toInt()
                canvas.drawText("¥", centerX, y + 8f, textPaint)
                textPaint.color = GameConfig.COLOR_HUD
                cardPaint.color = GameConfig.COLOR_HUD

                y += iconSize + 30f

                // Title
                textPaint.textSize = 26f
                canvas.drawText("¥ BONUS", centerX, y, textPaint)
                y += 40f

                // Description
                smallTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("+1% bonus yen", centerX, y, smallTextPaint)
            }
            null -> {}
        }
    }

    private fun renderEvolutionCard(canvas: Canvas, rect: RectF, option: UpgradeOption, state: GameState) {
        // Special golden background for evolution cards
        canvas.drawRect(rect, goldFillPaint)

        // Draw animated glow effect
        val time = System.currentTimeMillis() / 100f
        val pulseAlpha = (0.3f + 0.2f * sin(time * 0.5f)).coerceIn(0f, 1f)
        goldBorderPaint.alpha = (pulseAlpha * 255).toInt()
        goldBorderPaint.strokeWidth = 6f
        canvas.drawRect(rect, goldBorderPaint)
        goldBorderPaint.alpha = 255
        goldBorderPaint.strokeWidth = 3f
        canvas.drawRect(rect, goldBorderPaint)

        val centerX = rect.centerX()
        var y = rect.top + 30f

        // "EVOLUTION!" header with gold color
        evolutionHeaderPaint.textSize = 24f
        canvas.drawText("EVOLUTION!", centerX, y, evolutionHeaderPaint)

        y += 35f

        // Base weapon name
        val baseName = WeaponDefinitions.getWeaponDisplayName(option.baseWeaponId ?: "")
        textPaint.textSize = 18f
        canvas.drawText(baseName, centerX, y, textPaint)

        y += 25f

        // + Passive name
        val passiveName = PassiveDefinitions.getDisplayName(option.requiredPassiveId ?: "", state.activePilotId, state.astroLoopMode)
        smallTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("+ $passiveName", centerX, y, smallTextPaint)

        y += 30f

        // Arrow pointing down
        arrowPaint.textSize = 32f
        canvas.drawText("↓", centerX, y, arrowPaint)

        y += 35f

        // Evolved weapon name (gold and larger, pilot-aware for Autonomous Ace / TB-26-X)
        val evolvedWeaponDef = WeaponDefinitions.getWeaponDef(option.id)
        val evolvedName = WeaponDefinitions.getEvolutionDisplayName(option.id, state.activePilotId)
        evolutionHeaderPaint.textSize = 28f
        canvas.drawText(evolvedName, centerX, y, evolutionHeaderPaint)

        y += 30f

        // Icon area for evolved weapon
        val iconSize = rect.width() * 0.25f
        cardPaint.style = Paint.Style.STROKE
        cardPaint.color = 0xFFFFD700.toInt() // Gold
        IconRenderer.drawIcon(canvas, WeaponDefinitions.getWeaponIconId(option.id, state.activePilotId, state.astroLoopMode), true, centerX, y + iconSize / 2, iconSize, cardPaint)
        cardPaint.color = GameConfig.COLOR_HUD

        y += iconSize + 25f

        // Evolved weapon description
        val description = evolvedWeaponDef?.description ?: ""
        smallTextPaint.textAlign = Paint.Align.CENTER
        val lines = wrapText(description, rect.width() - 30f, smallTextPaint)
        for (line in lines.take(2)) {
            fitTextSize(line, rect.width() - 30f, smallTextPaint, 22f)
            canvas.drawText(line, centerX, y, smallTextPaint)
            y += 26f
        }
    }

    private fun fitTextSize(text: String, maxWidth: Float, paint: Paint, maxSize: Float, minSize: Float = 14f): Float {
        paint.textSize = maxSize
        if (paint.measureText(text) <= maxWidth) return maxSize
        var size = maxSize
        while (paint.measureText(text) > maxWidth && size > minSize) {
            size -= 1f
            paint.textSize = size
        }
        return size
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        for (segment in text.split('\n')) {
            val words = segment.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
        }
        return lines
    }

    private fun splitEffectText(text: String): List<String> {
        // Split effect text by newline, comma, "and", or ampersand for separate lines
        return text.split(Regex("\n|,\\s*|\\s+and\\s+|\\s*&\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun getSelectedOption(x: Float, y: Float): Int {
        for ((index, rect) in cardRects.withIndex()) {
            if (rect.contains(x, y)) {
                return index
            }
        }
        return -1
    }
}
