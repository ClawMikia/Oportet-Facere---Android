package com.reqsync.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// UserProgress — gamification state
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1, // singleton row
    val totalXp: Int = 0,
    val level: Int = 1,
    val rank: String = "RECRUIT",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Long = 0L,
    val totalCompleted: Int = 0,
    val totalItems: Int = 0,
    val missionsCompleted: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────────────────
// Achievement — unlockable badges
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,            // unique identifier e.g. "FIRST_COMPLETE"
    val title: String,
    val description: String,
    val iconName: String,
    val xpReward: Int = 100,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val category: AchievementCategory = AchievementCategory.GENERAL
)

enum class AchievementCategory {
    GENERAL, STREAK, COMPLETION, SPEED, SOCIAL, SPECIAL
}

// ─────────────────────────────────────────────────────────────────────────────
// Reminder — scheduled notifications per requirement item
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = RequirementItem::class,
            parentColumns = ["id"],
            childColumns = ["requirementItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("requirementItemId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requirementItemId: Long,
    val title: String,
    val message: String,
    val scheduledAt: Long,          // epoch millis
    val isRepeating: Boolean = false,
    val repeatIntervalHours: Int = 24,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────────────────
// Note — per-requirement notes / comments
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = RequirementItem::class,
            parentColumns = ["id"],
            childColumns = ["requirementItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("requirementItemId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requirementItemId: Long,
    val content: String,
    val attachmentUri: String? = null,  // local file URI placeholder
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AttachmentType {
    NONE, IMAGE, PDF, DOCUMENT
}
