package com.kangwei.expensetracker.data.db.dao

import androidx.room.*
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseTagCrossRef
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllWithDetailsFlow(): Flow<List<ExpenseWithDetails>>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: ExpenseTagCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: ExpenseTagCrossRef)

    @Query("DELETE FROM expense_tag_cross_refs WHERE expenseId = :expenseId")
    suspend fun deleteAllTagsForExpense(expenseId: Long)
}
