package com.astroloop.game.hangar

import com.astroloop.game.core.StoryStage

enum class SignMode { STEADY, BLINKING }

/**
 * Story-stage-aware bar decoration config. Pure data — no Android deps — so the
 * stage→dressing mapping is unit-testable. Consumed by BarPageRenderer.
 */
data class BarDressing(
    val lampColors: List<Int>,   // exactly 7, left→right; replaces the shipped rainbow
    val signMode: SignMode,      // neon BAR sign behavior
    val stringLights: Boolean,   // warm swags (NORMAL)
    val fairyLights: Boolean,    // colorful swags (ASTRO_LOOP)
    val seatedCrew: Boolean,     // walkers sit at stools (NORMAL, ASTRO_LOOP — corruption crew never lounges)
    val confetti: Boolean        // floor confetti (ASTRO_LOOP)
) {
    companion object {
        // Corruption red — mirrors Boss.CORRUPTION_COLOR without pulling an Android dep in.
        private const val CORRUPTION_RED = 0xFFAA2222.toInt()

        private val WARM = listOf(
            0xFFFFB366.toInt(), 0xFFFFAA00.toInt(), 0xFFFF8844.toInt(), 0xFFFFCC66.toInt(),
            0xFFFF9955.toInt(), 0xFFFFAA00.toInt(), 0xFFFFB366.toInt()
        )
        // Shipped hangar lamp palette (BarPageRenderer.drawBarArea).
        private val RAINBOW = listOf(
            0xFFFF0066.toInt(), 0xFF00FF88.toInt(), 0xFFFFAA00.toInt(), 0xFF00AAFF.toInt(),
            0xFFFF00FF.toInt(), 0xFF88FF00.toInt(), 0xFFFF8800.toInt()
        )
        private val RED = List(7) { CORRUPTION_RED }

        // Cached per stage — forStage is consulted every walker-update tick and render frame.
        private val NORMAL_DRESSING = BarDressing(
            lampColors = WARM, signMode = SignMode.STEADY,
            stringLights = true, fairyLights = false,
            seatedCrew = true, confetti = false
        )
        private val CORRUPTION_DRESSING = BarDressing(
            lampColors = RED, signMode = SignMode.BLINKING,
            stringLights = false, fairyLights = false,
            seatedCrew = false, confetti = false
        )
        private val ASTRO_LOOP_DRESSING = BarDressing(
            lampColors = RAINBOW, signMode = SignMode.STEADY,
            stringLights = false, fairyLights = true,
            seatedCrew = true, confetti = true
        )

        fun forStage(stage: StoryStage): BarDressing = when (stage) {
            StoryStage.NORMAL -> NORMAL_DRESSING
            StoryStage.CORRUPTION -> CORRUPTION_DRESSING
            StoryStage.ASTRO_LOOP -> ASTRO_LOOP_DRESSING
        }
    }
}
