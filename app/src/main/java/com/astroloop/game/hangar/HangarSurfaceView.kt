package com.astroloop.game.hangar

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.astroloop.game.MainActivity
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.ScreenLayout
import com.astroloop.game.core.SoundManager
import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.TelemetryManager
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.render.CrystalOrbPath
import com.astroloop.game.render.FontManager
import com.astroloop.game.render.IconCache
import kotlin.math.abs
import kotlin.math.pow

class HangarSurfaceView(
    context: Context,
    private val onLaunch: (shipId: String, pilotId: String) -> Unit
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var running = false

    val persistence = PersistenceManager(context)
    private val telemetryManager = TelemetryManager(context)
    private val state = HangarState(persistence)
    private val renderer = HangarRenderer(persistence)
    private val chatSystem = ChatSystem()

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var renderScale: Float = 1f
    private var crystalGlowSoundPlayed = false

    // System-cutout insets in physical px, forwarded by MainActivity. Divided by
    // renderScale into design units when building the ScreenLayout.
    private var insetLeftPx = 0f
    private var insetTopPx = 0f
    private var insetRightPx = 0f
    private var insetBottomPx = 0f

    // @Volatile: written on the UI thread (applyInsets/surfaceChanged), read by the render thread.
    @Volatile var layout: ScreenLayout = ScreenLayout.compute(GameConfig.DESIGN_WIDTH, GameConfig.DESIGN_HEIGHT)
        private set

    /** Called by MainActivity when display-cutout insets become known or change. */
    fun applyInsets(left: Float, top: Float, right: Float, bottom: Float) {
        // Android delivers identical insets repeatedly; ignore no-op deliveries so the
        // steady state never re-touches state the render thread reads. Real work happens
        // only on an actual cutout change (e.g. fold/unfold), which also fires surfaceChanged.
        if (left == insetLeftPx && top == insetTopPx &&
            right == insetRightPx && bottom == insetBottomPx
        ) return
        insetLeftPx = left; insetTopPx = top; insetRightPx = right; insetBottomPx = bottom
        if (width > 0 && height > 0) {
            applyScreenDimensions(width, height)
            renderer.initialize(layout)
            state.pilotScreenWidth = screenWidth
            initShipPositions()
        }
    }

    // Vibration
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private var isVibratingHalo = false
    private var isVibrationMuted: Boolean = context.getSharedPreferences("astrohunt_save", Context.MODE_PRIVATE)
        .getBoolean("vibration_muted", false)

    // Touch handling
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastTouchTime: Long = 0
    private var isDragging = false
    private var activeSwipe: SwipeTarget = SwipeTarget.NONE
    private var shipDragPossible = false
    private var spinButtonHeld = false
    private var stateInitialized = false

    private enum class SwipeTarget { NONE, PAGE, SHIP_DRAG }

    // Page scroll physics — all values are per-second, applied via deltaTime
    private val pageFrictionPerSecond = 0.05f     // 5% velocity remains after 1s (iOS paging feel)
    private val pageSnapDecay = 1e-10f            // snap settles in ~0.3s
    private val pageVelocityThreshold = 250f      // px/s — lower = responds to gentle flicks

    init {
        holder.addCallback(this)
        isFocusable = true
        FontManager.initialize(context)
    }

    private fun applyScreenDimensions(physW: Int, physH: Int) {
        renderScale = minOf(physW / GameConfig.DESIGN_WIDTH, physH / GameConfig.DESIGN_HEIGHT)
        screenWidth = physW / renderScale
        screenHeight = physH / renderScale
        layout = ScreenLayout.compute(
            width = screenWidth,
            height = screenHeight,
            insetLeft = insetLeftPx / renderScale,
            insetTop = insetTopPx / renderScale,
            insetRight = insetRightPx / renderScale,
            insetBottom = insetBottomPx / renderScale
        )
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        IconCache.preload(context)

        applyScreenDimensions(width, height)

        renderer.initialize(layout)
        state.pilotScreenWidth = screenWidth
        // Only initialize state on first surface creation — on re-attach after
        // a run, resetForReturn() has already set the correct state (e.g. bar page)
        if (!stateInitialized) {
            state.initialize()
            stateInitialized = true
        }
        SoundManager.setMuted(state.audioMuted)
        isVibrationMuted = state.vibrationMuted
        initShipPositions()

        if (persistence.isFirstLaunch()) {
            // Start on bar page
            state.currentPage = 0
            state.pilotX = state.getPilotWorldTarget(0)
            state.pilotTargetX = state.pilotX
            state.pilotWalking = false

            // Start with no yen — the counter is hidden during the intro cinematic anyway.
            persistence.setYen(0)
            state.actualYen = 0
            state.displayedYen = 0

            // Queue intro messages
            chatSystem.onFirstLaunch(state)

            persistence.setFirstLaunchComplete()
        }

        // Refresh the music set from current story stage before any ambient playback
        SoundManager.activeSet = StoryStateManager.stageMusicSet(persistence)

        // Start ambient for the initial page — but the intro cinematic plays the drone bed.
        if (state.introCinematic) {
            SoundManager.playAmbient("sfx_intro_drone")   // looping bed under the silent bar
        } else {
            SoundManager.playAmbient(getAmbientForPage(state.currentPage))
        }

        if (!running) {
            running = true
            gameThread = Thread(this)
            gameThread?.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val prevWidth = screenWidth
        applyScreenDimensions(width, height)
        renderer.initialize(layout)
        state.pilotScreenWidth = screenWidth
        initShipPositions()
        // Re-anchor pilot / TB-26 to the new layout whenever the surface dimensions change
        // (a live fold/unfold) or on the first valid layout (screenWidth was NaN — 0/0 in IEEE
        // 754 — when surfaceCreated() fired before this.width/height were known). A same-size
        // resume (prevWidth == screenWidth) is left alone so an in-progress walk isn't snapped.
        // Position fields are @Volatile and written together, so the render thread never reads a
        // torn/inconsistent pair; a fold is disruptive anyway, so we snap rather than animate.
        if (stateInitialized && (state.pilotX.isNaN() || screenWidth != prevWidth)) {
            state.pilotX = state.getPilotWorldTarget(state.currentPage)
            state.pilotTargetX = state.pilotX
            state.pilotWalking = false
            state.tb26BarX = screenWidth / 2f
            state.tb26BarTargetX = state.tb26BarX
        }
    }

    private fun initShipPositions() {
        val walkwayY = screenHeight * 0.60f
        state.shipRestingY = walkwayY + (screenHeight - walkwayY) * 0.4f
        if (!state.isDraggingShip) {
            state.shipDragY = state.shipRestingY
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopHaloRumble()
        running = false
        gameThread?.join()
    }

    override fun run() {
        var lastTime = System.nanoTime()

        while (running) {
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastTime) / 1_000_000_000f
            lastTime = currentTime

            // Mirror GameThread's resilience: a throwable from a single update/render
            // frame (e.g. on resume from background) must not kill the process.
            try {
                update(deltaTime)
                render()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun update(deltaTime: Float) {
        when (state.phase) {
            HangarPhase.BROWSING -> updateBrowsing(deltaTime)
            HangarPhase.LAUNCHING -> updateLaunching(deltaTime)
            else -> {}
        }

        // Update pilot walker
        state.updatePilotWalker(deltaTime)

        // Update NPC walkers
        state.updateNPCWalkers(deltaTime)

        // Animate yen counter
        state.updateYenDisplay(deltaTime)

        // Tick fade-from-black timer (corruption death return)
        if (state.fadeFromBlackTimer > 0f) {
            state.fadeFromBlackTimer = (state.fadeFromBlackTimer - deltaTime).coerceAtLeast(0f)
        }
        if (state.glitchTimer > 0f) {
            state.glitchTimer = (state.glitchTimer - deltaTime).coerceAtLeast(0f)
        }
    }

    private fun updateBrowsing(deltaTime: Float) {
        // --- Page scroll physics ---
        if (activeSwipe != SwipeTarget.PAGE) {
            if (abs(state.pageVelocity) > 0.1f) {
                state.pageScrollOffset += state.pageVelocity * deltaTime
                state.pageVelocity *= pageFrictionPerSecond.pow(deltaTime)
            }
            if (abs(state.pageVelocity) < 50f) {
                val diff = -state.pageScrollOffset
                if (abs(diff) > 1f) {
                    state.pageScrollOffset += diff * (1f - pageSnapDecay.pow(deltaTime))
                } else {
                    state.pageScrollOffset = 0f
                    state.pageVelocity = 0f
                }
            }
        }

        // Intro cinematic: advance the ASTRO LOOP title fade-in once on the launchpad.
        if (state.introCinematic && state.currentPage == 1) {
            state.introTitleTimer += deltaTime
        }

        // Crystal reveal: play the glow shimmer once when the GLOW phase begins.
        if (state.crystalRevealPhase == HangarState.CrystalRevealPhase.GLOW) {
            if (!crystalGlowSoundPlayed) {
                crystalGlowSoundPlayed = true
                SoundManager.playSFX("sfx_crystal_glow")
            }
        } else if (state.crystalRevealPhase == HangarState.CrystalRevealPhase.NONE) {
            crystalGlowSoundPlayed = false
        }

        // Crystal reveal: trigger orb travel when store page settles
        if (state.awaitingCrystalReveal && state.currentPage == 2
            && state.crystalRevealPhase == HangarState.CrystalRevealPhase.GLOW
            && abs(state.pageScrollOffset) < 2f && abs(state.pageVelocity) < 50f) {
            state.crystalRevealPhase = HangarState.CrystalRevealPhase.ORB_TRAVEL
            state.crystalRevealTimer = 0f
            SoundManager.playSFX("sfx_crystal_orb")
        }

        // Crystal reveal animation
        if (state.crystalRevealPhase == HangarState.CrystalRevealPhase.ORB_TRAVEL) {
            state.crystalRevealTimer += deltaTime
            if (state.crystalRevealTimer >= CrystalOrbPath.TRAVEL_DURATION) {
                state.crystalRevealPhase = HangarState.CrystalRevealPhase.FLASH
                state.crystalRevealTimer = 0f
                SoundManager.playSFX("sfx_crystal_activate")
            }
        } else if (state.crystalRevealPhase == HangarState.CrystalRevealPhase.FLASH) {
            state.crystalRevealTimer += deltaTime
            if (state.crystalRevealTimer >= CrystalOrbPath.FLASH_DURATION) {
                // Reveal complete — select Astro
                // Compute Astro's actual slot machine position (base, without walker offset)
                val margin = state.pilotScreenWidth * 0.1f
                val walkable = state.pilotScreenWidth * 0.8f
                val slotMachinePos = 2f * state.pilotScreenWidth + margin + 0.1f * walkable
                state.crystalRevealPhase = HangarState.CrystalRevealPhase.DONE
                state.awaitingCrystalReveal = false
                state.astroAtSlotMachine = false
                persistence.setAwaitingCrystalReveal(false)
                val astroIndex = PilotDefinitions.pilots.indexOfFirst { it.id == "pilot_astro" }
                if (astroIndex >= 0) state.selectedPilotIndex = astroIndex
                // Place pilot walker exactly where Astro was at the slot machine
                state.pilotX = slotMachinePos
                state.pilotTargetX = slotMachinePos
                state.pilotWalking = false
            }
        }

        // --- Lamp sway momentum (feeds from velocity, decays over time) ---
        if (abs(state.pageVelocity) > 50f) {
            state.swayMomentum = state.pageVelocity
        }
        state.swayMomentum *= 0.93f
        if (abs(state.swayMomentum) < 1f) state.swayMomentum = 0f

        // --- Ship scroll animation ---
        if (kotlin.math.abs(state.shipScrollOffset) > 1f) {
            state.shipScrollOffset += (0f - state.shipScrollOffset) * 0.12f
        } else {
            state.shipScrollOffset = 0f
        }

        // --- Ship drag snap-back animation ---
        if (!state.isDraggingShip && abs(state.shipDragY - state.shipRestingY) > 1f) {
            state.shipDragY += (state.shipRestingY - state.shipDragY) * 0.15f
        } else if (!state.isDraggingShip) {
            state.shipDragY = state.shipRestingY
        }

        // Halo vibration — light rumble while ship is held in halo zone
        val haloCenter = screenHeight / 2f
        val inHalo = state.isDraggingShip && abs(state.shipDragY - haloCenter) < 60f
        if (inHalo) startHaloRumble() else stopHaloRumble()

        // Update slot machine animation
        if (state.isSpinning) {
            val now = System.currentTimeMillis()
            if (now >= state.reelStopTimes[2]) {
                // All reels stopped — apply payout
                state.isSpinning = false
                state.spinResultTime = now
                // Jackpot sound when all three reels show rockets
                if (state.reelValues[0] == StorePageRenderer.SYM_ROCKET &&
                    state.reelValues[1] == StorePageRenderer.SYM_ROCKET &&
                    state.reelValues[2] == StorePageRenderer.SYM_ROCKET) {
                    SoundManager.playSFX("sfx_slot_jackpot")
                    spinButtonHeld = false  // Let jackpot animation play before resuming auto-spin
                }
                if (state.spinResultYen > 0) {
                    // Non-jackpot win sound
                    if (!(state.reelValues[0] == StorePageRenderer.SYM_ROCKET &&
                          state.reelValues[1] == StorePageRenderer.SYM_ROCKET &&
                          state.reelValues[2] == StorePageRenderer.SYM_ROCKET)) {
                        SoundManager.playSFX("sfx_slot_win")
                    }
                    synchronized(upgradeLock) {
                        val newYen = state.actualYen + state.spinResultYen
                        persistence.setYen(newYen)
                        state.actualYen = newYen
                    }
                }
            }
        }

        // Auto-spin when holding the spin button (disabled after jackpot)
        if (spinButtonHeld && !state.isSpinning && state.spinResultTime > 0 && state.spinResultUpgrade == null) {
            val sinceResult = System.currentTimeMillis() - state.spinResultTime
            if (sinceResult > 600L) {
                handleSlotSpin()
            }
        }

        // Astro auto-gambling at slot machine in corruption
        if (state.astroAtSlotMachine && state.currentPage == 2) {
            state.astroAutoSpinTimer += deltaTime
            if (state.astroAutoSpinTimer >= state.astroAutoSpinInterval && !state.isSpinning) {
                if (state.actualYen >= 100) {
                    handleSlotSpin()
                }
                state.astroAutoSpinTimer = 0f
                state.astroAutoSpinInterval = 5f + kotlin.random.Random.nextFloat() * 3f
            }
        }

        // Pilot card fade — selected card full opacity, others dim to 35%
        val lerpSpeed = 16f * deltaTime
        for (i in state.pilotCardFades.indices) {
            val target = if (i == state.selectedPilotIndex) 1f else 0.35f
            state.pilotCardFades[i] += (target - state.pilotCardFades[i]) * lerpSpeed
        }

        // Pilot card fade animation (tap selected pilot to reveal passive effect)
        if (state.pilotFlipTimer > 0f) {
            state.pilotFlipTimer -= deltaTime
            if (state.pilotFlipTimer <= 0f) {
                state.pilotFlipTimer = 0f
                state.pilotFlipIndex = -1
                state.pilotFlipProgress = 0f
                state.pilotFlipShowBack = false
            } else {
                val elapsed = 1.25f - state.pilotFlipTimer
                state.pilotFlipShowBack = elapsed >= 0.225f
                // pilotFlipProgress = alpha of current visible content (1=fully visible, 0=invisible)
                state.pilotFlipProgress = when {
                    elapsed < 0.225f -> 1f - elapsed / 0.225f          // fading out: 1→0
                    state.pilotFlipTimer < 0.225f -> state.pilotFlipTimer / 0.225f  // fading in: 0→1
                    else -> 1f                                          // back fully visible
                }
            }
        }

        // Update TB-26 bartender movement and beer sliding on bar page
        updateTb26Bar(deltaTime)

        // Update chat system
        chatSystem.update(deltaTime, state)
    }

    private fun updateLaunching(deltaTime: Float) {
        state.launchProgress += deltaTime / 2f  // 2 seconds total

        when {
            state.launchProgress < 0.20f -> state.launchPhase = 0   // Pilot boards (0-0.8s)
            state.launchProgress < 0.375f -> state.launchPhase = 1  // Engine charge (0.8-1.5s)
            state.launchProgress < 0.55f -> state.launchPhase = 2   // Liftoff (1.5-2.2s)
            else -> state.launchPhase = 3                           // Hyperspace (2.2-4.0s)
        }

        // Trigger game launch
        if (state.launchProgress >= 1f) {
            val ship = state.getSelectedShip()
            val pilot = state.getSelectedPilot()
            if (ship != null && pilot != null) {
                state.saveSelection()
                onLaunch(ship.id, pilot.id)
            }
        }
    }

    private fun updateTb26Bar(deltaTime: Float) {
        // Only update on bar page
        if (state.currentPage != 0) return

        // TB-26 not present in corruption state — no pacing, no beers
        if (StoryStateManager.isCorrupted(persistence)) return

        val counterLeft = 30f
        val counterRight = screenWidth - 30f

        // TB-26 pacing
        if (state.tb26BarMoving) {
            val dx = state.tb26BarTargetX - state.tb26BarX
            if (kotlin.math.abs(dx) < 3f) {
                state.tb26BarX = state.tb26BarTargetX
                state.tb26BarMoving = false
                state.tb26BarPauseTimer = 1f + kotlin.random.Random.nextFloat() * 1.5f
            } else {
                state.tb26BarX += kotlin.math.sign(dx) * 40f * deltaTime
            }
        } else {
            state.tb26BarPauseTimer -= deltaTime
            if (state.tb26BarPauseTimer <= 0f) {
                state.tb26BarTargetX = counterLeft + kotlin.random.Random.nextFloat() * (counterRight - counterLeft)
                state.tb26BarMoving = true
            }
        }

        // Beer timer
        state.beerTimer += deltaTime
        if (state.beerTimer >= state.beerInterval && !state.beerActive) {
            val walkers = state.npcWalkers
            if (walkers.isNotEmpty()) {
                val target = walkers[kotlin.random.Random.nextInt(walkers.size)]
                state.beerTargetPilotIndex = target.pilotIndex
                state.beerX = state.tb26BarX
                val margin = screenWidth * 0.1f
                val walkableWidth = screenWidth - 2 * margin
                state.beerTargetX = margin + target.x * walkableWidth
                state.beerFading = false
                state.beerFadeAlpha = 1f
                state.beerActive = true
                state.tb26BarMoving = false
                state.tb26BarPauseTimer = 0.25f
                state.beerTimer = 0f
                state.beerInterval = 5f + kotlin.random.Random.nextFloat() * 2.5f
            }
        }

        // Beer movement
        if (state.beerActive) {
            if (state.beerFading) {
                state.beerFadeAlpha -= deltaTime * 4f
                if (state.beerFadeAlpha <= 0f) {
                    state.beerActive = false
                    state.beerFading = false
                }
            } else {
                val dx = state.beerTargetX - state.beerX
                if (kotlin.math.abs(dx) < 5f) {
                    val margin = screenWidth * 0.1f
                    val walkableWidth = screenWidth - 2 * margin
                    val targetWalker = state.npcWalkers.find { it.pilotIndex == state.beerTargetPilotIndex }
                    if (targetWalker != null) {
                        val walkerScreenX = margin + targetWalker.x * walkableWidth
                        if (kotlin.math.abs(walkerScreenX - state.beerTargetX) < 20f) {
                            // Beer grabbed!
                            state.beerActive = false
                            targetWalker.armRaiseTimer = 0.5f
                        } else {
                            // Walker moved away — unclaimed
                            state.beerFading = true
                        }
                    } else {
                        // Target walker gone — unclaimed
                        state.beerFading = true
                    }
                } else {
                    state.beerX += kotlin.math.sign(dx) * 300f * deltaTime
                }
            }
        }

        // Arm raise tick
        for (npc in state.npcWalkers) {
            if (npc.armRaiseTimer > 0f) {
                npc.armRaiseTimer = (npc.armRaiseTimer - deltaTime).coerceAtLeast(0f)
            }
        }
    }

    private fun render() {
        val canvas: Canvas? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { holder.lockHardwareCanvas() } catch (e: Exception) { holder.lockCanvas() }
        } else {
            holder.lockCanvas()
        }
        canvas ?: return
        try {
            canvas.save()
            canvas.scale(renderScale, renderScale)
            renderer.render(canvas, state)
        } finally {
            canvas.restore()
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                // Surface was destroyed mid-frame
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val ex = event.x / renderScale
        val ey = event.y / renderScale
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ex
                touchStartY = ey
                lastTouchX = ex
                lastTouchY = ey
                lastTouchTime = event.eventTime
                isDragging = false
                activeSwipe = SwipeTarget.NONE
                shipDragPossible = false

                if (state.phase == HangarPhase.BROWSING) {
                    // Block interaction during crystal reveal animation
                    if (state.crystalRevealPhase == HangarState.CrystalRevealPhase.ORB_TRAVEL
                        || state.crystalRevealPhase == HangarState.CrystalRevealPhase.FLASH) {
                        return true
                    }
                    // Any touch below walkway on shipyard page can drag the ship
                    val walkwayY = screenHeight * 0.60f
                    if (state.currentPage == 1 && ey > walkwayY &&
                        state.isShipUnlocked(state.selectedShipIndex)) {
                        shipDragPossible = true
                    }
                    // Check if spin button is being held
                    if (state.currentPage == 2 && renderer.spinButtonRect.contains(ex, ey)) {
                        spinButtonHeld = true
                    }
                    state.pageVelocity = 0f
                    state.pageScrollOffset = 0f
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (state.phase == HangarPhase.BROWSING) {
                    if (!isDragging) {
                        val totalDx = abs(ex - touchStartX)
                        val totalDy = abs(ey - touchStartY)

                        if (totalDx > 15f || totalDy > 15f) {
                            isDragging = true
                            if (shipDragPossible && totalDy > totalDx) {
                                // Vertical drag on ship → ship drag
                                activeSwipe = SwipeTarget.SHIP_DRAG
                                state.isDraggingShip = true
                            } else {
                                // Horizontal movement or not on ship → page swipe
                                activeSwipe = SwipeTarget.PAGE
                            }
                        }
                    }

                    if (isDragging) {
                        val dx = ex - lastTouchX
                        val dy = ey - lastTouchY

                        when (activeSwipe) {
                            SwipeTarget.PAGE -> {
                                state.pageScrollOffset -= dx

                                // Add resistance at edges (page 0 left edge, page 2 right edge).
                                // During the intro cinematic the launchpad (page 1) is locked both
                                // ways — you may only arrive there from the bar.
                                if ((state.currentPage == 0 && state.pageScrollOffset < 0) ||
                                    (state.currentPage == 2 && state.pageScrollOffset > 0) ||
                                    (state.introCinematic && state.currentPage == 1)) {
                                    state.pageScrollOffset *= 0.3f
                                }

                                val touchDt = (event.eventTime - lastTouchTime).coerceAtLeast(1L).toFloat() / 1000f
                                state.pageVelocity = -dx / touchDt
                                lastTouchTime = event.eventTime
                            }
                            SwipeTarget.SHIP_DRAG -> {
                                state.shipDragY += dy
                                // Clamp: ship stops exactly at halo center, can't go above
                                val targetZoneY = screenHeight / 2f
                                state.shipDragY = state.shipDragY.coerceIn(
                                    targetZoneY,
                                    state.shipRestingY + 20f
                                )
                            }
                            else -> {}
                        }
                    }
                }

                lastTouchX = ex
                lastTouchY = ey
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Reset scroll offsets to prevent jitter on tap
                    if (shipDragPossible) {
                        // Was a tap on ship, not a drag — don't reset page scroll
                    } else {
                        state.pageScrollOffset = 0f
                        state.pageVelocity = 0f
                    }
                    handleTap(ex, ey)
                } else if (activeSwipe == SwipeTarget.PAGE) {
                    val shouldSwitch = abs(state.pageVelocity) > pageVelocityThreshold ||
                            abs(state.pageScrollOffset) > screenWidth * 0.25f

                    val oldPage = state.currentPage
                    if (shouldSwitch) {
                        // Intro cinematic only permits the single forward hop bar(0) → launchpad(1).
                        val cinematicLocked = state.introCinematic
                        if (state.pageScrollOffset > 0 && state.currentPage < 2 &&
                            !(cinematicLocked && state.currentPage >= 1)) {
                            state.currentPage++
                            state.pageScrollOffset -= screenWidth
                        } else if (state.pageScrollOffset < 0 && state.currentPage > 0 && !cinematicLocked) {
                            state.currentPage--
                            state.pageScrollOffset += screenWidth
                        }
                    }
                    // Play swipe sound and crossfade ambient on page change.
                    if (state.currentPage != oldPage) {
                        if (state.introCinematic) {
                            // The one allowed swipe (bar → launchpad): swell rises while drone fades out.
                            SoundManager.playIntroSwell(context)
                            SoundManager.stopAmbient(fadeOutMillis = 1200)
                        } else {
                            SoundManager.playSFX("sfx_ui_swipe")
                            SoundManager.playAmbient(getAmbientForPage(state.currentPage))
                        }
                    }
                    // Pilot walks to new page target (no teleporting)
                    state.setPageTarget(state.currentPage)
                    state.pageVelocity = 0f
                } else if (activeSwipe == SwipeTarget.SHIP_DRAG) {
                    // Check if ship is in the launch zone
                    val targetZoneY = screenHeight / 2f
                    val inZone = abs(state.shipDragY - targetZoneY) < 45f

                    stopHaloRumble()
                    if (inZone && state.isReadyToLaunch() && !state.pilotWalking) {
                        // Snap ship to center and launch
                        state.shipDragY = targetZoneY
                        state.isDraggingShip = false
                        state.phase = HangarPhase.LAUNCHING
                        state.launchProgress = 0f
                        state.launchPhase = 0
                        SoundManager.playSFX("sfx_launch", 0.25f)
                        SoundManager.startCombatMusicEarly(context)
                        // First launch ever: the intro cinematic is over for good.
                        if (state.introCinematic) {
                            persistence.setIntroDone()
                            state.introCinematic = false
                        }
                    } else {
                        // Release → snap back to resting position
                        state.isDraggingShip = false
                    }
                }

                isDragging = false
                activeSwipe = SwipeTarget.NONE
                shipDragPossible = false
                spinButtonHeld = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTap(x: Float, y: Float) {
        when (state.phase) {
            HangarPhase.BROWSING -> {
                // Check nav label taps: [CREW] [LAUNCH] [SHOP] at bottom
                val labelY = screenHeight * 0.95f
                // Nav labels are hidden during the intro cinematic — ignore their tap zone.
                if (!state.introCinematic && y > labelY - 30f && y < labelY + 15f) {
                    val centerX = screenWidth / 2f
                    // Must match HangarRenderer.drawPageIndicator's content-anchored spacing,
                    // or the side labels' tap zones drift off the drawn labels on wide screens.
                    val spacing = layout.content.width * 0.25f
                    for (i in 0..2) {
                        val labelCenterX = centerX + (i - 1) * spacing
                        if (abs(x - labelCenterX) < spacing * 0.4f && i != state.currentPage) {
                            navigateToPage(i)
                            return
                        }
                    }
                }
                when (state.currentPage) {
                    0 -> handleBarTap(x, y)
                    1 -> handleShipyardTap(x, y)
                    2 -> handleStoreTap(x, y)
                }
            }
            HangarPhase.CODEX -> {
                // Tap anywhere closes codex
                state.phase = HangarPhase.BROWSING
            }
            else -> {}
        }
    }

    private fun navigateToPage(targetPage: Int) {
        val oldPage = state.currentPage
        state.pageScrollOffset = (oldPage - targetPage) * screenWidth
        state.currentPage = targetPage
        state.pageVelocity = 0f
        SoundManager.playSFX("sfx_ui_swipe")
        SoundManager.playAmbient(getAmbientForPage(targetPage))
        state.setPageTarget(targetPage)
    }

    private fun handleBarTap(x: Float, y: Float) {
        SoundManager.playSFX("sfx_ui_tap")

        // Codex book on bar counter
        if (renderer.codexBookRect.contains(x, y)) {
            if (persistence.getDiscoveredEvolutions().isNotEmpty()) {
                state.phase = HangarPhase.CODEX
            }
            return
        }

        val pilotIndex = renderer.getPilotGridIndex(x, y)
        if (pilotIndex != null) {
            val pilot = PilotDefinitions.getPilotByIndex(pilotIndex)
            if (pilot != null && state.isPilotUnlocked(pilotIndex)) {
                if (pilotIndex == state.selectedPilotIndex) {
                    // Already selected — fade card to show passive effect
                    state.pilotFlipIndex = pilotIndex
                    state.pilotFlipTimer = 1.25f
                    state.pilotFlipProgress = 1f       // start from fully visible; was 0f which caused 1-frame invisible flash
                    state.pilotFlipShowBack = false    // reset if re-tapping while back face is displayed
                } else {
                    selectPilotAndStartWalk(pilotIndex)
                }
            }
        }
    }

    private fun selectPilotAndStartWalk(pilotIndex: Int) {
        val oldSelectedIndex = state.selectedPilotIndex
        val margin = screenWidth * 0.1f
        val walkable = screenWidth * 0.8f
        val npcWalker = state.npcWalkers.find { it.pilotIndex == pilotIndex }
        if (npcWalker != null) {
            state.pilotX = margin + npcWalker.x * walkable
        } else {
            state.pilotX = state.getPilotWorldTarget(state.currentPage)
        }
        state.pilotTargetX = state.getPilotWorldTarget(state.currentPage)
        state.pilotWalking = state.pilotX != state.pilotTargetX
        state.selectedPilotIndex = pilotIndex
        if (oldSelectedIndex != pilotIndex) {
            val oldPilot = PilotDefinitions.getPilotByIndex(oldSelectedIndex)
            if (oldPilot != null && state.isPilotUnlocked(oldSelectedIndex)) {
                state.pendingNPCAdds.add(WalkerNPC(
                    pilotIndex = oldSelectedIndex,
                    color = oldPilot.color,
                    x = 0.9f,
                    targetX = kotlin.random.Random.nextFloat() * 0.8f + 0.1f,
                    walking = true,
                    idleTimer = 0f
                ))
            }
            state.pendingNPCRemoves.add(pilotIndex)
        }
    }

    private fun handleShipyardTap(x: Float, y: Float) {
        SoundManager.playSFX("sfx_ui_tap")

        // Ship taps — tap on peek ships to switch, tap on selected to purchase
        val shipY = state.shipRestingY
        val shipHitSize = 70f
        if (y > shipY - shipHitSize && y < shipY + shipHitSize) {
            val centerX = screenWidth / 2
            val spacing = renderer.shipSpacing

            // Build visible ship list — mirrors HangarRenderer; corruption = no dead ships
            val isCorrupted = StoryStateManager.isCorrupted(persistence)
            val visibleShips = (0 until ShipDefinitions.getShipCount()).filter { i ->
                val s = ShipDefinitions.getShipByIndex(i) ?: return@filter false
                if (isCorrupted && StoryStateManager.isShipDead(persistence, s.id)) return@filter false
                // Intro cinematic: only Scout is visible, so it's the only tappable ship.
                if (state.introCinematic && i != state.selectedShipIndex) return@filter false
                true
            }
            val currentPos = visibleShips.indexOf(state.selectedShipIndex)
                .let { if (it < 0) 0 else it }

            // Tap left peek ship
            if (x < centerX - shipHitSize && currentPos > 0) {
                state.shipScrollOffset = -spacing
                state.selectedShipIndex = visibleShips[currentPos - 1]
                return
            }
            // Tap right peek ship
            if (x > centerX + shipHitSize && currentPos < visibleShips.size - 1) {
                state.shipScrollOffset = spacing
                state.selectedShipIndex = visibleShips[currentPos + 1]
                return
            }
            // Tap center ship (purchase)
            if (x > centerX - shipHitSize && x < centerX + shipHitSize) {
                val ship = state.getSelectedShip()
                if (ship != null && !state.isShipUnlocked(state.selectedShipIndex)) {
                    if (state.canUnlockShip(state.selectedShipIndex) &&
                        state.actualYen >= ship.cost) {
                        persistence.addYen(-ship.cost)
                        persistence.unlockShip(ship.id)
                        state.actualYen = persistence.getYen()
                        telemetryManager.logPurchase("ship_purchase", ship.id, 0, ship.cost, state.actualYen)
                        SoundManager.playSFX("sfx_ui_purchase")
                    }
                }
                return
            }
        }
    }

    private fun handleStoreTap(x: Float, y: Float) {
        // Mute toggle buttons (checked before generic tap sound)
        val audioRect = state.audioMuteButtonRect
        if (audioRect != null && audioRect.contains(x, y)) {
            state.audioMuted = !state.audioMuted
            persistence.setAudioMuted(state.audioMuted)
            SoundManager.setMuted(state.audioMuted)
            if (!state.audioMuted) SoundManager.playSFX("sfx_ui_tap")
            return
        }

        val vibRect = state.vibrationMuteButtonRect
        if (vibRect != null && vibRect.contains(x, y)) {
            state.vibrationMuted = !state.vibrationMuted
            persistence.setVibrationMuted(state.vibrationMuted)
            isVibrationMuted = state.vibrationMuted
            SoundManager.playSFX("sfx_ui_tap")
            if (!state.vibrationMuted) {
                // Gentle confirmation tap when re-enabling vibration
                vibrator.vibrate(VibrationEffect.createOneShot(30, 40))
            }
            return
        }

        SoundManager.playSFX("sfx_ui_tap")

        // Maintenance hatch tap (codex secret)
        val hatchRect = state.hatchRect
        if (hatchRect != null && hatchRect.contains(x, y) && !state.hatchOpen) {
            state.hatchTapCount++
            if (state.hatchTapCount >= 5) {
                state.hatchOpen = true
                if (!state.codexDiscovered) {
                    state.codexDiscovered = true
                    persistence.setCodexDiscovered()
                }
            }
            return
        }

        // Paper tap — open codex
        val paperRect = state.paperRect
        if (state.hatchOpen && paperRect != null && paperRect.contains(x, y)) {
            state.phase = HangarPhase.CODEX
            return
        }

        for ((index, rect) in renderer.upgradeRects.withIndex()) {
            if (rect.contains(x, y)) {
                handleUpgradeTap(index)
                return
            }
        }
        // Slot machine spin button
        if (renderer.spinButtonRect.contains(x, y)) {
            handleSlotSpin()
            return
        }
    }

    private fun handleSlotSpin() {
        if (state.isSpinning) return
        if (state.actualYen < 100) return

        synchronized(upgradeLock) {
            if (state.actualYen < 100) return
            // Deduct cost immediately
            val newYen = state.actualYen - 100
            persistence.setYen(newYen)
            state.actualYen = newYen
        }

        // Roll outcome
        val roll = kotlin.random.Random.nextFloat()
        var outcome: Triple<Int, Int, String?> = Triple(-1, 0, null) // default: loss
        val rascalRigged = StoryStateManager.hasLoopedBefore(persistence)
                && persistence.isPilotUnlocked("pilot_rascal")
                && !StoryStateManager.isAstroLoop(persistence)  // Astro Loop is never rigged
        val isCorrupted = StoryStateManager.isCorrupted(persistence)
        // Note: isCorrupted branches below are dead — the if (!isCorrupted) guard above skips
        // the entire when block in corruption runs (outcome stays at the default loss Triple).
        // Thresholds kept here so the non-corruption paths remain readable in one place.
        val jackpotThreshold = when {
            isCorrupted -> 0.005f
            rascalRigged -> 0.15f
            state.isWhiskersJackpotEligible() -> 0.10f
            else -> 0.005f
        }
        val diamondThreshold = when {
            isCorrupted -> 0.02f
            rascalRigged -> 0.15f
            else -> 0.02f
        }
        if (!isCorrupted) when {
            roll < jackpotThreshold -> {
                // Jackpot — free random upgrade or 10k yen
                val upgradeResult = tryGrantRandomUpgrade()
                if (upgradeResult != null) {
                    outcome = Triple(StorePageRenderer.SYM_ROCKET, 0, upgradeResult)
                } else {
                    outcome = Triple(StorePageRenderer.SYM_ROCKET, 10000, null)
                }
                // Recruit Whiskers if eligible
                if (state.isWhiskersJackpotEligible()) {
                    val pilot = state.recruitNextPilot()
                    if (pilot != null) {
                        telemetryManager.logPurchase("pilot_jackpot", pilot.id, 0, 0, persistence.getYen())
                        val pilotIndex = PilotDefinitions.pilots.indexOf(pilot)
                        val npcRandom = kotlin.random.Random(System.currentTimeMillis())
                        state.pendingNPCAdds.add(WalkerNPC(
                            pilotIndex = pilotIndex,
                            color = pilot.color,
                            x = npcRandom.nextFloat() * 0.8f + 0.1f,
                            targetX = npcRandom.nextFloat() * 0.8f + 0.1f,
                            walking = false,
                            idleTimer = 1f
                        ))
                        chatSystem.onPilotRecruited(state, pilot.callsign)
                        SoundManager.playSFX("sfx_pilot_recruit", 0.25f)
                    }
                }
            }
            roll < jackpotThreshold + diamondThreshold -> outcome = Triple(StorePageRenderer.SYM_DIAMOND, 2000, null)
            roll < jackpotThreshold + diamondThreshold + 0.06f -> outcome = Triple(StorePageRenderer.SYM_STAR, 700, null)
            roll < jackpotThreshold + diamondThreshold + 0.12f -> outcome = Triple(StorePageRenderer.SYM_YEN, 100, null)
            roll < jackpotThreshold + diamondThreshold + 0.20f -> outcome = Triple(StorePageRenderer.SYM_BOLT, 75, null)
            roll < jackpotThreshold + diamondThreshold + 0.30f -> outcome = Triple(StorePageRenderer.SYM_WRENCH, 50, null)
            else -> outcome = Triple(-1, 0, null)
        }

        val (symbol, yenPayout, upgradeName) = outcome

        // Set reel values — all three show the winning symbol (or mixed for loss)
        if (symbol == -1) {
            // Mixed — random non-matching symbols
            val rng = kotlin.random.Random
            state.reelValues[0] = rng.nextInt(StorePageRenderer.SYMBOL_COUNT)
            state.reelValues[1] = rng.nextInt(StorePageRenderer.SYMBOL_COUNT)
            do {
                state.reelValues[2] = rng.nextInt(StorePageRenderer.SYMBOL_COUNT)
            } while (state.reelValues[0] == state.reelValues[1] && state.reelValues[1] == state.reelValues[2])
        } else {
            state.reelValues[0] = symbol
            state.reelValues[1] = symbol
            state.reelValues[2] = symbol
        }

        // Stagger stop times: left first, then middle, then right
        val now = System.currentTimeMillis()
        state.reelStopTimes[0] = now + 800L
        state.reelStopTimes[1] = now + 1100L
        state.reelStopTimes[2] = now + 1400L

        state.spinResultYen = yenPayout
        state.spinResultUpgrade = upgradeName
        state.spinResultSymbol = symbol
        state.spinResultTime = 0  // Set when animation completes
        state.isSpinning = true
        SoundManager.playSFX("sfx_slot_spin")
        persistence.incrementCasinoSpins()

        val symbolNames = state.reelValues.map { StorePageRenderer.getSymbolName(it) }
        telemetryManager.logCasinoSpin(symbolNames, yenPayout, state.actualYen)
    }

    private fun tryGrantRandomUpgrade(): String? {
        val upgradeIds = listOf("health", "shields", "speed", "damage", "crit", "magnet", "yen_bonus", "salvage")
        val upgradeNames = listOf("SALVAGE PLATE", "DEFLECTOR RIG", "NITRO BOOST", "HOT ROUNDS", "LUCKY ROUNDS", "HAUL LINE", "FINDER'S FEE", "SCAVENGER RIG")
        val nonMaxed = mutableListOf<Int>()
        for (i in upgradeIds.indices) {
            if (persistence.getUpgradeLevel(upgradeIds[i]) < 5) {
                nonMaxed.add(i)
            }
        }
        if (nonMaxed.isEmpty()) return null
        val chosen = nonMaxed[kotlin.random.Random.nextInt(nonMaxed.size)]
        val id = upgradeIds[chosen]
        val currentLevel = persistence.getUpgradeLevel(id)
        persistence.setUpgradeLevel(id, currentLevel + 1)
        return upgradeNames[chosen]
    }

    private val upgradeLock = Any()

    private fun handleUpgradeTap(index: Int) {
        val upgradeIds = listOf("health", "shields", "speed", "damage", "crit", "magnet", "yen_bonus", "salvage")

        // Time Crystal — 9th tile (index 8), auto-equipped when unlocked — no purchase needed
        if (index == 8) return

        if (index >= upgradeIds.size) return

        synchronized(upgradeLock) {
            val id = upgradeIds[index]
            val currentLevel = persistence.getUpgradeLevel(id)
            if (currentLevel >= 5) return  // Already maxed

            val cost = PersistenceManager.getUpgradeCost(currentLevel)
            if (state.actualYen >= cost) {
                val newYen = state.actualYen - cost
                persistence.setYen(newYen)
                persistence.setUpgradeLevel(id, currentLevel + 1)
                state.actualYen = newYen
                telemetryManager.logPurchase("store_upgrade", id, currentLevel + 1, cost, newYen)
                SoundManager.playSFX("sfx_ui_purchase")
            }
        }
    }

    private fun getAmbientForPage(page: Int): String = "bgm_${SoundManager.activeSet}_hangar"

    private fun startHaloRumble() {
        if (isVibrationMuted) return
        if (isVibratingHalo) return
        isVibratingHalo = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(
                longArrayOf(0, 50, 50), intArrayOf(0, 40, 0), 0
            ))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50), 0)
        }
    }

    private fun stopHaloRumble() {
        if (!isVibratingHalo) return
        isVibratingHalo = false
        vibrator.cancel()
    }

    private fun checkPilotRecruitment() {
        if (state.checkPilotUnlockCondition()) {
            val pilot = state.recruitNextPilot()
            if (pilot != null) {
                telemetryManager.logPurchase("pilot_unlock", pilot.id, 0, 0, persistence.getYen())
                val pilotIndex = PilotDefinitions.pilots.indexOf(pilot)
                val npcRandom = kotlin.random.Random(System.currentTimeMillis())
                state.pendingNPCAdds.add(WalkerNPC(
                    pilotIndex = pilotIndex,
                    color = pilot.color,
                    x = npcRandom.nextFloat() * 0.8f + 0.1f,
                    targetX = npcRandom.nextFloat() * 0.8f + 0.1f,
                    walking = false,
                    idleTimer = 1f
                ))
                chatSystem.onPilotRecruited(state, pilot.callsign)
                SoundManager.playSFX("sfx_pilot_recruit", 0.25f)
            }
        }
    }

    fun pause() {
        stopHaloRumble()
        running = false
        gameThread?.join()
    }

    fun resume() {
        if (!running) {
            running = true
            gameThread = Thread(this)
            gameThread?.start()
        }
    }

    fun addYenFromRun(amount: Int) {
        persistence.addYen(amount)
        state.actualYen = persistence.getYen()
        persistence.incrementRunsSincePilotUnlock()
        if (!StoryStateManager.isCorrupted(persistence)) chatSystem.resetUsedLines()
        val pilotId = persistence.getSelectedPilotId()
        if (persistence.isFreshLoopStart()) {
            persistence.clearFreshLoopStart()
            chatSystem.onFirstLaunch(state)
        } else {
            chatSystem.onDeathReturn(state, pilotId)
        }

        // Corruption: check if all crew are dead and crystal should unlock
        if (StoryStateManager.isCorrupted(persistence)) {
            if (StoryStateManager.shouldUnlockCrystal(persistence)) {
                persistence.setCrystalUnlocked(true)
            }

            // Remove dead pilots from NPC walkers
            val deadPilots = persistence.getDeadPilots()
            if (deadPilots.isNotEmpty()) {
                for (i in 0 until PilotDefinitions.getPilotCount()) {
                    val pilot = PilotDefinitions.getPilotByIndex(i) ?: continue
                    if (deadPilots.contains(pilot.id)) {
                        state.pendingNPCRemoves.add(i)
                    }
                }
            }

            // Crystal reveal: set up pending reveal when crystal unlocked and all crew dead
            if (persistence.isCrystalUnlocked() && StoryStateManager.allCrewDead(persistence)) {
                if (!persistence.isAwaitingCrystalReveal()) {
                    // First time all crew dead + crystal unlocked — set up reveal
                    persistence.setAwaitingCrystalReveal(true)
                }
                // Don't auto-select Astro. initCorruptionState() will handle the rest.
                state.selectedPilotIndex = -1
                val specterIndex = ShipDefinitions.ships.indexOfFirst { it.id == "ship_white" }
                if (specterIndex >= 0) state.selectedShipIndex = specterIndex
            } else {
                // If currently selected pilot/ship is now dead, find first available
                if (!state.isPilotUnlocked(state.selectedPilotIndex)) {
                    val firstAvailable = (0 until PilotDefinitions.getPilotCount()).firstOrNull { state.isPilotUnlocked(it) }
                    if (firstAvailable != null) state.selectedPilotIndex = firstAvailable
                }
                if (!state.isShipUnlocked(state.selectedShipIndex)) {
                    val firstAvailable = (0 until ShipDefinitions.ships.size).firstOrNull { state.isShipUnlocked(it) }
                    if (firstAvailable != null) state.selectedShipIndex = firstAvailable
                }
            }
        }

        checkPilotRecruitment()
    }

    fun resetForReturn(fadeFromWhite: Boolean = false) {
        state.phase = HangarPhase.BROWSING
        state.launchProgress = 0f
        state.launchPhase = 0
        // Reset scroll states
        state.pageScrollOffset = 0f
        state.pageVelocity = 0f
        // Refresh the music set from current story stage
        SoundManager.activeSet = StoryStateManager.stageMusicSet(persistence)
        // Reset to bar page and start ambient
        state.currentPage = 0
        SoundManager.playAmbient(getAmbientForPage(0))
        val barTarget = state.getPilotWorldTarget(0)
        state.pilotX = barTarget
        state.pilotTargetX = barTarget
        state.pilotWalking = false

        // Reset slot machine result state
        state.spinResultYen = 0
        state.spinResultUpgrade = null
        state.spinResultSymbol = -1
        state.spinResultTime = 0

        // Reset ship drag
        state.isDraggingShip = false
        state.shipDragY = state.shipRestingY

        // Initialize corruption state if we're in the corruption phase
        // This rebuilds NPC walkers (filtering dead pilots) and auto-selects Astro+Specter
        if (StoryStateManager.isCorrupted(persistence)) {
            state.initCorruptionState(persistence)
            state.fadeFromBlackTimer = 3.0f
        } else if (StoryStateManager.isAstroLoop(persistence)) {
            if (persistence.isAstroLoopFirstEntry()) {
                // First entry: default to Astro + Specter
                val astroIndex = (0 until PilotDefinitions.getPilotCount()).firstOrNull { i ->
                    PilotDefinitions.getPilotByIndex(i)?.id == "pilot_astro"
                } ?: -1
                val specterIndex = (0 until ShipDefinitions.getShipCount()).firstOrNull { i ->
                    ShipDefinitions.getShipByIndex(i)?.id == "ship_white"
                } ?: -1
                if (astroIndex >= 0) state.selectedPilotIndex = astroIndex
                if (specterIndex >= 0) state.selectedShipIndex = specterIndex
            } else {
                // Later entries: restore last-flown selection
                val savedShipId = persistence.getSelectedShipId()
                val savedPilotId = persistence.getSelectedPilotId()
                state.selectedShipIndex = (0 until ShipDefinitions.getShipCount())
                    .firstOrNull { ShipDefinitions.getShipByIndex(it)?.id == savedShipId } ?: 0
                state.selectedPilotIndex = (0 until PilotDefinitions.getPilotCount())
                    .firstOrNull { PilotDefinitions.getPilotByIndex(it)?.id == savedPilotId } ?: 0
            }

            // Black fade-in whenever the game already faded itself to black before the
            // handoff (desert farewell on first entry, reckoning win). First entry
            // additionally fires TB's one-shot welcome.
            if (fadeFromWhite) {
                state.fadeFromBlackTimer = 2.0f
                if (persistence.isAstroLoopFirstEntry()) {
                    state.pendingTbWelcome = true
                    persistence.clearAstroLoopFirstEntry()
                }
            }

            // Rebuild the bar roster (mirrors the corruption branch above). Without this,
            // first entry inherits the empty end-of-corruption walker list and the revived
            // crew stays invisible until each pilot is cycled through the grid.
            state.rebuildNpcWalkers()
        } else {
            val savedShipId = persistence.getSelectedShipId()
            val savedPilotId = persistence.getSelectedPilotId()
            val shipIdx = (0 until ShipDefinitions.getShipCount())
                .firstOrNull { ShipDefinitions.getShipByIndex(it)?.id == savedShipId } ?: 0
            val pilotIdx = (0 until PilotDefinitions.getPilotCount())
                .firstOrNull { PilotDefinitions.getPilotByIndex(it)?.id == savedPilotId } ?: 0
            state.selectedShipIndex = shipIdx
            state.selectedPilotIndex = pilotIdx
            state.glitchTimer = 1.0f
        }
    }
}
