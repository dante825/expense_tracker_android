package com.kangwei.expensetracker.ui.settings

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class CurrencyOption(val code: String, val displayName: String)

val SUPPORTED_CURRENCIES = listOf(
    CurrencyOption("", "Device default"),
    CurrencyOption("USD", "USD — US Dollar"),
    CurrencyOption("EUR", "EUR — Euro"),
    CurrencyOption("GBP", "GBP — British Pound"),
    CurrencyOption("JPY", "JPY — Japanese Yen"),
    CurrencyOption("CNY", "CNY — Chinese Yuan"),
    CurrencyOption("CAD", "CAD — Canadian Dollar"),
    CurrencyOption("AUD", "AUD — Australian Dollar"),
    CurrencyOption("CHF", "CHF — Swiss Franc"),
    CurrencyOption("HKD", "HKD — Hong Kong Dollar"),
    CurrencyOption("SGD", "SGD — Singapore Dollar"),
    CurrencyOption("INR", "INR — Indian Rupee"),
    CurrencyOption("KRW", "KRW — South Korean Won"),
    CurrencyOption("BRL", "BRL — Brazilian Real"),
    CurrencyOption("MXN", "MXN — Mexican Peso"),
    CurrencyOption("MYR", "MYR — Malaysian Ringgit"),
    CurrencyOption("THB", "THB — Thai Baht"),
    CurrencyOption("IDR", "IDR — Indonesian Rupiah"),
    CurrencyOption("PHP", "PHP — Philippine Peso"),
    CurrencyOption("AED", "AED — UAE Dirham"),
    CurrencyOption("NOK", "NOK — Norwegian Krone"),
    CurrencyOption("SEK", "SEK — Swedish Krona"),
    CurrencyOption("NZD", "NZD — New Zealand Dollar"),
    CurrencyOption("ZAR", "ZAR — South African Rand"),
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = UserPreferences(app)
    private val db = (app as ExpenseTrackerApp).database

    private val _currencyCode = MutableStateFlow(prefs.getCurrencyCode())
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportShareIntent = MutableStateFlow<Intent?>(null)
    val exportShareIntent: StateFlow<Intent?> = _exportShareIntent.asStateFlow()

    fun setCurrency(code: String) {
        prefs.setCurrencyCode(code)
        _currencyCode.value = code
    }

    fun exportToCsv() {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val expenses = db.expenseDao().getAllWithDetailsList()

                val dir = File(app.cacheDir, "exports").also { it.mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(dir, "expenses_$timestamp.csv")

                file.bufferedWriter().use { w ->
                    w.write("Date,Description,Amount,Type,Category,Tags\n")
                    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    expenses.forEach { e ->
                        val date = dateFmt.format(Date(e.expense.date))
                        val tags = e.tags.sortedBy { it.name }.joinToString(";") { it.name }
                        w.write(listOf(
                            date,
                            e.expense.description.csvEscape(),
                            "%.2f".format(e.expense.amount),
                            if (e.expense.isIncome) "Income" else "Expense",
                            (e.category?.name ?: "").csvEscape(),
                            tags.csvEscape()
                        ).joinToString(",") + "\n")
                    }
                }

                val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                _exportShareIntent.value = Intent.createChooser(shareIntent, "Export expenses")
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportIntent() {
        _exportShareIntent.value = null
    }
}

private fun String.csvEscape(): String =
    if (contains(',') || contains('"') || contains('\n') || contains('\r'))
        "\"${replace("\"", "\"\"")}\""
    else this
