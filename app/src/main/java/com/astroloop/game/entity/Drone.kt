package com.astroloop.game.entity

import com.astroloop.game.util.Vector2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class DroneAIState {
    PATROL,     // Flying around near player
    ATTACK,     // Moving toward and shooting at target
    EVADE       // Dodging an obstacle
}

class Drone : Entity() {

    companion object {
        const val DEFAULT_MAX_SPEED = 280f
        const val DEFAULT_LEASH_DISTANCE = 450f
        const val DEFAULT_ATTACK_RANGE = 350f
        const val DEFAULT_FIRE_RATE = 1.2f
        const val DEFAULT_THEME_COLOR = 0xFF55CC66.toInt()
    }

    var parentShip: Ship? = null
    var weaponId: String = "tb26"
    var fireRate: Float = DEFAULT_FIRE_RATE  // Shots per second
    var fireCooldown: Float = 0f

    // AI state
    var aiState: DroneAIState = DroneAIState.PATROL
    var currentTarget: Entity? = null
    var evadeTarget: Entity? = null

    // Movement parameters
    private var maxSpeed: Float = DEFAULT_MAX_SPEED
    private val acceleration: Float = 600f
    private val turnSpeed: Float = 6f
    private var leashDistance: Float = DEFAULT_LEASH_DISTANCE
    private val preferredDistance: Float = 250f
    private var attackRange: Float = DEFAULT_ATTACK_RANGE
    private val evadeRange: Float = 100f

    // Patrol behavior
    private var patrolAngle: Float = 0f
    private var patrolTimer: Float = 0f
    private val patrolChangeInterval: Float = 2f

    // Visual state
    var thrusterActive: Boolean = false
    var targetRotation: Float = 0f

    // Dodge animation state
    var dodgeTimer: Float = 0f
    var dodgeOffsetX: Float = 0f
    var dodgeOffsetY: Float = 0f
    private val dodgeDuration: Float = 0.15f
    private val dodgeDistance: Float = 20f

    // Theme color (set by GameSurfaceView based on pilot)
    var themeColor: Int = DEFAULT_THEME_COLOR

    // Evolution state
    var evolved: Boolean = false

    init {
        radius = 10f
    }

    fun initialize(ship: Ship, startAngle: Float) {
        parentShip = ship
        patrolAngle = startAngle
        isActive = true
        fireCooldown = Random.nextFloat() * 0.5f  // Stagger initial fire
        aiState = DroneAIState.PATROL
        currentTarget = null
        evadeTarget = null

        // Start near the ship
        position.x = ship.position.x + cos(startAngle) * preferredDistance
        position.y = ship.position.y + sin(startAngle) * preferredDistance
        velocity.set(0f, 0f)
        rotation = startAngle
    }

    fun onHit() {
        if (dodgeTimer > 0f) return // Already dodging
        dodgeTimer = dodgeDuration
        // Jink perpendicular to facing
        val perpAngle = rotation + PI.toFloat() / 2f * (if (Random.nextBoolean()) 1f else -1f)
        dodgeOffsetX = cos(perpAngle) * dodgeDistance
        dodgeOffsetY = sin(perpAngle) * dodgeDistance
    }

    fun applyEvolution() {
        evolved = true
        leashDistance = 900f    // Double leash
        attackRange = 700f      // Much larger engagement range
        maxSpeed = 350f         // Faster
    }

    fun updateAI(
        deltaTime: Float,
        asteroids: List<Asteroid>,
        enemies: List<EnemyShip>,
        peers: List<Drone> = emptyList()
    ) {
        val ship = parentShip ?: return

        // Update dodge animation
        if (dodgeTimer > 0f) {
            dodgeTimer -= deltaTime
            if (dodgeTimer <= 0f) {
                dodgeTimer = 0f
                dodgeOffsetX = 0f
                dodgeOffsetY = 0f
            } else {
                // Lerp offset back to 0
                val t = 1f - (dodgeTimer / dodgeDuration)
                dodgeOffsetX *= (1f - t * 0.3f)
                dodgeOffsetY *= (1f - t * 0.3f)
            }
        }

        // Update fire cooldown
        if (fireCooldown > 0) {
            fireCooldown -= deltaTime
        }

        // Check for threats to evade
        val nearestThreat = findNearestThreat(asteroids)
        if (nearestThreat != null) {
            aiState = DroneAIState.EVADE
            evadeTarget = nearestThreat
        } else if (aiState == DroneAIState.EVADE) {
            aiState = DroneAIState.PATROL
            evadeTarget = null
        }

        // Find attack target (enemies prioritized)
        val validEnemies = enemies.filter { it.isActive && !it.isWarping }
        val validAsteroids = asteroids.filter { it.isActive }

        val nearestEnemy = validEnemies.minByOrNull { position.distanceSquared(it.position) }
        val nearestAsteroid = validAsteroids.minByOrNull { position.distanceSquared(it.position) }

        // Target selection: evolved always prioritizes enemies
        currentTarget = if (evolved) {
            nearestEnemy ?: nearestAsteroid
        } else {
            when {
                nearestEnemy != null && position.distance(nearestEnemy.position) < attackRange -> nearestEnemy
                nearestAsteroid != null -> nearestAsteroid
                else -> null
            }
        }

        // If we have a target and not evading, switch to attack
        if (currentTarget != null && aiState != DroneAIState.EVADE) {
            aiState = DroneAIState.ATTACK
        } else if (currentTarget == null && aiState == DroneAIState.ATTACK) {
            aiState = DroneAIState.PATROL
        }

        // Execute AI behavior
        when (aiState) {
            DroneAIState.PATROL -> updatePatrol(deltaTime, ship)
            DroneAIState.ATTACK -> updateAttack(deltaTime, ship)
            DroneAIState.EVADE -> updateEvade(deltaTime, ship)
        }

        // Enforce leash distance
        enforceLeash(deltaTime, ship)

        // Peer repulsion — push away from other drones and the ship
        val repulsionRadius = 60f
        val shipRepulsionRadius = 40f
        val repulsionForce = 80f

        for (peer in peers) {
            if (peer === this) continue
            val dx = position.x - peer.position.x
            val dy = position.y - peer.position.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < repulsionRadius && dist > 0f) {
                val strength = (repulsionRadius - dist) / repulsionRadius * repulsionForce
                velocity.x += (dx / dist) * strength
                velocity.y += (dy / dist) * strength
            }
        }

        val sdx = position.x - ship.position.x
        val sdy = position.y - ship.position.y
        val sdist = sqrt(sdx * sdx + sdy * sdy)
        if (sdist < shipRepulsionRadius && sdist > 0f) {
            val strength = (shipRepulsionRadius - sdist) / shipRepulsionRadius * repulsionForce
            velocity.x += (sdx / sdist) * strength
            velocity.y += (sdy / sdist) * strength
        }

        // Apply movement
        applyMovement(deltaTime)

        // Update rotation toward target rotation
        val angleDiff = normalizeAngle(targetRotation - rotation)
        rotation += angleDiff.coerceIn(-turnSpeed * deltaTime, turnSpeed * deltaTime)
    }

    private fun updatePatrol(deltaTime: Float, ship: Ship) {
        thrusterActive = true

        // Update patrol timer
        patrolTimer += deltaTime
        if (patrolTimer >= patrolChangeInterval) {
            patrolTimer = 0f
            patrolAngle += (Random.nextFloat() - 0.5f) * PI.toFloat()
        }

        // Calculate patrol position around ship
        val targetX = ship.position.x + cos(patrolAngle) * preferredDistance
        val targetY = ship.position.y + sin(patrolAngle) * preferredDistance

        // Move toward patrol position
        val toTarget = Vector2(targetX - position.x, targetY - position.y)
        val dist = toTarget.length()

        if (dist > 20f) {
            val dir = toTarget.normalized()
            velocity.x += dir.x * acceleration * deltaTime
            velocity.y += dir.y * acceleration * deltaTime
            targetRotation = atan2(dir.y, dir.x)
        } else {
            // Slow down when close
            velocity.x *= 0.95f
            velocity.y *= 0.95f
            thrusterActive = velocity.lengthSquared() > 100f
        }
    }

    private fun updateAttack(deltaTime: Float, ship: Ship) {
        val target = currentTarget ?: return
        thrusterActive = true
        val toTarget = target.position - position
        targetRotation = atan2(toTarget.y, toTarget.x)

        val idealPos = if (evolved) {
            // Flank: approach from perpendicular angle
            val playerToTarget = target.position - ship.position
            val perpAngle = atan2(playerToTarget.y, playerToTarget.x) + PI.toFloat() / 2f
            target.position + Vector2(cos(perpAngle), sin(perpAngle)) * 100f
        } else {
            ship.position + (target.position - ship.position).normalized() * (preferredDistance * 0.7f)
        }

        val toIdeal = idealPos - position
        val dist = toIdeal.length()
        if (dist > 30f) {
            val dir = toIdeal.normalized()
            velocity.x += dir.x * acceleration * deltaTime
            velocity.y += dir.y * acceleration * deltaTime
        }
    }

    private fun updateEvade(deltaTime: Float, ship: Ship) {
        val threat = evadeTarget ?: return
        thrusterActive = true

        // Move away from threat
        val awayFromThreat = (position - threat.position).normalized()

        // Also bias toward player
        val toPlayer = (ship.position - position).normalized()

        // Combine directions
        val evadeDir = (awayFromThreat * 0.7f + toPlayer * 0.3f).normalized()

        velocity.x += evadeDir.x * acceleration * 1.5f * deltaTime
        velocity.y += evadeDir.y * acceleration * 1.5f * deltaTime

        targetRotation = atan2(evadeDir.y, evadeDir.x)
    }

    private fun enforceLeash(deltaTime: Float, ship: Ship) {
        val toShip = ship.position - position
        val dist = toShip.length()

        if (dist > leashDistance) {
            // Pull back toward ship
            val pullStrength = (dist - leashDistance) / 50f
            val pullDir = toShip.normalized()
            velocity.x += pullDir.x * acceleration * pullStrength * deltaTime
            velocity.y += pullDir.y * acceleration * pullStrength * deltaTime
        }
    }

    private fun applyMovement(deltaTime: Float) {
        // Cap speed
        val speed = velocity.length()
        if (speed > maxSpeed) {
            velocity.x = velocity.x / speed * maxSpeed
            velocity.y = velocity.y / speed * maxSpeed
        }

        // Apply velocity
        position.x += velocity.x * deltaTime
        position.y += velocity.y * deltaTime

        // Apply drag
        velocity.x *= 0.98f
        velocity.y *= 0.98f
    }

    private fun findNearestThreat(asteroids: List<Asteroid>): Asteroid? {
        return asteroids
            .filter { it.isActive }
            .filter { position.distance(it.position) < evadeRange + it.radius }
            .minByOrNull { position.distanceSquared(it.position) }
    }

    fun canFire(): Boolean = fireCooldown <= 0f && currentTarget != null

    fun fire() {
        fireCooldown = 1f / fireRate
    }

    fun getFireDirection(): Vector2? {
        val target = currentTarget ?: return null
        return (target.position - position).normalized()
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > PI) a -= 2 * PI.toFloat()
        while (a < -PI) a += 2 * PI.toFloat()
        return a
    }

    override fun reset() {
        super.reset()
        parentShip = null
        aiState = DroneAIState.PATROL
        currentTarget = null
        evadeTarget = null
        patrolAngle = 0f
        patrolTimer = 0f
        weaponId = "tb26"
        fireRate = DEFAULT_FIRE_RATE
        fireCooldown = 0f
        thrusterActive = false
        targetRotation = 0f
        dodgeTimer = 0f
        dodgeOffsetX = 0f
        dodgeOffsetY = 0f
        themeColor = DEFAULT_THEME_COLOR
        evolved = false
        leashDistance = DEFAULT_LEASH_DISTANCE
        maxSpeed = DEFAULT_MAX_SPEED
        attackRange = DEFAULT_ATTACK_RANGE
    }
}
