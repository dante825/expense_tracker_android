package com.kangwei.expensetracker.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import com.kangwei.expensetracker.data.seeder.DataSeeder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class TimePeriod(val label: String) {
    WEEK("Week"), MONTH("Month"), YEAR("Year"), ALL("All");

    fun startMillis(): Long? {
        val cal = Calendar.getInstance()
        return when (this) {
            WEEK -> cal.apply { add(Calendar.WEEK_OF_YEAR, -1) }.timeInMillis
            MONTH -> cal.apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            YEAR -> cal.apply { set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            ALL -> null
        }
    }
}

data class ChartBar(val date: Long, val spent: Double, val income: Double)
data class CategorySlice(val name: String, val total: Double)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ExpenseTrackerApp).database

    val allExpenses: StateFlow<List<ExpenseWithDetails>> = db.expenseDao()
        .getAllWithDetailsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = db.tagDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    val selectedTagId = MutableStateFlow<Long?>(null)

    val filteredExpenses: StateFlow<List<ExpenseWithDetails>> = combine(
        allExpenses, selectedPeriod, selectedTagId
    ) { expenses, period, tagId ->
        val start = period.startMillis()
        val byPeriod = if (start == null) expenses else expenses.filter { it.expense.date >= start }
        if (tagId == null) byPeriod
        else byPeriod.filter { it.tags.any { t -> t.id == tagId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun seed() {
        viewModelScope.launch {
            DataSeeder.seedDefaultCategories(db.categoryDao())
            DataSeeder.generateRecurring(db.recurringExpenseDao(), db.expenseDao())
        }
    }
}
