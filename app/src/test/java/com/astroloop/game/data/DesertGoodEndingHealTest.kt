package com.astroloop.game.data

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Boot heal for saves that entered ASTRO_LOOP before the desert good ending
 * wrote desert_good_ending: the stage itself proves the good ending happened,
 * so the flag must be set — otherwise isPostHorrorRun stays true forever and
 * every astro-loop run uses the post-horror "loop-aware" radio/bar lines.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DesertGoodEndingHealTest {
    private lateinit var p: PersistenceManager

    @Before fun setup() {
        p = PersistenceManager(ApplicationProvider.getApplicationContext())
        p.resetAllProgress()
    }

    @Test fun `astro-loop save missing the good-ending flag is healed`() {
        p.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        p.setDesertCompleted()
        p.healDesertGoodEnding()
        assertTrue(p.hasDesertGoodEnding())
    }

    @Test fun `normal-stage post-horror save is left alone`() {
        p.setStoryStageCode(StoryStage.NORMAL.code)
        p.setDesertCompleted()   // horror ending reached, good ending not yet
        p.healDesertGoodEnding()
        assertFalse(p.hasDesertGoodEnding())
    }

    @Test fun `corruption-stage save is left alone`() {
        p.setStoryStageCode(StoryStage.CORRUPTION.code)
        p.setDesertCompleted()
        p.healDesertGoodEnding()
        assertFalse(p.hasDesertGoodEnding())
    }

    @Test fun `heal is idempotent`() {
        p.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        p.setDesertCompleted()
        p.healDesertGoodEnding()
        p.healDesertGoodEnding()
        assertTrue(p.hasDesertGoodEnding())
    }
}
