package com.usbmediaexplorer.data.store

import org.json.JSONObject

object JsonStoreMigrations {
    const val CURRENT_SCHEMA_VERSION = 2

    /** Idempotent migration for all legacy stores that had no explicit schema. */
    fun migrate(root: JSONObject): JSONObject = root.apply {
        if (optInt("schemaVersion", 1) < CURRENT_SCHEMA_VERSION) {
            put("schemaVersion", CURRENT_SCHEMA_VERSION)
            put("migratedAt", System.currentTimeMillis())
        }
    }
}
