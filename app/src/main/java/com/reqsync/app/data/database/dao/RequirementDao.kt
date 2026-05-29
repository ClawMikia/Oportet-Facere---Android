package com.reqsync.app.data.database.dao

import androidx.room.*
import com.reqsync.app.data.database.entities.RequirementCategory
import com.reqsync.app.data.database.entities.RequirementItem
import com.reqsync.app.data.database.entities.RequirementStatus
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// RequirementCategoryDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface RequirementCategoryDao {

    @Query("SELECT * FROM requirement_categories ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllCategories(): Flow<List<RequirementCategory>>

    @Query("SELECT * FROM requirement_categories WHERE sessionId = :sessionId ORDER BY sortOrder ASC")
    fun getCategoriesBySession(sessionId: Long): Flow<List<RequirementCategory>>

    @Query("SELECT * FROM requirement_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): RequirementCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: RequirementCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<RequirementCategory>): List<Long>

    @Update
    suspend fun updateCategory(category: RequirementCategory)

    @Delete
    suspend fun deleteCategory(category: RequirementCategory)

    @Query("DELETE FROM requirement_categories WHERE sessionId = :sessionId")
    suspend fun deleteCategoriesBySession(sessionId: Long)

    @Query("UPDATE requirement_categories SET isExpanded = :expanded WHERE id = :id")
    suspend fun setExpanded(id: Long, expanded: Boolean)
}

// ─────────────────────────────────────────────────────────────────────────────
// RequirementItemDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface RequirementItemDao {

    @Query("SELECT * FROM requirement_items WHERE categoryId = :categoryId ORDER BY sortOrder ASC")
    fun getItemsByCategory(categoryId: Long): Flow<List<RequirementItem>>

    @Query("SELECT * FROM requirement_items ORDER BY sortOrder ASC")
    fun getAllItems(): Flow<List<RequirementItem>>

    @Query("SELECT * FROM requirement_items WHERE id = :id")
    suspend fun getItemById(id: Long): RequirementItem?

    @Query("SELECT * FROM requirement_items WHERE status = :status")
    fun getItemsByStatus(status: RequirementStatus): Flow<List<RequirementItem>>

    @Query("""
        SELECT * FROM requirement_items 
        WHERE title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
        ORDER BY sortOrder ASC
    """)
    fun searchItems(query: String): Flow<List<RequirementItem>>

    @Query("SELECT COUNT(*) FROM requirement_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM requirement_items WHERE status = 'COMPLETED'")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM requirement_items WHERE status = 'PENDING' OR status = 'IN_PROGRESS'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM requirement_items WHERE categoryId = :categoryId")
    fun getTotalCountByCategory(categoryId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM requirement_items WHERE categoryId = :categoryId AND status = 'COMPLETED'")
    fun getCompletedCountByCategory(categoryId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: RequirementItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<RequirementItem>): List<Long>

    @Update
    suspend fun updateItem(item: RequirementItem)

    @Delete
    suspend fun deleteItem(item: RequirementItem)

    @Query("DELETE FROM requirement_items WHERE categoryId = :categoryId")
    suspend fun deleteItemsByCategory(categoryId: Long)

    @Query("""
        UPDATE requirement_items 
        SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateStatus(id: Long, status: RequirementStatus, completedAt: Long?, updatedAt: Long)
}
