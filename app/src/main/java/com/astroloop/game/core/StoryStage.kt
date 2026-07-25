package com.astroloop.game.core

/** Where the player is within a narrative loop. Persisted by explicit [code], never ordinal. */
enum class StoryStage(val code: Int) {
    NORMAL(0),       // standard roguelike, pre-Sacrifice
    CORRUPTION(1),   // post-Sacrifice; empty bar, every run is a corruption run
    ASTRO_LOOP(2);   // terminal clarity state (Crystal Astro); loop number irrelevant

    companion object {
        fun fromCode(c: Int): StoryStage = entries.firstOrNull { it.code == c } ?: NORMAL
    }
}
