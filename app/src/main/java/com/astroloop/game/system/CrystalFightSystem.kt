package com.astroloop.game.system

import com.astroloop.game.render.CrystalPalette

enum class CrystalPhase { P1, P2, P3, P4, P5 }

/**
 * Bandana-finale survival fight. Weapons are disabled, so nothing damages the crystal —
 * the fight is driven purely off an elapsed-time clock. Every 18s a new spiral layer is
 * added PERMANENTLY, so P5 runs all five at once: the escalation IS the crystal losing
 * its composure. Pure — no spawning, no render, no Random.
 */
class CrystalFightSystem {

    private val angles = FloatArray(LAYERS.size)
    private val cooldowns = FloatArray(LAYERS.size)
    private var elapsed = 0f
    private var lastPhase: CrystalPhase? = null

    // Cleared at the start of every update(); set only on a phase transition. Holds the new
    // phase until the NEXT update() — a plain, side-effect-free read for the taunt consumer.
    var phaseChanged: CrystalPhase? = null
        private set

    val elapsedTime: Float get() = elapsed
    val survived: Boolean get() = elapsed >= FIGHT_DURATION

    companion object {
        const val FIGHT_DURATION = 90f
        const val PHASE_DURATION = 18f

        // --- Fairness knobs (Touhou rules, adapted to free flight) ---
        // Crystal bullets hit the REGULAR ship hitbox (SHIP_BASE_SIZE 25 + BULLET_RADIUS 3 =
        // 56px danger diameter). Fair means every stream is threadable: spacing
        // (speed * interval) clears that diameter with room to react, and the open window at
        // a crossing point beats the time the full ship needs to cross the lane. Both are
        // unit-tested — the tests are the fairness contract.
        const val BULLET_RADIUS = 3f
        const val BULLET_DAMAGE = 100f  // decisive: one clean hit ends a base-hull run

        /**
         * Index == phase index. Phase N runs LAYERS.take(N + 1).
         *
         * Speeds NEVER drop below 375 * 1.3 = 487.5 px/s (max player speed x margin) or the
         * fight becomes winnable by flying away — see CrystalFightSystemTest.
         *
         * Intervals are FAIRNESS, not just density: spacing = speed * interval is the picket
         * gap the player threads when a scissor pinch (counter-rotating arms converging)
         * forces a crossing. At 0.06s every stream was a solid wall — spacing 30-54px against
         * the 56px danger diameter — and pinches were guaranteed hits. 0.30s keeps every
         * stream threadable by the FULL ship hitbox (see everyStreamIsThreadable /
         * crossingWindowBeatsCrossingExposure). Density comes from extra ARMS — separate,
         * widely-spaced streams (a windmill) — never from packing bullets tighter along one
         * lane: pressure the player can read.
         *
         * Density: live = (arms / interval) * (1500 / speed). Speed and lifetime are inversely
         * coupled — faster bullets exit the despawn radius sooner — so the ramp pays for its
         * own density. That formula assumes a STATIONARY camera; the despawn radius is actually
         * measured from the camera, which centres on the ship, so a bullet travelling with a
         * fleeing player closes on the boundary far slower and lives far longer. Stationary
         * figure at P5 ~= 86; real peak with the player fleeing at max speed ~= 116, well
         * under the 200 pre-allocation (the pool grows once to its high-water mark, it does
         * not allocate per frame). Retune and you MUST recompute the FLEEING number, not the
         * stationary one — see CrystalFightSystemTest.densityBudgetHoldsAtMaxEscalation.
         */
        val LAYERS = listOf(
            SpiralLayer(CrystalPalette.LAYER_COLORS[0], 500f,  0.6f, 2, 0.30f), // P1 cyan   ~20 live
            SpiralLayer(CrystalPalette.LAYER_COLORS[1], 600f, -1.1f, 2, 0.30f), // P2 blue   ~17
            SpiralLayer(CrystalPalette.LAYER_COLORS[2], 700f, -0.4f, 2, 0.30f), // P3 indigo ~14
            SpiralLayer(CrystalPalette.LAYER_COLORS[3], 800f,  1.6f, 3, 0.30f), // P4 violet ~19
            SpiralLayer(CrystalPalette.LAYER_COLORS[4], 900f,  2.4f, 3, 0.30f)  // P5 white  ~17
        )

        fun phaseFor(elapsed: Float): CrystalPhase {
            val i = (elapsed / PHASE_DURATION).toInt().coerceIn(0, CrystalPhase.values().size - 1)
            return CrystalPhase.values()[i]
        }

        fun layersFor(p: CrystalPhase): List<SpiralLayer> = LAYERS.take(p.ordinal + 1)
    }

    fun update(bossX: Float, bossY: Float, dt: Float): List<BulletSpec> {
        elapsed += dt
        phaseChanged = null
        val phase = phaseFor(elapsed)
        if (phase != lastPhase) { phaseChanged = phase; lastPhase = phase }

        val out = ArrayList<BulletSpec>()
        for (i in 0..phase.ordinal) {
            val layer = LAYERS[i]
            angles[i] += layer.rotSpeed * dt   // the arm sweeps; this is what makes it a spiral
            val cd = cooldowns[i] - dt
            if (cd <= 0f) {
                out += CrystalEmitter.emit(layer, bossX, bossY, angles[i])
                cooldowns[i] = layer.interval
            } else cooldowns[i] = cd
        }
        return out
    }

    fun reset() {
        angles.fill(0f)
        cooldowns.fill(0f)
        elapsed = 0f
        lastPhase = null
        phaseChanged = null
    }
}
