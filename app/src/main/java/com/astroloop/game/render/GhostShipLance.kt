package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import com.astroloop.game.data.BandanaDefinitions
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.ShipDefinitions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ghost-ship lance climax: twelve ghost-ships gather around the crystal boss and HOLD —
 * the crystal reaches for them, but nothing moves until [release] is called (fired by
 * Astro's "Go."). On release, all twelve punch through the crystal at once and out the
 * far side; the crystal bursts because they left, not because it was hit.
 *
 * Pure timeline in update(); Canvas only in render().
 * All Paint objects are lazy so unit tests can load the class without Android runtime.
 */
class GhostShipLance {

    enum class Stage { GATHER, HOLD, RELEASED, DONE }

    companion object {
        const val GHOST_COUNT = 12
        const val GAP = 1.2f                                       // per-ghost arrival gap (s)
        const val FADE_DUR = 0.5f                                  // ghost fade-in duration (s)
        val T_GATHERED = GHOST_COUNT * GAP + FADE_DUR              // 14.9s — all ghosts present,
                                                                    // landing as WHISKERS says
                                                                    // "We're ready." at t=15.0s
        const val LANCE_SPEED = 2200f                              // px/s through the crystal and out
        const val DONE_AFTER_RELEASE = 2.5f                        // hold for shatter + trails

        const val RING_RADIUS = 120f  // outer ring distance from boss center
        const val GHOST_SIZE = 25f    // matches GameConfig.SHIP_BASE_SIZE / Boss.BOSS_SIZE
        const val GHOST_MAX_ALPHA = 0.62f
        const val TRAIL_LEN = 220f    // streak drawn behind each ghost during the punch-through
        const val SHARD_COUNT = 80
    }

    // ── Ghost data (built in start()) ────────────────────────────────────────

    private class Ghost(
        val angle: Float,          // angle from boss center (rad) — ghost sits here
        val bornTime: Float,       // T at which this ghost starts fading in
        val accentColor: Int,
        val pilotColor: Int,
        val weaponId: String,
        var alpha: Float = 0f
    )

    // ── Burst shards ─────────────────────────────────────────────────────────

    private class Shard(
        var x: Float, var y: Float,
        val vx: Float, val vy: Float,
        var life: Float,
        val maxLife: Float,
        val color: Int
    )

    // ── State ─────────────────────────────────────────────────────────────────

    private var t = 0f
    private var tRelease = -1f
    private var bossWorldX = 0f
    private var bossWorldY = 0f
    private var ghosts: List<Ghost> = emptyList()
    private val shards = mutableListOf<Shard>()
    private var flashAlpha = 0f

    private var _stage: Stage = Stage.GATHER
    val stage: Stage get() = _stage

    private var _burstFired = false
    val burstFired: Boolean get() = _burstFired

    /** True once every ghost has faded in — the ring is whole and can be released. */
    val gathered: Boolean get() = t >= T_GATHERED

    private val released: Boolean get() = tRelease >= 0f

    // ── Public API ────────────────────────────────────────────────────────────

    fun start(bossX: Float, bossY: Float) {
        t = 0f
        bossWorldX = bossX
        bossWorldY = bossY
        _stage = Stage.GATHER
        _burstFired = false
        tRelease = -1f
        flashAlpha = 0f
        shards.clear()

        val pilots = PilotDefinitions.pilots
        val ships = ShipDefinitions.ships
        ghosts = pilots.mapIndexed { i, pilot ->
            // Spread evenly around the boss, starting at top (−π/2)
            val angle = (i.toFloat() / GHOST_COUNT) * (2f * PI.toFloat()) - PI.toFloat() / 2f
            Ghost(
                angle = angle,
                bornTime = i * GAP,
                accentColor = BandanaDefinitions.accentColor(pilot.id),
                pilotColor = pilot.color,
                weaponId = ships.getOrNull(i)?.startingWeaponId ?: "pulse_cannon"
            )
        }
    }

    /**
     * Advance the timeline by [dt] seconds.
     * Gathers, then HOLDS indefinitely until [release] — the crystal shatters because the crew
     * LEAVE, so nothing may move until Astro says "Go."
     * @return the current [Stage] after the update.
     */
    fun update(dt: Float): Stage {
        if (_stage == Stage.DONE) return _stage
        t += dt

        // Ghost fade-in
        for (g in ghosts) {
            if (t > g.bornTime) {
                g.alpha = ((t - g.bornTime) / FADE_DUR).coerceIn(0f, 1f) * GHOST_MAX_ALPHA
            }
        }

        // Burst trigger (one-shot): the ghosts cross the crystal ~RING_RADIUS/LANCE_SPEED
        // after release. The crystal bursts because they passed through it, not from a hit.
        if (released && !_burstFired && ghostRadius() <= 0f) {
            _burstFired = true
            flashAlpha = 1f
            spawnShards()
        }

        if (flashAlpha > 0f) flashAlpha = (flashAlpha - dt * 4f).coerceAtLeast(0f)

        val iter = shards.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.life -= dt
            if (s.life <= 0f) iter.remove()
        }

        _stage = when {
            released && t - tRelease >= DONE_AFTER_RELEASE -> Stage.DONE
            released                                       -> Stage.RELEASED
            gathered                                       -> Stage.HOLD
            else                                           -> Stage.GATHER
        }
        return _stage
    }

    /**
     * Fired by Astro's "Go." — the ghosts punch through the crystal and off the screen.
     * Ignored until the ring is whole, so a half-formed ring can never launch.
     */
    fun release() {
        if (released || !gathered) return
        tRelease = t
    }

    /**
     * Radial distance from the boss center. During the punch-through this drives straight
     * through zero and goes NEGATIVE — the ghosts cross the crystal and exit the far side.
     */
    private fun ghostRadius(): Float =
        if (!released) RING_RADIUS else RING_RADIUS - LANCE_SPEED * (t - tRelease)

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Draw the ghost-ship lance overlay in screen space.
     * [cameraX]/[cameraY] convert world → screen.
     */
    @Suppress("UNUSED_PARAMETER")
    fun render(
        canvas: Canvas,
        shapeRenderer: ShapeRenderer,
        cameraX: Float,
        cameraY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        if (_stage == Stage.DONE || ghosts.isEmpty()) return

        val bsx = bossWorldX - cameraX   // boss screen X
        val bsy = bossWorldY - cameraY   // boss screen Y

        // ── Tendrils (HOLD: the crystal reaches for the gathered ring) ─────
        if (!_burstFired && gathered) {
            tendrilPaint.alpha = ((0.25f + 0.5f * ((t - T_GATHERED) / 2f).coerceIn(0f, 1f)) * 255).toInt()
            for (g in ghosts) {
                val (gsx, gsy) = ghostScreenPos(g, cameraX, cameraY)
                val jx = (Random.nextFloat() - 0.5f) * 16f
                val jy = (Random.nextFloat() - 0.5f) * 16f
                val mx = (bsx + gsx) * 0.5f + jx
                val my = (bsy + gsy) * 0.5f + jy
                tendrilPaint.color = 0xFFC8EEFF.toInt()
                canvas.drawLine(bsx, bsy, mx, my, tendrilPaint)
                canvas.drawLine(mx, my, gsx, gsy, tendrilPaint)
            }
        }

        // ── Ghost ships ───────────────────────────────────────────────────
        for (g in ghosts) {
            val alpha = g.alpha        // NO fade-out: they leave the screen, they don't dissolve
            if (alpha <= 0.01f) continue

            val (gsx, gsy) = ghostScreenPos(g, cameraX, cameraY)

            // Punch-through trail — a long streak back toward the crystal they came through.
            if (released) {
                val rNow = ghostRadius()
                val streakSX = bossWorldX + cos(g.angle) * (rNow + TRAIL_LEN) - cameraX
                val streakSY = bossWorldY + sin(g.angle) * (rNow + TRAIL_LEN) - cameraY
                streakPaint.color = g.accentColor
                streakPaint.alpha = (alpha * 180).toInt()
                canvas.drawLine(streakSX, streakSY, gsx, gsy, streakPaint)
            }

            ShipRenderer.drawShip(
                canvas = canvas,
                shapeRenderer = shapeRenderer,
                x = gsx,
                y = gsy,
                rotation = g.angle + PI.toFloat(),  // nose points toward boss center
                size = GHOST_SIZE,
                shipColor = g.accentColor,
                pilotColor = g.pilotColor,
                startingWeaponId = g.weaponId,
                alpha = alpha
            )
        }

        // ── Burst shards ─────────────────────────────────────────────────
        for (s in shards) {
            val sx = s.x - cameraX
            val sy = s.y - cameraY
            val lifeFrac = (s.life / s.maxLife).coerceIn(0f, 1f)
            shardPaint.color = s.color
            shardPaint.alpha = (lifeFrac * 210).toInt()
            canvas.drawRect(sx - 1.5f, sy - 1.5f, sx + 1.5f, sy + 1.5f, shardPaint)
        }

        // ── Local flash (NOT full-screen death freeze) ────────────────────
        if (flashAlpha > 0f) {
            flashPaint.alpha = (flashAlpha * 190).toInt()
            canvas.drawCircle(bsx, bsy, 180f * flashAlpha, flashPaint)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Ghost position in screen space. */
    private fun ghostScreenPos(g: Ghost, cameraX: Float, cameraY: Float): Pair<Float, Float> {
        val a = g.angle
        val r = if (!released) {
            // Settle inward as each ghost fades in
            val settle = ((t - g.bornTime) / 0.5f).coerceIn(0f, 1f)
            RING_RADIUS + (1f - settle) * 40f
        } else ghostRadius()
        return Pair(bossWorldX + cos(a) * r - cameraX,
                    bossWorldY + sin(a) * r - cameraY)
    }

    private fun spawnShards() {
        val colors = ghosts.map { it.accentColor }
        repeat(SHARD_COUNT) { i ->
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 80f + Random.nextFloat() * 230f
            val life = 1.2f + Random.nextFloat() * 1.4f
            shards.add(Shard(
                x = bossWorldX, y = bossWorldY,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = life, maxLife = life,
                color = colors[i % colors.size]
            ))
        }
    }

    // ── Lazy paints (Android stubs not called until render()) ─────────────────

    private val tendrilPaint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
    }

    private val streakPaint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
    }

    private val shardPaint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    private val flashPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            color = 0xFFCCEEFF.toInt()
            isAntiAlias = true
        }
    }
}
