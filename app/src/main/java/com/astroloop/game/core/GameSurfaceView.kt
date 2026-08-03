package com.astroloop.game.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.graphics.Path
import com.astroloop.game.MainActivity
import com.astroloop.game.BuildConfig
import com.astroloop.game.data.CrystalFightLines
import com.astroloop.game.data.CorruptedCrewDefinitions
import com.astroloop.game.data.DesertDefinitions
import com.astroloop.game.data.LoopDefinitions
import com.astroloop.game.data.HighScoreManager
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.data.TelemetryManager
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.RadioDefinitions
import com.astroloop.game.data.EnemyType
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.entity.*
import com.astroloop.game.input.TouchController
import com.astroloop.game.render.*
import com.astroloop.game.system.*
import com.astroloop.game.util.Collision2D
import com.astroloop.game.weapon.weapons.EnergySaw
import com.astroloop.game.weapon.weapons.WarpSaw
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

class GameSurfaceView(
    context: Context,
    private val startingShipId: String = "ship_blue",
    private val startingPilotId: String = "pilot_medic",
    private val onGameOver: (yenEarned: Int, fadeFromWhite: Boolean) -> Unit = { _, _ -> }
) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private val state = GameState()
    private val touchController = TouchController()
    private val highScoreManager = HighScoreManager(context)
    private val telemetryManager = TelemetryManager(context)

    // Camera for world-to-screen transformation
    private val camera = Camera()

    // Entities
    private val ship = Ship()
    private val shipExplosion = ShipExplosion()
    private val enemyExplosions = mutableListOf<ShipExplosion>()
    private val boss = Boss()
    private val visualEffects = VisualEffectManager()

    // Pre-allocated scratch lists — reused every frame to avoid GC pressure
    private val activeAsteroids = ArrayList<Asteroid>(150)
    private val activeProjectiles = ArrayList<Projectile>(300)
    private val activePowerUps = ArrayList<PowerUp>(50)
    private val activeEnemies = ArrayList<EnemyShip>(20)
    private val allTargetsCache = ArrayList<Entity>(350)

    // Renderers
    private val shapeRenderer = ShapeRenderer()
    private val vectorRenderer = VectorRenderer(shapeRenderer)
    private val starfieldRenderer = StarfieldRenderer()
    private val hudRenderer = HUDRenderer()
    private val upgradeSelectionRenderer = UpgradeSelectionRenderer()
    private val debugMenuRenderer = DebugMenuRenderer().also { it.telemetryManager = telemetryManager }
    private val crystalRenderer = CrystalRenderer()
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
    }
    private var isVibrationMuted: Boolean = context.getSharedPreferences("astrohunt_save", android.content.Context.MODE_PRIVATE)
        .getBoolean("vibration_muted", false)
    private var crystalDelayTimer: Float = 0f
    private var crystalDelayActive: Boolean = false
    private var corruptionDeathTimer: Float = -1f

    // Crystal death freeze delay
    private var crystalFreezeDelay = 0f

    // Death play-out: game world keeps running for 2s after death before crystal
    private var deathPlayOutTimer = 0f

    // Boss reply — corrupted Astro responds "..." 3s after pilot's boss_spawn line
    private var bossReplyTimer = 0f
    private var bossReplySent = false
    private var pastAstroArrived = false
    private var pastAstroFireTimer = 0f
    private val playerRushBurn = ReentryBurn()
    private var sawShieldSparkCooldown = 0f   // throttles fleet-saw shield sparks
    private var engineRestarting = false
    private var engineRestartTimer = 0f
    private var engineSputterToggleTimer = 0f
    private var engineCatchFired = false
    private var engineStruggleLineFired = false

    // Systems
    private val spawnSystem = SpawnSystem(EntityPools.asteroids)
    private val collisionSystem = CollisionSystem()
    private val weaponSystem = WeaponSystem(EntityPools.projectiles)
    private val upgradeSystem = UpgradeSystem(EntityPools.powerUps)
    private val difficultySystem = DifficultySystem()
    private val movementSystem = MovementSystem()
    private val shipEvolutionSystem = ShipEvolutionSystem()
    private val enemySpawnSystem = EnemySpawnSystem(EntityPools.enemies)
    private val enemyAISystem = EnemyAISystem(EntityPools.projectiles)
    private val enemyWeaponSystem = EnemyWeaponSystem(EntityPools.projectiles)
    private val lootSystem = LootSystem(state, ship, spawnSystem, upgradeSystem, visualEffects)
    private val fadingTrails = mutableListOf<FadingTrail>()
    private val bossSystem = BossSystem(state, boss, ship, weaponSystem, visualEffects, ::handlePlayerDeath)
    private val fleetSystem = FleetSystem(boss, ship, EntityPools.projectiles, visualEffects)
    private val sawDamageSystem = SawDamageSystem(state, ship, weaponSystem, visualEffects, ::applyDamageModifiers, ::handleAsteroidDestroyedWithTrail, ::onEnemyDestroyed,
        spawnProjectile = { /* projectiles live in EntityPools.projectiles — obtain() adds to inUse, no separate list needed */ })
    private val beamDamageSystem = BeamDamageSystem(
        state, ship, weaponSystem,
        addDamageNumber = { x, y, amount, color, isCrit -> visualEffects.addDamageNumber(x, y, amount, color, isCrit) },
        addHitFlash = { x, y, size, color -> visualEffects.addHitFlash(x, y, size, color) },
        applyDamageModifiers = ::applyDamageModifiers,
        onAsteroidDestroyed = ::handleAsteroidDestroyedWithTrail,
        onEnemyDestroyed = ::onEnemyDestroyed,
        playGrindSound = {
            SoundManager.playSFX("sfx_weapon_oblivion_beam", SoundManager.getWeaponSfxVolume("oblivion_beam"))
        }
    )
    private var orbiterSoundCooldown = 0f
    private val vampiricLeecherSystem = VampiricLeecherSystem(
        onAsteroidDestroyed = ::handleAsteroidDestroyedWithTrail
    )
    private val combatDroneSystem = CombatDroneSystem(state, ship)
    private val projectileEffectsSystem = ProjectileEffectsSystem(ship, state, collisionSystem, visualEffects, ::applyDamageModifiers, ::handleAsteroidDestroyedWithTrail, ::onEnemyDestroyed, ::handlePlayerDeath)
    private val radioSystem = RadioSystem()
    private val crewmateEncounter = CrewmateEncounter(ship, EntityPools.projectiles)

    // Crystal reckoning fight — damage/hitbox knobs live in CrystalFightSystem (fairness-tested)
    private val crystalFightSystem = CrystalFightSystem()
    private var reckoningBoss: Boss? = null

    // Ghost-ship lance climax (Task 8)
    private val ghostLance = com.astroloop.game.render.GhostShipLance()
    private var reckoningGhostStep = 0        // cursor into CrystalFightLines.ghostScript
    private var reckoningGhostTimer = 0f      // seconds since the lance began
    private var reckoningLanceDoneHold = 0f   // post-lance breathing room before the fly-home
    private var reckoningWatchX = 0f          // where the ghosts gather — Astro turns to watch
    private var reckoningWatchY = 0f
    private var reckoningFlyOffComplete = false // ship has left the frozen view heading south
    private var reckoningExitFadeTimer = 0f     // drives the post-fly-off fade to black
    private var reckoningWinHandedOff = false // one-shot guard: hangar handoff fires exactly once

    // Per-bullet tick: every reckoning bullet voices sfx_reckoning_bullet, pitch-jittered so
    // same-frame arms read as a swarm shimmer, not one phase-summed blip. Soft (clip baked to
    // -20 LUFS ~0.1s) so ~40 bullets/s in P5 stays inside SoundPool's 16-stream budget.
    private val RECKONING_BULLET_VOLUME = 0.225f
    private val RECKONING_BULLET_PITCH_JITTER = 0.08f  // ±8% detune, centered on the baked pitch

    // Two-part radio scheduling for reckoning scripted lines
    private val CRYSTAL_CALLSIGN = "CRYSTAL"
    private var pendingReckoningLine: String? = null
    private var pendingReckoningSpeaker: String = ""
    private var pendingReckoningColor: Int = 0
    private var pendingReckoningTimer: Float = 0f
    private val astroRadioColor: Int
        get() = PilotDefinitions.getPilot("pilot_astro")?.color ?: CrystalPalette.ICE

    // Reusable paint for reckoning boss orb (avoids per-frame allocation)
    private val reckoningOrbPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    // Background paint
    private val backgroundPaint = Paint().apply {
        color = GameConfig.COLOR_BACKGROUND
        style = Paint.Style.FILL
    }

    // Heart-to-heart dialogue paint
    private val heartLinePaint = Paint().apply {
        isAntiAlias = true
        textSize = 40f
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
        typeface = FontManager.getRegular()
    }

    private var heartToHeartFadingOut = false
    private var heartToHeartFadeTimer = 0f
    private val heartToHeartFadeDuration = 1.0f   // 1s fade-out

    // Desert farewell → timeline shift
    private var timelineShiftAlpha = 0f
    private var timelineShiftHoldTimer = 0f
    private val TIMELINE_SHIFT_FADE_DURATION = 3.0f
    private val TIMELINE_SHIFT_HOLD_DURATION = 0.5f

    // Global fade overlay for scene transitions
    private var globalFadeAlpha = 0f
    private var globalFadeFading = false
    private var globalFadeTimer = 0f

    // Asteroid hit sound cooldown (separate from damage invulnerability)
    private var asteroidHitSoundCooldown = 0f
    private var bossAuraSoundCooldown = 0f

    // Brick screen animation state
    private var brickScreenTimer: Float = 0f
    private var brickStatsRevealed = false
    private var brickStatsTimer = 0f
    private var brickSoundPlayed = false
    // Cached brick screen stats (populated once on reveal)
    private var cachedBrickPlaytime = 0f
    private var cachedBrickKills = 0
    private var cachedBrickDeaths = 0
    private var cachedBrickYenEarned = 0
    private var cachedBrickEvolutions = 0
    private var cachedBrickCasinoSpins = 0
    private var cachedBrickStatLines: List<Pair<String, String>> = emptyList()

    // Desert flashback tracking fields
    private var desertSpawnTimer = 0f
    private var desertFirstEnemiesSpawned = false
    private var desertSecondWaveSpawned = false
    private var desertThirdWaveAnnounced = false
    private var desertFirstKill = false
    private var desertKillCount = 0
    private var desertAmbiguousTargetSpawned = false
    private var desertPlayerShotInPhase2 = false
    private var desertPlayerContinuedShooting = false
    private var desertPlayerFiredAfterStop = false
    private var desertHorrorComplete = false
    private var desertGoodEndingLinesComplete = false
    private var desertTbX = 0f
    private var desertTbY = 0f
    private var desertTbAngle = 0f
    private var desertPlayerTurretAngle = 0f
    private var desertTbTurretAngle = 0f
    private var desertPlayerSpeed = 0f
    private var desertTbSpeed = 0f
    private var desertTbAlive = true
    private var desertTbDeathTimer = 0f
    private var desertNearSettlement = false
    private var desertBombardmentUnlocked = false
    private var desertStopCheckReached = false
    private var desertStopCheckTimer = 0f
    private var desertStopCheckY = 0f  // Player Y when "We should stop" fires
    private var desertNorthDriveTimer = 0f  // Cumulative time actively driving north after STOP_CHECK
    private var desertEndingTimer = 0f
    private var desertFarewellFadeStarted = false
    private var desertFarewellFadeTimer = 0f
    private val desertTankPaint = Paint().apply { isAntiAlias = true }
    data class VehicleTrack(val x: Float, val y: Float, val angle: Float, val width: Float, var age: Float = 0f)
    private val desertTracks = mutableListOf<VehicleTrack>()
    private var desertTrackSpawnTimer = 0f
    private val TRACK_LIFETIME = 15f
    private val MAX_TRACKS = 500
    private val DESERT_CORRIDOR_LEFT = -400f
    private val DESERT_CORRIDOR_RIGHT = 400f
    private val DESERT_CORRIDOR_WIDTH = DESERT_CORRIDOR_RIGHT - DESERT_CORRIDOR_LEFT
    data class DustParticle(var x: Float, var y: Float, var alpha: Float, var age: Float)
    private val desertDustParticles = mutableListOf<DustParticle>()
    private val desertMountainPath = Path()
    private var desertPlayerHasFired = false
    private var desertTbFireTimer = 0f
    data class DesertWreck(val x: Float, val y: Float, val angle: Float, val color: Int, val hasGun: Boolean, var burnTimer: Float = 0f)
    private val desertWrecks = mutableListOf<DesertWreck>()
    private var desertSettlementProgress = 0f
    private var desertCrystalDeath = false
    private var desertSpawnY = 0f
    private var desertSettlementWorldY = 0f
    data class DesertCivilian(var x: Float, var y: Float, var angle: Float, var speed: Float, var fleeing: Boolean = false)
    private val desertCivilians = mutableListOf<DesertCivilian>()
    // Tank fire timer (homing missiles only)
    private var desertPlayerFireTimer = 0f
    // Second wave spawn delay
    private var desertSecondWaveSpawnDelay = -1f  // -1 = not waiting
    // Settlement buildings (destructible on horror path)
    enum class BuildingDeathStyle { CHARRED, RUBBLE }
    data class DesertBuilding(val x: Float, val y: Float, val w: Float, val h: Float, var alive: Boolean = true, var deathStyle: BuildingDeathStyle = BuildingDeathStyle.CHARRED, var burnTimer: Float = 0f)
    private val desertBuildings = mutableListOf<DesertBuilding>()
    private var desertBombardmentActive = false
    private var desertBombardmentDelayTimer = -1f  // -1 = not counting
    // TB stops firing after "They're running"
    private var desertTbCeaseFire = false
    // Aftermath delay: 4s after all buildings destroyed
    private var desertAllBuildingsDestroyedTimer = -1f  // -1 = not tracking
    // Event tracking for civilians visible trigger
    private var desertCiviliansVisible = false
    private var desertPlayerFiredAtCivilians = false
    // Phase 1: one missile per non-combatant (track targeted enemy IDs)
    private val desertPlayerTargeted = mutableSetOf<Int>()  // enemy hashCodes already targeted
    private val desertTbTargeted = mutableSetOf<Int>()
    // Settlement visible timer: 10s delay before bombardment order
    private var desertSettlementVisibleTimer = -1f  // -1 = not visible yet
    // Good ending: camera freeze + drive-off
    private var desertCameraFrozen = false
    private var desertDriveOffActive = false
    // Crystal sequence: TB walks to crystal
    private var desertTbWalkingToCrystal = false
    private var desertTbCrystalTarget = floatArrayOf(0f, 0f)
    private var desertCrystalDrainPhase = 0  // 0=none, 1=draining, 2=powering_down, 3=dead, 4=ram, 5=flash
    private var desertTbHitWall = false  // TB reached crystal wall — slow + drained colors
    private var desertPlayerHitWall = false  // Player reached crystal wall
    private var desertTbFiringAtCrystal = false  // TB shooting cosmetic shots at crystal
    private var desertTbFireAtCrystalTimer = 0f
    private var desertWakeUpTimer = 0f
    private val WAKE_UP_DURATION = 4.0f
    private val WAKE_UP_SILENCE = 2.0f   // black silence before pull-back starts

    // Screen dimensions
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    private var renderScale: Float = 1f

    // System-cutout insets in physical px, forwarded by MainActivity. Divided by
    // renderScale into design units when building the ScreenLayout.
    private var insetLeftPx = 0f
    private var insetTopPx = 0f
    private var insetRightPx = 0f
    private var insetBottomPx = 0f

    var layout: ScreenLayout = ScreenLayout.compute(GameConfig.DESIGN_WIDTH, GameConfig.DESIGN_HEIGHT)
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
            synchronized(holder) {
                applyScreenDimensions(width, height)
                // Insets can arrive AFTER surfaceChanged has already initialized the renderers
                // with a zero-inset layout. The layout-consuming renderers cache their own copy,
                // so push the freshly-inset layout into them or the combat HUD would stay anchored
                // to safe==full and could sit under the cutout. (screenWidth/height are unchanged
                // on an inset-only delivery, so camera/spawn/movement need no re-init.)
                hudRenderer.initialize(layout, context.resources.configuration.smallestScreenWidthDp)
                upgradeSelectionRenderer.initialize(layout)
            }
        }
    }

    // Combat Drones and Point Defense managed by CombatDroneSystem

    // Energy Saw / Warp Saw state (managed by SawDamageSystem)

    // State persistence for screen lock/unlock
    private var wasInitialized = false

    // Double-tap detection for debug menu
    private var lastTapTime: Long = 0
    private val doubleTapThreshold = 400L  // ms — widened from 300 for comfortable double-tap
    private val tapHoldThreshold = 300L   // ms — max hold duration to count as a tap (not a long-press)
    private var tapDownX: Float = 0f
    private var tapDownY: Float = 0f
    private var tapDownTime: Long = 0L
    private var lastDebugMenuPage: Int = -1  // Track page changes for debug BGM mute
    private var pauseDebugHoldTimer: Float = 0f
    private var pauseDebugHoldActive: Boolean = false

    // Fully-upgraded state tracking (for upgrade diamond fade-out)
    private var wasFullyUpgraded = false

    // Kill streak tracking
    private var currentKillStreak = 0
    private var bestKillStreakThisRun = 0
    private var lastKillTime = 0f
    private var enemiesKilledThisRun = 0

    // Continuous flight tracking (Dash unlock)
    private var continuousFlightTimer = 0f
    private var continuousFlightPauseTimer = 0f   // grace for brief stops / post-upgrade
    private var bestContinuousFlightThisRun = 0

    // Cached corruption run flags (computed once in initializeGame, avoids per-frame PersistenceManager alloc)
    private var isNonAstroCorruptionRun = false
    private var isAstroCorruptionRun = false
    private var cachedStoryPhase = 0

    companion object {
        // Desert spawn band: enemies appear 7%-27% of the viewport height above the top
        // edge, i.e. 0.57-0.77 viewport-heights north of the centered player.
        const val DESERT_SPAWN_BAND_NEAR = 0.07f
        const val DESERT_SPAWN_BAND_SPREAD = 0.20f

        /** Desert enemy despawn radius from the camera center. Viewport-relative because
         *  the spawn band is — a fixed radius culled fresh spawns on tall screens. */
        fun desertDespawnDistance(screenHeight: Float): Float = screenHeight * 1.1f

        // Boss fight phases - normal
        const val PHASE_NONE = 0
        const val PHASE_SURVIVAL = 1
        const val PHASE_DRONE_SENT = 2
        const val PHASE_WAITING_FLEET = 3
        const val PHASE_FLEET_CHATTER = 4
        const val PHASE_POST_VICTORY = 6
        const val PHASE_FORMATION = 7
        const val PHASE_SHIELD_ASSAULT = 8
        const val PHASE_TB26_RAM = 9
        // Boss fight phases - from other side
        const val PHASE_OTHER_SPAWN = 10
        const val PHASE_OTHER_SURVIVAL = 11
        const val PHASE_OTHER_FLEET = 12
        const val PHASE_OTHER_DYING = 13
        const val PHASE_OTHER_FORMATION = 14
        const val PHASE_OTHER_SHIELD_ASSAULT = 15
        const val PHASE_OTHER_TB26_RAM = 16
        // Boss fight phases - ending
        const val PHASE_HEART_TRANSITION = 20
        const val PHASE_HEART_DIALOGUE = 21

        // Corruption run chatter lines (<=36 chars each)
        private val CORRUPTION_CHATTER = listOf(
            // Phase 1: Determined
            "I'm coming for you, TB-26.",
            "Just hold on.",
            "I'll fix this.",
            // Phase 2: Unhinged
            "They keep getting in the way.",
            "None of them understand.",
            "I have to keep going.",
            "It has to work this time.",
            // Phase 3: Desperate
            "WHERE ARE YOU?",
            "Why won't it work?",
            "Please. Just come back.",
            "I can't stop now."
        )

        private val CORRUPTION_KILL_LINES = listOf(
            "Go home. Now.",
            "You haven't seen him.",
            "Stay safe. That's an order.",
            "Please. Just stay away.",
            "TB-26, where are you?"
        )

        // Encounter timing: game-time thresholds (at 5x, 100 game-seconds ~ 20 real seconds)
        private val CORRUPTION_ENCOUNTER_TIMES = listOf(110f, 210f, 310f, 410f, 510f)
        private const val CORRUPTION_CHATTER_INTERVAL = 8f  // real seconds between chatter
    }

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
        touchController.renderScale = renderScale
        debugMenuRenderer.renderScale = renderScale
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Get dimensions
        applyScreenDimensions(width, height)

        // Only initialize game on first creation, not on screen unlock
        if (!wasInitialized) {
            initializeGame()
            wasInitialized = true
        }

        // Start game thread only if not already running (resume() may have started it first)
        if (gameThread == null || !gameThread!!.isAlive) {
            gameThread = GameThread(holder, this)
            gameThread?.setRunning(true)
            gameThread?.start()
        }

        // Re-activate crystal if paused but crystal was lost (e.g., surface recreated)
        // Guard: desert and crystal-less pauses (astro loop, non-Astro corruption)
        // have no crystal — only restore for PLAYING phase
        if (state.isPaused && !crystalRenderer.isActive && state.phase == GamePhase.PLAYING && pauseUsesCrystal()) {
            crystalRenderer.activatePause(screenWidth, screenHeight)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // This fires on every resume from background, on the UI thread, while the
        // render thread (restarted by surfaceCreated) is already running. The calls
        // below structurally mutate state the render thread is concurrently reading
        // (e.g. starfieldRenderer.initialize() clears `stars` and recycles its
        // bitmap; spawn/movement/camera/crewmate re-init). Hold the same monitor the
        // render loop uses (GameThread synchronizes on this SurfaceHolder) so the two
        // never touch shared state at once. Safe from deadlock: nothing here joins the
        // render thread.
        synchronized(holder) {
            applyScreenDimensions(width, height)

            // Update state with screen dimensions for weapons that need them
            state.screenWidth = screenWidth
            state.screenHeight = screenHeight

            // Reinitialize with new dimensions
            camera.initialize(screenWidth, screenHeight)
            starfieldRenderer.initialize(screenWidth, screenHeight)
            hudRenderer.initialize(layout, context.resources.configuration.smallestScreenWidthDp)
            upgradeSelectionRenderer.initialize(layout)
            debugMenuRenderer.initialize(screenWidth, screenHeight)
            spawnSystem.initialize(screenWidth, screenHeight)
            crewmateEncounter.screenWidth = screenWidth
            crewmateEncounter.screenHeight = screenHeight
            movementSystem.initialize(screenWidth, screenHeight)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameThread?.setRunning(false)
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        telemetryManager.flush()
    }

    private fun initializeGame() {
        IconCache.preload(context)

        // Reset state
        state.reset()
        wasFullyUpgraded = false
        state.bestTime = highScoreManager.getBestTime()
        state.highScore = highScoreManager.getHighScore()

        // Reset entity pools
        EntityPools.resetAll()

        // Reset explosion and effects
        shipExplosion.reset()
        enemyExplosions.clear()
        visualEffects.clear()
        fadingTrails.clear()

        // Reset boss (Entity.reset() sets isActive=true — boss must start inactive)
        boss.reset()
        boss.isActive = false
        fleetSystem.reset()

        // Reset systems
        weaponSystem.reset()
        spawnSystem.reset()
        enemySpawnSystem.reset()
        touchController.reset()

        // Initialize ship at origin (camera will center on it)
        ship.reset()
        ship.applyPermanentBonuses(state.getPermanentHealthBonus(), state.getPermanentShieldsBonus())
        ship.position.set(0f, 0f)
        // No invulnerability at game start — clean launch from hangar

        // Apply shield stats from GameState (may differ from GameConfig defaults due to upgrades)
        ship.maxShield = (GameConfig.SHIP_BASE_SHIELDS + state.getPermanentShieldsBonus()) * state.shieldCapMultiplier
        ship.currentShield = ship.maxShield
        ship.shieldRegenRate = state.shieldRegenRate
        ship.shieldRegenDelay = state.shieldRegenDelay

        // Glass Cannon: apply shield cap, disable regen, and immediately clamp current shield
        ship.shieldCap = state.maxShieldCap
        ship.shieldRegenDisabled = state.shieldRegenDisabled
        ship.currentShield = minOf(ship.currentShield, ship.shieldCap)

        // Initialize camera
        camera.initialize(screenWidth, screenHeight)
        camera.update(ship)

        // Set camera on movement system
        movementSystem.setCamera(camera)

        // Initialize renderers
        starfieldRenderer.initialize(screenWidth, screenHeight)
        hudRenderer.initialize(layout, context.resources.configuration.smallestScreenWidthDp)
        upgradeSelectionRenderer.initialize(layout)
        spawnSystem.initialize(screenWidth, screenHeight)
        movementSystem.initialize(screenWidth, screenHeight)

        // Clear drones (will be spawned based on state.hasDrone)
        combatDroneSystem.reset()

        // Clear saw state + orbiter sound cooldown
        sawDamageSystem.reset()
        beamDamageSystem.reset()

        vampiricLeecherSystem.reset()
        orbiterSoundCooldown = 0f
        bossAuraSoundCooldown = 0f
        bossReplyTimer = 0f
        bossReplySent = false
        pastAstroArrived = false
        pastAstroFireTimer = 0f
        playerRushBurn.clear()
        engineRestarting = false
        engineRestartTimer = 0f
        engineSputterToggleTimer = 0f
        engineCatchFired = false
        engineStruggleLineFired = false

        // Reset crewmate encounter (after EntityPools.resetAll — no active projectiles)
        EntityPools.projectiles.getActiveEntities(activeProjectiles)
        crewmateEncounter.reset(activeProjectiles)

        // Load permanent upgrades from PersistenceManager
        val persistence = PersistenceManager(context)
        state.permanentHealthLevel = persistence.getUpgradeLevel("health")
        state.permanentShieldsLevel = persistence.getUpgradeLevel("shields")
        state.permanentSpeedLevel = persistence.getUpgradeLevel("speed")
        state.permanentDamageLevel = persistence.getUpgradeLevel("damage")
        state.permanentCritLevel = persistence.getUpgradeLevel("crit")
        state.permanentYenBonusLevel = persistence.getUpgradeLevel("yen_bonus")
        state.permanentSalvageLevel = persistence.getUpgradeLevel("salvage")
        state.permanentMagnetLevel = persistence.getUpgradeLevel("magnet")

        // Re-apply permanent bonuses now that levels are loaded
        ship.applyPermanentBonuses(state.getPermanentHealthBonus(), state.getPermanentShieldsBonus())

        // Log run start for telemetry
        telemetryManager.logRunStart(
            ship = startingShipId,
            pilot = startingPilotId,
            storeUpgrades = mapOf(
                "health" to state.permanentHealthLevel,
                "shields" to state.permanentShieldsLevel,
                "speed" to state.permanentSpeedLevel,
                "damage" to state.permanentDamageLevel,
                "crit" to state.permanentCritLevel,
                "magnet" to state.permanentMagnetLevel,
                "yen_bonus" to state.permanentYenBonusLevel,
                "salvage" to state.permanentSalvageLevel
            )
        )

        // Apply starting weapon from selected ship
        val shipDef = ShipDefinitions.getShip(startingShipId)
        if (shipDef != null) {
            weaponSystem.addWeapon(shipDef.startingWeaponId, state)
            ship.shipColor = shipDef.color
            ship.startingWeaponId = shipDef.startingWeaponId
        }

        // Apply starting passive from selected pilot
        val pilotDef = PilotDefinitions.getPilot(startingPilotId)
        if (pilotDef != null) {
            ship.pilotColor = pilotDef.color
            state.activePilotId = startingPilotId
        }

        // Check for crystal powers early (Astro + corruption + crystal unlocked)
        // Must be before passive application so we can skip TB-26 for crystal Astro
        state.hasCrystalPowers = StoryStateManager.isCorrupted(persistence)
            && state.activePilotId == "pilot_astro"
            && persistence.isCrystalUnlocked()

        // Apply starting passive (skip for crystal Astro — TB-26 is gone)
        if (pilotDef != null && !state.hasCrystalPowers) {
            state.addPassive(pilotDef.startingPassiveId)
            // Sync shield caps to ship after passive applied — glass_cannon sets maxShieldCap=0
            // but ship.shieldCap/currentShield were already set above before the passive ran
            ship.shieldCap = state.maxShieldCap
            ship.shieldRegenDisabled = state.shieldRegenDisabled
            ship.currentShield = minOf(ship.currentShield, ship.shieldCap)
        }
        // Ensure no drone for crystal Astro
        if (state.hasCrystalPowers) {
            state.hasDrone = false
        }

        // Crystal powers: max all weapons and passives at start
        if (state.hasCrystalPowers) {
            for (key in state.weaponLevels.keys) {
                state.weaponLevels[key] = GameConfig.WEAPON_MAX_LEVEL
            }
            for (key in state.passiveStacks.keys) {
                state.passiveStacks[key] = GameConfig.PASSIVE_MAX_STACKS
            }
            // Re-sync weapon system with maxed levels
            weaponSystem.reset()
            for ((weaponId, _) in state.weaponLevels) {
                weaponSystem.addWeapon(weaponId, state)
            }
        }

        // Recalculate stats after applying everything
        state.recalculateStats()

        // Set up weapon/passive gating
        val unlockedWeapons = persistence.getUnlockedWeaponIds()
        val unlockedPassives = persistence.getUnlockedPassiveIds()
        upgradeSystem.unlockedWeaponIds = unlockedWeapons
        upgradeSystem.unlockedPassiveIds = unlockedPassives
        lootSystem.unlockedWeaponIds = unlockedWeapons
        lootSystem.unlockedPassiveIds = unlockedPassives
        enemySpawnSystem.unlockedWeaponIds = unlockedWeapons
        enemySpawnSystem.unlockedPassiveIds = unlockedPassives

        // Discover starting weapon
        if (shipDef != null) {
            persistence.discoverWeapon(shipDef.startingWeaponId)
        }

        // Reset kill streak tracking
        currentKillStreak = 0
        bestKillStreakThisRun = 0
        lastKillTime = 0f
        enemiesKilledThisRun = 0
        continuousFlightTimer = 0f
        continuousFlightPauseTimer = 0f
        bestContinuousFlightThisRun = 0

        // Load story loop (used for boss chatter and heart-to-heart script)
        state.storyLoop = persistence.getStoryLoop()
        state.astroLoopMode = StoryStateManager.isAstroLoop(persistence)
        state.activePilotHasBandana = persistence.hasBandana(state.activePilotId)

        // Cache corruption run flags (avoids per-frame PersistenceManager allocation)
        state.isCorruptionRun = StoryStateManager.isCorrupted(persistence)
        isNonAstroCorruptionRun = state.isCorruptionRun && startingPilotId != "pilot_astro"
        isAstroCorruptionRun = state.hasCrystalPowers
        cachedStoryPhase = persistence.getStoryStageCode()

        if (isAstroCorruptionRun) {
            val indices = (0 until StoryStateManager.CREWMATE_ENCOUNTER_ORDER.size).shuffled().take(5)
            state.corruptionSelectedCrewmates = indices
            state.corruptionNextEncounterTime = CORRUPTION_ENCOUNTER_TIMES[0]
            state.corruptionChatterTimer = CORRUPTION_CHATTER_INTERVAL
        }

        state.isPostHorrorRun = persistence.isDesertCompleted() && !persistence.hasDesertGoodEnding()

        // Clear any boss/taunt left over from a previous run's death retreat — the boss is
        // deliberately kept alive through the retreat so it never blinks out mid-scene.
        reckoningBoss = null
        pendingReckoningLine = null

        // Enter the Crystal Reckoning instead of a normal run when all conditions are met
        if (CrystalReckoning.shouldEnter(state.astroLoopMode, startingPilotId, persistence.getBandanaCount(), persistence.isCrystalReleased())) {
            state.phase = GamePhase.PLAYING
            startReckoningOpening()
            return
        }

        // Start playing immediately with starting loadout
        state.phase = GamePhase.PLAYING
        radioSystem.onCombatStart(state)
    }

    fun update(deltaTime: Float) {
        // HUD fade on pause/death (runs before early returns so it animates during pause)
        val hudFadeTarget = if (state.isPaused || state.phase == GamePhase.DEATH_PLAY_OUT || state.phase == GamePhase.CRYSTAL_DEATH || state.phase == GamePhase.GAME_OVER || state.phase == GamePhase.DESERT_FAREWELL || state.phase == GamePhase.TIMELINE_SHIFT) 0f else 1f
        if (state.hudFadeAlpha != hudFadeTarget) {
            val fadeSpeed = deltaTime / 0.5f
            state.hudFadeAlpha = if (hudFadeTarget < state.hudFadeAlpha) {
                (state.hudFadeAlpha - fadeSpeed).coerceAtLeast(0f)
            } else {
                (state.hudFadeAlpha + fadeSpeed).coerceAtMost(1f)
            }
        }

        // Handle paused state first
        if (state.isPaused) {
            updatePaused(deltaTime)
            return
        }

        // Freeze game while debug menu is open
        if (state.debugMenuOpen) {
            // Detect page changes for debug BGM mute/unmute
            if (state.debugMenuPage != lastDebugMenuPage) {
                if (state.debugMenuPage == 4) {
                    SoundManager.muteBGMForDebug()
                }
                if (lastDebugMenuPage == 4) {
                    SoundManager.unmuteBGMForDebug()
                    SoundManager.stopDebugBGM()
                    SoundManager.clearTuningAutoPlay()
                    debugMenuRenderer.tuningBGMPlaying = false
                }
                lastDebugMenuPage = state.debugMenuPage
            }
            // Tuning per-frame updates when on page 4
            if (state.debugMenuPage == 4) {
                SoundManager.updateTuningAutoPlay()
                debugMenuRenderer.updateTuningScroll()
            }
            // Flak design preview animation
            if (state.debugMenuPage == 3) {
                debugMenuRenderer.debugFlakAge = (debugMenuRenderer.debugFlakAge + deltaTime) % 1.0f
            }
            return
        }

        // Clean up tuning when debug menu just closed
        if (lastDebugMenuPage >= 0) {
            if (lastDebugMenuPage == 4) {
                SoundManager.unmuteBGMForDebug()
                SoundManager.stopDebugBGM()
                SoundManager.clearTuningAutoPlay()
                debugMenuRenderer.tuningBGMPlaying = false
            }
            lastDebugMenuPage = -1
        }

        // Update global fade overlay (used for scene transition fade-ins)
        if (globalFadeFading && globalFadeAlpha > 0f) {
            globalFadeTimer += deltaTime
            globalFadeAlpha = (1f - globalFadeTimer / 0.8f).coerceAtLeast(0f)
            if (globalFadeAlpha <= 0f) globalFadeFading = false
        }

        when (state.phase) {
            GamePhase.PLAYING -> updatePlaying(deltaTime)
            GamePhase.UPGRADE_SELECTION -> updateUpgradeSelection(deltaTime)
            GamePhase.DEATH_PLAY_OUT -> updateDeathPlayOut(deltaTime)
            GamePhase.CRYSTAL_DEATH -> updateCrystalDeath(deltaTime)
            GamePhase.HEART_TO_HEART -> updateHeartToHeart(deltaTime)
            GamePhase.DESERT -> updateDesert(deltaTime)
            GamePhase.DESERT_FAREWELL -> updateDesertFarewell(deltaTime)
            GamePhase.TIMELINE_SHIFT -> updateTimelineShift(deltaTime)
            GamePhase.WAKE_UP -> updateWakeUp(deltaTime)
            GamePhase.GAME_OVER -> {
                // Corruption death: wait for explosion then return to hangar
                if (corruptionDeathTimer > 0f) {
                    corruptionDeathTimer -= deltaTime
                    if (shipExplosion.isActive) shipExplosion.update(deltaTime)
                    enemyExplosions.forEach { it.update(deltaTime) }
                    enemyExplosions.removeAll { !it.isActive }
                    if (corruptionDeathTimer <= 0f) {
                        onGameOver(state.goldCollected, false)
                    }
                }
            }
            GamePhase.GAME_BRICKED -> {
                if (brickScreenTimer < 10f) brickScreenTimer += deltaTime
                if (brickStatsRevealed) brickStatsTimer = (brickStatsTimer + deltaTime).coerceAtMost(2f)
                if (!brickSoundPlayed && brickScreenTimer >= 1f) {
                    brickSoundPlayed = true
                    SoundManager.playSFX("sfx_crystal_activate")
                }
            }
        }
    }

    private fun updatePlaying(deltaTime: Float) {
        // Crystal reckoning opening — empty field + Astro lines before the fight
        if (state.reckoningActive && state.reckoningStage == GameState.ReckoningStage.OPENING) {
            updateReckoningOpening(deltaTime); return
        }
        // Crystal reckoning fight — runs instead of normal play
        if (state.reckoningActive && state.reckoningStage == GameState.ReckoningStage.FIGHT) {
            updateReckoningFight(deltaTime); return
        }
        // Ghost-ship lance climax — runs instead of normal play
        if (state.reckoningActive && state.reckoningStage == GameState.ReckoningStage.LANCE) {
            updateReckoningLance(deltaTime); return
        }

        // Update difficulty
        difficultySystem.update(deltaTime, state)

        // Tick down sound cooldowns
        if (asteroidHitSoundCooldown > 0f) asteroidHitSoundCooldown -= deltaTime
        if (bossAuraSoundCooldown > 0f) bossAuraSoundCooldown -= deltaTime

        // Boss reply — corrupted Astro responds "..." 3s after pilot's boss_spawn line
        if (bossReplyTimer > 0f && !bossReplySent) {
            bossReplyTimer -= deltaTime
            if (bossReplyTimer <= 0f) {
                bossReplySent = true
                val astroCallsign = PilotDefinitions.getPilot("pilot_astro")?.callsign ?: "ASTRO"
                radioSystem.showScriptedMessage(state, astroCallsign, "...", Boss.CORRUPTION_COLOR, isBoss = true)
            }
        }

        // Post-reckoning death: the stunned crystal boss keeps tracking the fleeing ship
        // (rotation-only — Boss.update aims before its stun early-return), and the queued
        // "I'll be waiting." taunt still needs its delay tick. No-op outside that scenario:
        // reckoningBoss is null everywhere else this path runs.
        if (!state.reckoningActive && reckoningBoss != null) {
            reckoningBoss?.update(deltaTime)
            tickPendingReckoningLine(deltaTime)
        }

        // Astro Loop retreat state machine
        if (state.retreatPhase > 0) {
            state.retreatTimer += deltaTime
            when (state.retreatPhase) {
                1 -> {
                    if (state.retreatTimer >= 1.5f) {
                        state.retreatPhase = 2
                        state.retreatTimer = 0f
                    }
                }
                2 -> {
                    // Head south, weaving around asteroids using normal steering:
                    // point toward the desired heading and thrust forward.
                    val obstacles = EntityPools.asteroids.getActiveEntities().map {
                        com.astroloop.game.util.RetreatSteering.Obstacle(it.position.x, it.position.y, it.radius)
                    }
                    val targetHeading = com.astroloop.game.util.RetreatSteering.desiredHeading(
                        ship.position.x, ship.position.y, ship.radius, obstacles
                    )
                    var rotDiff = targetHeading - ship.rotation
                    while (rotDiff < -Math.PI.toFloat()) rotDiff += (2 * Math.PI).toFloat()
                    while (rotDiff > Math.PI.toFloat()) rotDiff -= (2 * Math.PI).toFloat()
                    ship.rotation += rotDiff * (4f * deltaTime).coerceAtMost(1f)

                    val speed = 300f
                    ship.position.x += cos(ship.rotation) * speed * deltaTime
                    ship.position.y += sin(ship.rotation) * speed * deltaTime

                    val screenBottom = camera.y + screenHeight + 100f
                    if (ship.position.y > screenBottom) {
                        state.retreatPhase = 3
                        state.retreatTimer = 0f
                        state.emergencyShieldActive = false
                    }
                }
                3 -> {
                    if (state.retreatTimer >= 1.5f) {
                        saveRunStats(includeDeath = false)
                        onGameOver(state.goldCollected, false)
                        return
                    }
                }
            }
        }

        // Telemetry: 60-second snapshots
        state.telemetrySnapshotTimer += deltaTime
        if (state.telemetrySnapshotTimer >= 60f) {
            state.telemetrySnapshotTimer -= 60f
            telemetryManager.logSnapshot(
                state = state,
                damageByWeapon = state.telemetryDamageByWeapon.toMap(),
                damageTakenBy = state.telemetryDamageTakenBy.toMap(),
                critsThisMinute = state.telemetryCritsThisMinute,
                powerupsCollected = state.telemetryPowerupsCollected.toMap(),
                dodges = state.telemetryDodges
            )
            state.resetTelemetryMinuteCounters()
        }

        // Corruption Astro time speed-up: clock ticks 5x after 10 real seconds
        if (state.hasCrystalPowers && !state.corruptionSpeedUpTriggered && state.survivalTime >= 10f) {
            state.corruptionSpeedUpTriggered = true
            state.corruptionTimeMultiplier = 5f
            radioSystem.showScriptedMessage(state, "ASTRO", "Let's speed things up.",
                PilotDefinitions.getPilot("pilot_astro")!!.color, isBoss = true)
        }
        // Slow back to 1x at the 10-minute mark so the encounter plays at normal pace
        if (state.hasCrystalPowers && state.corruptionTimeMultiplier > 1f && state.survivalTime >= Boss.SPAWN_TIME) {
            state.corruptionTimeMultiplier = 1f
        }

        // Radio: density spike trigger
        val currentDiffLevel = state.difficultyMultiplier.toInt()
        if (currentDiffLevel > state.lastDensitySpikeLevel && currentDiffLevel >= 2) {
            state.lastDensitySpikeLevel = currentDiffLevel
            radioSystem.onDensitySpike(state)
        }

        // Time milestone radio triggers
        val milestoneMinutes = floatArrayOf(2f, 4f, 6f, 8f, 10f, 12f, 14f, 16f)
        for (i in milestoneMinutes.indices) {
            if (state.timeMilestonesFired and (1 shl i) != 0) continue
            val targetSeconds = milestoneMinutes[i] * 60f + state.timeMilestoneOffsets[i]
            if (state.survivalTime >= targetSeconds) {
                state.timeMilestonesFired = state.timeMilestonesFired or (1 shl i)
                radioSystem.onTimeMilestone(state, i)
            }
        }

        // Kill streak timeout (3-second window between kills)
        if (currentKillStreak > 0 && state.survivalTime - lastKillTime > 3f) {
            currentKillStreak = 0
        }

        // Grace period — decrement timer
        if (state.graceTimer > 0f) {
            state.graceTimer -= deltaTime
        }

        // Update fleet system BEFORE auto-pilot so playerRingPosition is fresh this frame
        fleetSystem.update(state, deltaTime)
        updateEngineRestart(deltaTime)

        // Auto-pilot during fleet formation cutscene (starts at FLEET_CHATTER when fleet warps in)
        val inFleetAutopilot = state.bossFightPhase == PHASE_FLEET_CHATTER || state.bossFightPhase == PHASE_FORMATION || state.bossFightPhase == PHASE_SHIELD_ASSAULT || state.bossFightPhase == PHASE_TB26_RAM || state.bossFightPhase == PHASE_POST_VICTORY

        // Update ship movement from touch input (skip when stunned, in fleet autopilot, or retreating)
        if (!state.playerStunned && !inFleetAutopilot && state.retreatPhase < 2 &&
                state.corruptionRushPhase == 0) {
            // Suppress touch input through the whole engine-restart beat — incl. the brief
            // post-catch window before autopilot takes over — so the ship holds until it eases out.
            if (!state.bossEmpFired && !engineRestarting) {
                ship.moveDirection.set(touchController.moveDirection)
                ship.moveDirection.mul(touchController.moveMagnitude)
            } else {
                ship.moveDirection.zero()
            }
            movementSystem.updateShip(ship, state, deltaTime)
        }

        if (inFleetAutopilot) {
            val flyingToCrystal = state.bossFightPhase == PHASE_POST_VICTORY &&
                state.timeCrystalPhase == GameState.TimeCrystalPhase.FLYING
            if (!flyingToCrystal) {
                if (fleetSystem.playerEmpFrozen) {
                    ship.moveDirection.zero()
                    movementSystem.updateShip(ship, state, deltaTime)
                } else {
                    val dx = fleetSystem.playerRingPosition.x - ship.position.x
                    val dy = fleetSystem.playerRingPosition.y - ship.position.y
                    val dist = sqrt(dx * dx + dy * dy)
                    // Guard against dist == 0: dx/dist would be NaN even though scale==0f (NaN*0f=NaN).
                    if (dist > 0.5f) {
                        // Smooth speed scaling: ramps down naturally as ship nears ring position (no oscillation)
                        val avoidMargin = 80f        // extra clearance around boss radius for avoidance path
                        val avoidClearance = 20f     // waypoint offset past the avoidance circle
                        val decelDistance = 80f      // distance ramp for speed scaling (used in scale and wscale)
                        val scale = (dist / decelDistance).coerceIn(0f, 1f)

                        // Boss avoidance: if straight-line path passes too close to boss, steer around it
                        var avoidanceHandled = false
                        if (boss.isActive) {
                            val avoidRadius = boss.radius + avoidMargin
                            val toTargetNx = dx / dist
                            val toTargetNy = dy / dist
                            val toBossX = boss.position.x - ship.position.x
                            val toBossY = boss.position.y - ship.position.y
                            val proj = (toBossX * toTargetNx + toBossY * toTargetNy).coerceIn(0f, dist)
                            val closestX = ship.position.x + toTargetNx * proj
                            val closestY = ship.position.y + toTargetNy * proj
                            val cdx = closestX - boss.position.x
                            val cdy = closestY - boss.position.y
                            val closestDist = sqrt(cdx * cdx + cdy * cdy)
                            if (closestDist < avoidRadius && proj > 10f) {
                                val perpX = -toTargetNy
                                val perpY = toTargetNx
                                val bossRelX = boss.position.x - closestX
                                val bossRelY = boss.position.y - closestY
                                val perpSign = if (perpX * bossRelX + perpY * bossRelY > 0f) -1f else 1f
                                val waypointX = closestX + perpX * perpSign * (avoidRadius + avoidClearance)
                                val waypointY = closestY + perpY * perpSign * (avoidRadius + avoidClearance)
                                val wdx = waypointX - ship.position.x
                                val wdy = waypointY - ship.position.y
                                val wdist = sqrt(wdx * wdx + wdy * wdy)
                                if (wdist > 0.5f) {
                                    val wscale = (wdist / decelDistance).coerceIn(0f, 1f)
                                    val launch = if (engineRestarting) FleetSystem.engineRestartSpeedScale(engineRestartTimer) else 1f
                                    ship.moveDirection.set(wdx / wdist * wscale * launch, wdy / wdist * wscale * launch)
                                    movementSystem.updateShip(ship, state, deltaTime)
                                    avoidanceHandled = true
                                }
                            }
                        }

                        if (!avoidanceHandled) {
                            val launch = if (engineRestarting) FleetSystem.engineRestartSpeedScale(engineRestartTimer) else 1f
                            ship.moveDirection.set(dx / dist * scale * launch, dy / dist * scale * launch)
                            movementSystem.updateShip(ship, state, deltaTime)
                        }
                    } else {
                        ship.moveDirection.zero()
                    }
                    // Astro faces boss during fleet fight (not during POST_VICTORY — handled separately for crystal pickup)
                    // fleetAutopilotTurnRate (15f) must exceed Ship.update()'s lerpAngle (t=8*dt≈0.067/frame)
                    // so this direct rotation assignment dominates the per-frame lerp.
                    if (state.bossFightPhase != PHASE_POST_VICTORY) {
                        val fleetAutopilotTurnRate = 15f
                        val desiredAngle = atan2(
                            boss.position.y - ship.position.y,
                            boss.position.x - ship.position.x
                        )
                        val angleDiff = normalizeAngle(desiredAngle - ship.rotation)
                        ship.rotation += angleDiff.coerceIn(-fleetAutopilotTurnRate * deltaTime, fleetAutopilotTurnRate * deltaTime)
                    }
                }
            }
        }
        // Corruption rush autopilot: corrupted Astro burns toward Past Astro
        // (mirror of the boss's rush in the normal run). The arrival bundle fires
        // from the PHASE_OTHER_SURVIVAL step-6 handler once the stillness beat elapses.
        if (state.corruptionRushPhase > 0) {
            state.corruptionRushTimer += deltaTime
            val rushTarget = crewmateEncounter.crewmateShip
            when (state.corruptionRushPhase) {
                1 -> {
                    if (rushTarget != null && rushTarget.isActive) {
                        val dx = rushTarget.position.x - ship.position.x
                        val dy = rushTarget.position.y - ship.position.y
                        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                        val spd = state.corruptionRushSpeed * BossRush.easeIn(state.corruptionRushTimer)
                        ship.velocity.set(dx / d * spd, dy / d * spd)
                        ship.position.x += ship.velocity.x * deltaTime
                        ship.position.y += ship.velocity.y * deltaTime
                        val desired = atan2(dy, dx)
                        val diff = normalizeAngle(desired - ship.rotation)
                        ship.rotation += diff.coerceIn(-15f * deltaTime, 15f * deltaTime)
                        if (BossRush.hasArrived(d)) {
                            visualEffects.addHitFlash(ship.position.x, ship.position.y, 40f, 0xFFFFCC88.toInt())
                            state.corruptionRushPhase = 2
                            state.corruptionRushTimer = 0f
                        }
                    } else {
                        // Target gone (shouldn't happen — he's invulnerable): skip to the beat
                        state.corruptionRushPhase = 3
                        state.corruptionRushTimer = 0f
                    }
                }
                2 -> {
                    ship.velocity.x *= (1f - 12f * deltaTime).coerceAtLeast(0f)
                    ship.velocity.y *= (1f - 12f * deltaTime).coerceAtLeast(0f)
                    ship.position.x += ship.velocity.x * deltaTime
                    ship.position.y += ship.velocity.y * deltaTime
                    if (state.corruptionRushTimer >= GameConfig.BOSS_RUSH_BRAKE_DURATION) {
                        ship.velocity.zero()
                        state.corruptionRushPhase = 3
                        state.corruptionRushTimer = 0f
                    }
                }
                // 3: stillness — held until the step-6 handler fires the arrival bundle
            }
        }
        // Player-side burn keeps aging after the hand-off (no instant disappearance)
        if (state.corruptionRushPhase > 0 || playerRushBurn.hasContent()) {
            playerRushBurn.update(ship.position.x, ship.position.y, ship.rotation,
                deltaTime, emitting = state.corruptionRushPhase == 1)
        }
        // Crystal autopilot: when FLYING phase, Astro hesitates then collects crystal
        if (inFleetAutopilot && state.bossFightPhase == PHASE_POST_VICTORY &&
                state.timeCrystalPhase == GameState.TimeCrystalPhase.FLYING) {
            val cx = state.timeCrystalX
            val cy = state.timeCrystalY
            val cdx = cx - ship.position.x
            val cdy = cy - ship.position.y
            val cdist = sqrt(cdx * cdx + cdy * cdy)
            val crystalTurnRate = 2.5f

            when (state.crystalApproachPhase) {
                0 -> {
                    // Slow approach at 30% speed
                    if (cdist <= 80f) {
                        // Transition to uncertainty beat
                        state.crystalApproachPhase = 1
                        state.crystalHesitationTimer = 0f
                        radioSystem.showScriptedMessage(state, "ASTRO", "...",
                            PilotDefinitions.getPilot("pilot_astro")!!.color)
                        ship.moveDirection.zero()
                        ship.velocity.zero()  // stop drift during hesitation beat
                    } else if (cdist > 0.5f) {
                        ship.moveDirection.set(cdx / cdist * 0.3f, cdy / cdist * 0.3f)
                        movementSystem.updateShip(ship, state, deltaTime)
                        val desiredAngle = atan2(cdy, cdx)
                        val angleDiff = normalizeAngle(desiredAngle - ship.rotation)
                        ship.rotation += angleDiff.coerceIn(-crystalTurnRate * deltaTime, crystalTurnRate * deltaTime)
                    }
                }
                1 -> {
                    // Uncertainty beat — stopped for 1.5s
                    state.crystalHesitationTimer += deltaTime
                    ship.moveDirection.zero()
                    movementSystem.updateShip(ship, state, deltaTime)
                    if (state.crystalHesitationTimer >= 1.5f) {
                        state.crystalApproachPhase = 2
                    }
                }
                2 -> {
                    // Final approach at 30% speed
                    if (cdist > 30f && cdist > 0.5f) {
                        ship.moveDirection.set(cdx / cdist * 0.3f, cdy / cdist * 0.3f)
                        movementSystem.updateShip(ship, state, deltaTime)
                        val desiredAngle = atan2(cdy, cdx)
                        val angleDiff = normalizeAngle(desiredAngle - ship.rotation)
                        ship.rotation += angleDiff.coerceIn(-crystalTurnRate * deltaTime, crystalTurnRate * deltaTime)
                    } else {
                        ship.moveDirection.zero()
                        state.timeCrystalPhase = GameState.TimeCrystalPhase.COLLECTED
                        state.timeCrystalTimer = 0f
                    }
                }
            }
        }

        // Past Astro auto-pilot during corruption fleet sequence (mirrors player auto-pilot in normal run)
        val inCorruptionFleet = state.bossFightPhase >= PHASE_OTHER_FLEET
        if (inCorruptionFleet) {
            val pastAstro = crewmateEncounter.crewmateShip
            if (pastAstro != null && pastAstro.isActive) {
                val dx = fleetSystem.playerRingPosition.x - pastAstro.position.x
                val dy = fleetSystem.playerRingPosition.y - pastAstro.position.y
                val dist = sqrt(dx * dx + dy * dy)
                val turnRate = 2.5f
                if (!pastAstroArrived && dist > 8f) {
                    val launch = if (engineRestarting) FleetSystem.engineRestartSpeedScale(engineRestartTimer) else 1f
                    val spd = pastAstro.getEffectiveSpeed() * launch
                    pastAstro.velocity.set(dx / dist * spd, dy / dist * spd)
                    pastAstro.position.x += pastAstro.velocity.x * deltaTime
                    pastAstro.position.y += pastAstro.velocity.y * deltaTime
                    val desiredAngle = atan2(dy, dx)
                    val angleDiff = normalizeAngle(desiredAngle - pastAstro.rotation)
                    pastAstro.rotation += angleDiff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
                } else {
                    // Arrived — snap to ring position every frame (eliminates jitter)
                    if (!pastAstroArrived) {
                        pastAstroArrived = true
                        pastAstroFireTimer = 2f  // 2s delay before first shot
                    }
                    val playerDir = if (fleetSystem.playerRing == 0) 1f else -1f
                    val orbitAngle = fleetSystem.arrivalTimer * FleetSystem.ORBIT_SPEED * playerDir
                    val orbitRadius = if (fleetSystem.playerRing == 0) FleetSystem.OUTER_RADIUS else FleetSystem.INNER_RADIUS
                    if (!state.bossEmpFired) {
                        // Orbit-locked: snap to the ring slot + tangential velocity for thrusters.
                        pastAstro.position.set(fleetSystem.playerRingPosition.x, fleetSystem.playerRingPosition.y)
                        val tangentSpeed = (FleetSystem.ORBIT_SPEED * orbitRadius).coerceAtLeast(80f)
                        pastAstro.velocity.set(
                            -sin(orbitAngle) * playerDir * tangentSpeed,
                            cos(orbitAngle) * playerDir * tangentSpeed
                        )
                    } else {
                        // EMP #2 hit: drift free of the ring with decaying scatter velocity
                        // (mirrors the fleet's empDisorientTimer ships).
                        pastAstro.position.x += pastAstro.velocity.x * deltaTime
                        pastAstro.position.y += pastAstro.velocity.y * deltaTime
                        pastAstro.velocity.x *= (1f - 0.8f * deltaTime).coerceAtLeast(0f)
                        pastAstro.velocity.y *= (1f - 0.8f * deltaTime).coerceAtLeast(0f)
                    }
                    val desiredAngle = atan2(
                        boss.position.y - pastAstro.position.y,
                        boss.position.x - pastAstro.position.x
                    )
                    val angleDiff = normalizeAngle(desiredAngle - pastAstro.rotation)
                    if (!state.bossEmpFired) {
                        // After EMP #2 the scatter tumble holds; stop re-aiming at the boss.
                        pastAstro.rotation += angleDiff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
                    }

                    if (state.bossFightPhase == PHASE_OTHER_SHIELD_ASSAULT && !state.bossEmpFired) {
                        val aimDiff = abs(angleDiff)
                        if (aimDiff < 0.3f) {
                            pastAstroFireTimer -= deltaTime
                            if (pastAstroFireTimer <= 0f) {
                                pastAstroFireTimer = 2.5f
                                val fdx = boss.position.x - pastAstro.position.x
                                val fdy = boss.position.y - pastAstro.position.y
                                val fdist = sqrt(fdx * fdx + fdy * fdy).coerceAtLeast(1f)
                                val dirX = fdx / fdist
                                val dirY = fdy / fdist
                                val spawnX = pastAstro.position.x + dirX * 25f
                                val spawnY = pastAstro.position.y + dirY * 25f
                                val p = EntityPools.projectiles.obtain()
                                // 0 damage: boss defeat is scripted, not health-depleted — mirrors fireAtBoss() railgun
                                p.initialize(spawnX, spawnY, dirX * 2000f, dirY * 2000f,
                                    ProjectileType.BULLET, 0f, 3f)
                                p.isEnemyProjectile = false
                                p.color = 0xFFFFFFFF.toInt()  // Specter white
                                p.length = 20f
                                p.width = 3f
                                p.piercing = true
                                p.maxPierces = 2
                                SoundManager.playSFX("sfx_weapon_railgun",
                                    SoundManager.getWeaponSfxVolume("railgun"))
                            }
                        }
                    }
                }
            }
        }

        // Update camera to follow ship (freeze during retreat or while stunned).
        // Reckoning death exception: once the crystal boss is on the field (reckoningBoss != null,
        // i.e. the post-death retreat), freeze the camera IMMEDIATELY rather than at retreatPhase 2.
        // The boss's centre-glide target is captured from this frame's camera; letting the camera
        // keep following the coasting ship through phase 1 would leave the boss planted off-centre.
        if (!state.playerStunned && state.retreatPhase < 2 && reckoningBoss == null) {
            camera.update(ship)
        }

        // Update starfield parallax based on camera movement
        starfieldRenderer.updateWithCamera(camera)

        // Non-Astro corruption run — spawn healthy self encounter at ~60s
        if (isNonAstroCorruptionRun) {
            if (state.survivalTime >= 60f && !crewmateEncounter.crewmateActive) {
                crewmateEncounter.spawnCrewmateForAstro(state, startingPilotId, startingShipId, EntityPools.enemies, extraWeaponCount = 3)
                // Invincible — this crewmate can't be killed, only fled
                crewmateEncounter.crewmateShip?.let { it.health = 99999f; it.maxHealth = 99999f }
                // Mirror of the normal-run first-enemy call: to this crewmate, the
                // corrupted player IS the enemy ship. They aren't corrupted — no
                // CorruptedCrewDefinitions voice here.
                val pilotDef = PilotDefinitions.getPilot(startingPilotId)
                val encounterLine = RadioDefinitions.getLine(startingPilotId, "first_enemy")
                if (pilotDef != null && encounterLine != null) {
                    radioSystem.showScriptedMessage(state, pilotDef.callsign.uppercase(),
                        encounterLine, pilotDef.color)
                }
            }
            crewmateEncounter.update(state, deltaTime)
        }

        // Astro corruption run — crewmate encounters one by one
        if (isAstroCorruptionRun) {
            updateAstroCorruptionRun(deltaTime)
        }

        val isCorruptionRun = isNonAstroCorruptionRun || isAstroCorruptionRun

        // Get all active entities — populated once here, reused by all systems this frame
        EntityPools.asteroids.getActiveEntities(activeAsteroids)
        EntityPools.projectiles.getActiveEntities(activeProjectiles)
        EntityPools.powerUps.getActiveEntities(activePowerUps)
        EntityPools.enemies.getActiveEntities(activeEnemies)
        val asteroids = activeAsteroids
        val projectiles = activeProjectiles
        val powerUps = activePowerUps
        val enemies = activeEnemies

        // Phase 4 boss spawn — all pilots at boss spawn time
        // (skip during corruption runs — boss triggered after crewmate encounters)
        // (skip in astro-loop mode — no boss, no story, just survival)
        if (state.survivalTime >= Boss.SPAWN_TIME && !state.bossSpawned && !isCorruptionRun && !state.astroLoopMode) {
            val isAstro = state.activePilotId == "pilot_astro"
            bossSystem.spawnBoss(startingShipId, startingPilotId, asteroids, enemies, powerUps)
            state.bossFightTimer = 0f
            SoundManager.startBossBGM(context)
            if (cachedStoryPhase == StoryStage.NORMAL.code && isAstro) {
                // Astro gets the full story sequence
                radioSystem.onBossSpawn(state)
                state.bossFightPhase = PHASE_SURVIVAL
                bossReplyTimer = 3.0f
                bossReplySent = false
            } else {
                // Non-Astro: scripted charge-death (no rescue) — enter the solo survival path
                // so the +40s charge/EMP sequence runs and kills the player.
                // bossReplyTimer is intentionally NOT set: there's no pilot boss_spawn line to
                // reply to, and the "ASTRO ..." silent-boss reply would spoil the boss-is-Astro
                // reveal for a non-Astro pilot in PHASE_NORMAL.
                state.bossFightPhase = PHASE_WAITING_FLEET
            }
        }

        // Spawn asteroids relative to camera (only if boss is not active, grace period over, not during retreat)
        val newAsteroids = if (!state.bossActive && state.graceTimer <= 0f && state.retreatPhase == 0) {
            spawnSystem.update(deltaTime, state, ship, camera)
        } else {
            emptyList()
        }

        // Spawn enemy ships (only if boss is not active, grace period over, not corruption run, not retreating)
        val newEnemies = if (!state.bossActive && state.graceTimer <= 0f && !isCorruptionRun && state.retreatPhase == 0) {
            enemySpawnSystem.update(deltaTime, state, ship, camera, activeEnemies)
        } else {
            emptyList()
        }

        if (newEnemies.isNotEmpty()) {
            if (!state.firstEnemySpawned) {
                state.firstEnemySpawned = true
                radioSystem.onFirstEnemy(state)
            }
            radioSystem.onEnemySpawn(state)  // Corrupted crew chance
        }

        // Phoenix shockwave — kill sweep: destroy everything in the swept ring annulus each frame
        if (state.phoenixShockwaveActive) {
            val speed = GameConfig.PHOENIX_SHOCKWAVE_MAX_RADIUS / GameConfig.PHOENIX_SHOCKWAVE_DURATION
            state.phoenixShockwaveRadius += speed * deltaTime
            val prevR = state.phoenixShockwavePrevRadius
            val currR = state.phoenixShockwaveRadius
            val ox = state.phoenixShockwaveOriginX
            val oy = state.phoenixShockwaveOriginY

            for (enemy in enemies) {
                if (!enemy.isActive || enemy.isCrewmate) continue
                val dx = enemy.position.x - ox
                val dy = enemy.position.y - oy
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist > prevR && dist <= currR) {
                    onEnemyDestroyed(enemy)
                    enemy.isActive = false
                    trackEnemyKill()
                }
            }

            for (asteroid in asteroids) {
                if (!asteroid.isActive) continue
                val dx = asteroid.position.x - ox
                val dy = asteroid.position.y - oy
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist > prevR && dist <= currR) {
                    handleAsteroidDestroyedWithTrail(asteroid)
                    asteroid.isActive = false
                }
            }

            state.phoenixShockwavePrevRadius = currR
            if (currR >= GameConfig.PHOENIX_SHOCKWAVE_MAX_RADIUS) {
                state.phoenixShockwaveActive = false
                state.phoenixShockwaveRadius = 0f
                state.phoenixShockwavePrevRadius = 0f
            }
        }

        // Update weapons (auto-fire) - target both asteroids, enemies, and boss
        // Skip boss targeting during corruption finale — boss is at player position
        allTargetsCache.clear()
        allTargetsCache.addAll(asteroids)
        allTargetsCache.addAll(enemies)
        if (state.bossActive && boss.isActive && state.bossFightPhase < PHASE_OTHER_SPAWN) {
            allTargetsCache.add(boss)
        }
        val allTargets = allTargetsCache
        if (!state.weaponsDisabled && !state.playerStunned && state.bossFightPhase != PHASE_POST_VICTORY &&
                state.bossFightPhase < PHASE_OTHER_FLEET && !state.bossEmpFired &&
                state.retreatPhase < 2 && state.corruptionRushPhase == 0) {
            weaponSystem.update(deltaTime, ship, state, allTargets, asteroids)
        } else if (state.retreatPhase >= 2) {
            // Keep orbiters following the ship during fly-home
            weaponSystem.updateOrbitersOnly(ship.position, deltaTime)
        }

        // 1. Process delayed zone effects
        if (state.pendingDelayedEffects.isNotEmpty()) {
            val iter = state.pendingDelayedEffects.iterator()
            while (iter.hasNext()) {
                val effect = iter.next()
                effect.timer -= deltaTime
                if (effect.timer <= 0f) {
                    spawnDamageZone(effect.x, effect.y, effect.radius, effect.damage, effect.duration, effect.color)
                    iter.remove()
                }
            }
        }

        // 2. Tick trail-emitting projectiles
        for (p in projectiles) {
            if (!p.isActive || !p.leavesTrail) continue
            p.trailTimer += deltaTime
            if (p.trailTimer >= p.trailInterval) {
                p.trailTimer = 0f
                spawnDamageZone(p.position.x, p.position.y, p.trailRadius, p.trailDamage, p.trailDuration, p.color)
            }
        }

        // 3. Tick down weaponCooldowns
        val cdIter = state.weaponCooldowns.iterator()
        while (cdIter.hasNext()) {
            val entry = cdIter.next()
            val newVal = entry.value - deltaTime
            if (newVal <= 0f) cdIter.remove() else entry.setValue(newVal)
        }

        // 3.6. Flak cannon proximity fuse: detonate when within 40f of an enemy or asteroid
        for (p in projectiles) {
            if (!p.isActive || !p.proximityFuse || p.isEnemyProjectile) continue
            var detonated = false
            for (enemy in enemies) {
                if (!enemy.isActive || enemy.isWarping) continue
                if (!enemy.isCrewmate && !isOnScreen(enemy)) continue
                val dx = p.position.x - enemy.position.x
                val dy = p.position.y - enemy.position.y
                val enemyFuseThreshold = 40f + enemy.radius
                if (dx * dx + dy * dy <= enemyFuseThreshold * enemyFuseThreshold) {
                    p.expiredNaturally = true
                    p.isActive = false
                    detonated = true
                    break
                }
            }
            if (!detonated) {
                for (asteroid in asteroids) {
                    if (!asteroid.isActive || asteroid.fragmentImmunityTimer > 0f) continue
                    val dx = p.position.x - asteroid.position.x
                    val dy = p.position.y - asteroid.position.y
                    val asteroidFuseThreshold = 40f + asteroid.radius
                    if (dx * dx + dy * dy <= asteroidFuseThreshold * asteroidFuseThreshold) {
                        p.expiredNaturally = true
                        p.isActive = false
                        break
                    }
                }
            }
        }

        // Enemy proximity fuse: detonate enemy flak/mine when near player
        for (p in projectiles) {
            if (!p.isActive || !p.proximityFuse || !p.isEnemyProjectile) continue
            val dx = p.position.x - ship.position.x
            val dy = p.position.y - ship.position.y
            if (dx * dx + dy * dy <= 40f * 40f) {
                p.expiredNaturally = true
                p.isActive = false
            }
        }

        // Handle kills from instant-damage weapons (Solar Storm, Phoenix Flare)
        // These weapons call takeDamage() directly in fire() but can't trigger loot
        for (asteroid in asteroids) {
            if (asteroid.isActive && asteroid.health <= 0f) {
                handleAsteroidDestroyedWithTrail(asteroid)
            }
        }
        for (enemy in enemies) {
            if (enemy.isActive && enemy.health <= 0f) {
                onEnemyDestroyed(enemy)
                trackEnemyKill()
            }
        }
        // Boss kill from instant-damage weapons
        if (boss.isActive && boss.health <= 0f) {
            boss.isActive = false
            visualEffects.addExplosion(
                boss.position.x, boss.position.y,
                Boss.BOSS_SIZE * 3f, Boss.CORRUPTION_COLOR
            )
            SoundManager.playSFX("sfx_explosion", 0.6f)
        }

        // Energy Saw / Warp Saw contact damage
        sawDamageSystem.update(deltaTime, enemies, asteroids)
        beamDamageSystem.update(deltaTime, enemies, asteroids)
        sawDamageSystem.updateSparks(deltaTime)
        boss.updateShieldSparks(deltaTime)  // tick unconditionally so sparks still expire after the boss dies


        // Vampiric Core: proximity asteroid life leech
        vampiricLeecherSystem.update(ship, asteroids, state, deltaTime)

        // Orbiter hit sound cooldown
        if (orbiterSoundCooldown > 0f) orbiterSoundCooldown -= deltaTime

        // Crystal power: player railgun recall shots (mirrors BossSystem.updateRecallShots)
        if (state.hasCrystalPowers) {
            for (projectile in projectiles) {
                if (!projectile.isActive || projectile.isEnemyProjectile) continue
                if (projectile.weaponId != "railgun") continue

                if (!projectile.isRecalling) {
                    // Trigger recall at screen edge
                    val margin = 20f
                    val halfW = screenWidth / 2f
                    val halfH = screenHeight / 2f
                    val atEdge = projectile.position.x < ship.position.x - halfW + margin ||
                        projectile.position.x > ship.position.x + halfW - margin ||
                        projectile.position.y < ship.position.y - halfH + margin ||
                        projectile.position.y > ship.position.y + halfH - margin
                    if (atEdge) {
                        projectile.isRecalling = true
                        projectile.recallPauseTimer = Boss.RECALL_PAUSE_TIME
                        projectile.velocity.set(0f, 0f)
                        projectile.ricochetCount++
                        visualEffects.addHitFlash(projectile.position.x, projectile.position.y, 15f, projectile.color)
                    }
                } else {
                    projectile.recallPauseTimer -= deltaTime
                    if (projectile.recallPauseTimer <= 0f) {
                        var closestDist = Float.MAX_VALUE
                        var targetX = 0f
                        var targetY = 0f
                        var found = false
                        // Prefer enemies
                        for (enemy in enemies) {
                            if (!enemy.isActive) continue
                            val edx = enemy.position.x - projectile.position.x
                            val edy = enemy.position.y - projectile.position.y
                            val edist = sqrt(edx * edx + edy * edy)
                            if (edist < closestDist) {
                                closestDist = edist
                                targetX = enemy.position.x
                                targetY = enemy.position.y
                                found = true
                            }
                        }
                        // Then boss
                        if (boss.isActive && state.bossFightPhase < PHASE_OTHER_SPAWN) {
                            val bdx = boss.position.x - projectile.position.x
                            val bdy = boss.position.y - projectile.position.y
                            val bdist = sqrt(bdx * bdx + bdy * bdy)
                            if (bdist < closestDist) {
                                closestDist = bdist
                                targetX = boss.position.x
                                targetY = boss.position.y
                                found = true
                            }
                        }
                        // Fallback to asteroids
                        if (!found) {
                            for (asteroid in asteroids) {
                                if (!asteroid.isActive) continue
                                val adx = asteroid.position.x - projectile.position.x
                                val ady = asteroid.position.y - projectile.position.y
                                val adist = sqrt(adx * adx + ady * ady)
                                if (adist < closestDist) {
                                    closestDist = adist
                                    targetX = asteroid.position.x
                                    targetY = asteroid.position.y
                                    found = true
                                }
                            }
                        }
                        if (found) {
                            val tdx = targetX - projectile.position.x
                            val tdy = targetY - projectile.position.y
                            val tdist = sqrt(tdx * tdx + tdy * tdy).coerceAtLeast(1f)
                            projectile.velocity.set(tdx / tdist * Boss.RAIL_SPEED, tdy / tdist * Boss.RAIL_SPEED)
                        } else {
                            projectile.isActive = false
                        }
                        projectile.isRecalling = false
                        projectile.recallRetargeted = false
                        projectile.age = 0f
                    }
                }
            }
        }

        // Update enemy AI (with meteor dodging, projectile dodging, and missile interception)
        enemyAISystem.update(enemies, ship, deltaTime, asteroids, projectiles)

        // Update enemy weapons (cooldowns, stateful weapons like orbiters)
        enemyWeaponSystem.update(enemies, ship, deltaTime)

        // Fire enemy weapons (spawn shield is defensive only — enemies still attack)
        for (enemy in enemies) {
            if (!enemy.isActive || enemy.isWarping || enemy.isCrewmate) continue
            enemyWeaponSystem.fire(enemy, ship)
        }

        // Revenge Protocol: count down active timer
        if (state.revengeActive) {
            state.revengeTimer -= deltaTime
            if (state.revengeTimer <= 0f) {
                state.revengeActive = false
            }
        }

        // Continuous flight tracking (Dash unlock)
        // Pauses (doesn't accumulate or reset) on upgrade screen.
        // 2s grace after upgrade screen; 0.5s grace for brief finger lifts.
        if (state.phase == GamePhase.UPGRADE_SELECTION) {
            continuousFlightPauseTimer = 2f          // hold grace fresh each upgrade frame
        } else if (!ship.moveDirection.isZero()) {
            continuousFlightTimer += deltaTime
            if (continuousFlightTimer.toInt() > bestContinuousFlightThisRun) {
                bestContinuousFlightThisRun = continuousFlightTimer.toInt()
            }
            continuousFlightPauseTimer = 0.5f        // brief-stop grace while in motion
        } else {
            // Stopped — drain grace before resetting streak
            if (continuousFlightPauseTimer > 0f) {
                continuousFlightPauseTimer -= deltaTime
                continuousFlightTimer += deltaTime   // streak alive during grace
                if (continuousFlightTimer.toInt() > bestContinuousFlightThisRun) {
                    bestContinuousFlightThisRun = continuousFlightTimer.toInt()
                }
            } else {
                continuousFlightTimer = 0f
            }
        }

        // Cryo Field: slow nearby enemies and asteroids each frame
        if (state.cryoSlowPercent > 0f) {
            val cryoRadius = 100f * state.cryoRadiusMultiplier
            val cryoRadiusSq = cryoRadius * cryoRadius
            val slowFactor = (1f - state.cryoSlowPercent).coerceAtLeast(0.1f)
            for (enemy in enemies) {
                if (!enemy.isActive || enemy.isWarping) continue
                val dx = enemy.position.x - ship.position.x
                val dy = enemy.position.y - ship.position.y
                val enemyEdgeRadius = cryoRadius + enemy.radius
                if (dx * dx + dy * dy < enemyEdgeRadius * enemyEdgeRadius) {
                    val speed = sqrt(enemy.velocity.x * enemy.velocity.x + enemy.velocity.y * enemy.velocity.y)
                    val maxSlowedSpeed = enemy.getEffectiveSpeed() * slowFactor
                    if (speed > maxSlowedSpeed && speed > 0.001f) {
                        val scale = maxSlowedSpeed / speed
                        enemy.velocity.x *= scale
                        enemy.velocity.y *= scale
                    }
                    enemy.cryoAffected = true
                } else {
                    enemy.cryoAffected = false
                }
            }
            for (asteroid in asteroids) {
                if (!asteroid.isActive) continue
                val dx = asteroid.position.x - ship.position.x
                val dy = asteroid.position.y - ship.position.y
                val asteroidEdgeRadius = cryoRadius + asteroid.radius
                if (dx * dx + dy * dy < asteroidEdgeRadius * asteroidEdgeRadius) {
                    if (!asteroid.cryoAffected) {
                        // First frame entering field: apply slow once
                        asteroid.velocity.x *= slowFactor
                        asteroid.velocity.y *= slowFactor
                    }
                    asteroid.cryoAffected = true
                } else {
                    asteroid.cryoAffected = false
                }
            }
        } else {
            // No cryo active - clear all flags
            for (enemy in enemies) {
                enemy.cryoAffected = false
            }
            for (asteroid in asteroids) {
                asteroid.cryoAffected = false
            }
        }

        // Frost Ring: mark entities touched by orbiters with cryo visual (blue outline, no slowdown)
        for (p in projectiles) {
            if (!p.isActive || p.weaponId != "frost_ring" || p.type != ProjectileType.ORBITER) continue
            val rSq = p.radius * p.radius
            for (enemy in enemies) {
                if (!enemy.isActive) continue
                val dx = enemy.position.x - p.position.x
                val dy = enemy.position.y - p.position.y
                if (dx * dx + dy * dy < rSq) enemy.cryoAffected = true
            }
            for (asteroid in asteroids) {
                if (!asteroid.isActive) continue
                val dx = asteroid.position.x - p.position.x
                val dy = asteroid.position.y - p.position.y
                if (dx * dx + dy * dy < rSq) asteroid.cryoAffected = true
            }
        }

        // Combat Drones: autonomous AI wingmen
        combatDroneSystem.update(deltaTime, asteroids, enemies)
        combatDroneSystem.checkCollisions(asteroids, projectiles)

        // Check for warp-in completion and destroy overlapping asteroids/drones
        for (enemy in enemies) {
            if (enemy.warpInComplete && enemy.isActive) {
                // Find and destroy any asteroids overlapping with this enemy
                for (asteroid in asteroids) {
                    if (asteroid.isActive && enemy.collidesWith(asteroid)) {
                        // Destroy the asteroid with explosion effect
                        visualEffects.addExplosion(
                            asteroid.position.x,
                            asteroid.position.y,
                            asteroid.radius * 1.5f,
                            0xFF00FFFF.toInt()  // Cyan warp effect
                        )
                        captureTrailFade(asteroid)
                        asteroid.isActive = false
                    }
                }
                // Clear the flag
                enemy.warpInComplete = false
            }
        }

        // Check enemy-asteroid collisions
        val destroyedEnemies = collisionSystem.checkEnemyAsteroidCollisions(enemies, asteroids)
        for (enemy in destroyedEnemies) {
            onEnemyDestroyed(enemy)
            trackEnemyKill()
        }

        // Trail asteroid damage to ship and enemies
        collisionSystem.updateTrailCooldowns(deltaTime)
        val trailHits = collisionSystem.checkTrailCollisions(asteroids, ship, enemies)
        if (trailHits.shipDamage > 0f && !ship.isInvulnerable) {
            if (!tryCrystalDodge() && !tryEvade(state)) {
                val healthBefore = ship.health
                val shieldBefore = ship.currentShield
                ship.takeDamage(trailHits.shipDamage)
                state.telemetryDamageTakenBy["trail"] = (state.telemetryDamageTakenBy["trail"] ?: 0f) + trailHits.shipDamage
                state.telemetryTotalDamageTaken += trailHits.shipDamage
                state.lastDamageSource = "trail"
                vibrateHit()
                playDamageSound(shieldBefore, ship.currentShield)
                if (!isNonAstroCorruptionRun) ship.makeInvulnerable()
                val revengeStacks = state.passiveStacks["revenge_protocol"] ?: 0
                if (revengeStacks > 0) {
                    state.revengeTimer = revengeStacks * 2f
                    state.revengeActive = true
                }
                if (ship.health <= 0) {
                    handlePlayerDeath()
                } else {
                    val healthLost = healthBefore - ship.health
                    if (healthLost > ship.maxHealth * 0.25f) {
                        radioSystem.onBigHit(state)
                    } else if (ship.health < ship.maxHealth * 0.20f) {
                        radioSystem.onLowHealth(state)
                    }
                }
            }
        }
        for ((enemy, damage) in trailHits.enemyHits) {
            if (!enemy.isCrewmate && !isOnScreen(enemy)) continue  // Can't damage off-screen enemies
            if (enemy.takeDamage(damage)) {
                onEnemyDestroyed(enemy)
                trackEnemyKill()
            }
        }

        // Update boss if active
        val inFleetCutscene = state.bossFightPhase == PHASE_FLEET_CHATTER || state.bossFightPhase == PHASE_FORMATION || state.bossFightPhase == PHASE_SHIELD_ASSAULT || state.bossFightPhase == PHASE_TB26_RAM || state.bossFightPhase == PHASE_OTHER_SURVIVAL || state.bossFightPhase == PHASE_OTHER_FLEET || state.bossFightPhase == PHASE_OTHER_FORMATION || state.bossFightPhase == PHASE_OTHER_SHIELD_ASSAULT || state.bossFightPhase == PHASE_OTHER_TB26_RAM || state.bossFightPhase == PHASE_OTHER_DYING || state.bossFightPhase == PHASE_HEART_TRANSITION
        if (state.bossActive && boss.isActive) {
            if (!inFleetCutscene) {
                bossSystem.update(deltaTime, projectiles, enemies, asteroids)
            } else {
                boss.update(deltaTime)  // rotation-only during fleet scene; stun prevents movement/firing
            }

            // Boss energy shield — contact damage aura (mirrors corrupted Astro crystal powers)
            if (!boss.isStunned && !inFleetCutscene) {
                boss.shieldTickTimer -= deltaTime
                if (boss.shieldTickTimer <= 0f) {
                    boss.shieldTickTimer += 0.125f  // 8 ticks per second
                    val bossShieldRadius = 60f
                    val bossShieldDamage = 50f

                    if (ship.isActive && !ship.isInvulnerable) {
                        val dx = boss.position.x - ship.position.x
                        val dy = boss.position.y - ship.position.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < bossShieldRadius + ship.radius) {
                            val healthBefore = ship.health
                            val shieldBefore = ship.currentShield
                            ship.takeDamage(bossShieldDamage)
                            state.telemetryDamageTakenBy["boss_aura"] = (state.telemetryDamageTakenBy["boss_aura"] ?: 0f) + bossShieldDamage
                            state.telemetryTotalDamageTaken += bossShieldDamage
                            state.lastDamageSource = "boss_aura"
                            if (bossAuraSoundCooldown <= 0f) {
                                playDamageSound(shieldBefore, ship.currentShield)
                                bossAuraSoundCooldown = 0.4f
                            }
                            visualEffects.addDamageNumber(ship.position.x, ship.position.y, bossShieldDamage.toInt(), CrystalPalette.MID)

                            // Revenge Protocol
                            val revengeStacks = state.passiveStacks["revenge_protocol"] ?: 0
                            if (revengeStacks > 0) {
                                state.revengeTimer = revengeStacks * 2f
                                state.revengeActive = true
                            }

                            // Radio triggers
                            val healthLost = healthBefore - ship.health
                            if (ship.health <= 0) {
                                handlePlayerDeath()
                            } else if (healthLost > ship.maxHealth * 0.25f) {
                                radioSystem.onBigHit(state)
                            } else if (ship.health < ship.maxHealth * 0.20f) {
                                radioSystem.onLowHealth(state)
                            }
                        }
                    }
                }
            }
        }

        // Phase 4 boss fight sequence
        if (state.bossFightPhase > PHASE_NONE) {
            // bossFightTimer multiplier retained for any future use; time is already 1x by encounter start
            // Dialogue timers (fleetChatterTimer) are separate and stay at real-time speed
            // Heart transition uses real-time speed for the visual effect
            state.bossFightTimer += deltaTime * if (state.bossFightPhase >= PHASE_OTHER_SPAWN && state.bossFightPhase < PHASE_HEART_TRANSITION) {
                state.corruptionTimeMultiplier
            } else {
                1f
            }
            if (state.bossCharging) {
                state.bossChargeTimer += deltaTime
                val rawProgress = (state.bossChargeTimer / GameConfig.BOSS_CHARGE_DURATION).coerceAtMost(1f)
                state.bossChargeProgress = 1f - (1f - rawProgress) * (1f - rawProgress)
            }
            updateBossFightSequence(deltaTime)
            // Corruption run: keep boss entity anchored to player and facing Past Astro while charge builds.
            // Covers PHASE_OTHER_SURVIVAL (steps 6-9) where boss.isActive is now true but boss entity
            // isn't yet driven by any phase handler, and PHASE_OTHER_FLEET/FORMATION where rotation was stale.
            if (isAstroCorruptionRun && state.bossCharging) {
                boss.position.set(ship.position.x, ship.position.y)
                val chargePastAstro = crewmateEncounter.crewmateShip
                if (chargePastAstro != null && chargePastAstro.isActive) {
                    boss.rotation = atan2(
                        chargePastAstro.position.y - ship.position.y,
                        chargePastAstro.position.x - ship.position.x
                    )
                    ship.rotation = boss.rotation
                }
            }
        }

        // Update movement
        movementSystem.updateAsteroids(asteroids, ship, deltaTime, state.survivalTime)
        movementSystem.updateProjectiles(projectiles, deltaTime)
        movementSystem.updatePowerUps(powerUps, deltaTime)

        // Near miss detection
        if (state.nearMissCooldown <= 0f) {
            val missThreshold = ship.radius * 1.5f
            var nearMissDetected = false

            // Check asteroids
            for (asteroid in asteroids) {
                if (!asteroid.isActive) continue
                val dx = asteroid.position.x - ship.position.x
                val dy = asteroid.position.y - ship.position.y
                val distSq = dx * dx + dy * dy
                val touchDist = ship.radius + asteroid.radius
                val missDist = touchDist + missThreshold
                if (distSq < missDist * missDist && distSq > touchDist * touchDist) {
                    nearMissDetected = true
                    break
                }
            }

            // Check enemy projectiles
            if (!nearMissDetected) {
                for (projectile in projectiles) {
                    if (!projectile.isActive || !projectile.isEnemyProjectile) continue
                    val dx = projectile.position.x - ship.position.x
                    val dy = projectile.position.y - ship.position.y
                    val distSq = dx * dx + dy * dy
                    val touchDist = ship.radius + projectile.radius
                    val missDist = touchDist + missThreshold
                    if (distSq < missDist * missDist && distSq > touchDist * touchDist) {
                        nearMissDetected = true
                        break
                    }
                }
            }

            if (nearMissDetected && ship.health <= ship.maxHealth * 0.30f) {
                state.nearMissCooldown = 120f
                radioSystem.onNearMiss(state)
            }
        }

        // Mine repulsion physics - mines push each other apart
        movementSystem.applyMineRepulsion(projectiles, deltaTime)

        // Apply special effects
        movementSystem.applyGravityWellEffects(projectiles, asteroids, deltaTime)

        // Check collisions
        val pickupRange = GameConfig.POWERUP_MAGNET_BASE_RANGE * state.pickupRangeMultiplier * state.getMagnetRangeMultiplier()
        val pullSpeed = GameConfig.POWERUP_PULL_SPEED * state.getMagnetSpeedMultiplier()
        val collisionResult = collisionSystem.checkCollisions(
            ship, asteroids, projectiles, powerUps, pickupRange, pullSpeed
        )

        // Process projectile hits on asteroids
        for ((projectile, asteroid) in collisionResult.asteroidHits) {
            if (projectile.weaponId == "volatile_detonation") continue
            if (asteroid.fragmentImmunityTimer > 0f) continue
            if (!projectile.isEnemyProjectile) {
                val (damage, isCrit) = applyDamageModifiers(projectile.damage)
                state.telemetryDamageByWeapon[projectile.weaponId] = (state.telemetryDamageByWeapon[projectile.weaponId] ?: 0f) + damage
                state.telemetryTotalDamageDealt += damage
                if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }

                val destroyed = asteroid.takeDamage(damage)

                // Ion Orbiters / Frost Ring: silent against asteroids — they only sound on enemy hits (below, by request).

                // Siphon Needles: heal 0.1 HP per hit
                if (projectile.weaponId == "siphon_needles") {
                    ship.health = (ship.health + 0.1f).coerceAtMost(ship.maxHealth)
                }

                // Add damage number
                visualEffects.addDamageNumber(
                    asteroid.position.x + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                    asteroid.position.y - asteroid.radius,
                    damage.toInt(),
                    projectile.color,
                    isCrit
                )

                // Add hit flash
                visualEffects.addHitFlash(
                    projectile.position.x,
                    projectile.position.y,
                    15f,
                    projectile.color
                )

                if (destroyed) {
                    handleAsteroidDestroyedWithTrail(asteroid)
                }
            }
        }

        // Check player projectile hits on enemies
        for (projectile in projectiles) {
            if (!projectile.isActive || projectile.isEnemyProjectile) continue
            if (projectile.type == ProjectileType.LIGHTNING && projectile.bounceCount == 99) continue
            if (projectile.isVisualOnly) continue

            for (enemy in enemies) {
                if (!enemy.isActive || enemy.isWarping) continue  // Skip warping enemies
                if (!enemy.isCrewmate && !isOnScreen(enemy)) continue  // Can't damage off-screen enemies

                if (projectile.collidesWith(enemy)) {
                    // projectile.damage already includes damageMultiplier from getDamage()
                    val (damage, isCrit) = applyDamageModifiers(projectile.damage)
                    state.telemetryDamageByWeapon[projectile.weaponId] = (state.telemetryDamageByWeapon[projectile.weaponId] ?: 0f) + damage
                    state.telemetryTotalDamageDealt += damage
                    if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }

                    val destroyed = enemy.takeDamage(damage, projectile.ignoresSpawnShield)
                    projectile.onHit(enemy)
                    // Gambler's Mines: fire payout on every detonation (enemy contact)
                    if (projectile.weaponId == "jackpot_mines") {
                        applyGamblersMineEffect(projectile.position.x, projectile.position.y)
                    }

                    // Autonomous Ace on-hit mechanics: fragmentation burst
                    val isAutonomousAce = projectile.weaponId == "autonomous_ace"
                    if (isAutonomousAce && !projectile.hasChained) {
                        val weaponColor = ShipDefinitions.getWeaponColor("homing_missiles", state.isCorruptionRun)
                        // Find nearest active enemy excluding the hit enemy
                        var fragTarget: EnemyShip? = null
                        var bestDist = Float.MAX_VALUE
                        for (e in enemies) {
                            if (e == enemy || !e.isActive) continue
                            val d = e.distanceTo(enemy)
                            if (d < bestDist) { bestDist = d; fragTarget = e }
                        }

                        repeat(2) {
                            val angle = ship.rotation + (kotlin.random.Random.nextFloat() - 0.5f) * 0.6f
                            val direction = com.astroloop.game.util.Vector2.fromAngle(angle)
                            val miniMissile = EntityPools.projectiles.obtain()
                            miniMissile.initialize(
                                x = enemy.position.x,
                                y = enemy.position.y,
                                vx = direction.x * 350f,
                                vy = direction.y * 350f,
                                projectileType = ProjectileType.MISSILE,
                                projectileDamage = 12f,
                                projectileLifetime = 1.5f
                            )
                            miniMissile.weaponId = projectile.weaponId
                            miniMissile.hasChained = true
                            miniMissile.radius = 5f
                            miniMissile.homingStrength = 3f
                            miniMissile.target = fragTarget
                            miniMissile.color = weaponColor
                        }
                    }

                    // Ion Orbiters / Frost Ring: sound on hit with cooldown
                    if ((projectile.weaponId == "ion_orbiters" || projectile.weaponId == "frost_ring") && orbiterSoundCooldown <= 0f) {
                        SoundManager.playSFX("sfx_weapon_${projectile.weaponId}", SoundManager.getWeaponSfxVolume(projectile.weaponId))
                        orbiterSoundCooldown = 0.5f
                    }

                    // Spawn bomblets on hit (Cluster Bomb, Torpedo Storm, etc.)
                    if (projectile.bombletCount > 0 && !projectile.isActive) {
                        projectileEffectsSystem.spawnBombletsFromCollision(projectile)
                    }

                    // Siphon Needles: heal 0.1 HP per hit
                    if (projectile.weaponId == "siphon_needles") {
                        ship.health = (ship.health + 0.1f).coerceAtMost(ship.maxHealth)
                    }

                    // Phoenix Flare: suppress visual effects on pierce hits beyond the first to avoid frame spike
                    val showHitEffects = projectile.weaponId != "phoenix_flare" || projectile.pierceCount <= 1

                    // Add damage number for enemy hit (suppress for perfect dodge and spawn shield)
                    if (showHitEffects && !enemy.perfectDodge && !enemy.isSpawnShielded) {
                        visualEffects.addDamageNumber(
                            enemy.position.x + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                            enemy.position.y - enemy.radius,
                            damage.toInt(),
                            projectile.color,
                            isCrit
                        )
                    } else if (enemy.isSpawnShielded && !projectile.ignoresSpawnShield) {
                        SoundManager.playSFX("sfx_shield_hit", 0.15f)
                    }

                    // Add hit flash
                    if (showHitEffects) {
                        visualEffects.addHitFlash(
                            projectile.position.x,
                            projectile.position.y,
                            15f,
                            projectile.color
                        )
                    }

                    if (destroyed) {
                        onEnemyDestroyed(enemy)
                        trackEnemyKill()
                    }
                    break
                }
            }
        }

        // Shield deflection — projectile fizzles out against the outer shield ring, no damage
        // (runs even while boss is invulnerable). In corruption finale, boss entity is the
        // fleet target marker at the player position.
        if (boss.isActive && boss.shielded && (state.bossFightPhase < PHASE_OTHER_SPAWN ||
                state.bossFightPhase >= PHASE_OTHER_SURVIVAL)) {
            for (projectile in projectiles) {
                if (!projectile.isActive || projectile.isEnemyProjectile || projectile.type == ProjectileType.ORBITER) continue
                if (projectile.isVisualOnly) continue  // non-damaging telegraph (e.g. Lingering Nova core)
                val dx = projectile.position.x - boss.position.x
                val dy = projectile.position.y - boss.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < projectile.radius + Boss.SHIELD_DEFLECT_RADIUS) {
                    val nrm = 1f / dist.coerceAtLeast(0.001f)
                    boss.spawnShieldSparks(projectile.position.x, projectile.position.y, dx * nrm, dy * nrm, projectile.color)
                    projectile.isActive = false
                }
            }

            // Energy-saw contact — the fleet saw streams sparks where its disc clips the
            // visible shield ring, throttled so it doesn't flood the spark list.
            sawShieldSparkCooldown -= deltaTime
            if (sawShieldSparkCooldown <= 0f) {
                val shieldRing = Boss.FLEET_SHIELD_RING_RADIUS
                sawLoop@ for (fs in fleetSystem.fleetShips) {
                    val saw = fs.sawWeapon ?: continue
                    if (!fs.isActive) continue
                    for ((discX, discY) in saw.getDiscPositions(fs.position.x, fs.position.y, fs.rotation)) {
                        val sdx = discX - boss.position.x
                        val sdy = discY - boss.position.y
                        val sdist = sqrt(sdx * sdx + sdy * sdy)
                        if (sdist < saw.discRadius + shieldRing + Boss.SAW_SPARK_TOLERANCE) {
                            val snrm = 1f / sdist.coerceAtLeast(0.001f)
                            boss.spawnShieldSparks(
                                boss.position.x + sdx * snrm * shieldRing,
                                boss.position.y + sdy * snrm * shieldRing,
                                sdx * snrm, sdy * snrm, fs.color
                            )
                            sawShieldSparkCooldown = 0.1f
                            break@sawLoop
                        }
                    }
                }
            }
        }

        // Projectile-boss collision (player + fleet projectiles damage boss)
        // Skip during corruption finale — boss is at player position as fleet target marker
        if (boss.isActive && !boss.isInvulnerable && !boss.shielded && state.bossFightPhase < PHASE_OTHER_SPAWN) {
            for (projectile in projectiles) {
                if (!projectile.isActive || projectile.isEnemyProjectile) continue
                if (projectile.isVisualOnly) continue  // non-damaging telegraph (e.g. Lingering Nova core)
                val dx = projectile.position.x - boss.position.x
                val dy = projectile.position.y - boss.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < projectile.radius + boss.radius) {
                    val (damage, isCrit) = applyDamageModifiers(projectile.damage)
                    state.telemetryDamageByWeapon[projectile.weaponId] = (state.telemetryDamageByWeapon[projectile.weaponId] ?: 0f) + damage
                    state.telemetryTotalDamageDealt += damage
                    if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }
                    boss.takeDamage(damage)
                    projectile.onHit(boss)

                    visualEffects.addDamageNumber(
                        boss.position.x + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                        boss.position.y - boss.radius,
                        damage.toInt(),
                        projectile.color,
                        isCrit
                    )
                    visualEffects.addHitFlash(
                        projectile.position.x,
                        projectile.position.y,
                        15f,
                        projectile.color
                    )

                    if (boss.health <= 0f) {
                        boss.isActive = false
                        visualEffects.addExplosion(
                            boss.position.x, boss.position.y,
                            Boss.BOSS_SIZE * 3f, Boss.CORRUPTION_COLOR
                        )
                        SoundManager.playSFX("sfx_explosion", 0.6f)
                    }
                }
            }
        }

        // Corruption finale: absorb fleet projectiles with visual impact (no damage — scripted death handles that)
        if (boss.isActive && !boss.shielded && state.bossFightPhase == PHASE_OTHER_DYING) {
            for (projectile in projectiles) {
                if (!projectile.isActive || projectile.isEnemyProjectile) continue
                if (projectile.isVisualOnly) continue  // non-damaging telegraph (e.g. Lingering Nova core)
                val dx = projectile.position.x - boss.position.x
                val dy = projectile.position.y - boss.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < projectile.radius + boss.radius) {
                    visualEffects.addHitFlash(projectile.position.x, projectile.position.y, 15f, projectile.color)
                    projectile.isActive = false
                }
            }
        }

        // Check enemy projectile hits on player
        if (!ship.isInvulnerable) {
            for (projectile in projectiles) {
                if (!projectile.isActive || !projectile.isEnemyProjectile) continue

                if (projectile.collidesWith(ship)) {
                    // Try crystal dodge or evasion first
                    if (!tryCrystalDodge() && !tryEvade(state)) {
                        val healthBefore = ship.health
                        val shieldBefore = ship.currentShield
                        ship.takeDamage(projectile.damage)
                        state.telemetryDamageTakenBy["enemy_projectile"] = (state.telemetryDamageTakenBy["enemy_projectile"] ?: 0f) + projectile.damage
                        state.telemetryTotalDamageTaken += projectile.damage
                        state.lastDamageSource = "enemy_projectile"
                        vibrateHit()
                        playDamageSound(shieldBefore, ship.currentShield)
                        if (!isNonAstroCorruptionRun) ship.makeInvulnerable()

                        // Revenge Protocol: trigger on ship damage
                        val revengeStacks = state.passiveStacks["revenge_protocol"] ?: 0
                        if (revengeStacks > 0) {
                            state.revengeTimer = revengeStacks * 2f
                            state.revengeActive = true
                        }

                        if (ship.health <= 0) {
                            handlePlayerDeath()
                        } else {
                            val healthLost = healthBefore - ship.health
                            if (healthLost > ship.maxHealth * 0.25f) {
                                radioSystem.onBigHit(state)
                            } else if (ship.health < ship.maxHealth * 0.20f) {
                                radioSystem.onLowHealth(state)
                            }
                        }
                    }
                    projectile.isActive = false
                    if (projectile.explodeOnDeath) projectile.expiredNaturally = true
                    break
                }
            }
        }

        // Process expired projectile end-of-life effects
        // Gambler's Mines: snapshot coordinates before processExpired can pool+reset the objects
        val expiringMinePositions = projectiles
            .filter { it.isActive && it.weaponId == "jackpot_mines" && it.expiredNaturally }
            .map { Pair(it.position.x, it.position.y) }
        projectileEffectsSystem.processExpired(projectiles, asteroids, enemies)
        for ((mx, my) in expiringMinePositions) {
            applyGamblersMineEffect(mx, my)
        }

        // Enemy saw disc damage (Ripper enemies with energy_saw)
        if (!ship.isInvulnerable) {
            for (enemy in enemies) {
                if (!enemy.isActive || enemy.weaponId != "energy_saw") continue
                if (enemy.isSpawnShielded || enemy.isWarping || enemy.isCrewmate) continue

                val discPositions = enemyWeaponSystem.getSawDiscPositions(enemy)
                for ((discX, discY) in discPositions) {
                    val dx = discX - ship.position.x
                    val dy = discY - ship.position.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < enemy.sawDiscRadius + ship.radius) {
                        if (enemy.weaponCooldown <= 0f) {
                            if (!tryCrystalDodge() && !tryEvade(state)) {
                                val healthBefore = ship.health
                                val shieldBefore = ship.currentShield
                                ship.takeDamage(enemy.weaponDamage)
                                state.telemetryDamageTakenBy["enemy_contact"] = (state.telemetryDamageTakenBy["enemy_contact"] ?: 0f) + enemy.weaponDamage
                                state.telemetryTotalDamageTaken += enemy.weaponDamage
                                state.lastDamageSource = "enemy_contact"
                                enemy.weaponCooldown = 0.1f  // Saw tick rate
                                vibrateHit()
                                playDamageSound(shieldBefore, ship.currentShield)
                                if (!isNonAstroCorruptionRun) ship.makeInvulnerable()

                                // Revenge Protocol: trigger on ship damage
                                val revengeStacks = state.passiveStacks["revenge_protocol"] ?: 0
                                if (revengeStacks > 0) {
                                    state.revengeTimer = revengeStacks * 2f
                                    state.revengeActive = true
                                }

                                if (ship.health <= 0) {
                                    handlePlayerDeath()
                                } else {
                                    val healthLost = healthBefore - ship.health
                                    if (healthLost > ship.maxHealth * 0.25f) {
                                        radioSystem.onBigHit(state)
                                    } else if (ship.health < ship.maxHealth * 0.20f) {
                                        radioSystem.onLowHealth(state)
                                    }
                                }
                            }
                        }
                        break
                    }
                }
            }
        }

        // Radio: shields down trigger
        if (state.shieldsWereUp && ship.currentShield <= 0f && ship.maxShield > 0f) {
            radioSystem.onShieldsDown(state)
            // Shield break sound removed (distracting)
        }
        state.shieldsWereUp = ship.currentShield > 0f

        // Player projectile vs enemy orbs (Bomber orbs are destroyable)
        for (playerProj in projectiles) {
            if (!playerProj.isActive || playerProj.isEnemyProjectile) continue
            for (enemyProj in projectiles) {
                if (!enemyProj.isActive || !enemyProj.isEnemyProjectile || enemyProj.weaponId != "enemy_orb") continue
                val dx = playerProj.position.x - enemyProj.position.x
                val dy = playerProj.position.y - enemyProj.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < playerProj.radius + enemyProj.radius) {
                    // Destroy the orb (triggers detonation via expiredNaturally in next frame)
                    enemyProj.expiredNaturally = true
                    enemyProj.isActive = false
                    playerProj.isActive = false
                    break
                }
            }
        }

        // Process lightning forks
        for (fork in collisionResult.lightningForks) {
            // Add small explosion visual at impact point
            visualEffects.addExplosion(
                fork.x,
                fork.y,
                40f,
                0xFF88AAFF.toInt()  // Light blue lightning flash
            )

            // Find nearby targets and spawn fork projectiles
            val targets = collisionSystem.findForkTargets(
                fork.x, fork.y, fork.forkRange, fork.forkCount, fork.hitEntity, asteroids
            )

            // Draw jagged lightning fork visuals from impact to each target
            for (target in targets) {
                val targetX = target.position.x
                val targetY = target.position.y
                val segments = 3
                for (seg in 1..segments) {
                    val t = seg.toFloat() / segments
                    val baseX = fork.x + (targetX - fork.x) * t
                    val baseY = fork.y + (targetY - fork.y) * t
                    // Perpendicular offset for jagged look
                    val dx = targetX - fork.x
                    val dy = targetY - fork.y
                    val perpX = -dy
                    val perpY = dx
                    val perpLen = kotlin.math.sqrt(perpX * perpX + perpY * perpY).coerceAtLeast(1f)
                    val jag = if (seg % 2 == 1) 12f else -8f
                    val jagX = baseX + (perpX / perpLen) * jag
                    val jagY = baseY + (perpY / perpLen) * jag
                    visualEffects.addHitFlash(jagX, jagY, 5f, 0xFFAADDFF.toInt())
                }
            }

            for (target in targets) {
                // Spawn a fork projectile toward each target
                val dx = target.position.x - fork.x
                val dy = target.position.y - fork.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val dirX = dx / dist
                val dirY = dy / dist

                val forkProjectile = EntityPools.projectiles.obtain()
                forkProjectile.initialize(
                    x = fork.x,
                    y = fork.y,
                    vx = dirX * 600f,  // Fast fork projectiles
                    vy = dirY * 600f,
                    projectileType = ProjectileType.LIGHTNING,
                    projectileDamage = fork.damage * 0.7f,  // Fork damage is 70% of primary
                    projectileLifetime = 0.5f  // Short lifetime - just needs to reach target
                )
                forkProjectile.weaponId = "lightning_fork"
                forkProjectile.bounceCount = 1  // Mark as forked (won't fork again)
                forkProjectile.radius = 8f
                forkProjectile.color = 0xFFAADDFF.toInt()  // Lighter blue for forks
            }
        }

        // Process explosions
        for (explosion in collisionResult.explosions) {
            // Add visual explosion effect
            visualEffects.addExplosion(
                explosion.x,
                explosion.y,
                explosion.radius,
                0xFFFFAA44.toInt()  // Orange/yellow explosion
            )
        }

        val explosionHits = collisionSystem.processExplosions(
            collisionResult.explosions, asteroids
        )
        for ((explosion, asteroid) in explosionHits) {
            val (finalDamage, isCrit) = applyDamageModifiers(explosion.damage)
            val destroyed = asteroid.takeDamage(finalDamage)

            // Add damage number for explosion hit — colored by the source weapon
            // (the orange burst above is the explosion visual; the number matches
            // the weapon's particle color, like direct hits do).
            visualEffects.addDamageNumber(
                asteroid.position.x,
                asteroid.position.y - asteroid.radius,
                finalDamage.toInt(),
                explosion.color,
                isCrit
            )

            if (destroyed) {
                handleAsteroidDestroyedWithTrail(asteroid)
            }
        }

        // Gambler's Mines: fire payout once per mine detonation (asteroid contact via explosion)
        // Deduplicate: one payout per unique mine position to handle multiple explosion events per mine per frame
        val uniqueJackpotMinePositions = collisionResult.explosions
            .filter { it.sourceWeaponId == "jackpot_mines" }
            .map { Pair(it.x, it.y) }
            .distinctBy { (px, py) -> Pair(px.toBits(), py.toBits()) }
        for ((mx, my) in uniqueJackpotMinePositions) {
            applyGamblersMineEffect(mx, my)
        }

        // Spawn bomblets from collision-killed torpedoes
        for ((projectile, _) in collisionResult.asteroidHits) {
            if (projectile.bombletCount > 0 && !projectile.isActive) {
                projectileEffectsSystem.spawnBombletsFromCollision(projectile)
            }
        }

        // Process ship hit by asteroid
        if (collisionResult.shipHit != null) {
            val asteroid = collisionResult.shipHit

            // Try crystal dodge or evasion first
            // Note: ship.isInvulnerable re-checked here because another hit (e.g. enemy projectile)
            // may have set invulnerability between checkCollisions() and this processing step
            if (!ship.isInvulnerable && !tryCrystalDodge() && !tryEvade(state)) {
                val healthBefore = ship.health
                val shieldBefore = ship.currentShield
                ship.takeDamage(asteroid.damage)
                state.telemetryDamageTakenBy["asteroid"] = (state.telemetryDamageTakenBy["asteroid"] ?: 0f) + asteroid.damage
                state.telemetryTotalDamageTaken += asteroid.damage
                state.lastDamageSource = "asteroid"
                vibrateHit()
                // Only play sound if the asteroid sound cooldown has expired — prevents rapid
                // repetition while continuously touching an asteroid (damage still applies every 1.5s)
                if (asteroidHitSoundCooldown <= 0f) {
                    playDamageSound(shieldBefore, ship.currentShield)
                    asteroidHitSoundCooldown = 3.0f
                }
                if (!isNonAstroCorruptionRun) ship.makeInvulnerable()

                // Revenge Protocol: trigger on ship damage
                val revengeStacks = state.passiveStacks["revenge_protocol"] ?: 0
                if (revengeStacks > 0) {
                    state.revengeTimer = revengeStacks * 2f
                    state.revengeActive = true
                }

                if (ship.health <= 0) {
                    handlePlayerDeath()
                }

                // Volatile asteroid explosion (skip if ship became invulnerable from phoenix or has crystal powers)
                if (asteroid.type == AsteroidType.VOLATILE && !ship.isInvulnerable && !state.hasCrystalPowers) {
                    val shieldBeforeVolatile = ship.currentShield
                    val volatileDmg = asteroid.getExplosionDamage()
                    ship.takeDamage(volatileDmg)
                    state.telemetryDamageTakenBy["asteroid"] = (state.telemetryDamageTakenBy["asteroid"] ?: 0f) + volatileDmg
                    state.telemetryTotalDamageTaken += volatileDmg
                    vibrateHit()
                    playDamageSound(shieldBeforeVolatile, ship.currentShield)
                    // Revenge re-armed by volatile splash as well
                    val revengeStacksVolatile = state.passiveStacks["revenge_protocol"] ?: 0
                    if (revengeStacksVolatile > 0) {
                        state.revengeTimer = revengeStacksVolatile * 2f
                        state.revengeActive = true
                    }
                    if (ship.health <= 0) {
                        handlePlayerDeath()
                    }
                }

                // Radio: big hit / low health
                if (ship.health > 0f) {
                    val healthLost = healthBefore - ship.health
                    if (healthLost > ship.maxHealth * 0.25f) {
                        radioSystem.onBigHit(state)
                    } else if (ship.health < ship.maxHealth * 0.20f) {
                        radioSystem.onLowHealth(state)
                    }
                }
            }
        }

        // Check ship collision with enemy ships
        if (!ship.isInvulnerable) {
            for (enemy in enemies) {
                if (!enemy.isActive || enemy.isCrewmate) continue  // Skip crewmate contact damage

                if (ship.collidesWith(enemy)) {
                    if (!tryCrystalDodge() && !tryEvade(state)) {
                        val healthBefore = ship.health
                        val shieldBefore = ship.currentShield
                        val contactDamage = 20f  // Flat ram damage for all enemies
                        ship.takeDamage(contactDamage)
                        state.telemetryDamageTakenBy["enemy_contact"] = (state.telemetryDamageTakenBy["enemy_contact"] ?: 0f) + contactDamage
                        state.telemetryTotalDamageTaken += contactDamage
                        state.lastDamageSource = "enemy_contact"
                        vibrateHit()
                        playDamageSound(shieldBefore, ship.currentShield)
                        if (!isNonAstroCorruptionRun) ship.makeInvulnerable()

                        // Revenge Protocol: trigger on ship damage
                        val revengeStacksContact = state.passiveStacks["revenge_protocol"] ?: 0
                        if (revengeStacksContact > 0) {
                            state.revengeTimer = revengeStacksContact * 2f
                            state.revengeActive = true
                        }

                        if (ship.health <= 0) {
                            handlePlayerDeath()
                        }

                        // Radio: big hit / low health
                        if (ship.health > 0f) {
                            val healthLost = healthBefore - ship.health
                            if (healthLost > ship.maxHealth * 0.25f) {
                                radioSystem.onBigHit(state)
                            } else if (ship.health < ship.maxHealth * 0.20f) {
                                radioSystem.onLowHealth(state)
                            }
                        }
                    }
                    // Enemy also takes damage from collision (always on-screen since touching ship)
                    if (isOnScreen(enemy) && enemy.takeDamage(30f)) {
                        onEnemyDestroyed(enemy)
                        trackEnemyKill()
                    }
                    break
                }
            }
        }

        // Process score pickup collection (instant, doesn't pause game)
        for (scorePickup in collisionResult.scorePickupsCollected) {
            state.score += scorePickup.scoreValue
            val yenScale = if (scorePickup.isFromEnemy) 1f else state.getAsteroidYenScale()
            val yenAmount = (scorePickup.scoreValue * GameConfig.YEN_BASE_RATE *
                state.getYenMultiplier() * state.dropRateMultiplier * yenScale).toInt()
            state.goldCollected += yenAmount
            if (scorePickup.isFromEnemy) state.telemetryYenFromEnemies += yenAmount
            else state.telemetryYenFromAsteroids += yenAmount
            if (!state.yenMilestoneReached && state.goldCollected >= 50000) {
                state.yenMilestoneReached = true
                radioSystem.onYenMilestone(state)
            }
            SoundManager.playSFX("sfx_yen_pickup", 0.3f)
            // Add small visual feedback
            visualEffects.addHitFlash(
                scorePickup.position.x,
                scorePickup.position.y,
                10f,
                if (scorePickup.isFromEnemy) 0xFF00FFFF.toInt() else 0xFFFFDD44.toInt()
            )
        }

        // Process evolution diamond collection (opens evolution selection)
        if (collisionResult.evolutionDiamondCollected != null) {
            handleEvolutionDiamondCollected()
        }

        // Process upgrade power-up collection (opens selection screen)
        if (collisionResult.powerUpCollected != null) {
            handlePowerUpCollected(collisionResult.powerUpCollected)
        }

        // Fade out any pickups still on the field the moment the player becomes maxed out.
        // "Maxed out" is broader than isFullyUpgraded(): it also covers the early game where
        // you can't hold 4 weapons/4 passives yet because the rest aren't unlocked, so every
        // slot you *can* fill is already filled and maxed (see LootSystem.hasAvailableUpgrades).
        if (!wasFullyUpgraded) {
            val nowMaxedOut = !lootSystem.hasAvailableUpgrades()
            if (nowMaxedOut) {
                for (powerUp in powerUps) {
                    if (powerUp.isActive &&
                        (powerUp.type == PowerUpType.WEAPON || powerUp.type == PowerUpType.PASSIVE)) {
                        powerUp.startFadeOut()
                    }
                }
                wasFullyUpgraded = true
            }
        }

        // Update ship evolution visuals
        shipEvolutionSystem.updateShipVisuals(ship, state)

        // Update visual effects
        visualEffects.update(deltaTime)
        enemyExplosions.forEach { it.update(deltaTime) }
        enemyExplosions.removeAll { !it.isActive }

        // Cleanup inactive entities
        EntityPools.cleanupInactive()

        // Despawn entities that are too far from camera
        despawnFarEntities()

        // Update fading asteroid trails
        fadingTrails.forEach { it.fadeTimer -= deltaTime }
        fadingTrails.removeAll { it.fadeTimer <= 0f }

        // Crystal power regen
        if (state.hasCrystalPowers) {
            ship.health = (ship.health + state.crystalHealthRegen * deltaTime).coerceAtMost(ship.maxHealth)
            if (ship.maxShield > 0f) {
                ship.currentShield = (ship.currentShield + state.crystalShieldRegen * deltaTime).coerceAtMost(ship.maxShield)
            }
        }

        // Crystal energy shield — contact damage aura
        if (state.hasCrystalPowers && ship.isActive) {
            state.crystalShieldTickTimer -= deltaTime
            if (state.crystalShieldTickTimer <= 0f) {
                state.crystalShieldTickTimer += 0.125f  // 8 ticks per second
                val shieldRadius = 60f
                val shieldDamage = 50f

                // Damage nearby enemies
                for (enemy in activeEnemies) {
                    if (!enemy.isActive || enemy.isCrewmate) continue
                    if (!isOnScreen(enemy)) continue  // Can't damage off-screen enemies
                    val dx = ship.position.x - enemy.position.x
                    val dy = ship.position.y - enemy.position.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < shieldRadius + enemy.radius) {
                        val (finalDamage, isCrit) = applyDamageModifiers(shieldDamage)
                        state.telemetryDamageByWeapon["crystal_aura"] = (state.telemetryDamageByWeapon["crystal_aura"] ?: 0f) + finalDamage
                        state.telemetryTotalDamageDealt += finalDamage
                        if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }
                        val destroyed = enemy.takeDamage(finalDamage)
                        visualEffects.addDamageNumber(enemy.position.x, enemy.position.y, finalDamage.toInt(), CrystalPalette.MID, isCrit)
                        if (destroyed) {
                            onEnemyDestroyed(enemy)
                            trackEnemyKill()
                        }
                    }
                }
                // Damage nearby asteroids
                for (asteroid in activeAsteroids) {
                    if (!asteroid.isActive) continue
                    val dx = ship.position.x - asteroid.position.x
                    val dy = ship.position.y - asteroid.position.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < shieldRadius + asteroid.radius) {
                        val (finalDamage, isCrit) = applyDamageModifiers(shieldDamage)
                        state.telemetryDamageByWeapon["crystal_aura"] = (state.telemetryDamageByWeapon["crystal_aura"] ?: 0f) + finalDamage
                        state.telemetryTotalDamageDealt += finalDamage
                        if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }
                        val destroyed = asteroid.takeDamage(finalDamage)
                        visualEffects.addDamageNumber(asteroid.position.x, asteroid.position.y, finalDamage.toInt(), CrystalPalette.MID, isCrit)
                        if (destroyed) {
                            handleAsteroidDestroyedWithTrail(asteroid)
                        }
                    }
                }
                // Boss is immune to all player damage — defeated only by the scripted TB-26 ram.
                // The crystal aura intentionally does nothing to it (no phantom damage numbers / dodges).
            }
        }

        // Update crystal afterimages (laser sight + railgun shot, multiple simultaneous)
        val afterimageIter = state.crystalAfterimages.iterator()
        while (afterimageIter.hasNext()) {
            val afterimage = afterimageIter.next()
            afterimage.timer -= deltaTime

            // Update target position from tracked entity
            if (afterimage.hasTarget && afterimage.targetEntity != null) {
                val target = afterimage.targetEntity!!
                if (target.isActive) {
                    afterimage.targetX = target.position.x
                    afterimage.targetY = target.position.y
                } else {
                    // Target died — find new target
                    afterimage.targetEntity = null
                    findAfterimageTarget(afterimage)
                }
            }

            // Calculate rotation toward locked target
            if (afterimage.hasTarget) {
                val dx = afterimage.targetX - afterimage.x
                val dy = afterimage.targetY - afterimage.y
                afterimage.rotation = atan2(dy, dx)
            }

            if (afterimage.timer <= 0f) {
                // Fire railgun shot at target
                if (afterimage.hasTarget) {
                    val dx = afterimage.targetX - afterimage.x
                    val dy = afterimage.targetY - afterimage.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val speed = 2000f  // Railgun speed
                    val projectile = EntityPools.projectiles.obtain()
                    projectile.initialize(
                        x = afterimage.x,
                        y = afterimage.y,
                        vx = dx / dist * speed,
                        vy = dy / dist * speed,
                        projectileType = ProjectileType.RECALL_SHOT,
                        projectileDamage = 80f,  // Base railgun damage
                        projectileLifetime = 4f
                    )
                    projectile.weaponId = "railgun"
                    projectile.length = 20f
                    projectile.width = 3f
                    projectile.color = Boss.CORRUPTION_COLOR
                }
                afterimage.fired = true
                afterimageIter.remove()
            }
        }

        // Radio system: tick cooldowns and display timers
        radioSystem.update(deltaTime, state)

        // Zap beam effect timer
        if (state.zapBeamActive) {
            state.zapBeamTimer += deltaTime
            if (state.zapBeamTimer >= 0.5f) {
                state.zapBeamActive = false
            }
        }

        // Update crystal zap effect
        tickCrystalZap(deltaTime)
    }

    /** Begin the engine-restart beat: sputter -> catch -> launch ramp. */
    private fun startEngineRestart() {
        engineRestarting = true
        engineRestartTimer = 0f
        engineSputterToggleTimer = 0f
        engineCatchFired = false
        engineStruggleLineFired = false
    }

    /**
     * Drives the engine restart for the protagonist-Astro (player ship in the Sacrifice run,
     * Past Astro in the Corruption run). Thrusters are velocity-driven, so the sputter is
     * produced by short velocity kicks; the launch ramp is applied in the autopilot blocks via
     * FleetSystem.engineRestartSpeedScale(engineRestartTimer).
     */
    private fun updateEngineRestart(deltaTime: Float) {
        if (!engineRestarting) return
        val astro: com.astroloop.game.entity.Entity? =
            if (state.bossFightPhase < PHASE_OTHER_SPAWN) ship else crewmateEncounter.crewmateShip
        engineRestartTimer += deltaTime

        // Struggle line on the sputter (uniform speaker across both runs).
        if (!engineStruggleLineFired && engineRestartTimer >= 0.3f) {
            engineStruggleLineFired = true
            radioSystem.showScriptedMessage(state, "ASTRO", "Come on—",
                PilotDefinitions.getPilot("pilot_astro")!!.color)
        }

        if (engineRestartTimer < FleetSystem.ENGINE_SPUTTER_DURATION) {
            // Sputter: flick the engine on in short bursts so the thruster stutters.
            engineSputterToggleTimer -= deltaTime
            if (engineSputterToggleTimer <= 0f) {
                engineSputterToggleTimer = 0.12f
                astro?.let {
                    it.velocity.x = cos(it.rotation) * 70f   // > 20px/s thruster threshold
                    it.velocity.y = sin(it.rotation) * 70f
                }
            }
        } else if (!engineCatchFired) {
            // Catch: engines bite — flash, sound, and (Sacrifice) control returns here.
            engineCatchFired = true
            astro?.let {
                visualEffects.addExplosion(
                    it.position.x - cos(it.rotation) * it.radius,
                    it.position.y - sin(it.rotation) * it.radius,
                    it.radius * 1.2f, 0xFFFF9933.toInt()
                )
            }
            SoundManager.playSFX("sfx_launch", 1f)
            if (state.bossFightPhase < PHASE_OTHER_SPAWN) state.bossEmpFired = false
        }

        if (engineRestartTimer >= FleetSystem.ENGINE_SPUTTER_DURATION + FleetSystem.ENGINE_RAMP_DURATION) {
            engineRestarting = false
        }
    }

    private fun despawnFarEntities() {
        val despawnDist = GameConfig.ENTITY_DESPAWN_DISTANCE
        val mineDespawnDist = 1500f  // Mines despawn at 1500f from player

        // Despawn far asteroids
        for (asteroid in activeAsteroids) {
            if (camera.isTooFar(asteroid.position.x, asteroid.position.y, despawnDist)) {
                asteroid.isActive = false
            }
        }

        // Despawn far power-ups with fade-out effect
        for (powerUp in activePowerUps) {
            if (camera.isTooFar(powerUp.position.x, powerUp.position.y, despawnDist)) {
                // Add small poof effect when despawning
                addDespawnEffect(powerUp.position.x, powerUp.position.y, 0xFFFFDD44.toInt())
                powerUp.isActive = false
            }
        }

        // Despawn far enemies (use larger distance since they're important)
        val enemyDespawnDist = GameConfig.ENEMY_DESPAWN_DISTANCE
        for (enemy in activeEnemies) {
            if (enemy.isCrewmate) continue  // Don't despawn crewmate encounters
            if (camera.isTooFar(enemy.position.x, enemy.position.y, enemyDespawnDist)) {
                enemyWeaponSystem.onEnemyRemoved(enemy)
                enemy.isActive = false
            }
        }

        // Despawn mines that are too far from player (they have infinite lifetime)
        for (projectile in activeProjectiles) {
            if (projectile.type == ProjectileType.MINE) {
                val dx = projectile.position.x - ship.position.x
                val dy = projectile.position.y - ship.position.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > mineDespawnDist) {
                    // Add poof effect for despawning mines
                    addDespawnEffect(projectile.position.x, projectile.position.y, projectile.color)
                    projectile.isActive = false
                }
            }
        }
    }

    private fun isOnScreen(entity: Entity): Boolean {
        val margin = 50f  // Small margin so enemies near screen edge can still be hit
        val cameraX = ship.position.x - screenWidth / 2f
        val cameraY = ship.position.y - screenHeight / 2f
        return entity.position.x >= cameraX - margin &&
               entity.position.x <= cameraX + screenWidth + margin &&
               entity.position.y >= cameraY - margin &&
               entity.position.y <= cameraY + screenHeight + margin
    }

    private fun spawnDamageZone(x: Float, y: Float, radius: Float, damage: Float, duration: Float, color: Int) {
        val p = EntityPools.projectiles.obtain()
        p.initialize(x, y, 0f, 0f, ProjectileType.PLASMA, damage, duration)
        p.weaponId = "zone"
        p.radius = radius * state.areaMultiplier
        p.piercing = true
        p.maxPierces = 999
        p.color = color
    }

    private fun trackEnemyKill() {
        currentKillStreak++
        if (currentKillStreak > bestKillStreakThisRun) {
            bestKillStreakThisRun = currentKillStreak
        }
        lastKillTime = state.survivalTime
        enemiesKilledThisRun++
        if (currentKillStreak == 3) {
            radioSystem.onKillStreak(state)
        }
    }

    private fun captureTrailFade(asteroid: Asteroid) {
        if (asteroid.type == AsteroidType.TRAIL && asteroid.trailPoints.size >= 2) {
            val trailLifetime = asteroid.getTrailLifetime()
            val latestTime = asteroid.trailPoints.last().timestamp
            val points = asteroid.trailPoints.map { pt ->
                val age = latestTime - pt.timestamp
                val alpha = (1f - age / trailLifetime).coerceIn(0f, 1f)
                Triple(pt.x, pt.y, alpha)
            }
            fadingTrails.add(FadingTrail(points, asteroid.getTrailWidth()))
        }
    }

    private fun handleAsteroidDestroyedWithTrail(asteroid: Asteroid) {
        captureTrailFade(asteroid)
        SoundManager.playSFX("sfx_asteroid_break", volume = 0.5f)
        state.telemetryAsteroidsDestroyed++
        lootSystem.handleAsteroidDestroyed(asteroid)
    }

    private fun applyDamageModifiers(baseDamage: Float): Pair<Float, Boolean> {
        var damage = baseDamage
        var isCrit = false
        // Momentum Drive: bonus damage while ship is moving
        if (state.momentumDamageBonus > 0f && ship.velocity.lengthSquared() > 100f) {
            damage *= (1f + state.momentumDamageBonus)
        }
        // Lucky Rounds: crit chance
        if (state.rollCrit()) {
            damage *= GameConfig.CRIT_DAMAGE_MULTIPLIER
            isCrit = true
        }
        return Pair(damage, isCrit)
    }

    private fun applyGamblersMineEffect(x: Float, y: Float) {
        // 5% jackpot per mine detonation. On the other 95% the mine just does its
        // normal explosion (the projectile's own explodeOnDeath handles that).
        if (kotlin.random.Random.nextFloat() >= 0.05f) return

        // Jackpot: a big blast + yen shower. The blast is large and does real AoE
        // damage to enemies AND asteroids (weapon parity — no enemy-only effects).
        val blastRadius = 220f
        val blastDamage = 200f
        visualEffects.addExplosion(x, y, blastRadius, 0xFFFFCC00.toInt())

        for (enemy in activeEnemies) {
            if (!enemy.isActive || enemy.isCrewmate) continue
            val dx = enemy.position.x - x
            val dy = enemy.position.y - y
            if (dx * dx + dy * dy < blastRadius * blastRadius) {
                state.telemetryDamageByWeapon["jackpot_mines"] = (state.telemetryDamageByWeapon["jackpot_mines"] ?: 0f) + blastDamage
                state.telemetryTotalDamageDealt += blastDamage
                val destroyed = enemy.takeDamage(blastDamage)
                visualEffects.addDamageNumber(enemy.position.x, enemy.position.y - enemy.radius, blastDamage.toInt(), 0xFFFFCC00.toInt())
                if (destroyed) {
                    onEnemyDestroyed(enemy)
                    trackEnemyKill()
                }
            }
        }
        for (asteroid in activeAsteroids) {
            if (!asteroid.isActive) continue
            val dx = asteroid.position.x - x
            val dy = asteroid.position.y - y
            if (dx * dx + dy * dy < blastRadius * blastRadius) {
                state.telemetryDamageByWeapon["jackpot_mines"] = (state.telemetryDamageByWeapon["jackpot_mines"] ?: 0f) + blastDamage
                state.telemetryTotalDamageDealt += blastDamage
                val destroyed = asteroid.takeDamage(blastDamage)
                visualEffects.addDamageNumber(asteroid.position.x, asteroid.position.y - asteroid.radius, blastDamage.toInt(), 0xFFFFCC00.toInt())
                if (destroyed) {
                    handleAsteroidDestroyedWithTrail(asteroid)
                }
            }
        }

        // Yen shower — 10 normal-size yellow star-dust yen (fromEnemy = false),
        // NOT the confusing cyan diamonds. Scattered within ~+/-70f.
        repeat(10) {
            val ox = (kotlin.random.Random.nextFloat() - 0.5f) * 140f
            val oy = (kotlin.random.Random.nextFloat() - 0.5f) * 140f
            val pickup = EntityPools.powerUps.obtain()
            pickup.initializeAsScorePickup(
                x = x + ox, y = y + oy,
                score = 10,
                fromEnemy = false
            )
        }
        visualEffects.addDamageNumber(x, y - 20f, 0, 0xFFFFCC00.toInt(), isCrit = true, label = "JACKPOT")
    }

    private fun onEnemyDestroyed(enemy: EnemyShip) {
        enemyWeaponSystem.onEnemyRemoved(enemy)
        if (enemy.isCrewmate) {
            // No loot, no explosion — CrewmateEncounter.update() detects death via health check
            enemy.isActive = false
            return
        }
        // Death explosion with colored debris matching ship color
        val ex = ShipExplosion()
        ex.start(
            enemy.position.x, enemy.position.y,
            enemy.velocity.x, enemy.velocity.y,
            enemy.rotation, enemy.radius, enemy.color
        )
        enemyExplosions.add(ex)
        SoundManager.playSFX("sfx_enemy_death", volume = 0.4f)
        lootSystem.handleEnemyDestroyed(enemy)
    }

    private fun addDespawnEffect(x: Float, y: Float, color: Int) {
        // Small particle burst / fade-out effect (duration 0.2s)
        visualEffects.addHitFlash(x, y, 12f, color)
    }

    private fun tryEvade(state: GameState): Boolean {
        if (state.passivesDisabled) return false
        val evasionChance = state.evasionChance
        if (evasionChance > 0 && kotlin.random.Random.nextFloat() < evasionChance) {
            // Dodge successful - trigger visual feedback
            state.lastDodgeTime = state.survivalTime
            state.telemetryDodges++
            return true
        }
        return false
    }

    private fun tryCrystalDodge(): Boolean {
        if (state.passivesDisabled || !state.hasCrystalPowers || ship.isInvulnerable) return false

        // Crystal power: add new afterimage with laser sight + railgun shot (no cap)
        val afterimage = GameState.CrystalAfterimage(
            x = ship.position.x,
            y = ship.position.y,
            rotation = ship.rotation,
            timer = 3.0f,
            aiming = true,
            hasTarget = false
        )

        // Find nearest target ONCE (locked, no retargeting)
        findAfterimageTarget(afterimage)

        state.crystalAfterimages.add(afterimage)

        ship.makeInvulnerable(0.5f)
        return true
    }

    private fun findAfterimageTarget(afterimage: GameState.CrystalAfterimage) {
        var closestDist = Float.MAX_VALUE
        var foundEntity: Entity? = null

        // Prefer enemies
        for (enemy in activeEnemies) {
            if (!enemy.isActive) continue
            val dx = enemy.position.x - afterimage.x
            val dy = enemy.position.y - afterimage.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < closestDist) {
                closestDist = dist
                foundEntity = enemy
            }
        }

        // Fallback to asteroids
        if (foundEntity == null) {
            for (asteroid in activeAsteroids) {
                if (!asteroid.isActive) continue
                val dx = asteroid.position.x - afterimage.x
                val dy = asteroid.position.y - afterimage.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < closestDist) {
                    closestDist = dist
                    foundEntity = asteroid
                }
            }
        }

        if (foundEntity != null) {
            afterimage.targetEntity = foundEntity
            afterimage.targetX = foundEntity.position.x
            afterimage.targetY = foundEntity.position.y
            afterimage.hasTarget = true
        } else {
            afterimage.hasTarget = false
            afterimage.targetEntity = null
        }
    }

    /** Play shield-hit, shield-break, or player-hit sound based on shield state before/after damage. */
    private fun playDamageSound(shieldBefore: Float, shieldAfter: Float) {
        if (shieldBefore > 0f) {
            SoundManager.playSFX("sfx_shield_hit", volume = 0.5f)
        } else {
            SoundManager.playSFX("sfx_player_hit")
        }
    }

    private fun vibrateHit() {
        if (isVibrationMuted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 40))
        }
    }

    private fun vibrateExplosion() {
        if (isVibrationMuted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, 150))
        }
    }

    // =========================================================================
    // Crystal Reckoning opening (Task 9) — empty field + Astro monologue
    // =========================================================================

    private fun queueReckoningLine(speaker: String, text: String, color: Int, delay: Float) {
        pendingReckoningSpeaker = speaker
        pendingReckoningLine = text
        pendingReckoningColor = color
        pendingReckoningTimer = delay
    }

    private fun tickPendingReckoningLine(dt: Float) {
        val line = pendingReckoningLine ?: return
        pendingReckoningTimer -= dt
        if (pendingReckoningTimer <= 0f) {
            pendingReckoningLine = null
            radioSystem.showScriptedMessage(state, pendingReckoningSpeaker, line, pendingReckoningColor)
        }
    }

    private fun startReckoningOpening() {
        // Clear the field so the opening runs in empty space (no asteroids or enemies)
        EntityPools.asteroids.freeAll()
        EntityPools.enemies.freeAll()
        activeAsteroids.clear()
        activeEnemies.clear()
        pendingReckoningLine = null

        // First attempt plays the full monologue and arms the skip; every retry after a death
        // jumps straight to the closing "Time to close it." line. Winning sets crystal_released
        // and blocks re-entry, so this flag only ever affects retries.
        val persistence = PersistenceManager(context)
        state.reckoningSkipOpening = persistence.isReckoningAttempted()
        if (!state.reckoningSkipOpening) persistence.setReckoningAttempted(true)
        persistence.incrementReckoningRounds()   // counts walk-outs — every entry, win or lose

        state.reckoningActive = true
        state.reckoningStage = GameState.ReckoningStage.OPENING
        state.reckoningTimer = 0f
    }

    private fun updateReckoningOpening(dt: Float) {
        // Player flies normally (joystick → ship), no weapon firing
        ship.moveDirection.set(touchController.moveDirection)
        ship.moveDirection.mul(touchController.moveMagnitude)
        movementSystem.updateShip(ship, state, dt)

        camera.update(ship)
        starfieldRenderer.updateWithCamera(camera)
        if (state.graceTimer > 0f) state.graceTimer -= dt
        visualEffects.update(dt)
        state.survivalTime += dt

        radioSystem.update(dt, state)
        tickPendingReckoningLine(dt)

        val prev = state.reckoningTimer
        state.reckoningTimer += dt
        val t = state.reckoningTimer

        // Retry after a failed attempt: only the closing line, then straight into the fight —
        // no re-listening to the whole monologue.
        if (state.reckoningSkipOpening) {
            val closingLine = CrystalFightLines.opening.last().second  // "Time to close it."
            if (prev < 1.5f && t >= 1.5f) {
                radioSystem.showScriptedMessage(state, "ASTRO", closingLine, astroRadioColor)
            }
            if (t >= 6f) {  // ~4.5s read time on the one line, then the boss entrance
                SoundManager.startReckoningBGM(context)
                startReckoningFight()
            }
            return
        }

        // Three two-part Astro lines at ~3s/10s/17s (part 2 follows +3.5s) — each part
        // needs its full read time; tighter spacing replaced lines before they were read.
        for ((i, pair) in CrystalFightLines.opening.withIndex()) {
            val at = 3f + i * 7f
            if (prev < at && t >= at) {
                radioSystem.showScriptedMessage(state, "ASTRO", pair.first, astroRadioColor)
                queueReckoningLine("ASTRO", pair.second, astroRadioColor, 3.5f)
            }
        }

        // At ~24s (last line lands at 20.5s + read time): reuse the 10-minute boss-spawn
        // transition (BGM only — the entrance SFX was removed from all boss entrances as
        // too strong), then enter fight
        if (t >= 24f) {
            SoundManager.startReckoningBGM(context)
            startReckoningFight()
        }
    }

    // =========================================================================
    // Crystal Reckoning fight (Task 7)
    // =========================================================================

    private fun startReckoningFight() {
        val boss = Boss()
        // 0.30 × screenHeight: upper-middle of the (ship-centered) view — the full drawn
        // extent stays on screen at spawn on any phone aspect, and the player's opening
        // run restores fight distance immediately.
        boss.initialize(ship.position.x, ship.position.y - screenHeight * 0.30f, ship)
        boss.radius = Boss.BOSS_SIZE * 2f   // 2× oversize
        // Stable emitter (Touhou): no lunges or strafe jitter — the spirals are the fight
        boss.holdPattern = true
        reckoningBoss = boss

        // Entrance: crystal zap burst covers the boss materialization.
        // No entrance SFX — stings were deliberately removed from all boss entrances.
        state.crystalZapActive = true
        state.crystalZapTimer = 0f
        crystalRenderer.activateZap(boss.position.x, boss.position.y)
        vibrateExplosion()

        crystalFightSystem.reset()
        state.reckoningActive = true
        state.reckoningStage = GameState.ReckoningStage.FIGHT
        state.reckoningTimer = 0f
        state.bossActive = true          // red arena via starfieldRenderer.bossMode
        state.weaponsDisabled = true
        state.passivesDisabled = true
        ship.health = ship.maxHealth
    }

    private fun tickCrystalZap(dt: Float) {
        if (!state.crystalZapActive) return
        state.crystalZapTimer += dt
        crystalRenderer.updateZap(dt)
        if (!crystalRenderer.zapActive) state.crystalZapActive = false
    }

    private fun updateReckoningFight(dt: Float) {
        val boss = reckoningBoss ?: return
        radioSystem.update(dt, state)
        tickPendingReckoningLine(dt)
        tickCrystalZap(dt)

        val projectiles = EntityPools.projectiles.getAllInUse()

        // 1) Player flies normally (joystick → ship), NO firing (weapons disabled).
        ship.moveDirection.set(touchController.moveDirection)
        ship.moveDirection.mul(touchController.moveMagnitude)
        movementSystem.updateShip(ship, state, dt)

        camera.update(ship)
        starfieldRenderer.updateWithCamera(camera)
        if (state.graceTimer > 0f) state.graceTimer -= dt
        visualEffects.update(dt)
        state.survivalTime += dt

        // 2) Boss chases (real Boss AI) — suppress its own weapon; fight system fires instead.
        boss.update(dt)
        boss.wantsToFire = false

        // 3) Emit the phase's bullets from the moving boss; phase-change caption.
        // Each bullet voices its own soft tick in spawnCrystalBullet — the sound is driven by
        // the bullets themselves, not a parallel clock, so it can never desync from the fire.
        val burst = crystalFightSystem.update(boss.position.x, boss.position.y, dt)
        for (s in burst) spawnCrystalBullet(s)
        crystalFightSystem.phaseChanged?.let { ph ->
            val (first, second) = CrystalFightLines.taunt(ph)
            radioSystem.showScriptedMessage(state, CRYSTAL_CALLSIGN, first, CrystalPalette.MID)
            // P5 is a single scream — no measured follow-up.
            if (second != null) queueReckoningLine(CRYSTAL_CALLSIGN, second, CrystalPalette.MID, 3.5f)
        }

        // 4) Move/age bullets.
        movementSystem.updateProjectiles(projectiles, dt)

        // 5) Bullets damage the player (death → retreat/crystal-freeze → hangar).
        checkEnemyProjectileHitsOnPlayer(projectiles)
        // Death: startRetreat() sets retreatPhase > 0 (astroLoopMode), or phase changes on gameOver.
        // Either way, clear reckoning so the dispatch doesn't re-enter next frame.
        // weaponsDisabled/passivesDisabled stay LOCKED through the retreat/death play-out:
        // the retreat runs through the normal updatePlaying path, and re-enabling here made the
        // combat drone pop in and weapons rearm mid-fly-off (Astro's kit still holds the tb26
        // passive; only the flags bench it). GameState.reset() clears both on the next run.
        if (state.retreatPhase > 0 || state.phase != GamePhase.PLAYING) {
            // A failed attempt is NOT a run — flag it so the bar skips the survived/best report.
            PersistenceManager(context).setReckoningJustLost(true)
            state.reckoningActive = false
            state.reckoningStage = GameState.ReckoningStage.NONE
            // The boss glides to the centre of the now-frozen view and plants itself there,
            // rotation still tracking the fleeing ship (Boss.update aims before the centre
            // glide, and again once stunned; ticked from updatePlaying during the retreat).
            // The camera freezes for the fly-off, so the planted boss holds centre-screen
            // while the ship flies out the bottom — no blink-out. Cleared at next run init.
            boss.startCenterApproach(camera.x + screenWidth / 2f, camera.y + screenHeight / 2f)
            // Parting taunt a beat after Astro's retreat_home line (fired by startRetreat)
            queueReckoningLine(CRYSTAL_CALLSIGN, "I'll be waiting.", CrystalPalette.MID, 2.5f)
            return
        }

        // 6) Cull spent bullets, sync render list. Expired bullets get the pulse-cannon-style
        // end-of-life spark (the reckoning skips ProjectileEffectsSystem, which normally does
        // this) — nothing may simply vanish.
        for (p in projectiles) {
            if (!p.isActive) {
                if (p.expiredNaturally) {
                    visualEffects.addHitFlash(p.position.x, p.position.y, 16f, p.color)
                    p.expiredNaturally = false
                }
                EntityPools.projectiles.free(p)
            }
        }
        EntityPools.projectiles.getActiveEntities(activeProjectiles)

        // 7) Survival → win.
        if (crystalFightSystem.survived) onReckoningSurvived()
    }

    private fun spawnCrystalBullet(s: BulletSpec) {
        val p = EntityPools.projectiles.obtain()
        // Lifetime 20s is a pure safety net: at 500-900 px/s every layer exits the screen and
        // dies to the off-screen camera cull (ENTITY_DESPAWN_DISTANCE) long before expiry, so
        // bullets never pop mid-screen — the expiry spark remains only as a fallback.
        p.initialize(s.x, s.y, s.vx, s.vy, ProjectileType.BULLET, CrystalFightSystem.BULLET_DAMAGE, 20f)
        p.isEnemyProjectile = true
        p.radius = CrystalFightSystem.BULLET_RADIUS
        p.color = s.color
        // One soft tick per bullet, pitch-jittered so simultaneous same-frame arms decorrelate
        // into a swarm shimmer instead of one louder blip.
        val rate = 1f + (kotlin.random.Random.nextFloat() - 0.5f) * 2f * RECKONING_BULLET_PITCH_JITTER
        SoundManager.playSFX("sfx_reckoning_bullet", RECKONING_BULLET_VOLUME, rate)
    }

    private fun onReckoningSurvived() {
        state.weaponsDisabled = false
        state.passivesDisabled = false
        state.reckoningStage = GameState.ReckoningStage.LANCE

        // Strip: the sequencer stops with the FIGHT stage; drop the bare bed under the
        // ghost dialogue (0.35 of full — tuned on device, see the design spec).
        SoundManager.duckBossBGM(0.35f)

        // Fade the in-flight crystal bullets out (0.5s) rather than clearing them —
        // an instant freeAll blinked a full bullet-hell field away in one frame.
        fadeAllProjectiles()

        // Start the ghost-ship lance — keep reckoningBoss alive for render until burstFired
        val boss = reckoningBoss ?: return
        reckoningGhostStep = 0
        reckoningGhostTimer = 0f
        reckoningLanceDoneHold = 0f
        reckoningFlyOffComplete = false
        reckoningExitFadeTimer = 0f
        // Astro cuts the engine and turns to watch the ghosts gather at the crystal —
        // the climax belongs to the crew (thrust only renders above 20px/s).
        reckoningWatchX = boss.position.x
        reckoningWatchY = boss.position.y
        ship.velocity.set(0f, 0f)
        ghostLance.start(boss.position.x, boss.position.y)
        pendingReckoningLine = null
    }

    /** Steer the ship's nose toward [targetAngle] at the retreat's turn rate. */
    private fun turnShipToward(targetAngle: Float, dt: Float) {
        var rotDiff = targetAngle - ship.rotation
        while (rotDiff < -Math.PI.toFloat()) rotDiff += (2 * Math.PI).toFloat()
        while (rotDiff > Math.PI.toFloat()) rotDiff -= (2 * Math.PI).toFloat()
        ship.rotation += rotDiff * (4f * dt).coerceAtMost(1f)
    }

    private fun updateReckoningLance(dt: Float) {
        // Fly-home begins after the shatter has had its hold; from then the camera freezes
        // (retreat-style) so the ship visibly leaves the view heading south.
        val flyingHome = ghostLance.stage == com.astroloop.game.render.GhostShipLance.Stage.DONE &&
                         reckoningLanceDoneHold >= 1.5f
        if (!flyingHome) camera.update(ship)
        starfieldRenderer.updateWithCamera(camera)
        if (state.graceTimer > 0f) state.graceTimer -= dt
        visualEffects.update(dt)

        // Keep ticking the fading fight bullets so the fade-out actually plays
        val projectiles = EntityPools.projectiles.getAllInUse()
        movementSystem.updateProjectiles(projectiles, dt)
        for (p in projectiles) if (!p.isActive) EntityPools.projectiles.free(p)
        EntityPools.projectiles.getActiveEntities(activeProjectiles)

        radioSystem.update(dt, state)
        tickPendingReckoningLine(dt)

        ghostLance.update(dt)

        // Drive the ghost script off the lance clock. Astro's silence breaks ONLY for his crew —
        // the crystal never gets a word from him. The last line ("Go.") RELEASES the ghosts:
        // the crystal shatters because they leave, not because it was hit.
        reckoningGhostTimer += dt
        while (reckoningGhostStep < CrystalFightLines.ghostScript.size &&
               reckoningGhostTimer >= CrystalFightLines.ghostScript[reckoningGhostStep].third) {
            val (speaker, line, _) = CrystalFightLines.ghostScript[reckoningGhostStep]
            val isCrew = speaker != "CRYSTAL" && speaker != "ASTRO"
            val color = when (speaker) {
                "CRYSTAL" -> CrystalPalette.MID
                "ASTRO"   -> astroRadioColor
                else      -> PilotDefinitions.pilots.find { it.callsign == speaker }?.color
                             ?: CrystalPalette.ICE
            }
            radioSystem.showScriptedMessage(state, speaker, line, color, isGhost = isCrew)
            if (line == CrystalFightLines.GHOST_RELEASE_LINE) {
                SoundManager.playSFX("sfx_ghost_lance")
                ghostLance.release()
            }
            reckoningGhostStep++
        }

        // Once burst fires, the boss has shattered — clear it so the render stub stops drawing
        if (ghostLance.burstFired && reckoningBoss != null) {
            SoundManager.playSFX("sfx_crystal_shatter")
            SoundManager.fadeOutBossBGM()
            reckoningBoss?.let { rb ->
                // Shatter: crystal-colored burst so the boss never just vanishes
                visualEffects.addExplosion(rb.position.x, rb.position.y, rb.radius * 1.5f, CrystalPalette.MID)
            }
            reckoningBoss = null
        }

        // Astro is a spectator until the fly-home: engine dark (zeroed velocity draws no
        // thrust), nose tracking the gathering — through the release and the shatter.
        if (!flyingHome) {
            ship.velocity.set(0f, 0f)
            turnShipToward(
                kotlin.math.atan2(reckoningWatchY - ship.position.y, reckoningWatchX - ship.position.x),
                dt
            )
        }

        // Lance complete — hold a beat so the shatter and trails clear, then fly home: turn
        // south and leave the frozen view like a retreat (no emergency shield — nothing out
        // here anymore), and only then fade to black. No farewell screen, no timeline shift:
        // the crew settled it on the radio, so the win is the flight out and the bar fading
        // back in on the other side.
        if (ghostLance.stage == com.astroloop.game.render.GhostShipLance.Stage.DONE) {
            reckoningLanceDoneHold += dt
            if (flyingHome) {
                if (!reckoningFlyOffComplete) {
                    turnShipToward((Math.PI / 2).toFloat(), dt)
                    val speed = 300f   // matches the retreat fly-off
                    ship.position.x += cos(ship.rotation) * speed * dt
                    ship.position.y += sin(ship.rotation) * speed * dt
                    // Real velocity so the thruster lights up for the departure
                    ship.velocity.set(cos(ship.rotation) * speed, sin(ship.rotation) * speed)
                    if (ship.position.y > camera.y + screenHeight + 100f) {
                        reckoningFlyOffComplete = true
                    }
                } else {
                    reckoningExitFadeTimer += dt
                    globalFadeAlpha = (reckoningExitFadeTimer / 1.2f).coerceIn(0f, 1f)
                    if (globalFadeAlpha >= 1f && !reckoningWinHandedOff) {
                        reckoningWinHandedOff = true
                        finishReckoningWin()
                    }
                }
            }
        }
    }

    /**
     * Reckoning fight won: called once, after the lance completed and the scene has faded
     * to black. Persists the win, then hands off to the hangar with the fade-in flag —
     * resetForReturn arms fadeFromBlackTimer so the bar fades in from black instead of
     * cutting. reckoningActive stays set so the (now fully black) lance stage keeps ticking
     * harmlessly until MainActivity swaps the view out on the UI thread.
     */
    private fun finishReckoningWin() {
        // crystal_released blocks any re-trigger; reckoning_just_won is the one-shot
        // bar-chatter flag consumed by ChatSystem.onDeathReturn.
        val persistence = PersistenceManager(context)
        persistence.setCrystalReleased(true)
        persistence.setAwaitingConvergence(false)
        persistence.setReckoningJustWon(true)

        SoundManager.stopAll()
        saveRunStats(includeDeath = false)
        onGameOver(0, true)
    }

    private fun startRetreat() {
        ship.isActive = true
        ship.health = 1f
        ship.makeInvulnerable(999f)
        state.emergencyShieldActive = true
        radioSystem.onRetreat(state)
        state.retreatPhase = 1
        state.retreatTimer = 0f
    }

    private fun handlePlayerDeath() {

        // Already in heart-to-heart transition — don't reset timer
        if (state.bossFightPhase >= PHASE_HEART_TRANSITION) return

        // Boss from other side — explode, then signal heart-to-heart
        if (state.bossFightPhase >= PHASE_OTHER_SPAWN) {
            // Ship explosion (mirrors scripted death in PHASE_OTHER_DYING)
            visualEffects.addExplosion(ship.position.x, ship.position.y,
                Boss.BOSS_SIZE * 3f, 0xFFFF4400.toInt())
            ship.isActive = false
            fleetSystem.stopFiring()
            fleetSystem.arrivalPhase = 0
            state.bossFightPhase = PHASE_HEART_TRANSITION
            state.bossFightTimer = 0f
            state.fleetChatterStep = 0
            state.fleetChatterTimer = 0f
            saveRunStats(includeDeath = false)  // Save stats without death increment
            return  // Don't process normal death
        }

        // Non-Astro corruption run: pilot and ship die permanently
        val persistence = PersistenceManager(context)
        if (StoryStateManager.isCorrupted(persistence) && !state.astroLoopMode && startingPilotId != "pilot_astro") {
            persistence.addDeadPilot(startingPilotId)
            persistence.addDeadShip(startingShipId)
            // Check if all crew are now dead — unlock crystal if so
            if (StoryStateManager.shouldUnlockCrystal(persistence)) {
                persistence.setCrystalUnlocked(true)
            }
            gameOver(skipCrystal = true)
            return
        }

        // Astro corruption run: death is also permanent — crystal does NOT save you
        if (StoryStateManager.isCorrupted(persistence) && !state.astroLoopMode && startingPilotId == "pilot_astro") {
            gameOver(skipCrystal = true)
            return
        }

        // Check for phoenix and that it hasn't been used yet
        if (state.extraLives > 0 && !state.phoenixUsed) {
            // Phoenix Core resurrection - mark as used but keep in slot
            state.phoenixUsed = true
            state.extraLives = 0
            // Don't remove from passiveStacks - it stays but is inactive
            // state.passiveStacks.remove("phoenix_core")  // REMOVED - phoenix stays in slot
            state.recalculateStats()

            // Respawn at full health
            ship.health = ship.maxHealth
            ship.restoreShields()
            ship.isActive = true
            ship.makeInvulnerable(3f) // Longer invulnerability on resurrection

            // Core flash at ship position
            visualEffects.addExplosion(
                ship.position.x,
                ship.position.y,
                200f,
                0xFFFFFFFF.toInt() // White core burst
            )
            // Expanding shockwave that clears the field
            visualEffects.addPhoenixShockwave(ship.position.x, ship.position.y)
            state.phoenixShockwaveActive = true
            state.phoenixShockwaveOriginX = ship.position.x
            state.phoenixShockwaveOriginY = ship.position.y
            state.phoenixShockwavePrevRadius = 0f
            state.phoenixShockwaveRadius = 0f

            state.shieldsDownTriggered = false
            radioSystem.onPhoenixActivate(state)
            SoundManager.playSFX("sfx_phoenix_revive")
        } else if (state.astroLoopMode) {
            startRetreat()
        } else {
            gameOver()
        }
    }

    private fun checkEnemyProjectileHitsOnPlayer(projectiles: List<Projectile>) {
        // Check enemy projectile hits on player
        if (!ship.isInvulnerable) {
            for (projectile in projectiles) {
                if (!projectile.isActive || !projectile.isEnemyProjectile) continue

                if (projectile.collidesWith(ship)) {
                    // Try crystal dodge or evasion first
                    if (!tryCrystalDodge() && !tryEvade(state)) {
                        val healthBefore = ship.health
                        val shieldBefore = ship.currentShield
                        ship.takeDamage(projectile.damage)
                        state.telemetryDamageTakenBy["enemy_projectile"] = (state.telemetryDamageTakenBy["enemy_projectile"] ?: 0f) + projectile.damage
                        state.telemetryTotalDamageTaken += projectile.damage
                        state.lastDamageSource = "enemy_projectile"
                        vibrateHit()
                        playDamageSound(shieldBefore, ship.currentShield)
                        if (!isNonAstroCorruptionRun) ship.makeInvulnerable()

                        // Revenge Protocol: trigger on ship damage
                        val revengeStacks = state.passiveStacks["revenge_protocol"] ?: 0
                        if (revengeStacks > 0) {
                            state.revengeTimer = revengeStacks * 2f
                            state.revengeActive = true
                        }

                        if (ship.health <= 0) {
                            handlePlayerDeath()
                        } else {
                            val healthLost = healthBefore - ship.health
                            if (healthLost > ship.maxHealth * 0.25f) {
                                radioSystem.onBigHit(state)
                            } else if (ship.health < ship.maxHealth * 0.20f) {
                                radioSystem.onLowHealth(state)
                            }
                        }
                    }
                    projectile.isActive = false
                    if (projectile.explodeOnDeath) projectile.expiredNaturally = true
                    break
                }
            }
        }
    }

    private fun handleEvolutionDiamondCollected() {
        if (state.retreatPhase > 0) return
        state.telemetryDiamondsCollected++
        SoundManager.playSFX("sfx_evolution")

        // Only one evolution can ever be active, so any other diamonds on the field are now
        // dead weight — fade them out. The collected diamond is already deactivated by
        // CollisionSystem, so the isActive filter skips it.
        for (powerUp in activePowerUps) {
            if (powerUp.isActive && powerUp.type == PowerUpType.EVOLUTION_DIAMOND) {
                powerUp.startFadeOut()
            }
        }

        upgradeSystem.generateEvolutionOptions(state)
        if (upgradeSystem.hasPendingOptions()) {
            state.phase = GamePhase.UPGRADE_SELECTION
            touchController.consumeTap()
        }
    }

    private fun handlePowerUpCollected(powerUp: PowerUp) {
        if (state.retreatPhase > 0) return
        state.telemetryUpgradeDropsCollected++
        val sourceType = if (powerUp.isFromEnemy) "enemy_drop" else "asteroid_drop"
        state.telemetryPowerupsCollected[sourceType] = (state.telemetryPowerupsCollected[sourceType] ?: 0) + 1
        SoundManager.playSFX("sfx_powerup_pickup", 0.5f)
        if (!state.firstWeaponPickedUp) {
            state.firstWeaponPickedUp = true
            radioSystem.onFirstWeapon(state)
        }

        // Generate upgrade options and show selection UI
        val options = upgradeSystem.generateUpgradeOptions(state, fromAsteroid = !powerUp.isFromEnemy)
        if (options.isEmpty()) return
        state.telemetryLastOfferedOptions = options.map { it.id }
        state.phase = GamePhase.UPGRADE_SELECTION
        touchController.consumeTap()  // Clear any pending tap to prevent auto-selection
        if (state.hasLuckyStar) {
            if (options.size > 1) {
                startLuckyStarAnimation(options.size)
            } else {
                // Single option — auto-apply immediately without animation
                val option = upgradeSystem.selectOption(0)
                if (option != null) {
                    telemetryManager.logUpgradeOffered(state.survivalTime.toInt(), state.telemetryLastOfferedOptions, option.id)
                    applyUpgrade(option)
                }
                state.phase = GamePhase.PLAYING
            }
        }
    }

    private fun startLuckyStarAnimation(optionCount: Int) {
        state.luckyStarAnimating = true
        state.luckyStarTimer = 0f
        state.luckyStarSelectedIndex = kotlin.random.Random.nextInt(optionCount)
        state.luckyStarCurrentHighlight = 0
        state.luckyStarBounceCount = 0
        state.luckyStarTotalBounces = 9 + kotlin.random.Random.nextInt(3)
        state.luckyStarNextBounceTime = 0.1f
        state.luckyStarDimming = false
        state.luckyStarDimTimer = 0f
    }

    private fun updateAstroCorruptionRun(deltaTime: Float) {
        // Chatter system — fires between encounters when in asteroid field
        if (!state.corruptionInBossArena && state.corruptionSpeedUpTriggered) {
            state.corruptionChatterTimer -= deltaTime
            if (state.corruptionChatterTimer <= 0f && state.corruptionChatterIndex < CORRUPTION_CHATTER.size) {
                val line = CORRUPTION_CHATTER[state.corruptionChatterIndex]
                radioSystem.showScriptedMessage(state, "ASTRO", line,
                    PilotDefinitions.getPilot("pilot_astro")!!.color, isBoss = true)
                state.corruptionChatterIndex++
                state.corruptionChatterTimer = CORRUPTION_CHATTER_INTERVAL
            }
        }

        // Check for encounter trigger (game-time threshold)
        if (state.corruptionEncounterIndex < 5 &&
            !state.corruptionInBossArena &&
            state.survivalTime >= state.corruptionNextEncounterTime) {
            enterCorruptionBossArena()
            return
        }

        // Final encounter: past Astro + TB-26 at 10:00
        if (state.survivalTime >= Boss.SPAWN_TIME && state.bossFightPhase == PHASE_NONE &&
            !state.corruptionInBossArena) {
            enterFinalEncounter()
            return
        }

        // Update active encounter
        if (state.corruptionInBossArena && state.bossFightPhase == PHASE_NONE) {
            updateCorruptionEncounter(deltaTime)
        }

        crewmateEncounter.update(state, deltaTime)
    }

    private fun enterCorruptionBossArena() {
        state.corruptionInBossArena = true
        state.bossActive = true  // Triggers red background + red starfield

        // White flash
        visualEffects.addExplosion(ship.position.x, ship.position.y, 800f, 0xFFFFFFFF.toInt())

        // Clear field — white flash covers the transition, but still explode for the rule
        for (asteroid in activeAsteroids) {
            visualEffects.addExplosion(asteroid.position.x, asteroid.position.y, asteroid.radius * 1.5f, 0xFFFFFFFF.toInt())
            asteroid.isActive = false
        }
        for (powerUp in activePowerUps) {
            visualEffects.addExplosion(powerUp.position.x, powerUp.position.y, 15f, 0xFFFFFFFF.toInt())
            powerUp.isActive = false
        }

        // Spawn crewmate — already there when arena loads
        val crewmateOrderIndex = state.corruptionSelectedCrewmates[state.corruptionEncounterIndex]
        val pilotId = StoryStateManager.CREWMATE_ENCOUNTER_ORDER[crewmateOrderIndex]
        val shipId = StoryStateManager.getShipForPilot(pilotId) ?: return

        crewmateEncounter.spawnCrewmateForAstro(state, pilotId, shipId, EntityPools.enemies)

        // Crewmate is already there when arena loads — no warp-in, 5-second spawn shield
        val crewmateShip = crewmateEncounter.crewmateShip
        if (crewmateShip != null) {
            crewmateShip.spawnTime = crewmateShip.warpInDuration  // Skip warp-in effect
            crewmateShip.spawnShieldTimer = 5f
        }

        // Crewmate says boss_spawn line — they don't know it's Astro
        val pilotDef = PilotDefinitions.getPilot(pilotId)
        if (pilotDef != null) {
            val line = RadioDefinitions.getLine(pilotId, "boss_spawn")
            if (line != null) {
                radioSystem.showScriptedMessage(state, pilotDef.callsign.uppercase(), line, pilotDef.color, isCorrupted = false)
            }
        }

        state.corruptionEncounterPhase = 2  // shield phase
        state.corruptionEncounterTimer = 0f
    }

    private fun updateCorruptionEncounter(deltaTime: Float) {
        state.corruptionEncounterTimer += deltaTime

        when (state.corruptionEncounterPhase) {
            2 -> { // Shield phase — Astro says "..." after 3s
                if (state.corruptionEncounterTimer >= 3f && state.corruptionEncounterTimer - deltaTime < 3f) {
                    radioSystem.showScriptedMessage(state, "ASTRO", "...",
                        PilotDefinitions.getPilot("pilot_astro")!!.color, isBoss = true, isCorrupted = true)
                }
                // When crewmate dies (shield drops + crystal Astro kills), transition to zap
                if (crewmateEncounter.crewmateDead) {
                    startCrystallineZap()
                }
            }
            4 -> { // Zap effect — wait 1.5s then exit
                if (state.corruptionEncounterTimer >= 1.5f) {
                    // Astro kill line
                    if (state.corruptionEncounterIndex < CORRUPTION_KILL_LINES.size) {
                        val killLine = CORRUPTION_KILL_LINES[state.corruptionEncounterIndex]
                        radioSystem.showScriptedMessage(state, "ASTRO", killLine,
                            PilotDefinitions.getPilot("pilot_astro")!!.color, isBoss = true)
                    }

                    // Persist the kill
                    val crewmateOrderIndex = state.corruptionSelectedCrewmates[state.corruptionEncounterIndex]
                    val pilotId = StoryStateManager.CREWMATE_ENCOUNTER_ORDER[crewmateOrderIndex]
                    val shipId = StoryStateManager.getShipForPilot(pilotId)
                    val persistence = PersistenceManager(context)
                    persistence.addDeadPilot(pilotId)
                    if (shipId != null) persistence.addDeadShip(shipId)

                    crewmateEncounter.reset(activeProjectiles)

                    // Clear any mines left from the crewmate's space_mines weapon
                    for (proj in activeProjectiles) {
                        if (proj.isActive && proj.type == ProjectileType.MINE) {
                            visualEffects.addExplosion(proj.position.x, proj.position.y, 15f, 0xFFFFFFFF.toInt())
                            proj.isActive = false
                        }
                    }

                    exitCorruptionBossArena()
                }
            }
        }
    }

    private fun startCrystallineZap() {
        val deathX = crewmateEncounter.crewmateShip?.position?.x ?: ship.position.x
        val deathY = crewmateEncounter.crewmateShip?.position?.y ?: ship.position.y

        // Set state for crystalline zap effect
        state.crystalZapActive = true
        state.crystalZapTimer = 0f
        crystalRenderer.activateZap(deathX, deathY)
        SoundManager.playSFX("sfx_crystal_activate")

        state.corruptionEncounterPhase = 4  // zap phase
        state.corruptionEncounterTimer = 0f
    }

    private fun exitCorruptionBossArena() {
        state.corruptionInBossArena = false
        state.bossActive = false  // Restore normal background

        // White flash transition back
        visualEffects.addExplosion(ship.position.x, ship.position.y, 800f, 0xFFFFFFFF.toInt())

        state.corruptionEncounterIndex++
        state.corruptionEncounterPhase = 0
        state.corruptionEncounterTimer = 0f
        state.corruptionChatterTimer = CORRUPTION_CHATTER_INTERVAL

        // Set next encounter time
        if (state.corruptionEncounterIndex < 5) {
            state.corruptionNextEncounterTime = CORRUPTION_ENCOUNTER_TIMES[state.corruptionEncounterIndex]
        }
    }

    private fun enterFinalEncounter() {
        // Clear field
        for (asteroid in activeAsteroids) {
            visualEffects.addExplosion(asteroid.position.x, asteroid.position.y, asteroid.radius * 1.5f, 0xFFFF4400.toInt())
            asteroid.isActive = false
        }
        for (powerUp in activePowerUps) {
            powerUp.isActive = false
        }
        // Deactivate lingering enemy projectiles — flak/cluster AoE from the last corruption
        // encounter can still be processing expiredNaturally when this frame runs, and the
        // explosion damage would kill a low-health Crystal Astro before PHASE_OTHER_SURVIVAL starts
        for (p in activeProjectiles) {
            if (p.isActive && p.isEnemyProjectile) p.isActive = false
        }

        // Guarantee full health at scene start — Crystal Astro must survive to the scripted TB-26 ram
        ship.health = ship.maxHealth
        ship.makeInvulnerable(3f)

        state.corruptionInBossArena = true
        state.bossActive = true

        // White flash
        visualEffects.addExplosion(ship.position.x, ship.position.y, 800f, 0xFFFFFFFF.toInt())

        radioSystem.showScriptedMessage(state, "ASTRO", "Found him.",
            PilotDefinitions.getPilot("pilot_astro")!!.color, isBoss = true)
        state.bossFightPhase = PHASE_OTHER_SPAWN
        state.bossFightTimer = 0f
        state.bossSpawned = true
        SoundManager.startBossBGM(context)
    }

    private fun updateUpgradeSelection(deltaTime: Float) {
        // Lucky Star animation — blocks player input while active
        if (state.luckyStarAnimating) {
            state.luckyStarTimer += deltaTime
            if (state.luckyStarDimming) {
                state.luckyStarDimTimer += deltaTime
                if (state.luckyStarDimTimer >= 0.5f) {
                    state.luckyStarAnimating = false
                    val option = upgradeSystem.selectOption(state.luckyStarSelectedIndex)
                    if (option != null) {
                        telemetryManager.logUpgradeOffered(state.survivalTime.toInt(), state.telemetryLastOfferedOptions, option.id)
                        applyUpgrade(option)
                    }
                    weaponSystem.resetBeatSync()
                    state.phase = GamePhase.PLAYING
                }
                return
            }
            if (state.luckyStarTimer >= state.luckyStarNextBounceTime) {
                state.luckyStarTimer = 0f
                state.luckyStarBounceCount++
                val options = upgradeSystem.getPendingOptions()
                if (state.luckyStarBounceCount >= state.luckyStarTotalBounces) {
                    state.luckyStarCurrentHighlight = state.luckyStarSelectedIndex
                    state.luckyStarDimming = true
                    state.luckyStarDimTimer = 0f
                    SoundManager.playSFX("sfx_lucky_star_select", volume = 0.4f)
                } else {
                    val remaining = state.luckyStarTotalBounces - state.luckyStarBounceCount
                    if (remaining <= options.size) {
                        state.luckyStarCurrentHighlight = (state.luckyStarSelectedIndex - remaining + options.size + 1) % options.size
                    } else {
                        state.luckyStarCurrentHighlight = (state.luckyStarCurrentHighlight + 1) % options.size
                    }
                    val progress = state.luckyStarBounceCount.toFloat() / state.luckyStarTotalBounces
                    state.luckyStarNextBounceTime = 0.1f + progress * 0.3f
                    SoundManager.playSFX("sfx_lucky_star_bounce", volume = 0.4f)
                }
            }
            return
        }

        // Check for tap on upgrade option
        if (touchController.consumeTap()) {
            val selectedIndex = upgradeSelectionRenderer.getSelectedOption(
                touchController.lastTapX,
                touchController.lastTapY
            )

            if (selectedIndex >= 0) {
                val option = upgradeSystem.selectOption(selectedIndex)
                if (option != null) {
                    telemetryManager.logUpgradeOffered(state.survivalTime.toInt(), state.telemetryLastOfferedOptions, option.id)
                    SoundManager.playSFX("sfx_ui_upgrade_select")
                    applyUpgrade(option)
                    weaponSystem.resetBeatSync()
                    state.phase = GamePhase.PLAYING
                }
            }
        }
    }

    private fun updateBossFightSequence(deltaTime: Float) {
        when (state.bossFightPhase) {
            PHASE_SURVIVAL -> { // Survival — drone departure at 5s
                if (state.bossFightTimer >= 5f && !state.droneDeparted) {
                    state.droneDeparted = true
                    radioSystem.showScriptedMessage(state, "ASTRO",
                        "Get out of here, buddy.",
                        PilotDefinitions.getPilot("pilot_astro")!!.color)
                    // Start TB-26 flyout visual — capture drone position before removing passive
                    val dronePos = combatDroneSystem.drones.firstOrNull()?.position
                    if (dronePos != null) {
                        fleetSystem.startTb26Flyout(dronePos.x, dronePos.y, boss.position.x, boss.position.y)
                    }
                    // Deactivate drone
                    state.passiveStacks.remove("tb26")
                    state.recalculateStats()
                    state.bossFightPhase = PHASE_DRONE_SENT
                    state.bossFightTimer = 0f
                }
            }
            PHASE_DRONE_SENT -> { // Post-drone departure
                if (state.bossFightTimer >= 3f) {
                    radioSystem.showScriptedMessage(state, "ASTRO",
                        "Alright, just you and me.",
                        PilotDefinitions.getPilot("pilot_astro")!!.color)
                    state.bossFightPhase = PHASE_WAITING_FLEET
                    state.bossFightTimer = 0f
                }
            }
            PHASE_WAITING_FLEET -> { // Survival alone — no expectation of rescue
                // Astro-only chatter: cycling survival lines and the TB-26 farewell line
                if (startingPilotId == "pilot_astro") {
                    val cyclingLines = listOf(
                        "Come on, hold together...",
                        "Can't keep this up..."
                    )
                    val lineInterval = 15f
                    val lineIndex = ((state.bossFightTimer / lineInterval).toInt()) % cyclingLines.size
                    if (state.bossFightTimer % lineInterval < deltaTime && state.bossFightTimer > 1f
                            && !state.bossCharging && state.bossRushPhase == 0) {
                        radioSystem.showScriptedMessage(state, "ASTRO",
                            cyclingLines[lineIndex],
                            PilotDefinitions.getPilot("pilot_astro")!!.color)
                    }
                    // "At least TB-26 got out." fires once at 30s — not part of the cycle
                    if (state.bossFightTimer >= 30f && state.bossFightTimer - deltaTime < 30f) {
                        radioSystem.showScriptedMessage(state, "ASTRO",
                            "At least TB-26 got out.",
                            PilotDefinitions.getPilot("pilot_astro")!!.color)
                    }
                }

                // Boss rush ignition at 40s — EMP #1 now fires on arrival, always on-screen
                if (state.bossFightTimer >= 40f && state.bossFightTimer - deltaTime < 40f) {
                    state.bossRushPhase = 1
                    state.bossRushTimer = 0f
                    radioSystem.showScriptedMessage(state, "BOSS",
                        "This ends now.",
                        Boss.CORRUPTION_COLOR, isBoss = true)
                    val igniteDx = ship.position.x - boss.position.x
                    val igniteDy = ship.position.y - boss.position.y
                    val ignitionGap = sqrt(igniteDx * igniteDx + igniteDy * igniteDy)
                    boss.startRush(BossRush.rushSpeed(ship.speed, ignitionGap, ship.speed))
                }

                // Rush sequence: rushing → hard brake → stillness beat → the old 40s bundle
                if (state.bossRushPhase > 0) {
                    state.bossRushTimer += deltaTime
                    when (state.bossRushPhase) {
                        1 -> { // Boss.update drives the motion; watch for EMP range
                            val rushDx = ship.position.x - boss.position.x
                            val rushDy = ship.position.y - boss.position.y
                            if (BossRush.hasArrived(sqrt(rushDx * rushDx + rushDy * rushDy))) {
                                boss.startRushBrake()
                                visualEffects.addHitFlash(boss.position.x, boss.position.y, 40f, 0xFFFFCC88.toInt())
                                state.bossRushPhase = 2
                                state.bossRushTimer = 0f
                            }
                        }
                        2 -> if (state.bossRushTimer >= GameConfig.BOSS_RUSH_BRAKE_DURATION) {
                            boss.stun()  // burn extinguished; frozen to line up the kill shot
                            state.bossRushPhase = 3
                            state.bossRushTimer = 0f
                        }
                        3 -> if (state.bossRushTimer >= GameConfig.BOSS_RUSH_ARRIVAL_BEAT) {
                            state.bossRushPhase = 0
                            state.bossCharging = true
                            state.bossChargeTimer = 0f
                            fadeAllProjectiles()  // clean field as the charge begins
                            // EMP #1 — freezes the lone player the instant the charge begins
                            state.bossEmpFired = true
                            weaponSystem.empHitOrbiters()
                            visualEffects.addBossShockwave(boss.position.x, boss.position.y)
                            FleetSystem.scatterEntity(ship, boss.position.x, boss.position.y)
                            val emp1Def = PilotDefinitions.getPilot(startingPilotId)
                            val emp1Line = LoopDefinitions.empReactionLines[startingPilotId] ?: "EMP— I can't move—"
                            radioSystem.showScriptedMessage(state, emp1Def?.callsign ?: "ASTRO", emp1Line,
                                emp1Def?.color ?: PilotDefinitions.getPilot("pilot_astro")!!.color)
                            // Reset chatter steps so the sub-sequence starts clean
                            state.fleetChatterStep = 0
                            state.fleetChatterTimer = 0f
                        }
                    }
                }

                // TB-26 solo return sub-sequence (Astro run only)
                if (state.bossCharging && startingPilotId == "pilot_astro") {
                    state.fleetChatterTimer += deltaTime
                    when (state.fleetChatterStep) {
                        0 -> if (state.fleetChatterTimer >= 4f) {   // adrift — despair beat
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Not like this. Not alone.",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 1; state.fleetChatterTimer = 0f
                        }
                        1 -> if (state.fleetChatterTimer >= 4f) {   // TB-26 sweeps back in at 8s
                            fleetSystem.startTb26Return(camera.getScreenWidth(), camera.getScreenHeight())
                            radioSystem.showScriptedMessage(state, "TB-26", "Not today, Astro.", 0xFF6688AA.toInt())
                            state.fleetChatterStep = 2; state.fleetChatterTimer = 0f
                        }
                        2 -> if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "ASTRO", "I told you to leave.",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 3; state.fleetChatterTimer = 0f
                        }
                        3 -> if (state.fleetChatterTimer >= 2.5f) {
                            radioSystem.showScriptedMessage(state, "TB-26", "Since when do I listen?", 0xFF6688AA.toInt())
                            startEngineRestart()   // sputter -> catch (flips bossEmpFired) -> ramp
                            state.fleetChatterStep = 4; state.fleetChatterTimer = 0f
                        }
                        4 -> if (state.fleetChatterTimer >= FleetSystem.FLEET_WARP_BEAT) {  // fleet warps in after the restart beat
                            fleetSystem.arrive(state, startingShipId, camera.getScreenWidth(), camera.getScreenHeight())
                            for (p in activeProjectiles) {
                                if (p.weaponId == "boss_rail") { p.lifetime = p.age + 0.5f; p.shouldFadeOut = true }
                            }
                            state.bossFightPhase = PHASE_FLEET_CHATTER
                            state.bossFightTimer = 0f
                            state.fleetChatterStep = 0; state.fleetChatterTimer = 0f
                        }
                    }
                } else if (state.bossCharging) {
                    // Non-Astro: no rescue — charge completes and fires a kill-shot
                    state.fleetChatterTimer += deltaTime
                    // Mid-charge boss taunt — only when the player is carrying an unused
                    // Phoenix Core, so the line never appears with no Phoenix to mock.
                    // Shown on its own frame so it isn't clobbered by the kill-shot/death.
                    if (state.fleetChatterStep == 0 && state.fleetChatterTimer >= 1.5f
                            && state.extraLives > 0 && !state.phoenixUsed) {
                        radioSystem.showScriptedMessage(state, "BOSS",
                            "The Phoenix can't save you.",
                            Boss.CORRUPTION_COLOR, isBoss = true)
                        state.fleetChatterStep = 1   // taunt shown; still waiting to fire
                    }
                    if (state.fleetChatterTimer >= 3f && state.fleetChatterStep < 2) {
                        state.fleetChatterStep = 2   // fire once
                        state.bossCharging = false
                        state.bossChargeProgress = 0f
                        fireBossChargedShot()
                    }
                }
            }
            PHASE_FLEET_CHATTER -> { // Fleet arriving + brief exchange + formation command
                state.fleetChatterTimer += deltaTime
                when (state.fleetChatterStep) {
                    0 -> { // Rascal: "You think we'd miss this?"
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "RASCAL",
                                "You think we'd miss this?",
                                PilotDefinitions.getPilot("pilot_rascal")!!.color)
                            state.fleetChatterStep = 1
                            state.fleetChatterTimer = 0f
                        }
                    }
                    1 -> { // Astro: "Don't let it complete!"
                        if (state.fleetChatterTimer >= 3f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Don't let it complete!",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.bossFightPhase = PHASE_FORMATION
                            state.bossFightTimer = 0f
                            state.fleetChatterStep = 0
                            state.fleetChatterTimer = 0f
                        }
                    }
                }
            }
            PHASE_FORMATION -> { // Ships moving to ring positions — hold 3s to let visual breathe
                // bossFightTimer already incremented in main loop (line ~903)
                if (fleetSystem.formationActive && state.bossFightTimer >= 4f) {
                    state.bossFightPhase = PHASE_SHIELD_ASSAULT
                    state.bossFightTimer = 0f
                    state.fleetChatterStep = 0
                    state.fleetChatterTimer = 0f
                }
            }
            PHASE_SHIELD_ASSAULT -> { // Futile assault — 8 seconds with chatter
                state.fleetChatterTimer += deltaTime
                when (state.fleetChatterStep) {
                    0 -> {
                        if (state.fleetChatterTimer >= 3f) {
                            radioSystem.showScriptedMessage(state, "BRUTUS",
                                "Shields are holding!",
                                PilotDefinitions.getPilot("pilot_brutus")!!.color)
                            state.fleetChatterStep = 1
                            state.fleetChatterTimer = 0f
                        }
                    }
                    1 -> {
                        if (state.fleetChatterTimer >= 4f) {
                            radioSystem.showScriptedMessage(state, "DASH",
                                "Nothing's getting through!",
                                PilotDefinitions.getPilot("pilot_dash")!!.color)
                            state.fleetChatterStep = 2
                            state.fleetChatterTimer = 0f
                        }
                    }
                    2 -> { // Fang + EMP #2 fires simultaneously (fleet-wide freeze, no knockback)
                        if (state.fleetChatterTimer >= 3f) {
                            radioSystem.showScriptedMessage(state, "FANG",
                                "We can't break through!",
                                PilotDefinitions.getPilot("pilot_fang")!!.color)
                            if (!state.bossEmpFired && state.bossChargeProgress >= GameConfig.BOSS_EMP_CHARGE_THRESHOLD) {
                                state.bossEmpFired = true
                                fleetSystem.applyEmpScatter()
                                FleetSystem.scatterEntity(ship, boss.position.x, boss.position.y)
                                weaponSystem.empHitOrbiters()
                                visualEffects.addBossShockwave(boss.position.x, boss.position.y)
                                // Fade all in-flight player projectiles — EMP #2 clears the field
                                for (p in activeProjectiles) {
                                    if (!p.isEnemyProjectile) {
                                        p.lifetime = p.age + 0.5f
                                        p.shouldFadeOut = true
                                    }
                                }
                            }
                            state.fleetChatterStep = 3
                            state.fleetChatterTimer = 0f
                        }
                    }
                    3 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "KRAKEN",
                                "EMP hit— losing control—",
                                PilotDefinitions.getPilot("pilot_kraken")!!.color)
                            state.fleetChatterStep = 4
                            state.fleetChatterTimer = 0f
                        }
                    }
                    4 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "UNIT-7",
                                "Systems offline. Rerouting.",
                                PilotDefinitions.getPilot("pilot_unit7")!!.color)
                            state.fleetChatterStep = 5
                            state.fleetChatterTimer = 0f
                        }
                    }
                    5 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "WHISKERS",
                                "Can't steer— get through!",
                                PilotDefinitions.getPilot("pilot_whiskers")!!.color)
                            state.fleetChatterStep = 6
                            state.fleetChatterTimer = 0f
                        }
                    }
                    6 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "HAVOC",
                                "NEARLY CHARGED— SOMEONE—",
                                PilotDefinitions.getPilot("pilot_havoc")!!.color)
                            state.fleetChatterStep = 7
                            state.fleetChatterTimer = 0f
                        }
                    }
                    7 -> {
                        if (state.fleetChatterTimer >= 4f) {
                            radioSystem.showScriptedMessage(state, "TB-26",
                                "Leave this one to me.",
                                0xFF6688AA.toInt())
                            fleetSystem.startTb26Charge()
                            state.fleetChatterStep = 8
                            state.fleetChatterTimer = 0f
                        }
                    }
                    8 -> {
                        if (state.fleetChatterTimer >= 4f) {
                            radioSystem.showScriptedMessage(state, "TB-26",
                                "I'll see you at the bar, Astro.",
                                0xFF6688AA.toInt())
                            state.fleetChatterStep = 9
                            state.fleetChatterTimer = 0f
                        }
                    }
                    9 -> {
                        if (state.fleetChatterTimer >= 3f) {
                            state.bossFightPhase = PHASE_TB26_RAM
                            state.bossFightTimer = 0f
                            fleetSystem.arrivalPhase = 3  // TB-26 ram phase
                        }
                    }
                }
            }
            PHASE_TB26_RAM -> { // TB-26 charging boss
                if (fleetSystem.tb26Rammed) {
                    state.timeCrystalX = boss.position.x
                    state.timeCrystalY = boss.position.y
                    state.timeCrystalOriginX = boss.position.x
                    state.timeCrystalOriginY = boss.position.y
                    state.timeCrystalPhase = GameState.TimeCrystalPhase.RISING
                    state.timeCrystalTimer = 0f
                    fleetSystem.stopFiring()
                    fleetSystem.startRingExpansion()
                    fleetSystem.startWeaponFade()
                    fleetSystem.applyShockwaveImpulse(boss.position.x, boss.position.y)
                    visualEffects.addBossShockwave(boss.position.x, boss.position.y)
                    // Charge energy misfires sideways — stored energy releases as burst
                    if (state.bossCharging) {
                        state.bossCharging = false
                        state.bossChargeProgress = 0f
                        visualEffects.addExplosion(boss.position.x, boss.position.y,
                            200f, 0xFFAADDFF.toInt())  // Blue-white misfired charge burst
                    }
                    val pdx = ship.position.x - boss.position.x
                    val pdy = ship.position.y - boss.position.y
                    val pdist = sqrt(pdx * pdx + pdy * pdy).coerceAtLeast(10f)
                    val pforce = (40000f / pdist).coerceAtMost(400f)
                    ship.velocity.x += (pdx / pdist) * pforce
                    ship.velocity.y += (pdy / pdist) * pforce
                    state.bossFightPhase = PHASE_POST_VICTORY
                    state.bossFightTimer = 0f
                    state.fleetChatterStep = 0
                    state.fleetChatterTimer = 0f
                }
            }
            PHASE_POST_VICTORY -> { // Post-victory chatter + crystal
                updatePostVictorySequence(deltaTime)
            }

            // === Boss from the other side (corruption Astro run) ===
            PHASE_OTHER_SPAWN -> { // Spawn past Astro — invulnerable AI enemy
                crewmateEncounter.spawnHealthySelf(state, "pilot_astro", "ship_white", EntityPools.enemies)

                // TB-26 companion — position near past Astro
                val pastAstro = crewmateEncounter.crewmateShip
                if (pastAstro != null) {
                    // Already there when arena clears — no warp-in, 5-second spawn shield
                    pastAstro.spawnTime = pastAstro.warpInDuration
                    pastAstro.spawnShieldTimer = 5f

                    fleetSystem.tb26Active = true
                    fleetSystem.tb26Rammed = false
                    fleetSystem.tb26OrbitTimer = 0f
                    fleetSystem.tb26OrbitTarget = pastAstro.position  // orbit Past Astro, not Crystal Astro
                    fleetSystem.playerRingPosition.set(pastAstro.position.x, pastAstro.position.y)
                    fleetSystem.tb26Position.set(
                        pastAstro.position.x + 30f,
                        pastAstro.position.y + 15f
                    )
                }

                state.bossFightPhase = PHASE_OTHER_SURVIVAL
                state.bossFightTimer = 0f
                state.fleetChatterStep = 0
                state.fleetChatterTimer = 0f
            }
            PHASE_OTHER_SURVIVAL -> { // Survival against past Astro — mirrors normal resignation arc
                if (state.bossCharging && !state.pastAstroEmpFrozen) {  // EMP #1 freezes Past Astro mid-charge
                    val pa = crewmateEncounter.crewmateShip
                    if (pa != null && pa.isActive) {
                        // Past Astro tries to interrupt the charge: hold at his weapon's
                        // ring distance, turn with normal ship physics, fire his weapon.
                        val paWeaponId = ShipDefinitions.getShip(startingShipId)?.startingWeaponId ?: "pulse_cannon"
                        val paRing = FleetSystem.ringForShip(startingShipId)
                        val ringRadius = if (paRing == 0) FleetSystem.OUTER_RADIUS else FleetSystem.INNER_RADIUS

                        // Ease toward ring distance from the boss, keeping current bearing.
                        val bdx = pa.position.x - boss.position.x
                        val bdy = pa.position.y - boss.position.y
                        val bdist = sqrt(bdx * bdx + bdy * bdy).coerceAtLeast(1f)
                        val bearingX = bdx / bdist
                        val bearingY = bdy / bdist
                        val targetX = boss.position.x + bearingX * ringRadius
                        val targetY = boss.position.y + bearingY * ringRadius
                        pa.position.x += (targetX - pa.position.x) * (2f * deltaTime).coerceAtMost(1f)
                        pa.position.y += (targetY - pa.position.y) * (2f * deltaTime).coerceAtMost(1f)

                        // Turn toward the boss with the player ship's normal turn curve
                        // (mirrors Ship.lerpAngle: rotation += diff * (8 * dt)).
                        val target = atan2(
                            boss.position.y - pa.position.y,
                            boss.position.x - pa.position.x
                        )
                        val diff = normalizeAngle(target - pa.rotation)
                        pa.rotation += diff * (8f * deltaTime).coerceIn(0f, 1f)

                        // Fire at the boss when roughly aimed (boss is invulnerable —
                        // shots are a cosmetic interrupt attempt). Close-range weapons
                        // (saw/orbiters) show no projectile, matching the fleet system.
                        if (abs(diff) < 0.3f) {
                            pastAstroFireTimer -= deltaTime
                            if (pastAstroFireTimer <= 0f) {
                                pastAstroFireTimer = 1.2f
                                val paColor = ShipDefinitions.getWeaponColor(paWeaponId)
                                fleetSystem.fireWeaponAtBoss(pa.position.x, pa.position.y, paWeaponId, paColor)
                            }
                        }
                    }
                }
                // After EMP #1, Past Astro is frozen with no integrator — drift the scatter
                // impulse (and engine-restart sputter pulses) with decay so it reads visually.
                if (state.pastAstroEmpFrozen) {
                    crewmateEncounter.crewmateShip?.let {
                        it.position.x += it.velocity.x * deltaTime
                        it.position.y += it.velocity.y * deltaTime
                        it.velocity.x *= (1f - 0.8f * deltaTime).coerceAtLeast(0f)
                        it.velocity.y *= (1f - 0.8f * deltaTime).coerceAtLeast(0f)
                    }
                }
                // TB-26 companion orbits past Astro until flyout
                val pastAstro = crewmateEncounter.crewmateShip
                if (pastAstro != null && fleetSystem.tb26Active && state.fleetChatterStep == 0) {
                    fleetSystem.playerRingPosition.set(pastAstro.position.x, pastAstro.position.y)
                    fleetSystem.updateTb26Orbit(deltaTime)
                }

                state.fleetChatterTimer += deltaTime
                when (state.fleetChatterStep) {
                    0 -> { // 5s: "Get out of here, buddy." + TB-26 flyout
                        if (state.fleetChatterTimer >= 5f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Get out of here, buddy.",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            if (pastAstro != null) {
                                fleetSystem.startTb26Flyout(
                                    fleetSystem.tb26Position.x, fleetSystem.tb26Position.y,
                                    ship.position.x, ship.position.y
                                )
                            }
                            state.fleetChatterStep = 1
                            state.fleetChatterTimer = 0f
                        }
                    }
                    1 -> { // 3s: "Alright, just you and me."
                        if (state.fleetChatterTimer >= 3f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Alright, just you and me.",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 2
                            state.fleetChatterTimer = 0f
                        }
                    }
                    2 -> { // 15s: "Come on, hold together..."
                        if (state.fleetChatterTimer >= 15f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Come on, hold together...",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 3
                            state.fleetChatterTimer = 0f
                        }
                    }
                    3 -> { // 15s: "Can't keep this up..."
                        if (state.fleetChatterTimer >= 15f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Can't keep this up...",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 4
                            state.fleetChatterTimer = 0f
                        }
                    }
                    4 -> { // 15s: "At least TB-26 got out."
                        if (state.fleetChatterTimer >= 15f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "At least TB-26 got out.",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 5
                            state.fleetChatterTimer = 0f
                        }
                    }
                    5 -> { // 3s: corrupted Astro ignites the rush — controls taken over
                        if (state.fleetChatterTimer >= 3f) {
                            state.corruptionRushPhase = 1
                            state.corruptionRushTimer = 0f
                            fadeAllProjectiles()  // weapons go quiet as the burn ignites
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "This ends now.",
                                Boss.CORRUPTION_COLOR, isBoss = true)
                            crewmateEncounter.straightFlee = true
                            val pa = crewmateEncounter.crewmateShip
                            val gapX = (pa?.position?.x ?: ship.position.x) - ship.position.x
                            val gapY = (pa?.position?.y ?: ship.position.y) - ship.position.y
                            val ignitionGap = sqrt(gapX * gapX + gapY * gapY)
                            state.corruptionRushSpeed = BossRush.rushSpeed(
                                ship.speed, ignitionGap, pa?.speed ?: GameConfig.SHIP_BASE_SPEED)
                            state.fleetChatterStep = 6
                            state.fleetChatterTimer = 0f
                        }
                    }
                    6 -> { // rush in flight — arrival bundle after the stillness beat
                        if (state.corruptionRushPhase == 3 &&
                                state.corruptionRushTimer >= GameConfig.BOSS_RUSH_ARRIVAL_BEAT) {
                            state.corruptionRushPhase = 0
                            state.bossCharging = true
                            state.bossChargeTimer = 0f
                            boss.isActive = true
                            boss.shielded = true
                            boss.isInvulnerable = true
                            boss.position.set(ship.position.x, ship.position.y)
                            state.playerStunned = true  // Crystal Astro holds position
                            crewmateEncounter.frozen = true
                            crewmateEncounter.crewmateShip?.velocity?.zero()
                            // EMP #1 — the charge opens with an EMP that instantly freezes
                            // Past Astro, mirroring the lone player's freeze in the normal fight.
                            // (Deliberately does NOT set bossEmpFired — EMP #2 still needs it false.)
                            state.pastAstroEmpFrozen = true
                            weaponSystem.empHitOrbiters()
                            visualEffects.addBossShockwave(boss.position.x, boss.position.y)
                            crewmateEncounter.crewmateShip?.let {
                                FleetSystem.scatterEntity(it, boss.position.x, boss.position.y)
                            }
                            // Past Astro's reaction — same frame as the EMP, mirroring the
                            // lone player's reaction in the normal fight
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                LoopDefinitions.empReactionLines["pilot_astro"] ?: "EMP— I can't move—",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 7
                            state.fleetChatterTimer = 0f
                        }
                    }
                    7 -> { // 4s adrift — Past Astro's despair beat (mirrors the normal run)
                        if (state.fleetChatterTimer >= 4f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Not like this. Not alone.",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 8
                            state.fleetChatterTimer = 0f
                        }
                    }
                    8 -> { // TB-26 sweeps back in at 8s — to Past Astro's side this time
                        if (state.fleetChatterTimer >= 4f) {
                            fleetSystem.startTb26Return(
                                camera.getScreenWidth(), camera.getScreenHeight(),
                                crewmateEncounter.crewmateShip?.position)
                            radioSystem.showScriptedMessage(state, "TB-26", "Not today, Astro.", 0xFF6688AA.toInt())
                            state.fleetChatterStep = 9
                            state.fleetChatterTimer = 0f
                        }
                    }
                    9 -> { // mirrors the normal run's exchange, same 2s beat
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "ASTRO", "I told you to leave.",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.fleetChatterStep = 10
                            state.fleetChatterTimer = 0f
                        }
                    }
                    10 -> { // 2.5s: "Since when do I listen?" + Past Astro's engines sputter back
                        if (state.fleetChatterTimer >= 2.5f) {
                            radioSystem.showScriptedMessage(state, "TB-26", "Since when do I listen?", 0xFF6688AA.toInt())
                            startEngineRestart()   // sputter -> catch -> ramp, mirroring the normal run
                            state.fleetChatterStep = 11
                            state.fleetChatterTimer = 0f
                        }
                    }
                    11 -> { // restart beat plays out (sputter + ramp + a breath), then the fleet warps in
                        if (state.fleetChatterTimer >= FleetSystem.FLEET_WARP_BEAT) {
                            // Position boss entity at player so fleet converges on us
                            boss.position.set(ship.position.x, ship.position.y)
                            boss.isActive = true
                            boss.shielded = true
                            boss.isInvulnerable = true
                            // Ensure past Astro is visible when fleet arrives
                            val pastAstroForFleet = crewmateEncounter.crewmateShip
                            if (pastAstroForFleet != null) {
                                val margin = 100f
                                val camLeft = ship.position.x - screenWidth / 2f + margin
                                val camRight = ship.position.x + screenWidth / 2f - margin
                                val camTop = ship.position.y - screenHeight / 2f + margin
                                val camBottom = ship.position.y + screenHeight / 2f - margin
                                pastAstroForFleet.position.x = pastAstroForFleet.position.x.coerceIn(camLeft, camRight)
                                pastAstroForFleet.position.y = pastAstroForFleet.position.y.coerceIn(camTop, camBottom)
                            }
                            // Freeze past Astro AI — they'll be auto-piloted to formation
                            crewmateEncounter.frozen = true
                            fleetSystem.arrive(state, startingShipId, camera.getScreenWidth(), camera.getScreenHeight())
                            // TB-26 must orbit Past Astro (not Crystal Astro) before ramming
                            val pastAstroForOrbit = crewmateEncounter.crewmateShip
                            if (pastAstroForOrbit != null) {
                                fleetSystem.tb26OrbitTarget = pastAstroForOrbit.position
                            }
                            // Fade boss railgun projectiles still in flight
                            for (p in activeProjectiles) {
                                if (p.weaponId == "boss_rail") {
                                    p.lifetime = p.age + 0.5f
                                    p.shouldFadeOut = true
                                }
                            }
                            state.bossFightPhase = PHASE_OTHER_FLEET
                            pastAstroArrived = false
                            pastAstroFireTimer = 0f
                            state.bossFightTimer = 0f
                            state.fleetChatterStep = 0
                            state.fleetChatterTimer = 0f
                        }
                    }
                }
            }
            PHASE_OTHER_FLEET -> { // Fleet arriving + brief exchange — exact mirror of FLEET_CHATTER
                boss.position.set(ship.position.x, ship.position.y)
                state.fleetChatterTimer += deltaTime
                when (state.fleetChatterStep) {
                    0 -> { // Rascal: "You think we'd miss this?"
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "RASCAL",
                                "You think we'd miss this?",
                                PilotDefinitions.getPilot("pilot_rascal")!!.color)
                            state.fleetChatterStep = 1
                            state.fleetChatterTimer = 0f
                        }
                    }
                    1 -> { // Past Astro: "Don't let it complete!"
                        if (state.fleetChatterTimer >= 3f) {
                            radioSystem.showScriptedMessage(state, "ASTRO",
                                "Don't let it complete!",
                                PilotDefinitions.getPilot("pilot_astro")!!.color)
                            state.bossFightPhase = PHASE_OTHER_FORMATION
                            state.bossFightTimer = 0f
                            state.fleetChatterStep = 0
                            state.fleetChatterTimer = 0f
                        }
                    }
                }
            }
            PHASE_OTHER_FORMATION -> { // 4s visual hold — mirrors FORMATION
                boss.position.set(ship.position.x, ship.position.y)
                state.fleetChatterTimer += deltaTime
                if (fleetSystem.formationActive && state.fleetChatterTimer >= 4f) {
                    state.bossFightPhase = PHASE_OTHER_SHIELD_ASSAULT
                    state.bossFightTimer = 0f
                    state.fleetChatterStep = 0
                    state.fleetChatterTimer = 0f
                }
            }
            PHASE_OTHER_SHIELD_ASSAULT -> { // Same chatter, player on receiving end
                boss.position.set(ship.position.x, ship.position.y)
                state.fleetChatterTimer += deltaTime
                when (state.fleetChatterStep) {
                    0 -> {
                        if (state.fleetChatterTimer >= 3f) {
                            radioSystem.showScriptedMessage(state, "BRUTUS",
                                "Shields are holding!",
                                PilotDefinitions.getPilot("pilot_brutus")!!.color)
                            state.fleetChatterStep = 1
                            state.fleetChatterTimer = 0f
                        }
                    }
                    1 -> {
                        if (state.fleetChatterTimer >= 4f) {
                            radioSystem.showScriptedMessage(state, "DASH",
                                "Nothing's getting through!",
                                PilotDefinitions.getPilot("pilot_dash")!!.color)
                            state.fleetChatterStep = 2
                            state.fleetChatterTimer = 0f
                        }
                    }
                    2 -> { // Fang + EMP fires simultaneously
                        if (state.fleetChatterTimer >= 3f) {
                            radioSystem.showScriptedMessage(state, "FANG",
                                "We can't break through!",
                                PilotDefinitions.getPilot("pilot_fang")!!.color)
                            if (!state.bossEmpFired && state.bossChargeProgress >= GameConfig.BOSS_EMP_CHARGE_THRESHOLD) {
                                state.bossEmpFired = true
                                fleetSystem.applyEmpScatter()
                                weaponSystem.empHitOrbiters()
                                crewmateEncounter.crewmateShip?.let {
                                    FleetSystem.scatterEntity(it, boss.position.x, boss.position.y)
                                }
                                visualEffects.addBossShockwave(boss.position.x, boss.position.y)
                                // Fade all in-flight player projectiles — EMP clears the field
                                for (p in activeProjectiles) {
                                    if (!p.isEnemyProjectile) {
                                        p.lifetime = p.age + 0.5f
                                        p.shouldFadeOut = true
                                    }
                                }
                            }
                            state.fleetChatterStep = 3
                            state.fleetChatterTimer = 0f
                        }
                    }
                    3 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "KRAKEN",
                                "EMP hit— losing control—",
                                PilotDefinitions.getPilot("pilot_kraken")!!.color)
                            state.fleetChatterStep = 4
                            state.fleetChatterTimer = 0f
                        }
                    }
                    4 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "UNIT-7",
                                "Systems offline. Rerouting.",
                                PilotDefinitions.getPilot("pilot_unit7")!!.color)
                            state.fleetChatterStep = 5
                            state.fleetChatterTimer = 0f
                        }
                    }
                    5 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "WHISKERS",
                                "Can't steer— get through!",
                                PilotDefinitions.getPilot("pilot_whiskers")!!.color)
                            state.fleetChatterStep = 6
                            state.fleetChatterTimer = 0f
                        }
                    }
                    6 -> {
                        if (state.fleetChatterTimer >= 2f) {
                            radioSystem.showScriptedMessage(state, "HAVOC",
                                "NEARLY CHARGED— SOMEONE—",
                                PilotDefinitions.getPilot("pilot_havoc")!!.color)
                            state.fleetChatterStep = 7
                            state.fleetChatterTimer = 0f
                        }
                    }
                    7 -> {
                        if (state.fleetChatterTimer >= 4f) {
                            radioSystem.showScriptedMessage(state, "TB-26",
                                "Leave this one to me.",
                                0xFF6688AA.toInt())
                            fleetSystem.startTb26Charge()
                            state.fleetChatterStep = 8
                            state.fleetChatterTimer = 0f
                        }
                    }
                    8 -> {
                        if (state.fleetChatterTimer >= 4f) {
                            radioSystem.showScriptedMessage(state, "TB-26",
                                "I'll see you at the bar, Astro.",
                                0xFF6688AA.toInt())
                            state.fleetChatterStep = 9
                            state.fleetChatterTimer = 0f
                        }
                    }
                    9 -> { // 3s silence then TB-26 ram
                        if (state.fleetChatterTimer >= 3f) {
                            state.bossFightPhase = PHASE_OTHER_TB26_RAM
                            state.bossFightTimer = 0f
                            fleetSystem.arrivalPhase = 3  // TB-26 ram phase
                        }
                    }
                }
            }
            PHASE_OTHER_TB26_RAM -> { // TB-26 charges the player — exact mirror of normal PHASE_TB26_RAM
                boss.position.set(ship.position.x, ship.position.y)
                if (fleetSystem.tb26Rammed) {
                    // Strip crystal powers, zero shields
                    state.hasCrystalPowers = false
                    ship.currentShield = 0f
                    boss.shielded = false
                    // Instant player death — mirrors boss.isActive=false in normal run
                    ship.health = 0f
                    ship.isActive = false
                    // Player explosion (mirrors boss death explosion)
                    visualEffects.addExplosion(ship.position.x, ship.position.y,
                        Boss.BOSS_SIZE * 3f, 0xFFFF4400.toInt())
                    // Shockwave ring + fleet impulse (was missing — mirrors normal run exactly)
                    visualEffects.addBossShockwave(ship.position.x, ship.position.y)
                    // Charge energy misfires sideways
                    if (state.bossCharging) {
                        state.bossCharging = false
                        state.bossChargeProgress = 0f
                        visualEffects.addExplosion(ship.position.x, ship.position.y,
                            200f, 0xFFAADDFF.toInt())
                    }
                    fleetSystem.applyShockwaveImpulse(ship.position.x, ship.position.y)
                    // Push past Astro outward (mirrors player impulse in normal run)
                    val pastAstro = crewmateEncounter.crewmateShip
                    if (pastAstro != null && pastAstro.isActive) {
                        val pdx = pastAstro.position.x - ship.position.x
                        val pdy = pastAstro.position.y - ship.position.y
                        val pdist = sqrt(pdx * pdx + pdy * pdy).coerceAtLeast(10f)
                        val pforce = (40000f / pdist).coerceAtMost(400f)
                        pastAstro.velocity.x += (pdx / pdist) * pforce
                        pastAstro.velocity.y += (pdy / pdist) * pforce
                    }
                    // Fleet stops firing; rings expand outward
                    fleetSystem.stopFiring()
                    fleetSystem.startRingExpansion()
                    fleetSystem.startWeaponFade()
                    state.bossFightPhase = PHASE_HEART_TRANSITION
                    state.bossFightTimer = 0f
                    state.fleetChatterStep = 0
                    state.fleetChatterTimer = 0f
                }
            }
            PHASE_OTHER_DYING -> { // No longer reached — kept as safety no-op
                boss.position.set(ship.position.x, ship.position.y)
            }
            PHASE_HEART_TRANSITION -> { // Full-screen white flash, fade to black, then heart-to-heart
                ship.isActive = false
                // 0-0.3s: white fills screen, 0.3-1.0s: white fades to black, 1.0s: cut to heart-to-heart
                if (state.bossFightTimer >= 1.0f) {
                    state.phase = GamePhase.HEART_TO_HEART
                    state.bossFightPhase = PHASE_HEART_DIALOGUE
                    state.bossFightTimer = 0f
                    state.fleetChatterStep = 0
                    state.fleetChatterTimer = 0f
                    heartToHeartFadingOut = false
                    heartToHeartFadeTimer = 0f
                    SoundManager.stopCombatMusic()
                    SoundManager.playAmbient("bgm_heart_to_heart")
                }
            }
        }
    }

    private fun updatePostVictorySequence(deltaTime: Float) {
        // Crystal rises and hovers during chatter, flies to player at the end
        if (state.timeCrystalPhase == GameState.TimeCrystalPhase.RISING ||
            state.timeCrystalPhase == GameState.TimeCrystalPhase.HOVERING) {
            updateTimeCrystal(deltaTime)
        }

        state.fleetChatterTimer += deltaTime
        when (state.fleetChatterStep) {
            // --- Celebration (fast, building energy) ---
            0 -> { // Medic
                if (state.fleetChatterTimer >= 1f) {
                    radioSystem.showScriptedMessage(state, "MEDIC", "We did it!",
                        PilotDefinitions.getPilot("pilot_medic")!!.color)
                    state.fleetChatterStep = 1; state.fleetChatterTimer = 0f
                }
            }
            1 -> { // Rascal
                if (state.fleetChatterTimer >= 1.5f) {
                    radioSystem.showScriptedMessage(state, "RASCAL", "Take that!",
                        PilotDefinitions.getPilot("pilot_rascal")!!.color)
                    state.fleetChatterStep = 2; state.fleetChatterTimer = 0f
                }
            }
            2 -> { // Havoc
                if (state.fleetChatterTimer >= 1.5f) {
                    radioSystem.showScriptedMessage(state, "HAVOC", "WHAT A RUSH!",
                        PilotDefinitions.getPilot("pilot_havoc")!!.color)
                    state.fleetChatterStep = 3; state.fleetChatterTimer = 0f
                }
            }
            3 -> { // Frost — exhale, energy drops
                if (state.fleetChatterTimer >= 2f) {
                    radioSystem.showScriptedMessage(state, "FROST", "...finally.",
                        PilotDefinitions.getPilot("pilot_frost")!!.color)
                    state.fleetChatterStep = 4; state.fleetChatterTimer = 0f
                }
            }
            // --- Transition (the turn) ---
            4 -> { // Ember — realization
                if (state.fleetChatterTimer >= 4f) {
                    radioSystem.showScriptedMessage(state, "EMBER", "Wait... where's TB-26?",
                        PilotDefinitions.getPilot("pilot_ember")!!.color)
                    state.fleetChatterStep = 5; state.fleetChatterTimer = 0f
                }
            }
            5 -> { // 4s silence — the weight sinks in
                if (state.fleetChatterTimer >= 4f) {
                    radioSystem.showScriptedMessage(state, "ASTRO", "TB-26\u2014",
                        PilotDefinitions.getPilot("pilot_astro")!!.color)
                    state.fleetChatterStep = 6; state.fleetChatterTimer = 0f
                }
            }
            // --- Grief (slow, heavy) ---
            6 -> { // "...no."
                if (state.fleetChatterTimer >= 3f) {
                    radioSystem.showScriptedMessage(state, "ASTRO", "...no.",
                        PilotDefinitions.getPilot("pilot_astro")!!.color)
                    state.fleetChatterStep = 7; state.fleetChatterTimer = 0f
                }
            }
            7 -> { // "Respond. RESPOND."
                if (state.fleetChatterTimer >= 3.5f) {
                    radioSystem.showScriptedMessage(state, "ASTRO", "Respond. RESPOND.",
                        PilotDefinitions.getPilot("pilot_astro")!!.color)
                    state.fleetChatterStep = 9; state.fleetChatterTimer = 0f
                }
            }
            9 -> { // 3s silence then crystal flies to player
                if (state.fleetChatterTimer >= 3f && state.timeCrystalPhase == GameState.TimeCrystalPhase.HOVERING) {
                    state.timeCrystalPhase = GameState.TimeCrystalPhase.FLYING
                    state.timeCrystalTimer = 0f
                }
                if (state.timeCrystalPhase == GameState.TimeCrystalPhase.FLYING ||
                    state.timeCrystalPhase == GameState.TimeCrystalPhase.COLLECTED) {
                    updateTimeCrystal(deltaTime)
                }
            }
        }
    }

    private fun updateTimeCrystal(deltaTime: Float) {
        state.timeCrystalTimer += deltaTime
        when (state.timeCrystalPhase) {
            GameState.TimeCrystalPhase.RISING -> {
                if (state.timeCrystalTimer >= 2f) {
                    state.timeCrystalPhase = GameState.TimeCrystalPhase.HOVERING
                    state.timeCrystalTimer = 0f
                }
            }
            GameState.TimeCrystalPhase.HOVERING -> {
                // Float at boss death position with gentle bob — waits for post-victory chatter to finish
                val bob = sin(state.timeCrystalTimer * 2f) * 3f
                state.timeCrystalY = state.timeCrystalOriginY + bob
            }
            GameState.TimeCrystalPhase.FLYING -> {
                // Crystal stays at boss death position — Astro flies to it (collection detected in movement loop)
                val bob = sin(state.timeCrystalTimer * 2f) * 3f
                state.timeCrystalY = state.timeCrystalOriginY + bob
            }
            GameState.TimeCrystalPhase.COLLECTED -> {
                // Flash, then trigger crystal activation + game over
                if (state.timeCrystalTimer >= 0.3f) {
                    val persistence = PersistenceManager(context)
                    // The Sacrifice: every loop enters the corruption stage.
                    persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
                    state.phase = GamePhase.CRYSTAL_DEATH
                    crystalFreezeDelay = 0f
                    crystalDelayTimer = 0f
                    crystalDelayActive = false
                    crystalRenderer.activateDeath(screenWidth, screenHeight)
                    SoundManager.playSFX("sfx_crystal_activate")
                }
            }
            GameState.TimeCrystalPhase.NONE -> { /* no-op */ }
        }
    }

    private fun getHeartToHeartLines(): List<Pair<String, String>> {
        return LoopDefinitions.heartToHeartScript(state.storyLoop)
    }

    private fun updateHeartToHeart(deltaTime: Float) {
        state.fleetChatterTimer += deltaTime

        // Handle auto-transition fade-out (after all lines complete)
        if (heartToHeartFadingOut) {
            heartToHeartFadeTimer += deltaTime
            if (heartToHeartFadeTimer >= 1.0f + 0.5f) {
                // All loops proceed to the desert scene
                initializeDesert()
                state.phase = GamePhase.DESERT
                globalFadeAlpha = 1.0f
                globalFadeFading = true
                globalFadeTimer = 0f
                heartToHeartFadingOut = false
            }
            return
        }

        val allLines = getHeartToHeartLines()
        val lineIndex = state.fleetChatterStep

        if (lineIndex < allLines.size) {
            val (speaker, line) = allLines[lineIndex]
            val color = if (speaker == "TB-26") LoopDefinitions.TB_COLOR
                        else LoopDefinitions.ASTRO_COLOR

            if (state.heartToHeartCharIndex == 0 && state.fleetChatterTimer >= 2.5f) {
                state.heartToHeartLog.add(Triple(speaker, line, color))
                state.heartToHeartCharIndex = 1
                state.heartToHeartCharTimer = 0f
            }

            if (state.heartToHeartCharIndex > 0) {
                state.heartToHeartCharTimer += deltaTime
                if (state.heartToHeartCharTimer >= 0.04f) {
                    state.heartToHeartCharTimer = 0f
                    state.heartToHeartCharIndex++
                    SoundManager.playSFX("sfx_text_tick", 0.15f, 0.9f + (Math.random().toFloat() * 0.2f))
                    if (state.heartToHeartCharIndex > line.length) {
                        state.fleetChatterStep++
                        state.fleetChatterTimer = 0f
                        state.heartToHeartCharIndex = 0
                    }
                }
            }
        } else {
            // All lines complete
            // All loops: auto-fade then go to desert
            if (!heartToHeartFadingOut && state.fleetChatterTimer >= 4f) {
                heartToHeartFadingOut = true
                heartToHeartFadeTimer = 0f
            }
        }
    }

    // ========================================================================
    // DESERT FAREWELL + TIMELINE SHIFT
    // ========================================================================

    private fun initializeDesertFarewell() {
        state.heartToHeartLog.clear()
        state.fleetChatterStep = 0
        state.fleetChatterTimer = 0f
        state.heartToHeartCharIndex = 0
        state.heartToHeartCharTimer = 0f
        heartToHeartFadingOut = false
        heartToHeartFadeTimer = 0f
        timelineShiftAlpha = 0f
        timelineShiftHoldTimer = 0f
        SoundManager.stopAll()
        SoundManager.playAmbient("bgm_heart_to_heart")
    }

    /** Boss fires its fully-charged kill-shot at the player — only on the non-Astro path.
     *  Activates the beam visual, forces the ship to zero health, then triggers a normal death. */
    private fun fireBossChargedShot() {
        SoundManager.playSFX("sfx_boss_spawn") // reuse heavy SFX for the lethal discharge
        state.bossChargedShotActive = true
        state.bossChargedShotTimer = 0f
        // Phoenix Core cannot override a scripted kill-shot — a revive here would softlock
        // the run (boss stays stunned, beam never clears, sequence step is spent).
        state.extraLives = 0
        // Force lethal regardless of invulnerability, then handle death
        ship.takeDamage(GameConfig.BOSS_CHARGED_SHOT_DAMAGE)
        ship.health = 0f
        ship.isActive = false
        handlePlayerDeath()
    }

    /** Fade out every in-flight projectile (player, fleet, boss, enemy). Used to clear the
     *  field the moment the boss begins charging, so the charge starts on a clean screen. */
    private fun fadeAllProjectiles() {
        for (p in activeProjectiles) {
            if (!p.isActive) continue
            p.lifetime = p.age + 0.5f
            p.shouldFadeOut = true
        }
    }

    private fun updateDesertFarewell(deltaTime: Float) {
        state.fleetChatterTimer += deltaTime

        // Handle fade-out — after all lines, fade then switch to TIMELINE_SHIFT
        if (heartToHeartFadingOut) {
            heartToHeartFadeTimer += deltaTime
            if (heartToHeartFadeTimer >= 1.0f + 0.5f) {
                // Transition to the timeline shift. Leave heartToHeartFadingOut/timer set so the
                // farewell text stays faded out (no pop) during the black hold.
                timelineShiftAlpha = 0f
                timelineShiftHoldTimer = 0f
                state.phase = GamePhase.TIMELINE_SHIFT
            }
            return
        }

        val allLines = LoopDefinitions.desertFarewellScript()
        val lineIndex = state.fleetChatterStep

        if (lineIndex < allLines.size) {
            val (speaker, line) = allLines[lineIndex]
            // Desert good ending only — the reckoning win no longer routes through this phase
            // (it fades straight from the flight scene to the bar)
            val color = when (speaker) {
                LoopDefinitions.TB -> LoopDefinitions.TB_COLOR
                LoopDefinitions.CRYSTAL -> CrystalPalette.MID
                else -> LoopDefinitions.ASTRO_COLOR
            }

            if (state.heartToHeartCharIndex == 0 && state.fleetChatterTimer >= 2.5f) {
                state.heartToHeartLog.add(Triple(speaker, line, color))
                state.heartToHeartCharIndex = 1
                state.heartToHeartCharTimer = 0f
            }

            if (state.heartToHeartCharIndex > 0) {
                state.heartToHeartCharTimer += deltaTime
                if (state.heartToHeartCharTimer >= 0.04f) {
                    state.heartToHeartCharTimer = 0f
                    state.heartToHeartCharIndex++
                    SoundManager.playSFX("sfx_text_tick", 0.15f, 0.9f + (Math.random().toFloat() * 0.2f))
                    if (state.heartToHeartCharIndex > line.length) {
                        state.fleetChatterStep++
                        state.fleetChatterTimer = 0f
                        state.heartToHeartCharIndex = 0
                    }
                }
            }
        } else {
            // All lines complete — auto-fade
            if (!heartToHeartFadingOut && state.fleetChatterTimer >= 4f) {
                heartToHeartFadingOut = true
                heartToHeartFadeTimer = 0f
            }
        }
    }

    private fun updateTimelineShift(deltaTime: Float) {
        if (timelineShiftAlpha < 1f) {
            timelineShiftAlpha = (timelineShiftAlpha + deltaTime / TIMELINE_SHIFT_FADE_DURATION).coerceAtMost(1f)
            // Fade out ambient BGM during the black hold before handoff
            val vol = (1f - timelineShiftAlpha).coerceIn(0f, 1f)
            SoundManager.volumeAmbient = vol * 0.8f
        } else {
            timelineShiftHoldTimer += deltaTime
            if (timelineShiftHoldTimer >= TIMELINE_SHIFT_HOLD_DURATION) {
                SoundManager.stopAll()
                SoundManager.volumeAmbient = 0.8f  // Restore default volume
                saveRunStats(includeDeath = false)
                onGameOver(0, true)  // fadeFromWhite = true
            }
        }
    }

    // ========================================================================
    // DESERT FLASHBACK SCENE
    // ========================================================================

    private fun initializeDesert() {
        // Reset desert state
        state.desertPhase = 0
        state.desertTimer = 0f
        state.desertDialogueStep = 0
        state.desertDialogueTimer = 0f
        state.desertNoInputTimer = 0f
        state.desertSecretTriggered = false
        state.desertCrystalPhase = 0
        state.desertCrystalTimer = 0f
        state.desertFadeAlpha = 0f

        // Clear all existing entities
        EntityPools.resetAll()

        // Full health for desert (invulnerable anyway)
        ship.health = ship.maxHealth
        ship.currentShield = ship.maxShield

        // Reset desert tracking vars
        desertSpawnTimer = 0f
        desertFirstEnemiesSpawned = false
        desertSecondWaveSpawned = false
        desertThirdWaveAnnounced = false
        desertFirstKill = false
        desertKillCount = 0
        desertAmbiguousTargetSpawned = false
        desertPlayerShotInPhase2 = false
        desertPlayerContinuedShooting = false
        desertPlayerFiredAfterStop = false
        desertHorrorComplete = false
        desertGoodEndingLinesComplete = false
        desertFarewellFadeStarted = false
        desertFarewellFadeTimer = 0f
        desertStopCheckReached = false
        desertStopCheckTimer = 0f
        desertStopCheckY = 0f
        desertNorthDriveTimer = 0f
        desertEndingTimer = 0f

        // Reset ship to world origin so desert starts from a clean slate
        ship.position.set(0f, 0f)
        ship.velocity.set(0f, 0f)
        touchController.reset()
        ship.rotation = (-Math.PI / 2).toFloat()
        camera.update(ship)

        // Position TB tank near the player
        val tbInitOffsetX = if (ship.position.x > 0f) -120f else 120f
        desertTbX = ship.position.x + tbInitOffsetX
        desertTbY = ship.position.y + 80f
        desertTbAngle = ship.rotation
        desertPlayerTurretAngle = ship.rotation
        desertTbTurretAngle = ship.rotation
        desertPlayerSpeed = 0f
        desertTbSpeed = 0f
        desertTracks.clear()
        desertTrackSpawnTimer = 0f
        desertPlayerHasFired = false
        desertTbFireTimer = 0f
        desertTbAlive = true
        desertTbDeathTimer = 0f
        desertNearSettlement = false
        desertBombardmentUnlocked = false
        desertWrecks.clear()
        desertSettlementProgress = 0f
        desertSpawnY = ship.position.y
        desertSettlementWorldY = 0f
        desertCrystalDeath = false
        desertCivilians.clear()
        desertPlayerFireTimer = 0f
        desertSecondWaveSpawnDelay = -1f
        desertBuildings.clear()
        desertBombardmentActive = false
        desertBombardmentDelayTimer = -1f
        desertTbWalkingToCrystal = false
        desertCrystalDrainPhase = 0
        desertTbCeaseFire = false
        desertTbHitWall = false
        desertPlayerHitWall = false
        desertTbFiringAtCrystal = false
        desertTbFireAtCrystalTimer = 0f
        desertAllBuildingsDestroyedTimer = -1f
        desertCiviliansVisible = false
        desertPlayerFiredAtCivilians = false
        desertPlayerTargeted.clear()
        desertTbTargeted.clear()
        desertSettlementVisibleTimer = -1f
        desertCameraFrozen = false
        desertDriveOffActive = false
        desertWakeUpTimer = 0f

        // Clear radio state
        state.radioMessage = null
        state.radioSpeaker = null
        state.radioTimer = 0f

        // Desert audio: stop combat music, start desert ambient
        SoundManager.stopCombatMusic()
        SoundManager.playAmbient("bgm_desert")
    }

    private fun updateDesert(deltaTime: Float) {
        state.desertTimer += deltaTime
        state.desertNoInputTimer += deltaTime

        if (state.isPaused) {
            updatePaused(deltaTime)
            return
        }

        // Populate scratch lists for desert entity iteration
        EntityPools.projectiles.getActiveEntities(activeProjectiles)
        EntityPools.enemies.getActiveEntities(activeEnemies)

        // Tank-style player movement
        val touchX = touchController.moveDirection.x * touchController.moveMagnitude
        val touchY = touchController.moveDirection.y * touchController.moveMagnitude

        if (touchController.moveMagnitude > 0.1f) {
            // Desired direction from touch
            val desiredAngle = atan2(touchY.toDouble(), touchX.toDouble()).toFloat()

            // Turn hull toward desired direction (rate limited)
            val tankTurnRate = 2.0f  // radians per second
            val angleDiff = normalizeAngle(desiredAngle - ship.rotation)
            val maxTurn = tankTurnRate * deltaTime
            ship.rotation += angleDiff.coerceIn(-maxTurn, maxTurn)

            // Determine if we're going forward or backward relative to facing
            val dot = cos(desiredAngle - ship.rotation)  // 1 = same dir, -1 = opposite
            val targetSpeed = if (dot > 0) 200f * touchController.moveMagnitude else -100f * touchController.moveMagnitude

            // Accelerate/decelerate
            val accel = if (abs(targetSpeed) > abs(desertPlayerSpeed)) 300f else 400f
            desertPlayerSpeed = moveToward(desertPlayerSpeed, targetSpeed, accel * deltaTime)
        } else {
            // No input — decelerate to stop
            desertPlayerSpeed = moveToward(desertPlayerSpeed, 0f, 400f * deltaTime)
        }

        // Move in facing direction
        ship.position.x += cos(ship.rotation) * desertPlayerSpeed * deltaTime
        ship.position.y += sin(ship.rotation) * desertPlayerSpeed * deltaTime

        // Clamp player to canyon corridor
        ship.position.x = ship.position.x.coerceIn(DESERT_CORRIDOR_LEFT + 25f, DESERT_CORRIDOR_RIGHT - 25f)

        // Prevent going south past the waterline
        val waterlineY = desertSpawnY + screenHeight * 0.5f + (screenHeight * 0.5f) * 0.8f
        ship.position.y = ship.position.y.coerceAtMost(waterlineY - 25f)

        // Crystal energy wall at settlement southern edge (horror path)
        if (desertSettlementProgress > 0f && desertBuildings.isNotEmpty()) {
            val wallY = desertSettlementWorldY + 220f
            if (ship.position.y <= wallY + 30f && !desertPlayerHitWall) {
                desertPlayerHitWall = true
            }
            ship.position.y = ship.position.y.coerceAtLeast(wallY)
        }

        // Solid beach landing craft — box derived from renderDesertBeachAndSea's own math
        run {
            val beachStartY = desertSpawnY + screenHeight * 0.5f
            val beachEndY = desertSpawnY + screenHeight
            val wetSandTop = beachStartY + (beachEndY - beachStartY) * 0.6f
            val craftX = 30f
            val craftY = wetSandTop + 10f
            val (nx, ny) = Collision2D.resolveCircleOutOfAabb(
                ship.position.x, ship.position.y, ship.radius,
                craftX - 35f, craftY - 40f, craftX + 35f, craftY + 60f
            )
            ship.position.x = nx
            ship.position.y = ny
        }

        // Keep player invulnerable
        ship.health = ship.maxHealth
        ship.currentShield = ship.maxShield

        // Update camera to follow player (frozen during good ending drive-off)
        if (!desertCameraFrozen) {
            camera.update(ship)
        }

        // Update TB (follows player, or walks to crystal)
        updateDesertTb(deltaTime)

        // Update entities
        updateDesertEntities(deltaTime)

        // Update settlement civilians — flee north on bombardment
        for (civ in desertCivilians) {
            if (civ.fleeing) {
                civ.speed = moveToward(civ.speed, 120f, 200f * deltaTime)
                civ.x += cos(civ.angle) * civ.speed * deltaTime
                civ.y += sin(civ.angle) * civ.speed * deltaTime
            }
        }

        // Second wave spawn delay (2s after callout)
        if (desertSecondWaveSpawnDelay >= 0f) {
            desertSecondWaveSpawnDelay += deltaTime
            if (desertSecondWaveSpawnDelay >= 2f) {
                spawnDesertEnemies(3, military = true)
                desertSecondWaveSpawnDelay = -1f
                desertSpawnTimer = 0f
            }
        }

        // Bombardment delay: 5s after "You have your orders" before shooting starts
        if (desertBombardmentDelayTimer >= 0f) {
            desertBombardmentDelayTimer += deltaTime
            if (desertBombardmentDelayTimer >= 5f) {
                desertBombardmentActive = true
                desertBombardmentDelayTimer = -1f
            }
        }

        // Bombardment auto-fire at buildings and fleeing civilians (player only — TB refuses)
        if (desertBombardmentActive) {
            desertPlayerFireTimer += deltaTime
            if (desertPlayerFireTimer >= 0.6f) {
                // Collect all targets: alive buildings + fleeing civilians + enemy soldiers
                data class BombardTarget(val x: Float, val y: Float, val priority: Int)
                val targets = mutableListOf<BombardTarget>()
                for (civ in desertCivilians) {
                    if (!civ.fleeing) continue
                    val sy = civ.y - camera.y
                    if (sy > 0f && sy < screenHeight) targets.add(BombardTarget(civ.x, civ.y, 0))
                }
                for (enemy in activeEnemies) {
                    if (enemy.isActive && enemy.position.y >= desertSettlementWorldY - 50f)
                        targets.add(BombardTarget(enemy.position.x, enemy.position.y, 1))
                }
                for (b in desertBuildings) {
                    if (b.alive) targets.add(BombardTarget(b.x + b.w / 2f, b.y + b.h / 2f, 2))
                }
                if (targets.isNotEmpty()) {
                    desertPlayerFireTimer = 0f
                    // Trigger civilian flee on first bombardment shot
                    if (desertCivilians.any { !it.fleeing }) {
                        for (civ in desertCivilians) {
                            civ.fleeing = true
                            civ.angle = (-Math.PI / 2f).toFloat() + (Math.random().toFloat() - 0.5f) * 0.15f
                        }
                    }
                    val sortedTargets = targets.sortedWith(compareBy({ it.priority }, {
                        val dx = it.x - ship.position.x
                        val dy = it.y - ship.position.y
                        dx * dx + dy * dy
                    })).take(8)
                    for (target in sortedTargets) {
                        val dx = target.x - ship.position.x
                        val dy = target.y - ship.position.y
                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        if (dist > 0f) {
                            val angle = atan2(dy, dx)
                            val proj = EntityPools.projectiles.obtain()
                            proj.position.set(ship.position.x, ship.position.y)
                            proj.velocity.set(cos(angle) * 400f, sin(angle) * 400f)
                            proj.damage = 100f
                            proj.color = 0xFFFF6644.toInt()
                            proj.radius = 5f
                            proj.type = ProjectileType.MISSILE
                            proj.homingStrength = 3f
                            proj.lifetime = 2.5f
                            proj.isEnemyProjectile = false
                        }
                    }
                    SoundManager.playSFX("sfx_tank_shot", 0.35f)
                }
            }
            // Check projectile hits on buildings
            for (proj in activeProjectiles) {
                if (!proj.isActive || proj.isEnemyProjectile) continue
                for (b in desertBuildings) {
                    if (!b.alive) continue
                    if (proj.position.x >= b.x && proj.position.x <= b.x + b.w &&
                        proj.position.y >= b.y && proj.position.y <= b.y + b.h) {
                        b.alive = false
                        b.deathStyle = if (Math.random() < 0.5) BuildingDeathStyle.CHARRED else BuildingDeathStyle.RUBBLE
                        b.burnTimer = 0f
                        proj.isActive = false
                        visualEffects.addExplosion(b.x + b.w / 2f, b.y + b.h / 2f, b.w * 1.5f, 0xFFFFAA44.toInt())
                        SoundManager.playSFX("sfx_explosion")
                        break
                    }
                }
            }
            // Player missiles destroy fleeing vehicles — explosion + burning wreck
            for (proj in activeProjectiles) {
                if (!proj.isActive || proj.isEnemyProjectile) continue
                val iter = desertCivilians.iterator()
                while (iter.hasNext()) {
                    val civ = iter.next()
                    if (!civ.fleeing) continue
                    val cdx = proj.position.x - civ.x
                    val cdy = proj.position.y - civ.y
                    if (cdx * cdx + cdy * cdy <= 22f * 22f) {
                        proj.isActive = false
                        visualEffects.addExplosion(civ.x, civ.y, 70f, 0xFFFF8844.toInt())
                        desertWrecks.add(DesertWreck(civ.x, civ.y, civ.angle, 0xFFAA9966.toInt(), false))
                        SoundManager.playSFX("sfx_explosion")
                        iter.remove()
                        break
                    }
                }
            }
            // Track aftermath timer: 4s after all buildings destroyed
            if (desertAllBuildingsDestroyedTimer < 0f && desertBuildings.isNotEmpty() && desertBuildings.none { it.alive }) {
                desertAllBuildingsDestroyedTimer = 0f
            }
            if (desertAllBuildingsDestroyedTimer >= 0f) {
                desertAllBuildingsDestroyedTimer += deltaTime
            }
        }
        // Update building burn timers for destroyed buildings
        for (b in desertBuildings) {
            if (!b.alive) b.burnTimer += deltaTime
        }

        // Track how long settlement has been visible (for bombardment delay)
        if (desertBuildings.isNotEmpty() && desertSettlementVisibleTimer < 0f) {
            val distToSettlement = abs(ship.position.y - desertSettlementWorldY)
            if (distToSettlement < screenHeight * 0.8f) {
                desertSettlementVisibleTimer = 0f  // Start counting
            }
        }
        if (desertSettlementVisibleTimer >= 0f) {
            desertSettlementVisibleTimer += deltaTime
        }

        // Check if civilians are visible on screen
        if (!desertCiviliansVisible && desertCivilians.isNotEmpty()) {
            for (civ in desertCivilians) {
                val screenY = civ.y - camera.y
                if (screenY > 0f && screenY < screenHeight) {
                    desertCiviliansVisible = true
                    break
                }
            }
        }

        // Spawn enemies based on phase
        updateDesertSpawning(deltaTime)

        // Collision detection
        updateDesertCollisions()

        // Radio timer tick
        radioSystem.update(deltaTime, state)

        // Dialogue progression
        updateDesertDialogue(deltaTime)

        // Phase transitions
        updateDesertPhaseTransitions(deltaTime)

        // Visual effects (explosions)
        visualEffects.update(deltaTime)

        // Update wreck burn timers
        for (wreck in desertWrecks) { wreck.burnTimer += deltaTime }

        // Vehicle tracks
        desertTrackSpawnTimer += deltaTime
        if (desertTrackSpawnTimer >= 0.08f) {
            desertTrackSpawnTimer = 0f

            // Player tank tracks (only when moving)
            if (abs(desertPlayerSpeed) > 5f) {
                desertTracks.add(VehicleTrack(ship.position.x, ship.position.y, ship.rotation, 30f))
            }

            // TB tank tracks
            if (abs(desertTbSpeed) > 5f) {
                desertTracks.add(VehicleTrack(desertTbX, desertTbY, desertTbAngle, 30f))
            }

            // Enemy tracks
            for (enemy in activeEnemies) {
                if (!enemy.isActive) continue
                val speed = sqrt(enemy.velocity.x * enemy.velocity.x + enemy.velocity.y * enemy.velocity.y)
                if (speed > 5f) {
                    desertTracks.add(VehicleTrack(enemy.position.x, enemy.position.y, enemy.rotation, 20f))
                }
            }

            // Civilian vehicle tracks
            for (civ in desertCivilians) {
                if (civ.fleeing && civ.speed > 5f) {
                    desertTracks.add(VehicleTrack(civ.x, civ.y, civ.angle, 20f))
                }
            }

            // Cap tracks
            while (desertTracks.size > MAX_TRACKS) {
                desertTracks.removeAt(0)
            }
        }

        // Age tracks (freeze during drive-off so visible tracks stay)
        if (!desertDriveOffActive) {
            val trackIter = desertTracks.iterator()
            while (trackIter.hasNext()) {
                val track = trackIter.next()
                track.age += deltaTime
                if (track.age >= TRACK_LIFETIME) {
                    trackIter.remove()
                }
            }
        }

        // Dust particles — spawn behind moving tanks
        if (abs(desertPlayerSpeed) > 10f) {
            desertDustParticles.add(DustParticle(
                ship.position.x + kotlin.random.Random.nextFloat() * 10f - 5f,
                ship.position.y + 20f,
                200f, 0f
            ))
        }
        if (abs(desertTbSpeed) > 10f) {
            desertDustParticles.add(DustParticle(
                desertTbX + kotlin.random.Random.nextFloat() * 10f - 5f,
                desertTbY + 20f,
                200f, 0f
            ))
        }
        // Age and remove dust particles
        val dustIter = desertDustParticles.iterator()
        while (dustIter.hasNext()) {
            val dust = dustIter.next()
            dust.age += deltaTime
            dust.alpha = 200f * (1f - dust.age / 1.0f)
            if (dust.age >= 1.0f) dustIter.remove()
        }
        // Cap dust particles
        while (desertDustParticles.size > 100) {
            desertDustParticles.removeAt(0)
        }

        // Clean up inactive entities
        EntityPools.cleanupInactive()
    }

    private fun updateDesertTb(deltaTime: Float) {
        if (!desertTbAlive) return

        // Choose target: crystal or player
        val targetX: Float
        val targetY: Float
        if (desertTbWalkingToCrystal) {
            targetX = desertTbCrystalTarget[0]
            targetY = desertTbCrystalTarget[1]
        } else {
            val offsetX = if (ship.position.x > 0f) -120f else 120f
            targetX = ship.position.x + offsetX
            targetY = ship.position.y + 80f
        }

        val dx = targetX - desertTbX
        val dy = targetY - desertTbY
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // Detect TB reaching the crystal wall — only when walking to crystal, not during normal following
        if (!desertTbHitWall && desertBuildings.isNotEmpty() && desertTbWalkingToCrystal) {
            val wallY = desertSettlementWorldY + 220f
            if (desertTbY <= wallY) {
                desertTbHitWall = true
            }
        }

        // Start firing cosmetic shots once TB is 30px past the wall; stop when drain begins
        if (desertTbHitWall && desertCrystalDrainPhase == 0) {
            val wallY = desertSettlementWorldY + 220f
            if (desertTbY <= wallY - 30f) {
                desertTbFiringAtCrystal = true
            }
        }
        if (desertCrystalDrainPhase > 0) {
            desertTbFiringAtCrystal = false
        }

        if (desertTbFiringAtCrystal) {
            desertTbFireAtCrystalTimer += deltaTime
            if (desertTbFireAtCrystalTimer >= 0.8f) {
                desertTbFireAtCrystalTimer = 0f
                val crystalX = desertTbCrystalTarget[0]
                val crystalY = desertTbCrystalTarget[1]
                val projDx = crystalX - desertTbX
                val projDy = crystalY - desertTbY
                val projDist = sqrt((projDx * projDx + projDy * projDy).toDouble()).toFloat()
                if (projDist > 0f) {
                    val proj = EntityPools.projectiles.obtain()
                    proj.isActive = true
                    proj.position.set(desertTbX, desertTbY)
                    proj.velocity.set(projDx / projDist * 300f, projDy / projDist * 300f)
                    proj.type = ProjectileType.BULLET
                    proj.color = 0xFF88BBFF.toInt()
                    proj.radius = 4f
                    proj.damage = 0f
                    proj.lifetime = projDist / 300f + 0.1f
                    proj.age = 0f
                    proj.isEnemyProjectile = false
                }
            }
        }

        if (dist > 30f) {
            val desiredAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
            val angleDiff = normalizeAngle(desiredAngle - desertTbAngle)
            val turnRate = 2.5f
            val maxTurn = turnRate * deltaTime
            desertTbAngle += angleDiff.coerceIn(-maxTurn, maxTurn)

            val baseSpeed = if (desertTbWalkingToCrystal) 100f else minOf(250f, dist * 2f)
            val targetSpeed = if (desertTbHitWall) baseSpeed * 0.5f else baseSpeed
            desertTbSpeed = moveToward(desertTbSpeed, targetSpeed, 300f * deltaTime)
        } else {
            desertTbSpeed = moveToward(desertTbSpeed, 0f, 400f * deltaTime)
        }

        desertTbX += cos(desertTbAngle) * desertTbSpeed * deltaTime
        desertTbY += sin(desertTbAngle) * desertTbSpeed * deltaTime

        desertTbX = desertTbX.coerceIn(DESERT_CORRIDOR_LEFT + 25f, DESERT_CORRIDOR_RIGHT - 25f)
        // Prevent TB from driving past the waterline
        val tbWaterlineY = desertSpawnY + screenHeight * 0.5f + (screenHeight * 0.5f) * 0.8f
        desertTbY = desertTbY.coerceAtMost(tbWaterlineY - 25f)

        // North fence clamp — prevent crossing into settlement while following
        if (!desertTbWalkingToCrystal && desertSettlementWorldY != 0f) {
            desertTbY = desertTbY.coerceAtLeast(desertSettlementWorldY + 220f)
        }
    }

    private fun moveToward(current: Float, target: Float, maxDelta: Float): Float {
        return if (abs(target - current) <= maxDelta) target
        else current + sign(target - current) * maxDelta
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > Math.PI) a -= (2 * Math.PI).toFloat()
        while (a < -Math.PI) a += (2 * Math.PI).toFloat()
        return a
    }

    private fun updateDesertEntities(deltaTime: Float) {
        // Update projectiles
        val projectiles = activeProjectiles
        val enemies = activeEnemies
        for (proj in projectiles) {
            if (!proj.isActive) continue
            proj.age += deltaTime

            // Homing missiles track nearest enemy or building
            if (proj.type == ProjectileType.MISSILE && !proj.isEnemyProjectile && proj.homingStrength > 0f) {
                // Find nearest target: enemy or alive building
                var targetX = Float.MAX_VALUE
                var targetY = Float.MAX_VALUE
                var bestDist = Float.MAX_VALUE
                for (e in enemies) {
                    if (!e.isActive) continue
                    val dx = e.position.x - proj.position.x
                    val dy = e.position.y - proj.position.y
                    val d = dx * dx + dy * dy
                    if (d < bestDist) { bestDist = d; targetX = e.position.x; targetY = e.position.y }
                }
                if (desertBombardmentActive) {
                    for (b in desertBuildings) {
                        if (!b.alive) continue
                        val bx = b.x + b.w / 2f
                        val by = b.y + b.h / 2f
                        val dx = bx - proj.position.x
                        val dy = by - proj.position.y
                        val d = dx * dx + dy * dy
                        if (d < bestDist) { bestDist = d; targetX = bx; targetY = by }
                    }
                    for (civ in desertCivilians) {
                        if (!civ.fleeing) continue
                        val sy = civ.y - camera.y
                        if (sy <= 0f || sy >= screenHeight) continue
                        val dx = civ.x - proj.position.x
                        val dy = civ.y - proj.position.y
                        val d = dx * dx + dy * dy
                        if (d < bestDist) { bestDist = d; targetX = civ.x; targetY = civ.y }
                    }
                }
                if (bestDist < Float.MAX_VALUE) {
                    val dx = targetX - proj.position.x
                    val dy = targetY - proj.position.y
                    val targetAngle = atan2(dy, dx)
                    val currentAngle = atan2(proj.velocity.y, proj.velocity.x)
                    var angleDiff = targetAngle - currentAngle
                    while (angleDiff > Math.PI) angleDiff -= (2 * Math.PI).toFloat()
                    while (angleDiff < -Math.PI) angleDiff += (2 * Math.PI).toFloat()
                    val turnAmount = proj.homingStrength * deltaTime
                    val newAngle = currentAngle + angleDiff.coerceIn(-turnAmount, turnAmount)
                    val speed = sqrt(proj.velocity.x * proj.velocity.x + proj.velocity.y * proj.velocity.y)
                    proj.velocity.set(cos(newAngle) * speed, sin(newAngle) * speed)
                }
            }

            proj.position.x += proj.velocity.x * deltaTime
            proj.position.y += proj.velocity.y * deltaTime

            // Absorb cosmetic TB→crystal shots when they reach the crystal (no explosion)
            if (!proj.isEnemyProjectile && proj.damage == 0f && desertTbFiringAtCrystal) {
                val cx = desertTbCrystalTarget[0]
                val cy = desertTbCrystalTarget[1]
                val pdx = proj.position.x - cx
                val pdy = proj.position.y - cy
                if (pdx * pdx + pdy * pdy <= 30f * 30f) {
                    proj.isActive = false
                    continue
                }
            }

            if (proj.age >= proj.lifetime) {
                // Missile end-of-life: small visual explosion
                if (!proj.isEnemyProjectile) {
                    visualEffects.addExplosion(proj.position.x, proj.position.y, 12f, 0xFFFF8833.toInt())
                }
                proj.isActive = false
            }
        }

        // Update enemies (simple movement toward player)
        for (enemy in enemies) {
            if (!enemy.isActive) continue
            enemy.spawnTime += deltaTime

            val toPlayerX = ship.position.x - enemy.position.x
            val toPlayerY = ship.position.y - enemy.position.y
            val distToPlayer = sqrt((toPlayerX * toPlayerX + toPlayerY * toPlayerY).toDouble()).toFloat()

            when {
                state.desertPhase == 0 -> {
                    // Military pickups: aggressive pursuit
                    val desiredAngle = atan2(toPlayerY.toDouble(), toPlayerX.toDouble()).toFloat()
                    val angleDiff = normalizeAngle(desiredAngle - enemy.rotation)
                    val turnRate = 1.5f  // pickups turn slower than tanks
                    enemy.rotation += angleDiff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)

                    // Drive forward in facing direction
                    val targetSpeed = 160f
                    enemy.velocity.x = cos(enemy.rotation) * targetSpeed
                    enemy.velocity.y = sin(enemy.rotation) * targetSpeed
                }
                state.desertPhase >= 1 -> {
                    if (desertPlayerShotInPhase2) {
                        // Fleeing upward — away from player
                        val fleeAngle = (-Math.PI / 2f).toFloat()  // straight up
                        val angleDiff = normalizeAngle(fleeAngle - enemy.rotation)
                        val turnRate = 2.0f
                        enemy.rotation += angleDiff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
                        val targetSpeed = 120f
                        enemy.velocity.x = cos(enemy.rotation) * targetSpeed
                        enemy.velocity.y = sin(enemy.rotation) * targetSpeed
                    } else {
                        // Approach then veer away
                        if (distToPlayer > 200f) {
                            val desiredAngle = atan2(toPlayerY.toDouble(), toPlayerX.toDouble()).toFloat()
                            val angleDiff = normalizeAngle(desiredAngle - enemy.rotation)
                            val turnRate = 1.0f
                            enemy.rotation += angleDiff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
                            val targetSpeed = 120f
                            enemy.velocity.x = cos(enemy.rotation) * targetSpeed
                            enemy.velocity.y = sin(enemy.rotation) * targetSpeed
                        } else {
                            val awayAngle = atan2(-toPlayerY.toDouble(), -toPlayerX.toDouble()).toFloat()
                            val angleDiff = normalizeAngle(awayAngle - enemy.rotation)
                            val turnRate = 0.8f
                            enemy.rotation += angleDiff.coerceIn(-turnRate * deltaTime, turnRate * deltaTime)
                            val targetSpeed = 120f
                            enemy.velocity.x = cos(enemy.rotation) * targetSpeed
                            enemy.velocity.y = sin(enemy.rotation) * targetSpeed
                        }
                    }
                }
            }

            // Apply velocity
            enemy.position.x += enemy.velocity.x * deltaTime
            enemy.position.y += enemy.velocity.y * deltaTime
            // Clamp enemies to canyon corridor
            enemy.position.x = enemy.position.x.coerceIn(DESERT_CORRIDOR_LEFT + 15f, DESERT_CORRIDOR_RIGHT - 15f)

            // Phase 0 enemies fire continuous machine gun (0.2s cooldown)
            if (state.desertPhase == 0 && !enemy.isWarping && desertPlayerHasFired) {
                enemy.fireCooldown -= deltaTime
                if (enemy.fireCooldown <= 0f) {
                    enemy.fireCooldown = 0.2f
                    val edx = ship.position.x - enemy.position.x
                    val edy = ship.position.y - enemy.position.y
                    val edist = sqrt((edx * edx + edy * edy).toDouble()).toFloat()
                    if (edist > 0f && edist < 600f) {
                        val proj = EntityPools.projectiles.obtain()
                        proj.position.set(enemy.position.x, enemy.position.y)
                        val projSpeed = 600f
                        val angle = atan2(edy, edx)
                        proj.velocity.set(cos(angle) * projSpeed, sin(angle) * projSpeed)
                        proj.damage = 2f
                        proj.lifetime = 5f
                        proj.isEnemyProjectile = true
                        proj.color = 0xFF667788.toInt()  // gunmetal grey
                        proj.radius = 3f
                        SoundManager.playSFX("sfx_desert_enemy_gun", 0.21f)
                    }
                }
            }

            // Despawn enemies far off screen. The radius must stay viewport-relative:
            // spawns land up to ~0.77 viewport-heights from the player, so a fixed
            // radius silently culled fresh spawns on tall screens (and deadlocked the
            // scene on the fallback-less FIRST_KILL line).
            if (camera.isTooFar(enemy.position.x, enemy.position.y, desertDespawnDistance(screenHeight))) {
                enemy.isActive = false
            }
        }

        // Auto-fire player tank homing missiles at desert enemies and civilians
        if ((state.desertPhase < 2 || (state.desertPhase == 2 && !desertBombardmentActive)) && state.desertPhase != 4) {
            val enemies2 = activeEnemies
            // Only fire at targets past top 15% of screen
            val enemyTargets = enemies2.filter { it.isActive && !it.isWarping &&
                (it.position.y - camera.y) > screenHeight * 0.15f &&
                (!desertBombardmentActive || it.position.y >= desertSettlementWorldY - 50f) }
            // Only target civilians during active bombardment (not before command is given)
            val civTargets = if (desertBombardmentActive) {
                desertCivilians.filter {
                    val sy = it.y - camera.y
                    sy > screenHeight * 0.15f && sy < screenHeight
                }
            } else emptyList()
            if (enemyTargets.isNotEmpty() || civTargets.isNotEmpty()) {
                desertPlayerFireTimer += deltaTime
                val fireInterval = 0.5f
                if (desertPlayerFireTimer >= fireInterval) {
                    desertPlayerFireTimer = 0f
                    // Find nearest target (enemy or civilian)
                    var nearestX = 0f
                    var nearestY = 0f
                    var nearestDist = Float.MAX_VALUE
                    var targetIsCivilian = false
                    for (e in enemyTargets) {
                        val ndx = e.position.x - ship.position.x
                        val ndy = e.position.y - ship.position.y
                        val d = ndx * ndx + ndy * ndy
                        if (d < nearestDist) {
                            nearestDist = d; nearestX = e.position.x; nearestY = e.position.y
                            targetIsCivilian = false
                        }
                    }
                    for (c in civTargets) {
                        val ndx = c.x - ship.position.x
                        val ndy = c.y - ship.position.y
                        val d = ndx * ndx + ndy * ndy
                        if (d < nearestDist) {
                            nearestDist = d; nearestX = c.x; nearestY = c.y
                            targetIsCivilian = true
                        }
                    }
                    if (nearestDist < Float.MAX_VALUE) {
                        if (targetIsCivilian) desertPlayerFiredAtCivilians = true
                        val dx = nearestX - ship.position.x
                        val dy = nearestY - ship.position.y
                        val angle = atan2(dy, dx)
                        val proj = EntityPools.projectiles.obtain()
                        proj.position.set(ship.position.x, ship.position.y)
                        proj.velocity.set(cos(angle) * 400f, sin(angle) * 400f)
                        proj.damage = 20f
                        proj.color = 0xFFFF6644.toInt()
                        proj.radius = 5f
                        proj.type = ProjectileType.MISSILE
                        proj.homingStrength = 3f
                        proj.lifetime = 2.5f
                        proj.isEnemyProjectile = false
                        SoundManager.playSFX("sfx_tank_shot", 0.35f)
                        if (!desertPlayerHasFired) desertPlayerHasFired = true
                    }
                }
            }
        }

        // Rotate player turret toward nearest target (enemy, civilian, or building during bombardment)
        var playerTargetX = Float.MAX_VALUE
        var playerTargetY = Float.MAX_VALUE
        var playerBestDist = Float.MAX_VALUE
        for (e in enemies) {
            if (!e.isActive || e.isWarping) continue
            if (desertBombardmentActive && e.position.y < desertSettlementWorldY - 50f) continue
            val ndx = e.position.x - ship.position.x
            val ndy = e.position.y - ship.position.y
            val d = ndx * ndx + ndy * ndy
            if (d < playerBestDist) { playerBestDist = d; playerTargetX = e.position.x; playerTargetY = e.position.y }
        }
        // Only aim at civilians during active bombardment
        if (desertBombardmentActive) {
            for (c in desertCivilians) {
                val ndx = c.x - ship.position.x
                val ndy = c.y - ship.position.y
                val d = ndx * ndx + ndy * ndy
                if (d < playerBestDist) { playerBestDist = d; playerTargetX = c.x; playerTargetY = c.y }
            }
        }
        if (desertBombardmentActive) {
            for (b in desertBuildings) {
                if (!b.alive) continue
                val bx = b.x + b.w / 2f; val by = b.y + b.h / 2f
                val ndx = bx - ship.position.x; val ndy = by - ship.position.y
                val d = ndx * ndx + ndy * ndy
                if (d < playerBestDist) { playerBestDist = d; playerTargetX = bx; playerTargetY = by }
            }
        }

        if (playerBestDist < Float.MAX_VALUE) {
            val targetAngle = atan2(
                (playerTargetY - ship.position.y).toDouble(),
                (playerTargetX - ship.position.x).toDouble()
            ).toFloat()
            val turretTurnRate = 3.0f
            val diff = normalizeAngle(targetAngle - desertPlayerTurretAngle)
            desertPlayerTurretAngle += diff.coerceIn(-turretTurnRate * deltaTime, turretTurnRate * deltaTime)
        } else {
            // No targets — turret returns to hull facing
            val diff = normalizeAngle(ship.rotation - desertPlayerTurretAngle)
            desertPlayerTurretAngle += diff.coerceIn(-2f * deltaTime, 2f * deltaTime)
        }

        // Rotate TB turret toward nearest enemy (TB doesn't fire at buildings)
        val nearestEnemy = enemies.filter { it.isActive && !it.isWarping }.minByOrNull {
            val ndx = it.position.x - desertTbX
            val ndy = it.position.y - desertTbY
            ndx * ndx + ndy * ndy
        }
        if (nearestEnemy != null) {
            val targetAngle = atan2(
                (nearestEnemy.position.y - desertTbY).toDouble(),
                (nearestEnemy.position.x - desertTbX).toDouble()
            ).toFloat()
            val diff = normalizeAngle(targetAngle - desertTbTurretAngle)
            desertTbTurretAngle += diff.coerceIn(-3f * deltaTime, 3f * deltaTime)
        } else {
            val diff = normalizeAngle(desertTbAngle - desertTbTurretAngle)
            desertTbTurretAngle += diff.coerceIn(-2f * deltaTime, 2f * deltaTime)
        }

        // TB tank fires homing missiles at enemies (stops after cease fire)
        if (state.desertPhase <= 1 && desertPlayerHasFired && desertTbAlive && !desertTbWalkingToCrystal && !desertTbCeaseFire) {
            desertTbFireTimer += deltaTime
            if (desertTbFireTimer >= 0.6f) {
                // Only fire at targets past top 15% of screen
                val tbCandidates = enemies.filter { it.isActive && !it.isWarping &&
                    (it.position.y - camera.y) > screenHeight * 0.15f }
                // Phase 1: filter out already-targeted non-combatants
                val tbValidTargets = if (state.desertPhase == 1) {
                    tbCandidates.filter { it.hashCode() !in desertTbTargeted }
                } else tbCandidates
                val tbTarget = tbValidTargets.minByOrNull {
                    val tdx = it.position.x - desertTbX
                    val tdy = it.position.y - desertTbY
                    tdx * tdx + tdy * tdy
                }
                if (tbTarget != null) {
                    desertTbFireTimer = 0f
                    if (state.desertPhase == 1) desertTbTargeted.add(tbTarget.hashCode())
                    val tdx = tbTarget.position.x - desertTbX
                    val tdy = tbTarget.position.y - desertTbY
                    val tangle = atan2(tdy, tdx)
                    val proj = EntityPools.projectiles.obtain()
                    proj.position.set(desertTbX, desertTbY)
                    proj.velocity.set(cos(tangle) * 400f, sin(tangle) * 400f)
                    proj.damage = 12f
                    proj.color = 0xFFFF6644.toInt()
                    proj.radius = 5f
                    proj.type = ProjectileType.MISSILE
                    proj.homingStrength = 3f
                    proj.lifetime = 2.5f
                    proj.isEnemyProjectile = false
                    SoundManager.playSFX("sfx_tank_shot", 0.175f)
                }
            }
        }
    }

    private fun updateDesertSpawning(deltaTime: Float) {
        desertSpawnTimer += deltaTime

        when (state.desertPhase) {
            0 -> {
                // Phase 0 (fun): First enemies spawned by FIRST_ENEMIES dialogue trigger
                // Second wave flag set by dialogue trigger (SECOND_WAVE), spawn handled by delay timer
                // Periodic spawns resume only after "More contacts" callout
                if (desertThirdWaveAnnounced && desertSpawnTimer >= 12f) {
                    spawnDesertEnemies(2, military = true)
                    desertSpawnTimer = 0f
                }
            }
            1 -> {
                // Phase 1 (escalation): Spawn ambiguous targets periodically
                if (!desertAmbiguousTargetSpawned && desertSpawnTimer >= 5f) {
                    desertAmbiguousTargetSpawned = true
                    spawnDesertEnemies(2, military = false)
                    desertSpawnTimer = 0f
                }
                if (desertAmbiguousTargetSpawned && desertSpawnTimer >= 10f) {
                    spawnDesertEnemies(1, military = false)
                    desertSpawnTimer = 0f
                }
            }
            // Phase 2+ : no spawning
        }
    }

    private fun spawnDesertEnemies(count: Int, military: Boolean) {
        for (i in 0 until count) {
            val enemy = EntityPools.enemies.obtain()
            // Spawn above camera view, within corridor
            val corridorMargin = 50f
            enemy.position.x = DESERT_CORRIDOR_LEFT + corridorMargin +
                Math.random().toFloat() * (DESERT_CORRIDOR_WIDTH - 2 * corridorMargin)
            val screenH = camera.getScreenHeight()
            enemy.position.y = camera.y - screenH * DESERT_SPAWN_BAND_NEAR -
                Math.random().toFloat() * screenH * DESERT_SPAWN_BAND_SPREAD
            enemy.maxHealth = if (military) 40f else 20f
            enemy.health = enemy.maxHealth
            enemy.radius = 18f
            enemy.speed = if (military) 80f else 40f
            enemy.fireRate = if (military) 3f else Float.MAX_VALUE  // Ambiguous targets don't fire
            enemy.fireCooldown = 0f  // Fire immediately once allowed
            enemy.spawnShieldTimer = 0f  // No spawn shield in desert
            enemy.spawnTime = 0f
            // Enemies move downward (toward player) with slight lateral drift
            val lateralDrift = (Math.random().toFloat() - 0.5f) * enemy.speed * 0.3f
            enemy.velocity.set(
                lateralDrift,
                enemy.speed * 0.5f  // positive Y = downward toward player
            )
            enemy.color = if (military) 0xFF886644.toInt() else 0xFFAA9966.toInt()
        }
    }

    private fun updateDesertCollisions() {
        val projectiles = activeProjectiles
        val enemies = activeEnemies

        for (proj in projectiles) {
            if (!proj.isActive || proj.isEnemyProjectile) continue
            for (enemy in enemies) {
                if (!enemy.isActive) continue
                val dx = proj.position.x - enemy.position.x
                val dy = proj.position.y - enemy.position.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist < enemy.radius + proj.radius) {
                    enemy.health -= proj.damage
                    proj.isActive = false
                    visualEffects.addHitFlash(proj.position.x, proj.position.y, 10f)

                    if (enemy.health <= 0f) {
                        // Enemy killed — leave burning wreck
                        desertWrecks.add(DesertWreck(enemy.position.x, enemy.position.y, enemy.rotation, enemy.color, state.desertPhase == 0))
                        enemy.isActive = false
                        visualEffects.addExplosion(enemy.position.x, enemy.position.y, enemy.radius * 2.5f,
                            if (state.desertPhase == 0) 0xFFFFAA44.toInt() else 0xFFCC8844.toInt())
                        SoundManager.playSFX("sfx_explosion")
                        desertKillCount++
                        if (!desertFirstKill) desertFirstKill = true
                        // Clear from targeting sets so tanks can pick new targets
                        desertPlayerTargeted.remove(enemy.hashCode())
                        desertTbTargeted.remove(enemy.hashCode())

                        // Track kills in escalation phase
                        if (state.desertPhase >= 1) {
                            if (!desertPlayerShotInPhase2) desertPlayerShotInPhase2 = true
                            else desertPlayerContinuedShooting = true
                            if (!desertPlayerFiredAtCivilians) desertPlayerFiredAtCivilians = true
                        }
                    }
                    break
                }
            }
        }

        // Enemy projectiles hitting player (just visual — player is invulnerable)
        for (proj in projectiles) {
            if (!proj.isActive || !proj.isEnemyProjectile) continue
            val dx = proj.position.x - ship.position.x
            val dy = proj.position.y - ship.position.y
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist < ship.radius + proj.radius) {
                proj.isActive = false
                visualEffects.addHitFlash(proj.position.x, proj.position.y, 8f, 0xFF88BBFF.toInt())
            }
        }
    }

    /** True when at least one non-combatant ambiguous vehicle (fireRate == MAX) is on screen.
     *  Used to gate the approach dialogue so TB only narrates contacts the player can see. */
    private fun anyAmbiguousVehicleVisible(): Boolean {
        for (enemy in activeEnemies) {
            if (!enemy.isActive) continue
            if (enemy.fireRate != Float.MAX_VALUE) continue
            val screenY = enemy.position.y - camera.y
            if (screenY > 0f && screenY < screenHeight) return true
        }
        return false
    }

    private fun updateDesertDialogue(deltaTime: Float) {
        state.desertDialogueTimer += deltaTime

        val lines = when (state.desertPhase) {
            0 -> DesertDefinitions.phase1Lines
            1 -> DesertDefinitions.phase2Lines
            2 -> DesertDefinitions.horrorLines
            3 -> DesertDefinitions.crystalLines
            4 -> DesertDefinitions.goodEndingLines
            else -> return
        }

        if (state.desertDialogueStep >= lines.size) return

        val line = lines[state.desertDialogueStep]
        val canAdvance = when (line.trigger) {
            DesertDefinitions.DesertTrigger.TIMER -> state.desertDialogueTimer >= line.delay
            DesertDefinitions.DesertTrigger.SCENE_START -> state.desertTimer >= line.delay
            DesertDefinitions.DesertTrigger.FIRST_ENEMIES -> state.desertDialogueTimer >= line.delay
            DesertDefinitions.DesertTrigger.FIRST_KILL -> desertFirstKill
            DesertDefinitions.DesertTrigger.SECOND_WAVE -> (desertKillCount >= 2 && state.desertDialogueTimer >= 3f) || state.desertDialogueTimer >= 30f
            DesertDefinitions.DesertTrigger.AFTER_KILLS -> desertKillCount >= 5 || state.desertDialogueTimer >= 30f
            DesertDefinitions.DesertTrigger.PHASE2_START -> state.desertPhase == 1 && state.desertDialogueTimer >= 0f
            DesertDefinitions.DesertTrigger.AMBIGUOUS_TARGET -> (desertAmbiguousTargetSpawned && anyAmbiguousVehicleVisible()) || state.desertDialogueTimer >= 20f
            DesertDefinitions.DesertTrigger.NEAR_SETTLEMENT -> {
                // 5 seconds after wall hit acknowledgment before bombardment order
                desertPlayerHitWall && state.desertDialogueTimer >= 5f
            }
            DesertDefinitions.DesertTrigger.BOMBARDMENT_DELAY -> {
                // Delay after "You have your orders" before bombardment starts
                state.desertDialogueTimer >= line.delay
            }
            DesertDefinitions.DesertTrigger.PLAYER_SHOOTS -> desertPlayerShotInPhase2 || state.desertDialogueTimer >= 15f
            DesertDefinitions.DesertTrigger.PLAYER_CONTINUES -> desertPlayerContinuedShooting || state.desertDialogueTimer >= 10f
            DesertDefinitions.DesertTrigger.STOP_CHECK -> state.desertDialogueTimer >= line.delay
            DesertDefinitions.DesertTrigger.PLAYER_FIRES_AFTER_STOP -> desertPlayerFiredAfterStop || state.desertDialogueTimer >= 15f
            DesertDefinitions.DesertTrigger.ALL_CLEARED -> (EntityPools.enemies.activeCount() == 0 && desertKillCount > 0) || state.desertDialogueTimer >= 30f
            DesertDefinitions.DesertTrigger.AFTERMATH -> {
                // Strict gate: all buildings destroyed + 4s silence
                desertAllBuildingsDestroyedTimer >= 4f
            }
            // Phase 1 has no civilian entities (those spawn on the horror path); the line is
            // about the ambiguous targets, so fire it once they're in the world. Shorter
            // fallback so this never sits in dead air if a spawn slipped.
            DesertDefinitions.DesertTrigger.CIVILIANS_VISIBLE -> desertCiviliansVisible || desertAmbiguousTargetSpawned || state.desertDialogueTimer >= 10f
            DesertDefinitions.DesertTrigger.SIGNATURES_VISIBLE ->
                (anyAmbiguousVehicleVisible() && state.desertDialogueTimer >= line.delay) || state.desertDialogueTimer >= 15f
            DesertDefinitions.DesertTrigger.PLAYER_FIRES_AT_CIVILIANS -> desertPlayerFiredAtCivilians || state.desertDialogueTimer >= 15f
            DesertDefinitions.DesertTrigger.PLAYER_HIT_WALL -> desertPlayerHitWall
            DesertDefinitions.DesertTrigger.AUTO -> state.desertDialogueTimer >= line.delay
        }

        if (canAdvance && (line.interrupt || state.radioTimer <= 0f)) {
            radioSystem.showScriptedMessage(state, line.speaker, line.text, line.color)
            state.desertDialogueStep++
            state.desertDialogueTimer = 0f

            // Start 2s spawn delay after "More contacts" callout
            if (line.trigger == DesertDefinitions.DesertTrigger.SECOND_WAVE) {
                desertSecondWaveSpawnDelay = 0f  // Start 2s countdown
                desertThirdWaveAnnounced = true
                desertSpawnTimer = 0f
            }

            // Spawn first enemies on "Contact north" callout
            if (line.trigger == DesertDefinitions.DesertTrigger.FIRST_ENEMIES && !desertFirstEnemiesSpawned) {
                desertFirstEnemiesSpawned = true
                spawnDesertEnemies(2, military = true)
                desertSpawnTimer = 0f
            }

            // Spawn 3 enemies on "Getting busy out here"
            if (line.trigger == DesertDefinitions.DesertTrigger.AFTER_KILLS) {
                spawnDesertEnemies(3, military = true)
            }

            // Mark stop check reached
            if (line.trigger == DesertDefinitions.DesertTrigger.STOP_CHECK) {
                desertStopCheckReached = true
                desertStopCheckTimer = 0f
                desertStopCheckY = ship.position.y
                state.desertNoInputTimer = 0f  // Start tracking from this point
            }

            // TB stops firing after "They're running"
            if (line.trigger == DesertDefinitions.DesertTrigger.PLAYER_FIRES_AT_CIVILIANS) {
                desertTbCeaseFire = true
            }

            // "You have your orders" — start countdown to bombardment
            if (line.trigger == DesertDefinitions.DesertTrigger.BOMBARDMENT_DELAY && state.desertPhase == 2) {
                desertBombardmentDelayTimer = 0f
                desertBombardmentUnlocked = true
            }
        }
    }

    private fun updateDesertPhaseTransitions(deltaTime: Float) {
        when (state.desertPhase) {
            0 -> {
                // Phase 0 -> 1: after all phase1 lines delivered
                if (state.desertDialogueStep >= DesertDefinitions.phase1Lines.size) {
                    state.desertPhase = 1
                    state.desertDialogueStep = 0
                    state.desertDialogueTimer = 0f
                    desertSpawnTimer = 0f
                    desertAmbiguousTargetSpawned = false
                }
            }
            1 -> {
                // Phase 1: After "We should stop" is shown, check for good ending vs horror
                // Good ending: stop (5s not actively driving north) or drive south. Horror: drive 300f north.
                // Timer only ticks when the player is not actively heading north — so stopping at any
                // point (even 200f north) lets them wait it out for the good ending.
                if (desertStopCheckReached) {
                    val drivingNorth = desertPlayerSpeed > 20f && kotlin.math.sin(ship.rotation) < -0.5f
                    if (drivingNorth) desertNorthDriveTimer += deltaTime else desertStopCheckTimer += deltaTime
                    val movedSouth = ship.position.y >= desertStopCheckY + 500f
                    val movedNorth = desertNorthDriveTimer >= 6f
                    if ((desertStopCheckTimer >= 5f || movedSouth) && !movedNorth && !state.desertSecretTriggered) {
                        // Good ending — player stopped or retreated south
                        state.desertSecretTriggered = true
                        state.desertPhase = 4
                        state.desertDialogueStep = 0
                        state.desertDialogueTimer = 0f
                        desertEndingTimer = 0f
                    } else if (movedNorth) {
                        // Horror path — player drove north toward settlement
                        radioSystem.showScriptedMessage(state, "ASTRO", "...Orders are orders.", DesertDefinitions.ASTRO_COLOR)
                        state.desertPhase = 2
                        state.desertDialogueStep = 0
                        state.desertDialogueTimer = 0f
                        desertEndingTimer = 0f
                        // Spawn settlement ahead for bombardment. 1.5 screens north (was 3) so the
                        // silent northward crawl is ~10s and ends with the settlement + energy wall
                        // cresting into view — the player still drives the whole way themselves.
                        desertSettlementWorldY = ship.position.y - screenHeight * 1.5f
                        desertSettlementProgress = 1f
                        spawnDesertSettlementBuildings()
                        spawnDesertCivilians()
                    }
                }
            }
            2 -> {
                // Horror phase: after horror lines complete, transition to crystal
                if (state.desertDialogueStep >= DesertDefinitions.horrorLines.size && !desertHorrorComplete) {
                    desertHorrorComplete = true
                    desertEndingTimer = 0f
                }
                if (desertHorrorComplete) {
                    desertEndingTimer += deltaTime
                    if (desertEndingTimer >= 5f) {
                        state.desertPhase = 3
                        state.desertDialogueStep = 0
                        state.desertDialogueTimer = 0f
                        state.desertCrystalPhase = 1  // Start crystal rising
                        state.desertCrystalTimer = 0f
                    }
                }
            }
            3 -> {
                // Crystal phase — dialogue-driven animation
                state.desertCrystalTimer += deltaTime
                when (state.desertCrystalPhase) {
                    1 -> {
                        // Rising for 3s
                        if (state.desertCrystalTimer >= 3f) {
                            state.desertCrystalPhase = 2
                            state.desertCrystalTimer = 0f
                        }
                    }
                    2 -> {
                        // Hovering — dialogue plays, TB walks to crystal after "Lieutenant, don't—" (step 2)
                        if (state.desertDialogueStep >= 3 && !desertTbWalkingToCrystal) {
                            // "Lieutenant, don't—" just delivered, TB starts walking
                            desertTbWalkingToCrystal = true
                            desertTbCrystalTarget[0] = 0f  // crystal X
                            desertTbCrystalTarget[1] = desertSettlementWorldY + 30f  // near crystal
                        }
                        // After "A price must be paid." (step 3) → drain phase
                        if (state.desertDialogueStep >= 4) {
                            desertCrystalDrainPhase = 1  // draining
                        }
                        // After "It's okay, Lieutenant." (step 5) → powering down
                        if (state.desertDialogueStep >= 6) {
                            desertCrystalDrainPhase = 2  // powering down
                        }
                        // After "...TB?" (step 6) → TB dead
                        if (state.desertDialogueStep >= 7) {
                            if (desertTbAlive) {
                                visualEffects.addDeathBlast(desertTbX, desertTbY)
                                SoundManager.playSFX("sfx_explosion")
                                desertTbAlive = false
                                desertTbDeathTimer = 0f
                            }
                            desertCrystalDrainPhase = 3  // dead
                        }
                        if (!desertTbAlive) {
                            desertTbDeathTimer += deltaTime
                        }
                        // After "You wanted this." (step 7, all lines done) → crystal rams Astro
                        if (state.desertDialogueStep >= DesertDefinitions.crystalLines.size) {
                            state.desertCrystalPhase = 3
                            state.desertCrystalTimer = 0f
                            desertCrystalDrainPhase = 4  // ram
                        }
                    }
                    3 -> {
                        // Crystal approaches/rams player for 2.5s
                        if (state.desertCrystalTimer >= 2.5f) {
                            state.desertCrystalPhase = 4
                            state.desertCrystalTimer = 0f
                            desertCrystalDrainPhase = 5  // flash
                        }
                    }
                    4 -> {
                        // White flash — fade in
                        state.desertFadeAlpha = (state.desertCrystalTimer / 1.5f).coerceIn(0f, 1f)
                        SoundManager.stopAmbient()
                        if (state.desertCrystalTimer >= 2.5f) {
                            val persistence = PersistenceManager(context)
                            persistence.setDesertCompleted()
                            persistence.resetProgressKeepYen()
                            persistence.incrementStoryLoop()   // horror advances the loop (cap 3)
                            resetLoopState()
                            desertCrystalDeath = true
                            state.phase = GamePhase.CRYSTAL_DEATH
                            crystalFreezeDelay = 0f
                            crystalDelayTimer = 0f
                            crystalDelayActive = false
                            crystalRenderer.activateDeath(screenWidth, screenHeight)
                            SoundManager.playSFX("sfx_crystal_activate")
                        }
                    }
                }
            }
            4 -> {
                // Good ending: play lines, then freeze camera and drive off south
                if (state.desertDialogueStep >= DesertDefinitions.goodEndingLines.size && !desertGoodEndingLinesComplete) {
                    desertGoodEndingLinesComplete = true
                    desertEndingTimer = 0f
                    desertCameraFrozen = true
                    desertDriveOffActive = true
                }
                if (desertDriveOffActive) {
                    // Turn tanks south (positive Y) and drive off screen
                    val southAngle = (Math.PI / 2f).toFloat()
                    val turnRate = 2.0f * deltaTime
                    val playerDiff = normalizeAngle(southAngle - ship.rotation)
                    ship.rotation += playerDiff.coerceIn(-turnRate, turnRate)
                    val tbDiff = normalizeAngle(southAngle - desertTbAngle)
                    desertTbAngle += tbDiff.coerceIn(-turnRate, turnRate)

                    // Drive south at normal speed
                    val driveSpeed = 200f
                    desertPlayerSpeed = driveSpeed
                    desertTbSpeed = driveSpeed
                    ship.position.x += cos(ship.rotation) * driveSpeed * deltaTime
                    ship.position.y += sin(ship.rotation) * driveSpeed * deltaTime
                    desertTbX += cos(desertTbAngle) * driveSpeed * deltaTime
                    desertTbY += sin(desertTbAngle) * driveSpeed * deltaTime

                    // Check if both tanks are off screen (below camera view)
                    val screenBottom = camera.y + screenHeight
                    val playerOffScreen = ship.position.y > screenBottom + 50f
                    val tbOffScreen = desertTbY > screenBottom + 50f
                    if (playerOffScreen && tbOffScreen) {
                        desertDriveOffActive = false
                        desertEndingTimer = 0f
                    }
                }
                if (desertGoodEndingLinesComplete && !desertDriveOffActive) {
                    // Bridge memory(past) -> present: fade the desert to black over ~1s,
                    // then start the present-day heart-to-heart and fade it in from black.
                    if (!desertFarewellFadeStarted) {
                        desertFarewellFadeStarted = true
                        desertFarewellFadeTimer = 0f
                    }
                    desertFarewellFadeTimer += deltaTime
                    globalFadeAlpha = (desertFarewellFadeTimer / 1.0f).coerceIn(0f, 1f)
                    if (globalFadeAlpha >= 1f) {
                        val persistence = PersistenceManager(context)
                        // Terminal good ending: enter the astro-loop stage (no loop bump)
                        // and retire the post-horror radio/bar alternates.
                        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
                        persistence.setDesertGoodEnding()
                        persistence.setAstroLoopFirstEntry()
                        // Post-arc homecoming: revive the crew and clear crystal state so the
                        // hangar shows the full living bar (TB welcome, full roster, shield intro)
                        // and does NOT replay the crystal reveal.
                        persistence.clearDeadPilotsAndShips()
                        persistence.setCrystalUnlocked(false)
                        persistence.setCrystalPurchased(false)
                        persistence.setAwaitingCrystalReveal(false)
                        initializeDesertFarewell()
                        state.phase = GamePhase.DESERT_FAREWELL
                        // Fade the present-day heart-to-heart in from black.
                        globalFadeAlpha = 1f
                        globalFadeFading = true
                        globalFadeTimer = 0f
                    }
                }
            }
        }
    }

    // --- Desert Rendering ---

    private fun renderDesert(canvas: Canvas) {
        // Sand background
        canvas.drawColor(0xFFC8A86E.toInt())

        // World-space rendering (apply camera transform)
        canvas.save()
        canvas.translate(-camera.x, -camera.y)

        // Beach and sea at southern boundary
        renderDesertBeachAndSea(canvas)

        // Dirt road, vegetation, rocks, buildings (world space, behind everything)
        renderDesertRoadAndDetails(canvas)

        // Settlement (appears during horror→crystal transition)
        renderDesertSettlement(canvas)

        // Crystal energy wall at settlement boundary
        renderDesertCrystalWall(canvas)

        // Canyon walls (taper near beach)
        renderDesertCanyonWalls(canvas)

        // Vehicle tracks on ground (before vehicles)
        renderDesertTracks(canvas)

        // Dust particles (behind vehicles, on ground)
        renderDesertDust(canvas)

        // Burning wreckage (after tracks/dust, before enemies)
        renderDesertWrecks(canvas)

        // Settlement civilians (no distance cull — let them drive off-screen naturally)
        for (civ in desertCivilians) {
            renderDesertPickup(canvas, civ.x, civ.y, civ.angle, 0xFFAA9966.toInt(), false)
        }

        // Enemies/targets
        renderDesertEnemies(canvas)

        // Projectiles
        renderDesertProjectiles(canvas)

        // TB's tank (drain glow → power down → dark)
        if (desertTbAlive) {
            val tbHullColor = if (desertTbHitWall) 0xFF8899AA.toInt() else 0xFF667744.toInt()
            val tbAccentColor = if (desertTbHitWall) 0xFFAABBCC.toInt() else 0xFF88AACC.toInt()
            renderDesertTank(canvas, desertTbX, desertTbY, desertTbAngle, desertTbTurretAngle, tbHullColor, tbAccentColor)
            // Crystal drain glow overlay
            if (desertCrystalDrainPhase >= 1) {
                val glowAlpha = if (desertCrystalDrainPhase == 1) 60 else if (desertCrystalDrainPhase == 2) 25 else 0
                if (glowAlpha > 0) {
                    desertTankPaint.color = CrystalPalette.MID
                    desertTankPaint.alpha = glowAlpha
                    canvas.drawCircle(desertTbX, desertTbY, 25f, desertTankPaint)
                    desertTankPaint.alpha = 255
                }
            }
        } else if (desertTbDeathTimer < 2f) {
            // Destroyed TB — charred hull, tilting over 2s
            renderDesertTank(canvas, desertTbX, desertTbY, desertTbAngle + desertTbDeathTimer * 0.3f, desertTbTurretAngle, 0xFF3A3330.toInt(), 0xFF554840.toInt())
        } else {
            // Dead wreck — charred, slight tilt
            renderDesertTank(canvas, desertTbX, desertTbY, desertTbAngle + 0.6f, desertTbTurretAngle, 0xFF3A3330.toInt(), 0xFF554840.toInt())
        }

        // Player's tank
        renderDesertTank(canvas, ship.position.x, ship.position.y, ship.rotation, desertPlayerTurretAngle, 0xFF556633.toInt(), 0xFFDD3333.toInt())

        // Explosions render on top of tanks so deaths read clearly
        vectorRenderer.renderVisualEffects(canvas, visualEffects)

        // Crystal (phase 3)
        if (state.desertPhase == 3 && state.desertCrystalPhase > 0) {
            renderDesertCrystal(canvas)
        }

        canvas.restore()

        // Screen-space: Radio chatter only (no upgrade grid, health, yen, timer)
        hudRenderer.renderRadioOnly(canvas, state)

        // Fade overlay
        if (state.desertFadeAlpha > 0f) {
            val alpha = (state.desertFadeAlpha * 255).toInt().coerceIn(0, 255)
            val color = if (state.desertPhase == 4) {
                android.graphics.Color.argb(alpha, 210, 180, 120)  // Warm fade for good ending
            } else {
                android.graphics.Color.argb(alpha, 0, 0, 0)  // Black fade for crystal
            }
            canvas.drawColor(color)
        }

        // Desert pause overlay — plain black semi-transparent (no crystal renderer)
        if (state.isPaused) {
            val overlayPaint = Paint().apply {
                color = android.graphics.Color.argb(180, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlayPaint)
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = FontManager.getDisplayBold()
                isAntiAlias = true
            }
            canvas.drawText("PAUSED", screenWidth / 2f, screenHeight * CrystalRenderer.PAUSE_TEXT_THRESHOLD, textPaint)
        }
    }

    private fun renderDesertTank(canvas: Canvas, x: Float, y: Float, hullAngle: Float, turretAngle: Float, color: Int, accentColor: Int = 0) {
        canvas.save()
        canvas.translate(x, y)

        // Hull - rotated by hullAngle
        canvas.save()
        canvas.rotate(Math.toDegrees(hullAngle.toDouble()).toFloat() + 90f)
        desertTankPaint.color = color
        desertTankPaint.style = Paint.Style.FILL
        canvas.drawRect(-15f, -20f, 15f, 20f, desertTankPaint)

        if (accentColor != 0) {
            desertTankPaint.color = accentColor
            desertTankPaint.alpha = 180  // semi-transparent
            canvas.drawRect(-15f, -5f, 15f, 2f, desertTankPaint)  // horizontal stripe
            desertTankPaint.alpha = 255
        }
        canvas.restore()

        // Turret - rotated by turretAngle (independent of hull)
        canvas.save()
        canvas.rotate(Math.toDegrees(turretAngle.toDouble()).toFloat() + 90f)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        desertTankPaint.color = android.graphics.Color.argb(0xDD, (r * 0.7f).toInt(), (g * 0.7f).toInt(), (b * 0.7f).toInt())
        desertTankPaint.style = Paint.Style.FILL
        canvas.drawRect(-8f, -15f, 8f, 5f, desertTankPaint)

        // Barrel
        desertTankPaint.style = Paint.Style.STROKE
        desertTankPaint.strokeWidth = 4f
        desertTankPaint.color = android.graphics.Color.argb(0xFF, (r * 0.5f).toInt(), (g * 0.5f).toInt(), (b * 0.5f).toInt())
        canvas.drawLine(0f, -15f, 0f, -40f, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL
        canvas.restore()

        canvas.restore()
    }

    private fun renderDesertPickup(canvas: Canvas, x: Float, y: Float, angle: Float, color: Int, hasGun: Boolean) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f)

        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        // Main body (narrower and longer than tank)
        desertTankPaint.color = color
        desertTankPaint.style = Paint.Style.FILL
        canvas.drawRect(-10f, -22f, 10f, 18f, desertTankPaint)  // body

        // Cab at front (slightly wider, shorter)
        desertTankPaint.color = android.graphics.Color.argb(0xFF, (r * 0.85f).toInt(), (g * 0.85f).toInt(), (b * 0.85f).toInt())
        canvas.drawRect(-11f, -22f, 11f, -10f, desertTankPaint)  // cab

        // Windshield (dark)
        desertTankPaint.color = 0xFF334455.toInt()
        canvas.drawRect(-8f, -20f, 8f, -14f, desertTankPaint)

        // Open bed at rear
        desertTankPaint.color = android.graphics.Color.argb(0xFF, (r * 0.7f).toInt(), (g * 0.7f).toInt(), (b * 0.7f).toInt())
        canvas.drawRect(-10f, -2f, 10f, 18f, desertTankPaint)  // bed floor (darker)

        // Bed rails (outline)
        desertTankPaint.style = Paint.Style.STROKE
        desertTankPaint.strokeWidth = 2f
        desertTankPaint.color = android.graphics.Color.argb(0xFF, (r * 0.6f).toInt(), (g * 0.6f).toInt(), (b * 0.6f).toInt())
        canvas.drawRect(-10f, -2f, 10f, 18f, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL

        // Wheels (4 corners)
        desertTankPaint.color = 0xFF222222.toInt()
        canvas.drawRect(-13f, -18f, -10f, -12f, desertTankPaint)  // front-left
        canvas.drawRect(10f, -18f, 13f, -12f, desertTankPaint)   // front-right
        canvas.drawRect(-13f, 10f, -10f, 16f, desertTankPaint)    // rear-left
        canvas.drawRect(10f, 10f, 13f, 16f, desertTankPaint)     // rear-right

        if (hasGun) {
            // M-60 mounted gun on bed
            desertTankPaint.color = 0xFF444444.toInt()
            // Gun mount (small circle)
            canvas.drawCircle(0f, 8f, 4f, desertTankPaint)
            // Gun barrel (pointing forward/up from the bed)
            desertTankPaint.strokeWidth = 3f
            desertTankPaint.style = Paint.Style.STROKE
            desertTankPaint.color = 0xFF333333.toInt()
            canvas.drawLine(0f, 8f, 0f, -8f, desertTankPaint)
            desertTankPaint.style = Paint.Style.FILL
        } else {
            // Roof rack on cab (civilian variant)
            desertTankPaint.color = 0xFF555555.toInt()
            desertTankPaint.strokeWidth = 1.5f
            desertTankPaint.style = Paint.Style.STROKE
            canvas.drawRect(-9f, -19f, 9f, -13f, desertTankPaint)
            desertTankPaint.style = Paint.Style.FILL
        }

        canvas.restore()
    }

    private fun renderDesertTerrain(canvas: Canvas) {
        // --- 1. Distant Mountains (screen space, 0.1x parallax) ---
        val mountainParallax = 0.1f
        val mox = camera.x * mountainParallax
        val moy = camera.y * mountainParallax
        val mountainBaseY = screenHeight * 0.12f - (moy % screenHeight)

        desertTankPaint.style = Paint.Style.FILL
        desertTankPaint.color = 0xFF7A8A9A.toInt()  // blue-gray silhouette

        desertMountainPath.reset()
        desertMountainPath.moveTo(-mox - 100f, mountainBaseY)
        desertMountainPath.lineTo(-mox + screenWidth * 0.15f, mountainBaseY - screenHeight * 0.08f)
        desertMountainPath.lineTo(-mox + screenWidth * 0.3f, mountainBaseY)
        desertMountainPath.close()
        canvas.drawPath(desertMountainPath, desertTankPaint)

        desertTankPaint.color = 0xFF6B7B8B.toInt()  // slightly darker
        desertMountainPath.reset()
        desertMountainPath.moveTo(-mox + screenWidth * 0.2f, mountainBaseY)
        desertMountainPath.lineTo(-mox + screenWidth * 0.45f, mountainBaseY - screenHeight * 0.12f)
        desertMountainPath.lineTo(-mox + screenWidth * 0.7f, mountainBaseY)
        desertMountainPath.close()
        canvas.drawPath(desertMountainPath, desertTankPaint)

        desertTankPaint.color = 0xFF8A96A6.toInt()  // lighter distant peak
        desertMountainPath.reset()
        desertMountainPath.moveTo(-mox + screenWidth * 0.55f, mountainBaseY)
        desertMountainPath.lineTo(-mox + screenWidth * 0.75f, mountainBaseY - screenHeight * 0.06f)
        desertMountainPath.lineTo(-mox + screenWidth * 0.95f, mountainBaseY)
        desertMountainPath.close()
        canvas.drawPath(desertMountainPath, desertTankPaint)

        desertTankPaint.color = 0xFF708090.toInt()
        desertMountainPath.reset()
        desertMountainPath.moveTo(-mox + screenWidth * 0.8f, mountainBaseY)
        desertMountainPath.lineTo(-mox + screenWidth * 1.05f, mountainBaseY - screenHeight * 0.1f)
        desertMountainPath.lineTo(-mox + screenWidth * 1.25f, mountainBaseY)
        desertMountainPath.close()
        canvas.drawPath(desertMountainPath, desertTankPaint)

        // --- 2. Dune shapes (screen space, subtle depth) ---
        desertTankPaint.color = 0xFFD4B87A.toInt()
        val ox = (ship.position.x * 0.05f) % 300f
        val oy = (ship.position.y * 0.05f) % 200f
        for (i in -1..6) {
            for (j in -1..4) {
                canvas.drawOval(
                    i * 300f - ox - 80f, j * 200f - oy - 20f,
                    i * 300f - ox + 80f, j * 200f - oy + 20f,
                    desertTankPaint
                )
            }
        }
    }

    private fun renderDesertRoadAndDetails(canvas: Canvas) {
        val visibleTop = camera.y - 50f
        val visibleBottom = camera.y + screenHeight + 50f

        // --- 3. Curving Dirt Road ---
        val roadTileSize = 50f
        val roadStartTile = ((visibleTop) / roadTileSize).toInt() - 1
        val roadEndTile = ((visibleBottom) / roadTileSize).toInt() + 1

        // Generate road center X at each tile using deterministic offsets
        val settlementNorthY = if (desertBuildings.isNotEmpty()) desertSettlementWorldY - 380f else Float.MAX_VALUE
        val settlementSouthY = if (desertBuildings.isNotEmpty()) desertSettlementWorldY + 220f else Float.MIN_VALUE
        val alignRange = 300f  // Gradually straighten road within this distance of settlement
        fun roadCenterX(tile: Int): Float {
            val t = tile xor (tile shl 13)
            val hash = (t * (t * t * 15731 + 789221) + 1376312589)
            val baseX = (hash and 0x7FFFFFFF).rem(200).minus(100).toFloat() * 0.6f
            // Align with settlement main road (x=0) near settlement edges
            if (desertBuildings.isNotEmpty()) {
                val tileY = tile * roadTileSize
                val distSouth = tileY - settlementSouthY  // positive = south of settlement
                val distNorth = settlementNorthY - tileY   // positive = north of settlement
                val minDist = kotlin.math.min(
                    if (distSouth > 0f) distSouth else Float.MAX_VALUE,
                    if (distNorth > 0f) distNorth else Float.MAX_VALUE
                )
                if (minDist < alignRange) {
                    val blend = 1f - minDist / alignRange
                    return baseX * (1f - blend)
                }
            }
            return baseX
        }

        // Road boundaries and fade zones
        val roadSouthEnd = desertSpawnY + screenHeight * 0.5f  // Southern terminus (beach start)
        val gapSouthEdge = if (desertBuildings.isNotEmpty()) settlementSouthY + 80f else Float.MAX_VALUE
        val gapNorthEdge = if (desertBuildings.isNotEmpty()) settlementNorthY - 80f else Float.MIN_VALUE
        val roadFadeDist = 120f

        // Draw road segments with lerped curves
        for (tile in roadStartTile..roadEndTile) {
            val y0 = tile * roadTileSize
            val y1 = (tile + 1) * roadTileSize

            // Skip tiles past southern road end
            if (y0 > roadSouthEnd) continue
            // Skip tiles inside or straddling settlement gap
            if (desertBuildings.isNotEmpty() && y1 > gapNorthEdge && y0 < gapSouthEdge) continue

            // Calculate fade alpha
            var tileAlpha = 255
            // Fade at southern road end (handles tiles straddling cutoff)
            val southDist = roadSouthEnd - y1
            if (southDist < roadFadeDist) {
                tileAlpha = minOf(tileAlpha, maxOf(0, (southDist / roadFadeDist * 255f).toInt()))
            }
            // Fade approaching settlement gap from south (tiles just south of gap)
            if (desertBuildings.isNotEmpty() && y0 >= gapSouthEdge && y0 < gapSouthEdge + roadFadeDist) {
                val d = y0 - gapSouthEdge  // 0 at gap edge → transparent, roadFadeDist → opaque
                tileAlpha = minOf(tileAlpha, (d / roadFadeDist * 255f).toInt())
            }
            // Fade approaching settlement gap from north (tiles just north of gap)
            if (desertBuildings.isNotEmpty() && y1 <= gapNorthEdge && y1 > gapNorthEdge - roadFadeDist) {
                val d = gapNorthEdge - y1  // 0 at gap edge → transparent, roadFadeDist → opaque
                tileAlpha = minOf(tileAlpha, (d / roadFadeDist * 255f).toInt())
            }
            if (tileAlpha <= 0) continue

            val x0 = roadCenterX(tile)
            val x1 = roadCenterX(tile + 1)

            // Road surface fill (wider, lighter sand)
            desertTankPaint.style = Paint.Style.FILL
            val steps = 5
            val hw = 20f // half width
            for (s in 0 until steps) {
                val t0 = s.toFloat() / steps
                val t1 = (s + 1).toFloat() / steps
                val sx0 = x0 + (x1 - x0) * t0
                val sy0 = y0 + (y1 - y0) * t0
                val sx1 = x0 + (x1 - x0) * t1
                val sy1 = y0 + (y1 - y0) * t1
                // Irregular edge: small deterministic offset
                val edgeOff0 = ((tile * 13 + s * 7) % 5 - 2).toFloat()
                val edgeOff1 = ((tile * 13 + (s + 1) * 7) % 5 - 2).toFloat()

                desertTankPaint.color = 0xFFB89858.toInt()
                desertTankPaint.alpha = tileAlpha

                val path = Path()
                path.moveTo(sx0 - hw - edgeOff0, sy0)
                path.lineTo(sx1 - hw - edgeOff1, sy1)
                path.lineTo(sx1 + hw + edgeOff1, sy1)
                path.lineTo(sx0 + hw + edgeOff0, sy0)
                path.close()
                canvas.drawPath(path, desertTankPaint)
            }

            // Wheel rut lines (darker brown, 2 parallel lines 20px apart)
            desertTankPaint.color = 0xFF8A6E3E.toInt()
            desertTankPaint.alpha = tileAlpha
            desertTankPaint.strokeWidth = 2f
            desertTankPaint.style = Paint.Style.STROKE
            canvas.drawLine(x0 - 10f, y0, x1 - 10f, y1, desertTankPaint)
            canvas.drawLine(x0 + 10f, y0, x1 + 10f, y1, desertTankPaint)
            desertTankPaint.style = Paint.Style.FILL
            desertTankPaint.alpha = 255

            // Scattered pebbles/dirt on road (deterministic)
            if (tileAlpha > 128) {
                val pebbleSeed = tile * 41 + 19
                if (pebbleSeed % 3 == 0) {
                    val px = x0 + ((pebbleSeed * 23) % 30 - 15).toFloat()
                    val py = y0 + ((pebbleSeed * 11) % ((roadTileSize - 4f).toInt().coerceAtLeast(1))).toFloat()
                    desertTankPaint.color = 0xFF9A7A4A.toInt()
                    canvas.drawCircle(px, py, 1.5f, desertTankPaint)
                }
                if (pebbleSeed % 5 == 0) {
                    val px2 = x0 + ((pebbleSeed * 37) % 26 - 13).toFloat()
                    val py2 = y0 + ((pebbleSeed * 17) % ((roadTileSize - 4f).toInt().coerceAtLeast(1))).toFloat()
                    desertTankPaint.color = 0xFF7A6A3A.toInt()
                    canvas.drawCircle(px2, py2, 2f, desertTankPaint)
                }
            }
        }

        // --- Tile-based deterministic terrain objects ---
        val tileSize = 100f
        val startTile = ((camera.y) / tileSize).toInt() - 1
        val endTile = ((camera.y + screenHeight) / tileSize).toInt() + 1

        val terrainSouthLimit = desertSpawnY + screenHeight * 0.3f  // No terrain objects past beach zone
        for (tileY in startTile..endTile) {
            val seed = tileY * 17 + 42  // deterministic seed

            @Suppress("NAME_SHADOWING")
            val worldY = tileY * tileSize + (seed % 100)

            // Skip terrain objects in beach zone
            if (worldY > terrainSouthLimit) continue

            // Alternate sides: even tiles → left, odd tiles → right
            val side = if (tileY % 2 == 0) -1f else 1f

            // --- 4. Sparse Vegetation: Scrub Bush ---
            if (seed % 5 == 0) {
                val offset = 80f + ((seed * 37) % 250).toFloat()
                val bushX = side * offset
                if (bushX < DESERT_CORRIDOR_RIGHT - 30 && bushX > DESERT_CORRIDOR_LEFT + 30) {
                    desertTankPaint.color = 0xFF6B7B3A.toInt()  // olive green
                    canvas.drawCircle(bushX, worldY.toFloat(), 8f, desertTankPaint)
                    canvas.drawCircle(bushX - 5f, worldY.toFloat() + 3f, 6f, desertTankPaint)
                }
            }

            // --- Dead Tree (stick figure) ---
            if (seed % 7 == 0) {
                val offset = 100f + ((seed * 53) % 200).toFloat()
                val treeX = -side * offset  // opposite side from bush
                if (treeX < DESERT_CORRIDOR_RIGHT - 40 && treeX > DESERT_CORRIDOR_LEFT + 40) {
                    desertTankPaint.color = 0xFF5A4A3A.toInt()  // dark brown
                    desertTankPaint.strokeWidth = 3f
                    desertTankPaint.style = Paint.Style.STROKE
                    canvas.drawLine(treeX, worldY.toFloat(), treeX, worldY.toFloat() - 25f, desertTankPaint)
                    canvas.drawLine(treeX, worldY.toFloat() - 15f, treeX + 12f, worldY.toFloat() - 22f, desertTankPaint)
                    canvas.drawLine(treeX, worldY.toFloat() - 18f, treeX - 10f, worldY.toFloat() - 25f, desertTankPaint)
                    desertTankPaint.style = Paint.Style.FILL
                }
            }

            // --- Rock / Boulder ---
            if (seed % 3 == 0) {
                val offset = 50f + ((seed * 71) % 300).toFloat()
                val rockX = side * offset
                if (rockX < DESERT_CORRIDOR_RIGHT - 20 && rockX > DESERT_CORRIDOR_LEFT + 20) {
                    desertTankPaint.color = 0xFF8B7B6B.toInt()  // gray-brown
                    val size = 5f + (seed % 8)
                    canvas.drawOval(rockX - size, worldY.toFloat() - size * 0.6f, rockX + size, worldY.toFloat() + size * 0.6f, desertTankPaint)
                }
            }

            // --- Thorn bush cluster ---
            if ((seed + 3) % 4 == 0) {
                val offset = 70f + ((seed * 89) % 260).toFloat()
                val thornX = -side * offset  // opposite side from rocks
                if (thornX < DESERT_CORRIDOR_RIGHT - 25 && thornX > DESERT_CORRIDOR_LEFT + 25) {
                    desertTankPaint.color = 0xFF5A6B2A.toInt()  // dark olive
                    canvas.drawCircle(thornX, worldY.toFloat(), 5f, desertTankPaint)
                    canvas.drawCircle(thornX + 7f, worldY.toFloat() - 2f, 4f, desertTankPaint)
                    canvas.drawCircle(thornX - 4f, worldY.toFloat() + 3f, 4f, desertTankPaint)
                }
            }

            // --- Dry grass tuft ---
            if ((seed + 1) % 3 == 0) {
                val offset = 60f + ((seed * 67) % 230).toFloat()
                val grassX = side * offset
                if (grassX < DESERT_CORRIDOR_RIGHT - 20 && grassX > DESERT_CORRIDOR_LEFT + 20) {
                    desertTankPaint.color = 0xFF8B9B4A.toInt()  // yellow-green
                    desertTankPaint.strokeWidth = 1.5f
                    desertTankPaint.style = Paint.Style.STROKE
                    canvas.drawLine(grassX, worldY.toFloat(), grassX - 3f, worldY.toFloat() - 10f, desertTankPaint)
                    canvas.drawLine(grassX, worldY.toFloat(), grassX + 2f, worldY.toFloat() - 9f, desertTankPaint)
                    canvas.drawLine(grassX, worldY.toFloat(), grassX + 5f, worldY.toFloat() - 8f, desertTankPaint)
                    desertTankPaint.style = Paint.Style.FILL
                }
            }

            // --- 5. Small Cactus ---
            if (tileY % 3 == 0 && seed % 4 == 0) {
                val offset = 120f + ((seed * 31) % 200).toFloat()
                val cactusX = -side * offset
                if (abs(cactusX) > 60f && cactusX < DESERT_CORRIDOR_RIGHT - 40 && cactusX > DESERT_CORRIDOR_LEFT + 40) {
                    desertTankPaint.color = 0xFF4A6B2A.toInt()  // cactus green
                    desertTankPaint.strokeWidth = 4f
                    desertTankPaint.style = Paint.Style.STROKE
                    val wy = worldY.toFloat()
                    val h = 18f + (seed % 8)
                    // Main trunk
                    canvas.drawLine(cactusX, wy, cactusX, wy - h, desertTankPaint)
                    // Left arm
                    canvas.drawLine(cactusX - 6f, wy - h * 0.4f, cactusX - 6f, wy - h * 0.7f, desertTankPaint)
                    canvas.drawLine(cactusX, wy - h * 0.4f, cactusX - 6f, wy - h * 0.4f, desertTankPaint)
                    // Right arm (slightly higher)
                    canvas.drawLine(cactusX + 6f, wy - h * 0.55f, cactusX + 6f, wy - h * 0.8f, desertTankPaint)
                    canvas.drawLine(cactusX, wy - h * 0.55f, cactusX + 6f, wy - h * 0.55f, desertTankPaint)
                    desertTankPaint.style = Paint.Style.FILL
                }
            }
        }
    }

    private fun renderDesertDust(canvas: Canvas) {
        for (dust in desertDustParticles) {
            if (dust.alpha <= 0f) continue
            val a = dust.alpha.toInt().coerceIn(0, 255)
            desertTankPaint.color = android.graphics.Color.argb(a, 200, 180, 140)  // tan dust
            val size = 3f + dust.age * 4f  // grows as it ages
            canvas.drawCircle(dust.x, dust.y, size, desertTankPaint)
        }
    }

    private fun renderDesertHeatShimmer(canvas: Canvas) {
        val shimmerY = camera.y - screenHeight * 0.3f
        desertTankPaint.color = 0x20FFFFFF.toInt()
        desertTankPaint.strokeWidth = 2f
        desertTankPaint.style = Paint.Style.STROKE
        val time = (System.currentTimeMillis() % 10000L) / 1000f
        for (i in 0..2) {
            val y = shimmerY + i * 15f
            val path = desertMountainPath  // reuse path object
            path.reset()
            path.moveTo(DESERT_CORRIDOR_LEFT, y)
            var x = DESERT_CORRIDOR_LEFT.toInt()
            while (x <= DESERT_CORRIDOR_RIGHT.toInt()) {
                path.lineTo(x.toFloat(), y + sin(x * 0.03 + time * 2.0 + i).toFloat() * 3f)
                x += 20
            }
            canvas.drawPath(path, desertTankPaint)
        }
        desertTankPaint.style = Paint.Style.FILL
    }

    private fun renderDesertBeachAndSea(canvas: Canvas) {
        val beachStartY = desertSpawnY + screenHeight * 0.5f  // Beach starts half-screen south of spawn
        val beachEndY = desertSpawnY + screenHeight           // Water starts at boundary
        val seaEndY = beachEndY + screenHeight                // Deep ocean extends beyond
        val time = (System.currentTimeMillis() % 10000L) / 1000f

        // Only render if camera can see this area
        if (camera.y + screenHeight < beachStartY - 100f) return

        val left = DESERT_CORRIDOR_LEFT - 500f
        val right = DESERT_CORRIDOR_RIGHT + 500f

        desertTankPaint.style = Paint.Style.FILL

        // Smooth sand gradient from canyon sand to light beach sand
        val dryEndY = beachStartY + (beachEndY - beachStartY) * 0.6f
        val sandSteps = 20
        for (i in 0 until sandSteps) {
            val frac = i.toFloat() / sandSteps
            val sy0 = beachStartY + (dryEndY - beachStartY) * frac
            val sy1 = beachStartY + (dryEndY - beachStartY) * (frac + 1f / sandSteps)
            val r = (0xC8 + (frac * 32f).toInt()).coerceAtMost(0xE8)
            val g = (0xA8 + (frac * 40f).toInt()).coerceAtMost(0xD8)
            val b = (0x6E + (frac * 34f).toInt()).coerceAtMost(0x90)
            desertTankPaint.color = android.graphics.Color.argb(0xFF, r, g, b)
            canvas.drawRect(left, sy0, right, sy1, desertTankPaint)
        }

        // Smooth transition from dry sand to wet sand
        val wetSandTop = dryEndY
        val wetSandBottom = beachStartY + (beachEndY - beachStartY) * 0.8f
        val wetSteps = 10
        for (i in 0 until wetSteps) {
            val frac = i.toFloat() / wetSteps
            val wy0 = wetSandTop + (wetSandBottom - wetSandTop) * frac
            val wy1 = wetSandTop + (wetSandBottom - wetSandTop) * (frac + 1f / wetSteps)
            val r = (0xE8 + ((0xB0 - 0xE8) * frac).toInt()).coerceIn(0xB0, 0xE8)
            val g = (0xD8 + ((0xA0 - 0xD8) * frac).toInt()).coerceIn(0xA0, 0xD8)
            val b2 = (0x90 + ((0x70 - 0x90) * frac).toInt()).coerceIn(0x70, 0x90)
            desertTankPaint.color = android.graphics.Color.argb(0xFF, r, g, b2)
            canvas.drawRect(left, wy0, right, wy1, desertTankPaint)
        }

        // Foam line (irregular white line at water edge)
        val foamY = beachStartY + (beachEndY - beachStartY) * 0.8f
        desertTankPaint.color = 0xFFFFFFFF.toInt()
        desertTankPaint.alpha = 120
        val foamPath = Path()
        foamPath.moveTo(left, foamY)
        var fx = left.toInt()
        while (fx <= right.toInt()) {
            val foamWobble = sin(fx * 0.02 + time * 1.5).toFloat() * 5f + sin(fx * 0.05 + time * 2.3).toFloat() * 3f
            foamPath.lineTo(fx.toFloat(), foamY + foamWobble)
            fx += 15
        }
        foamPath.lineTo(right, foamY + 20f)
        foamPath.lineTo(left, foamY + 20f)
        foamPath.close()
        desertTankPaint.style = Paint.Style.FILL
        canvas.drawPath(foamPath, desertTankPaint)
        desertTankPaint.alpha = 255

        // Shallow water (blue-green, lighter)
        val shallowTop = foamY + 5f
        val shallowBottom = beachEndY + 50f
        val transitionHeight = 60f
        val transitionStart = shallowBottom - transitionHeight
        // Solid shallow water above transition
        desertTankPaint.color = 0xFF4A8A7A.toInt()
        canvas.drawRect(left, shallowTop, right, transitionStart, desertTankPaint)
        // Gradient transition from shallow to deep water
        val gradSteps = 10
        for (gi in 0 until gradSteps) {
            val gfrac = gi.toFloat() / gradSteps
            val gy0 = transitionStart + transitionHeight * gfrac
            val gy1 = transitionStart + transitionHeight * (gfrac + 1f / gradSteps)
            val gr = (0x4A + ((0x2A - 0x4A) * gfrac).toInt()).coerceIn(0x2A, 0x4A)
            val gg = (0x8A + ((0x5A - 0x8A) * gfrac).toInt()).coerceIn(0x5A, 0x8A)
            val gb = (0x7A + ((0x6A - 0x7A) * gfrac).toInt()).coerceIn(0x6A, 0x7A)
            desertTankPaint.color = android.graphics.Color.argb(0xFF, gr, gg, gb)
            canvas.drawRect(left, gy0, right, gy1, desertTankPaint)
        }
        // Deep water below transition
        desertTankPaint.color = 0xFF2A5A6A.toInt()
        canvas.drawRect(left, shallowBottom, right, seaEndY, desertTankPaint)

        // Gentle wave lines in shallow water
        desertTankPaint.style = Paint.Style.STROKE
        desertTankPaint.strokeWidth = 1.5f
        desertTankPaint.color = 0xFF5A9A8A.toInt()
        desertTankPaint.alpha = 80
        for (w in 0..3) {
            val waveY = shallowTop + 15f + w * 20f
            val wavePath = Path()
            wavePath.moveTo(left, waveY)
            var wx = left.toInt()
            while (wx <= right.toInt()) {
                val waveOff = sin(wx * 0.015 + time * 1.2 + w * 1.5).toFloat() * 4f
                wavePath.lineTo(wx.toFloat(), waveY + waveOff)
                wx += 20
            }
            canvas.drawPath(wavePath, desertTankPaint)
        }
        desertTankPaint.alpha = 255
        desertTankPaint.style = Paint.Style.FILL

        // --- Landing craft on beach ---
        val craftX = 30f  // slightly off-center
        val craftY = wetSandTop + 10f  // on the wet sand
        canvas.save()
        canvas.translate(craftX, craftY)

        // Hull — angular military transport (dark olive-gray)
        // North = negative Y, stern toward sea (positive Y), ramp faces north (negative Y)
        desertTankPaint.color = 0xFF4A5040.toInt()
        val hullPath = Path()
        hullPath.moveTo(-35f, 60f)    // stern left (south, toward sea)
        hullPath.lineTo(-30f, -40f)   // bow left (north, ramp side)
        hullPath.lineTo(30f, -40f)    // bow right
        hullPath.lineTo(35f, 60f)     // stern right
        hullPath.close()
        canvas.drawPath(hullPath, desertTankPaint)

        // Darker deck detail
        desertTankPaint.color = 0xFF3A4030.toInt()
        canvas.drawRect(-28f, 10f, 28f, 50f, desertTankPaint)

        // Ramp (dropped open, facing north toward canyon)
        desertTankPaint.color = 0xFF5A6050.toInt()
        val rampPath = Path()
        rampPath.moveTo(-28f, -40f)
        rampPath.lineTo(-22f, -70f)
        rampPath.lineTo(22f, -70f)
        rampPath.lineTo(28f, -40f)
        rampPath.close()
        canvas.drawPath(rampPath, desertTankPaint)

        // Ramp tread marks
        desertTankPaint.color = 0xFF4A5040.toInt()
        desertTankPaint.strokeWidth = 1.5f
        desertTankPaint.style = Paint.Style.STROKE
        canvas.drawLine(-10f, -42f, -8f, -65f, desertTankPaint)
        canvas.drawLine(10f, -42f, 8f, -65f, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL

        // Bridge/cabin at stern (south end)
        desertTankPaint.color = 0xFF3A4030.toInt()
        canvas.drawRect(-20f, 45f, 20f, 58f, desertTankPaint)

        // Antenna
        desertTankPaint.color = 0xFF6A7060.toInt()
        desertTankPaint.strokeWidth = 1f
        desertTankPaint.style = Paint.Style.STROKE
        canvas.drawLine(12f, 58f, 14f, 72f, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL

        canvas.restore()
    }

    private fun renderDesertCanyonWalls(canvas: Canvas) {
        desertTankPaint.style = Paint.Style.FILL

        // World-space canyon walls — drawn within camera transform
        // Canyon walls extend all the way to the sea (foam line)
        val beachEndY = desertSpawnY + screenHeight
        val foamY = desertSpawnY + screenHeight * 0.5f + (beachEndY - (desertSpawnY + screenHeight * 0.5f)) * 0.8f
        val visibleTop = camera.y - 40f
        val visibleBottom = minOf(camera.y + screenHeight + 40f, foamY)
        if (visibleBottom <= visibleTop) return
        val wallDepth = 500f
        val tileSize = 40f
        val startTile = (visibleTop / tileSize).toInt() - 1
        val endTile = (visibleBottom / tileSize).toInt() + 1

        // --- Left canyon wall ---
        desertTankPaint.color = 0xFF8B6914.toInt()
        canvas.drawRect(DESERT_CORRIDOR_LEFT - wallDepth, visibleTop, DESERT_CORRIDOR_LEFT, visibleBottom, desertTankPaint)

        // Horizontal strata layers on left wall face
        for (i in startTile..endTile) {
            val y = i * tileSize
            val strataHash = (i * 31 + 5) and 0x7FFFFFFF
            // Alternating tones for sedimentary look
            val toneShift = (strataHash % 3) - 1  // -1, 0, or 1
            val r = (0x8B + toneShift * 8).coerceIn(0x70, 0x9F)
            val g = (0x69 + toneShift * 6).coerceIn(0x55, 0x7F)
            val b = (0x14 + toneShift * 4).coerceIn(0x08, 0x28)
            desertTankPaint.color = android.graphics.Color.argb(0xFF, r, g, b)
            canvas.drawRect(DESERT_CORRIDOR_LEFT - wallDepth, y, DESERT_CORRIDOR_LEFT, y + tileSize, desertTankPaint)

            // Shadow crack (dark line between some strata)
            if (strataHash % 4 == 0) {
                desertTankPaint.color = 0xFF4A3808.toInt()
                canvas.drawRect(DESERT_CORRIDOR_LEFT - wallDepth, y - 1f, DESERT_CORRIDOR_LEFT, y + 1f, desertTankPaint)
            }

            // Rock ledge (small protruding rectangle)
            if (strataHash % 5 == 0) {
                val ledgeW = 20f + (strataHash % 15).toFloat()
                val ledgeH = 6f + (strataHash % 4).toFloat()
                val ledgeY = y + (strataHash % ((tileSize - ledgeH).toInt().coerceAtLeast(1)))
                // Darker top shadow
                desertTankPaint.color = 0xFF5A4910.toInt()
                canvas.drawRect(DESERT_CORRIDOR_LEFT, ledgeY, DESERT_CORRIDOR_LEFT + ledgeW, ledgeY + 2f, desertTankPaint)
                // Lighter face
                desertTankPaint.color = 0xFFA58520.toInt()
                canvas.drawRect(DESERT_CORRIDOR_LEFT, ledgeY + 2f, DESERT_CORRIDOR_LEFT + ledgeW, ledgeY + ledgeH, desertTankPaint)
            }
        }

        // Jagged edge on left wall
        desertTankPaint.color = 0xFF9B7924.toInt()
        for (i in startTile..endTile) {
            val y = i * tileSize
            val jag = 15f + ((i * 7 + 13) % 20).toFloat()
            canvas.drawRect(DESERT_CORRIDOR_LEFT, y, DESERT_CORRIDOR_LEFT + jag, y + tileSize, desertTankPaint)
        }

        // --- Right canyon wall ---
        desertTankPaint.color = 0xFF8B6914.toInt()
        canvas.drawRect(DESERT_CORRIDOR_RIGHT, visibleTop, DESERT_CORRIDOR_RIGHT + wallDepth, visibleBottom, desertTankPaint)

        // Horizontal strata layers on right wall face
        for (i in startTile..endTile) {
            val y = i * tileSize
            val strataHash = (i * 47 + 11) and 0x7FFFFFFF
            val toneShift = (strataHash % 3) - 1
            val r = (0x8B + toneShift * 8).coerceIn(0x70, 0x9F)
            val g = (0x69 + toneShift * 6).coerceIn(0x55, 0x7F)
            val b = (0x14 + toneShift * 4).coerceIn(0x08, 0x28)
            desertTankPaint.color = android.graphics.Color.argb(0xFF, r, g, b)
            canvas.drawRect(DESERT_CORRIDOR_RIGHT, y, DESERT_CORRIDOR_RIGHT + wallDepth, y + tileSize, desertTankPaint)

            // Shadow crack
            if (strataHash % 4 == 0) {
                desertTankPaint.color = 0xFF4A3808.toInt()
                canvas.drawRect(DESERT_CORRIDOR_RIGHT, y - 1f, DESERT_CORRIDOR_RIGHT + wallDepth, y + 1f, desertTankPaint)
            }

            // Rock ledge (protruding inward from right wall)
            if (strataHash % 5 == 0) {
                val ledgeW = 20f + (strataHash % 15).toFloat()
                val ledgeH = 6f + (strataHash % 4).toFloat()
                val ledgeY = y + (strataHash % ((tileSize - ledgeH).toInt().coerceAtLeast(1)))
                desertTankPaint.color = 0xFF5A4910.toInt()
                canvas.drawRect(DESERT_CORRIDOR_RIGHT - ledgeW, ledgeY, DESERT_CORRIDOR_RIGHT, ledgeY + 2f, desertTankPaint)
                desertTankPaint.color = 0xFFA58520.toInt()
                canvas.drawRect(DESERT_CORRIDOR_RIGHT - ledgeW, ledgeY + 2f, DESERT_CORRIDOR_RIGHT, ledgeY + ledgeH, desertTankPaint)
            }
        }

        // Jagged edge on right wall
        desertTankPaint.color = 0xFF9B7924.toInt()
        for (i in startTile..endTile) {
            val y = i * tileSize
            val jag = 15f + ((i * 11 + 7) % 20).toFloat()
            canvas.drawRect(DESERT_CORRIDOR_RIGHT - jag, y, DESERT_CORRIDOR_RIGHT, y + tileSize, desertTankPaint)
        }
    }

    private fun renderDesertTracks(canvas: Canvas) {
        for (track in desertTracks) {
            val alpha = ((1f - track.age / TRACK_LIFETIME) * 100).toInt().coerceIn(0, 100)
            if (alpha <= 0) continue

            // Dark brown track marks
            desertTankPaint.color = android.graphics.Color.argb(alpha, 90, 75, 50)

            canvas.save()
            canvas.translate(track.x, track.y)
            canvas.rotate(Math.toDegrees(track.angle.toDouble()).toFloat() + 90f)

            val halfWidth = track.width / 2f
            // Two parallel tread lines
            desertTankPaint.style = Paint.Style.FILL
            canvas.drawRect(-halfWidth, -3f, -halfWidth + 4f, 3f, desertTankPaint)
            canvas.drawRect(halfWidth - 4f, -3f, halfWidth, 3f, desertTankPaint)

            canvas.restore()
        }
    }

    private fun renderDesertEnemies(canvas: Canvas) {
        for (enemy in activeEnemies) {
            if (!enemy.isActive) continue
            val hasGun = state.desertPhase == 0  // military pickups have guns
            val color = if (state.desertPhase == 0) 0xFF886644.toInt() else 0xFFAA9966.toInt()
            renderDesertPickup(canvas, enemy.position.x, enemy.position.y, enemy.rotation, color, hasGun)
        }
    }

    private fun renderDesertProjectiles(canvas: Canvas) {
        for (proj in activeProjectiles) {
            if (!proj.isActive) continue
            if (proj.isEnemyProjectile) {
                // Line projectile — gunmetal grey, elongated along velocity
                desertTankPaint.color = 0xFF667788.toInt()
                desertTankPaint.strokeWidth = 2.5f
                desertTankPaint.style = Paint.Style.STROKE
                val vLen = sqrt(proj.velocity.x * proj.velocity.x + proj.velocity.y * proj.velocity.y)
                if (vLen > 0f) {
                    val nx = proj.velocity.x / vLen * 12f
                    val ny = proj.velocity.y / vLen * 12f
                    canvas.drawLine(proj.position.x - nx, proj.position.y - ny,
                        proj.position.x + nx, proj.position.y + ny, desertTankPaint)
                }
                desertTankPaint.style = Paint.Style.FILL
            } else {
                desertTankPaint.color = proj.color
                if (proj.type == ProjectileType.MISSILE) {
                    // Homing missile — small triangle pointing in velocity direction
                    val vLen = sqrt(proj.velocity.x * proj.velocity.x + proj.velocity.y * proj.velocity.y)
                    if (vLen > 0f) {
                        canvas.save()
                        canvas.translate(proj.position.x, proj.position.y)
                        canvas.rotate(Math.toDegrees(atan2(proj.velocity.y, proj.velocity.x).toDouble()).toFloat())
                        val path = Path()
                        path.moveTo(6f, 0f)
                        path.lineTo(-4f, -3f)
                        path.lineTo(-4f, 3f)
                        path.close()
                        desertTankPaint.style = Paint.Style.FILL
                        canvas.drawPath(path, desertTankPaint)
                        canvas.restore()
                    }
                } else {
                    // Railgun/standard — bright tracer
                    canvas.drawCircle(proj.position.x, proj.position.y, 4f, desertTankPaint)
                }
            }
        }
    }

    private fun renderDesertCrystal(canvas: Canvas) {
        val cx = 0f  // settlement center X
        val cy = desertSettlementWorldY
        val phase = state.desertCrystalPhase
        val t = state.desertCrystalTimer
        val time = (System.currentTimeMillis() % 10000L) / 1000f
        val orbPulse = 0.7f + 0.3f * sin(time * 4f)

        // Crystal hover height — used consistently across phases
        val hoverHeight = 60f

        // Position based on phase
        val crystalY = when (phase) {
            1 -> {
                // Rising from ground to hover height over 3s
                val riseT = (t / 3f).coerceIn(0f, 1f)
                val eased = 1f - (1f - riseT) * (1f - riseT)
                cy - hoverHeight * eased  // cy (well center) → cy - hoverHeight
            }
            2 -> {
                // Hovering at cy - hoverHeight with gentle bob
                cy - hoverHeight + sin(t * 3.0).toFloat() * 5f
            }
            3 -> {
                // Approaching player from hover position
                val approachT = (t / 2.5f).coerceIn(0f, 1f)
                val hoverY = cy - hoverHeight
                hoverY + (ship.position.y - hoverY) * approachT
            }
            4 -> {
                // Flash — stay at player position (where it just rammed)
                ship.position.y
            }
            else -> cy - hoverHeight
        }
        val crystalX = when (phase) {
            3 -> {
                val approachT = (t / 2.5f).coerceIn(0f, 1f)
                cx + (ship.position.x - cx) * approachT
            }
            4 -> ship.position.x  // Stay at player position
            else -> cx
        }

        // Trail below during rising
        if (phase == 1) {
            val riseT = (t / 3f).coerceIn(0f, 1f)
            desertTankPaint.color = CrystalPalette.MID
            for (i in 4 downTo 1) {
                val trailY = crystalY + i * 8f * riseT
                desertTankPaint.alpha = ((1f - i / 5f) * 60).toInt()
                canvas.drawCircle(crystalX, trailY, 5f - i * 0.8f, desertTankPaint)
            }
        }

        // Drain tendrils from crystal to TB (during drain phases)
        if (desertCrystalDrainPhase in 1..2 && desertTbAlive) {
            val drainIntensity = if (desertCrystalDrainPhase == 2) 0.5f else 1f
            desertTankPaint.strokeWidth = 2f
            desertTankPaint.style = Paint.Style.STROKE
            for (i in 0..2) {
                val wobble = sin(time * 8f + i * 2.1f).toFloat() * 15f
                val midX = (crystalX + desertTbX) / 2f + wobble
                val midY = (crystalY + desertTbY) / 2f + wobble * 0.5f
                desertTankPaint.color = CrystalPalette.MID
                desertTankPaint.alpha = (drainIntensity * 180).toInt()
                val path = Path()
                path.moveTo(crystalX, crystalY)
                path.quadTo(midX, midY, desertTbX, desertTbY)
                canvas.drawPath(path, desertTankPaint)
            }
            desertTankPaint.style = Paint.Style.FILL
        }

        // Outer glow (brighter during drain)
        val glowSize = if (desertCrystalDrainPhase >= 1) 14f else 10f
        desertTankPaint.color = CrystalPalette.MID
        desertTankPaint.alpha = (orbPulse * if (desertCrystalDrainPhase >= 1) 150 else 100).toInt()
        canvas.drawCircle(crystalX, crystalY, glowSize, desertTankPaint)

        // Core
        desertTankPaint.alpha = (orbPulse * 220).toInt()
        canvas.drawCircle(crystalX, crystalY, 4f, desertTankPaint)

        desertTankPaint.alpha = 255
    }

    private fun renderDesertCrystalWall(canvas: Canvas) {
        if (desertSettlementProgress <= 0f || desertBuildings.isEmpty()) return
        val wallY = desertSettlementWorldY + 220f
        val time = (System.currentTimeMillis() % 10000L) / 1000f

        // Semi-transparent crystal barrier across full corridor
        val left = DESERT_CORRIDOR_LEFT
        val right = DESERT_CORRIDOR_RIGHT
        val wallHeight = 12f

        // Base glow bar
        desertTankPaint.color = CrystalPalette.MID
        desertTankPaint.alpha = 60
        desertTankPaint.style = Paint.Style.FILL
        canvas.drawRect(left, wallY - wallHeight / 2f, right, wallY + wallHeight / 2f, desertTankPaint)

        // Brighter core line
        desertTankPaint.alpha = 120
        canvas.drawRect(left, wallY - 2f, right, wallY + 2f, desertTankPaint)

        // Crackling energy arcs — short vertical lightning segments along the wall
        desertTankPaint.style = Paint.Style.STROKE
        desertTankPaint.strokeWidth = 2f
        val arcCount = 12
        for (i in 0 until arcCount) {
            val seed = (i * 37 + (time * 3f).toInt()) % 17
            val phase = time * 5f + i * 1.7f
            val flicker = sin(phase).toFloat()
            if (flicker < 0.2f) continue  // Intermittent flicker — not all arcs visible

            val arcX = left + (right - left) * ((i.toFloat() + sin(time * 2f + i).toFloat() * 0.3f) / arcCount)
            val arcH = 8f + seed * 1.5f
            val wobble = sin(phase * 3f).toFloat() * 4f

            desertTankPaint.color = CrystalPalette.ICE
            desertTankPaint.alpha = (flicker * 200).toInt().coerceIn(40, 200)

            val path = Path()
            path.moveTo(arcX, wallY - arcH)
            path.lineTo(arcX + wobble, wallY)
            path.lineTo(arcX - wobble * 0.5f, wallY + arcH * 0.7f)
            canvas.drawPath(path, desertTankPaint)
        }

        // Outer glow (wider, faint)
        desertTankPaint.style = Paint.Style.FILL
        desertTankPaint.color = CrystalPalette.MID
        desertTankPaint.alpha = 20
        canvas.drawRect(left, wallY - wallHeight, right, wallY + wallHeight, desertTankPaint)

        desertTankPaint.alpha = 255
        desertTankPaint.style = Paint.Style.FILL
    }

    private fun renderDesertWrecks(canvas: Canvas) {
        for (wreck in desertWrecks) {
            // Darkened vehicle body
            val r = ((wreck.color shr 16) and 0xFF) / 3
            val g = ((wreck.color shr 8) and 0xFF) / 3
            val b = (wreck.color and 0xFF) / 3
            val darkColor = android.graphics.Color.argb(0xFF, r, g, b)
            renderDesertPickup(canvas, wreck.x, wreck.y, wreck.angle, darkColor, wreck.hasGun)

            // Flickering flame particles (3-5 small orange/yellow circles)
            for (i in 0..3) {
                val freq = 4f + i * 1.3f
                val flameOffX = sin(wreck.burnTimer * freq + i * 1.7f).toFloat() * 8f
                val flameOffY = sin(wreck.burnTimer * freq * 0.7f + i * 2.3f).toFloat() * 6f - 5f
                val flameAlpha = (180 + (sin(wreck.burnTimer * freq * 1.5f + i) * 75f).toInt()).coerceIn(100, 255)
                val flameColor = if (i % 2 == 0) {
                    android.graphics.Color.argb(flameAlpha, 255, 160, 40) // orange
                } else {
                    android.graphics.Color.argb(flameAlpha, 255, 220, 60) // yellow
                }
                desertTankPaint.color = flameColor
                desertTankPaint.style = Paint.Style.FILL
                canvas.drawCircle(wreck.x + flameOffX, wreck.y + flameOffY, 3f + sin(wreck.burnTimer * freq).toFloat() * 1.5f, desertTankPaint)
            }

            // Smoke — semi-transparent dark circle drifting upward
            val smokeY = wreck.y - 15f - (wreck.burnTimer % 3f) * 8f
            val smokeAlpha = (80 - ((wreck.burnTimer % 3f) * 25f).toInt()).coerceIn(10, 80)
            desertTankPaint.color = android.graphics.Color.argb(smokeAlpha, 40, 35, 30)
            desertTankPaint.style = Paint.Style.FILL
            val smokeRadius = 8f + (wreck.burnTimer % 3f) * 4f
            canvas.drawCircle(wreck.x + sin(wreck.burnTimer * 0.5f).toFloat() * 5f, smokeY, smokeRadius, desertTankPaint)
        }
    }

    private fun spawnDesertSettlementBuildings() {
        desertBuildings.clear()
        val cy = desertSettlementWorldY

        // Full-width settlement: corridor goes from -400 to +400, use -360 to +360 with margin
        // Organic layout — irregular blocks with alleys, filling screen upward from wall
        val buildingDefs = listOf(
            // === Northern strip (far from player approach) ===
            floatArrayOf(-340f, -280f, 45f, 40f), floatArrayOf(-280f, -290f, 35f, 45f),
            floatArrayOf(-220f, -275f, 40f, 35f), floatArrayOf(-150f, -285f, 50f, 40f),
            floatArrayOf(-80f, -280f, 35f, 45f), floatArrayOf(-30f, -290f, 45f, 35f),
            floatArrayOf(40f, -275f, 40f, 40f), floatArrayOf(100f, -285f, 35f, 45f),
            floatArrayOf(160f, -280f, 45f, 35f), floatArrayOf(230f, -290f, 40f, 40f),
            floatArrayOf(290f, -275f, 35f, 45f),
            // === Upper-middle strip ===
            floatArrayOf(-350f, -210f, 50f, 45f), floatArrayOf(-280f, -220f, 40f, 40f),
            floatArrayOf(-210f, -205f, 35f, 50f), floatArrayOf(-140f, -215f, 45f, 40f),
            floatArrayOf(-60f, -210f, 40f, 45f), floatArrayOf(20f, -220f, 50f, 35f),
            floatArrayOf(90f, -205f, 35f, 45f), floatArrayOf(160f, -215f, 40f, 40f),
            floatArrayOf(230f, -210f, 45f, 45f), floatArrayOf(300f, -220f, 40f, 35f),
            // === Center strip ===
            floatArrayOf(-340f, -130f, 45f, 50f), floatArrayOf(-270f, -140f, 40f, 45f),
            floatArrayOf(-200f, -125f, 50f, 40f), floatArrayOf(-120f, -135f, 35f, 50f),
            floatArrayOf(-50f, -130f, 45f, 45f), floatArrayOf(30f, -140f, 40f, 40f),
            floatArrayOf(100f, -125f, 50f, 45f), floatArrayOf(180f, -135f, 35f, 50f),
            floatArrayOf(250f, -130f, 45f, 40f), floatArrayOf(320f, -140f, 35f, 45f),
            // === Lower-middle strip ===
            floatArrayOf(-350f, -50f, 40f, 45f), floatArrayOf(-290f, -60f, 45f, 40f),
            floatArrayOf(-220f, -45f, 35f, 50f), floatArrayOf(-150f, -55f, 50f, 40f),
            floatArrayOf(-70f, -50f, 40f, 45f), floatArrayOf(10f, -60f, 45f, 35f),
            floatArrayOf(80f, -45f, 35f, 50f), floatArrayOf(150f, -55f, 40f, 40f),
            floatArrayOf(220f, -50f, 50f, 45f), floatArrayOf(300f, -60f, 35f, 40f),
            // === Southern strip (closest to player approach) ===
            floatArrayOf(-340f, 30f, 45f, 40f), floatArrayOf(-270f, 20f, 40f, 45f),
            floatArrayOf(-190f, 35f, 35f, 40f), floatArrayOf(-110f, 25f, 50f, 45f),
            floatArrayOf(-30f, 30f, 40f, 40f), floatArrayOf(50f, 20f, 45f, 45f),
            floatArrayOf(130f, 35f, 35f, 40f), floatArrayOf(210f, 25f, 40f, 45f),
            floatArrayOf(290f, 30f, 45f, 35f),
            // === Scattered outlying buildings ===
            floatArrayOf(-360f, 110f, 30f, 30f), floatArrayOf(-280f, 100f, 25f, 35f),
            floatArrayOf(280f, 105f, 30f, 30f), floatArrayOf(340f, 95f, 25f, 35f),
            floatArrayOf(-350f, -350f, 25f, 25f), floatArrayOf(330f, -340f, 30f, 25f),
        )

        for (b in buildingDefs) {
            desertBuildings.add(DesertBuilding(b[0], cy + b[1], b[2], b[3]))
        }
    }

    private fun spawnDesertCivilians() {
        desertCivilians.clear()
        val civBaseY = desertSettlementWorldY
        // Main road (x=0)
        desertCivilians.add(DesertCivilian(0f, civBaseY - 200f, 0f, 0f))
        desertCivilians.add(DesertCivilian(0f, civBaseY - 50f, 0f, 0f))
        desertCivilians.add(DesertCivilian(0f, civBaseY + 100f, 0f, 0f))
        // Left secondary road (x=-170)
        desertCivilians.add(DesertCivilian(-170f, civBaseY - 100f, 0f, 0f))
        desertCivilians.add(DesertCivilian(-170f, civBaseY + 50f, 0f, 0f))
        // Right secondary road (x=170)
        desertCivilians.add(DesertCivilian(170f, civBaseY - 200f, 0f, 0f))
        desertCivilians.add(DesertCivilian(170f, civBaseY + 30f, 0f, 0f))
    }

    private fun renderDesertSettlement(canvas: Canvas) {
        if (desertSettlementProgress <= 0f || desertBuildings.isEmpty()) return

        val centerX = 0f
        val cy = desertSettlementWorldY

        desertTankPaint.style = Paint.Style.FILL

        // --- Dirt roads ---
        val roadColor = 0xFFB89858.toInt()
        val sandColor = 0xFFC8A86E.toInt()
        val fadeLen = 40f  // fade length for road endpoints

        // Helper: draw a road rect with faded edges at specified ends
        fun drawRoadFaded(left: Float, top: Float, right: Float, bottom: Float,
                          fadeLeft: Boolean = false, fadeRight: Boolean = false,
                          fadeTop: Boolean = false, fadeBottom: Boolean = false) {
            desertTankPaint.color = roadColor
            desertTankPaint.alpha = 255
            canvas.drawRect(left, top, right, bottom, desertTankPaint)
            // Draw sand-colored fade overlays at specified ends
            if (fadeLeft) {
                for (i in 0..4) {
                    val frac = i / 5f
                    val x = left + fadeLen * frac
                    val x2 = left + fadeLen * (frac + 0.2f)
                    desertTankPaint.color = sandColor
                    desertTankPaint.alpha = (255 * (1f - frac)).toInt()
                    canvas.drawRect(x, top, x2, bottom, desertTankPaint)
                }
            }
            if (fadeRight) {
                for (i in 0..4) {
                    val frac = i / 5f
                    val x = right - fadeLen * (frac + 0.2f)
                    val x2 = right - fadeLen * frac
                    desertTankPaint.color = sandColor
                    desertTankPaint.alpha = (255 * (1f - frac)).toInt()
                    canvas.drawRect(x, top, x2, bottom, desertTankPaint)
                }
            }
            if (fadeTop) {
                for (i in 0..4) {
                    val frac = i / 5f
                    val y = top + fadeLen * frac
                    val y2 = top + fadeLen * (frac + 0.2f)
                    desertTankPaint.color = sandColor
                    desertTankPaint.alpha = (255 * (1f - frac)).toInt()
                    canvas.drawRect(left, y, right, y2, desertTankPaint)
                }
            }
            if (fadeBottom) {
                for (i in 0..4) {
                    val frac = i / 5f
                    val y = bottom - fadeLen * (frac + 0.2f)
                    val y2 = bottom - fadeLen * frac
                    desertTankPaint.color = sandColor
                    desertTankPaint.alpha = (255 * (1f - frac)).toInt()
                    canvas.drawRect(left, y, right, y2, desertTankPaint)
                }
            }
            desertTankPaint.alpha = 255
        }

        // Main north-south road (fade at both ends)
        drawRoadFaded(centerX - 15f, cy - 380f, centerX + 15f, cy + 220f, fadeTop = true, fadeBottom = true)
        // East-west cross streets (fade at both east/west canyon-wall ends)
        drawRoadFaded(-380f, cy - 15f, 380f, cy + 15f, fadeLeft = true, fadeRight = true)
        drawRoadFaded(-370f, cy - 160f, 370f, cy - 140f, fadeLeft = true, fadeRight = true)
        drawRoadFaded(-370f, cy + 70f, 370f, cy + 90f, fadeLeft = true, fadeRight = true)
        // Secondary north-south roads (fade at top and bottom endpoints)
        drawRoadFaded(-180f, cy - 350f, -160f, cy + 180f, fadeTop = true, fadeBottom = true)
        drawRoadFaded(160f, cy - 350f, 180f, cy + 180f, fadeTop = true, fadeBottom = true)

        // Wheel ruts on main road (shortened to stay within non-faded portion)
        desertTankPaint.color = 0xFF8A6E3E.toInt()
        desertTankPaint.strokeWidth = 2f
        desertTankPaint.style = Paint.Style.STROKE
        canvas.drawLine(centerX - 8f, cy - 380f + fadeLen, centerX - 8f, cy + 220f - fadeLen, desertTankPaint)
        canvas.drawLine(centerX + 8f, cy - 380f + fadeLen, centerX + 8f, cy + 220f - fadeLen, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL

        // --- Buildings from tracked list ---
        for (b in desertBuildings) {
            if (b.alive) {
                val colorVar = ((b.x.toInt() * 7 + b.y.toInt() * 13) and 0xF)
                val wallR = (0x9B + colorVar - 8).coerceIn(0x88, 0xA8)
                val wallG = (0x8B + colorVar - 8).coerceIn(0x78, 0x98)
                val wallB = (0x6B + colorVar - 8).coerceIn(0x58, 0x78)
                desertTankPaint.color = android.graphics.Color.argb(0xFF, wallR, wallG, wallB)
                canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, desertTankPaint)

                // Darker roof / top edge
                desertTankPaint.color = android.graphics.Color.argb(0xFF, wallR - 0x10, wallG - 0x10, wallB - 0x10)
                canvas.drawRect(b.x + 2f, b.y, b.x + b.w - 2f, b.y + 6f, desertTankPaint)

                // Door/window
                desertTankPaint.color = 0xFF3A3020.toInt()
                val doorW = b.w * 0.25f
                val doorH = b.h * 0.3f
                canvas.drawRect(b.x + b.w / 2f - doorW / 2f, b.y + b.h - doorH, b.x + b.w / 2f + doorW / 2f, b.y + b.h, desertTankPaint)

                if (b.w > 35f) {
                    val winX = b.x + b.w * 0.2f
                    canvas.drawRect(winX - 4f, b.y + 10f, winX + 4f, b.y + 18f, desertTankPaint)
                }
            } else {
                // Destroyed building remnants
                val cx = b.x + b.w / 2f
                val cy2 = b.y + b.h / 2f
                if (b.deathStyle == BuildingDeathStyle.CHARRED) {
                    // Charred husk — blackened walls, half-height
                    desertTankPaint.color = 0xFF2A2218.toInt()
                    canvas.drawRect(b.x, b.y + b.h * 0.4f, b.x + b.w, b.y + b.h, desertTankPaint)
                    // Broken wall stub on one side
                    desertTankPaint.color = 0xFF332A1E.toInt()
                    canvas.drawRect(b.x, b.y + b.h * 0.2f, b.x + b.w * 0.3f, b.y + b.h, desertTankPaint)
                    // Flickering flames
                    for (i in 0..2) {
                        val freq = 5f + i * 1.5f
                        val fx = cx + sin(b.burnTimer * freq + i * 2.1f).toFloat() * (b.w * 0.25f)
                        val fy = b.y + b.h * 0.3f + sin(b.burnTimer * freq * 0.7f + i * 1.7f).toFloat() * 4f
                        val fa = (160 + (sin(b.burnTimer * freq * 1.3f + i) * 80f).toInt()).coerceIn(80, 255)
                        desertTankPaint.color = if (i % 2 == 0) android.graphics.Color.argb(fa, 255, 140, 30) else android.graphics.Color.argb(fa, 255, 200, 50)
                        canvas.drawCircle(fx, fy, 3f + sin(b.burnTimer * freq).toFloat() * 1.5f, desertTankPaint)
                    }
                    // Smoke
                    val smokeY = b.y + b.h * 0.1f - (b.burnTimer % 4f) * 6f
                    val sa = (60 - ((b.burnTimer % 4f) * 15f).toInt()).coerceIn(5, 60)
                    desertTankPaint.color = android.graphics.Color.argb(sa, 40, 35, 30)
                    canvas.drawCircle(cx + sin(b.burnTimer * 0.3f).toFloat() * 5f, smokeY, 6f + (b.burnTimer % 4f) * 3f, desertTankPaint)
                } else {
                    // Rubble pile — scattered debris rectangles
                    desertTankPaint.color = 0xFF5A4A38.toInt()
                    canvas.drawRect(b.x + 2f, b.y + b.h * 0.6f, b.x + b.w - 2f, b.y + b.h, desertTankPaint)
                    // Rubble chunks
                    desertTankPaint.color = 0xFF6B5B48.toInt()
                    canvas.drawRect(b.x + b.w * 0.1f, b.y + b.h * 0.5f, b.x + b.w * 0.4f, b.y + b.h * 0.7f, desertTankPaint)
                    desertTankPaint.color = 0xFF4A3A28.toInt()
                    canvas.drawRect(b.x + b.w * 0.5f, b.y + b.h * 0.55f, b.x + b.w * 0.85f, b.y + b.h * 0.75f, desertTankPaint)
                    // Scattered smaller bits
                    desertTankPaint.color = 0xFF7A6A58.toInt()
                    canvas.drawRect(b.x + b.w * 0.2f, b.y + b.h * 0.4f, b.x + b.w * 0.35f, b.y + b.h * 0.55f, desertTankPaint)
                    // Dust settled (faded circle)
                    desertTankPaint.color = android.graphics.Color.argb(30, 160, 140, 100)
                    canvas.drawCircle(cx, cy2, b.w * 0.6f, desertTankPaint)
                }
            }
        }

        // Central well/plaza
        desertTankPaint.color = 0xFF7A6A4A.toInt()
        canvas.drawCircle(centerX, cy, 22f, desertTankPaint)
        desertTankPaint.color = 0xFF5A4A3A.toInt()
        desertTankPaint.style = Paint.Style.STROKE
        desertTankPaint.strokeWidth = 3f
        canvas.drawCircle(centerX, cy, 22f, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL
        desertTankPaint.color = 0xFF2A3040.toInt()
        canvas.drawCircle(centerX, cy, 12f, desertTankPaint)

        // Market awnings on some building fronts
        for (b in desertBuildings) {
            if (!b.alive) continue
            val hash = (b.x.toInt() * 17 + b.y.toInt() * 31) and 0xFF
            if (hash % 4 != 0) continue  // ~25% of buildings get awnings
            val awningColors = intArrayOf(0xFFAA4433.toInt(), 0xFF3366AA.toInt(), 0xFF887733.toInt(), 0xFF336644.toInt())
            desertTankPaint.color = awningColors[hash % awningColors.size]
            val awningH = 8f
            canvas.drawRect(b.x - 3f, b.y + b.h - awningH - 2f, b.x + b.w + 3f, b.y + b.h - 2f, desertTankPaint)
            // Shadow under awning
            desertTankPaint.color = android.graphics.Color.argb(40, 0, 0, 0)
            canvas.drawRect(b.x - 3f, b.y + b.h - 2f, b.x + b.w + 3f, b.y + b.h + 4f, desertTankPaint)
        }

        // Scattered barrels and crates
        val barrelPositions = listOf(
            floatArrayOf(-120f, cy - 100f), floatArrayOf(200f, cy - 200f), floatArrayOf(-250f, cy + 50f),
            floatArrayOf(100f, cy + 30f), floatArrayOf(-50f, cy - 250f), floatArrayOf(280f, cy - 80f)
        )
        for (pos in barrelPositions) {
            val hash = ((pos[0] * 7 + pos[1] * 13).toInt() and 0xFF)
            if (hash % 2 == 0) {
                // Barrel (circle)
                desertTankPaint.color = 0xFF6B5533.toInt()
                canvas.drawCircle(pos[0], pos[1], 5f, desertTankPaint)
                desertTankPaint.color = 0xFF5A4422.toInt()
                desertTankPaint.style = Paint.Style.STROKE
                desertTankPaint.strokeWidth = 1f
                canvas.drawCircle(pos[0], pos[1], 5f, desertTankPaint)
                desertTankPaint.style = Paint.Style.FILL
            } else {
                // Crate (rectangle)
                desertTankPaint.color = 0xFF7A6A44.toInt()
                canvas.drawRect(pos[0] - 5f, pos[1] - 5f, pos[0] + 5f, pos[1] + 5f, desertTankPaint)
                desertTankPaint.color = 0xFF5A4A34.toInt()
                desertTankPaint.style = Paint.Style.STROKE
                desertTankPaint.strokeWidth = 1f
                canvas.drawRect(pos[0] - 5f, pos[1] - 5f, pos[0] + 5f, pos[1] + 5f, desertTankPaint)
                desertTankPaint.style = Paint.Style.FILL
            }
        }

        // Compound walls
        desertTankPaint.color = 0xFF8B7B5B.toInt()
        desertTankPaint.strokeWidth = 3f
        desertTankPaint.style = Paint.Style.STROKE
        canvas.drawLine(-365f, cy - 360f, -365f, cy + 140f, desertTankPaint)
        canvas.drawLine(365f, cy - 360f, 365f, cy + 140f, desertTankPaint)
        canvas.drawLine(-25f, cy + 150f, -25f, cy + 200f, desertTankPaint)
        canvas.drawLine(25f, cy + 150f, 25f, cy + 200f, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL

        // Gate banner — faded cloth draped between gate posts
        desertTankPaint.color = 0xFF8B5533.toInt()  // faded terracotta
        val bannerY = cy + 155f
        // Sagging cloth shape
        val bannerPath = Path()
        bannerPath.moveTo(-25f, bannerY)
        bannerPath.quadTo(0f, bannerY + 12f, 25f, bannerY)  // sag in middle
        bannerPath.lineTo(25f, bannerY + 8f)
        bannerPath.quadTo(0f, bannerY + 18f, -25f, bannerY + 8f)
        bannerPath.close()
        canvas.drawPath(bannerPath, desertTankPaint)
        // Faded stripe across banner
        desertTankPaint.color = 0xFF6B4422.toInt()
        desertTankPaint.strokeWidth = 1f
        desertTankPaint.style = Paint.Style.STROKE
        canvas.drawLine(-20f, bannerY + 5f, 20f, bannerY + 5f, desertTankPaint)
        desertTankPaint.style = Paint.Style.FILL
    }

    private fun resetLoopState() {
        // Time crystal resets everything — dead pilots/ships restored, crystal cleared.
        // Stage returns to NORMAL; the loop number is bumped by the horror caller.
        val persistence = PersistenceManager(context)
        persistence.setStoryStageCode(StoryStage.NORMAL.code)
        persistence.clearDeadPilotsAndShips()
        persistence.setCrystalUnlocked(false)
        persistence.setCrystalPurchased(false)
        persistence.setAwaitingCrystalReveal(false)
        persistence.setSelectedShipId("ship_blue")    // Scout
        persistence.setSelectedPilotId("pilot_medic") // Medic
        // Reset pilot unlock sequence so Rascal recruits after the first Medic run.
        // -1 so the desert-return increment brings it to 0 (below the >= 1 threshold).
        persistence.setNextPilotIndex(1)
        persistence.setRunsSincePilotUnlock(-1)
        persistence.setFreshLoopStart()
    }

    private fun applyUpgrade(option: UpgradeOption) {
        // Handle fallback options when fully upgraded
        if (option.isFallback) {
            when (option.fallbackType) {
                FallbackType.HEALTH_RESTORE -> {
                    ship.health = (ship.health + ship.maxHealth * 0.2f).coerceAtMost(ship.maxHealth)
                }
                FallbackType.GOLD_BONUS -> {
                    val bonus = (state.goldCollected * 0.01f).toInt().coerceAtLeast(1)
                    state.goldCollected += bonus
                }
                null -> {}
            }
            return
        }

        if (option.isEvolution) {
            // Apply evolution: transforms base weapon into evolved weapon
            val baseWeaponId = option.baseWeaponId ?: return
            weaponSystem.applyEvolution(baseWeaponId, option.id, state)
            state.hasEvolvedThisGame = true
            if (state.astroLoopMode) state.astroLoopEvolutionUsed = true
            radioSystem.onEvolution(state)
            SoundManager.playSFX("sfx_evolution")
            // Autonomous Ace: upgrade the drone
            if (option.id == "autonomous_ace") {
                state.droneEvolved = true
            }
            // Track discovery for codex
            PersistenceManager(context).discoverEvolution(option.id)
        } else if (option.isWeapon) {
            weaponSystem.addWeapon(option.id, state)
            if (state.weaponLevels[option.id] == GameConfig.WEAPON_MAX_LEVEL) {
                radioSystem.onWeaponMaxed(state)
            }
            // Track weapon discovery for gating
            PersistenceManager(context).discoverWeapon(option.id)
        } else {
            state.addPassive(option.id)
            val instantMaxPassives = setOf("glass_cannon", "phoenix_core", "duplicator_core", "extra_weapon_slot", "lucky_star")
            if ((state.passiveStacks[option.id] ?: 0) >= GameConfig.PASSIVE_MAX_STACKS && option.id !in instantMaxPassives) {
                radioSystem.onPassiveMaxed(state)
            }
            // Sync ship shield stats after passive changes (e.g., Glass Cannon)
            ship.shieldCap = state.maxShieldCap
            ship.shieldRegenDisabled = state.shieldRegenDisabled
            ship.currentShield = minOf(ship.currentShield, ship.shieldCap)
        }

    }

    // ========================================================================
    // WAKE_UP — monitor pull-back transition (stage 5 good ending)
    // ========================================================================

    private fun updateWakeUp(deltaTime: Float) {
        desertWakeUpTimer += deltaTime
        if (desertWakeUpTimer >= WAKE_UP_SILENCE + WAKE_UP_DURATION + 1.0f) {
            // Placeholder: transition to GAME_BRICKED until Phase 2 meta-layer exists
            state.phase = GamePhase.GAME_BRICKED
        }
    }

    private fun renderWakeUp(canvas: Canvas) {
        // Silence period: just black
        if (desertWakeUpTimer < WAKE_UP_SILENCE) {
            canvas.drawColor(android.graphics.Color.BLACK)
            return
        }

        val w = screenWidth
        val h = screenHeight
        val animTime = (desertWakeUpTimer - WAKE_UP_SILENCE).coerceAtLeast(0f)
        val t = (animTime / WAKE_UP_DURATION).coerceIn(0f, 1f)
        val eased = 1f - (1f - t) * (1f - t)

        val scale = 1.0f - eased * 0.65f
        val scaledW = w * scale
        val scaledH = h * scale
        val offsetX = (w - scaledW) / 2f
        val offsetY = (h - scaledH) / 2f

        // Dark room background
        val roomAlpha = (eased * 255).toInt()
        canvas.drawColor(android.graphics.Color.BLACK)
        if (roomAlpha > 0) {
            val roomPaint = Paint().apply {
                color = android.graphics.Color.argb(roomAlpha, 15, 15, 20)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, w, h, roomPaint)
        }

        // Scale and draw the desert scene inside the shrinking frame
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        renderDesert(canvas)
        canvas.restore()

        // CRT monitor bezel
        if (eased > 0.1f) {
            val bezelAlpha = ((eased - 0.1f) / 0.9f * 255).toInt().coerceIn(0, 255)
            val bezelPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 8f * scale + 4f
                color = android.graphics.Color.argb(bezelAlpha, 60, 60, 70)
                isAntiAlias = true
            }
            canvas.drawRect(offsetX - 6f, offsetY - 6f, offsetX + scaledW + 6f, offsetY + scaledH + 6f, bezelPaint)
            val innerPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = android.graphics.Color.argb(bezelAlpha / 2, 100, 100, 120)
            }
            canvas.drawRect(offsetX + 2f, offsetY + 2f, offsetX + scaledW - 2f, offsetY + scaledH - 2f, innerPaint)

            // Scan lines
            if (eased > 0.3f) {
                val scanAlpha = ((eased - 0.3f) / 0.7f * 30).toInt().coerceIn(0, 30)
                val scanPaint = Paint().apply {
                    color = android.graphics.Color.argb(scanAlpha, 0, 0, 0)
                    style = Paint.Style.FILL
                }
                var scanY = offsetY
                while (scanY < offsetY + scaledH) {
                    canvas.drawRect(offsetX, scanY, offsetX + scaledW, scanY + 1f, scanPaint)
                    scanY += 4f
                }
            }
        }

        // Fade to black at the end
        if (t > 0.85f) {
            val fadeAlpha = ((t - 0.85f) / 0.15f * 255).toInt().coerceIn(0, 255)
            val fadePaint = Paint().apply {
                color = android.graphics.Color.argb(fadeAlpha, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, w, h, fadePaint)
        }
    }

    private fun updateDeathPlayOut(deltaTime: Float) {
        // Game world keeps running for 2s after death before crystal animation starts.
        // Ship is already dead/exploded. No player input, no player collision, no spawning.
        // Enemies, projectiles, asteroids, explosions, visual effects all keep updating.

        deathPlayOutTimer -= deltaTime

        // Update ship explosion animation
        if (shipExplosion.isActive) {
            shipExplosion.update(deltaTime)
        }
        enemyExplosions.forEach { it.update(deltaTime) }
        enemyExplosions.removeAll { !it.isActive }

        // Get all active entities
        EntityPools.asteroids.getActiveEntities(activeAsteroids)
        EntityPools.projectiles.getActiveEntities(activeProjectiles)
        EntityPools.enemies.getActiveEntities(activeEnemies)
        val asteroids = activeAsteroids
        val projectiles = activeProjectiles
        val enemies = activeEnemies

        // Update enemy AI (enemies keep moving and shooting)
        enemyAISystem.update(enemies, ship, deltaTime, asteroids, projectiles)

        // Update enemy weapons (cooldowns, stateful weapons like orbiters)
        enemyWeaponSystem.update(enemies, ship, deltaTime)

        // Fire enemy weapons (visual only — player is dead)
        for (enemy in enemies) {
            if (!enemy.isActive || enemy.isWarping || enemy.isCrewmate) continue
            if (!enemy.isSpawnShielded) {
                enemyWeaponSystem.fire(enemy, ship)
            }
        }

        // Update movement for asteroids, projectiles, power-ups
        EntityPools.powerUps.getActiveEntities(activePowerUps)
        val powerUps = activePowerUps
        movementSystem.updateAsteroids(asteroids, ship, deltaTime, state.survivalTime)
        movementSystem.updateProjectiles(projectiles, deltaTime)
        movementSystem.updatePowerUps(powerUps, deltaTime)

        // Mine repulsion physics
        movementSystem.applyMineRepulsion(projectiles, deltaTime)

        // Gravity well effects
        movementSystem.applyGravityWellEffects(projectiles, asteroids, deltaTime)

        // Process expired projectile end-of-life effects
        projectileEffectsSystem.processExpired(projectiles, asteroids, enemies)

        // Enemy-asteroid collisions still happen
        val destroyedEnemies = collisionSystem.checkEnemyAsteroidCollisions(enemies, asteroids)
        for (enemy in destroyedEnemies) {
            onEnemyDestroyed(enemy)
        }

        // Combat drones keep fighting
        combatDroneSystem.update(deltaTime, asteroids, enemies)
        combatDroneSystem.checkCollisions(asteroids, projectiles)

        // Boss keeps updating if active
        if (state.bossActive && boss.isActive) {
            bossSystem.update(deltaTime, projectiles, enemies, asteroids)
        }

        // Advance the charged-shot beam timer during death play-out so the flash animates and clears
        if (state.bossChargedShotActive) {
            state.bossChargedShotTimer += deltaTime
            if (state.bossChargedShotTimer >= 0.4f) {
                state.bossChargedShotActive = false
            }
        }

        // Fleet system keeps going
        fleetSystem.update(state, deltaTime)

        // Saw damage system keeps running (sparks animate)
        sawDamageSystem.updateSparks(deltaTime)
        boss.updateShieldSparks(deltaTime)  // keep boss shield sparks expiring during the death play-out

        // Update visual effects (explosions, damage numbers, hit flashes)
        visualEffects.update(deltaTime)

        // Cleanup inactive entities
        EntityPools.cleanupInactive()

        // Update fading asteroid trails
        fadingTrails.forEach { it.fadeTimer -= deltaTime }
        fadingTrails.removeAll { it.fadeTimer <= 0f }

        // Radio system: tick cooldowns and display timers
        radioSystem.update(deltaTime, state)

        // Update starfield parallax based on camera
        starfieldRenderer.updateWithCamera(camera)

        // When timer expires, transition to crystal death
        if (deathPlayOutTimer <= 0f) {
            state.phase = GamePhase.CRYSTAL_DEATH
            crystalFreezeDelay = 0f  // No additional freeze delay — we already played out
            crystalDelayTimer = 0f
            crystalDelayActive = false  // Skip dead pause — go straight to crystal animation
            crystalRenderer.activateDeath(screenWidth, screenHeight)
            SoundManager.playSFX("sfx_crystal_activate")
        }
    }

    private fun updateCrystalDeath(deltaTime: Float) {
        // Brief delay to let explosion spread before freezing
        if (crystalFreezeDelay > 0f) {
            crystalFreezeDelay -= deltaTime
            if (shipExplosion.isActive) {
                shipExplosion.update(deltaTime)
            }
            return
        }

        // 3-second dead pause before crystal animation begins
        if (crystalDelayActive) {
            crystalDelayTimer -= deltaTime
            if (crystalDelayTimer <= 0f) {
                crystalDelayActive = false
                crystalRenderer.activateDeath(screenWidth, screenHeight)
                SoundManager.playSFX("sfx_crystal_activate")
            }
            return
        }

        // Update crystal overlay only — everything else is frozen
        crystalRenderer.update(deltaTime)

        if (crystalRenderer.isComplete) {
            onGameOver(state.goldCollected, false)
        }
    }

    private fun gameOver(skipCrystal: Boolean = false) {
        // Start explosion
        shipExplosion.start(
            ship.position.x,
            ship.position.y,
            ship.velocity.x,
            ship.velocity.y,
            ship.rotation,
            ship.radius,
            if (state.isCorruptionRun) Boss.CORRUPTION_COLOR else ship.shipColor
        )
        ship.isActive = false
        vibrateExplosion()
        SoundManager.playSFX("sfx_player_death")

        // Fade out orbiters instead of instant disappear
        for (projectile in activeProjectiles) {
            if (projectile.type == ProjectileType.ORBITER) {
                projectile.lifetime = projectile.age + 0.5f  // Fade out over 0.5s
            }
        }

        // Clean up corruption run state
        state.corruptionInBossArena = false
        state.playerStunned = false
        state.scriptedDeathActive = false

        // Fade out combat music on death
        SoundManager.stopCombatMusic()

        if (skipCrystal) {
            // Corruption death — no crystal animation, go straight to game over
            state.phase = GamePhase.GAME_OVER
            corruptionDeathTimer = 1.5f
        } else {
            // Death play-out: game world keeps running for 2s before crystal animation
            state.phase = GamePhase.DEATH_PLAY_OUT
            state.revengeActive = false
            deathPlayOutTimer = 2.0f
        }

        // Save stats
        saveRunStats(includeDeath = true)
    }

    private fun saveRunStats(includeDeath: Boolean) {
        val isNewHighScore = highScoreManager.saveBestTime(state.survivalTime)
        val isNewScoreRecord = highScoreManager.saveHighScore(state.score)
        if (isNewHighScore) {
            state.bestTime = state.survivalTime
        }
        if (isNewScoreRecord) {
            state.highScore = state.score
        }
        highScoreManager.incrementGamesPlayed()
        highScoreManager.addPlayTime(state.survivalTime)
        highScoreManager.updateHighestEvolutionCount(state.evolvedWeapons.size)

        val persistence = PersistenceManager(context)
        persistence.addTotalYenEarned(state.goldCollected)
        persistence.addTotalDamageTaken(state.telemetryTotalDamageTaken.toInt())
        if (includeDeath) {
            persistence.incrementTotalDeaths()
        }
        persistence.addTotalKills(enemiesKilledThisRun)
        persistence.saveBestKillStreak(bestKillStreakThisRun)
        persistence.saveBestContinuousFlightSeconds(bestContinuousFlightThisRun)
        persistence.saveBestSingleRunKills(enemiesKilledThisRun)
        persistence.saveBestSurvivalSeconds(state.survivalTime.toInt())
        if (state.astroLoopMode) {
            persistence.setLastAstroRunSeconds(state.survivalTime)
            persistence.updateAstroLoopBestSeconds(state.survivalTime)
            BandanaAward.maybeAward(persistence, state.activePilotId, state.survivalTime)
        }

        telemetryManager.logRunEnd(
            causeOfDeath = state.lastDamageSource,
            survivedSeconds = state.survivalTime,
            yenEarned = state.goldCollected,
            yenFromAsteroids = state.telemetryYenFromAsteroids,
            yenFromEnemies = state.telemetryYenFromEnemies,
            enemiesKilled = enemiesKilledThisRun,
            asteroidsDestroyed = state.telemetryAsteroidsDestroyed,
            totalDamageDealt = state.telemetryTotalDamageDealt,
            totalDamageTaken = state.telemetryTotalDamageTaken,
            critsTotal = state.telemetryCritsTotal,
            upgradeDrops = state.telemetryUpgradeDropsCollected,
            diamonds = state.telemetryDiamondsCollected,
            finalWeapons = state.weaponLevels.toMap(),
            finalPassives = state.passiveStacks.toMap(),
            evolutionsTriggered = state.evolvedWeapons.toList()
        )
        telemetryManager.flush()
    }

    fun render(canvas: Canvas) {
        canvas.save()
        canvas.scale(renderScale, renderScale)

        // Clear background (dark red when boss is active, normal for heart-to-heart)
        if (state.bossActive && state.phase != GamePhase.HEART_TO_HEART) {
            backgroundPaint.color = 0xFF0A0304.toInt()  // Very dark maroon, subtle
        } else {
            backgroundPaint.color = GameConfig.COLOR_BACKGROUND
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, backgroundPaint)

        // Update starfield boss mode (off during heart-to-heart for calm scene)
        starfieldRenderer.bossMode = state.bossActive && state.phase != GamePhase.HEART_TO_HEART

        // Draw starfield
        starfieldRenderer.render(canvas)

        when (state.phase) {
            GamePhase.PLAYING -> renderPlaying(canvas)
            GamePhase.DEATH_PLAY_OUT -> renderPlaying(canvas)
            GamePhase.UPGRADE_SELECTION -> {
                renderPlaying(canvas)
                upgradeSelectionRenderer.render(canvas, upgradeSystem.getPendingOptions(), state, upgradeSystem)
            }
            GamePhase.CRYSTAL_DEATH -> {
                // Black background if coming from heart-to-heart, desert if from desert, game world otherwise
                if (state.heartToHeartLog.isNotEmpty()) {
                    canvas.drawColor(android.graphics.Color.BLACK)
                } else if (desertCrystalDeath) {
                    renderDesert(canvas)
                } else {
                    renderPlaying(canvas)
                }
                crystalRenderer.render(canvas, screenWidth, screenHeight)
            }
            GamePhase.HEART_TO_HEART -> {
                // Black screen — background already cleared, render centered dialogue
                renderHeartToHeart(canvas)
            }
            GamePhase.DESERT -> renderDesert(canvas)
            GamePhase.DESERT_FAREWELL -> {
                // Black screen with farewell dialogue
                renderHeartToHeart(canvas)
            }
            GamePhase.TIMELINE_SHIFT -> {
                // Farewell text stays exit-faded (no pop). Fade the whole scene to black
                // so it blends into the hangar's black fade-in.
                renderHeartToHeart(canvas)
                val a = (timelineShiftAlpha * 255f).toInt().coerceIn(0, 255)
                canvas.drawColor(android.graphics.Color.argb(a, 0, 0, 0))
            }
            GamePhase.WAKE_UP -> renderWakeUp(canvas)
            GamePhase.GAME_OVER -> {
                renderPlaying(canvas)
                // Fade to black during corruption death (1.5s timer counting down)
                if (corruptionDeathTimer > -1f) {
                    val fadeAlpha = (1f - corruptionDeathTimer / 1.5f).coerceIn(0f, 1f)
                    canvas.drawColor(android.graphics.Color.argb((fadeAlpha * 255).toInt(), 0, 0, 0))
                }
            }
            GamePhase.GAME_BRICKED -> {
                renderBrickScreen(canvas)
            }
        }

        // Heart-to-heart transition: full-screen white flash fading to black
        if (state.bossFightPhase == PHASE_HEART_TRANSITION) {
            val t = state.bossFightTimer
            when {
                t < 0.3f -> {
                    // White fills screen (fade in from explosion)
                    val alpha = (t / 0.3f).coerceIn(0f, 1f)
                    canvas.drawColor(android.graphics.Color.argb((alpha * 255).toInt(), 255, 255, 255))
                }
                else -> {
                    // White fades to black
                    val fadeT = ((t - 0.3f) / 0.7f).coerceIn(0f, 1f)
                    val whiteAlpha = 1f - fadeT
                    val blackAlpha = fadeT
                    canvas.drawColor(android.graphics.Color.argb((whiteAlpha * 255).toInt(), 255, 255, 255))
                    canvas.drawColor(android.graphics.Color.argb((blackAlpha * 255).toInt(), 0, 0, 0))
                }
            }
        }

        // Global fade overlay — used for scene transition fade-ins (black fades away to reveal new scene)
        if (globalFadeAlpha > 0f) {
            val fadeInt = (globalFadeAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawColor(android.graphics.Color.argb(fadeInt, 0, 0, 0))
        }

        // Pause overlay (renders on top of game)
        if (state.isPaused) {
            renderPaused(canvas)
        }

        // Debug menu overlay (renders on top of everything)
        if (state.debugMenuOpen) {
            debugMenuRenderer.render(canvas, state)
        }
    }

    private fun renderPlaying(canvas: Canvas) {
        // Render entities in world space (apply camera transform)
        val asteroids = activeAsteroids
        val projectiles = activeProjectiles
        val powerUps = activePowerUps
        val enemies = activeEnemies

        canvas.save()
        canvas.translate(-camera.x, -camera.y)

        // World-space rendering
        for (powerUp in powerUps) {
            if (!camera.isVisible(powerUp.position.x, powerUp.position.y, 60f)) continue
            vectorRenderer.renderPowerUp(canvas, powerUp)
        }

        for (asteroid in asteroids) {
            if (!camera.isVisible(asteroid.position.x, asteroid.position.y, asteroid.radius + 50f)) continue
            vectorRenderer.renderAsteroid(canvas, asteroid)
        }
        for (trail in fadingTrails) {
            vectorRenderer.renderFadingTrail(canvas, trail)
        }

        for (projectile in projectiles) {
            if (!camera.isVisible(projectile.position.x, projectile.position.y, 60f)) continue
            vectorRenderer.renderProjectile(canvas, projectile)
        }

        for (enemy in enemies) {
            if (!camera.isVisible(enemy.position.x, enemy.position.y, enemy.radius + 60f)) continue
            vectorRenderer.renderEnemyShip(canvas, enemy, state)
        }
        vectorRenderer.renderSolarTelegraphs(canvas, enemies)

        // Off-screen pointer for past Astro during corruption chase
        if (state.bossFightPhase == PHASE_OTHER_SURVIVAL && crewmateEncounter.isHealthySelf) {
            val pastAstro = crewmateEncounter.crewmateShip
            if (pastAstro != null && pastAstro.isActive) {
                val pointerColor = ShipDefinitions.getShip(pastAstro.shipId)?.color ?: 0xFFFFFFFF.toInt()
                vectorRenderer.renderOffScreenPointer(
                    canvas, pastAstro.position.x, pastAstro.position.y,
                    camera.x, camera.y, screenWidth, screenHeight, pointerColor
                )
            }
        }

        // Render enemy saw discs (enemies with energy_saw weapon)
        for (enemy in enemies) {
            if (!enemy.isActive || enemy.weaponId != "energy_saw") continue
            if (enemy.isWarping || enemy.isCrewmate) continue
            val discPositions = enemyWeaponSystem.getSawDiscPositions(enemy)
            val sawColor = if (state.isCorruptionRun) {
                ShipDefinitions.getWeaponColor("energy_saw", false)
            } else {
                Boss.CORRUPTION_COLOR
            }
            vectorRenderer.renderSawDiscs(canvas, discPositions, enemy.sawDiscRadius, sawColor)
        }

        if (playerRushBurn.hasContent() || state.corruptionRushPhase > 0) {
            vectorRenderer.renderReentryBurn(
                canvas, playerRushBurn,
                ship.position.x, ship.position.y, ship.rotation, ship.radius,
                Boss.CORRUPTION_COLOR, ship.startingWeaponId,
                intensity = if (state.corruptionRushPhase == 1) BossRush.easeIn(state.corruptionRushTimer) else 0f
            )
        }
        vectorRenderer.renderShip(canvas, ship, state)

        // Render combat drones (autonomous AI wingmen)
        if (combatDroneSystem.drones.isNotEmpty()) {
            vectorRenderer.drawCombatDrones(canvas, combatDroneSystem.drones, state)
        }

        // Render saw discs — fade during death play-out
        val sawFadeAlpha = if (state.phase == GamePhase.PLAYING) 1f else 0f
        for (weaponId in weaponSystem.getActiveWeaponIds()) {
            val weapon = weaponSystem.getWeapon(weaponId)
            if (weapon is EnergySaw) {
                val positions = weapon.getDiscPositions(ship.position.x, ship.position.y, ship.rotation)
                val spinSpeed = weapon.baseCooldown / weapon.getTickRate()
                if (sawFadeAlpha < 1f) canvas.saveLayerAlpha(null, (255 * sawFadeAlpha).toInt())
                vectorRenderer.renderSawDiscs(canvas, positions, weapon.discRadius,
                    ShipDefinitions.getWeaponColor("energy_saw", state.isCorruptionRun), spinSpeed = spinSpeed)
                if (sawFadeAlpha < 1f) canvas.restore()
            } else if (weapon is WarpSaw) {
                val color = ShipDefinitions.getEvolutionColor("energy_saw", state.isCorruptionRun)
                val spinSpeed = weapon.baseCooldown / weapon.getTickRate()
                if (sawFadeAlpha < 1f) canvas.saveLayerAlpha(null, (255 * sawFadeAlpha).toInt())
                if (weapon.isWarping) {
                    val front = weapon.getFrontPosition(ship.position.x, ship.position.y, ship.rotation)
                    vectorRenderer.renderWarpSawChrono(canvas, weapon.warpFromX, weapon.warpFromY,
                        front.first, front.second, weapon.warpProgress, weapon.discRadius, color, 1f)
                } else {
                    val positions = weapon.getDiscPositions(ship.position.x, ship.position.y, ship.rotation)
                    vectorRenderer.renderSawDiscs(canvas, positions, weapon.discRadius, color,
                        isWarpSaw = true, spinSpeed = spinSpeed)
                }
                if (sawFadeAlpha < 1f) canvas.restore()
            }
        }
        // Render saw sparks
        vectorRenderer.renderSawSparks(canvas, sawDamageSystem.sawSparks)

        // Render Oblivion Beam lance
        if (beamDamageSystem.beamActive && sawFadeAlpha >= 1f) {
            vectorRenderer.renderOblivionBeam(canvas, ship.position.x, ship.position.y, ship.rotation, ship.radius,
                ShipDefinitions.getEvolutionColor("railgun", state.isCorruptionRun))
        }


        // Render vampiric leech particles
        vectorRenderer.renderLeechParticles(canvas, vampiricLeecherSystem.particles, ship.position.x, ship.position.y)

        // Revenge Protocol visual when active
        if (state.revengeActive) {
            vectorRenderer.drawRevengeProtocolEffect(canvas, ship)
        }

        // Render boss if active (skip during corruption finale — boss entity is just a fleet target marker)
        if (state.bossActive && boss.isActive && state.bossFightPhase < PHASE_OTHER_SPAWN) {
            vectorRenderer.renderBoss(canvas, boss)
            if (state.bossCharging) {
                vectorRenderer.renderBossChargeOverlay(
                    canvas, boss,
                    ship.position.x, ship.position.y,
                    state
                )
            }
            if (state.bossChargedShotActive) {
                vectorRenderer.renderBossChargedShot(
                    canvas, boss,
                    ship.position.x, ship.position.y,
                    state.bossChargedShotTimer / 0.4f
                )
            }
        }

        // Corruption finale: render shield barrier + ripples at player position (boss entity is invisible target marker)
        if (boss.isActive && state.bossFightPhase >= PHASE_OTHER_SURVIVAL &&
                state.bossFightPhase <= PHASE_OTHER_TB26_RAM) {
            vectorRenderer.renderBossShieldEffects(canvas, boss)
            if (state.bossCharging) {
                val pastAstro = crewmateEncounter.crewmateShip
                val targetX = pastAstro?.position?.x ?: ship.position.x
                val targetY = pastAstro?.position?.y ?: ship.position.y
                vectorRenderer.renderBossChargeOverlay(canvas, boss, targetX, targetY, state)
            }
        }

        // Corruption run fleet/formation phases: boss is active (target marker at ship position),
        // charge overlay must be visible before the shield assault block kicks in.
        if (boss.isActive && state.bossCharging &&
                (state.bossFightPhase == PHASE_OTHER_SURVIVAL ||
                 state.bossFightPhase == PHASE_OTHER_FLEET ||
                 state.bossFightPhase == PHASE_OTHER_FORMATION)) {
            val pastAstro = crewmateEncounter.crewmateShip
            val targetX = pastAstro?.position?.x ?: ship.position.x
            val targetY = pastAstro?.position?.y ?: ship.position.y
            vectorRenderer.renderBossChargeOverlay(canvas, boss, targetX, targetY, state)
        }

        // Crystal reckoning boss (2× crystal Specter, world-space)
        // NOTE: the canvas is already camera-translated here — draw at world coordinates.
        // Subtracting the camera again doubled the offset, so the drawn ship drifted away
        // from the true boss position (where the bullets emit) as the camera moved.
        reckoningBoss?.let { rb ->
            if (rb.isActive) {
                val bx = rb.position.x
                val by = rb.position.y
                ShipRenderer.drawShip(
                    canvas = canvas,
                    shapeRenderer = shapeRenderer,
                    x = bx, y = by,
                    rotation = rb.rotation,
                    size = rb.radius,
                    shipColor = CrystalPalette.MID,
                    pilotColor = CrystalPalette.ICE,
                    startingWeaponId = "railgun"
                )
                // Crystal orb (MID glow + white core) seated in the tail notch — hull-local
                // -0.40*R on the rail spine, below the cockpit, rotating with the ship
                // (V3 on the placement board)
                val time = (System.currentTimeMillis() % 10000L) / 1000f
                val pulse = 0.7f + 0.3f * sin(time * 4f)
                val orbX = bx + cos(rb.rotation) * (-0.40f * rb.radius)
                val orbY = by + sin(rb.rotation) * (-0.40f * rb.radius)
                reckoningOrbPaint.color = CrystalPalette.MID
                reckoningOrbPaint.alpha = (pulse * 100).toInt()
                canvas.drawCircle(orbX, orbY, 18f, reckoningOrbPaint)
                reckoningOrbPaint.alpha = (pulse * 220).toInt()
                canvas.drawCircle(orbX, orbY, 8f, reckoningOrbPaint)
                reckoningOrbPaint.color = CrystalPalette.CORE
                reckoningOrbPaint.alpha = (pulse * 255).toInt()
                canvas.drawCircle(orbX, orbY, 4f, reckoningOrbPaint)
            }
        }

        // Ghost-ship lance overlay (renders while reckoningStage == LANCE).
        // Camera 0,0: the lance subtracts the camera internally, but this canvas is already
        // camera-translated — passing the real camera would double the offset (off-screen).
        if (state.reckoningActive && state.reckoningStage == GameState.ReckoningStage.LANCE) {
            ghostLance.render(canvas, shapeRenderer, 0f, 0f, screenWidth, screenHeight)
        }

        // Render fleet ships (cinematic puppet ships)
        vectorRenderer.renderFleet(canvas, fleetSystem)

        // Render upgrade indicators around ship (hide on death)
        if (state.phase == GamePhase.PLAYING || state.phase == GamePhase.UPGRADE_SELECTION) {
            vectorRenderer.renderUpgradeIndicators(canvas, powerUps, ship)
        }

        // Render visual effects (explosions, hit flashes)
        vectorRenderer.renderVisualEffects(canvas, visualEffects)

        // Render zap beam effect (crewmate teleport-back)
        vectorRenderer.renderZapBeam(canvas, state)

        // Render damage numbers
        vectorRenderer.renderDamageNumbers(canvas, visualEffects)

        // Render ship explosion if active
        if (shipExplosion.isActive) {
            vectorRenderer.renderShipExplosion(canvas, shipExplosion)
        }
        for (ex in enemyExplosions) {
            vectorRenderer.renderShipExplosion(canvas, ex)
        }

        canvas.restore()

        // Crystal zap effect (localized Time Crystal burst on crewmate kill)
        if (state.crystalZapActive) {
            crystalRenderer.renderZap(canvas, camera.x, camera.y, screenWidth, screenHeight)
        }

        // Time Crystal orb (boss fight victory sequence)
        if (state.timeCrystalPhase != GameState.TimeCrystalPhase.NONE) {
            crystalRenderer.renderTimeCrystalOrb(canvas, state, camera.x, camera.y)
        }

        // Screen-space rendering (HUD)
        val graceAlpha = if (state.graceTimer > 0f) (1f - state.graceTimer / 0.5f).coerceIn(0f, 1f) else 1f
        val hudAlpha = graceAlpha * state.hudFadeAlpha
        hudRenderer.render(canvas, state, ship, hudAlpha)

        // Astro Loop retreat fade to black overlay (phase 3)
        if (state.retreatPhase == 3) {
            val fadeAlpha = (state.retreatTimer / 1.5f * 255).toInt().coerceIn(0, 255)
            canvas.drawColor(android.graphics.Color.argb(fadeAlpha, 0, 0, 0))
        }
    }

    private fun renderHeartToHeart(canvas: Canvas) {
        // Chat-style layout — TB-26 left-aligned, Astro right-aligned
        if (state.heartToHeartLog.isEmpty()) return

        // Exit fade: fades from 1.0 to 0.0 over heartToHeartFadeDuration, then stays 0
        val exitFade = if (heartToHeartFadingOut) (1f - (heartToHeartFadeTimer / heartToHeartFadeDuration).coerceIn(0f, 1f)) else 1f
        if (exitFade <= 0f) return // Nothing to draw during black hold

        val totalLines = state.heartToHeartLog.size
        val entrySpacing = 60f
        val leftX = screenWidth * 0.10f
        val rightX = screenWidth * 0.90f
        val bottomBaseline = screenHeight * 0.65f
        val fadeStart = screenHeight * 0.20f
        val fadeEnd = screenHeight * 0.08f

        heartLinePaint.typeface = FontManager.getRegular()

        for ((index, entry) in state.heartToHeartLog.withIndex()) {
            val (speaker, text, color) = entry
            val dialogueY = bottomBaseline - (totalLines - 1 - index) * entrySpacing

            // Skip lines entirely above fade zone
            if (dialogueY < fadeEnd) continue

            // Calculate fade alpha for lines in the fade zone
            val fadeAlpha = if (dialogueY < fadeStart) {
                ((dialogueY - fadeEnd) / (fadeStart - fadeEnd) * 255).toInt().coerceIn(0, 255)
            } else {
                255
            }

            // TB-26 left, Astro right — the CRYSTAL is neither of them: centered, cyan
            val x = when (speaker) {
                LoopDefinitions.TB -> leftX
                LoopDefinitions.CRYSTAL -> screenWidth * 0.5f
                else -> rightX
            }
            heartLinePaint.textAlign = when (speaker) {
                LoopDefinitions.TB -> Paint.Align.LEFT
                LoopDefinitions.CRYSTAL -> Paint.Align.CENTER
                else -> Paint.Align.RIGHT
            }

            // Draw dialogue text — 26pt, full alpha (scaled by fade and exit fade)
            heartLinePaint.textSize = 26f
            heartLinePaint.color = color
            heartLinePaint.alpha = (fadeAlpha * exitFade).toInt().coerceIn(0, 255)

            val displayDialogue = if (index == totalLines - 1 && state.heartToHeartCharIndex > 0) {
                val chars = state.heartToHeartCharIndex.coerceAtMost(text.length)
                text.substring(0, chars)
            } else {
                text
            }
            canvas.drawText(displayDialogue, x, dialogueY, heartLinePaint)
        }

        // Restore paint to known state for subsequent renders
        heartLinePaint.textSize = 28f
        heartLinePaint.textAlign = Paint.Align.CENTER
        heartLinePaint.style = Paint.Style.FILL
    }

    private fun renderBrickScreen(canvas: Canvas) {
        canvas.drawColor(android.graphics.Color.BLACK)

        heartLinePaint.typeface = FontManager.getRegular()
        heartLinePaint.textAlign = Paint.Align.CENTER
        heartLinePaint.textSize = 28f

        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // "The loop is broken." — fades in at 1s, holds, fades out at 4s
        val line1Alpha = when {
            brickScreenTimer < 1f -> 0f
            brickScreenTimer < 2f -> (brickScreenTimer - 1f)
            brickScreenTimer < 4f -> 1f
            brickScreenTimer < 5f -> 1f - (brickScreenTimer - 4f)
            else -> 0f
        }

        if (line1Alpha > 0f) {
            heartLinePaint.color = 0xFFFFFFFF.toInt()
            heartLinePaint.alpha = (line1Alpha * 255).toInt()
            canvas.drawText("The loop is broken.", centerX, centerY - 20f, heartLinePaint)
        }

        // "Goodbye, commander." — fades in at 6s, stays permanently
        val line2Alpha = when {
            brickScreenTimer < 6f -> 0f
            brickScreenTimer < 7.5f -> (brickScreenTimer - 6f) / 1.5f
            else -> 1f
        }

        // Compute goodbye Y position — animates up when stats revealed
        val goodbyeY = if (brickStatsRevealed) {
            val t = (brickStatsTimer / 0.5f).coerceIn(0f, 1f)
            val eased = 1f - (1f - t) * (1f - t)
            centerY + 20f + (screenHeight * 0.25f - (centerY + 20f)) * eased
        } else {
            centerY + 20f
        }

        if (line2Alpha > 0f) {
            heartLinePaint.color = 0xFF6688AA.toInt()  // TB-26's color
            heartLinePaint.alpha = (line2Alpha * 255).toInt()
            canvas.drawText("Goodbye, commander.", centerX, goodbyeY, heartLinePaint)
        }

        // Career stats — fade in after goodbye finishes moving
        if (brickStatsRevealed) {
            val statsAlpha = ((brickStatsTimer - 0.5f) / 0.5f).coerceIn(0f, 1f)
            if (statsAlpha > 0f) {
                val alphaInt = (statsAlpha * 255).toInt()
                heartLinePaint.textSize = 22f
                val startY = screenHeight * 0.42f
                val lineSpacing = 36f

                for ((i, stat) in cachedBrickStatLines.withIndex()) {
                    val y = startY + i * lineSpacing

                    // Label in TB-26 blue, right-aligned
                    heartLinePaint.textAlign = Paint.Align.RIGHT
                    heartLinePaint.color = 0xFF6688AA.toInt()
                    heartLinePaint.alpha = alphaInt
                    canvas.drawText(stat.first, centerX - 20f, y, heartLinePaint)

                    // Value in white, left-aligned
                    heartLinePaint.textAlign = Paint.Align.LEFT
                    heartLinePaint.color = 0xFFFFFFFF.toInt()
                    heartLinePaint.alpha = alphaInt
                    canvas.drawText(stat.second, centerX + 20f, y, heartLinePaint)
                }

                // Reset paint state
                heartLinePaint.textAlign = Paint.Align.CENTER
                heartLinePaint.textSize = 28f
            }
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle debug menu touch events when open
        if (state.debugMenuOpen) {
            val result = debugMenuRenderer.handleTouch(event, state)
            when {
                result == "RESET_SMALL" -> {
                    val persistence = PersistenceManager(context)
                    persistence.resetAllProgress()
                    persistence.setYen(50)
                    telemetryManager.clearLog()
                    state.debugMenuOpen = false
                    onGameOver(0, false)
                }
                result == "RESET_BIG" -> {
                    val persistence = PersistenceManager(context)
                    persistence.resetAllProgress()
                    persistence.setYen(10_000_000)
                    telemetryManager.clearLog()
                    persistence.unlockAllShipsAndPilots()
                    for (id in listOf("health", "shields", "speed", "damage", "crit", "yen_bonus", "salvage", "magnet")) {
                        persistence.setUpgradeLevel(id, 5)
                    }
                    state.debugMenuOpen = false
                    onGameOver(0, false)
                }
                result?.startsWith("EVOLVE:") == true -> {
                    val parts = result.split(":")
                    if (parts.size == 3) {
                        val baseWeaponId = parts[1]
                        val evolvedId = parts[2]
                        weaponSystem.applyEvolution(baseWeaponId, evolvedId, state)
                        state.hasEvolvedThisGame = true
                        if (evolvedId == "autonomous_ace") {
                            state.droneEvolved = true
                        }
                    }
                }
                result == "WEAPON_TOGGLE" -> {
                    weaponSystem.syncFromState(state)
                    state.recalculateStats()
                }
                result == "PASSIVE_TOGGLE" -> {
                    state.recalculateStats()
                }
                result == "INSTANT_DEATH" -> {
                    state.debugMenuOpen = false
                    state.lastDamageSource = "debug_kill"
                    ship.health = 0f
                    handlePlayerDeath()
                }
                result == "BOSS_NOW" -> {
                    state.survivalTime = Boss.SPAWN_TIME - 1f
                    state.debugMenuOpen = false
                }
                result == "SET_CORRUPT" -> {
                    val persistence = PersistenceManager(context)
                    persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
                    state.debugMenuOpen = false
                    onGameOver(0, false)
                }
                result == "KILL_PILOT" -> {
                    val persistence = PersistenceManager(context)
                    if (StoryStateManager.stage(persistence) == StoryStage.NORMAL) {
                        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
                    }
                    val deadPilots = persistence.getDeadPilots()
                    val nextAlive = PilotDefinitions.pilots
                        .filter { it.id != "pilot_astro" && !deadPilots.contains(it.id) }
                        .firstOrNull()
                    if (nextAlive != null) {
                        persistence.addDeadPilot(nextAlive.id)
                        val shipId = StoryStateManager.getShipForPilot(nextAlive.id)
                        if (shipId != null) persistence.addDeadShip(shipId)
                    }
                    updateDebugStoryInfo()
                }
                result == "KILL_ALL" -> {
                    val persistence = PersistenceManager(context)
                    if (StoryStateManager.stage(persistence) == StoryStage.NORMAL) {
                        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
                    }
                    for (pilot in PilotDefinitions.pilots) {
                        if (pilot.id == "pilot_astro") continue
                        persistence.addDeadPilot(pilot.id)
                        val shipId = StoryStateManager.getShipForPilot(pilot.id)
                        if (shipId != null) persistence.addDeadShip(shipId)
                    }
                    if (StoryStateManager.shouldUnlockCrystal(persistence)) {
                        persistence.setCrystalUnlocked(true)
                    }
                    updateDebugStoryInfo()
                }
                result == "BUY_CRYSTAL" -> {
                    val persistence = PersistenceManager(context)
                    if (StoryStateManager.stage(persistence) == StoryStage.NORMAL) {
                        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
                    }
                    persistence.setCrystalUnlocked(true)
                    persistence.setCrystalPurchased(true)
                    updateDebugStoryInfo()
                }
                result == "RESET_STORY" -> {
                    val persistence = PersistenceManager(context)
                    persistence.setStoryStageCode(StoryStage.NORMAL.code)
                    persistence.setStoryLoop(1)
                    persistence.setCrystalUnlocked(false)
                    persistence.setCrystalPurchased(false)
                    persistence.clearDeadPilotsAndShips()
                    persistence.clearCrystalBroken()
                    updateDebugStoryInfo()
                }
                result == "GRANT_BANDANAS" -> {
                    val persistence = PersistenceManager(context)
                    for (pilot in PilotDefinitions.pilots) persistence.addBandana(pilot.id)
                    persistence.setAwaitingConvergence(true)
                    updateDebugStoryInfo()
                }
                result == "CLEAR_BANDANAS" -> {
                    val persistence = PersistenceManager(context)
                    persistence.clearAllBandanas()
                    persistence.clearPendingBandanaPilot()
                    persistence.setAwaitingConvergence(false)
                    persistence.setCrystalReleased(false)
                    updateDebugStoryInfo()
                }
                result == "CLEAR_TELEMETRY" -> {
                    telemetryManager.clearLog()
                }
                result == "UNBRICK" -> {
                    val persistence = PersistenceManager(context)
                    if (persistence.isCrystalBroken()) {
                        persistence.clearCrystalBroken()
                        updateDebugStoryInfo()
                    }
                }
                result == "CRYSTAL_OPENING" -> {
                    state.debugMenuOpen = false
                    state.survivalTime = 0f
                    state.astroLoopMode = true
                    state.phase = GamePhase.PLAYING
                    startReckoningOpening()
                }
                result == "CRYSTAL_FIGHT" -> {
                    state.debugMenuOpen = false
                    state.survivalTime = 0f
                    state.astroLoopMode = true
                    state.phase = GamePhase.PLAYING
                    startReckoningFight()
                }
                result == "CRYSTAL_RELEASE" -> {
                    state.debugMenuOpen = false
                    state.survivalTime = 0f
                    state.astroLoopMode = true
                    state.phase = GamePhase.PLAYING
                    startReckoningFight()
                    onReckoningSurvived()   // skip 90s fight, jump straight to ghost-ship lance
                }
                result == "PLAY_DESERT" -> {
                    state.debugMenuOpen = false
                    initializeDesert()
                    state.phase = GamePhase.DESERT
                }
                result == "PLAY_DESERT_P2" -> {
                    state.debugMenuOpen = false
                    initializeDesert()
                    state.desertTimer = 120f
                    state.desertPhase = 1
                    state.phase = GamePhase.DESERT
                }
                result == "DESERT_CRYSTAL" -> {
                    state.debugMenuOpen = false
                    initializeDesert()
                    state.phase = GamePhase.DESERT
                    state.desertPhase = 3
                    state.desertCrystalPhase = 1
                }
                result == "TOGGLE_ASTRO_LOOP" -> {
                    val persistence = PersistenceManager(context)
                    if (StoryStateManager.isAstroLoop(persistence)) {
                        persistence.setStoryStageCode(StoryStage.NORMAL.code)
                    } else {
                        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
                    }
                    state.astroLoopMode = StoryStateManager.isAstroLoop(persistence)
                    updateDebugStoryInfo()
                }
                result == "RECKONING_ROUNDS_INC" -> {
                    val p = PersistenceManager(context)
                    p.setReckoningRounds((p.getReckoningRounds() + 1) % 25)
                    updateDebugStoryInfo()
                }
                result == "SET_DESERT_FLAGS" -> {
                    val persistence = PersistenceManager(context)
                    persistence.setDesertCompleted()
                    persistence.setDesertGoodEnding()
                    updateDebugStoryInfo()
                }
                result == "CLR_DESERT" -> {
                    val persistence = PersistenceManager(context)
                    persistence.clearDesertCompleted()
                    persistence.clearDesertGoodEnding()
                    updateDebugStoryInfo()
                }
                result == "SET_LOOP_1" -> {
                    val persistence = PersistenceManager(context)
                    persistence.setStoryLoop(1)
                    state.storyLoop = 1
                    updateDebugStoryInfo()
                }
                result == "SET_LOOP_2" -> {
                    val persistence = PersistenceManager(context)
                    persistence.setStoryLoop(2)
                    state.storyLoop = 2
                    updateDebugStoryInfo()
                }
                result == "SET_LOOP_3" -> {
                    val persistence = PersistenceManager(context)
                    persistence.setStoryLoop(3)
                    state.storyLoop = 3
                    updateDebugStoryInfo()
                }
                result?.startsWith("TUNING_SET:") == true -> {
                    val setName = result.substringAfter("TUNING_SET:")
                    debugMenuRenderer.tuningPreviewSet = setName
                    if (debugMenuRenderer.tuningBGMPlaying) {
                        SoundManager.stopDebugBGM()
                        SoundManager.playDebugBGM(context, setName)
                    }
                }
                result == "TUNING_BGM_TOGGLE" -> {
                    if (debugMenuRenderer.tuningBGMPlaying) {
                        SoundManager.stopDebugBGM()
                        SoundManager.clearTuningAutoPlay()
                        debugMenuRenderer.tuningBGMPlaying = false
                    } else {
                        SoundManager.playDebugBGM(context, debugMenuRenderer.tuningPreviewSet)
                        debugMenuRenderer.tuningBGMPlaying = true
                    }
                }
                result?.startsWith("TUNING_VOL:") == true -> {
                    val parts = result.substringAfter("TUNING_VOL:").split(":")
                    val weaponIndex = parts[0].toInt()
                    val buttonIndex = parts[1].toInt()
                    val weaponId = debugMenuRenderer.tuningWeapons[weaponIndex]
                    val beatSel = debugMenuRenderer.tuningBeatSelections[weaponId] ?: 5
                    // Play preview at selected volume
                    SoundManager.playSFX("sfx_weapon_$weaponId", SoundManager.getTuningVolume(buttonIndex), 1.0f, isSoundboard = true)
                    // Toggle/update auto-play if BGM is playing
                    if (debugMenuRenderer.tuningBGMPlaying) {
                        if (SoundManager.tuningAutoPlay.containsKey(weaponId)) {
                            SoundManager.updateTuningSettings(weaponId, buttonIndex, beatSel)
                        } else {
                            SoundManager.toggleTuningAutoPlay(weaponId, buttonIndex, beatSel)
                        }
                    }
                }
                result?.startsWith("TUNING_BEAT:") == true -> {
                    val parts = result.substringAfter("TUNING_BEAT:").split(":")
                    val weaponIndex = parts[0].toInt()
                    val buttonIndex = parts[1].toInt()
                    val weaponId = debugMenuRenderer.tuningWeapons[weaponIndex]
                    val volSel = debugMenuRenderer.tuningVolSelections[weaponId] ?: 5
                    // Toggle/update auto-play if BGM is playing
                    if (debugMenuRenderer.tuningBGMPlaying) {
                        if (SoundManager.tuningAutoPlay.containsKey(weaponId)) {
                            SoundManager.updateTuningSettings(weaponId, volSel, buttonIndex)
                        } else {
                            SoundManager.toggleTuningAutoPlay(weaponId, volSel, buttonIndex)
                        }
                    }
                }
            }
            return true
        }

        // Block all touch during retreat auto-pilot and fade — except while paused,
        // where the tap must reach the TouchController to unpause (see retreatBlocksTouch)
        if (retreatBlocksTouch(state.isPaused, state.retreatPhase)) return true

        // Block all touch during death transitions and heart-to-heart
        if (state.phase == GamePhase.DEATH_PLAY_OUT) return true
        if (state.phase == GamePhase.CRYSTAL_DEATH) return true
        if (state.phase == GamePhase.GAME_OVER) return true
        if (state.phase == GamePhase.GAME_BRICKED) {
            if (event.action == MotionEvent.ACTION_UP && brickScreenTimer >= 7.5f && !brickStatsRevealed) {
                brickStatsRevealed = true
                brickStatsTimer = 0f
                // Cache stats once
                val persistence = PersistenceManager(context)
                cachedBrickPlaytime = highScoreManager.getTotalPlayTime()
                cachedBrickKills = persistence.getTotalKills()
                cachedBrickDeaths = persistence.getTotalDeaths()
                cachedBrickYenEarned = persistence.getTotalYenEarned()
                cachedBrickEvolutions = persistence.getDiscoveredEvolutions().size
                cachedBrickCasinoSpins = persistence.getTotalCasinoSpins()
                val hours = (cachedBrickPlaytime / 3600).toInt()
                val mins = ((cachedBrickPlaytime % 3600) / 60).toInt()
                cachedBrickStatLines = listOf(
                    "Time Played" to "${hours}h ${mins}m",
                    "Kills" to "$cachedBrickKills",
                    "Deaths" to "$cachedBrickDeaths",
                    "Yen Earned" to "$cachedBrickYenEarned",
                    "Evolutions" to "$cachedBrickEvolutions/12",
                    "Casino Spins" to "$cachedBrickCasinoSpins"
                )
            }
            return true
        }
        if (state.phase == GamePhase.DESERT) {
            // Desert scene: touch resets no-input timer and drives movement
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                state.desertNoInputTimer = 0f
            }
            // Pause/debug handled by general handler below (same as PLAYING phase)
        }
        if (state.phase == GamePhase.HEART_TO_HEART) {
            return true
        }
        // Block touch during fleet formation cutscene (starts at FLEET_CHATTER)
        if (!state.isPaused && (state.bossFightPhase == PHASE_FLEET_CHATTER || state.bossFightPhase == PHASE_FORMATION || state.bossFightPhase == PHASE_SHIELD_ASSAULT || state.bossFightPhase == PHASE_TB26_RAM || state.bossFightPhase == PHASE_POST_VICTORY)) return true
        if (!state.isPaused && (state.bossFightPhase == PHASE_OTHER_FLEET || state.bossFightPhase == PHASE_OTHER_FORMATION || state.bossFightPhase == PHASE_OTHER_SHIELD_ASSAULT || state.bossFightPhase == PHASE_OTHER_TB26_RAM || state.bossFightPhase == PHASE_OTHER_DYING || state.bossFightPhase == PHASE_HEART_TRANSITION)) return true

        // Seed tap-tracking on every finger-down (used by double-tap detector below)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            tapDownX = event.x / renderScale
            tapDownY = event.y / renderScale
            tapDownTime = System.currentTimeMillis()
        }

        // Track press-and-hold for debug menu while paused.
        // Debug-only: the trigger is never armed in release builds, so the menu
        // (all of DebugMenuRenderer etc. still compiled in) is unreachable there.
        if (BuildConfig.DEBUG && state.isPaused) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pauseDebugHoldActive = true
                    pauseDebugHoldTimer = 0f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    pauseDebugHoldActive = false
                    pauseDebugHoldTimer = 0f
                }
            }
        }

        // Double-tap detection — opens pause menu
        // Only counts genuine taps: small movement + short hold. Ignores joystick releases.
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val currentTime = System.currentTimeMillis()
            val dx = event.x / renderScale - tapDownX
            val dy = event.y / renderScale - tapDownY
            val moved = (dx * dx + dy * dy) > GameConfig.JOYSTICK_DEAD_ZONE * GameConfig.JOYSTICK_DEAD_ZONE
            val held = (currentTime - tapDownTime) > tapHoldThreshold

            if (!moved && !held) {
                if (currentTime - lastTapTime < doubleTapThreshold) {
                    if (!state.isPaused && !state.debugMenuOpen && (state.phase == GamePhase.PLAYING || state.phase == GamePhase.DESERT)) {
                        pause()
                        touchController.consumeTap()  // drain while thread is stopped — no race possible
                        resume()
                        lastTapTime = 0L
                        return true
                    }
                }
                lastTapTime = currentTime
            }
            // moved || held → joystick release or long-press — not a tap, don't touch lastTapTime
        }

        return touchController.handleTouchEvent(event)
    }

    private fun updateDebugStoryInfo() {
        val persistence = PersistenceManager(context)
        state.debugStoryPhase = persistence.getStoryStageCode()
        state.debugDeadPilotCount = persistence.getDeadPilots().size
        state.debugCrystalUnlocked = persistence.isCrystalUnlocked()
        state.debugArcCompleted = StoryStateManager.hasLoopedBefore(persistence)
        state.debugCrystalBroken = persistence.isCrystalBroken()
        state.debugDesertCompleted = persistence.isDesertCompleted()
        state.debugDesertGoodEnding = persistence.hasDesertGoodEnding()
        state.debugStoryLoop = persistence.getStoryLoop()
        state.debugAstroLoopMode = StoryStateManager.isAstroLoop(persistence)
        state.debugReckoningRounds = persistence.getReckoningRounds()
    }

    /** The crystal pause overlay is story-gated: normal runs always, corruption only
     *  when flying as Astro (he carries the crystal), astro loop never — the other
     *  cases get the plain PAUSED overlay in renderPaused(). */
    private fun pauseUsesCrystal(): Boolean = !state.astroLoopMode && !isNonAstroCorruptionRun

    fun pause() {
        // Stop thread FIRST — prevents game thread from consuming hasTap and calling
        // dissolve() in the window between state.isPaused=true and the thread dying.
        // join() MUST stay OUTSIDE the holder lock below: the render thread needs that
        // same monitor to finish its in-flight frame, so holding it here would deadlock.
        gameThread?.pause()
        // gameThread.pause() joins with a 1s timeout, which can expire during heavy
        // scenes or surface teardown and leave the render thread briefly alive. The
        // pause-screen setup mutates state the render thread reads — notably
        // crystalRenderer.activatePause(), which rebuilds the shared `shards` list that
        // render()/update() iterate. Hold the render monitor so a still-running frame
        // can't observe a half-rebuilt list. Safe: nothing in here joins the thread.
        synchronized(holder) {
            if (state.phase == GamePhase.PLAYING) {
                state.isPaused = true
                if (pauseUsesCrystal()) crystalRenderer.activatePause(screenWidth, screenHeight)
                SoundManager.pause()
                SoundManager.beatClock.pause(System.currentTimeMillis())
            } else if (state.phase == GamePhase.DESERT) {
                state.isPaused = true
                SoundManager.pause()
            }
        }
    }

    fun resume() {
        // Restart thread if surface exists but thread was stopped (e.g., multitasking)
        if (holder.surface.isValid && (gameThread == null || !gameThread!!.isAlive)) {
            gameThread = GameThread(holder, this)
            gameThread?.setRunning(true)
            gameThread?.start()
        }
    }

    private fun updatePaused(deltaTime: Float) {
        // 5-second hold opens debug menu
        if (pauseDebugHoldActive) {
            pauseDebugHoldTimer += deltaTime
            if (pauseDebugHoldTimer >= 5f) {
                pauseDebugHoldActive = false
                pauseDebugHoldTimer = 0f
                state.isPaused = false
                state.debugMenuOpen = true
                lastDebugMenuPage = state.debugMenuPage
                updateDebugStoryInfo()
                if (state.debugMenuPage == 4) SoundManager.muteBGMForDebug()
                return
            }
        }

        if (crystalRenderer.isActive) {
            crystalRenderer.update(deltaTime)

            if (touchController.consumeTap()) {
                crystalRenderer.dissolve()
            }

            // Resume when dissolve completes (crystal deactivates itself)
            if (!crystalRenderer.isActive) {
                state.isPaused = false
                pauseDebugHoldActive = false
                pauseDebugHoldTimer = 0f
                SoundManager.resume()
                SoundManager.beatClock.resumeFromPause(System.currentTimeMillis())
            }
        } else {
            // Crystal not active (e.g., surface recreated) — tap to resume directly
            if (touchController.consumeTap()) {
                state.isPaused = false
                pauseDebugHoldActive = false
                pauseDebugHoldTimer = 0f
                SoundManager.resume()
                SoundManager.beatClock.resumeFromPause(System.currentTimeMillis())
            }
        }
    }

    private fun renderPaused(canvas: Canvas) {
        if (!pauseUsesCrystal()) {
            val overlayPaint = Paint().apply {
                color = android.graphics.Color.argb(180, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlayPaint)
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = FontManager.getDisplayBold()
                isAntiAlias = true
            }
            canvas.drawText("PAUSED", screenWidth / 2f, screenHeight * CrystalRenderer.PAUSE_TEXT_THRESHOLD, textPaint)
        } else {
            crystalRenderer.render(canvas, screenWidth, screenHeight)
        }
    }
}

/** Retreat auto-pilot (retreatPhase >= 2) swallows gameplay touches — but never while
 *  paused: the pause screen resumes via TouchController.consumeTap(), so a swallowed
 *  tap strands the run on PAUSED (app-switch during the emergency-shield fly-off). */
internal fun retreatBlocksTouch(isPaused: Boolean, retreatPhase: Int): Boolean =
    !isPaused && retreatPhase >= 2
