package com.astroloop.game.entity

import com.astroloop.game.util.Vector2
import kotlin.math.cos
import kotlin.math.sin

enum class ProjectileType {
    BULLET,         // Standard projectile
    MISSILE,        // Homing projectile
    BEAM,           // Continuous laser segment
    ORBITER,        // Orbiting projectile
    MINE,           // Stationary explosive
    LIGHTNING,      // Chain lightning segment
    TORPEDO,        // Slow, high damage
    FLAK,           // Exploding shell
    GRAVITY,        // Gravity well effect
    PLASMA,         // Plasma whip segment
    BOMBLET,        // Cluster bomb sub-munition
    FRAGMENT,       // L5 cluster bomb fragment
    RECALL_SHOT     // Boss railgun shot that pauses and returns
}

class Projectile : Entity() {

    var type: ProjectileType = ProjectileType.BULLET
    var damage: Float = 10f
    var lifetime: Float = 2f
    var age: Float = 0f
    var piercing: Boolean = false
    var pierceCount: Int = 0
    var maxPierces: Int = 1
    var ownerId: Int = 0  // To identify source
    var weaponId: String = ""
    var isEnemyProjectile: Boolean = false  // True if fired by enemy ship
    var ignoresSpawnShield: Boolean = false

    // For homing projectiles
    var target: Entity? = null
    var homingStrength: Float = 0f

    // For orbiters
    var orbitAngle: Float = 0f
    var orbitRadius: Float = 0f
    var orbitSpeed: Float = 0f
    var orbitCenter: Vector2? = null

    // For attached beams like Plasma Lance
    var beamOrigin: Vector2? = null

    // For chain lightning
    var bounceCount: Int = 0
    var maxBounces: Int = 3
    var hitEntities: MutableSet<Entity> = mutableSetOf()

    // For explosions
    var explodeOnDeath: Boolean = false
    var explosionRadius: Float = 0f
    var explosionDamage: Float = 0f
    var proximityFuse: Boolean = false  // Flak: detonate when near enemy

    // For cluster bomb bomblets/fragments
    var bombletCount: Int = 0
    var bombletDamage: Float = 0f
    var bombletExplosionRadius: Float = 0f
    var hasFragments: Boolean = false
    var fragmentDamage: Float = 0f
    var isVisualOnly: Boolean = false

    // Trail-emitting projectiles (napalm trail, scatter trails)
    var leavesTrail: Boolean = false
    var trailTimer: Float = 0f
    var trailInterval: Float = 0.4f
    var trailRadius: Float = 30f
    var trailDamage: Float = 5f
    var trailDuration: Float = 1.5f

    // Gravity sink mines (Space Mines L5)
    var gravitySink: Boolean = false
    var gravitySinkTimer: Float = 0f

    // Chain arc (Pulse Cannon L5)
    var hasChained: Boolean = false

    // Recall shot data
    var isRecalling: Boolean = false
    var recallPauseTimer: Float = 0f
    var recallRetargeted: Boolean = false
    var ricochetCount: Int = 0

    // Deceleration
    var decelerationRate: Float = 0f  // Speed reduction per second; 0 = no decel

    // Visual
    var length: Float = 10f
    var width: Float = 2f
    var color: Int = 0xFFFFFF00.toInt()
    var fadeAlpha: Float = 1f  // For fade-in effects (0→1 over time)

    // For beam weapons - store rotation angle when velocity is 0
    var beamAngle: Float = 0f

    // Flag to trigger disappear effect
    var expiredNaturally: Boolean = false

    // Crit flag for damage number display (used by instant-hit weapons like SolarStorm)
    var isCrit: Boolean = false
    var shouldFadeOut: Boolean = false  // Force fade-out and expiry; bypasses RECALL_SHOT exemption

    override fun update(deltaTime: Float) {
        age += deltaTime

        if (fadeAlpha < 1f) {
            fadeAlpha = (fadeAlpha + deltaTime * 2f).coerceAtMost(1f)  // 0.5s fade
        }

        // Fade out in last 0.5 seconds of life (orbiters always; others when force-expired)
        if (type == ProjectileType.ORBITER || shouldFadeOut) {
            val remaining = lifetime - age
            if (remaining < 0.5f) {
                fadeAlpha = (remaining / 0.5f).coerceIn(0f, 1f)
            }
        }

        // shouldFadeOut bypasses the RECALL_SHOT lifetime exemption
        if (shouldFadeOut && age >= lifetime) {
            isActive = false
            return
        }

        if (age >= lifetime && type != ProjectileType.RECALL_SHOT) {
            expiredNaturally = true  // Flag for visual effect
            isActive = false
            return
        }

        // Apply deceleration before position update
        if (decelerationRate > 0f) {
            val speed = velocity.length()
            if (speed > 0f) {
                val newSpeed = (speed - decelerationRate * deltaTime).coerceAtLeast(0f)
                velocity.mul(newSpeed / speed)
                // Zero out deceleration once stopped so mine repulsion can work
                if (newSpeed == 0f && type == ProjectileType.MINE) {
                    decelerationRate = 0f
                }
            }
        }

        when (type) {
            ProjectileType.MISSILE, ProjectileType.TORPEDO -> updateHoming(deltaTime)
            ProjectileType.ORBITER -> updateOrbiter(deltaTime)
            ProjectileType.BEAM -> {
                // Update attached beam position from origin
                beamOrigin?.let { origin ->
                    position.set(origin)
                }
                // Reset hit tracking each frame so beams can deal continuous damage
                hitEntities.clear()
            }
            ProjectileType.PLASMA -> {
                super.update(deltaTime)
                // If attached to a ship via orbitCenter, follow its movement
                orbitCenter?.let { center ->
                    beamOrigin?.let { origin ->
                        position.add(center.x - origin.x, center.y - origin.y)
                        origin.set(center)
                    }
                }
            }
            else -> super.update(deltaTime)
        }
    }

    private fun updateHoming(deltaTime: Float) {
        target?.let { t ->
            if (t.isActive) {
                val toTarget = Vector2(t.position.x - position.x, t.position.y - position.y)
                val targetAngle = toTarget.angle()
                val currentAngle = velocity.angle()

                var angleDiff = targetAngle - currentAngle
                while (angleDiff > Math.PI) angleDiff -= (2 * Math.PI).toFloat()
                while (angleDiff < -Math.PI) angleDiff += (2 * Math.PI).toFloat()

                val turnAmount = homingStrength * deltaTime
                val newAngle = currentAngle + angleDiff.coerceIn(-turnAmount, turnAmount)

                val speed = velocity.length()
                velocity.set(Vector2.fromAngle(newAngle, speed))
            }
        }

        super.update(deltaTime)
    }

    private fun updateOrbiter(deltaTime: Float) {
        orbitCenter?.let { center ->
            orbitAngle += orbitSpeed * deltaTime
            position.x = center.x + cos(orbitAngle) * orbitRadius
            position.y = center.y + sin(orbitAngle) * orbitRadius
        }
        // Reset hit tracking each frame so orbiters can hit the same target again
        // on subsequent passes. The collision system won't double-hit within a single
        // frame because hitEntities is checked before each onHit call.
        hitEntities.clear()
    }

    fun initialize(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        projectileType: ProjectileType,
        projectileDamage: Float,
        projectileLifetime: Float
    ) {
        position.set(x, y)
        velocity.set(vx, vy)
        type = projectileType
        damage = projectileDamage
        lifetime = projectileLifetime
        age = 0f
        isActive = true
        piercing = false
        pierceCount = 0
        maxPierces = 1
        isEnemyProjectile = false
        ignoresSpawnShield = false
        target = null
        homingStrength = 0f
        orbitCenter = null
        bounceCount = 0
        hitEntities.clear()
        explodeOnDeath = false

        // Set visual properties based on type
        when (type) {
            ProjectileType.BULLET -> {
                length = 8f
                width = 2f
                radius = 4f
            }
            ProjectileType.MISSILE -> {
                length = 12f
                width = 4f
                radius = 6f
            }
            ProjectileType.TORPEDO -> {
                length = 16f
                width = 6f
                radius = 8f
            }
            ProjectileType.ORBITER -> {
                length = 6f
                width = 6f
                radius = 6f
            }
            ProjectileType.MINE -> {
                length = 10f
                width = 10f
                radius = 10f
            }
            ProjectileType.BEAM -> {
                length = 20f
                width = 3f
                radius = 3f
            }
            ProjectileType.LIGHTNING -> {
                length = 15f
                width = 2f
                radius = 5f
            }
            ProjectileType.FLAK -> {
                length = 8f
                width = 4f
                radius = 5f
            }
            ProjectileType.GRAVITY -> {
                length = 20f
                width = 20f
                radius = 20f
            }
            ProjectileType.PLASMA -> {
                length = 12f
                width = 4f
                radius = 6f
            }
            ProjectileType.BOMBLET -> {
                length = 5f
                width = 5f
                radius = 5f
            }
            ProjectileType.FRAGMENT -> {
                length = 3f
                width = 3f
                radius = 3f
            }
            ProjectileType.RECALL_SHOT -> {
                length = 14f
                width = 3f
                radius = 5f
            }
        }

        fadeAlpha = 1f
        beamAngle = 0f
        expiredNaturally = false
        isCrit = false
        shouldFadeOut = false
    }

    fun onHit(entity: Entity): Boolean {
        if (piercing) {
            if (hitEntities.contains(entity)) {
                return false  // Already hit this one
            }
            hitEntities.add(entity)
            pierceCount++
            if (pierceCount >= maxPierces) {
                isActive = false
            }
            return true
        } else {
            isActive = false
            return true
        }
    }

    fun canBounce(): Boolean = bounceCount < maxBounces

    fun recordBounce() {
        bounceCount++
    }

    override fun reset() {
        super.reset()
        type = ProjectileType.BULLET
        damage = 10f
        lifetime = 2f
        age = 0f
        piercing = false
        pierceCount = 0
        maxPierces = 1
        target = null
        homingStrength = 0f
        orbitAngle = 0f
        orbitRadius = 0f
        orbitSpeed = 0f
        orbitCenter = null
        beamOrigin = null
        bounceCount = 0
        maxBounces = 3
        hitEntities.clear()
        explodeOnDeath = false
        explosionRadius = 0f
        explosionDamage = 0f
        proximityFuse = false
        bombletCount = 0
        bombletDamage = 0f
        bombletExplosionRadius = 0f
        hasFragments = false
        fragmentDamage = 0f
        isVisualOnly = false

        leavesTrail = false
        trailTimer = 0f
        trailInterval = 0.4f
        trailRadius = 30f
        trailDamage = 5f
        trailDuration = 1.5f
        gravitySink = false
        gravitySinkTimer = 0f
        hasChained = false
        decelerationRate = 0f
        isRecalling = false
        recallPauseTimer = 0f
        recallRetargeted = false
        ricochetCount = 0
        weaponId = ""
        isEnemyProjectile = false
        ignoresSpawnShield = false
        fadeAlpha = 1f
        beamAngle = 0f
        expiredNaturally = false
        isCrit = false
        shouldFadeOut = false
    }

    companion object {
        const val GRAVITY_SINK_DURATION = 0.3f
        const val GRAVITY_SINK_RADIUS = 150f
        const val GRAVITY_SINK_FORCE = 180f
    }
}
