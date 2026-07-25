package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.core.SoundManager
import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.entity.*
import com.astroloop.game.util.Vector2
import com.astroloop.game.weapon.weapons.EnergySaw
import com.astroloop.game.weapon.weapons.IonOrbiters
import kotlin.math.*
import kotlin.random.Random

data class FleetShip(
    val shipId: String,
    val pilotId: String,
    val weaponId: String,
    val color: Int,
    val pilotColor: Int,
    var position: Vector2 = Vector2(0f, 0f),
    var targetPosition: Vector2 = Vector2(0f, 0f),
    var rotation: Float = 0f,
    var isActive: Boolean = false,
    var fireTimer: Float = 0f,
    var fireCooldown: Float = 1.5f,
    var preferredOrbitRadius: Float = 350f,
    var ring: Int = 0,       // 0=outer, 1=inner
    var ringIndex: Int = 0,  // position within ring
    var sawWeapon: EnergySaw? = null,
    var orbiterWeapon: IonOrbiters? = null,
    var velocity: Vector2 = Vector2(0f, 0f),
    var empDisorientTimer: Float = 0f,   // > 0 → tumbling, no firing
    var empRecoveryTimer: Float = 0f    // > 0 → smooth lerp back to orbit
)

class FleetSystem(
    private val boss: Boss,
    private val ship: Ship,
    private val projectilePool: EntityPool<Projectile>,
    private val visualEffects: VisualEffectManager
) {
    companion object {
        const val OUTER_RADIUS = 400f
        const val INNER_RADIUS = 140f
        const val ORBIT_SPEED = 0.3f  // rad/s

        private const val RING_EXPANSION_DURATION = 3f
        private const val OUTER_RADIUS_TARGET = 600f
        private const val INNER_RADIUS_TARGET = 250f

        // Ring assignments by ship ID — grouped by weapon range
        val OUTER_SHIPS = listOf(
            "ship_blue",    // Scout — pulse_cannon
            "ship_green",   // Tracer — homing_missiles
            "ship_lime",    // Devastator — flak_cannon
            "ship_coral",   // Tempest — solar_storm
            "ship_indigo",  // Dreadnought — cluster_bomb
            "ship_purple",  // Hedgehog — needle_gun
            "ship_white"    // Specter — railgun
        )
        val INNER_SHIPS = listOf(
            "ship_magenta", // Ripper — energy_saw
            "ship_red",     // Nova — nova_blast
            "ship_orange",  // Shrapnel — scatter_shot
            "ship_yellow",  // Trap — space_mines
            "ship_cyan"     // Sentinel — ion_orbiters
        )

        /** 0 = outer ring (long range), 1 = inner ring (close range). Defaults to outer. */
        fun ringForShip(shipId: String): Int = if (INNER_SHIPS.contains(shipId)) 1 else 0

        /** EMP knockback impulse magnitude (px/s), matching applyEmpScatter's per-ship shove. */
        const val EMP_SCATTER_IMPULSE = 200f

        /** Margin (px) past the viewport edge where TB-26 spawns before flying in. */
        const val FLEET_EDGE_MARGIN = 120f

        /**
         * Distance from the (camera-centered) player to spawn TB-26 so it starts just
         * outside the visible viewport on ANY resolution/aspect — including unfolded
         * foldables. Uses the viewport half-diagonal (the largest on-screen reach) plus
         * a margin. Camera is strict 1:1, so world units == screen pixels.
         */
        fun tb26SpawnDistance(viewW: Float, viewH: Float): Float =
            0.5f * sqrt(viewW * viewW + viewH * viewH) + FLEET_EDGE_MARGIN

        // Legacy solo-return approach bearing: TB-26 swoops in from the player's
        // forward-right (the pre-fix hardcoded (+700, -200) offset, kept as a bearing).
        private const val TB26_RETURN_BEARING_X = 700f
        private const val TB26_RETURN_BEARING_Y = -200f

        /** Solo-return spawn offset from the player: the legacy approach bearing,
         *  normalized, pushed out to the viewport-relative spawn distance so TB-26
         *  never pops in on-screen. */
        fun tb26ReturnSpawnOffset(viewW: Float, viewH: Float): Pair<Float, Float> {
            // Double math so component rounding stays within a sub-pixel of the distance.
            val dist = tb26SpawnDistance(viewW, viewH).toDouble()
            val bx = TB26_RETURN_BEARING_X.toDouble()
            val by = TB26_RETURN_BEARING_Y.toDouble()
            val len = sqrt(bx * bx + by * by)
            return Pair((bx / len * dist).toFloat(), (by / len * dist).toFloat())
        }

        /** TB-26 warp-in approach speed (px/s) — slower than fleet ships so the fly-in reads. */
        const val TB26_WARP_IN_SPEED = 450f

        /** Engine-restart sputter length (s) before the catch. */
        const val ENGINE_SPUTTER_DURATION = 0.6f
        /** Engine-restart launch ramp length (s) after the catch. */
        const val ENGINE_RAMP_DURATION = 1.5f
        /** Pause between starting the engine restart and the fleet warping in.
         *  Covers the full sputter + ramp (2.1s) plus a breath of full thrust, so the
         *  restart reads as its own beat in both perspectives. */
        const val FLEET_WARP_BEAT = 4f

        /**
         * Autopilot move-speed multiplier during an engine restart: 0 while sputtering,
         * eases linearly 0→1 over the ramp, then holds at full. [t] is seconds since the
         * restart began.
         */
        fun engineRestartSpeedScale(t: Float): Float = when {
            t < ENGINE_SPUTTER_DURATION -> 0f
            t < ENGINE_SPUTTER_DURATION + ENGINE_RAMP_DURATION ->
                (t - ENGINE_SPUTTER_DURATION) / ENGINE_RAMP_DURATION
            else -> 1f
        }

        /**
         * Point on the shield ring (radius [ringRadius]) where a shot from
         * ([fromX],[fromY]) toward the boss at ([bossX],[bossY]) crosses it, plus the
         * outward unit normal. Returns [x, y, nx, ny]. Safe when shooter is on the boss.
         */
        fun shieldRingHit(fromX: Float, fromY: Float, bossX: Float, bossY: Float, ringRadius: Float): FloatArray {
            val dx = fromX - bossX
            val dy = fromY - bossY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            val nx = dx / dist
            val ny = dy / dist
            return floatArrayOf(bossX + nx * ringRadius, bossY + ny * ringRadius, nx, ny)
        }

        /**
         * EMP knockback: shove [target] away from ([fromX],[fromY]) and add a random
         * rotation tumble. Same treatment fleet ships get in applyEmpScatter(); recovery (if
         * any) is the caller's responsibility. Safe when target is on the source.
         */
        fun scatterEntity(target: Entity, fromX: Float, fromY: Float) {
            val dx = target.position.x - fromX
            val dy = target.position.y - fromY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
            target.velocity.x += (dx / dist) * EMP_SCATTER_IMPULSE
            target.velocity.y += (dy / dist) * EMP_SCATTER_IMPULSE
            val tumble = (if (Random.nextBoolean()) 1f else -1f) * (1.5f + Random.nextFloat() * 1.5f)
            target.rotation += tumble
        }
    }

    // Zero-damage neutral state for fleet weapon instances — orbiters are visual-only
    private val fleetNeutralState = GameState.createNeutral().also { it.damageMultiplier = 0f }

    // Wraps a FleetShip's position as a Firer so weapon.fire() can be called
    private fun firerFor(fs: FleetShip) = object : com.astroloop.game.entity.Firer {
        override val position get() = fs.position
        override val velocity = com.astroloop.game.util.Vector2(0f, 0f)
        override val rotation get() = fs.rotation
        override val radius = com.astroloop.game.core.GameConfig.SHIP_BASE_SIZE
        override val isEnemyFirer = false
    }

    val fleetShips = mutableListOf<FleetShip>()
    var isActive = false
    var arrivalTimer = 0f
    var arrivalPhase = 0  // 0=not arrived, 1=warping in, 2=shield assault, 3=TB-26 ram, 4=attacking

    val bossPosition: Vector2 get() = boss.position

    var tb26Active = false
    var tb26Position = Vector2(0f, 0f)
    var tb26Velocity = Vector2(0f, 0f)
    var tb26Rotation = 0f
    var tb26Rammed = false
    var tb26OrbitTimer = 0f
    var tb26ChargeProgress: Float = 0f   // 0f=not charging, 1f=fully charged
    var tb26Charging: Boolean = false
    private val TB26_CHARGE_DURATION = 4.5f
    private val TB26_RAM_TURN_RATE = 5f

    var playerRing: Int = 0  // 0=outer, 1=inner — set at arrive() based on player's ship
    var playerRingPosition = Vector2(0f, 0f)
    var playerEmpFrozen: Boolean = false
    var tb26OrbitTarget: Vector2? = null  // null = orbit ship; set to Past Astro in corruption run
    var formationActive = false

    var ringExpanding = false
        private set
    private var ringExpansionTimer = 0f

    private var weaponFading = false
    var fleetWeaponAlpha = 1f
        private set

    // TB-26 flyout: drone flies away from boss before fleet arrives
    private var tb26FlyingOut = false
    private var tb26FlyOutDirX = 0f
    private var tb26FlyOutDirY = 0f

    var tb26Returning = false

    fun startRingExpansion() {
        ringExpanding = true
        ringExpansionTimer = 0f
    }

    fun startWeaponFade() {
        weaponFading = true
        // Clear orbiters via their weapon instances — targets fleet ships only, not player
        for (fs in fleetShips) {
            fs.orbiterWeapon?.clearOrbiters()
        }
    }

    fun startTb26Charge() {
        tb26Charging = true
        tb26ChargeProgress = 0f
    }

    private fun getCurrentRadius(ring: Int): Float {
        // Early return only when expansion has never started — once started, hold at target after completion
        if (!ringExpanding && ringExpansionTimer == 0f) return when (ring) {
            0 -> OUTER_RADIUS; else -> INNER_RADIUS
        }
        val t = (ringExpansionTimer / RING_EXPANSION_DURATION).coerceIn(0f, 1f)
        val eased = 1f - (1f - t) * (1f - t)
        return when (ring) {
            0 -> OUTER_RADIUS + (OUTER_RADIUS_TARGET - OUTER_RADIUS) * eased
            else -> INNER_RADIUS + (INNER_RADIUS_TARGET - INNER_RADIUS) * eased
        }
    }

    fun arrive(state: GameState, playerShipId: String, viewW: Float, viewH: Float) {
        isActive = true
        arrivalTimer = 0f
        arrivalPhase = 1

        playerRing = ringForShip(playerShipId)

        fleetShips.clear()
        for ((shipId, pilotId) in StoryStateManager.FLEET_MAPPING) {
            if (shipId == playerShipId) continue
            val shipDef = ShipDefinitions.getShip(shipId) ?: continue
            val pilotDef = PilotDefinitions.getPilot(pilotId) ?: continue

            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val dist = 2000f
            val spawnX = boss.position.x + cos(angle) * dist
            val spawnY = boss.position.y + sin(angle) * dist

            // Defaults a ship in neither list to inner (1) — intentionally differs from ringForShip's outer default
            val ring = if (OUTER_SHIPS.contains(shipId)) 0 else 1
            val ringShips = if (ring == 0) OUTER_SHIPS else INNER_SHIPS
            // slot 0 in the player's ring is reserved for the player's position (angle = 0).
            val fleetShipsInRing = ringShips.filter { it != playerShipId }
            val ringIndex = if (ring == playerRing) fleetShipsInRing.indexOf(shipId) + 1 else ringShips.indexOf(shipId)

            val preferredOrbitRadius = if (ring == 0) OUTER_RADIUS else INNER_RADIUS

            val weaponCooldown = when (shipDef.startingWeaponId) {
                "pulse_cannon"    -> 0.5f
                "scatter_shot"    -> 1.0f
                "homing_missiles" -> 1.0f
                "needle_gun"      -> 0.25f
                "nova_blast"      -> 4.0f
                "flak_cannon"     -> 1.0f
                "solar_storm"     -> 2.0f
                "space_mines"     -> 2.0f
                "cluster_bomb"    -> 2.0f
                "railgun"         -> 1.5f
                else              -> 1.5f
            }

            fleetShips.add(FleetShip(
                shipId = shipId,
                pilotId = pilotId,
                weaponId = shipDef.startingWeaponId,
                color = shipDef.color,
                pilotColor = pilotDef.color,
                position = Vector2(spawnX, spawnY),
                isActive = true,
                fireCooldown = weaponCooldown,
                fireTimer = Random.nextFloat() * weaponCooldown,
                preferredOrbitRadius = preferredOrbitRadius,
                ring = ring,
                ringIndex = ringIndex
            ))
        }

        // Attach real weapon instances to inner ring ships for accurate visual rendering
        for (fs in fleetShips) {
            when (fs.weaponId) {
                "energy_saw" -> fs.sawWeapon = EnergySaw().also { it.level = 1 }
                "ion_orbiters" -> {
                    val weapon = IonOrbiters().also { it.level = 1 }
                    fs.orbiterWeapon = weapon
                    weapon.fire(firerFor(fs), fleetNeutralState, projectilePool, emptyList())
                    weapon.makeOrbitersVisible()
                }
            }
        }

        // Set player ring position immediately so auto-pilot has a valid target
        val initialPlayerAngle = 0f
        playerRingPosition.set(
            boss.position.x + cos(initialPlayerAngle) * getCurrentRadius(playerRing),
            boss.position.y + sin(initialPlayerAngle) * getCurrentRadius(playerRing)
        )

        tb26Active = true
        tb26Rammed = false
        tb26OrbitTimer = 0f
        // Spawn TB-26 behind whoever he's protecting, away from the boss — the player
        // in the normal run, Past Astro in the corruption mirror (there the player IS
        // the boss, so the old ship-based bearing degenerated to atan2(0,0)).
        val protectedPos = tb26OrbitTarget ?: ship.position
        val tb26Angle = atan2(
            protectedPos.y - boss.position.y,
            protectedPos.x - boss.position.x
        )
        val tb26SpawnDist = tb26SpawnDistance(viewW, viewH)
        tb26Position = Vector2(
            protectedPos.x + cos(tb26Angle) * tb26SpawnDist,
            protectedPos.y + sin(tb26Angle) * tb26SpawnDist
        )
    }

    /**
     * Start TB-26 flying away from the boss — called when drone is "sent for help"
     * before the fleet arrives. The drone visually flies away instead of vanishing.
     */
    fun startTb26Flyout(droneX: Float, droneY: Float, bossX: Float, bossY: Float) {
        tb26Position.set(droneX, droneY)
        val dx = droneX - bossX
        val dy = droneY - bossY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        tb26FlyOutDirX = dx / dist
        tb26FlyOutDirY = dy / dist
        tb26Rotation = atan2(tb26FlyOutDirY, tb26FlyOutDirX)
        tb26FlyingOut = true
        tb26Active = true
    }

    /** Fly TB-26 back in from off-screen toward whoever he's protecting, then circle
     *  them — the player by default (normal run), Past Astro in the corruption mirror. */
    fun startTb26Return(viewW: Float, viewH: Float, target: Vector2? = null) {
        val (dx, dy) = tb26ReturnSpawnOffset(viewW, viewH)
        tb26Position.set(ship.position.x + dx, ship.position.y + dy)
        tb26OrbitTarget = target   // null = orbit the player
        tb26Active = true
        tb26Returning = true
        tb26FlyingOut = false
    }

    fun update(state: GameState, deltaTime: Float) {
        // Update TB-26 flyout independently of fleet arrival
        if (tb26FlyingOut) {
            tb26Position.x += tb26FlyOutDirX * 500f * deltaTime
            tb26Position.y += tb26FlyOutDirY * 500f * deltaTime
            val distFromShip = sqrt(
                (tb26Position.x - ship.position.x).pow(2) +
                (tb26Position.y - ship.position.y).pow(2)
            )
            if (distFromShip > 2000f) {
                tb26FlyingOut = false
                tb26Active = false  // Now truly gone
            }
        }

        // TB-26 solo return + circle (before the fleet arrives)
        if (tb26Returning) {
            val returnTarget = tb26OrbitTarget ?: ship.position
            val dx = returnTarget.x - tb26Position.x
            val dy = returnTarget.y - tb26Position.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > 40f) {
                val step = (900f * deltaTime).coerceAtMost(dist)
                tb26Position.x += (dx / dist) * step
                tb26Position.y += (dy / dist) * step
                tb26Rotation = atan2(dy, dx)
            } else {
                tb26Returning = false
                tb26OrbitTimer = 0f
            }
        } else if (tb26Active && !tb26FlyingOut && arrivalPhase == 0) {
            updateTb26Orbit(deltaTime)
        }

        if (weaponFading && fleetWeaponAlpha > 0f) {
            fleetWeaponAlpha = (fleetWeaponAlpha - deltaTime).coerceAtLeast(0f)
        }

        if (!isActive) return
        arrivalTimer += deltaTime
        if (ringExpanding) {
            ringExpansionTimer += deltaTime
            if (ringExpansionTimer >= RING_EXPANSION_DURATION) ringExpanding = false
        }

        when (arrivalPhase) {
            1 -> {
                updateWarpIn(state, deltaTime)
                updateAllOrbiters(deltaTime)
            }
            2 -> {
                updateOrbiting(deltaTime)
                updateTb26Orbit(deltaTime)
            }
            3 -> { updateOrbiting(deltaTime); updateTb26Ram(deltaTime) }
            4 -> updateOrbiting(deltaTime)
        }

    }

    private fun updateWarpIn(state: GameState, deltaTime: Float) {
        var allArrived = true

        for (fs in fleetShips) {
            if (!fs.isActive) continue
            val ringRadius = fs.preferredOrbitRadius
            val ringShips = if (fs.ring == 0) OUTER_SHIPS else INNER_SHIPS
            val ringCount = ringShips.size
            val angle = (fs.ringIndex.toFloat() / ringCount) * 2f * PI.toFloat()

            fs.targetPosition.set(
                boss.position.x + cos(angle) * ringRadius,
                boss.position.y + sin(angle) * ringRadius
            )

            val dx = fs.targetPosition.x - fs.position.x
            val dy = fs.targetPosition.y - fs.position.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > 5f) {
                allArrived = false
                val step = (1200f * deltaTime).coerceAtMost(dist)
                fs.position.x += (dx / dist) * step
                fs.position.y += (dy / dist) * step
            }

            fs.rotation = atan2(
                boss.position.y - fs.position.y,
                boss.position.x - fs.position.x
            )
        }

        // Compute initial player ring position (index 0 in player's ring)
        val playerAngle = 0f
        playerRingPosition.set(
            boss.position.x + cos(playerAngle) * getCurrentRadius(playerRing),
            boss.position.y + sin(playerAngle) * getCurrentRadius(playerRing)
        )

        // TB-26 warp-in — now flies toward player position (not boss)
        val tb26TargetX = playerRingPosition.x
        val tb26TargetY = playerRingPosition.y
        val tb26dx = tb26TargetX - tb26Position.x
        val tb26dy = tb26TargetY - tb26Position.y
        val tb26dist = sqrt(tb26dx * tb26dx + tb26dy * tb26dy)
        if (tb26dist > 5f) {
            val tb26step = (TB26_WARP_IN_SPEED * deltaTime).coerceAtMost(tb26dist)
            tb26Position.x += (tb26dx / tb26dist) * tb26step
            tb26Position.y += (tb26dy / tb26dist) * tb26step
            tb26Rotation = atan2(tb26dy, tb26dx)
        }

        if (allArrived && arrivalTimer > 1.5f) {
            arrivalPhase = 2
            arrivalTimer = 0f
            formationActive = true
        }
    }

    fun updateTb26Orbit(deltaTime: Float) {
        if (tb26Charging) {
            tb26ChargeProgress = (tb26ChargeProgress + deltaTime / TB26_CHARGE_DURATION).coerceAtMost(1f)
        }

        tb26OrbitTimer += deltaTime
        val orbitRadius = 30f
        val orbitSpeed = lerp(2f, 10f, tb26ChargeProgress)
        val angle = tb26OrbitTimer * orbitSpeed
        val orbitCenter = tb26OrbitTarget ?: ship.position
        tb26Position.set(
            orbitCenter.x + cos(angle) * orbitRadius,
            orbitCenter.y + sin(angle) * orbitRadius
        )
        tb26Rotation = angle + PI.toFloat() / 2f
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    private fun updateTb26Ram(deltaTime: Float) {
        if (!tb26Rammed) {
            val dx = boss.position.x - tb26Position.x
            val dy = boss.position.y - tb26Position.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > boss.radius + 15f) {
                val desiredAngle = atan2(dy, dx)
                val angleDiff = normalizeAngle(desiredAngle - tb26Rotation)
                tb26Rotation += angleDiff.coerceIn(
                    -TB26_RAM_TURN_RATE * deltaTime,
                    TB26_RAM_TURN_RATE * deltaTime
                )
                val speed = 600f
                tb26Position.x += cos(tb26Rotation) * speed * deltaTime
                tb26Position.y += sin(tb26Rotation) * speed * deltaTime
            } else {
                tb26Rammed = true
                tb26Active = false
                tb26Charging = false
                visualEffects.addExplosion(tb26Position.x, tb26Position.y, 250f, 0xFFFFFFFF.toInt())
                SoundManager.playSFX("sfx_explosion", 1f)
                boss.health = 0f
                boss.isActive = false
                arrivalPhase = 4
                // Don't reset arrivalTimer — orbit angles depend on it, resetting snaps ships
            }
        }
    }

    private fun normalizeAngle(a: Float): Float {
        var r = a % (2f * PI.toFloat())
        if (r > PI.toFloat()) r -= 2f * PI.toFloat()
        if (r < -PI.toFloat()) r += 2f * PI.toFloat()
        return r
    }

    private fun updateOrbiting(deltaTime: Float) {
        for (fs in fleetShips) {
            if (!fs.isActive) continue

            val ringRadius = getCurrentRadius(fs.ring)
            val ringShips = if (fs.ring == 0) OUTER_SHIPS else INNER_SHIPS
            val ringCount = ringShips.size
            val direction = if (fs.ring == 0) 1f else -1f  // outer CW, inner CCW
            val angle = (fs.ringIndex.toFloat() / ringCount) * 2f * PI.toFloat() +
                arrivalTimer * ORBIT_SPEED * direction

            val orbitX = boss.position.x + cos(angle) * ringRadius
            val orbitY = boss.position.y + sin(angle) * ringRadius

            if (fs.empDisorientTimer > 0f) {
                // Permanently scattered — drift with velocity, no reorientation, no recovery
                fs.position.x += fs.velocity.x * deltaTime
                fs.position.y += fs.velocity.y * deltaTime
                fs.velocity.x *= (1f - 0.8f * deltaTime).coerceAtLeast(0f)
                fs.velocity.y *= (1f - 0.8f * deltaTime).coerceAtLeast(0f)
            } else if (fs.empRecoveryTimer > 0f) {
                // Smooth return to orbit — lerp toward formation position
                fs.empRecoveryTimer -= deltaTime
                val factor = (5f * deltaTime).coerceAtMost(1f)
                fs.position.x += (orbitX - fs.position.x) * factor
                fs.position.y += (orbitY - fs.position.y) * factor
                fs.velocity.x *= (1f - 5f * deltaTime).coerceAtLeast(0f)
                fs.velocity.y *= (1f - 5f * deltaTime).coerceAtLeast(0f)
                // Resume aiming at boss during recovery
                val targetRot = atan2(
                    boss.position.y - fs.position.y,
                    boss.position.x - fs.position.x
                )
                val diff = normalizeAngle(targetRot - fs.rotation)
                fs.rotation += diff * deltaTime * 2f
            } else {
                // Orbit-locked: snap to formation position
                fs.position.set(orbitX + fs.velocity.x * deltaTime, orbitY + fs.velocity.y * deltaTime)
                fs.velocity.x *= (1f - 5f * deltaTime).coerceAtLeast(0f)
                fs.velocity.y *= (1f - 5f * deltaTime).coerceAtLeast(0f)
                fs.rotation = atan2(
                    boss.position.y - fs.position.y,
                    boss.position.x - fs.position.x
                )
                fs.fireTimer -= deltaTime
                if (fs.fireTimer <= 0f) {
                    fireAtBoss(fs)
                    fs.fireTimer = fs.fireCooldown
                }
            }
        }

        // Update player ring position (index 0 in player's ring, direction matches ring)
        if (!playerEmpFrozen) {
            val playerDirection = if (playerRing == 0) 1f else -1f
            val playerAngle = arrivalTimer * ORBIT_SPEED * playerDirection
            playerRingPosition.set(
                boss.position.x + cos(playerAngle) * getCurrentRadius(playerRing),
                boss.position.y + sin(playerAngle) * getCurrentRadius(playerRing)
            )
        }

        updateAllOrbiters(deltaTime)
    }

    private fun fireAtBoss(fs: FleetShip) {
        fireWeaponAtBoss(fs.position.x, fs.position.y, fs.weaponId, fs.color)
    }

    /**
     * Fires [weaponId] from ([originX], [originY]) at the boss using the fleet's
     * per-weapon projectile patterns. Reused by Past Astro during the charge.
     */
    fun fireWeaponAtBoss(originX: Float, originY: Float, weaponId: String, color: Int) {
        if (!boss.isActive) return
        val dx = boss.position.x - originX
        val dy = boss.position.y - originY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val dirX = dx / dist
        val dirY = dy / dist
        val spawnX = originX + dirX * 25f
        val spawnY = originY + dirY * 25f

        when (weaponId) {
            "homing_missiles" -> {
                // Slower projectile with slight random spread to look homing-ish
                val spread = (Random.nextFloat() - 0.5f) * 0.2f
                val angle = atan2(dirY, dirX) + spread
                val p = projectilePool.obtain()
                p.initialize(spawnX, spawnY, cos(angle) * 350f, sin(angle) * 350f, ProjectileType.MISSILE, 30f, 4f)
                p.isEnemyProjectile = false
                p.color = color
                p.target = boss
                p.homingStrength = 3.5f
            }
            "scatter_shot" -> {
                val baseAngle = atan2(dirY, dirX)
                val spreadAngle = PI.toFloat() / 3f
                repeat(5) {
                    val angle = baseAngle + (Random.nextFloat() - 0.5f) * spreadAngle
                    val p = projectilePool.obtain()
                    p.initialize(spawnX, spawnY, cos(angle) * 500f, sin(angle) * 500f, ProjectileType.BULLET, 20f, 1.5f)
                    p.isEnemyProjectile = false
                    p.color = color
                    p.radius = 3f
                }
            }
            "energy_saw", "ion_orbiters" -> {
                // Close range — no projectile, the visual of being close is enough
                return
            }
            "needle_gun" -> {
                // Rapid small shots
                for (i in 0..2) {
                    val offset = (i - 1) * 0.05f
                    val angle = atan2(dirY, dirX) + offset
                    val p = projectilePool.obtain()
                    p.initialize(spawnX, spawnY, cos(angle) * 800f, sin(angle) * 800f, ProjectileType.BULLET, 15f, 2f)
                    p.isEnemyProjectile = false
                    p.color = color
                    p.length = 14f
                    p.piercing = true
                    p.maxPierces = 3
                }
            }
            "nova_blast" -> {
                repeat(8) { i ->
                    val angle = i * 2f * PI.toFloat() / 8f
                    val p = projectilePool.obtain()
                    p.initialize(spawnX, spawnY, cos(angle) * 540f, sin(angle) * 540f, ProjectileType.PLASMA, 0f, 0.3f)
                    p.isEnemyProjectile = false
                    p.color = color
                    p.radius = 60f
                    p.piercing = true
                    p.maxPierces = 1000
                }
            }
            "flak_cannon" -> {
                val p = projectilePool.obtain()
                p.initialize(spawnX, spawnY, dirX * 400f, dirY * 400f, ProjectileType.FLAK, 25f, 1.5f)
                p.isEnemyProjectile = false
                p.color = color
                p.radius = 6f
                p.explodeOnDeath = true
                p.explosionRadius = 60f
                p.explosionDamage = 15f
                p.proximityFuse = true
            }
            "solar_storm" -> {
                val p = projectilePool.obtain()
                p.initialize(boss.position.x, boss.position.y, 0f, 0f, ProjectileType.LIGHTNING, 0f, 0.4f)
                p.isEnemyProjectile = false
                p.color = color
                p.radius = 60f
                p.bounceCount = 99
            }
            "space_mines" -> {
                // Drop a slow-moving mine toward boss
                val p = projectilePool.obtain()
                p.initialize(spawnX, spawnY, dirX * 150f, dirY * 150f, ProjectileType.MINE, 35f, 4f)
                p.isEnemyProjectile = false
                p.color = color
                p.radius = 12f
                p.decelerationRate = 100f
                p.explodeOnDeath = true
                p.explosionRadius = 40f
            }
            "cluster_bomb" -> {
                // Slow bomb lobbed toward boss
                val p = projectilePool.obtain()
                p.initialize(spawnX, spawnY, dirX * 200f, dirY * 200f, ProjectileType.TORPEDO, 60f, 6f)
                p.isEnemyProjectile = false
                p.color = color
                p.radius = 12f
                p.explodeOnDeath = true
                p.explosionRadius = 80f
                p.explosionDamage = 18f
                p.bombletCount = 4
                p.bombletExplosionRadius = 48f
                p.bombletDamage = 8f
            }
            "railgun" -> {
                val p = projectilePool.obtain()
                p.initialize(spawnX, spawnY, dirX * 2000f, dirY * 2000f, ProjectileType.BULLET, 0f, 3f)
                p.isEnemyProjectile = false
                p.color = color
                p.length = 20f
                p.width = 11f
                p.piercing = true
                p.maxPierces = 2
            }
            else -> {
                // Default: pulse cannon style
                val p = projectilePool.obtain()
                p.initialize(spawnX, spawnY, dirX * 600f, dirY * 600f, ProjectileType.BULLET, 30f, 2f)
                p.isEnemyProjectile = false
                p.color = color
            }
        }

        // Shield reacts the instant the shot is fired (no projectile travel-time gap).
        if (boss.shielded) {
            val hit = shieldRingHit(originX, originY, boss.position.x, boss.position.y, Boss.FLEET_SHIELD_RING_RADIUS)
            boss.spawnShieldSparks(hit[0], hit[1], hit[2], hit[3], color)
        }

        // Play weapon sound for fleet ship
        val soundId = "sfx_weapon_${weaponId}"
        SoundManager.playSFX(soundId, SoundManager.getWeaponSfxVolume(weaponId))
    }

    private fun updateAllOrbiters(deltaTime: Float) {
        for (fs in fleetShips) {
            if (!fs.isActive) continue
            fs.orbiterWeapon?.updateOrbiters(fs.position, deltaTime)
        }
    }

    fun stopFiring() {
        for (fs in fleetShips) {
            fs.fireTimer = Float.MAX_VALUE
        }
    }

    fun applyEmpScatter() {
        for (fs in fleetShips) {
            if (!fs.isActive) continue
            val dx = fs.position.x - boss.position.x
            val dy = fs.position.y - boss.position.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
            fs.velocity.x += (dx / dist) * 200f
            fs.velocity.y += (dy / dist) * 200f
            val scatter = (if (Random.nextBoolean()) 1f else -1f) * (1.5f + Random.nextFloat() * 1.5f)
            fs.rotation += scatter
            fs.empDisorientTimer = Float.MAX_VALUE
            fs.empRecoveryTimer = 0f
        }
        playerEmpFrozen = true

        // Separation pass — push overlapping ships apart so they drift clear on the next frame
        val minSep = GameConfig.SHIP_BASE_SIZE * 2.4f  // ~60f
        for (i in fleetShips.indices) {
            val a = fleetShips[i]
            if (!a.isActive) continue
            for (j in i + 1 until fleetShips.size) {
                val b = fleetShips[j]
                if (!b.isActive) continue
                val dx = b.position.x - a.position.x
                val dy = b.position.y - a.position.y
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f)
                if (dist < minSep) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val push = (minSep - dist) * 9f
                    a.velocity.x -= nx * push
                    a.velocity.y -= ny * push
                    b.velocity.x += nx * push
                    b.velocity.y += ny * push
                }
            }
        }
    }

    fun applyShockwaveImpulse(bossX: Float, bossY: Float) {
        for (fs in fleetShips) {
            if (!fs.isActive) continue
            val dx = fs.position.x - bossX
            val dy = fs.position.y - bossY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
            val force = (40000f / dist).coerceAtMost(600f)
            fs.velocity.x += (dx / dist) * force
            fs.velocity.y += (dy / dist) * force
        }
    }

    fun reset() {
        for (fs in fleetShips) {
            fs.orbiterWeapon?.clearOrbiters()
        }
        fleetShips.clear()
        isActive = false
        arrivalTimer = 0f
        arrivalPhase = 0
        tb26Active = false
        tb26Rotation = 0f
        tb26Rammed = false
        tb26OrbitTimer = 0f
        tb26Charging = false
        tb26ChargeProgress = 0f
        tb26FlyingOut = false
        tb26FlyOutDirX = 0f
        tb26FlyOutDirY = 0f
        tb26Returning = false
        playerRing = 0
        playerRingPosition = Vector2(0f, 0f)
        formationActive = false
        ringExpanding = false
        ringExpansionTimer = 0f
        weaponFading = false
        fleetWeaponAlpha = 1f
        playerEmpFrozen = false
        tb26OrbitTarget = null
    }
}
