package com.astroloop.game.data

import android.content.Context
import android.content.SharedPreferences

class HighScoreManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    fun getBestTime(): Float {
        return prefs.getFloat(KEY_BEST_TIME, 0f)
    }

    fun saveBestTime(time: Float): Boolean {
        val currentBest = getBestTime()
        if (time > currentBest) {
            prefs.edit().putFloat(KEY_BEST_TIME, time).apply()
            return true
        }
        return false
    }

    fun getHighScore(): Int {
        return prefs.getInt(KEY_HIGH_SCORE, 0)
    }

    fun saveHighScore(score: Int): Boolean {
        val currentBest = getHighScore()
        if (score > currentBest) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
            return true
        }
        return false
    }

    fun getTotalGamesPlayed(): Int {
        return prefs.getInt(KEY_GAMES_PLAYED, 0)
    }

    fun incrementGamesPlayed() {
        val current = getTotalGamesPlayed()
        prefs.edit().putInt(KEY_GAMES_PLAYED, current + 1).apply()
    }

    fun getTotalPlayTime(): Float {
        return prefs.getFloat(KEY_TOTAL_PLAY_TIME, 0f)
    }

    fun addPlayTime(time: Float) {
        val current = getTotalPlayTime()
        prefs.edit().putFloat(KEY_TOTAL_PLAY_TIME, current + time).apply()
    }

    fun getHighestEvolutionCount(): Int {
        return prefs.getInt(KEY_MAX_EVOLUTIONS, 0)
    }

    fun updateHighestEvolutionCount(count: Int) {
        val current = getHighestEvolutionCount()
        if (count > current) {
            prefs.edit().putInt(KEY_MAX_EVOLUTIONS, count).apply()
        }
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "asteroid_survivor_prefs"
        private const val KEY_BEST_TIME = "best_time"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_TOTAL_PLAY_TIME = "total_play_time"
        private const val KEY_MAX_EVOLUTIONS = "max_evolutions"
    }
}
