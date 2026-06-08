package com.kangwei.expensetracker.data.seeder

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kangwei.expensetracker.data.db.AppDatabase
import com.kangwei.expensetracker.data.db.dao.CategoryDao
import com.kangwei.expensetracker.data.db.dao.ExpenseDao
import com.kangwei.expensetracker.data.db.dao.RecurringExpenseDao
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class DataSeederTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var recurringDao: RecurringExpenseDao

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = db.categoryDao()
        expenseDao = db.expenseDao()
        recurringDao = db.recurringExpenseDao()
    }

    @After fun tearDown() = db.close()

    // Returns midnight of today + dayOffset. Mirrors DataSeeder's todayStart calculation.
    private fun midnight(dayOffset: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis

    // Returns midnight of today minus N calendar months.
    private fun midnightMonthsAgo(months: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -months)
        }.timeInMillis

    private suspend fun insertRecurring(
        frequency: String,
        nextDate: Long,
        endDate: Long? = null,
        withCategory: Boolean = true
    ) {
        val catId = if (withCategory) categoryDao.insert(CategoryEntity(name = "Cat")) else null
        recurringDao.insert(
            RecurringExpenseEntity(
                amount = 5.0,
                description = "rec",
                frequency = frequency,
                nextDate = nextDate,
                endDate = endDate,
                isActive = true,
                categoryId = catId
            )
        )
    }

    // ── seedDefaultCategories ───────────────────────────────────────────────────

    @Test fun seedDefaultCategories_insertsElevenCategories() = runTest {
        DataSeeder.seedDefaultCategories(categoryDao)
        assertEquals(11, categoryDao.count())
    }

    @Test fun seedDefaultCategories_isIdempotent() = runTest {
        DataSeeder.seedDefaultCategories(categoryDao)
        DataSeeder.seedDefaultCategories(categoryDao)
        assertEquals(11, categoryDao.count())
    }

    @Test fun seedDefaultCategories_skipsInsertWhenCategoriesAlreadyExist() = runTest {
        categoryDao.insert(CategoryEntity(name = "Custom"))
        DataSeeder.seedDefaultCategories(categoryDao)
        assertEquals(1, categoryDao.count()) // default 11 NOT inserted
    }

    // ── generateRecurring — frequency materialization ───────────────────────────

    // Loop runs while nextMillis <= todayStart (inclusive), so nextDate = N days ago
    // produces N+1 expenses (N days ago through today).

    @Test fun generateRecurring_daily_materializesOncePerDay() = runTest {
        // nextDate = 2 days ago → occurrences: 2-days-ago, 1-day-ago, today = 3
        insertRecurring("daily", midnight(-2))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        assertEquals(3, expenseDao.getAllWithDetailsFlow().first().size)
    }

    @Test fun generateRecurring_weekly_materializesOncePerWeek() = runTest {
        // nextDate = 1 week ago → occurrences: 1-week-ago, today = 2
        insertRecurring("weekly", midnight(-7))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        assertEquals(2, expenseDao.getAllWithDetailsFlow().first().size)
    }

    @Test fun generateRecurring_monthly_materializesOncePerMonth() = runTest {
        // nextDate = 1 calendar month ago → occurrences: 1-month-ago, today = 2
        insertRecurring("monthly", midnightMonthsAgo(1))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        assertEquals(2, expenseDao.getAllWithDetailsFlow().first().size)
    }

    // ── generateRecurring — boundary / edge cases ───────────────────────────────

    @Test fun generateRecurring_futureNextDate_createsNoExpenses() = runTest {
        insertRecurring("daily", midnight(1))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        assertTrue(expenseDao.getAllWithDetailsFlow().first().isEmpty())
    }

    @Test fun generateRecurring_advancesNextDateAfterMaterialization() = runTest {
        // nextDate = today → 1 expense inserted, template nextDate bumped to tomorrow
        insertRecurring("daily", midnight(0))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        val updated = recurringDao.getActiveRecurrings().first()
        assertEquals(midnight(1), updated.nextDate)
    }

    @Test fun generateRecurring_endDate_lastOccurrenceOnEndDateIsIncluded() = runTest {
        // nextDate = 3 days ago, endDate = yesterday (midnight(-1))
        // Iterations: 3-ago (ok), 2-ago (ok), yesterday (ok — endDate check is strictly >),
        // today > yesterday → break. 3 expenses, template deactivated.
        insertRecurring("daily", midnight(-3), endDate = midnight(-1))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        assertEquals(3, expenseDao.getAllWithDetailsFlow().first().size)
        assertTrue(recurringDao.getActiveRecurrings().isEmpty())
    }

    @Test fun generateRecurring_nullCategoryId_createsNoExpenses() = runTest {
        insertRecurring("daily", midnight(-1), withCategory = false)
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        assertTrue(expenseDao.getAllWithDetailsFlow().first().isEmpty())
    }

    @Test fun generateRecurring_multipleRecurrings_eachMaterializedIndependently() = runTest {
        val catId = categoryDao.insert(CategoryEntity(name = "Cat"))
        // A: nextDate = yesterday → 2 expenses (yesterday, today)
        recurringDao.insert(RecurringExpenseEntity(
            amount = 10.0, description = "A", frequency = "daily",
            nextDate = midnight(-1), isActive = true, categoryId = catId
        ))
        // B: nextDate = 2 days ago → 3 expenses (2-ago, yesterday, today)
        recurringDao.insert(RecurringExpenseEntity(
            amount = 20.0, description = "B", frequency = "daily",
            nextDate = midnight(-2), isActive = true, categoryId = catId
        ))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        assertEquals(5, expenseDao.getAllWithDetailsFlow().first().size)
    }

    @Test fun generateRecurring_doesNotUpdateTemplateWhenNothingMaterialized() = runTest {
        // nextDate is in the future — loop never runs, no DB write should happen
        val catId = categoryDao.insert(CategoryEntity(name = "Cat"))
        val futureDate = midnight(5)
        recurringDao.insert(RecurringExpenseEntity(
            amount = 10.0, description = "future", frequency = "daily",
            nextDate = futureDate, isActive = true, categoryId = catId
        ))
        DataSeeder.generateRecurring(recurringDao, expenseDao)
        val recurring = recurringDao.getActiveRecurrings().first()
        assertEquals(futureDate, recurring.nextDate) // unchanged
    }
}
