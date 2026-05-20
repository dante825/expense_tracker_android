package com.kangwei.expensetracker.data.seeder

import com.kangwei.expensetracker.data.db.dao.CategoryDao
import com.kangwei.expensetracker.data.db.dao.ExpenseDao
import com.kangwei.expensetracker.data.db.dao.RecurringExpenseDao
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.RecurringExpenseEntity
import java.util.*

object DataSeeder {

    private val defaultCategories = listOf(
        "Food & Dining", "Transportation", "Groceries", "Utilities",
        "Entertainment", "Shopping", "Healthcare", "Housing",
        "Education", "Travel", "Other"
    )

    suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
        if (categoryDao.count() > 0) return
        defaultCategories.forEach { name ->
            categoryDao.insert(CategoryEntity(name = name))
        }
    }

    suspend fun generateRecurring(
        recurringDao: RecurringExpenseDao,
        expenseDao: ExpenseDao
    ) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        for (r in recurringDao.getActiveRecurrings()) {
            val categoryId = r.categoryId ?: continue
            var nextMillis = r.nextDate
            var stillActive = r.isActive

            while (nextMillis <= todayStart) {
                if (r.endDate != null && nextMillis > r.endDate) {
                    stillActive = false
                    break
                }
                expenseDao.insert(
                    ExpenseEntity(
                        amount = r.amount,
                        description = r.description,
                        date = nextMillis,
                        isIncome = false,
                        createdAt = System.currentTimeMillis(),
                        categoryId = categoryId
                    )
                )
                nextMillis = advanceDate(nextMillis, r.frequency)
            }

            if (nextMillis != r.nextDate || stillActive != r.isActive) {
                recurringDao.update(r.copy(nextDate = nextMillis, isActive = stillActive))
            }
        }
    }

    private fun advanceDate(millis: Long, frequency: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        when (frequency) {
            "daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }


}
