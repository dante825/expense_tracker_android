package com.kangwei.expensetracker.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kangwei.expensetracker.data.db.AppDatabase
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CategoryDao
    private lateinit var expenseDao: ExpenseDao

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.categoryDao()
        expenseDao = db.expenseDao()
    }

    @After fun tearDown() = db.close()

    private suspend fun insertCategory(name: String = "Food") =
        dao.insert(CategoryEntity(name = name))

    private suspend fun insertExpense(
        catId: Long,
        amount: Double = 10.0,
        isIncome: Boolean = false,
        date: Long = System.currentTimeMillis()
    ) = expenseDao.insert(
        ExpenseEntity(
            amount = amount,
            description = "test",
            date = date,
            isIncome = isIncome,
            createdAt = System.currentTimeMillis(),
            categoryId = catId
        )
    )

    @Test fun count_returnsZeroOnEmptyDb() = runTest {
        assertEquals(0, dao.count())
    }

    @Test fun insert_incrementsCount() = runTest {
        insertCategory("A"); insertCategory("B")
        assertEquals(2, dao.count())
    }

    @Test fun getAllFlow_orderedByNameAsc() = runTest {
        insertCategory("Zoo"); insertCategory("Apple")
        val names = dao.getAllFlow().first().map { it.name }
        assertEquals(listOf("Apple", "Zoo"), names)
    }

    @Test fun update_persistsNameChange() = runTest {
        val id = insertCategory("Old")
        dao.update(CategoryEntity(id = id, name = "New"))
        assertEquals("New", dao.getAllFlow().first()[0].name)
    }

    @Test fun delete_removesCategory() = runTest {
        insertCategory()
        dao.delete(dao.getAllFlow().first()[0])
        assertEquals(0, dao.count())
    }

    @Test fun expenseCount_countsOnlyForThatCategory() = runTest {
        val catA = insertCategory("A")
        val catB = insertCategory("B")
        insertExpense(catA); insertExpense(catA); insertExpense(catB)
        assertEquals(2, dao.expenseCount(catA))
        assertEquals(1, dao.expenseCount(catB))
    }

    @Test fun monthSpent_sumsExpensesInRangeExcludingIncome() = runTest {
        val catId = insertCategory()
        val now = System.currentTimeMillis()
        val monthStart = now - 10_000
        insertExpense(catId, amount = 30.0, date = now)                    // in range, counts
        insertExpense(catId, amount = 5.0, date = monthStart - 1)          // before range, excluded
        insertExpense(catId, amount = 8.0, isIncome = true, date = now)    // income, excluded
        assertEquals(30.0, dao.monthSpent(catId, monthStart)!!, 0.001)
    }

    @Test fun monthSpent_returnsNullWhenNoMatchingExpenses() = runTest {
        val catId = insertCategory()
        assertNull(dao.monthSpent(catId, 0L))
    }

    @Test fun delete_cascadesToExpenses() = runTest {
        val catId = insertCategory()
        insertExpense(catId)
        dao.delete(dao.getAllFlow().first()[0])
        assertTrue(expenseDao.getAllWithDetailsFlow().first().isEmpty())
    }

    @Test fun expenseCount_zeroForCategoryWithNoExpenses() = runTest {
        val catId = insertCategory()
        assertEquals(0, dao.expenseCount(catId))
    }
}
