package com.kangwei.expensetracker.data.prefs

import android.content.Context

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun getCurrencyCode(): String = prefs.getString("currency_code", "") ?: ""

    fun setCurrencyCode(code: String) {
        prefs.edit().putString("currency_code", code).apply()
    }
}
