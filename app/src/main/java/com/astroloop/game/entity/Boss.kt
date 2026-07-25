package com.astroloop.game.entity

import com.astroloop.game.core.BossRush
import com.astroloop.game.util.Vector2
import kotlin.math.*
import kotlin.random.Random


/**
 * Ace pilot boss -- corrupted Specter with railgun and crystal powers.
 * Small, agile ship (similar size to player). Mirrors the player's
 * corruption run moveset: railgun, recall shots, afterimage dodge, regen.
 */
class Boss : Entity() {
    companion object {
        const val SPAWN_TIME = 600f  // 10 minutes
        const val BOSS_SIZE = 25f    // Matches GameConfig.SHIP_BASE_SIZE — mirror player ship size
        const val BASE_SPEED = 280f
        const val MAX_SPEED = 350f
        const val CENTER_APPROACH_SPEED = 450f  // death-retreat glide-to-centre speed (px/s)
        const val CENTER_ARRIVE_EPS = 4f        // snap-and-plant threshold (px)
        const val PREFERRED_DISTANCE = 300f
        const val CLOSE_DISTANCE = 200f
        // Reckoning hold-pattern (Touhou stable-emitter): chase hard only beyond the leash,
        // ease out of point-blank, otherwise drift slowly so the spiral geometry stays readable.
        const val HOLD_LEASH = 520f       // beyond this: close at 105% — flying away stays impossible
        const val HOLD_STANDOFF = 260f    // inside this: ease back out — bullets never spawn point-blank
        const val HOLD_DRIFT_SPEED = 70f  // in the band: slow orbit, no lunges, no strafe jitter
        const val RAIL_DAMAGE = 40f
        const val RAIL_SPEED = 800f
        const val RAIL_COOLDOWN = 1.8f
        const val RECALL_PAUSE_TIME = 1.0f
        const val AFTERIMAGE_DURATION = 0.3f
        const val AFTERIMAGE_DODGE_DISTANCE = 60f
        const val HEALTH_REGEN_RATE = 5f
        const val SHIELD_REGEN_RATE = 8f
        const val BOSS_MAX_HEALTH = 3000f
        const val BOSS_MAX_SHIELD = 200f
        const val SPEED_RAMP_DURATION = 60f
        const val CORRUPTION_COLOR = 0xFFAA2222.toInt()
        const val SHIELD_DEFLECT_RADIUS = 60f  // shots dissipate here — the energy aura ring in renderBoss (NOT the fleet barrier at BOSS_SIZE*1.8 = 45f)
        const val FLEET_SHIELD_RING_RADIUS = BOSS_SIZE * 1.8f  // 45f — the pulsing cyan barrier ring drawn while shielded
        const val SAW_SPARK_TOLERANCE = 10f  // extra reach so the inner-ring saw reliably grazes the shield ring
    }

    // AI state
    enum class BossAIState { STRAFING, CLOSING, RETREATING, CHARGING }
    var aiState = BossAIState.STRAFING
    var aiTimer = 0f
    var targetPlayer: Ship? = null
    var timeAlive = 0f

    // Movement
    var currentSpeed = BASE_SPEED
    var strafeDirection = 1f

    // Railgun
    var railCooldown = 0f
    var wantsToFire = false

    // Crystal powers
    var shieldTickTimer = 0f
    var afterimageActive = false
    var afterimageTimer = 0f
    var afterimageX = 0f
    var afterimageY = 0f
    var afterimageRotation = 0f

    // Shield (boss-specific, separate from Entity.health/maxHealth)
    var shield = BOSS_MAX_SHIELD
    var maxShield = BOSS_MAX_SHIELD

    // Invulnerability, regen, and stun
    var isInvulnerable = true
    var regenActive = true
    var isStunned = false

    // Death-retreat centring: glide to the frozen view's centre, then plant (stun) there.
    private var isCentering = false
    private var centerTargetX = 0f
    private var centerTargetY = 0f

    // EMP rush-in (normal run) — pursuit driven here, sequencing in GameSurfaceView
    var isRushing = false
    var rushBraking = false
    var rushSpeed = 0f
    var rushTimer = 0f
    val reentryBurn = ReentryBurn()

    // Visual
    var enginePulse = 0f

    // Fleet shield — boss is invulnerable until fleet is defeated
    var shielded: Boolean = false

    // Reckoning hold-pattern — replaces the STRAFING/CLOSING/CHARGING AI with a stable emitter
    var holdPattern = false

    // Shield deflection sparks — saw-hit style; updated independently of Boss.update()
    // so they still tick while the boss is stunned during the charge.
    val shieldSparks = mutableListOf<SawSpark>()

    fun spawnShieldSparks(impactX: Float, impactY: Float, awayX: Float, awayY: Float, color: Int) {
        val baseAngle = atan2(awayY, awayX)
        for (i in 0 until 5) {
            val a = baseAngle + (Random.nextFloat() - 0.5f) * PI.toFloat() * 0.6f
            val speed = 90f + Random.nextFloat() * 140f
            shieldSparks.add(SawSpark(
                x = impactX, y = impactY,
                vx = cos(a) * speed, vy = sin(a) * speed,
                lifetime = 0.3f, color = color
            ))
        }
    }

    fun updateShieldSparks(deltaTime: Float) {
        val it = shieldSparks.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.x += s.vx * deltaTime
            s.y += s.vy * deltaTime
            s.age += deltaTime
            if (s.age >= s.lifetime) it.remove()
        }
    }

    init {
        radius = BOSS_SIZE
        maxHealth = BOSS_MAX_HEALTH
        health = maxHealth
    }

    fun initialize(x: Float, y: Float, player: Ship) {
        position.set(x, y)
        velocity.zero()
        radius = BOSS_SIZE
        isActive = true
        targetPlayer = player
        timeAlive = 0f
        currentSpeed = BASE_SPEED
        aiState = BossAIState.STRAFING
        aiTimer = 0f
        strafeDirection = if (Random.nextBoolean()) 1f else -1f
        railCooldown = 2f
        wantsToFire = false
        afterimageActive = false
        afterimageTimer = 0f
        shieldTickTimer = 0f
        isInvulnerable = true
        isStunned = false
        isRushing = false
        rushBraking = false
        rushSpeed = 0f
        rushTimer = 0f
        reentryBurn.clear()
        holdPattern = false
        maxHealth = BOSS_MAX_HEALTH
        health = maxHealth
        shield = BOSS_MAX_SHIELD
        maxShield = BOSS_MAX_SHIELD
        regenActive = true
        enginePulse = 0f
        shielded = true
    }

    fun stun() {
        isStunned = true
        isRushing = false
        rushBraking = false
        velocity.set(0f, 0f)
        wantsToFire = false
    }

    /**
     * Reckoning death beat: glide toward ([tx],[ty]) — the frozen view's centre — then plant
     * (stun) on arrival. Rotation keeps tracking the fleeing player via [update]'s aim step.
     */
    fun startCenterApproach(tx: Float, ty: Float) {
        isCentering = true
        centerTargetX = tx
        centerTargetY = ty
        isRushing = false
        rushBraking = false
        wantsToFire = false
    }

    /** Ignite the EMP rush: straight pursuit at [speed], railgun quiet, burn on. */
    fun startRush(speed: Float) {
        isRushing = true
        rushBraking = false
        rushSpeed = speed
        rushTimer = 0f
        wantsToFire = false
    }

    /** Arrival: cut the burn and bleed speed off hard (GameSurfaceView times the window). */
    fun startRushBrake() {
        rushBraking = true
    }

    override fun update(deltaTime: Float) {
        if (!isActive) return
        val player = targetPlayer ?: return  // needed for rotation even when stunned

        // Always track player — rotation runs even during charge stun
        val toPlayer = Vector2(player.position.x - position.x, player.position.y - position.y)
        rotation = atan2(toPlayer.y, toPlayer.x)

        // Burn trail ages even while stunned — ghosts fade out after the rush ends
        reentryBurn.update(position.x, position.y, rotation, deltaTime,
            emitting = isRushing && !rushBraking)

        if (isStunned) return  // Boss is frozen — no AI, no movement, no firing

        // Death-retreat centring: glide straight to the frozen view's centre, then plant there.
        // Runs before the normal AI/rush logic and skips it entirely while active.
        if (isCentering) {
            wantsToFire = false
            val dx = centerTargetX - position.x
            val dy = centerTargetY - position.y
            val d = sqrt(dx * dx + dy * dy)
            if (d <= CENTER_ARRIVE_EPS) {
                position.x = centerTargetX
                position.y = centerTargetY
                isCentering = false
                stun()
            } else {
                val step = (CENTER_APPROACH_SPEED * deltaTime).coerceAtMost(d)
                position.x += dx / d * step
                position.y += dy / d * step
            }
            return
        }

        timeAlive += deltaTime
        enginePulse += deltaTime * 6f

        if (isRushing) {
            rushTimer += deltaTime
            wantsToFire = false
            if (rushBraking) {
                velocity.x *= (1f - 12f * deltaTime).coerceAtLeast(0f)
                velocity.y *= (1f - 12f * deltaTime).coerceAtLeast(0f)
            } else {
                val dx = player.position.x - position.x
                val dy = player.position.y - position.y
                val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val spd = rushSpeed * BossRush.easeIn(rushTimer)
                velocity.set(dx / d * spd, dy / d * spd)
            }
            position.x += velocity.x * deltaTime
            position.y += velocity.y * deltaTime
            clampMinDistance(player)
            return
        }

        // Reckoning hold-pattern: a Touhou stage boss adapted to free flight. The pattern is
        // the fight, not the chase — bullets already outrun the player (CrystalFightSystem
        // speed guard), so the chase exists only to keep the fight on screen. Leash chase
        // can't be outrun, the standoff keeps the emitter off the player's face, and the
        // in-band drift is slow enough that the spiral geometry stays readable.
        if (holdPattern) {
            wantsToFire = false
            val dx = player.position.x - position.x
            val dy = player.position.y - position.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val nx = dx / dist
            val ny = dy / dist
            val target = when {
                dist > HOLD_LEASH -> {
                    val s = player.speed * 1.05f
                    Vector2(nx * s, ny * s)
                }
                dist < HOLD_STANDOFF -> {
                    val s = player.speed * 0.4f
                    Vector2(-nx * s, -ny * s)
                }
                else -> Vector2(
                    -ny * strafeDirection * HOLD_DRIFT_SPEED,
                    nx * strafeDirection * HOLD_DRIFT_SPEED
                )
            }
            velocity.x += (target.x - velocity.x) * 3f * deltaTime
            velocity.y += (target.y - velocity.y) * 3f * deltaTime
            position.x += velocity.x * deltaTime
            position.y += velocity.y * deltaTime
            clampMinDistance(player)
            return
        }

        // Speed = 105% of player's effective max speed
        val playerMaxSpeed = targetPlayer?.speed ?: BASE_SPEED
        currentSpeed = playerMaxSpeed * 1.05f

        // Regen
        if (regenActive) {
            health = (health + HEALTH_REGEN_RATE * deltaTime).coerceAtMost(maxHealth)
            shield = (shield + SHIELD_REGEN_RATE * deltaTime).coerceAtMost(maxShield)
        }

        // Rail cooldown
        if (railCooldown > 0f) railCooldown -= deltaTime
        wantsToFire = railCooldown <= 0f

        // Afterimage decay
        if (afterimageActive) {
            afterimageTimer -= deltaTime
            if (afterimageTimer <= 0f) afterimageActive = false
        }

        // AI state machine
        aiTimer -= deltaTime
        updateAI(deltaTime, player)

        // Apply velocity (manual, do NOT call super.update to avoid double-move)
        position.x += velocity.x * deltaTime
        position.y += velocity.y * deltaTime

        clampMinDistance(player)
    }

    // Hard clamp: never get close enough for energy shield to hit player
    private fun clampMinDistance(player: Ship) {
        val dx = position.x - player.position.x
        val dy = position.y - player.position.y
        val toPlayerDist = sqrt(dx * dx + dy * dy)
        val minSafeDist = 95f  // shield radius (60) + ship radius (25) + buffer (10)
        if (toPlayerDist < minSafeDist) {
            val dist = toPlayerDist.coerceAtLeast(1f)
            position.x = player.position.x + dx / dist * minSafeDist
            position.y = player.position.y + dy / dist * minSafeDist
        }
    }

    private fun updateAI(deltaTime: Float, player: Ship) {
        val dx = player.position.x - position.x
        val dy = player.position.y - position.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val toPlayerNorm = Vector2(dx / dist, dy / dist)

        if (aiTimer <= 0f) {
            aiState = when {
                dist > PREFERRED_DISTANCE * 1.5f -> BossAIState.CLOSING
                dist < CLOSE_DISTANCE -> BossAIState.RETREATING
                Random.nextFloat() < 0.3f -> BossAIState.CHARGING
                else -> {
                    strafeDirection = if (Random.nextBoolean()) 1f else -1f
                    BossAIState.STRAFING
                }
            }
            aiTimer = when (aiState) {
                BossAIState.STRAFING -> 2f + Random.nextFloat() * 2f
                BossAIState.CLOSING -> 1.5f
                BossAIState.RETREATING -> 1f
                BossAIState.CHARGING -> 1.5f
            }
        }

        val targetVelocity = when (aiState) {
            BossAIState.STRAFING -> {
                val perp = Vector2(-toPlayerNorm.y * strafeDirection, toPlayerNorm.x * strafeDirection)
                val distCorrection = if (dist > PREFERRED_DISTANCE) 0.3f else if (dist < PREFERRED_DISTANCE * 0.7f) -0.3f else 0f
                Vector2(
                    (perp.x + toPlayerNorm.x * distCorrection) * currentSpeed,
                    (perp.y + toPlayerNorm.y * distCorrection) * currentSpeed
                )
            }
            BossAIState.CLOSING -> Vector2(toPlayerNorm.x * currentSpeed, toPlayerNorm.y * currentSpeed)
            BossAIState.RETREATING -> Vector2(-toPlayerNorm.x * currentSpeed * 0.8f, -toPlayerNorm.y * currentSpeed * 0.8f)
            BossAIState.CHARGING -> Vector2(toPlayerNorm.x * currentSpeed * 1.3f, toPlayerNorm.y * currentSpeed * 1.3f)
        }

        velocity.x += (targetVelocity.x - velocity.x) * 3f * deltaTime
        velocity.y += (targetVelocity.y - velocity.y) * 3f * deltaTime
    }

    fun getAimDirection(): Vector2 {
        val player = targetPlayer ?: return Vector2(cos(rotation), sin(rotation))
        val dx = player.position.x - position.x
        val dy = player.position.y - position.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        return Vector2(dx / dist, dy / dist)
    }

    override fun takeDamage(amount: Float): Boolean {
        return takeDamage(amount, false)
    }

    fun takeDamage(damage: Float, ignoresShield: Boolean): Boolean {
        if (isInvulnerable) {
            triggerAfterimage()
            return false
        }
        var remaining = damage
        if (!ignoresShield && shield > 0f) {
            val absorbed = remaining.coerceAtMost(shield)
            shield -= absorbed
            remaining -= absorbed
        }
        if (remaining > 0f) {
            health -= remaining
        }
        return health <= 0f
    }

    fun triggerAfterimage() {
        afterimageX = position.x
        afterimageY = position.y
        afterimageRotation = rotation
        afterimageActive = true
        afterimageTimer = AFTERIMAGE_DURATION
        val dodgeAngle = rotation + PI.toFloat()
        position.x += cos(dodgeAngle) * AFTERIMAGE_DODGE_DISTANCE
        position.y += sin(dodgeAngle) * AFTERIMAGE_DODGE_DISTANCE
    }

    fun getHealthPercent(): Float = (health / maxHealth).coerceIn(0f, 1f)

    override fun reset() {
        super.reset()
        targetPlayer = null
        timeAlive = 0f
        aiState = BossAIState.STRAFING
        aiTimer = 0f
        currentSpeed = BASE_SPEED
        strafeDirection = 1f
        railCooldown = 0f
        wantsToFire = false
        afterimageActive = false
        afterimageTimer = 0f
        shieldTickTimer = 0f
        isInvulnerable = true
        isStunned = false
        isCentering = false
        maxHealth = BOSS_MAX_HEALTH
        health = maxHealth
        shield = BOSS_MAX_SHIELD
        maxShield = BOSS_MAX_SHIELD
        regenActive = true
        enginePulse = 0f
        shielded = true
        isRushing = false
        rushBraking = false
        rushSpeed = 0f
        rushTimer = 0f
        holdPattern = false
        reentryBurn.clear()
        shieldSparks.clear()
    }
}
