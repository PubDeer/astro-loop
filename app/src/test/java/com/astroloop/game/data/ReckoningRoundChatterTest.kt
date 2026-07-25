package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ReckoningRoundChatterTest {

    private val all = ReckoningRoundChatter.rounds + ReckoningRoundChatter.lostCountPool

    @Test
    fun twentyAuthoredRoundsAndFourPoolConversations() {
        assertEquals(20, ReckoningRoundChatter.ROUND_COUNT)
        assertEquals(20, ReckoningRoundChatter.rounds.size)
        assertEquals(4, ReckoningRoundChatter.lostCountPool.size)
    }

    @Test
    fun roundsOneThroughTwentyMapInOrder() {
        for (n in 1..20) {
            val (convo, poolIdx) = ReckoningRoundChatter.forRound(n, lastPoolIndex = -1)
            assertSame("round $n must map to rounds[${n - 1}]", ReckoningRoundChatter.rounds[n - 1], convo)
            assertEquals(-1, poolIdx)
        }
    }

    @Test
    fun roundZeroOrNegativeClampsToRoundOne() {
        assertSame(ReckoningRoundChatter.rounds[0], ReckoningRoundChatter.forRound(0, -1).first)
        assertSame(ReckoningRoundChatter.rounds[0], ReckoningRoundChatter.forRound(-3, -1).first)
    }

    @Test
    fun roundsPastTwentyUseThePool() {
        for (n in 21..40) {
            val (convo, poolIdx) = ReckoningRoundChatter.forRound(n, lastPoolIndex = -1, random = Random(n))
            assertTrue("round $n must return a pool index", poolIdx in 0..3)
            assertSame(ReckoningRoundChatter.lostCountPool[poolIdx], convo)
        }
    }

    @Test
    fun poolNeverRepeatsTheLastIndex() {
        for (last in 0..3) {
            repeat(50) { seed ->
                val (_, poolIdx) = ReckoningRoundChatter.forRound(25, lastPoolIndex = last, random = Random(seed))
                assertNotEquals("pool repeated index $last", last, poolIdx)
            }
        }
    }

    @Test
    fun freshPoolIndexExcludesNothing() {
        val seen = (0 until 200).map { seed ->
            ReckoningRoundChatter.forRound(25, lastPoolIndex = -1, random = Random(seed)).second
        }.toSet()
        assertEquals("with no history all four pool entries must be reachable", setOf(0, 1, 2, 3), seen)
    }

    @Test
    fun everyConversationEndsOnAnAstroRetryLine() {
        all.forEachIndexed { i, convo ->
            assertTrue("conversation $i is empty", convo.isNotEmpty())
            assertEquals("conversation $i must end on ASTRO (the retry button)", "ASTRO", convo.last().first)
        }
    }

    @Test
    fun onlyTobarAndAstroSpeak() {
        all.flatten().forEach { (speaker, _) ->
            assertTrue("unexpected speaker $speaker", speaker == "TOBAR" || speaker == "ASTRO")
        }
    }

    @Test
    fun allLinesFitTheChatColumn() {
        val limit = 58
        all.flatten().forEach { (s, l) ->
            assertTrue("$s: \"$l\" is ${l.length} chars (limit $limit)", l.length <= limit)
        }
    }

    @Test
    fun roundOneIsTheOriginalLostScript() {
        // Guards the spec's "verbatim" promise independently of CrystalFightLines
        // (whose barChatterLost has been removed).
        assertEquals(
            listOf(
                "TOBAR" to "You're back early.",
                "TOBAR" to "Something out there's still awake, isn't it.",
                "ASTRO" to "…I'm going back out."
            ),
            ReckoningRoundChatter.rounds[0]
        )
    }
}
