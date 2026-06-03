package com.reqsync.app.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migration from version 1 → 2: Add isArchived column to requirement_categories. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE requirement_categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}

/** Migration from version 2 → 3: Add isArchived column to requirement_items. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE requirement_items ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}
