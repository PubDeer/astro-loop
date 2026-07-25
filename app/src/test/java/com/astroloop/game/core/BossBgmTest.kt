package com.astroloop.game.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * startBossBGM runs on the boss-spawn frame BEFORE bossFightPhase is set, and GameThread
 * swallows anything update() throws — so a throw here silently freezes the entire TB-26
 * sacrifice sequence (boss spawns and fires, but no flyout/charge/fleet/ram ever comes).
 *
 * That exact freeze shipped once: bgm_boss was wired code-only (d77be4e5) with no asset,
 * and MediaPlayer.create(context, 0) threw Resources.NotFoundException on device. These
 * tests lock in both halves of the fix: the track must exist in res/raw, and the call
 * must never throw even if it goes missing again (startBossBGM guards resId == 0).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BossBgmTest {

    @Test
    fun bgmBossTrackShipsInResRaw() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier("bgm_boss", "raw", context.packageName)
        assertNotEquals("bgm_boss.ogg is missing from res/raw — the 10-minute boss fight " +
            "choreography freezes without it (see startBossBGM)", 0, resId)
    }

    @Test
    fun startBossBgmDoesNotThrow() {
        SoundManager.startBossBGM(ApplicationProvider.getApplicationContext())
    }
}
