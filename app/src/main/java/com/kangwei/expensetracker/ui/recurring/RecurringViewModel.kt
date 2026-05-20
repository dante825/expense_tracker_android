package com.kangwei.expensetracker.ui.recurring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.RecurringExpenseEntity
import com.kangwei.expensetracker.data.db.relation.RecurringWithCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecurringViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ExpenseTrackerApp).database

    val recurrings: StateFlow<List<RecurringWithCategory>> = db.recurringExpenseDao()
        .getAllWithCategoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleActive(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            db.recurringExpenseDao().update(item.copy(isActive = !item.isActive))
        }
    }

    fun delete(item: RecurringExpenseEntity) {
        viewModelScope.launch { db.recurringExpenseDao().delete(item) }
    }

    fun add(
        amount: Double,
        description: String,
        frequency: String,
        startDate: Long,
        endDate: Long?,
        categoryId: Long
    ) {
        viewModelScope.launch {
            db.recurringExpenseDao().insert(
                RecurringExpenseEntity(
                    amount = amount,
                    description = description,
                    frequency = frequency,
                    nextDate = startDate,
                    endDate = endDate,
                    isActive = true,
                    categoryId = categoryId
                )
            )
        }
    }
}
