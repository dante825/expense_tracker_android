package com.kangwei.expensetracker

import android.app.Application
import com.kangwei.expensetracker.data.db.AppDatabase

class ExpenseTrackerApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
}
