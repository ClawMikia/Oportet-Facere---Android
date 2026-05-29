package com.reqsync.app

import android.app.Application
import com.reqsync.app.data.database.ReqSyncDatabase
import com.reqsync.app.data.repository.*
import com.reqsync.app.utils.NotificationHelper
import com.reqsync.app.utils.PreferencesDataStore

/**
 * Application class — initialises the database, repositories, and
 * notification channels. Acts as a simple manual DI container.
 */
class ReqSyncApp : Application() {

    // Lazy singletons
    val database by lazy { ReqSyncDatabase.getInstance(this) }

    val requirementRepository by lazy {
        RequirementRepository(
            database.requirementCategoryDao(),
            database.requirementItemDao()
        )
    }

    val gamificationRepository by lazy {
        GamificationRepository(
            database.userProgressDao(),
            database.achievementDao()
        )
    }

    val noteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    val reminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }

    val preferences by lazy { PreferencesDataStore(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
