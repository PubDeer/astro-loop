package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Firer
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.Weapon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LingeringNova : Weapon(
    id = "lingering_nova",
    name = "Lingering Nova",
    description = "AoE burst that detonates, lingers, then detonates again"
) {
    override val baseDamage = 30f
    override val baseCooldown = 4f
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 1

    // Delayed second detonation ("the lingering core explodes again").
    private var pendingBlastTimer: Float = -1f
    private var pendingBlastX: Float = 0f
    private var pendingBlastY: Float = 0f
    private var pendingIsEnemy: Boolean = false
    private var pendingIsCorruption: Boolean = false

    private fun getBlastRadius(state: GameState): Float = 360f * state.areaMultiplier

    private fun fireRingBurst(
        x: Float,
        y: Float,
        pool: EntityPool<Projectile>,
        state: GameState,
        isEnemy: Boolean,
        isCorruption: Boolean
    ) {
        val damage = getDamage(state)
        val blastRadius = getBlastRadius(state)
        val ringCount = 8
        for (i in 0 until ringCount) {
            val angle = (2f * PI.toFloat() * i / ringCount)
            val p = pool.obtain()
            p.initialize(
                x = x,
                y = y,
                vx = cos(angle) * blastRadius * 3f,
                vy = sin(angle) * blastRadius * 3f,
                projectileType = ProjectileType.PLASMA,
                projectileDamage = damage,
                projectileLifetime = 0.3f
            )
            p.isEnemyProjectile = isEnemy
            p.weaponId = id
            p.radius = blastRadius / 3f
            p.piercing = true
            p.maxPierces = 1000
            p.color = ShipDefinitions.getEvolutionColor("nova_blast", isCorruption)
        }
    }

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        if (!canFire()) return

        // Orphan guard: if a previous core never detonated, do it now so a core
        // can never be left undetonated.
        if (pendingBlastTimer >= 0f) {
            fireRingBurst(pendingBlastX, pendingBlastY, projectilePool, state, pendingIsEnemy, pendingIsCorruption)
            pendingBlastTimer = -1f
        }

        val cooldown = getCooldown(state)

        // Explosion #1 — immediate ring burst at the fire position.
        fireRingBurst(firer.position.x, firer.position.y, projectilePool, state, firer.isEnemyFirer, state.isCorruptionRun)

        // Lingering core — a stationary, non-damaging visual orb that sits until
        // the second detonation. Telegraph only (no DoT). Lifetime equals the
        // cooldown so it pops exactly as the next shot lands.
        val core = projectilePool.obtain()
        core.initialize(
            x = firer.position.x,
            y = firer.position.y,
            vx = 0f,
            vy = 0f,
            projectileType = ProjectileType.PLASMA,
            projectileDamage = 0f,
            projectileLifetime = cooldown
        )
        core.isEnemyProjectile = firer.isEnemyFirer
        core.weaponId = id
        core.radius = 40f * state.areaMultiplier
        core.piercing = true
        core.maxPierces = 1000
        core.isVisualOnly = true
        core.color = ShipDefinitions.getEvolutionColor("nova_blast", state.isCorruptionRun)

        // Arm explosion #2 at the same spot, one cooldown later.
        pendingBlastTimer = cooldown
        pendingBlastX = firer.position.x
        pendingBlastY = firer.position.y
        pendingIsEnemy = firer.isEnemyFirer
        pendingIsCorruption = state.isCorruptionRun

        cooldownTimer = cooldown
    }

    /** Drives the delayed second detonation. Wired in WeaponSystem like SpaceMines. */
    fun updatePending(deltaTime: Float, projectilePool: EntityPool<Projectile>, state: GameState) {
        if (pendingBlastTimer < 0f) return
        pendingBlastTimer -= deltaTime
        if (pendingBlastTimer <= 0f) {
            fireRingBurst(pendingBlastX, pendingBlastY, projectilePool, state, pendingIsEnemy, pendingIsCorruption)
            pendingBlastTimer = -1f
        }
    }
}
