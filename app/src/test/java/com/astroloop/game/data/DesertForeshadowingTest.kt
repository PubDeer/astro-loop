package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

class DesertForeshadowingTest {

    @Test
    fun belowSixIsNull() {
        for (c in 0..5) assertNull("count=$c should be null", LoopDefinitions.desertForeshadowing(c))
    }

    @Test
    fun tierBoundaries() {
        val t1 = LoopDefinitions.desertForeshadowing(6)
        assertSame(t1, LoopDefinitions.desertForeshadowing(8))
        val t2 = LoopDefinitions.desertForeshadowing(9)
        assertSame(t2, LoopDefinitions.desertForeshadowing(10))
        val t3 = LoopDefinitions.desertForeshadowing(11)
        assertSame(t3, LoopDefinitions.desertForeshadowing(12))
        assertNotSame(t1, t2)
        assertNotSame(t2, t3)
    }

    @Test
    fun everyTierHasBothVoices() {
        for (c in listOf(6, 9, 11)) {
            val tier = LoopDefinitions.desertForeshadowing(c)!!
            assertTrue("count=$c tobar empty", tier.tobarLines.isNotEmpty())
            assertTrue("count=$c pilot empty", tier.pilotLines.isNotEmpty())
        }
    }
}
