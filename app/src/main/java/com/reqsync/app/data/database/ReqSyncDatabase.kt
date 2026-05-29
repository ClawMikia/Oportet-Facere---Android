package com.reqsync.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.reqsync.app.data.database.dao.*
import com.reqsync.app.data.database.entities.*
import com.reqsync.app.data.database.migrations.MIGRATION_1_2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        RequirementCategory::class,
        RequirementItem::class,
        UserProgress::class,
        Achievement::class,
        Reminder::class,
        Note::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ReqSyncDatabase : RoomDatabase() {

    abstract fun requirementCategoryDao(): RequirementCategoryDao
    abstract fun requirementItemDao(): RequirementItemDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: ReqSyncDatabase? = null

        fun getInstance(context: Context): ReqSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReqSyncDatabase::class.java,
                    "reqsync_database"
                )
                    .addCallback(DatabaseCallback())
                    // .addMigrations(MIGRATION_1_2) // enable when needed
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Seeds default achievements and user progress on first creation.
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedUserProgress(database.userProgressDao())
                    seedAchievements(database.achievementDao())
                }
            }
        }

        private suspend fun seedUserProgress(dao: UserProgressDao) {
            dao.insertOrUpdate(UserProgress())
        }

        private suspend fun seedAchievements(dao: AchievementDao) {
            val achievements = listOf(
                Achievement(key = "FIRST_MISSION",     title = "First Mission",      description = "Complete your first requirement",          iconName = "ic_badge_first",    xpReward = 100,  category = AchievementCategory.GENERAL),
                Achievement(key = "SPEED_RUN",         title = "Speed Runner",       description = "Complete 5 tasks in a single day",         iconName = "ic_badge_speed",    xpReward = 200,  category = AchievementCategory.SPEED),
                Achievement(key = "STREAK_3",          title = "Momentum",           description = "Maintain a 3-day streak",                  iconName = "ic_badge_streak3",  xpReward = 150,  category = AchievementCategory.STREAK),
                Achievement(key = "STREAK_7",          title = "Unstoppable",        description = "Maintain a 7-day streak",                  iconName = "ic_badge_streak7",  xpReward = 300,  category = AchievementCategory.STREAK),
                Achievement(key = "STREAK_30",         title = "Legendary",          description = "Maintain a 30-day streak",                 iconName = "ic_badge_legend",   xpReward = 1000, category = AchievementCategory.STREAK),
                Achievement(key = "COMPLETE_10",       title = "Operative",          description = "Complete 10 total requirements",           iconName = "ic_badge_10",       xpReward = 250,  category = AchievementCategory.COMPLETION),
                Achievement(key = "COMPLETE_50",       title = "Specialist",         description = "Complete 50 total requirements",           iconName = "ic_badge_50",       xpReward = 500,  category = AchievementCategory.COMPLETION),
                Achievement(key = "COMPLETE_100",      title = "Elite Agent",        description = "Complete 100 total requirements",          iconName = "ic_badge_100",      xpReward = 1000, category = AchievementCategory.COMPLETION),
                Achievement(key = "FULL_MISSION",      title = "Mission Clear",      description = "Complete an entire category",             iconName = "ic_badge_mission",  xpReward = 400,  category = AchievementCategory.GENERAL),
                Achievement(key = "PARSER_MASTER",     title = "Parser Master",      description = "Parse 5 different requirement sets",      iconName = "ic_badge_parser",   xpReward = 300,  category = AchievementCategory.SPECIAL),
                Achievement(key = "NIGHT_OWL",         title = "Night Owl",          description = "Complete tasks after midnight",            iconName = "ic_badge_owl",      xpReward = 150,  category = AchievementCategory.SPECIAL),
                Achievement(key = "LEVEL_5",           title = "Rising Star",        description = "Reach Level 5",                           iconName = "ic_badge_lv5",      xpReward = 200,  category = AchievementCategory.GENERAL),
                Achievement(key = "LEVEL_10",          title = "Cyber Warrior",      description = "Reach Level 10",                          iconName = "ic_badge_lv10",     xpReward = 500,  category = AchievementCategory.GENERAL),
                Achievement(key = "LEVEL_25",          title = "Ghost Protocol",     description = "Reach Level 25",                          iconName = "ic_badge_lv25",     xpReward = 2000, category = AchievementCategory.GENERAL)
            )
            dao.insertAll(achievements)
        }
    }
}
