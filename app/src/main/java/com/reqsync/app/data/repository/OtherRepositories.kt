package com.reqsync.app.data.repository

import com.reqsync.app.data.database.dao.*
import com.reqsync.app.data.database.entities.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// GamificationRepository
// ─────────────────────────────────────────────────────────────────────────────
class GamificationRepository(
    private val progressDao: UserProgressDao,
    private val achievementDao: AchievementDao
) {
    // Progress
    fun getProgress(): Flow<UserProgress?> = progressDao.getProgress()
    suspend fun getProgressOnce(): UserProgress? = progressDao.getProgressOnce()
    suspend fun ensureProgressExists() {
        if (progressDao.getProgressOnce() == null) {
            progressDao.insertOrUpdate(UserProgress())
        }
    }

    suspend fun addXp(xp: Int) {
        progressDao.addXp(xp)
        checkLevelUp()
    }

    suspend fun incrementCompleted() = progressDao.incrementCompleted()

    suspend fun removeXp(xp: Int) {
        progressDao.removeXp(xp)
        // Check if we need to level down
        val p = progressDao.getProgressOnce() ?: return
        val currentLevel = calculateLevel(p.totalXp)
        if (currentLevel < p.level) {
            val newLevel = currentLevel
            val rank = getRankForLevel(newLevel)
            progressDao.insertOrUpdate(p.copy(level = newLevel, rank = rank))
        }
    }

    suspend fun decrementCompleted() = progressDao.decrementCompleted()

    private suspend fun checkLevelUp() {
        val p = progressDao.getProgressOnce() ?: return
        val newLevel = calculateLevel(p.totalXp)
        if (newLevel > p.level) {
            val rank = getRankForLevel(newLevel)
            progressDao.insertOrUpdate(p.copy(level = newLevel, rank = rank))
            // Trigger level achievement
            when {
                newLevel >= 25 -> achievementDao.unlockAchievement("LEVEL_25")
                newLevel >= 10 -> achievementDao.unlockAchievement("LEVEL_10")
                newLevel >= 5  -> achievementDao.unlockAchievement("LEVEL_5")
            }
        }
    }

    fun calculateLevel(xp: Int): Int {
        // XP curve: level = floor(1 + sqrt(xp / 100))
        return (1 + Math.sqrt(xp / 100.0)).toInt().coerceAtLeast(1)
    }

    fun xpForNextLevel(currentLevel: Int): Int {
        val nextLevel = currentLevel + 1
        return (nextLevel - 1) * (nextLevel - 1) * 100
    }

    fun getRankForLevel(level: Int): String = when {
        level >= 50 -> "GHOST PROTOCOL"
        level >= 40 -> "SHADOW AGENT"
        level >= 30 -> "CYBER ELITE"
        level >= 25 -> "PHANTOM"
        level >= 20 -> "SPECIALIST"
        level >= 15 -> "OPERATIVE"
        level >= 10 -> "CYBER WARRIOR"
        level >= 7  -> "ENFORCER"
        level >= 5  -> "RISING STAR"
        level >= 3  -> "INITIATE"
        else        -> "RECRUIT"
    }

    suspend fun updateStreak() {
        val p = progressDao.getProgressOnce() ?: return
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        val diff = now - p.lastActiveDate
        val newStreak = if (diff <= oneDayMs * 2) p.currentStreak + 1 else 1
        val longest = maxOf(newStreak, p.longestStreak)
        progressDao.updateStreak(newStreak, longest)
        progressDao.insertOrUpdate(
            p.copy(currentStreak = newStreak, longestStreak = longest, lastActiveDate = now)
        )
        // Streak achievements
        when {
            newStreak >= 30 -> achievementDao.unlockAchievement("STREAK_30")
            newStreak >= 7  -> achievementDao.unlockAchievement("STREAK_7")
            newStreak >= 3  -> achievementDao.unlockAchievement("STREAK_3")
        }
    }

    // Achievements
    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()
    fun getUnlockedAchievements(): Flow<List<Achievement>> = achievementDao.getUnlockedAchievements()
    fun getUnlockedCount(): Flow<Int> = achievementDao.getUnlockedCount()

    suspend fun unlockAchievement(key: String) {
        val a = achievementDao.getByKey(key) ?: return
        if (!a.isUnlocked) {
            achievementDao.unlockAchievement(key)
            addXp(a.xpReward)
        }
    }

    suspend fun checkCompletionAchievements(totalCompleted: Int) {
        when {
            totalCompleted >= 100 -> unlockAchievement("COMPLETE_100")
            totalCompleted >= 50  -> unlockAchievement("COMPLETE_50")
            totalCompleted >= 10  -> unlockAchievement("COMPLETE_10")
            totalCompleted >= 1   -> unlockAchievement("FIRST_MISSION")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NoteRepository
// ─────────────────────────────────────────────────────────────────────────────
class NoteRepository(private val noteDao: NoteDao) {

    fun getNotesByItem(itemId: Long): Flow<List<Note>> = noteDao.getNotesByItem(itemId)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
}

// ─────────────────────────────────────────────────────────────────────────────
// ReminderRepository
// ─────────────────────────────────────────────────────────────────────────────
class ReminderRepository(private val reminderDao: ReminderDao) {

    fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActiveReminders()

    fun getRemindersByItem(itemId: Long): Flow<List<Reminder>> =
        reminderDao.getRemindersByItem(itemId)

    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)

    suspend fun deactivateReminder(id: Long) = reminderDao.deactivateReminder(id)
}
