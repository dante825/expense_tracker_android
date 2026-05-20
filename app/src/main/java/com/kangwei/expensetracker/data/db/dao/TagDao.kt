package com.kangwei.expensetracker.data.db.dao

import androidx.room.*
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.TagWithExpenses
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Transaction
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllWithExpensesFlow(): Flow<List<TagWithExpenses>>

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT COUNT(*) FROM tags WHERE LOWER(name) = LOWER(:name)")
    suspend fun countByName(name: String): Int
}
