package com.reqsync.app.utils

import com.reqsync.app.data.database.entities.RequirementCategory
import com.reqsync.app.data.database.entities.RequirementItem
import com.reqsync.app.data.database.entities.RequirementStatus

/**
 * Calculates completion stats for a [RequirementCategory] from a flat item list.
 * Used by adapters and ViewModels to avoid extra DB queries.
 */
object CategoryProgressHelper {

    data class CategoryStats(
        val categoryId: Long,
        val total: Int,
        val completed: Int,
        val percent: Int
    ) {
        val progressText: String get() = "$completed / $total"
        val percentText: String get() = "$percent%"
        val isComplete: Boolean get() = total > 0 && completed == total
    }

    fun computeStats(
        categories: List<RequirementCategory>,
        allItems: List<RequirementItem>
    ): Map<Long, CategoryStats> {
        val itemsByCategory = allItems.groupBy { it.categoryId }
        return categories.associate { cat ->
            val items = itemsByCategory[cat.id] ?: emptyList()
            val total = items.size
            val completed = items.count { it.status == RequirementStatus.COMPLETED }
            val percent = if (total > 0) (completed * 100) / total else 0
            cat.id to CategoryStats(
                categoryId = cat.id,
                total = total,
                completed = completed,
                percent = percent
            )
        }
    }

    /** Single-category shortcut. */
    fun computeForCategory(categoryId: Long, items: List<RequirementItem>): CategoryStats {
        val total = items.size
        val completed = items.count { it.status == RequirementStatus.COMPLETED }
        val percent = if (total > 0) (completed * 100) / total else 0
        return CategoryStats(categoryId, total, completed, percent)
    }
}
