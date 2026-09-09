package com.usbmediaexplorer.data.store

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonStoreMigrationTest {
    @Test
    fun `legacy root migrates once and remains idempotent`() {
        val legacy = JSONObject().put("items", JSONObject())
        val migrated = JsonStoreMigrations.migrate(legacy)
        assertEquals(2, migrated.getInt("schemaVersion"))
        val timestamp = migrated.getLong("migratedAt")
        assertEquals(timestamp, JsonStoreMigrations.migrate(migrated).getLong("migratedAt"))
    }

    @Test
    fun `newer schema is not downgraded`() {
        val future = JSONObject().put("schemaVersion", 9)
        assertEquals(9, JsonStoreMigrations.migrate(future).getInt("schemaVersion"))
    }
}
