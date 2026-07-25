// app/src/test/java/com/astroloop/game/hangar/SignFlickerTest.kt
package com.astroloop.game.hangar

import org.junit.Assert.*
import org.junit.Test

class SignFlickerTest {

    @Test
    fun `dim is always within the flicker envelope`() {
        // Bright branch spans [0.4, 1.0] (0.7 ± 0.3), dark branch is exactly 0.3.
        for (t in 0L..20_000L step 17L) {
            val d = SignFlicker.dim(t)
            assertTrue("dim in range at t=$t: $d", d in 0.3f..1.0001f)
        }
    }

    @Test
    fun `dark flickers occur over a long span`() {
        var sawDark = false
        for (t in 0L..20_000L step 13L) {
            if (SignFlicker.dim(t) == 0.3f) { sawDark = true; break }
        }
        assertTrue("expected at least one dark flicker frame", sawDark)
    }

    @Test
    fun `bright frames are not the dark constant`() {
        // At least some frames are in the bright band, distinct from 0.3.
        val anyBright = (0L..5000L step 7L).any { SignFlicker.dim(it) > 0.4f }
        assertTrue(anyBright)
    }
}
