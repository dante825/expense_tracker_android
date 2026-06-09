package com.kangwei.expensetracker.ui.addedit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseTagCrossRef
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AddEditExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ExpenseTrackerApp).database

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = db.tagDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date.asStateFlow()

    private val _isIncome = MutableStateFlow(false)
    val isIncome: StateFlow<Boolean> = _isIncome.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    private val _receiptPath = MutableStateFlow<String?>(null)
    val receiptPath: StateFlow<String?> = _receiptPath.asStateFlow()

    private var editingId: Long? = null

    fun loadExisting(item: ExpenseWithDetails) {
        editingId = item.expense.id
        _amount.value = item.expense.amount.toString()
        _description.value = item.expense.description
        _date.value = item.expense.date
        _isIncome.value = item.expense.isIncome
        _selectedCategoryId.value = item.category?.id
        _selectedTagIds.value = item.tags.map { it.id }.toSet()
        _receiptPath.value = item.expense.receiptPath
    }

    fun onAmountChange(v: String) { _amount.value = v }
    fun onDescriptionChange(v: String) { _description.value = v }
    fun onDateChange(v: Long) { _date.value = v }
    fun onIsIncomeChange(v: Boolean) { _isIncome.value = v }
    fun onCategorySelected(id: Long) { _selectedCategoryId.value = id }
    fun onTagToggled(id: Long, selected: Boolean) {
        _selectedTagIds.value = if (selected) _selectedTagIds.value + id else _selectedTagIds.value - id
    }

    fun attachReceipt(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val dir = File(app.filesDir, "receipts").also { it.mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            app.contentResolver.openInputStream(uri)?.use { it.copyTo(file.outputStream()) }
            _receiptPath.value?.let { File(it).delete() }
            _receiptPath.value = file.absolutePath
        }
    }

    fun removeReceipt() {
        _receiptPath.value?.let { File(it).delete() }
        _receiptPath.value = null
    }

    val isValid: StateFlow<Boolean> = combine(_amount, _selectedCategoryId) { amt, catId ->
        amt.toDoubleOrNull() != null && catId != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun save(onDone: () -> Unit) {
        val amountVal = _amount.value.toDoubleOrNull() ?: return
        val catId = _selectedCategoryId.value ?: return
        viewModelScope.launch {
            val expenseId = if (editingId != null) {
                db.expenseDao().update(
                    ExpenseEntity(
                        id = editingId!!,
                        amount = amountVal,
                        description = _description.value,
                        date = _date.value,
                        isIncome = _isIncome.value,
                        createdAt = System.currentTimeMillis(),
                        receiptPath = _receiptPath.value,
                        categoryId = catId
                    )
                )
                editingId!!
            } else {
                db.expenseDao().insert(
                    ExpenseEntity(
                        amount = amountVal,
                        description = _description.value,
                        date = _date.value,
                        isIncome = _isIncome.value,
                        createdAt = System.currentTimeMillis(),
                        receiptPath = _receiptPath.value,
                        categoryId = catId
                    )
                )
            }
            db.expenseDao().deleteAllTagsForExpense(expenseId)
            _selectedTagIds.value.forEach { tagId ->
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
            _selectedTagIds.value = _selectedTagIds.value + id
        }
    }
}
