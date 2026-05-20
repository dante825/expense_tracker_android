package com.kangwei.expensetracker.data.db.dao

import androidx.room.*
import com.kangwei.expensetracker.data.db.entity.RecurringExpenseEntity
import com.kangwei.expensetracker.data.db.relation.RecurringWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Transaction
    @Query("SELECT * FROM recurring_expenses ORDER BY nextDate ASC")
    fun getAllWithCategoryFlow(): Flow<List<RecurringWithCategory>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1")
    suspend fun getActiveRecurrings(): List<RecurringExpenseEntity>

    @Insert
    suspend fun insert(recurring: RecurringExpenseEntity): Long

    @Update
    suspend fun update(recurring: RecurringExpenseEntity)

    @Delete
    suspend fun delete(recurring: RecurringExpenseEntity)
}
