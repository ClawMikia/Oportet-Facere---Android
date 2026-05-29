package com.reqsync.app.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Example migration from version 1 → 2 (add future columns here). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example: database.execSQL("ALTER TABLE requirement_items ADD COLUMN tagColor TEXT DEFAULT '#00F5FF'")
    }
}
