package com.astroloop.game.core

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StoryStateManagerTest {
    private lateinit var p: PersistenceManager

    @Before fun setup() {
        p = PersistenceManager(ApplicationProvider.getApplicationContext())
        p.resetAllProgress()
    }

    @Test fun `normal stage`() {
        p.setStoryStageCode(StoryStage.NORMAL.code)
        assertEquals(StoryStage.NORMAL, StoryStateManager.stage(p))
        assertFalse(StoryStateManager.isCorrupted(p))
        assertFalse(StoryStateManager.isAstroLoop(p))
        assertEquals("normal", StoryStateManager.stageMusicSet(p))
    }

    @Test fun `corruption stage is corrupted only`() {
        p.setStoryStageCode(StoryStage.CORRUPTION.code)
        assertTrue(StoryStateManager.isCorrupted(p))
        assertFalse(StoryStateManager.isAstroLoop(p))
        assertEquals("corruption", StoryStateManager.stageMusicSet(p))
    }

    @Test fun `astro loop is not corrupted`() {
        p.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        assertFalse("astro-loop must NOT read as corrupted", StoryStateManager.isCorrupted(p))
        assertTrue(StoryStateManager.isAstroLoop(p))
        assertEquals("astroloop", StoryStateManager.stageMusicSet(p))
    }

    @Test fun `hasLoopedBefore is loop greater-equal 2`() {
        p.setStoryLoop(1); assertFalse(StoryStateManager.hasLoopedBefore(p))
        p.setStoryLoop(2); assertTrue(StoryStateManager.hasLoopedBefore(p))
    }
}
