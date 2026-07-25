package com.astroloop.game.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PersistenceMigrationTest {

    @Test
    fun `torpedo_storm migrates to hunter_killer`() {
        val migrated = PersistenceManager.migrateWeaponIds(setOf("torpedo_storm", "railgun"))
        assertEquals(setOf("hunter_killer", "railgun"), migrated)
    }

    @Test
    fun `unrelated ids pass through`() {
        val ids = setOf("warp_saw", "oblivion_beam")
        assertEquals(ids, PersistenceManager.migrateWeaponIds(ids))
    }
}
