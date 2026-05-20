package com.kangwei.expensetracker.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.RecurringExpenseEntity

data class RecurringWithCategory(
    @Embedded val recurring: RecurringExpenseEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?
)
