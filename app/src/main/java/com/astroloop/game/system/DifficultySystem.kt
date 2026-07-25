package com.astroloop.game.system

import com.astroloop.game.core.GameState

class DifficultySystem {

    companion object {
        private const val PEAK_MINUTES = 8.0f
        private const val PEAK_DIFFICULTY = 4.0f
        private const val LINEAR_SLOPE = 0.4f
    }

    fun update(deltaTime: Float, state: GameState) {
        state.survivalTime += deltaTime * state.corruptionTimeMultiplier

        if (state.hasCrystalPowers) {
            state.difficultyMultiplier = 1f
            return
        }

        val minutes = state.survivalTime / 60f

        state.difficultyMultiplier = when {
            state.astroLoopMode && minutes > PEAK_MINUTES ->
                PEAK_DIFFICULTY + LINEAR_SLOPE * (minutes - PEAK_MINUTES)
            state.astroLoopMode ->
                (1f + 0.75f * minutes - 0.046875f * minutes * minutes)
            else ->
                (1f + 0.706f * minutes - 0.0415f * minutes * minutes).coerceAtMost(4.0f)
        }
    }

    fun reset() {
        // Nothing to reset - state handles this
    }
}
