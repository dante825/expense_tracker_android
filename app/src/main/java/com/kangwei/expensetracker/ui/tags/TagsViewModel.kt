package com.kangwei.expensetracker.ui.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.TagWithExpenses
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TagsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ExpenseTrackerApp).database

    val tags: StateFlow<List<TagWithExpenses>> = db.tagDao()
        .getAllWithExpensesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (db.tagDao().countByName(trimmed) == 0) {
                db.tagDao().insert(TagEntity(name = trimmed))
            }
        }
    }

    fun rename(tag: TagEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (db.tagDao().countByName(trimmed) == 0) {
                db.tagDao().update(tag.copy(name = trimmed))
            }
        }
    }

    fun delete(tag: TagEntity) {
        viewModelScope.launch { db.tagDao().delete(tag) }
    }
}
