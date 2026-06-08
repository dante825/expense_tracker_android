package com.kangwei.expensetracker.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kangwei.expensetracker.data.db.AppDatabase
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseTagCrossRef
import com.kangwei.expensetracker.data.db.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var tagDao: TagDao

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.expenseDao()
        categoryDao = db.categoryDao()
        tagDao = db.tagDao()
    }

    @After fun tearDown() = db.close()

    private suspend fun insertCategory(name: String = "Food") =
        categoryDao.insert(CategoryEntity(name = name))

    private suspend fun insertExpense(
        catId: Long,
        amount: Double = 10.0,
        description: String = "test",
        isIncome: Boolean = false,
        date: Long = System.currentTimeMillis()
    ) = dao.insert(
        ExpenseEntity(
            amount = amount,
            description = description,
            date = date,
            isIncome = isIncome,
            createdAt = System.currentTimeMillis(),
            categoryId = catId
        )
    )

    @Test fun insert_returnsPositiveId() = runTest {
        val id = insertExpense(insertCategory())
        assertTrue(id > 0)
    }

    @Test fun getAllWithDetails_returnsInsertedExpense() = runTest {
        insertExpense(insertCategory(), amount = 25.0)
        val results = dao.getAllWithDetailsFlow().first()
        assertEquals(1, results.size)
        assertEquals(25.0, results[0].expense.amount, 0.0)
    }

    @Test fun getAllWithDetails_categoryIsPopulated() = runTest {
        val catId = insertCategory("Groceries")
        insertExpense(catId)
        val result = dao.getAllWithDetailsFlow().first()[0]
        assertEquals("Groceries", result.category?.name)
    }

    @Test fun getAllWithDetails_tagsEmptyByDefault() = runTest {
        insertExpense(insertCategory())
        val result = dao.getAllWithDetailsFlow().first()[0]
        assertTrue(result.tags.isEmpty())
    }

    @Test fun getAllWithDetails_tagsResolvedViaJunction() = runTest {
        val expId = insertExpense(insertCategory())
        val tagId = tagDao.insert(TagEntity(name = "work"))
        dao.insertCrossRef(ExpenseTagCrossRef(expenseId = expId, tagId = tagId))
        val result = dao.getAllWithDetailsFlow().first()[0]
        assertEquals(listOf("work"), result.tags.map { it.name })
    }

    @Test fun deleteAllTagsForExpense_removesAllCrossRefs() = runTest {
        val expId = insertExpense(insertCategory())
        val t1 = tagDao.insert(TagEntity(name = "a"))
        val t2 = tagDao.insert(TagEntity(name = "b"))
        dao.insertCrossRef(ExpenseTagCrossRef(expId, t1))
        dao.insertCrossRef(ExpenseTagCrossRef(expId, t2))
        dao.deleteAllTagsForExpense(expId)
        assertTrue(dao.getAllWithDetailsFlow().first()[0].tags.isEmpty())
    }

    @Test fun deleteAllTagsForExpense_doesNotAffectOtherExpenses() = runTest {
        val catId = insertCategory()
        val exp1 = insertExpense(catId)
        val exp2 = insertExpense(catId)
        val tagId = tagDao.insert(TagEntity(name = "shared"))
        dao.insertCrossRef(ExpenseTagCrossRef(exp1, tagId))
        dao.insertCrossRef(ExpenseTagCrossRef(exp2, tagId))
        dao.deleteAllTagsForExpense(exp1)
        val exp2Result = dao.getAllWithDetailsFlow().first().first { it.expense.id == exp2 }
        assertEquals(1, exp2Result.tags.size)
    }

    @Test fun update_persistsChange() = runTest {
        insertExpense(insertCategory(), amount = 5.0)
        val original = dao.getAllWithDetailsFlow().first()[0].expense
        dao.update(original.copy(amount = 99.0))
        assertEquals(99.0, dao.getAllWithDetailsFlow().first()[0].expense.amount, 0.0)
    }

    @Test fun delete_removesExpense() = runTest {
        insertExpense(insertCategory())
        val expense = dao.getAllWithDetailsFlow().first()[0].expense
        dao.delete(expense)
        assertTrue(dao.getAllWithDetailsFlow().first().isEmpty())
    }

    @Test fun getAllWithDetails_orderedByDateDesc() = runTest {
        val catId = insertCategory()
        val now = System.currentTimeMillis()
        insertExpense(catId, description = "old", date = now - 1000)
        insertExpense(catId, description = "new", date = now)
        val results = dao.getAllWithDetailsFlow().first()
        assertEquals("new", results[0].expense.description)
        assertEquals("old", results[1].expense.description)
    }

    @Test fun insertCrossRef_replace_allowsReinsert() = runTest {
        val expId = insertExpense(insertCategory())
        val tagId = tagDao.insert(TagEntity(name = "tag"))
        dao.insertCrossRef(ExpenseTagCrossRef(expId, tagId))
        // Simulates the delete-then-reinsert pattern used in AddEditExpenseViewModel
        dao.deleteAllTagsForExpense(expId)
        dao.insertCrossRef(ExpenseTagCrossRef(expId, tagId))
        assertEquals(1, dao.getAllWithDetailsFlow().first()[0].tags.size)
    }
}
