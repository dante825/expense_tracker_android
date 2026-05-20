package com.kangwei.expensetracker.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpensesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ExpenseTrackerApp).database

    val allExpenses: StateFlow<List<ExpenseWithDetails>> = db.expenseDao()
        .getAllWithDetailsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = db.tagDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val search = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedTagId = MutableStateFlow<Long?>(null)
    val entryTypeFilter = MutableStateFlow("all")

    val filtered: StateFlow<List<ExpenseWithDetails>> = combine(
        allExpenses, search, selectedCategoryId, selectedTagId, entryTypeFilter
    ) { expenses, q, catId, tagId, typeFilter ->
        expenses.filter { e ->
            val matchSearch = q.isBlank() || e.expense.description.contains(q, ignoreCase = true)
            val matchCat = catId == null || e.expense.categoryId == catId
            val matchTag = tagId == null || e.tags.any { t -> t.id == tagId }
            val matchType = when (typeFilter) {
                "income" -> e.expense.isIncome
                "expense" -> !e.expense.isIncome
                else -> true
            }
            matchSearch && matchCat && matchTag && matchType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(item: ExpenseWithDetails) {
        viewModelScope.launch {
            db.expenseDao().delete(item.expense)
        }
    }
}
