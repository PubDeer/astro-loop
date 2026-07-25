package com.astroloop.game.system

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RecentLineTrackerTest {

    @Test
    fun `pick returns null for an empty pool`() {
        val t = RecentLineTracker()
        assertNull(t.pick("k", emptyList()))
    }

    @Test
    fun `pick cycles through the whole pool before repeating`() {
        val t = RecentLineTracker()
        val pool = listOf("a", "b", "c")
        val rng = Random(1)
        val first3 = setOf(t.pick("k", pool, rng), t.pick("k", pool, rng), t.pick("k", pool, rng))
        assertEquals("all three distinct lines should appear before any repeat", setOf("a", "b", "c"), first3)
    }

    @Test
    fun `pick never returns the same text twice in a row across keys`() {
        val t = RecentLineTracker()
        assertEquals("a", t.pick("k1", listOf("a")))      // forces lastText = "a"
        assertEquals("b", t.pick("k2", listOf("a", "b"))) // "a" excluded as immediate repeat
    }

    @Test
    fun `reset clears memory so previously shown lines can return`() {
        val t = RecentLineTracker()
        val single = listOf("only")
        assertEquals("only", t.pick("k", single))
        t.reset()
        assertEquals("only", t.pick("k", single)) // available again after reset
    }
}
