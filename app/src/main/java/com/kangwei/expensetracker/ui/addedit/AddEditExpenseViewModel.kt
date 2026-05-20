package com.kangwei.expensetracker.ui.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseTagCrossRef
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AddEditExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ExpenseTrackerApp).database

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = db.tagDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var amount = MutableStateFlow("")
    var description = MutableStateFlow("")
    var date = MutableStateFlow(System.currentTimeMillis())
    var isIncome = MutableStateFlow(false)
    var selectedCategoryId = MutableStateFlow<Long?>(null)
    var selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    var receiptData = MutableStateFlow<ByteArray?>(null)

    private var editingId: Long? = null

    fun loadExisting(item: ExpenseWithDetails) {
        editingId = item.expense.id
        amount.value = item.expense.amount.toString()
        description.value = item.expense.description
        date.value = item.expense.date
        isIncome.value = item.expense.isIncome
        selectedCategoryId.value = item.category?.id
        selectedTagIds.value = item.tags.map { it.id }.toSet()
        receiptData.value = item.expense.receiptData
    }

    val isValid: StateFlow<Boolean> = combine(amount, selectedCategoryId) { amt, catId ->
        amt.toDoubleOrNull() != null && catId != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun save(onDone: () -> Unit) {
        val amountVal = amount.value.toDoubleOrNull() ?: return
        val catId = selectedCategoryId.value ?: return
        viewModelScope.launch {
            val expenseId = if (editingId != null) {
                db.expenseDao().update(
                    ExpenseEntity(
                        id = editingId!!,
                        amount = amountVal,
                        description = description.value,
                        date = date.value,
                        isIncome = isIncome.value,
                        createdAt = System.currentTimeMillis(),
                        receiptData = receiptData.value,
                        categoryId = catId
                    )
                )
                editingId!!
            } else {
                db.expenseDao().insert(
                    ExpenseEntity(
                        amount = amountVal,
                        description = description.value,
                        date = date.value,
                        isIncome = isIncome.value,
                        createdAt = System.currentTimeMillis(),
                        receiptData = receiptData.value,
                        categoryId = catId
                    )
                )
            }
            db.expenseDao().deleteAllTagsForExpense(expenseId)
            selectedTagIds.value.forEach { tagId ->
                db.expenseDao().insertCrossRef(ExpenseTagCrossRef(expenseId, tagId))
            }
            onDone()
        }
    }

    fun addNewTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = db.tagDao().getAllFlow().first().firstOrNull {
                it.name.equals(trimmed, ignoreCase = true)
            }
            val id = existing?.id ?: db.tagDao().insert(TagEntity(name = trimmed))
            selectedTagIds.value = selectedTagIds.value + id
        }
    }
}
