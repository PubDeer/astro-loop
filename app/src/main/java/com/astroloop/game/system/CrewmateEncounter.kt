package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.core.SoundManager
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.entity.*
import com.astroloop.game.entity.Boss
import com.astroloop.game.weapon.Weapon
import com.astroloop.game.weapon.WeaponFactory
import com.astroloop.game.weapon.weapons.IonOrbiters
import com.astroloop.game.weapon.weapons.FrostRing
import com.astroloop.game.weapon.weapons.SpaceMines
import kotlin.math.*
import kotlin.random.Random

class CrewmateEncounter(
    private val ship: Ship,
    private val projectilePool: EntityPool<Projectile>
) {
    var crewmateActive = false
    var crewmateShip: EnemyShip? = null
    private var crewmateWeaponInstances = mutableListOf<Weapon>()
    private val neutralState = GameState.createNeutral()
    var crewmatePassives = mutableListOf<String>()
    var crewmatePilotId: String = ""
    var crewmateShipId: String = ""
    var crewmateDead = false
    var isHealthySelf = false  // True for past Astro — uses evasive AI
    var frozen = false  // True when past Astro joins fleet formation — stops AI updates
    var straightFlee = false  // rush: Past Astro saw you ignite — flat-out flight, no juking

    // Screen bounds for evasive AI (set from GameSurfaceView)
    var screenWidth: Float = 0f
    var screenHeight: Float = 0f

    // Zig-zag flee state for past Astro
    private var zigzagTimer: Float = 0f
    private var zigzagInterval: Float = 3f
    private var fleeAngle: Float = 0f
    private var targetFleeAngle: Float = 0f
    private val turnRate: Float = 2.5f  // radians/sec (~1.3s for 180° turn)
    private var zigzagInitialized: Boolean = false

    fun spawnHealthySelf(
        state: GameState,
        pilotId: String,
        shipId: String,
        enemyPool: EntityPool<EnemyShip>
    ) {
        ShipDefinitions.getShip(shipId) ?: return
        PilotDefinitions.getPilot(pilotId) ?: return

        crewmatePilotId = pilotId
        crewmateShipId = shipId
        crewmateDead = false
        isHealthySelf = true

        // Spawn at distance
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val dist = 350f
        val spawnX = ship.position.x + cos(angle) * dist
        val spawnY = ship.position.y + sin(angle) * dist

        val enemy = enemyPool.obtain()
        enemy.position.set(spawnX, spawnY)
        enemy.isActive = true
        enemy.health = 99999f  // Effectively invulnerable
        enemy.maxHealth = 99999f
        enemy.shipId = shipId
        enemy.radius = GameConfig.SHIP_BASE_SIZE
        enemy.speed = 300f * (0.95f + Random.nextFloat() * 0.1f)  // 95-105% of player base speed
        enemy.preferredDistance = 200f
        enemy.isCrewmate = true
        enemy.spawnShieldTimer = 0f
        enemy.perfectDodge = true  // 100% evasion with visual sidestep

        crewmateShip = enemy
        crewmateActive = true

        // No weapons — past Astro is purely fleeing

        zigzagInitialized = false
        zigzagTimer = 0f
        straightFlee = false
    }

    fun spawnCrewmateForAstro(
        state: GameState,
        pilotId: String,
        shipId: String,
        enemyPool: EntityPool<EnemyShip>,
        extraWeaponCount: Int = 0
    ) {
        val shipDef = ShipDefinitions.getShip(shipId) ?: return

        crewmatePilotId = pilotId
        crewmateShipId = shipId
        crewmateDead = false

        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val dist = 600f
        val spawnX = ship.position.x + cos(angle) * dist
        val spawnY = ship.position.y + sin(angle) * dist

        val enemy = enemyPool.obtain()
        enemy.position.set(spawnX, spawnY)
        enemy.isActive = true
        enemy.health = 80f  // Killable
        enemy.maxHealth = 80f
        enemy.shipId = shipId
        enemy.radius = GameConfig.SHIP_BASE_SIZE
        enemy.speed = 200f
        enemy.preferredDistance = 250f
        enemy.isCrewmate = true
        enemy.spawnShieldTimer = 0f

        crewmateShip = enemy
        crewmateActive = true

        // Single L1 starting weapon — matches the ship's normal loadout
        crewmateWeaponInstances.clear()
        WeaponFactory.createWeapon(shipDef.startingWeaponId)?.also { weapon ->
            weapon.cooldownTimer = 0.8f + Random.nextFloat() * 0.5f
            crewmateWeaponInstances.add(weapon)
        }

        if (extraWeaponCount > 0) {
            val extraPool = WeaponFactory.getBaseWeaponIds().filter { it != shipDef.startingWeaponId && it != "energy_saw" }
            for (extraId in extraPool.shuffled().take(extraWeaponCount)) {
                WeaponFactory.createWeapon(extraId)?.also { weapon ->
                    weapon.cooldownTimer = 0.8f + Random.nextFloat() * 0.5f
                    when (weapon) {
                        is IonOrbiters -> weapon.clearOrbiters()
                        is FrostRing   -> weapon.clearOrbiters()
                        else           -> {}
                    }
                    crewmateWeaponInstances.add(weapon)
                }
            }
        }

        // Close-range weapons need to be within their effective reach
        enemy.preferredDistance = when (shipDef.startingWeaponId) {
            "energy_saw"   -> 70f   // saw reach is 80f — stay within that
            "ion_orbiters" -> 80f   // orbit radius is 70f at L1 — stay close
            else           -> 250f
        }

        // 4 random passives (thematic — crewmate doesn't use full passive logic)
        crewmatePassives.clear()
        val allPassives = PassiveDefinitions.passives.map { it.id }
        crewmatePassives.addAll(allPassives.shuffled().take(4))
    }

    fun update(state: GameState, deltaTime: Float) {
        val enemy = crewmateShip ?: return
        if (!crewmateActive) return

        // Check if crewmate died — must run before isActive check since
        // onEnemyDestroyed() sets isActive=false before we get here
        if (enemy.health <= 0f) {
            crewmateDead = true
            enemy.isActive = false
            return
        }

        if (!enemy.isActive) return
        if (frozen) return

        // Update dodge animation
        enemy.updateDodge(deltaTime)

        if (isHealthySelf) {
            updateEvasiveAI(enemy, deltaTime)
        } else {
            updatePursuitAI(enemy, deltaTime)
        }
    }

    /** Normal crewmate AI — pursue player and fire weapons */
    private fun updatePursuitAI(enemy: EnemyShip, deltaTime: Float) {
        val dx = ship.position.x - enemy.position.x
        val dy = ship.position.y - enemy.position.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        enemy.rotation = atan2(dy, dx)

        // Set velocity for thruster; move position directly (EnemyAISystem skips update for crewmates)
        if (dist > enemy.preferredDistance) {
            enemy.velocity.set((dx / dist) * enemy.speed, (dy / dist) * enemy.speed)
            enemy.position.x += enemy.velocity.x * deltaTime
            enemy.position.y += enemy.velocity.y * deltaTime
        } else {
            enemy.velocity.set(0f, 0f)
        }

        for (weapon in crewmateWeaponInstances) {
            weapon.update(deltaTime)
            when (weapon) {
                is IonOrbiters -> weapon.updateOrbiters(enemy.position, deltaTime)
                is FrostRing   -> weapon.updateOrbiters(enemy.position, deltaTime)
                is SpaceMines  -> weapon.updatePendingMines(deltaTime, projectilePool, neutralState)
            }
            if (weapon.canFire()) {
                weapon.fire(enemy, neutralState, projectilePool, listOf(ship))
            }
        }
    }

    /** Past Astro AI — panicked zig-zag fleeing, no weapons, can go off-screen */
    private fun updateEvasiveAI(enemy: EnemyShip, deltaTime: Float) {
        // Initialize flee direction away from player
        if (!zigzagInitialized) {
            val dx = enemy.position.x - ship.position.x
            val dy = enemy.position.y - ship.position.y
            fleeAngle = atan2(dy, dx)
            targetFleeAngle = fleeAngle
            zigzagInitialized = true
            zigzagTimer = 0f
            zigzagInterval = 2f + Random.nextFloat() * 2f
        }

        // Distance check
        val dx = enemy.position.x - ship.position.x
        val dy = enemy.position.y - ship.position.y
        val distToPlayer = sqrt(dx * dx + dy * dy)
        val minDistance = 350f

        if (straightFlee || distToPlayer < minDistance) {
            // Override: flee directly away from player
            targetFleeAngle = atan2(dy, dx)
            zigzagTimer = 0f
        } else {
            // Panicked zig-zag: change target direction periodically
            zigzagTimer += deltaTime
            if (zigzagTimer >= zigzagInterval) {
                val turnAmount = (PI.toFloat() / 6f) + Random.nextFloat() * (PI.toFloat() / 3f)
                val turnDir = if (Random.nextBoolean()) 1f else -1f
                targetFleeAngle += turnDir * turnAmount

                // Bias back toward "away from player" to prevent circling back
                val awayAngle = atan2(dy, dx)
                targetFleeAngle = targetFleeAngle * 0.7f + awayAngle * 0.3f

                zigzagTimer = 0f
                zigzagInterval = 2f + Random.nextFloat() * 2f
            }
        }

        // Smooth turning: interpolate fleeAngle toward targetFleeAngle
        var angleDiff = targetFleeAngle - fleeAngle
        while (angleDiff > PI.toFloat()) angleDiff -= 2f * PI.toFloat()
        while (angleDiff < -PI.toFloat()) angleDiff += 2f * PI.toFloat()
        val maxTurn = turnRate * deltaTime
        fleeAngle += angleDiff.coerceIn(-maxTurn, maxTurn)

        // Set velocity for thruster; move position directly (EnemyAISystem skips update for crewmates)
        val fleeX = cos(fleeAngle)
        val fleeY = sin(fleeAngle)
        enemy.velocity.set(fleeX * enemy.speed, fleeY * enemy.speed)
        enemy.position.x += enemy.velocity.x * deltaTime
        enemy.position.y += enemy.velocity.y * deltaTime

        // Face flee direction
        enemy.rotation = fleeAngle
    }

    fun reset(projectiles: List<Projectile>) {
        crewmateActive = false
        straightFlee = false
        crewmateShip = null
        // Fade active orbiters/frost rings before clearing — no instant disappearance
        for (proj in projectiles) {
            if (!proj.isActive) continue
            if (proj.type == ProjectileType.ORBITER) {
                proj.lifetime = proj.age + 0.5f
            }
        }
        crewmateWeaponInstances.forEach { weapon ->
            when (weapon) {
                is IonOrbiters -> weapon.clearOrbiters()
                is FrostRing   -> weapon.clearOrbiters()
            }
        }
        crewmateWeaponInstances.clear()
        crewmatePassives.clear()
        crewmateDead = false
        isHealthySelf = false
        frozen = false
    }
}
