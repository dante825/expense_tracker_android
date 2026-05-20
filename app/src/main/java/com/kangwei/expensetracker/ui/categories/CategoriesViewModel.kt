package com.kangwei.expensetracker.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.seeder.DataSeeder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class CategoriesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ExpenseTrackerApp).database

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun seed() {
        viewModelScope.launch { DataSeeder.seedDefaultCategories(db.categoryDao()) }
    }

    fun addCategory(name: String, budgetLimit: Double?) {
        viewModelScope.launch {
            db.categoryDao().insert(CategoryEntity(name = name, budgetLimit = budgetLimit ?: 0.0))
        }
    }

    fun updateBudget(category: CategoryEntity, budget: Double) {
        viewModelScope.launch {
            db.categoryDao().update(category.copy(budgetLimit = budget))
        }
    }

    fun delete(category: CategoryEntity) {
        viewModelScope.launch {
            if (db.categoryDao().expenseCount(category.id) == 0) {
                db.categoryDao().delete(category)
            }
        }
    }

    fun monthSpent(categoryId: Long, onResult: (Double) -> Unit) {
        viewModelScope.launch {
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onResult(db.categoryDao().monthSpent(categoryId, monthStart) ?: 0.0)
        }
    }
}
