package com.reqsync.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a grouped category of requirements (e.g. "Pre-Employment Medical (PEME)").
 * Each category holds multiple [RequirementItem] entries.
 */
@Entity(tableName = "requirement_categories")
data class RequirementCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val colorTag: String = "#00F5FF",   // neon cyan default
    val iconName: String = "ic_folder", // drawable name
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isExpanded: Boolean = true,
    val sessionId: Long = 0             // links to a parse session
)
