package com.astroloop.game.hangar

import com.astroloop.game.data.BarConversation
import org.junit.Assert.*
import org.junit.Test

class ChatPickerHelpersTest {

    private fun convo(vararg ids: String) =
        BarConversation(ids.toList(), listOf(ChatMessage(ids.first(), "x", 0)))

    @Test
    fun `pageFilter keeps everything on the bar page`() {
        val convos = listOf(convo("pilot_medic", "pilot_rascal"), convo("pilot_dash", "pilot_astro"))
        assertEquals(convos, ChatSystem.pageFilter(convos, isBarPage = true, selectedPilotId = "pilot_dash"))
    }

    @Test
    fun `pageFilter drops selected-pilot convos off the bar page`() {
        val withSel = convo("pilot_dash", "pilot_astro")
        val without = convo("pilot_medic", "pilot_rascal")
        val tb = convo("tb26", "pilot_medic")
        val result = ChatSystem.pageFilter(listOf(withSel, without, tb), isBarPage = false, selectedPilotId = "pilot_dash")
        assertFalse(withSel in result)
        assertTrue(without in result)
        assertTrue(tb in result)
    }

    @Test
    fun `pageFilter drops nothing when no pilot selected`() {
        val convos = listOf(convo("pilot_dash", "pilot_astro"))
        assertEquals(convos, ChatSystem.pageFilter(convos, isBarPage = false, selectedPilotId = null))
    }

    private val W = mapOf(1 to 25, 2 to 40, 3 to 23, 4 to 12)

    @Test
    fun `chooseCategory single eligible always returns it`() {
        assertEquals(3, ChatSystem.chooseCategory(W, setOf(3), 0.0f))
        assertEquals(3, ChatSystem.chooseCategory(W, setOf(3), 0.99f))
    }

    @Test
    fun `chooseCategory respects default weight bands over the full set`() {
        val full = setOf(1, 2, 3, 4) // bands 1:[0,25) 2:[25,65) 3:[65,88) 4:[88,100)
        assertEquals(1, ChatSystem.chooseCategory(W, full, 0.00f))
        assertEquals(1, ChatSystem.chooseCategory(W, full, 0.24f))
        assertEquals(2, ChatSystem.chooseCategory(W, full, 0.25f))
        assertEquals(2, ChatSystem.chooseCategory(W, full, 0.64f))
        assertEquals(3, ChatSystem.chooseCategory(W, full, 0.65f))
        assertEquals(3, ChatSystem.chooseCategory(W, full, 0.87f))
        assertEquals(4, ChatSystem.chooseCategory(W, full, 0.88f))
        assertEquals(4, ChatSystem.chooseCategory(W, full, 0.999f))
    }

    @Test
    fun `chooseCategory renormalizes over the eligible subset`() {
        val sub = setOf(1, 2, 3) // no 4-way: total 88, bands 1:[0,25) 2:[25,65) 3:[65,88)
        assertEquals(1, ChatSystem.chooseCategory(W, sub, 0.0f))
        assertEquals(3, ChatSystem.chooseCategory(W, sub, 0.99f)) // would be 4 in the full set
        for (i in 0..99) assertTrue(ChatSystem.chooseCategory(W, sub, i / 100f) in sub)
    }

    @Test
    fun `pickUnusedLineIndex returns each index once then null`() {
        val used = mutableSetOf<Int>()
        val seen = mutableSetOf<Int>()
        repeat(5) {
            val idx = ChatSystem.pickUnusedLineIndex(5, used, 0.0f)
            assertNotNull(idx); used.add(idx!!); seen.add(idx)
        }
        assertEquals(setOf(0, 1, 2, 3, 4), seen)
        assertNull("exhausted pool must return null (no recycle)", ChatSystem.pickUnusedLineIndex(5, used, 0.5f))
    }

    @Test
    fun `pickUnusedLineIndex handles empty pool`() {
        assertNull(ChatSystem.pickUnusedLineIndex(0, emptySet(), 0.5f))
    }

    @Test
    fun `pickUnusedLineIndex picks within available by roll`() {
        assertEquals(0, ChatSystem.pickUnusedLineIndex(4, setOf(1), 0.0f))   // available [0,2,3] -> first
        assertEquals(3, ChatSystem.pickUnusedLineIndex(4, setOf(1), 0.99f))  // available [0,2,3] -> last
    }
}
