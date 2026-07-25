package com.astroloop.game.data

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StoryStateMigrationTest {
    private lateinit var p: PersistenceManager

    @Before fun setup() {
        p = PersistenceManager(ApplicationProvider.getApplicationContext())
        p.resetAllProgress()
    }

    @Test fun `loop accessor defaults to 1 and caps at 3`() {
        assertEquals(1, p.getStoryLoop())
        p.setStoryLoop(5); assertEquals(3, p.getStoryLoop())
        p.setStoryLoop(0); assertEquals(1, p.getStoryLoop())
        p.setStoryLoop(2); p.incrementStoryLoop(); assertEquals(3, p.getStoryLoop())
        p.incrementStoryLoop(); assertEquals(3, p.getStoryLoop())
    }

    @Test fun `migration maps astro_loop_mode true to ASTRO_LOOP`() {
        p.prefsForTest().edit()
            .putBoolean("astro_loop_mode", true).putInt("story_phase", 0)
            .putInt("narrative_loop", 2).putBoolean("story_state_migrated", false).apply()
        p.migrateStoryState()
        assertEquals(StoryStage.ASTRO_LOOP.code, p.getStoryStageCode())
        assertEquals(2, p.getStoryLoop())
    }

    @Test fun `migration maps old NG_PLUS phase 2 to CORRUPTION`() {
        p.prefsForTest().edit()
            .putBoolean("astro_loop_mode", false).putInt("story_phase", 2)
            .putBoolean("story_state_migrated", false).apply()
        p.migrateStoryState()
        assertEquals(StoryStage.CORRUPTION.code, p.getStoryStageCode())
    }

    @Test fun `migration carries normal and corruption phases through`() {
        p.prefsForTest().edit().putInt("story_phase", 1)
            .putBoolean("story_state_migrated", false).apply()
        p.migrateStoryState()
        assertEquals(StoryStage.CORRUPTION.code, p.getStoryStageCode())
    }

    @Test fun `resetAllProgress restores story-state keys to defaults`() {
        p.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        p.setStoryLoop(3)
        p.prefsForTest().edit().putBoolean("story_state_migrated", true).apply()
        p.resetAllProgress()
        assertEquals(StoryStage.NORMAL.code, p.getStoryStageCode())
        assertEquals(1, p.getStoryLoop())
    }

    @Test fun `migration is idempotent`() {
        p.prefsForTest().edit().putBoolean("astro_loop_mode", true)
            .putBoolean("story_state_migrated", false).apply()
        p.migrateStoryState()
        p.setStoryStageCode(StoryStage.NORMAL.code) // pretend gameplay moved it
        p.migrateStoryState()                       // must NOT re-migrate
        assertEquals(StoryStage.NORMAL.code, p.getStoryStageCode())
    }
}
