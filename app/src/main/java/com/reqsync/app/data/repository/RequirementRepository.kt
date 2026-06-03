package com.reqsync.app.data.repository

import com.reqsync.app.data.database.dao.RequirementCategoryDao
import com.reqsync.app.data.database.dao.RequirementItemDao
import com.reqsync.app.data.database.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for requirement categories and items.
 * All UI layers interact with data through this repository.
 */
class RequirementRepository(
    private val categoryDao: RequirementCategoryDao,
    private val itemDao: RequirementItemDao
) {

    // ── Categories ────────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<RequirementCategory>> =
        categoryDao.getAllCategories()

    fun getCategoriesBySession(sessionId: Long): Flow<List<RequirementCategory>> =
        categoryDao.getCategoriesBySession(sessionId)

    suspend fun getCategoryById(id: Long): RequirementCategory? =
        categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: RequirementCategory): Long =
        categoryDao.insertCategory(category)

    suspend fun insertCategories(categories: List<RequirementCategory>): List<Long> =
        categoryDao.insertCategories(categories)

    suspend fun updateCategory(category: RequirementCategory) =
        categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: RequirementCategory) =
        categoryDao.deleteCategory(category)

    suspend fun toggleCategoryExpanded(id: Long, expanded: Boolean) =
        categoryDao.setExpanded(id, expanded)

    suspend fun archiveCategory(id: Long, archived: Boolean = true) =
        categoryDao.setArchived(id, archived)

    suspend fun archiveItem(id: Long, archived: Boolean = true) =
        itemDao.setArchived(id, archived)

    fun getArchivedCategories(): Flow<List<RequirementCategory>> =
        categoryDao.getArchivedCategories()

    // ── Items ─────────────────────────────────────────────────────────────────

    fun getAllItems(): Flow<List<RequirementItem>> =
        itemDao.getAllItems()

    fun getActiveItems(): Flow<List<RequirementItem>> =
        itemDao.getActiveItems()

    fun getItemsByCategory(categoryId: Long): Flow<List<RequirementItem>> =
        itemDao.getItemsByCategory(categoryId)

    fun getItemsByStatus(status: RequirementStatus): Flow<List<RequirementItem>> =
        itemDao.getItemsByStatus(status)

    fun searchItems(query: String): Flow<List<RequirementItem>> =
        itemDao.searchItems(query)

    fun getTotalCount(): Flow<Int> = itemDao.getTotalCount()

    fun getCompletedCount(): Flow<Int> = itemDao.getCompletedCount()

    fun getPendingCount(): Flow<Int> = itemDao.getPendingCount()

    fun getTotalCountByCategory(categoryId: Long): Flow<Int> =
        itemDao.getTotalCountByCategory(categoryId)

    fun getCompletedCountByCategory(categoryId: Long): Flow<Int> =
        itemDao.getCompletedCountByCategory(categoryId)

    suspend fun getItemById(id: Long): RequirementItem? =
        itemDao.getItemById(id)

    suspend fun insertItem(item: RequirementItem): Long =
        itemDao.insertItem(item)

    suspend fun insertItems(items: List<RequirementItem>): List<Long> =
        itemDao.insertItems(items)

    suspend fun updateItem(item: RequirementItem) =
        itemDao.updateItem(item)

    suspend fun deleteItem(item: RequirementItem) =
        itemDao.deleteItem(item)

    suspend fun updateStatus(id: Long, status: RequirementStatus) {
        val completedAt = if (status == RequirementStatus.COMPLETED)
            System.currentTimeMillis() else null
        itemDao.updateStatus(id, status, completedAt, System.currentTimeMillis())
    }

    /**
     * Bulk insert a parsed session: categories + their items atomically.
     * Returns a map of category index → inserted category ID.
     */
    suspend fun saveParsedSession(
        categories: List<RequirementCategory>,
        itemsByCategory: Map<Int, List<RequirementItem>>
    ): Map<Int, Long> {
        val categoryIds = mutableMapOf<Int, Long>()
        categories.forEachIndexed { index, category ->
            val catId = insertCategory(category)
            categoryIds[index] = catId
            val items = itemsByCategory[index]?.map { it.copy(categoryId = catId) } ?: emptyList()
            if (items.isNotEmpty()) insertItems(items)
        }
        return categoryIds
    }
}
