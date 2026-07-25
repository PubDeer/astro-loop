package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.data.TelemetryManager
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.DesertDefinitions
import com.astroloop.game.core.SoundManager

class DebugMenuRenderer {
    private val SOUND_SETS = listOf("normal", "corruption", "astroloop")
    private val bgPaint = Paint().apply {
        color = 0xCC000000.toInt()
        style = Paint.Style.FILL
    }
    private val titlePaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 30f
        isAntiAlias = true
        typeface = FontManager.getBold()
        textAlign = Paint.Align.CENTER
    }
    private val btnFillPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val btnStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val btnTextPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 13f
        isAntiAlias = true
        typeface = FontManager.getBold()
        textAlign = Paint.Align.CENTER
    }
    private val btnNumPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 18f
        isAntiAlias = true
        typeface = FontManager.getBold()
        textAlign = Paint.Align.CENTER
    }
    private val closeBtnPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val closeStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val closeTextPaint = Paint().apply {
        color = 0xFFCCCCCC.toInt()
        textSize = 20f
        isAntiAlias = true
        typeface = FontManager.getBold()
        textAlign = Paint.Align.CENTER
    }
    private val infoPaint = Paint().apply {
        color = 0xFF888888.toInt()
        textSize = 14f
        isAntiAlias = true
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }
    private val dotPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val starPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = 0xFF8899BB.toInt()
    }
    private var screenWidth = 0f
    private var screenHeight = 0f

    // Page 0: Weapons & Passives
    private val weaponRects = Array(12) { RectF() }
    private val passiveRects = Array(PassiveDefinitions.getAllPassives().size) { RectF() }

    // Page 1: Evolutions & Resets
    private val evolutionRects = Array(12) { RectF() }
    private var resetSmallRect = RectF()
    private var resetBigRect = RectF()
    private var dieRect = RectF()

    // Page 3: Flak Designs (active designs: 35, 36, 38 — randomly selected at runtime)
    var debugFlakAge: Float = 0f
    private val flakCellRects = Array(3) { RectF() }
    private val flakActiveDesigns = intArrayOf(35, 36, 38)

    // Page 6: Black Market Designs
    private val BLACK_MARKET_PAGE = 6
    private var blackMarketScrollY = 0f
    private var blackMarketLastY = 0f
    private var debugStars: List<FloatArray>? = null   // [x, y, r] per star

    private fun drawDebugStarfield(canvas: Canvas) {
        val stars = debugStars ?: run {
            val rnd = java.util.Random(7)
            val list = ArrayList<FloatArray>(120)
            repeat(120) {
                list.add(floatArrayOf(
                    rnd.nextFloat() * screenWidth,
                    rnd.nextFloat() * screenHeight,
                    0.6f + rnd.nextFloat() * 1.6f
                ))
            }
            debugStars = list
            list
        }
        for (s in stars) canvas.drawCircle(s[0], s[1], s[2], starPaint)
    }

    // Page 2: Story Debug
    private var bossNowRect = RectF()
    private var setCorruptRect = RectF()
    private var killPilotRect = RectF()
    private var killAllRect = RectF()
    private var unlockCrystalRect = RectF()
    private var resetStoryRect = RectF()
    private var unbrickRect = RectF()
    private var grantBandRect = RectF()
    private var clrBandRect = RectF()
    // Crystal opening / fight / release debug buttons
    private var crystalOpeningRect = RectF()
    private var crystalFightRect = RectF()
    private var crystalReleaseRect = RectF()
    // Desert debug buttons
    private var playDesertRect = RectF()
    private var playDesertP2Rect = RectF()
    private var desertCrystalRect = RectF()
    private var astroLoopRect = RectF()
    private var reckoningRoundsRect = RectF()
    private var setDesertFlagsRect = RectF()
    private var clrDesertRect = RectF()
    private var loopRect1 = RectF()
    private var loopRect2 = RectF()
    private var loopRect3 = RectF()

    // Page 4: Weapon Tuning
    val tuningWeapons = listOf(
        "storm_cannon", "warp_saw", "leech_burst", "autonomous_ace",
        "frost_ring", "oblivion_beam", "jackpot_mines", "phoenix_flare",
        "lingering_nova", "siphon_needles", "hunter_killer", "flak_barrage"
    )
    private val TUNING_BTN_COUNT = 11
    private val tuningVolRects = Array(12) { Array(TUNING_BTN_COUNT) { RectF() } }
    private val tuningBeatRects = Array(12) { Array(TUNING_BTN_COUNT) { RectF() } }
    private val tuningSetRects = Array(SOUND_SETS.size) { RectF() }
    private var tuningPlayStopRect = RectF()
    private var tuningGridTop = 0f
    private var tuningGridBottom = 0f
    private var tuningScrollY = 0f
    private var tuningScrollVelocity = 0f
    var tuningPreviewSet: String = "normal"
    var tuningBGMPlaying = false
    val tuningVolSelections = mutableMapOf<String, Int>()   // weaponId → vol button (0-10, default 5)
    val tuningBeatSelections = mutableMapOf<String, Int>()   // weaponId → beat button (0-10, default 5)

    // Page 3: Flak Designs
    var telemetryManager: TelemetryManager? = null
    private var clearLogRect = RectF()
    private var telemetryScrollOffset = 0f
    private var telemetryScrollVelocity = 0f
    private var expandedRunIndex = -1
    private var telemetryLastY = 0f
    private var telemetryDragging = false

    private var closeButtonRect = RectF()

    var renderScale: Float = 1f

    // Swipe tracking
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var isSwiping = false

    private val marginX = 16f
    private val buttonGap = 8f

    private val instantMaxPassives = setOf("glass_cannon", "phoenix_core", "duplicator_core", "extra_weapon_slot")

    fun initialize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    fun render(canvas: Canvas, state: GameState) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        when (state.debugMenuPage) {
            0 -> renderWeaponsPassivesPage(canvas, state)
            1 -> renderEvolutionsResetsPage(canvas, state)
            2 -> renderPhase4Page(canvas, state)
            3 -> renderFlakDesignsPage(canvas, state)
            5 -> drawThrusterPage(canvas)
            6 -> renderBlackMarketPage(canvas, state)
        }

        drawPageDots(canvas, state.debugMenuPage)
        drawCloseButton(canvas)
    }

    // =======================================================================
    // Page 0: Weapons & Passives
    // =======================================================================

    private fun renderWeaponsPassivesPage(canvas: Canvas, state: GameState) {
        var y = 60f

        canvas.drawText("WEAPONS & PASSIVES", screenWidth / 2f, y, titlePaint)
        y += 40f

        val sectionCols = 3
        val sectionRows = 4
        val gridWidth = screenWidth - marginX * 2
        val sectionHeight = (screenHeight - y - 140f) / 2f
        val buttonWidth = (gridWidth - (sectionCols - 1) * buttonGap) / sectionCols
        val buttonHeight = (sectionHeight - (sectionRows - 1) * buttonGap - 24f) / sectionRows

        infoPaint.color = 0xFF888888.toInt()
        canvas.drawText("WEAPONS", screenWidth / 2f, y, infoPaint)
        y += 20f

        val baseWeapons = WeaponDefinitions.getBaseWeapons()
        for (i in baseWeapons.indices) {
            val row = i / sectionCols
            val col = i % sectionCols
            val bx = marginX + col * (buttonWidth + buttonGap)
            val by = y + row * (buttonHeight + buttonGap)
            weaponRects[i] = RectF(bx, by, bx + buttonWidth, by + buttonHeight)

            val level = state.getWeaponLevel(baseWeapons[i].id)
            val hasIt = level > 0

            btnFillPaint.color = if (hasIt) 0xFF224488.toInt() else 0xFF222222.toInt()
            canvas.drawRoundRect(weaponRects[i], 6f, 6f, btnFillPaint)

            btnStrokePaint.color = if (hasIt) 0xFF4488FF.toInt() else 0xFF444444.toInt()
            canvas.drawRoundRect(weaponRects[i], 6f, 6f, btnStrokePaint)

            btnTextPaint.color = if (hasIt) 0xFFFFFFFF.toInt() else 0xFF777777.toInt()
            btnTextPaint.textSize = 11f
            canvas.drawText(baseWeapons[i].name, bx + buttonWidth / 2f, by + buttonHeight * 0.45f, btnTextPaint)

            btnNumPaint.color = if (hasIt) 0xFF88BBFF.toInt() else 0xFF555555.toInt()
            canvas.drawText("L$level", bx + buttonWidth / 2f, by + buttonHeight * 0.80f, btnNumPaint)
        }
        btnTextPaint.textSize = 13f

        y += sectionHeight + 10f
        infoPaint.color = 0xFF888888.toInt()
        canvas.drawText("PASSIVES", screenWidth / 2f, y, infoPaint)
        y += 20f

        val passives = PassiveDefinitions.getAllPassives()
        for (i in passives.indices) {
            val row = i / sectionCols
            val col = i % sectionCols
            val bx = marginX + col * (buttonWidth + buttonGap)
            val by = y + row * (buttonHeight + buttonGap)
            passiveRects[i] = RectF(bx, by, bx + buttonWidth, by + buttonHeight)

            val stacks = state.getPassiveStacks(passives[i].id)
            val hasIt = stacks > 0

            btnFillPaint.color = if (hasIt) 0xFF226633.toInt() else 0xFF222222.toInt()
            canvas.drawRoundRect(passiveRects[i], 6f, 6f, btnFillPaint)

            btnStrokePaint.color = if (hasIt) 0xFF44CC66.toInt() else 0xFF444444.toInt()
            canvas.drawRoundRect(passiveRects[i], 6f, 6f, btnStrokePaint)

            btnTextPaint.color = if (hasIt) 0xFFFFFFFF.toInt() else 0xFF777777.toInt()
            btnTextPaint.textSize = 11f
            canvas.drawText(passives[i].name, bx + buttonWidth / 2f, by + buttonHeight * 0.45f, btnTextPaint)

            btnNumPaint.color = if (hasIt) 0xFF88FFaa.toInt() else 0xFF555555.toInt()
            canvas.drawText("x$stacks", bx + buttonWidth / 2f, by + buttonHeight * 0.80f, btnNumPaint)
        }
        btnTextPaint.textSize = 13f
    }

    // =======================================================================
    // Page 1: Evolutions & Resets
    // =======================================================================

    private fun renderEvolutionsResetsPage(canvas: Canvas, state: GameState) {
        var y = 60f

        canvas.drawText("EVOLUTIONS & RESETS", screenWidth / 2f, y, titlePaint)
        y += 40f

        val sectionCols = 3
        val sectionRows = 4
        val gridWidth = screenWidth - marginX * 2
        val buttonWidth = (gridWidth - (sectionCols - 1) * buttonGap) / sectionCols
        val evoSectionHeight = screenHeight * 0.55f
        val buttonHeight = (evoSectionHeight - (sectionRows - 1) * buttonGap - 24f) / sectionRows

        infoPaint.color = 0xFF888888.toInt()
        canvas.drawText("EVOLUTIONS", screenWidth / 2f, y, infoPaint)
        y += 20f

        val baseWeapons = WeaponDefinitions.getBaseWeapons()
        for (i in baseWeapons.indices) {
            val weapon = baseWeapons[i]
            val evolvedId = weapon.evolutionWeaponId ?: continue
            val evolvedDef = WeaponDefinitions.getWeaponDef(evolvedId)

            val row = i / sectionCols
            val col = i % sectionCols
            val bx = marginX + col * (buttonWidth + buttonGap)
            val by = y + row * (buttonHeight + buttonGap)
            evolutionRects[i] = RectF(bx, by, bx + buttonWidth, by + buttonHeight)

            val hasIt = state.hasEvolution(evolvedId)

            btnFillPaint.color = if (hasIt) 0xFF664400.toInt() else 0xFF222222.toInt()
            canvas.drawRoundRect(evolutionRects[i], 6f, 6f, btnFillPaint)

            btnStrokePaint.color = if (hasIt) 0xFFFF8800.toInt() else 0xFF444444.toInt()
            canvas.drawRoundRect(evolutionRects[i], 6f, 6f, btnStrokePaint)

            val name = evolvedDef?.name ?: evolvedId
            btnTextPaint.color = if (hasIt) 0xFFFFFFFF.toInt() else 0xFF777777.toInt()
            btnTextPaint.textSize = 11f
            canvas.drawText(name, bx + buttonWidth / 2f, by + buttonHeight * 0.40f, btnTextPaint)

            infoPaint.color = if (hasIt) 0xFFFFCC88.toInt() else 0xFF555555.toInt()
            canvas.drawText(weapon.name, bx + buttonWidth / 2f, by + buttonHeight * 0.72f, infoPaint)
        }
        btnTextPaint.textSize = 13f

        y += evoSectionHeight + 20f

        val resetBtnWidth = screenWidth * 0.28f
        val resetBtnHeight = 60f
        val gap = 12f
        val totalResetWidth = resetBtnWidth * 3 + gap * 2
        val startX = (screenWidth - totalResetWidth) / 2f

        resetSmallRect = RectF(startX, y, startX + resetBtnWidth, y + resetBtnHeight)
        btnFillPaint.color = 0xFF442222.toInt()
        canvas.drawRoundRect(resetSmallRect, 8f, 8f, btnFillPaint)
        btnStrokePaint.color = 0xFFAA4444.toInt()
        canvas.drawRoundRect(resetSmallRect, 8f, 8f, btnStrokePaint)
        closeTextPaint.color = 0xFFFFAAAA.toInt()
        closeTextPaint.textSize = 16f
        canvas.drawText("RESET + \u00A5100", resetSmallRect.centerX(), resetSmallRect.centerY() + 6f, closeTextPaint)

        val bigX = startX + resetBtnWidth + gap
        resetBigRect = RectF(bigX, y, bigX + resetBtnWidth, y + resetBtnHeight)
        btnFillPaint.color = 0xFF224422.toInt()
        canvas.drawRoundRect(resetBigRect, 8f, 8f, btnFillPaint)
        btnStrokePaint.color = 0xFF44AA44.toInt()
        canvas.drawRoundRect(resetBigRect, 8f, 8f, btnStrokePaint)
        closeTextPaint.color = 0xFFAAFFAA.toInt()
        canvas.drawText("RICH RESET", resetBigRect.centerX(), resetBigRect.centerY() + 6f, closeTextPaint)

        val dieX = bigX + resetBtnWidth + gap
        dieRect = RectF(dieX, y, dieX + resetBtnWidth, y + resetBtnHeight)
        btnFillPaint.color = 0xFF441111.toInt()
        canvas.drawRoundRect(dieRect, 8f, 8f, btnFillPaint)
        btnStrokePaint.color = 0xFFFF2222.toInt()
        canvas.drawRoundRect(dieRect, 8f, 8f, btnStrokePaint)
        closeTextPaint.color = 0xFFFF4444.toInt()
        canvas.drawText("DIE", dieRect.centerX(), dieRect.centerY() + 6f, closeTextPaint)

        closeTextPaint.color = 0xFFCCCCCC.toInt()
        closeTextPaint.textSize = 20f
    }

    // =======================================================================
    // Page 2: Story Debug
    // =======================================================================

    private fun renderPhase4Page(canvas: Canvas, state: GameState) {
        var y = 60f

        canvas.drawText("STORY DEBUG", screenWidth / 2f, y, titlePaint)
        y += 24f

        infoPaint.color = 0xFF888888.toInt()
        val phaseName = when (state.debugStoryPhase) { 0 -> "NORMAL"; 1 -> "CORRUPT"; 2 -> "ASTRO"; else -> "?" }
        canvas.drawText("Stage: $phaseName | Dead: ${state.debugDeadPilotCount}/11 | Crystal: ${if (state.debugCrystalUnlocked) "Y" else "N"}", screenWidth / 2f, y, infoPaint)
        y += 16f
        val loopText = "Loop: ${state.debugStoryLoop}"
        val brickText = if (state.debugCrystalBroken) " | BRICKED" else ""
        canvas.drawText("$loopText$brickText", screenWidth / 2f, y, infoPaint)
        y += 14f
        // Desert info line
        val desertStatus = when {
            state.debugDesertGoodEnding -> "good"
            state.debugDesertCompleted -> "done"
            else -> "none"
        }
        canvas.drawText("Desert:$desertStatus", screenWidth / 2f, y, infoPaint)
        y += 24f

        val btnWidth = screenWidth * 0.42f
        val btnHeight = 42f
        val gap = 8f
        val leftX = (screenWidth - btnWidth * 2 - gap) / 2f
        val rightX = leftX + btnWidth + gap

        // Row 1: BOSS NOW + SET CORRUPT
        bossNowRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, bossNowRect, "BOSS NOW", "Skip to 9:59", 0xFF442244.toInt(), 0xFFAA44AA.toInt())

        setCorruptRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, setCorruptRect, "SET CORRUPT", "storyPhase = 1", 0xFF442222.toInt(), 0xFFAA2222.toInt())

        y += btnHeight + gap

        // Row 3: KILL PILOT + KILL ALL
        killPilotRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, killPilotRect, "KILL PILOT", "Next alive pilot", 0xFF443322.toInt(), 0xFFAA6644.toInt())

        killAllRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, killAllRect, "KILL ALL", "All 11 pilots", 0xFF442211.toInt(), 0xFFAA4422.toInt())

        y += btnHeight + gap

        // Row 4: BUY CRYSTAL + RESET STORY
        unlockCrystalRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, unlockCrystalRect, "BUY CRYSTAL", "Unlock + purchase", 0xFF224444.toInt(), 0xFF44AAAA.toInt())

        resetStoryRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, resetStoryRect, "RESET STORY", "Clear all story", 0xFF224422.toInt(), 0xFF44AA44.toInt())

        y += btnHeight + gap

        // Row 5: UNBRICK + CLR DESERT
        unbrickRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        if (state.debugCrystalBroken) {
            drawPhase4Button(canvas, unbrickRect, "UNBRICK", "Clear crystal_broken", 0xFF224422.toInt(), 0xFF44AA44.toInt())
        } else {
            drawPhase4Button(canvas, unbrickRect, "UNBRICK", "Not bricked", 0xFF1a1a1a.toInt(), 0xFF333333.toInt())
        }

        clrDesertRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, clrDesertRect, "CLR DESERT", "Clear all desert", 0xFF442222.toInt(), 0xFFAA4444.toInt())

        y += btnHeight + gap

        // Row 6: GRANT BAND + CLR BAND
        grantBandRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, grantBandRect, "GRANT BAND", "All 12 bandanas", 0xFF223344.toInt(), 0xFF4488CC.toInt())

        clrBandRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, clrBandRect, "CLR BAND", "Clear bandanas", 0xFF332222.toInt(), 0xFFCC6666.toInt())

        y += btnHeight + gap

        // Row 7: CRYSTAL OPENING (full-width — entry point for the ~15s opening sequence)
        crystalOpeningRect = RectF(leftX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, crystalOpeningRect, "CRYSTAL OPENING", "Empty field + Astro lines", 0xFF1A2030.toInt(), 0xFF33D6CC.toInt())

        y += btnHeight + gap

        // Row 8: CRYSTAL FIGHT + CRYSTAL RELEASE (two-column)
        crystalFightRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, crystalFightRect, "CRYSTAL FIGHT", "90s survival fight", 0xFF1A2233.toInt(), 0xFF44BBEE.toInt())

        crystalReleaseRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, crystalReleaseRect, "CRYSTAL RELEASE", "Skip to ghost lance", 0xFF1A2433.toInt(), 0xFF44DDAA.toInt())

        y += btnHeight + gap

        // --- Desert section ---
        infoPaint.color = 0xFFAA8844.toInt()
        canvas.drawText("DESERT", screenWidth / 2f, y + 10f, infoPaint)
        y += 18f

        // Row 6: DESERT + DESERT P2
        playDesertRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, playDesertRect, "DESERT", "Start phase 0", 0xFF443311.toInt(), 0xFFAA8833.toInt())

        playDesertP2Rect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, playDesertP2Rect, "DESERT P2", "Escalation", 0xFF443311.toInt(), 0xFFAA6622.toInt())

        y += btnHeight + gap

        // Row 7: CRYSTAL + DST FLAGS
        desertCrystalRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, desertCrystalRect, "CRYSTAL", "Crystal phase", 0xFF224444.toInt(), 0xFF44AACC.toInt())

        setDesertFlagsRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, setDesertFlagsRect, "DST FLAGS", "Done + good ending", 0xFF443322.toInt(), 0xFFAA8844.toInt())

        y += btnHeight + gap

        // Row 9: RECKONING ROUNDS + ASTRO LOOP
        reckoningRoundsRect = RectF(leftX, y, leftX + btnWidth, y + btnHeight)
        drawPhase4Button(canvas, reckoningRoundsRect, "ROUNDS: ${state.debugReckoningRounds}",
            "Tap: +1 (wraps at 25)", 0xFF222A44.toInt(), 0xFF6688CC.toInt())

        astroLoopRect = RectF(rightX, y, rightX + btnWidth, y + btnHeight)
        val astroLabel = if (state.debugAstroLoopMode) "ASTRO: ON" else "ASTRO: OFF"
        val astroSub = if (state.debugAstroLoopMode) "Tap to clear" else "Tap to set"
        val astroFill = if (state.debugAstroLoopMode) 0xFF224444.toInt() else 0xFF1a1a1a.toInt()
        val astroStroke = if (state.debugAstroLoopMode) 0xFF44AAAA.toInt() else 0xFF333333.toInt()
        drawPhase4Button(canvas, astroLoopRect, astroLabel, astroSub, astroFill, astroStroke)

        y += btnHeight + gap

        // Row 10: LOOP 1 / LOOP 2 / LOOP 3
        val thirdWidth = (rightX + btnWidth - leftX) / 3f - gap / 2f
        loopRect1 = RectF(leftX, y, leftX + thirdWidth, y + btnHeight)
        loopRect2 = RectF(leftX + thirdWidth + gap / 2f, y, leftX + thirdWidth * 2 + gap / 2f, y + btnHeight)
        loopRect3 = RectF(leftX + thirdWidth * 2 + gap, y, rightX + btnWidth, y + btnHeight)
        val loop1Fill = if (state.debugStoryLoop == 1) 0xFF224422.toInt() else 0xFF222244.toInt()
        val loop1Stroke = if (state.debugStoryLoop == 1) 0xFF44AA44.toInt() else 0xFF4444AA.toInt()
        val loop2Fill = if (state.debugStoryLoop == 2) 0xFF224422.toInt() else 0xFF222244.toInt()
        val loop2Stroke = if (state.debugStoryLoop == 2) 0xFF44AA44.toInt() else 0xFF4444AA.toInt()
        val loop3Fill = if (state.debugStoryLoop == 3) 0xFF224422.toInt() else 0xFF222244.toInt()
        val loop3Stroke = if (state.debugStoryLoop == 3) 0xFF44AA44.toInt() else 0xFF4444AA.toInt()
        drawPhase4Button(canvas, loopRect1, "LOOP 1", "First run", loop1Fill, loop1Stroke)
        drawPhase4Button(canvas, loopRect2, "LOOP 2", "Stop option on", loop2Fill, loop2Stroke)
        drawPhase4Button(canvas, loopRect3, "LOOP 3", "Nudge loop", loop3Fill, loop3Stroke)
    }

    private fun drawPhase4Button(canvas: Canvas, rect: RectF, title: String, subtitle: String, fillColor: Int, strokeColor: Int) {
        btnFillPaint.color = fillColor
        canvas.drawRoundRect(rect, 8f, 8f, btnFillPaint)
        btnStrokePaint.color = strokeColor
        canvas.drawRoundRect(rect, 8f, 8f, btnStrokePaint)
        closeTextPaint.color = 0xFFFFFFFF.toInt()
        closeTextPaint.textSize = 16f
        canvas.drawText(title, rect.centerX(), rect.centerY() - 2f, closeTextPaint)
        infoPaint.color = 0xFF999999.toInt()
        canvas.drawText(subtitle, rect.centerX(), rect.centerY() + 16f, infoPaint)
        closeTextPaint.color = 0xFFCCCCCC.toInt()
        closeTextPaint.textSize = 20f
    }

    // =======================================================================
    // Page 3: Flak Designs (designs 20–39)
    // =======================================================================

    private val flakHighlightPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0xFFFFFF44.toInt()
        isAntiAlias = true
    }
    private val flakCellBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0xFF111122.toInt()
    }
    private val flakCellStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0xFF334455.toInt()
    }
    private val flakIndexPaint = Paint().apply {
        color = 0xFF888888.toInt()
        textSize = 10f
        isAntiAlias = true
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.CENTER
    }

    private fun renderFlakDesignsPage(canvas: Canvas, state: GameState) {
        var y = 60f

        canvas.drawText("FLAK DESIGNS", screenWidth / 2f, y, titlePaint)
        y += 24f

        infoPaint.color = 0xFF888888.toInt()
        canvas.drawText("Randomly selected: #35, #36, #38", screenWidth / 2f, y, infoPaint)
        y += 22f

        // 3 cells in a single row
        val cols = 3
        val gridLeft = marginX
        val gridRight = screenWidth - marginX
        val gridBottom = screenHeight - 120f
        val cellW = (gridRight - gridLeft - (cols - 1) * 4f) / cols
        val cellH = gridBottom - y
        val previewRadius = (cellW.coerceAtMost(cellH) * 0.38f)

        for (i in 0 until 3) {
            val cellX = gridLeft + i * (cellW + 4f)
            val cellY = y
            flakCellRects[i] = RectF(cellX, cellY, cellX + cellW, cellY + cellH)

            val designIndex = flakActiveDesigns[i]

            canvas.drawRoundRect(flakCellRects[i], 4f, 4f, flakCellBgPaint)
            canvas.drawRoundRect(flakCellRects[i], 4f, 4f, flakCellStrokePaint)

            val cx = cellX + cellW / 2f
            val cy = cellY + cellH * 0.48f

            canvas.save()
            canvas.clipRect(cellX + 1f, cellY + 1f, cellX + cellW - 1f, cellY + cellH - 1f)
            FlakDesigns.render(canvas, designIndex, cx, cy, previewRadius, debugFlakAge)
            canvas.restore()

            flakIndexPaint.color = 0xFF666677.toInt()
            canvas.drawText("#$designIndex", cx, cellY + cellH - 4f, flakIndexPaint)
        }
    }

    private fun handleFlakDesignsTouch(ex: Float, ey: Float, state: GameState): String? {
        return null
    }

    // =======================================================================
    // Page 5: Thruster Designs
    // =======================================================================

    private fun drawThrusterPage(canvas: Canvas) {
        val cols = 4; val rows = 5
        val topPad = 80f; val botPad = 120f
        val cellW = screenWidth / cols.toFloat()
        val cellH = (screenHeight - topPad - botPad) / rows.toFloat()

        canvas.drawText("THRUSTER DESIGNS", screenWidth / 2f, 55f, titlePaint)

        val designs: List<(Canvas, Float, Float, Float, ShapeRenderer) -> Unit> = listOf(
            { c, cx, cy, s, sr -> ThrusterDesigns.design01_current(c, cx, cy, s, sr) },
            ThrusterDesigns::design02_narrowJet,
            ThrusterDesigns::design03_wideFan,
            ThrusterDesigns::design04_diamond,
            ThrusterDesigns::design05_doubleCone,
            ThrusterDesigns::design06_stepped,
            ThrusterDesigns::design07_ionicBlue,
            ThrusterDesigns::design08_plasmaPulse,
            ThrusterDesigns::design09_afterburner,
            ThrusterDesigns::design10_wave,
            ThrusterDesigns::design11_splitFork,
            { c, cx, cy, s, sr -> ThrusterDesigns.design12_diffuseCloud(c, cx, cy, s, sr) },
            ThrusterDesigns::design13_cometTail,
            ThrusterDesigns::design14_arrowhead,
            ThrusterDesigns::design15_starBurst,
            ThrusterDesigns::design16_pulseRing,
            ThrusterDesigns::design17_chevron,
            ThrusterDesigns::design18_spiral,
            ThrusterDesigns::design19_flare,
            ThrusterDesigns::design20_crystal
        )

        val labelPaint = Paint().apply {
            color = 0xFF888888.toInt()
            textSize = 11f
            textAlign = Paint.Align.CENTER
            typeface = FontManager.getRegular()
            isAntiAlias = true
        }
        val names = listOf(
            "1 CURRENT","2 NARROW JET","3 WIDE FAN","4 DIAMOND",
            "5 DBL CONE","6 STEPPED","7 IONIC","8 PLASMA",
            "9 AFTERBURN","10 WAVE","11 FORK","12 CLOUD",
            "13 COMET","14 ARROWHEAD","15 STARBURST","16 PULSE RING",
            "17 CHEVRON","18 SPIRAL","19 FLARE","20 CRYSTAL"
        )

        val cellBgPaint = Paint().apply { color = 0x22FFFFFF.toInt(); style = Paint.Style.FILL }
        val shipPaint = Paint().apply {
            color = 0xFF888888.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        for (i in 0 until 20) {
            val col = i % cols
            val row = i / cols
            val cx = cellW * col + cellW * 0.65f
            val cy = topPad + cellH * row + cellH * 0.5f
            val shipSize = minOf(cellW, cellH) * 0.18f

            // Cell background
            val cellLeft = cellW * col + 4f
            val cellTop = topPad + cellH * row + 4f
            canvas.drawRoundRect(
                android.graphics.RectF(cellLeft, cellTop, cellLeft + cellW - 8f, cellTop + cellH - 8f),
                8f, 8f, cellBgPaint)

            // Ship body (triangle pointing right)
            val path = android.graphics.Path().apply {
                moveTo(cx + shipSize, cy)
                lineTo(cx - shipSize * 0.7f, cy - shipSize * 0.6f)
                lineTo(cx - shipSize * 0.4f, cy)
                lineTo(cx - shipSize * 0.7f, cy + shipSize * 0.6f)
                close()
            }
            canvas.drawPath(path, shipPaint)

            // Thruster at nozzle (left indent of ship)
            val nozzleX = cx - shipSize * 0.4f
            val nozzleY = cy
            designs[i](canvas, nozzleX, nozzleY, shipSize, shapeRenderer)

            // Label
            val labelY = cellTop + cellH - 6f
            canvas.drawText(names[i], cx, labelY, labelPaint)
        }
    }

    // =======================================================================
    // Page 6: Black Market Designs
    // =======================================================================

    private fun renderBlackMarketPage(canvas: Canvas, state: GameState) {
        canvas.drawText("BLACK MARKET", screenWidth / 2f, 55f, titlePaint)

        // Starfield backdrop (designs are transparent, so stars show through them)
        drawDebugStarfield(canvas)

        val listTop = 78f
        val listBottom = screenHeight - 120f
        val bandH = listBottom - listTop          // one design fills the viewport
        val maxScroll = (BlackMarketDesigns.COUNT * bandH - (listBottom - listTop)).coerceAtLeast(0f)
        blackMarketScrollY = blackMarketScrollY.coerceIn(0f, maxScroll)

        canvas.save()
        canvas.clipRect(0f, listTop, screenWidth, listBottom)
        canvas.translate(0f, -blackMarketScrollY)

        for (i in 0 until BlackMarketDesigns.COUNT) {
            val top = listTop + i * bandH
            // cull bands fully outside the viewport
            if (top + bandH < listTop + blackMarketScrollY || top > listTop + blackMarketScrollY + bandH) continue
            val bounds = RectF(marginX, top, screenWidth - marginX, top + bandH)
            BlackMarketDesigns.render(canvas, i, bounds)
            BlackMarketDesigns.drawMiniSlot(canvas, bounds)
            infoPaint.color = 0xFFCCCCDD.toInt()
            canvas.drawText("#${i + 1}  ${BlackMarketDesigns.NAMES[i]}", bounds.centerX(), top + 16f, infoPaint)
        }
        canvas.restore()
    }

    // =======================================================================
    // Shared UI
    // =======================================================================

    private val PAGE_COUNT = 7
    private val shapeRenderer = ShapeRenderer()

    private fun drawPageDots(canvas: Canvas, currentPage: Int) {
        val dotRadius = 6f
        val dotGap = 20f
        val totalWidth = PAGE_COUNT * dotRadius * 2 + (PAGE_COUNT - 1) * dotGap
        val startX = (screenWidth - totalWidth) / 2f + dotRadius
        val dotY = screenHeight - 100f

        for (i in 0 until PAGE_COUNT) {
            dotPaint.color = if (i == currentPage) 0xFFFFFFFF.toInt() else 0xFF555555.toInt()
            canvas.drawCircle(startX + i * (dotRadius * 2 + dotGap), dotY, dotRadius, dotPaint)
        }
    }

    private fun drawCloseButton(canvas: Canvas) {
        val closeBtnWidth = 200f
        val closeBtnHeight = 48f
        val closeBtnX = (screenWidth - closeBtnWidth) / 2f
        val closeBtnY = screenHeight - 60f
        closeButtonRect = RectF(closeBtnX, closeBtnY, closeBtnX + closeBtnWidth, closeBtnY + closeBtnHeight)

        closeBtnPaint.color = 0x44888888.toInt()
        canvas.drawRoundRect(closeButtonRect, 8f, 8f, closeBtnPaint)
        closeStrokePaint.color = 0xFF888888.toInt()
        canvas.drawRoundRect(closeButtonRect, 8f, 8f, closeStrokePaint)
        canvas.drawText("CLOSE", closeButtonRect.centerX(), closeButtonRect.centerY() + 7f, closeTextPaint)
    }

    // =======================================================================
    // Touch handling
    // =======================================================================

    fun handleTouch(event: MotionEvent, state: GameState): String? {
        val ex = event.x / renderScale
        val ey = event.y / renderScale
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartX = ex
                swipeStartY = ey
                isSwiping = false
                telemetryDragging = false
                telemetryLastY = ey
                telemetryScrollVelocity = 0f
                // Tuning scroll
                blackMarketLastY = ey
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ex - swipeStartX
                if (kotlin.math.abs(dx) > 50f) {
                    isSwiping = true
                }
                if (state.debugMenuPage == BLACK_MARKET_PAGE && !isSwiping) {
                    blackMarketScrollY += (blackMarketLastY - ey)
                    blackMarketLastY = ey
                }
            }
            MotionEvent.ACTION_UP -> {
                val dx = ex - swipeStartX
                if (isSwiping && kotlin.math.abs(dx) > 50f) {
                    if (dx < 0 && state.debugMenuPage < PAGE_COUNT - 1) {
                        state.debugMenuPage++
                    } else if (dx > 0 && state.debugMenuPage > 0) {
                        state.debugMenuPage--
                    }
                    isSwiping = false
                    return null
                }

                return handleTapForPage(ex, ey, state)
            }
        }
        return null
    }

    private fun handleTapForPage(ex: Float, ey: Float, state: GameState): String? {
        if (closeButtonRect.contains(ex, ey)) {
            state.debugMenuOpen = false
            return "CLOSE"
        }

        when (state.debugMenuPage) {
            0 -> return handleWeaponsPassivesTouch(ex, ey, state)
            1 -> return handleEvolutionsResetsTouch(ex, ey, state)
            2 -> return handlePhase4Touch(ex, ey, state)
            3 -> return handleFlakDesignsTouch(ex, ey, state)
        }
        return null
    }

    private fun handleWeaponsPassivesTouch(ex: Float, ey: Float, state: GameState): String? {
        val baseWeapons = WeaponDefinitions.getBaseWeapons()

        for (i in baseWeapons.indices) {
            if (weaponRects[i].contains(ex, ey)) {
                val weaponId = baseWeapons[i].id
                val currentLevel = state.getWeaponLevel(weaponId)
                if (currentLevel >= GameConfig.WEAPON_MAX_LEVEL) {
                    state.weaponLevels.remove(weaponId)
                } else if (currentLevel == 0) {
                    state.weaponLevels[weaponId] = 1
                } else {
                    state.weaponLevels[weaponId] = currentLevel + 1
                }
                return "WEAPON_TOGGLE"
            }
        }

        val passives = PassiveDefinitions.getAllPassives()
        for (i in passives.indices) {
            if (passiveRects[i].contains(ex, ey)) {
                val passiveId = passives[i].id
                val currentStacks = state.getPassiveStacks(passiveId)

                if (instantMaxPassives.contains(passiveId)) {
                    if (currentStacks > 0) {
                        state.passiveStacks.remove(passiveId)
                    } else {
                        state.passiveStacks[passiveId] = GameConfig.PASSIVE_MAX_STACKS
                    }
                } else {
                    if (currentStacks >= GameConfig.PASSIVE_MAX_STACKS) {
                        state.passiveStacks.remove(passiveId)
                    } else if (currentStacks == 0) {
                        state.passiveStacks[passiveId] = 1
                    } else {
                        state.passiveStacks[passiveId] = currentStacks + 1
                    }
                }
                state.recalculateStats()
                return "PASSIVE_TOGGLE"
            }
        }
        return null
    }

    private fun handleEvolutionsResetsTouch(ex: Float, ey: Float, state: GameState): String? {
        val baseWeapons = WeaponDefinitions.getBaseWeapons()

        for (i in baseWeapons.indices) {
            if (evolutionRects[i].contains(ex, ey)) {
                val weapon = baseWeapons[i]
                val evolvedId = weapon.evolutionWeaponId ?: return null
                return "EVOLVE:${weapon.id}:$evolvedId"
            }
        }

        if (resetSmallRect.contains(ex, ey)) {
            return "RESET_SMALL"
        }
        if (resetBigRect.contains(ex, ey)) {
            return "RESET_BIG"
        }
        if (dieRect.contains(ex, ey)) {
            return "INSTANT_DEATH"
        }

        return null
    }

    private fun handlePhase4Touch(ex: Float, ey: Float, state: GameState): String? {
        if (bossNowRect.contains(ex, ey)) return "BOSS_NOW"
        if (setCorruptRect.contains(ex, ey)) return "SET_CORRUPT"
        if (killPilotRect.contains(ex, ey)) return "KILL_PILOT"
        if (killAllRect.contains(ex, ey)) return "KILL_ALL"
        if (unlockCrystalRect.contains(ex, ey)) return "BUY_CRYSTAL"
        if (resetStoryRect.contains(ex, ey)) return "RESET_STORY"
        if (unbrickRect.contains(ex, ey)) return "UNBRICK"
        if (grantBandRect.contains(ex, ey)) return "GRANT_BANDANAS"
        if (clrBandRect.contains(ex, ey)) return "CLEAR_BANDANAS"
        if (crystalOpeningRect.contains(ex, ey)) return "CRYSTAL_OPENING"
        if (crystalFightRect.contains(ex, ey)) return "CRYSTAL_FIGHT"
        if (crystalReleaseRect.contains(ex, ey)) return "CRYSTAL_RELEASE"
        // Desert buttons
        if (playDesertRect.contains(ex, ey)) return "PLAY_DESERT"
        if (playDesertP2Rect.contains(ex, ey)) return "PLAY_DESERT_P2"
        if (desertCrystalRect.contains(ex, ey)) return "DESERT_CRYSTAL"
        if (astroLoopRect.contains(ex, ey)) return "TOGGLE_ASTRO_LOOP"
        if (reckoningRoundsRect.contains(ex, ey)) return "RECKONING_ROUNDS_INC"
        if (setDesertFlagsRect.contains(ex, ey)) return "SET_DESERT_FLAGS"
        if (clrDesertRect.contains(ex, ey)) return "CLR_DESERT"
        if (loopRect1.contains(ex, ey)) return "SET_LOOP_1"
        if (loopRect2.contains(ex, ey)) return "SET_LOOP_2"
        if (loopRect3.contains(ex, ey)) return "SET_LOOP_3"
        return null
    }

    // =======================================================================
    // Page 3: Flak Designs
    // =======================================================================

    private val telemetryLinePaint = Paint().apply {
        color = 0xFFCCCCCC.toInt()
        textSize = 14f
        isAntiAlias = true
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.LEFT
    }

    private val telemetryDetailPaint = Paint().apply {
        color = 0xFF999999.toInt()
        textSize = 12f
        isAntiAlias = true
        typeface = FontManager.getRegular()
        textAlign = Paint.Align.LEFT
    }

    private fun renderTelemetryPage(canvas: Canvas, state: GameState) {
        val tm = telemetryManager ?: return
        var y = 60f

        val runCount = tm.getRunCount()
        val sizeKB = String.format("%.1f", tm.getFileSizeKB())
        canvas.drawText("TELEMETRY ($runCount runs, ${sizeKB}KB)", screenWidth / 2f, y, titlePaint)
        y += 30f

        // CLEAR LOG button
        val clearBtnWidth = 200f
        val clearBtnHeight = 40f
        val clearBtnX = (screenWidth - clearBtnWidth) / 2f
        clearLogRect = RectF(clearBtnX, y, clearBtnX + clearBtnWidth, y + clearBtnHeight)
        btnFillPaint.color = 0xFF442222.toInt()
        canvas.drawRoundRect(clearLogRect, 8f, 8f, btnFillPaint)
        btnStrokePaint.color = 0xFFAA4444.toInt()
        canvas.drawRoundRect(clearLogRect, 8f, 8f, btnStrokePaint)
        closeTextPaint.color = 0xFFFFAAAA.toInt()
        closeTextPaint.textSize = 16f
        canvas.drawText("CLEAR LOG", clearLogRect.centerX(), clearLogRect.centerY() + 6f, closeTextPaint)
        closeTextPaint.color = 0xFFCCCCCC.toInt()
        closeTextPaint.textSize = 20f
        y += clearBtnHeight + 16f

        // Scrollable run list
        val listTop = y
        val listBottom = screenHeight - 120f
        canvas.save()
        canvas.clipRect(0f, listTop, screenWidth, listBottom)

        val summaries = tm.getRunSummaries()
        val lineHeight = 20f
        val detailLineHeight = 16f
        var drawY = listTop - telemetryScrollOffset

        for (i in summaries.indices) {
            // Summary line
            if (drawY + lineHeight > listTop - 20f && drawY < listBottom + 20f) {
                telemetryLinePaint.color = if (i == expandedRunIndex) 0xFF44CCFF.toInt() else 0xFFCCCCCC.toInt()
                canvas.drawText(summaries[i], marginX, drawY + lineHeight, telemetryLinePaint)
            }
            drawY += lineHeight + 4f

            // Expanded detail lines
            if (i == expandedRunIndex) {
                val details = tm.getRunDetail(i)
                for (detail in details) {
                    if (drawY + detailLineHeight > listTop - 20f && drawY < listBottom + 20f) {
                        canvas.drawText("  $detail", marginX, drawY + detailLineHeight, telemetryDetailPaint)
                    }
                    drawY += detailLineHeight + 2f
                }
                drawY += 4f
            }
        }

        // Clamp scroll to content
        val totalContentHeight = drawY + telemetryScrollOffset - listTop
        val maxScroll = (totalContentHeight - (listBottom - listTop)).coerceAtLeast(0f)
        telemetryScrollOffset = telemetryScrollOffset.coerceIn(0f, maxScroll)

        canvas.restore()

        if (summaries.isEmpty()) {
            infoPaint.color = 0xFF666666.toInt()
            canvas.drawText("No runs recorded yet", screenWidth / 2f, listTop + 40f, infoPaint)
        }

        // File path at bottom
        infoPaint.color = 0xFF555555.toInt()
        canvas.drawText("telemetry.json", screenWidth / 2f, screenHeight - 115f, infoPaint)
    }

    private fun handleTelemetryTouch(ex: Float, ey: Float, state: GameState): String? {
        if (clearLogRect.contains(ex, ey)) {
            return "CLEAR_TELEMETRY"
        }

        // Tap on run line toggles expansion
        val tm = telemetryManager ?: return null
        val summaries = tm.getRunSummaries()
        val listTop = 60f + 30f + 40f + 16f  // title + clearBtn + gap
        val lineHeight = 20f
        val detailLineHeight = 16f
        var drawY = listTop - telemetryScrollOffset

        for (i in summaries.indices) {
            val lineTop = drawY
            val lineBot = drawY + lineHeight + 4f
            drawY = lineBot

            if (i == expandedRunIndex) {
                val details = tm.getRunDetail(i)
                drawY += details.size * (detailLineHeight + 2f) + 4f
            }

            if (ey in lineTop..lineBot && ex > marginX) {
                expandedRunIndex = if (expandedRunIndex == i) -1 else i
                return null
            }
        }

        return null
    }

    // =======================================================================
    // Page 4: Weapon Tuning
    // =======================================================================

    private fun renderTuningPage(canvas: Canvas, state: GameState) {
        var y = 60f

        canvas.drawText("WEAPON TUNING", screenWidth / 2f, y, titlePaint)
        y += 36f

        // Set buttons row
        val setBtnWidth = (screenWidth - marginX * 2 - (SOUND_SETS.size - 1) * buttonGap) / SOUND_SETS.size
        val setBtnHeight = 36f
        for (s in SOUND_SETS.indices) {
            val bx = marginX + s * (setBtnWidth + buttonGap)
            val rect = RectF(bx, y, bx + setBtnWidth, y + setBtnHeight)
            tuningSetRects[s] = rect
            val isSelected = SOUND_SETS[s] == tuningPreviewSet
            btnFillPaint.color = if (isSelected) 0xFF006666.toInt() else 0xFF222222.toInt()
            canvas.drawRoundRect(rect, 6f, 6f, btnFillPaint)
            btnStrokePaint.color = if (isSelected) 0xFF00CCCC.toInt() else 0xFF666666.toInt()
            canvas.drawRoundRect(rect, 6f, 6f, btnStrokePaint)
            closeTextPaint.color = if (isSelected) 0xFF00FFFF.toInt() else 0xFFAAAAAA.toInt()
            closeTextPaint.textSize = 14f
            canvas.drawText(SOUND_SETS[s].uppercase(), rect.centerX(), rect.centerY() + 5f, closeTextPaint)
        }
        y += setBtnHeight + 8f

        // Play/Stop button
        val playStopWidth = screenWidth - marginX * 2
        val playStopHeight = 36f
        tuningPlayStopRect = RectF(marginX, y, marginX + playStopWidth, y + playStopHeight)
        if (tuningBGMPlaying) {
            btnFillPaint.color = 0xFF442222.toInt()
            canvas.drawRoundRect(tuningPlayStopRect, 6f, 6f, btnFillPaint)
            btnStrokePaint.color = 0xFFFF4444.toInt()
            canvas.drawRoundRect(tuningPlayStopRect, 6f, 6f, btnStrokePaint)
            closeTextPaint.color = 0xFFFF6666.toInt()
            closeTextPaint.textSize = 16f
            canvas.drawText("STOP", tuningPlayStopRect.centerX(), tuningPlayStopRect.centerY() + 6f, closeTextPaint)
        } else {
            btnFillPaint.color = 0xFF224422.toInt()
            canvas.drawRoundRect(tuningPlayStopRect, 6f, 6f, btnFillPaint)
            btnStrokePaint.color = 0xFF44AA44.toInt()
            canvas.drawRoundRect(tuningPlayStopRect, 6f, 6f, btnStrokePaint)
            closeTextPaint.color = 0xFFAAFFAA.toInt()
            closeTextPaint.textSize = 16f
            canvas.drawText("PLAY", tuningPlayStopRect.centerX(), tuningPlayStopRect.centerY() + 6f, closeTextPaint)
        }
        y += playStopHeight + 8f

        // Scrollable weapon tuning grid
        tuningGridTop = y
        tuningGridBottom = screenHeight - 120f

        canvas.save()
        canvas.clipRect(0f, tuningGridTop, screenWidth, tuningGridBottom)
        canvas.translate(0f, -tuningScrollY)

        val labelWidth = 60f
        val gridAvailWidth = screenWidth - marginX * 2 - labelWidth
        val btnGap = 2f
        val btnSize = ((gridAvailWidth - (TUNING_BTN_COUNT - 1) * btnGap) / TUNING_BTN_COUNT).coerceAtLeast(22f)
        val rowHeight = btnSize + 4f
        val blockHeight = rowHeight * 2 + 16f  // 2 rows + weapon name + gap

        for (i in tuningWeapons.indices) {
            val weaponId = tuningWeapons[i]
            val blockY = tuningGridTop + i * blockHeight
            val volSel = tuningVolSelections[weaponId] ?: 5
            val beatSel = tuningBeatSelections[weaponId] ?: 5
            val isAutoPlaying = SoundManager.tuningAutoPlay.containsKey(weaponId)

            // Weapon name
            btnTextPaint.color = if (isAutoPlaying) 0xFFFFFF00.toInt() else 0xFFCCCCCC.toInt()
            btnTextPaint.textSize = 11f
            btnTextPaint.textAlign = Paint.Align.LEFT
            val abbrev = weaponId.replace("_", " ").split(" ").joinToString(" ") { it.take(4) }
            canvas.drawText(abbrev, marginX, blockY + 10f, btnTextPaint)
            btnTextPaint.textAlign = Paint.Align.CENTER

            // VOL label
            btnTextPaint.color = 0xFF888888.toInt()
            btnTextPaint.textSize = 9f
            btnTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("VOL", marginX, blockY + 14f + rowHeight * 0.65f, btnTextPaint)
            btnTextPaint.textAlign = Paint.Align.CENTER

            // Volume buttons
            val volRowY = blockY + 14f
            for (v in 0 until TUNING_BTN_COUNT) {
                val bx = marginX + labelWidth + v * (btnSize + btnGap)
                val rect = RectF(bx, volRowY, bx + btnSize, volRowY + btnSize)
                tuningVolRects[i][v] = rect
                val isSel = v == volSel
                when {
                    isSel && isAutoPlaying -> {
                        btnFillPaint.color = 0xFF444400.toInt()
                        btnStrokePaint.color = 0xFFFFFF00.toInt()
                        btnNumPaint.color = 0xFFFFFF00.toInt()
                    }
                    isSel -> {
                        btnFillPaint.color = 0xFF003344.toInt()
                        btnStrokePaint.color = 0xFF00CCCC.toInt()
                        btnNumPaint.color = 0xFF00CCCC.toInt()
                    }
                    v == 5 -> {
                        btnFillPaint.color = 0xFF1a1a1a.toInt()
                        btnStrokePaint.color = 0xFF555555.toInt()
                        btnNumPaint.color = 0xFF888888.toInt()
                    }
                    else -> {
                        btnFillPaint.color = 0xFF1a1a1a.toInt()
                        btnStrokePaint.color = 0xFF333333.toInt()
                        btnNumPaint.color = 0xFF555555.toInt()
                    }
                }
                canvas.drawRoundRect(rect, 3f, 3f, btnFillPaint)
                canvas.drawRoundRect(rect, 3f, 3f, btnStrokePaint)
                btnNumPaint.textSize = 10f
                canvas.drawText("${v + 1}", rect.centerX(), rect.centerY() + 3f, btnNumPaint)
            }

            // BEAT label
            btnTextPaint.color = 0xFF888888.toInt()
            btnTextPaint.textSize = 9f
            btnTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("BEAT", marginX, blockY + 14f + rowHeight + rowHeight * 0.65f, btnTextPaint)
            btnTextPaint.textAlign = Paint.Align.CENTER

            // Beat offset buttons
            val beatRowY = blockY + 14f + rowHeight
            for (v in 0 until TUNING_BTN_COUNT) {
                val bx = marginX + labelWidth + v * (btnSize + btnGap)
                val rect = RectF(bx, beatRowY, bx + btnSize, beatRowY + btnSize)
                tuningBeatRects[i][v] = rect
                val isSel = v == beatSel
                when {
                    isSel && isAutoPlaying -> {
                        btnFillPaint.color = 0xFF444400.toInt()
                        btnStrokePaint.color = 0xFFFFFF00.toInt()
                        btnNumPaint.color = 0xFFFFFF00.toInt()
                    }
                    isSel -> {
                        btnFillPaint.color = 0xFF330033.toInt()
                        btnStrokePaint.color = 0xFFCC44CC.toInt()
                        btnNumPaint.color = 0xFFCC44CC.toInt()
                    }
                    v == 5 -> {
                        btnFillPaint.color = 0xFF1a1a1a.toInt()
                        btnStrokePaint.color = 0xFF555555.toInt()
                        btnNumPaint.color = 0xFF888888.toInt()
                    }
                    else -> {
                        btnFillPaint.color = 0xFF1a1a1a.toInt()
                        btnStrokePaint.color = 0xFF333333.toInt()
                        btnNumPaint.color = 0xFF555555.toInt()
                    }
                }
                canvas.drawRoundRect(rect, 3f, 3f, btnFillPaint)
                canvas.drawRoundRect(rect, 3f, 3f, btnStrokePaint)
                btnNumPaint.textSize = 10f
                canvas.drawText("${v + 1}", rect.centerX(), rect.centerY() + 3f, btnNumPaint)
            }
        }

        canvas.restore()
        btnNumPaint.textSize = 18f
        btnTextPaint.textSize = 13f
        closeTextPaint.color = 0xFFCCCCCC.toInt()
        closeTextPaint.textSize = 20f
    }

    private fun handleTuningTouch(x: Float, y: Float): String? {
        // Set buttons
        for (s in SOUND_SETS.indices) {
            if (tuningSetRects[s].contains(x, y)) {
                tuningPreviewSet = SOUND_SETS[s]
                return "TUNING_SET:${SOUND_SETS[s]}"
            }
        }

        // Play/Stop
        if (tuningPlayStopRect.contains(x, y)) {
            return "TUNING_BGM_TOGGLE"
        }

        // Weapon grid (adjusted for scroll)
        if (y >= tuningGridTop && y <= tuningGridBottom) {
            val adjustedY = y + tuningScrollY
            val labelWidth = 60f
            val gridAvailWidth = screenWidth - marginX * 2 - labelWidth
            val btnGap = 2f
            val btnSize = ((gridAvailWidth - (TUNING_BTN_COUNT - 1) * btnGap) / TUNING_BTN_COUNT).coerceAtLeast(22f)
            val rowHeight = btnSize + 4f
            val blockHeight = rowHeight * 2 + 16f

            for (i in tuningWeapons.indices) {
                val blockY = tuningGridTop + i * blockHeight
                val volRowY = blockY + 14f
                val beatRowY = blockY + 14f + rowHeight

                // Volume buttons
                if (adjustedY >= volRowY && adjustedY <= volRowY + btnSize) {
                    for (v in 0 until TUNING_BTN_COUNT) {
                        val bx = marginX + labelWidth + v * (btnSize + btnGap)
                        if (x >= bx && x <= bx + btnSize) {
                            tuningVolSelections[tuningWeapons[i]] = v
                            return "TUNING_VOL:$i:$v"
                        }
                    }
                }

                // Beat buttons
                if (adjustedY >= beatRowY && adjustedY <= beatRowY + btnSize) {
                    for (v in 0 until TUNING_BTN_COUNT) {
                        val bx = marginX + labelWidth + v * (btnSize + btnGap)
                        if (x >= bx && x <= bx + btnSize) {
                            tuningBeatSelections[tuningWeapons[i]] = v
                            return "TUNING_BEAT:$i:$v"
                        }
                    }
                }
            }
        }

        return null
    }

    fun updateTuningScroll() {
        if (tuningScrollVelocity != 0f) {
            tuningScrollY += tuningScrollVelocity
            tuningScrollVelocity *= 0.95f
            if (Math.abs(tuningScrollVelocity) < 0.5f) tuningScrollVelocity = 0f
            val labelWidth = 60f
            val gridAvailWidth = screenWidth - marginX * 2 - labelWidth
            val btnGap = 2f
            val btnSize = ((gridAvailWidth - (TUNING_BTN_COUNT - 1) * btnGap) / TUNING_BTN_COUNT).coerceAtLeast(22f)
            val rowHeight = btnSize + 4f
            val blockHeight = rowHeight * 2 + 16f
            val gridVisibleHeight = tuningGridBottom - tuningGridTop
            val maxScroll = (tuningWeapons.size * blockHeight - gridVisibleHeight).coerceAtLeast(0f)
            tuningScrollY = tuningScrollY.coerceIn(0f, maxScroll)
        }
    }

    fun handleTap(x: Float, y: Float, state: GameState): String? {
        return null
    }
}
