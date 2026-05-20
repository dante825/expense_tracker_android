package com.kangwei.expensetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kangwei.expensetracker.data.db.dao.*
import com.kangwei.expensetracker.data.db.entity.*

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        RecurringExpenseEntity::class,
        TagEntity::class,
        ExpenseTagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db"
                ).build().also { INSTANCE = it }
            }
    }
}
