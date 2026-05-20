package com.kangwei.expensetracker.data.db.dao

import androidx.room.*
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun expenseCount(categoryId: Long): Int

    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId AND isIncome = 0 AND date >= :monthStart")
    suspend fun monthSpent(categoryId: Long, monthStart: Long): Double?
}
