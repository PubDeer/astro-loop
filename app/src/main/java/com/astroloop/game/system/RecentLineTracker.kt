package com.astroloop.game.system

import kotlin.random.Random

/**
 * Per-run memory that keeps radio chatter from repeating the same line text.
 * Within a key (pilot+event), a line is not shown again until that key's pool
 * is exhausted; across all keys, the same text never fires twice in a row.
 */
class RecentLineTracker {

    private val shownPerKey = mutableMapOf<String, MutableSet<String>>()
    private var lastText: String? = null

    fun pick(key: String, pool: List<String>, rng: Random = Random.Default): String? {
        if (pool.isEmpty()) return null
        val shown = shownPerKey.getOrPut(key) { mutableSetOf() }
        var candidates = pool.filter { it !in shown && it != lastText }
        if (candidates.isEmpty()) {
            shown.clear()
            candidates = pool.filter { it != lastText }
            if (candidates.isEmpty()) candidates = pool
        }
        val choice = candidates.random(rng)
        shown.add(choice)
        lastText = choice
        return choice
    }

    fun reset() {
        shownPerKey.clear()
        lastText = null
    }
}
