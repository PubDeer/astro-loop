package com.astroloop.game.hangar

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.LayoutRect
import com.astroloop.game.core.ScreenLayout
import java.util.concurrent.CopyOnWriteArrayList
import com.astroloop.game.data.BandanaDefinitions
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.render.FontManager
import com.astroloop.game.render.IconRenderer
import com.astroloop.game.render.ShapeRenderer
import com.astroloop.game.render.ShipRenderer
import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.entity.Boss
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class HangarRenderer(private val persistence: PersistenceManager) {

    private val shapeRenderer = ShapeRenderer()
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var roomWidth = 0f

    // --- Paints ---
    private val textPaint = Paint().apply {
        color = 0xFFCCCCCC.toInt()
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = FontManager.getRegular()
    }
    private val costPaint = Paint().apply {
        color = 0xFFFFAA00.toInt()
        textSize = 18f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = FontManager.getRegular()
    }

    // --- Layout ---
    private var layout: ScreenLayout = ScreenLayout.compute(GameConfig.DESIGN_WIDTH, GameConfig.DESIGN_HEIGHT)
    var shipCenterY = 0f
    var shipSpacing = 0f
    private var walkwayY = 0f
    private var ceilingY = 0f

    // --- Page sub-renderers ---
    private val barPageRenderer = BarPageRenderer(textPaint, costPaint, persistence)
    private val storePageRenderer = StorePageRenderer(persistence, textPaint, costPaint)

    // --- Tap rects (delegated to sub-renderers) ---
    val upgradeRects get() = storePageRenderer.upgradeRects
    val storeButtonRects get() = storePageRenderer.storeButtonRects
    val spinButtonRect get() = storePageRenderer.spinButtonRect
    val codexBookRect get() = barPageRenderer.codexBookRect
    private val pilotCardRects get() = barPageRenderer.pilotCardRects
    // --- Stars ---
    private data class HangarStar(val x: Float, val y: Float, val size: Float, val color: Int,
                                      val pulseSpeed: Float = 0f)  // 0 = static, >0 = pulsating
    @Volatile private var stars = listOf<HangarStar>()
    private val starPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Hyperspace phase reusable objects (avoid per-frame allocation)
    private val hyperBgPaint = Paint()
    private val hyperStarPaint = Paint().apply { style = Paint.Style.FILL }
    private val hyperStreakPaint = Paint().apply { strokeCap = Paint.Cap.ROUND }
    private val hyperStreakRandom = java.util.Random(42)
    private val hyperStarRandom = java.util.Random(123)
    private val glitchPaint = Paint()

    // ASTRO LOOP title (first-launch intro cinematic, launchpad page)
    private val introTitlePaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = FontManager.getDisplayBold()
        letterSpacing = 0.15f
    }

    fun initialize(layout: ScreenLayout, roomWidth: Float) {
        this.roomWidth = roomWidth
        this.layout = layout
        val width = layout.width
        val height = layout.height
        screenWidth = width
        screenHeight = height

        // Ship center position
        shipCenterY = height / 2f
        shipSpacing = layout.content.width * 0.30f

        // Walkway at ~60% screen height
        walkwayY = height * 0.60f
        ceilingY = walkwayY - 80f

        // Initialize sub-renderers with shared layout
        val roomFrameLambda = { canvas: Canvas, hasCeiling: Boolean, leftSolid: Boolean, rightSolid: Boolean ->
            drawRoomFrame(canvas, hasCeiling,
                if (leftSolid) RoomEdge.SOLID else RoomEdge.ARCHWAY,
                if (rightSolid) RoomEdge.SOLID else RoomEdge.ARCHWAY)
        }
        barPageRenderer.screenWidth = width
        barPageRenderer.roomWidth = roomWidth
        barPageRenderer.screenHeight = height
        barPageRenderer.walkwayY = walkwayY
        barPageRenderer.ceilingY = ceilingY
        barPageRenderer.content = layout.content
        barPageRenderer.drawRoomFrame = roomFrameLambda
        barPageRenderer.drawNPCWalkers = ::drawNPCWalkers
        barPageRenderer.drawCharacter = { canvas, x, y, color, walking, arm -> drawCharacter(canvas, x, y, color, walking, arm) }
        barPageRenderer.corrupted = StoryStateManager.isCorrupted(persistence)
        barPageRenderer.astroLoop = StoryStateManager.isAstroLoop(persistence)
        barPageRenderer.dressing = com.astroloop.game.hangar.BarDressing.forStage(
            StoryStateManager.stage(persistence)
        )
        storePageRenderer.screenWidth = width
        storePageRenderer.roomWidth = roomWidth
        storePageRenderer.screenHeight = height
        storePageRenderer.walkwayY = walkwayY
        storePageRenderer.ceilingY = ceilingY
        storePageRenderer.content = layout.content
        storePageRenderer.drawRoomFrame = roomFrameLambda

        // Generate static stars above walkway (built locally, then swapped atomically)
        val newStars = mutableListOf<HangarStar>()
        val starRandom = Random(42)
        repeat(40) {
            newStars.add(HangarStar(
                x = starRandom.nextFloat() * width,
                y = starRandom.nextFloat() * walkwayY,
                size = 1f + starRandom.nextFloat() * 0.5f,
                color = 0xFF444444.toInt()
            ))
        }
        repeat(25) {
            newStars.add(HangarStar(
                x = starRandom.nextFloat() * width,
                y = starRandom.nextFloat() * walkwayY,
                size = 1.5f + starRandom.nextFloat() * 0.5f,
                color = 0xFF888888.toInt()
            ))
        }
        repeat(10) {
            newStars.add(HangarStar(
                x = starRandom.nextFloat() * width,
                y = starRandom.nextFloat() * walkwayY,
                size = 2f + starRandom.nextFloat(),
                color = 0xFFCCCCCC.toInt()
            ))
        }
        // Pulsating/sparkling stars (faint, slow twinkle)
        repeat(8) {
            newStars.add(HangarStar(
                x = starRandom.nextFloat() * width,
                y = starRandom.nextFloat() * walkwayY,
                size = 1.5f + starRandom.nextFloat(),
                color = 0xFFAABBDD.toInt(),
                pulseSpeed = 600f + starRandom.nextFloat() * 800f  // Varied pulse periods
            ))
        }
        stars = newStars
    }

    // =======================================================================
    // Main render
    // =======================================================================

    fun render(canvas: Canvas, state: HangarState) {
        // Background
        canvas.drawColor(0xFF000011.toInt())

        when (state.phase) {
            HangarPhase.BROWSING -> {
                drawStars(canvas)

                // Draw current page content with scroll offset for peeking
                drawPageContent(canvas, state)

                // Walkway and pilot walker (drawn over page content)
                drawWalkway(canvas, state)
                drawPilotWalker(canvas, state)
                // The intro cinematic hides all HUD chrome (nav labels + yen counter)
                // and instead shows the ASTRO LOOP title on the launchpad.
                if (state.introCinematic) {
                    if (state.currentPage == 1) {
                        drawIntroTitle(canvas, state)
                    }
                } else {
                    // Page indicator dots
                    drawPageIndicator(canvas, state)

                    // Yen counter — fades with ship drag on shipyard page
                    val yenAlpha = if (state.currentPage == 1) shipDragFade(state) else 1f
                    drawYenCounter(canvas, state, yenAlpha)
                }
            }
            HangarPhase.LAUNCHING -> {
                drawStars(canvas)
                // Walkway stays visible until liftoff, then drops away
                // Phase 0-1: static walkway. Phase 2: drops (drawn inside sequence). Phase 3: gone.
                if (state.launchPhase < 2) {
                    drawWalkway(canvas, state)
                }
                drawLaunchSequence(canvas, state)
            }
            HangarPhase.CODEX -> {
                drawStars(canvas)
                barPageRenderer.draw(canvas, state, 0f)
                drawWalkway(canvas, state)
                drawCodex(canvas, state)
            }
        }

        // Glitch overlay (death return, fades over 1 second)
        drawGlitchOverlay(canvas, state)

        // Fade from black overlay (corruption death return)
        if (state.fadeFromBlackTimer > 0f) {
            val alpha = (state.fadeFromBlackTimer / 1.0f).coerceIn(0f, 1f)
            canvas.drawColor(android.graphics.Color.argb((alpha * 255).toInt(), 0, 0, 0))
        }
    }

    private fun drawGlitchOverlay(canvas: Canvas, state: HangarState) {
        val t = state.glitchTimer
        if (t <= 0f) return

        val seed = System.currentTimeMillis() / 50L
        val rng = java.util.Random(seed)
        val w = screenWidth
        val h = screenHeight

        // Pixel scramble: 10 colored rects at random positions
        repeat(10) {
            val rx = rng.nextFloat() * w
            val ry = rng.nextFloat() * h
            val rw = (20f + rng.nextFloat() * 60f)
            val rh = (8f + rng.nextFloat() * 20f)
            glitchPaint.color = when (rng.nextInt(3)) {
                0 -> android.graphics.Color.rgb(255, 0, 0)
                1 -> android.graphics.Color.rgb(0, 255, 0)
                else -> android.graphics.Color.rgb(0, 100, 255)
            }
            glitchPaint.alpha = (t * 128).toInt().coerceIn(0, 128)
            canvas.drawRect(rx, ry, rx + rw, ry + rh, glitchPaint)
        }

        // Screen tear: 4 horizontal bands
        repeat(4) {
            val ty = rng.nextFloat() * h
            val th = (3f + rng.nextFloat() * 8f)
            glitchPaint.color = if (rng.nextBoolean())
                android.graphics.Color.WHITE else android.graphics.Color.BLACK
            glitchPaint.alpha = (t * 100).toInt().coerceIn(0, 100)
            canvas.drawRect(0f, ty, w, ty + th, glitchPaint)
        }

        // Color fringe: 2 thin horizontal lines (red + cyan offset)
        val fringeY = rng.nextFloat() * h

        glitchPaint.color = android.graphics.Color.rgb(255, 50, 50)
        glitchPaint.alpha = (t * 80).toInt().coerceIn(0, 80)
        canvas.drawRect(0f, fringeY, w, fringeY + 2f, glitchPaint)

        glitchPaint.color = android.graphics.Color.rgb(0, 220, 220)
        glitchPaint.alpha = (t * 80).toInt().coerceIn(0, 80)
        canvas.drawRect(0f, fringeY + 4f, w, fringeY + 6f, glitchPaint)
    }

    // =======================================================================
    // Stars
    // =======================================================================

    private fun drawStars(canvas: Canvas) {
        val time = System.currentTimeMillis()
        for (star in stars) {
            if (star.pulseSpeed > 0f) {
                // Pulsating star — fades in and out
                val pulse = (0.3f + 0.7f * ((sin(time / star.pulseSpeed.toDouble()) + 1f) / 2f)).toFloat()
                starPaint.color = star.color
                starPaint.alpha = (pulse * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(star.x, star.y, star.size * (0.8f + 0.2f * pulse), starPaint)
                starPaint.alpha = 255
            } else {
                starPaint.color = star.color
                canvas.drawCircle(star.x, star.y, star.size, starPaint)
            }
        }
    }

    // =======================================================================
    // Page content dispatcher
    // =======================================================================

    private fun drawPageContent(canvas: Canvas, state: HangarState) {
        // Rooms tile edge to edge one roomWidth apart. Below sw600dp roomWidth == screenWidth,
        // so this is arithmetically identical to the single-page-per-screen layout.
        val stride = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)
        val viewportX = viewportX(state)

        // A room is visible if any part of it falls inside the screen. The test is against
        // screenWidth, not stride: on wide screens several rooms are on screen at once.
        val barX = 0f - viewportX
        if (barX > -stride && barX < screenWidth) {
            barPageRenderer.draw(canvas, state, -barX)
        }

        val shipyardX = stride - viewportX
        if (shipyardX > -stride && shipyardX < screenWidth) {
            drawShipyardPage(canvas, state, -shipyardX)
        }

        val storeX = 2f * stride - viewportX
        if (storeX > -stride && storeX < screenWidth) {
            storePageRenderer.draw(canvas, state, -storeX)
        }
    }

    /**
     * World X of the left screen edge, for this frame's page and scroll offset. One definition
     * shared by the page pass, the walkway and the pilot walker — three hand-written copies of
     * it drifting apart is what put content in the wrong room in the first place.
     */
    private fun viewportX(state: HangarState): Float =
        HangarMetrics.viewportX(state.currentPage, state.pageScrollOffset, roomWidth, screenWidth)

    /**
     * Screen-space horizontal extent of the hangar building (its three rooms), for the walkway
     * and anything that must sit flush with it.
     *
     * Below the gate the building is exactly as wide as the screen (effectiveRoomWidth returns
     * screenWidth there), so it can never be narrower than the screen and the clip below must
     * never engage. HangarSurfaceView's rubber-band resistance damps pageScrollOffset toward zero
     * during edge overscroll but never clamps it to exactly zero, so feeding viewportX into the
     * clip in that branch would shave a sliver off the flush edge for the whole duration of the
     * drag. Bypass it entirely: the walkway always spans the full screen here, independent of
     * currentPage/pageScrollOffset.
     */
    private fun buildingExtent(state: HangarState): Pair<Float, Float> {
        val stride = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)
        if (stride >= screenWidth) return 0f to screenWidth
        val viewportX = viewportX(state)
        return (0f - viewportX).coerceAtLeast(0f) to (3f * stride - viewportX).coerceAtMost(screenWidth)
    }

    // =======================================================================
    // Shipyard page (fully functional)
    // =======================================================================

    private fun drawShipyardPage(canvas: Canvas, state: HangarState, xOffset: Float) {
        canvas.save()
        canvas.translate(-xOffset, 0f)
        // Clip to page bounds so ships don't bleed into adjacent pages
        val rw = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)
        canvas.clipRect(0f, 0f, rw, screenHeight)

        // Room frame: no ceiling (open to space), archways on both sides
        drawRoomFrame(canvas, false, RoomEdge.ARCHWAY, RoomEdge.ARCHWAY)

        val ship = ShipDefinitions.getShipByIndex(state.selectedShipIndex)
        val isSelectedLocked = !state.isShipUnlocked(state.selectedShipIndex)
        val shipColor = when {
            isSelectedLocked -> 0xFF555555.toInt()
            StoryStateManager.isCorrupted(persistence) -> StoryStateManager.corruptShipColor(ship?.color ?: 0xFF00AAFF.toInt())
            else -> ship?.color ?: 0xFF00AAFF.toInt()
        }

        // Target zone indicator (energy field at center of the room — we're drawing in this
        // page's room-local space per the translate above, so it must centre on the room, not
        // the screen)
        // Brightness follows chevron logic: brightens on approach, pulses when in halo zone
        val dragProximity = if (state.isDraggingShip) {
            val dist = kotlin.math.abs(state.shipDragY - shipCenterY)
            val maxDist = state.shipRestingY - shipCenterY
            (1f - (dist / maxDist).coerceIn(0f, 1f))
        } else 0.2f
        val inHaloZone = state.isDraggingShip &&
            kotlin.math.abs(state.shipDragY - shipCenterY) < 60f
        drawEnergyField(canvas, shipColor, rw / 2f, dragProximity, inHaloZone)

        // Launch rail chevrons between resting position and target zone
        drawLaunchRail(canvas, state, shipColor)

        // Ships below walkway
        drawShips(canvas, state)

        canvas.restore()
    }

    // centerX is supplied by the caller rather than derived here: this is drawn both inside
    // drawShipyardPage's room-local translate (needs the room centre) and directly from
    // drawLaunchSequence in plain screen space (needs the screen centre), and the function has
    // no way to tell those two contexts apart on its own.
    private fun drawEnergyField(canvas: Canvas, shipColor: Int, centerX: Float, intensity: Float = 1f, inHaloZone: Boolean = false) {
        val shipY = shipCenterY
        val time = System.currentTimeMillis()

        val outerAlpha: Float
        val innerAlpha: Float

        if (inHaloZone) {
            // In halo zone — pulse at full intensity, "ready to launch"
            outerAlpha = 0.5f + 0.3f * kotlin.math.sin(time / 600.0).toFloat()
            innerAlpha = 0.4f + 0.25f * kotlin.math.sin(time / 400.0 + 1.0).toFloat()
        } else {
            // Approaching or idle — steady brightness tracks drag progress, minimal pulse
            outerAlpha = 0.1f + 0.5f * intensity + 0.05f * kotlin.math.sin(time / 800.0).toFloat()
            innerAlpha = 0.08f + 0.4f * intensity + 0.04f * kotlin.math.sin(time / 500.0 + 1.0).toFloat()
        }

        // Outer ring
        val outerPaint = Paint().apply {
            color = shipColor
            alpha = (outerAlpha * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, shipY, 55f, outerPaint)

        // Inner ring
        val innerPaint = Paint().apply {
            color = shipColor
            alpha = (innerAlpha * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, shipY, 42f, innerPaint)
    }

    private fun drawLaunchRail(canvas: Canvas, state: HangarState, shipColor: Int) {
        // Called only from drawShipyardPage's room-local translate, so centre on the room.
        val centerX = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth) / 2
        val targetZoneY = shipCenterY
        val restingY = state.shipRestingY

        // Brightness tied to drag progress — faint at rest, bright approaching halo, dim in halo
        val dragFadeStart = restingY
        val dragFadeEnd = (restingY + walkwayY) / 2f
        val dragFade = if (dragFadeStart != dragFadeEnd)
            ((state.shipDragY - dragFadeEnd) / (dragFadeStart - dragFadeEnd)).coerceIn(0f, 1f)
        else 1f
        val dragProgress = 1f - dragFade  // 0 at rest, 1 when text fully faded
        // Dim when ship enters halo zone
        val distToHalo = kotlin.math.abs(state.shipDragY - targetZoneY)
        val haloDim = if (distToHalo < 60f) (distToHalo / 60f).coerceIn(0f, 1f) else 1f
        val baseAlpha = (200f * dragProgress * haloDim).toInt().coerceIn(0, 220)

        val chevronPaint = Paint().apply {
            color = shipColor
            alpha = baseAlpha
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val chevronCount = 6
        val totalDistance = restingY - targetZoneY - 60f
        val spacing = totalDistance / (chevronCount + 1)
        val chevronWidth = 12f

        for (i in 1..chevronCount) {
            val y = restingY - 30f - i * spacing
            canvas.drawLine(centerX - chevronWidth, y + 6f, centerX, y, chevronPaint)
            canvas.drawLine(centerX, y, centerX + chevronWidth, y + 6f, chevronPaint)
        }
    }

    private fun drawShips(canvas: Canvas, state: HangarState) {
        // Called only from drawShipyardPage's room-local translate, so centre on the room —
        // and cull against the room width below, not the screen width.
        val rw = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)
        val centerX = rw / 2

        val selectedPilot = PilotDefinitions.getPilotByIndex(state.selectedPilotIndex)

        // Build visible ship list — dead ships hidden in corruption
        val isCorrupted = StoryStateManager.isCorrupted(persistence)
        val visibleShipIndices = (0 until ShipDefinitions.getShipCount()).filter { i ->
            val s = ShipDefinitions.getShipByIndex(i) ?: return@filter false
            if (isCorrupted && StoryStateManager.isShipDead(persistence, s.id)) return@filter false
            // Intro cinematic: hide the carousel peek — only the starting ship (Scout) shows.
            if (state.introCinematic && i != state.selectedShipIndex) return@filter false
            true
        }

        // Find the visual position of the selected ship in the filtered list
        val selectedVisualPos = visibleShipIndices.indexOf(state.selectedShipIndex)
            .let { if (it < 0) 0 else it }

        // Global drag fade — text starts fading when selected ship starts moving,
        // fully gone halfway between resting position and walkway
        val dragFadeStart = state.shipRestingY
        val dragFadeEnd = (state.shipRestingY + walkwayY) / 2f
        val dragFade = if (dragFadeStart != dragFadeEnd)
            ((state.shipDragY - dragFadeEnd) / (dragFadeStart - dragFadeEnd)).coerceIn(0f, 1f)
        else 1f

        for ((visualIndex, i) in visibleShipIndices.withIndex()) {
            val ship = ShipDefinitions.getShipByIndex(i) ?: continue

            val offsetFromSelected = visualIndex - selectedVisualPos
            val shipX = centerX + offsetFromSelected * shipSpacing + state.shipScrollOffset

            // Only draw if on screen (room-local space, so cull against the room, not the screen)
            if (shipX < -100f || shipX > rw + 100f) continue

            val isUnlocked = state.isShipUnlocked(i)
            val isSelected = (i == state.selectedShipIndex)

            // Selected ship follows drag Y; peek ships stay at resting Y
            val shipY = if (isSelected) state.shipDragY else state.shipRestingY

            // Dim non-selected ships (peek effect)
            val peekAlpha = if (isSelected) 1f else 0.5f * dragFade

            // Non-selected ships fade to grey as text fades; player ship keeps its color
            val greyColor = 0xFF555555.toInt()
            val rawShipColor = if (StoryStateManager.isCorrupted(persistence)) StoryStateManager.corruptShipColor(ship.color) else ship.color
            val baseShipColor = if (isUnlocked) rawShipColor else greyColor
            val displayColor = if (isSelected) baseShipColor
                else lerpColor(baseShipColor, greyColor, 1f - dragFade)

            drawShipAtPosition(canvas, shipX, shipY, ship, selectedPilot, isUnlocked, isSelected, peekAlpha, displayColor)

            // Ship name and weapon — all text fades together as selected ship is dragged
            if (dragFade > 0f) {
                val combinedAlpha = if (isSelected) dragFade else peekAlpha

                textPaint.textSize = if (isSelected) 24f else 16f
                textPaint.color = if (isSelected) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
                textPaint.alpha = (combinedAlpha * 255).toInt()
                val displayName = ShipDefinitions.getShipName(i)
                canvas.drawText(displayName, shipX, shipY + 85f, textPaint)

                // Show weapon name only for unlocked ships or the next unlockable one
                val showWeapon = isUnlocked || (state.canUnlockShip(i) && !isUnlocked)
                val weaponText = if (showWeapon) {
                    WeaponDefinitions.getWeaponDisplayName(ship.startingWeaponId)
                } else "???"
                textPaint.textSize = if (isSelected) 22f else 14f
                textPaint.color = if (isSelected) 0xFF88CCFF.toInt() else 0xFF666688.toInt()
                textPaint.alpha = (combinedAlpha * 255).toInt()
                canvas.drawText(weaponText, shipX, shipY + 107f, textPaint)
            }

            // Swipe-up hint chevrons below ship (only at resting position, unlocked)
            if (isSelected && isUnlocked && !state.isDraggingShip &&
                kotlin.math.abs(state.shipDragY - state.shipRestingY) < 2f) {
                val time = System.currentTimeMillis()
                val bob = (sin(time / 400.0) * 3f).toFloat()
                val hintAlpha = (0.3f + 0.2f * sin(time / 600.0)).toFloat()
                val hintPaint = Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    alpha = (hintAlpha * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val hintY = state.shipRestingY + 155f + bob
                val chevSize = 20f
                // Two upward chevrons
                canvas.drawLine(shipX - chevSize, hintY + 8f, shipX, hintY - 4f, hintPaint)
                canvas.drawLine(shipX, hintY - 4f, shipX + chevSize, hintY + 8f, hintPaint)
                canvas.drawLine(shipX - chevSize, hintY + 22f, shipX, hintY + 10f, hintPaint)
                canvas.drawLine(shipX, hintY + 10f, shipX + chevSize, hintY + 22f, hintPaint)
            }

            // Cost or "LOCKED" — also fades with drag
            if (!isUnlocked && dragFade > 0f) {
                if (state.canUnlockShip(i)) {
                    costPaint.color = 0xFFFFD700.toInt()
                    costPaint.alpha = (peekAlpha * 255).toInt()
                    canvas.drawText(GameConfig.formatYen(ship.cost), shipX, shipY - 50f, costPaint)
                } else {
                    costPaint.color = 0xFF666666.toInt()
                    costPaint.alpha = (peekAlpha * 255).toInt()
                    canvas.drawText("LOCKED", shipX, shipY - 50f, costPaint)
                }
            }
        }

        // Reset alpha
        textPaint.alpha = 255
        costPaint.alpha = 255
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int {
        val r = ((from shr 16 and 0xFF) + ((to shr 16 and 0xFF) - (from shr 16 and 0xFF)) * t).toInt()
        val g = ((from shr 8 and 0xFF) + ((to shr 8 and 0xFF) - (from shr 8 and 0xFF)) * t).toInt()
        val b = ((from and 0xFF) + ((to and 0xFF) - (from and 0xFF)) * t).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun drawShipAtPosition(
        canvas: Canvas,
        x: Float,
        y: Float,
        ship: com.astroloop.game.data.ShipDef,
        @Suppress("UNUSED_PARAMETER") pilot: com.astroloop.game.data.PilotDef?,
        unlocked: Boolean,
        @Suppress("UNUSED_PARAMETER") selected: Boolean,
        peekAlpha: Float = 1f,
        displayColor: Int = 0
    ) {
        val baseAlpha = if (unlocked) 1f else 0.7f
        val color = if (displayColor != 0) displayColor
            else if (unlocked) ship.color else 0xFF555555.toInt()
        ShipRenderer.drawShip(
            canvas = canvas,
            shapeRenderer = shapeRenderer,
            x = x,
            y = y,
            rotation = (-Math.PI / 2).toFloat(),
            size = GameConfig.SHIP_BASE_SIZE,
            shipColor = color,
            pilotColor = color,
            startingWeaponId = ship.startingWeaponId,
            alpha = baseAlpha * peekAlpha
        )
    }

    // =======================================================================
    // Room frame (ceiling, walls, archways)
    // =======================================================================

    private enum class RoomEdge { SOLID, ARCHWAY }

    private fun drawRoomFrame(canvas: Canvas, hasCeiling: Boolean, leftEdge: RoomEdge, rightEdge: RoomEdge) {
        // Called from inside each page's translated space, so this draws THAT room's ceiling
        // and walls — it must span the room, not the screen, or neighbouring rooms overlap.
        val rw = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)

        val wallPaint = Paint().apply {
            color = 0xFF2A2A30.toInt()
            style = Paint.Style.FILL
        }
        val highlightPaint = Paint().apply {
            color = 0xFF3A3A40.toInt()
            style = Paint.Style.FILL
        }

        // Ceiling line
        if (hasCeiling) {
            canvas.drawRect(0f, ceilingY, rw, ceilingY + 3f, wallPaint)
            canvas.drawRect(0f, ceilingY, rw, ceilingY + 1f, highlightPaint)
        }

        val wallWidth = 5f
        val archOpening = 30f  // Height of archway opening from walkway upward

        // Left edge
        when (leftEdge) {
            RoomEdge.SOLID -> {
                canvas.drawRect(0f, ceilingY, wallWidth, walkwayY, wallPaint)
            }
            RoomEdge.ARCHWAY -> {
                if (hasCeiling) {
                    // Wall from ceiling down to archway opening
                    canvas.drawRect(0f, ceilingY, wallWidth, walkwayY - archOpening, wallPaint)
                }
                // Open-air rooms: no wall, archway is fully open
            }
        }

        // Right edge
        when (rightEdge) {
            RoomEdge.SOLID -> {
                canvas.drawRect(rw - wallWidth, ceilingY, rw, walkwayY, wallPaint)
            }
            RoomEdge.ARCHWAY -> {
                if (hasCeiling) {
                    canvas.drawRect(rw - wallWidth, ceilingY, rw, walkwayY - archOpening, wallPaint)
                }
            }
        }
    }

    // =======================================================================
    // Walkway and pilot walker
    // =======================================================================

    private fun drawWalkway(canvas: Canvas, state: HangarState) {
        // Global screen-space layer drawn over the page content. The hangar building is only
        // three rooms wide, so on a wide screen a full-width walkway would extend past the last
        // room and hang in open space. Clip the walkway (and its edge highlight, which must
        // match or it would float past the walkway itself) to the building.
        val stride = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)
        // Same viewport transform drawPageContent uses, so the walkway (and the runway lights
        // below, which also consume it) lines up with the rooms.
        val viewportX = viewportX(state)
        val (buildingLeft, buildingRight) = buildingExtent(state)

        if (buildingRight > buildingLeft) {
            val walkwayPaint = Paint().apply {
                color = 0xFF2A2A30.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(buildingLeft, walkwayY, buildingRight, walkwayY + 4f, walkwayPaint)

            // Subtle edge highlight
            val highlightPaint = Paint().apply {
                color = 0xFF3A3A40.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(buildingLeft, walkwayY, buildingRight, walkwayY + 1f, highlightPaint)
        }

        // Runway lights along walkway (shipyard page only)
        if (state.currentPage == 1 || state.phase == HangarPhase.LAUNCHING) {
            val lightPaint = Paint().apply { style = Paint.Style.FILL }
            val lightCount = 10
            // Lights belong to the launchpad room (page index 1), not the full screen — space
            // and position them across that room using the same stride/viewportX transform
            // drawPageContent uses to place the shipyard page itself.
            val launchpadLeft = stride - viewportX
            val spacing = stride / (lightCount + 1)
            val time = System.currentTimeMillis()

            // Check if ship is in the halo zone
            val shipInHalo = state.isDraggingShip &&
                    kotlin.math.abs(state.shipDragY - shipCenterY) < 60f
            val launchActive = state.phase == HangarPhase.LAUNCHING

            for (i in 1..lightCount) {
                val lx = launchpadLeft + spacing * i
                val ly = walkwayY + 2f

                if (shipInHalo || launchActive) {
                    // Blinking red
                    val pulse = (0.5f + 0.5f * sin(time / 120.0 + i * 0.5)).toFloat()
                    lightPaint.color = 0xFFFF2200.toInt()
                    lightPaint.alpha = (pulse * 200).toInt().coerceIn(0, 255)
                } else {
                    // Steady white/yellow
                    lightPaint.color = 0xFFCCAA44.toInt()
                    lightPaint.alpha = 80
                }
                canvas.drawCircle(lx, ly, 2f, lightPaint)
            }
        }
    }

    private fun drawCharacter(canvas: Canvas, x: Float, y: Float, color: Int, walking: Boolean, armRaiseTimer: Float = 0f, bandanaColor: Int? = null) {
        val dotPaint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val limbPaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Body dot
        canvas.drawCircle(x, y - 6f, 5f, dotPaint)

        // Bandana band across the forehead (top of the 5px body dot) — same 2px weight
        // as the limbs, chord-fit to the head so it reads as a band, not a floating dash.
        if (bandanaColor != null) {
            val bandPaint = Paint().apply {
                this.color = bandanaColor
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawLine(x - 4.1f, y - 8.4f, x + 4.1f, y - 8.4f, bandPaint)
        }

        val time = System.currentTimeMillis()

        if (walking) {
            val legPhase = (time / 100) % 4
            val armPhase = (time / 100 + 2) % 4

            // Legs
            when (legPhase.toInt()) {
                0 -> {
                    canvas.drawLine(x - 2f, y, x - 4f, y + 5f, limbPaint)
                    canvas.drawLine(x + 2f, y, x + 4f, y + 5f, limbPaint)
                }
                1 -> {
                    canvas.drawLine(x - 2f, y, x - 5f, y + 4f, limbPaint)
                    canvas.drawLine(x + 2f, y, x + 2f, y + 5f, limbPaint)
                }
                2 -> {
                    canvas.drawLine(x - 2f, y, x - 2f, y + 5f, limbPaint)
                    canvas.drawLine(x + 2f, y, x + 5f, y + 4f, limbPaint)
                }
                3 -> {
                    canvas.drawLine(x - 2f, y, x - 4f, y + 5f, limbPaint)
                    canvas.drawLine(x + 2f, y, x + 4f, y + 5f, limbPaint)
                }
            }

            // Arms (opposite phase to legs)
            when (armPhase.toInt()) {
                0 -> {
                    canvas.drawLine(x - 4f, y - 5f, x - 7f, y - 1f, limbPaint)
                    canvas.drawLine(x + 4f, y - 5f, x + 7f, y - 1f, limbPaint)
                }
                1 -> {
                    canvas.drawLine(x - 4f, y - 5f, x - 8f, y - 3f, limbPaint)
                    canvas.drawLine(x + 4f, y - 5f, x + 5f, y - 1f, limbPaint)
                }
                2 -> {
                    canvas.drawLine(x - 4f, y - 5f, x - 5f, y - 1f, limbPaint)
                    canvas.drawLine(x + 4f, y - 5f, x + 8f, y - 3f, limbPaint)
                }
                3 -> {
                    canvas.drawLine(x - 4f, y - 5f, x - 7f, y - 1f, limbPaint)
                    canvas.drawLine(x + 4f, y - 5f, x + 7f, y - 1f, limbPaint)
                }
            }
        } else {
            // Standing still
            canvas.drawLine(x - 2f, y, x - 3f, y + 5f, limbPaint)  // left leg
            canvas.drawLine(x + 2f, y, x + 3f, y + 5f, limbPaint)  // right leg
            if (armRaiseTimer > 0f) {
                // One arm raised (grabbing beer)
                canvas.drawLine(x - 4f, y - 5f, x - 5f, y - 1f, limbPaint)  // left arm normal
                canvas.drawLine(x + 4f, y - 5f, x + 7f, y - 10f, limbPaint) // right arm raised
            } else {
                canvas.drawLine(x - 4f, y - 5f, x - 5f, y - 1f, limbPaint)  // left arm
                canvas.drawLine(x + 4f, y - 5f, x + 5f, y - 1f, limbPaint)  // right arm
            }
        }
    }

    private fun drawPilotWalker(canvas: Canvas, state: HangarState) {
        val pilot = state.getSelectedPilot() ?: return
        // Pilot has a world-space position; subtract viewport to get screen position.
        // Same viewport transform drawPageContent uses, so the walker lines up with the room.
        val walkerX = state.pilotX - viewportX(state)
        val walkerY = walkwayY - 8f
        // Only draw if on screen
        if (walkerX > -20f && walkerX < screenWidth + 20f) {
            val color = if (StoryStateManager.isCorrupted(persistence)) StoryStateManager.corruptColor(pilot.color) else pilot.color
            val bandana = if (persistence.hasBandana(pilot.id)) BandanaDefinitions.accentColor(pilot.id) else null
            drawCharacter(canvas, walkerX, walkerY, color, state.pilotWalking, 0f, bandana)
        }
    }

    private fun drawNPCWalkers(canvas: Canvas, state: HangarState) {
        // Called inside BarPageRenderer's room-local translated canvas, so this must map the
        // walker's normalized x into room width, not screen width, or roaming crew stray past
        // the counter on wide screens (the same 0.1/0.8 band stoolNormalizedX maps into).
        val rw = HangarMetrics.effectiveRoomWidth(roomWidth, screenWidth)
        val margin = rw * 0.1f
        val walkableWidth = rw - 2 * margin
        val corrupted = StoryStateManager.isCorrupted(persistence)

        for (npc in state.npcWalkers) {
            if (npc.seated) continue   // drawn at the counter by BarPageRenderer
            val npcX = margin + npc.x * walkableWidth
            val npcY = walkwayY - 8f
            val color = if (corrupted) StoryStateManager.corruptColor(npc.color) else npc.color
            val npcPilot = PilotDefinitions.getPilotByIndex(npc.pilotIndex)
            val bandana = if (npcPilot != null && persistence.hasBandana(npcPilot.id))
                BandanaDefinitions.accentColor(npcPilot.id) else null
            drawCharacter(canvas, npcX, npcY, color, npc.walking, npc.armRaiseTimer, bandana)
        }
    }

    /** Ship drag-fade: 1.0 at the resting position, fading to 0.0 as the ship is dragged up to launch. */
    private fun shipDragFade(state: HangarState): Float {
        if (state.shipRestingY == 0f) return 1f
        val fadeStart = state.shipRestingY
        val fadeEnd = (state.shipRestingY + walkwayY) / 2f
        return if (fadeStart != fadeEnd)
            ((state.shipDragY - fadeEnd) / (fadeStart - fadeEnd)).coerceIn(0f, 1f)
        else 1f
    }

    private fun drawIntroTitle(canvas: Canvas, state: HangarState) {
        // Fade in slowly (~3s) after arriving at the launchpad...
        val fadeIn = (state.introTitleTimer / 3.0f).coerceIn(0f, 1f)
        // ...then fade out with the ship drag — but half as fast as the yen counter, so the
        // title lingers. The yen-counter fade completes halfway to the walkway; doubling the
        // drag distance (fadeEnd = walkwayY) stretches the title fade over twice the range.
        val dragFade = if (state.shipRestingY == 0f || state.shipRestingY == walkwayY) 1f
            else ((state.shipDragY - walkwayY) / (state.shipRestingY - walkwayY)).coerceIn(0f, 1f)

        val alpha = (fadeIn * dragFade * 255f).toInt().coerceIn(0, 255)
        if (alpha <= 0) return

        introTitlePaint.textSize = (screenWidth * 0.11f).coerceIn(48f, 96f)
        introTitlePaint.alpha = alpha
        canvas.drawText("ASTRO LOOP", screenWidth / 2f, screenHeight * 0.30f, introTitlePaint)
    }

    // =======================================================================
    // Page indicator dots
    // =======================================================================

    private fun drawPageIndicator(canvas: Canvas, state: HangarState) {
        val labels = listOf("CREW", "LAUNCH", "SHOP")
        val centerX = screenWidth / 2
        val labelY = screenHeight * 0.95f
        val spacing = layout.content.width * 0.25f

        for (i in labels.indices) {
            val dx = (i - 1) * spacing
            textPaint.textSize = 20f
            textPaint.color = if (i == state.currentPage) 0xFFFFFFFF.toInt() else 0xFF444444.toInt()
            canvas.drawText("[${labels[i]}]", centerX + dx, labelY, textPaint)
        }
    }

    // =======================================================================
    // Yen counter (shared)
    // =======================================================================

    private fun drawYenCounter(canvas: Canvas, state: HangarState, alpha: Float = 1f) {
        textPaint.textSize = 28f
        textPaint.textAlign = Paint.Align.RIGHT
        val baseColor = 0xFFD700  // Gold RGB without alpha
        textPaint.color = ((alpha * 255).toInt().coerceIn(0, 255) shl 24) or baseColor
        // Anchor to the inset-safe area (edge-to-edge devices push the display cutout
        // into the top-right corner where the yen lives — raw screenWidth/50f put it
        // behind the notch). safe == full on non-cutout devices, so this is a no-op there.
        canvas.drawText(GameConfig.formatYen(state.displayedYen), layout.safe.right - 20f, layout.safe.top + 50f, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = 0xFFFFFFFF.toInt()
    }

    // =======================================================================
    // Ship tap rect
    // =======================================================================

    fun getShipTapRect(): RectF {
        val centerX = screenWidth / 2
        val centerY = shipCenterY
        val size = 60f
        return RectF(centerX - size, centerY - size, centerX + size, centerY + size)
    }

    // =======================================================================
    // Pilot grid index
    // =======================================================================

    /**
     * Index of the pilot card under a tap, or null.
     *
     * [roomX] is ROOM-local (see HangarMetrics.toRoomX) because the grid is drawn room-local by
     * BarPageRenderer; [y] is screen space, which room tiling never touches. Below the gate the
     * two spaces coincide and the caller passes the raw touch X, exactly as before.
     */
    fun getPilotGridIndex(roomX: Float, y: Float): Int? {
        val gridPadding = 12f
        // Must match BarPageRenderer.drawNormalBar's grid bounds exactly, in the same space.
        val gridLeft = HangarMetrics.contentXInRoom(layout.content.left, roomWidth, screenWidth) + gridPadding
        val gridRight = HangarMetrics.contentXInRoom(layout.content.right, roomWidth, screenWidth) - gridPadding
        val gridTop = layout.content.top + 70f
        val gridBottom = layout.content.top + layout.content.height * 0.52f
        val cardGap = 8f
        val cols = 4
        val rows = 3
        val cardWidth = (gridRight - gridLeft - cardGap * (cols - 1)) / cols
        val cardHeight = (gridBottom - gridTop - cardGap * (rows - 1)) / rows
        if (roomX < gridLeft || roomX > gridRight || y < gridTop || y > gridBottom) return null
        val col = ((roomX - gridLeft) / (cardWidth + cardGap)).toInt()
        val row = ((y - gridTop) / (cardHeight + cardGap)).toInt()
        if (col >= cols || row >= rows) return null
        // Verify tap is in card body, not in gap
        val withinCardX = (roomX - gridLeft) - col * (cardWidth + cardGap)
        val withinCardY = (y - gridTop) - row * (cardHeight + cardGap)
        if (withinCardX > cardWidth || withinCardY > cardHeight) return null
        val index = row * cols + col
        if (index >= PilotDefinitions.getPilotCount()) return null
        return index
    }

    // =======================================================================
    // Launch sequence
    // =======================================================================

    private fun drawLaunchSequence(canvas: Canvas, state: HangarState) {
        val progress = state.launchProgress
        val centerX = screenWidth / 2
        val shipY = shipCenterY

        val selectedShip = ShipDefinitions.getShipByIndex(state.selectedShipIndex)
        val selectedPilot = PilotDefinitions.getPilotByIndex(state.selectedPilotIndex)
        val isCorruptedLaunch = StoryStateManager.isCorrupted(persistence)
        val launchColor = if (isCorruptedLaunch) Boss.CORRUPTION_COLOR else selectedShip?.color ?: 0xFF00AAFF.toInt()
        val launchAccent = if (isCorruptedLaunch) Boss.CORRUPTION_COLOR else selectedShip?.color ?: 0xFF00AAFF.toInt()

        when {
            // Phase 1: Pilot boards (0.0 - 0.20, ~0.8s)
            progress < 0.20f -> {
                val phase = progress / 0.20f

                // Energy field active
                drawEnergyField(canvas, launchColor, centerX)

                // Ship without pilot color
                if (selectedShip != null) {
                    ShipRenderer.drawShip(
                        canvas = canvas, shapeRenderer = shapeRenderer,
                        x = centerX, y = shipY,
                        rotation = (-Math.PI / 2).toFloat(),
                        size = GameConfig.SHIP_BASE_SIZE,
                        shipColor = launchColor,
                        pilotColor = launchColor,  // Hidden
                        startingWeaponId = selectedShip.startingWeaponId,
                        alpha = 1f
                    )
                }

                // Pilot jumping from walkway to cockpit
                if (selectedPilot != null) {
                    val startY = walkwayY - 8f
                    val endY = shipY
                    val limbPaint = Paint().apply {
                        color = selectedPilot.color
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    val pilotPaint = Paint().apply {
                        color = selectedPilot.color
                        style = Paint.Style.FILL
                    }

                    val jumpY = if (phase < 0.5f) {
                        val t = phase / 0.5f
                        startY - t * (startY - endY + 60f)
                    } else {
                        val t = (phase - 0.5f) / 0.5f
                        (endY - 60f) + t * 60f
                    }

                    if (phase < 0.80f) {
                        // Fade out as pilot approaches cockpit
                        val pilotAlpha = if (phase > 0.60f) ((0.80f - phase) / 0.20f) else 1f
                        pilotPaint.alpha = (pilotAlpha * 255).toInt()
                        limbPaint.alpha = (pilotAlpha * 255).toInt()
                        canvas.drawCircle(centerX, jumpY - 6f, 5f, pilotPaint)
                        // Arms up!
                        canvas.drawLine(centerX - 4f, jumpY - 8f, centerX - 7f, jumpY - 16f, limbPaint)
                        canvas.drawLine(centerX + 4f, jumpY - 8f, centerX + 7f, jumpY - 16f, limbPaint)
                        // Legs tucked
                        canvas.drawLine(centerX - 2f, jumpY, centerX - 3f, jumpY + 3f, limbPaint)
                        canvas.drawLine(centerX + 2f, jumpY, centerX + 3f, jumpY + 3f, limbPaint)
                    }
                }
            }

            // Phase 2: Engine charge (0.20 - 0.375, ~0.7s)
            progress < 0.375f -> {
                val phase = (progress - 0.20f) / 0.175f

                // Energy field intensifying then fading
                val fieldIntensity = 1f + phase * 2f
                val fieldAlpha = 1f - phase
                drawEnergyField(canvas, launchColor, centerX, fieldIntensity * fieldAlpha)

                // Ship now has pilot color
                if (selectedShip != null) {
                    ShipRenderer.drawShip(
                        canvas = canvas, shapeRenderer = shapeRenderer,
                        x = centerX, y = shipY,
                        rotation = (-Math.PI / 2).toFloat(),
                        size = GameConfig.SHIP_BASE_SIZE,
                        shipColor = launchColor,
                        pilotColor = selectedPilot?.color ?: 0xFFFFFFFF.toInt(),
                        startingWeaponId = selectedShip.startingWeaponId,
                        alpha = 1f
                    )
                }

                // Engine glow behind ship
                val glowPaint = Paint().apply {
                    color = if (isCorruptedLaunch) 0xFFCC3300.toInt() else 0xFFFFAA00.toInt()
                    alpha = (phase * 200).toInt()
                }
                canvas.drawCircle(centerX, shipY + 30f, 6f + phase * 15f, glowPaint)

                // Neon accent pulses
                val accentPaint = Paint().apply {
                    color = launchAccent
                    alpha = ((0.5f + 0.5f * sin(System.currentTimeMillis() / 80.0)) * 255).toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(centerX - 35f, shipY, 3f, accentPaint)
                canvas.drawCircle(centerX + 35f, shipY, 3f, accentPaint)
            }

            // Phase 3: Liftoff (0.375 - 0.55, ~0.7s) — world moves down, ship stays
            progress < 0.55f -> {
                val phase = (progress - 0.375f) / 0.175f
                val worldDropY = phase * (screenHeight * 0.8f)

                canvas.save()
                val shakeIntensity = 6f + phase * 10f
                canvas.translate(
                    (Random.nextFloat() - 0.5f) * shakeIntensity,
                    (Random.nextFloat() - 0.5f) * shakeIntensity
                )

                // Massive thruster flare below stationary ship
                val thrusterSize = 20f + phase * 120f
                val flamePaint = Paint().apply {
                    color = if (isCorruptedLaunch) 0xFFCC3300.toInt() else 0xFFFF6600.toInt()
                    alpha = 220
                }
                val flameY = shipY + 30f
                canvas.drawOval(
                    RectF(centerX - thrusterSize * 0.4f, flameY,
                        centerX + thrusterSize * 0.4f, flameY + thrusterSize), flamePaint)

                val corePaint = Paint().apply {
                    color = if (isCorruptedLaunch) 0xFFFF8888.toInt() else 0xFFFFFF88.toInt()
                    alpha = 240
                }
                canvas.drawOval(
                    RectF(centerX - thrusterSize * 0.15f, flameY,
                        centerX + thrusterSize * 0.15f, flameY + thrusterSize * 0.6f), corePaint)

                // Ship stays at center
                if (selectedShip != null) {
                    ShipRenderer.drawShip(
                        canvas = canvas, shapeRenderer = shapeRenderer,
                        x = centerX, y = shipY,
                        rotation = (-Math.PI / 2).toFloat(),
                        size = GameConfig.SHIP_BASE_SIZE,
                        shipColor = launchColor,
                        pilotColor = selectedPilot?.color ?: 0xFFFFFFFF.toInt(),
                        startingWeaponId = selectedShip.startingWeaponId,
                        alpha = 1f
                    )
                }

                // Walkway sliding down with the world — same extent as the static walkway it
                // takes over from (drawWalkway), or it would pop wider the instant it drops.
                // Below the gate that extent is 0..screenWidth, exactly as before.
                val walkwayDropPaint = Paint().apply {
                    color = 0xFF2A2A30.toInt()
                    style = Paint.Style.FILL
                }
                val (dropLeft, dropRight) = buildingExtent(state)
                canvas.drawRect(dropLeft, walkwayY + worldDropY, dropRight, walkwayY + worldDropY + 4f, walkwayDropPaint)

                // Particles drop with world
                val particlePaint = Paint().apply {
                    color = if (isCorruptedLaunch) 0xFFDD4422.toInt() else 0xFFFFAA44.toInt()
                    alpha = ((1f - phase) * 200).toInt()
                }
                for (i in 0..20) {
                    val angle = Random.nextFloat() * 2f * PI.toFloat()
                    val dist = Random.nextFloat() * 80f * phase
                    val px = centerX + cos(angle) * dist
                    val py = shipY + worldDropY + sin(angle).coerceAtLeast(0f) * dist
                    canvas.drawCircle(px, py, 2f + Random.nextFloat() * 4f, particlePaint)
                }

                canvas.restore()
            }

            // Phase 4: Hyperspace (0.55 - 1.0, ~1.8s)
            else -> {
                val hyperPhase = (progress - 0.55f) / 0.45f  // 0.0 to 1.0 within hyperspace
                val arrivalStart = 0.67f  // ~1.2s travel, ~0.6s arrival
                val isArrival = hyperPhase >= arrivalStart
                val arrivalPhase = if (isArrival) (hyperPhase - arrivalStart) / (1f - arrivalStart) else 0f

                // Background: dark blue-black during travel, lerps to game background during arrival
                // Game background is 0xFF000011 = rgb(0, 0, 17)
                val bgR = if (isArrival) (10 * (1f - arrivalPhase)).toInt() else 10
                val bgG = if (isArrival) (10 * (1f - arrivalPhase)).toInt() else 10
                val bgB = if (isArrival) (26 + (17 - 26) * arrivalPhase).toInt() else 26
                hyperBgPaint.color = android.graphics.Color.rgb(bgR, bgG, bgB)
                canvas.drawRect(0f, 0f, screenWidth, screenHeight, hyperBgPaint)

                // Star streaks — white/blue only
                val numStreaks = 60
                hyperStreakRandom.setSeed(42)

                // Arrival deceleration — length shrinks, alpha fades, offset decelerates
                val lengthMult = if (isArrival) 1f - arrivalPhase * 0.8f else 1f
                val streakAlphaBase = if (isArrival) 1f - arrivalPhase else 1f

                // Accumulated offset: linear during travel, decelerating during arrival
                // During travel (0..arrivalStart): offset grows linearly
                // During arrival: offset continues growing but rate drops (integral of speedMult)
                val travelOffset = hyperPhase.coerceAtMost(arrivalStart) * screenHeight * 0.6f
                val arrivalOffset = if (isArrival) {
                    // Integral of (1 - t*0.8) from 0 to arrivalPhase = arrivalPhase - 0.4*arrivalPhase^2
                    val integrated = arrivalPhase - 0.4f * arrivalPhase * arrivalPhase
                    integrated * (1f - arrivalStart) * screenHeight * 0.6f
                } else 0f
                val yOffset = travelOffset + arrivalOffset

                for (i in 0 until numStreaks) {
                    val xPos = hyperStreakRandom.nextFloat() * screenWidth
                    val baseY = hyperStreakRandom.nextFloat() * screenHeight
                    val maxLength = 40f + hyperPhase.coerceAtMost(0.5f) * 2f * 250f
                    val streakLength = maxLength * lengthMult
                    val startY = (baseY + yOffset) % (screenHeight + 300f)
                    val endY = startY + streakLength

                    // Skip streaks that cross the ship area
                    val shipZoneLeft = centerX - 40f
                    val shipZoneRight = centerX + 40f
                    if (xPos > shipZoneLeft && xPos < shipZoneRight) continue

                    // Fade in during first 10% of travel
                    val fadeIn = if (hyperPhase < 0.10f) hyperPhase / 0.10f else 1f
                    val alpha = (fadeIn * streakAlphaBase * 255).toInt().coerceIn(0, 255)
                    if (alpha == 0) continue

                    hyperStreakPaint.alpha = alpha
                    // ~60% white, ~40% light blue
                    hyperStreakPaint.color = if (i % 5 < 3) 0xFFFFFFFF.toInt() else 0xFF88BBFF.toInt()
                    hyperStreakPaint.strokeWidth = if (i % 4 == 0) 3f else if (i % 2 == 0) 2f else 1f

                    canvas.drawLine(xPos, startY, xPos, endY, hyperStreakPaint)
                }

                // Ship stays visible at center — drawn last, on top of everything
                if (selectedShip != null) {
                    ShipRenderer.drawShip(
                        canvas = canvas, shapeRenderer = shapeRenderer,
                        x = centerX, y = shipCenterY,
                        rotation = (-Math.PI / 2).toFloat(),
                        size = GameConfig.SHIP_BASE_SIZE,
                        shipColor = launchColor,
                        pilotColor = selectedPilot?.color ?: 0xFFFFFFFF.toInt(),
                        startingWeaponId = selectedShip.startingWeaponId,
                        alpha = 1f
                    )
                }
            }
        }
    }

    // =======================================================================
    // Codex (preserved faithfully from original)
    // =======================================================================

    private fun drawCodex(canvas: Canvas, state: HangarState) {
        // Fullscreen overlay
        val bgPaint = Paint().apply {
            color = 0xEE111122.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        val discovered = persistence.getDiscoveredEvolutions()
        val evolutions = WeaponDefinitions.evolutions
        val baseWeapons = WeaponDefinitions.weapons

        // Build evolution → (baseWeaponId, passiveId) mapping
        data class EvolutionMapping(
            val baseWeaponId: String,
            val baseWeaponName: String,
            val passiveId: String,
            val effectivePassiveId: String,
            val passiveName: String,
            val evolutionId: String,
            val evolutionName: String,
            val isWeaponKnown: Boolean,
            val isPassiveKnown: Boolean
        )

        val mappings = evolutions.mapNotNull { evo ->
            val base = baseWeapons.find { it.evolutionWeaponId == evo.id } ?: return@mapNotNull null
            val passiveId = base.evolutionPassive ?: return@mapNotNull null
            val passiveName = PassiveDefinitions.getDisplayName(
                                passiveId,
                                PassiveDefinitions.ASTRO_PILOT_ID,
                                isAstroLoopRun = StoryStateManager.isAstroLoop(persistence)
                            )
            val effectivePassiveId = PassiveDefinitions.getEffectivePassiveId(
                passiveId,
                PassiveDefinitions.ASTRO_PILOT_ID,
                isAstroLoopRun = StoryStateManager.isAstroLoop(persistence)
            )

            val weaponShipIndex = ShipDefinitions.ships.indexOfFirst { it.startingWeaponId == base.id }
            val isWeaponKnown = weaponShipIndex >= 0 && state.isShipUnlocked(weaponShipIndex)

            val passivePilotIndex = PilotDefinitions.pilots.indexOfFirst { it.startingPassiveId == passiveId }
            val isPassiveKnown = passivePilotIndex >= 0 && state.isPilotUnlocked(passivePilotIndex)

            EvolutionMapping(base.id, base.name, passiveId, effectivePassiveId, passiveName, evo.id, evo.name, isWeaponKnown, isPassiveKnown)
        }

        // Layout: title at top, back hint at bottom, evolutions fill the rest
        val topMargin = 30f
        val bottomMargin = 20f
        val usableHeight = screenHeight - topMargin - bottomMargin
        val rowHeight = usableHeight / mappings.size.coerceAtLeast(1)

        val iconSize = (rowHeight * 0.45f).coerceIn(16f, 60f)
        val labelSize = (rowHeight * 0.30f).coerceIn(11f, 24f)
        val iconGap = iconSize * 0.30f
        val symbolTextSize = (iconSize * 0.60f).coerceIn(18f, 32f)
        val symbolYOffset = iconSize * 0.10f
        val iconPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        for ((index, mapping) in mappings.withIndex()) {
            val isDiscovered = discovered.contains(mapping.evolutionId)
            val rowCenterY = topMargin + rowHeight * index + rowHeight / 2f

            // Horizontal layout: [weapon] + [passive] = [evolution]
            // Centered on screen with even spacing
            val spacing = layout.content.width / 6f
            val weaponX = layout.content.centerX - spacing * 1.5f
            val plusX = layout.content.centerX - spacing * 0.75f
            val passiveX = layout.content.centerX
            val equalsX = layout.content.centerX + spacing * 0.75f
            val evoX = layout.content.centerX + spacing * 1.5f

            val iconY = rowCenterY - iconSize * 0.3f  // Offset up to make room for name below
            val nameY = iconY + iconSize + iconGap

            iconPaint.alpha = 255  // reset per-row to prevent bleed from previous iteration

            if (isDiscovered) {
                // All three fully bright
                iconPaint.color = 0xFFFFFFFF.toInt()
                iconPaint.alpha = 255
                IconRenderer.drawIcon(canvas, mapping.baseWeaponId, true, weaponX, iconY, iconSize, iconPaint)
                IconRenderer.drawIcon(canvas, mapping.effectivePassiveId, false, passiveX, iconY, iconSize, iconPaint)
                IconRenderer.drawIcon(canvas, mapping.evolutionId, true, evoX, iconY, iconSize, iconPaint)

                textPaint.textSize = symbolTextSize
                textPaint.color = 0xFF888888.toInt()
                canvas.drawText("+", plusX, iconY + symbolYOffset, textPaint)
                canvas.drawText("=", equalsX, iconY + symbolYOffset, textPaint)

                textPaint.textSize = labelSize
                textPaint.color = 0xFF999999.toInt()
                canvas.drawText(mapping.baseWeaponName, weaponX, nameY, textPaint)
                canvas.drawText(mapping.passiveName, passiveX, nameY, textPaint)
                textPaint.color = 0xFFFFFFFF.toInt()
                canvas.drawText(mapping.evolutionName, evoX, nameY, textPaint)
            } else {
                // Weapon column
                if (mapping.isWeaponKnown) {
                    iconPaint.color = 0xFFFFFFFF.toInt()
                    iconPaint.alpha = 255
                    IconRenderer.drawIcon(canvas, mapping.baseWeaponId, true, weaponX, iconY, iconSize, iconPaint)
                    textPaint.textSize = labelSize
                    textPaint.color = 0xFF999999.toInt()
                    canvas.drawText(mapping.baseWeaponName, weaponX, nameY, textPaint)
                } else {
                    iconPaint.color = 0xFF333344.toInt()
                    iconPaint.alpha = 80
                    IconRenderer.drawIcon(canvas, mapping.baseWeaponId, true, weaponX, iconY, iconSize, iconPaint)
                    textPaint.textSize = labelSize
                    textPaint.color = 0xFF333344.toInt()
                    canvas.drawText("???", weaponX, nameY, textPaint)
                }

                // Passive column
                if (mapping.isPassiveKnown) {
                    iconPaint.color = 0xFFFFFFFF.toInt()
                    iconPaint.alpha = 255
                    IconRenderer.drawIcon(canvas, mapping.effectivePassiveId, false, passiveX, iconY, iconSize, iconPaint)
                    textPaint.textSize = labelSize
                    textPaint.color = 0xFF999999.toInt()
                    canvas.drawText(mapping.passiveName, passiveX, nameY, textPaint)
                } else {
                    iconPaint.color = 0xFF333344.toInt()
                    iconPaint.alpha = 80
                    IconRenderer.drawIcon(canvas, mapping.effectivePassiveId, false, passiveX, iconY, iconSize, iconPaint)
                    textPaint.textSize = labelSize
                    textPaint.color = 0xFF333344.toInt()
                    canvas.drawText("???", passiveX, nameY, textPaint)
                }

                // + symbol: bright only when both sides are known
                textPaint.textSize = symbolTextSize
                textPaint.color = if (mapping.isWeaponKnown && mapping.isPassiveKnown) 0xFF888888.toInt() else 0xFF333344.toInt()
                canvas.drawText("+", plusX, iconY + symbolYOffset, textPaint)

                // = and evolution: always dimmed until discovered
                textPaint.color = 0xFF333344.toInt()
                canvas.drawText("=", equalsX, iconY + symbolYOffset, textPaint)
                iconPaint.color = 0xFF333344.toInt()
                iconPaint.alpha = 80
                IconRenderer.drawIcon(canvas, mapping.evolutionId, true, evoX, iconY, iconSize, iconPaint)
                textPaint.textSize = labelSize
                canvas.drawText("???", evoX, nameY, textPaint)
            }
        }

        textPaint.color = 0xFFFFFFFF.toInt()
    }
}
