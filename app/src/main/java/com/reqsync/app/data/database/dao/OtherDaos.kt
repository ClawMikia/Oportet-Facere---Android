package com.reqsync.app.data.database.dao

import androidx.room.*
import com.reqsync.app.data.database.entities.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// UserProgressDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface UserProgressDao {

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getProgress(): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getProgressOnce(): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: UserProgress)

    @Query("UPDATE user_progress SET totalXp = totalXp + :xp, updatedAt = :now WHERE id = 1")
    suspend fun addXp(xp: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_progress SET currentStreak = :streak, longestStreak = :longest WHERE id = 1")
    suspend fun updateStreak(streak: Int, longest: Int)

    @Query("UPDATE user_progress SET totalCompleted = totalCompleted + 1, updatedAt = :now WHERE id = 1")
    suspend fun incrementCompleted(now: Long = System.currentTimeMillis())

    @Query("UPDATE user_progress SET totalXp = totalXp - :xp, updatedAt = :now WHERE id = 1")
    suspend fun removeXp(xp: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_progress SET totalCompleted = totalCompleted - 1, updatedAt = :now WHERE id = 1")
    suspend fun decrementCompleted(now: Long = System.currentTimeMillis())
}

// ─────────────────────────────────────────────────────────────────────────────
// AchievementDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY category ASC, isUnlocked DESC")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :now WHERE `key` = :key")
    suspend fun unlockAchievement(key: String, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedCount(): Flow<Int>
}

// ─────────────────────────────────────────────────────────────────────────────
// ReminderDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY scheduledAt ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE requirementItemId = :itemId")
    fun getRemindersByItem(itemId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isActive = 0 WHERE id = :id")
    suspend fun deactivateReminder(id: Long)
}

// ─────────────────────────────────────────────────────────────────────────────
// NoteDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE requirementItemId = :itemId ORDER BY createdAt DESC")
    fun getNotesByItem(itemId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE requirementItemId = :itemId")
    suspend fun deleteNotesByItem(itemId: Long)
}
