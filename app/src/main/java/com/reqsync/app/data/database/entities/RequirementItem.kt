package com.reqsync.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single checklist requirement belonging to a [RequirementCategory].
 * Tracks completion status, priority, XP reward, and due date.
 */
@Entity(
    tableName = "requirement_items",
    foreignKeys = [
        ForeignKey(
            entity = RequirementCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class RequirementItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val description: String = "",
    val status: RequirementStatus = RequirementStatus.PENDING,
    val priority: Priority = Priority.NORMAL,
    val xpReward: Int = 50,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hasNote: Boolean = false,
    val hasAttachment: Boolean = false,
    val isOptional: Boolean = false
)

enum class RequirementStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    OVERDUE,
    SKIPPED
}

enum class Priority {
    LOW, NORMAL, HIGH, CRITICAL
}
