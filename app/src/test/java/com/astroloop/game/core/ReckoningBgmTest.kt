package com.astroloop.game.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * startReckoningBGM runs on the reckoning boss-spawn frame, and GameThread swallows
 * anything update() throws — a throw here would silently freeze the fight entrance
 * (same failure mode as the shipped bgm_boss freeze, see BossBgmTest). These lock in
 * the guards: a missing track bails out cleanly, duck/fade are safe with no player.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReckoningBgmTest {

    @Test
    fun startReckoningBgmDoesNotThrow() {
        SoundManager.startReckoningBGM(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun duckAndFadeOutDoNotThrowWithoutAPlayer() {
        SoundManager.duckBossBGM(0.35f)
        SoundManager.fadeOutBossBGM()
    }

    @Test
    fun bgmReckoningTrackShipsInResRaw() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier("bgm_reckoning", "raw", context.packageName)
        assertNotEquals("bgm_reckoning.ogg is missing from res/raw — the reckoning keeps the " +
            "combat loop and the layer instruments ride the wrong bed (see startReckoningBGM)",
            0, resId)
    }
}
