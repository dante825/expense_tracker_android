package com.kangwei.expensetracker.ui.addedit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.AppDatabase
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests AddEditExpenseViewModel save() paths using the real app database.
 * Uses CountDownLatch to synchronize on viewModelScope.launch completions
 * (which run on Dispatchers.Main, separate from the instrumentation thread).
 *
 * All test data is namespaced with a unique suffix and cleaned up in @After.
 */
@RunWith(AndroidJUnit4::class)
class AddEditExpenseViewModelTest {

    private lateinit var app: ExpenseTrackerApp
    private lateinit var db: AppDatabase
    private lateinit var vm: AddEditExpenseViewModel
    private var testCatId: Long = -1
    private val suffix = System.nanoTime()

    @Before fun setUp() = runBlocking {
        app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as ExpenseTrackerApp
        db = app.database
        vm = AddEditExpenseViewModel(app)
        testCatId = db.categoryDao().insert(CategoryEntity(name = "__testcat_$suffix"))
    }

    @After fun tearDown() = runBlocking {
        // Cascade-delete the test category (ForeignKey.CASCADE removes its expenses too)
        db.categoryDao().getAllFlow().first()
            .filter { it.name.startsWith("__testcat_") }
            .forEach { db.categoryDao().delete(it) }
        // Clean up any test tags
        db.tagDao().getAllFlow().first()
            .filter { it.name.startsWith("__testtag_") }
            .forEach { db.tagDao().delete(it) }
    }

    private fun save(v: AddEditExpenseViewModel): Boolean {
        val latch = CountDownLatch(1)
        v.save { latch.countDown() }
        return latch.await(5, TimeUnit.SECONDS)
    }

    // ── create path ──────────────────────────────────────────────────────────

    @Test fun save_createPath_insertsNewExpense() = runBlocking {
        vm.amount.value = "42.0"
        vm.description.value = "create_test_$suffix"
        vm.selectedCategoryId.value = testCatId

        assertTrue("save timed out", save(vm))

        val expenses = db.expenseDao().getAllWithDetailsFlow().first()
            .filter { it.expense.categoryId == testCatId }
        assertEquals(1, expenses.size)
        assertEquals(42.0, expenses[0].expense.amount, 0.0)
        assertEquals("create_test_$suffix", expenses[0].expense.description)
    }

    @Test fun save_createPath_calledTwice_insertsTwoExpenses() = runBlocking {
        vm.amount.value = "10.0"
        vm.description.value = "first_$suffix"
        vm.selectedCategoryId.value = testCatId
        assertTrue(save(vm))

        // New VM instance = new create (no editingId set)
        val vm2 = AddEditExpenseViewModel(app)
        vm2.amount.value = "20.0"
        vm2.description.value = "second_$suffix"
        vm2.selectedCategoryId.value = testCatId
        assertTrue(save(vm2))

        val expenses = db.expenseDao().getAllWithDetailsFlow().first()
            .filter { it.expense.categoryId == testCatId }
        assertEquals(2, expenses.size)
    }

    // ── edit path ────────────────────────────────────────────────────────────

    @Test fun save_editPath_updatesExistingExpenseNotInsertNew() = runBlocking {
        // Create via create path
        vm.amount.value = "10.0"
        vm.description.value = "original_$suffix"
        vm.selectedCategoryId.value = testCatId
        assertTrue(save(vm))

        val original = db.expenseDao().getAllWithDetailsFlow().first()
            .first { it.expense.categoryId == testCatId }

        // Edit via a new VM with loadExisting
        val editVm = AddEditExpenseViewModel(app)
        editVm.loadExisting(original)
        editVm.amount.value = "99.0"
        assertTrue(save(editVm))

        val after = db.expenseDao().getAllWithDetailsFlow().first()
            .filter { it.expense.categoryId == testCatId }
        assertEquals("edit should update, not insert a second row", 1, after.size)
        assertEquals(99.0, after[0].expense.amount, 0.0)
    }

    @Test fun loadExisting_populatesAllStateFields() = runBlocking {
        // Insert a bare expense and load it into the VM
        val expId = db.expenseDao().insert(
            com.kangwei.expensetracker.data.db.entity.ExpenseEntity(
                amount = 77.5, description = "loaded_$suffix",
                date = 111111L, isIncome = true,
                createdAt = 0L, categoryId = testCatId
            )
        )
        val expenseWithDetails = db.expenseDao().getAllWithDetailsFlow().first()
            .first { it.expense.id == expId }

        vm.loadExisting(expenseWithDetails)

        assertEquals("77.5", vm.amount.value)
        assertEquals("loaded_$suffix", vm.description.value)
        assertEquals(111111L, vm.date.value)
        assertTrue(vm.isIncome.value)
        assertEquals(testCatId, vm.selectedCategoryId.value)
    }

    // ── delete-then-reinsert tags ────────────────────────────────────────────

    @Test fun save_createPath_withTags_insertsCrossRefs() = runBlocking {
        val tagId = db.tagDao().insert(TagEntity(name = "__testtag_a_$suffix"))
        vm.amount.value = "5.0"
        vm.selectedCategoryId.value = testCatId
        vm.selectedTagIds.value = setOf(tagId)
        assertTrue(save(vm))

        val result = db.expenseDao().getAllWithDetailsFlow().first()
            .first { it.expense.categoryId == testCatId }
        assertEquals(1, result.tags.size)
        assertEquals(tagId, result.tags[0].id)
    }

    @Test fun save_editPath_deletesThenReinsertsTagsCorrectly() = runBlocking {
        val tagA = db.tagDao().insert(TagEntity(name = "__testtag_a_$suffix"))
        val tagB = db.tagDao().insert(TagEntity(name = "__testtag_b_$suffix"))

        // Create with tag A
        vm.amount.value = "5.0"
        vm.selectedCategoryId.value = testCatId
        vm.selectedTagIds.value = setOf(tagA)
        assertTrue(save(vm))

        val original = db.expenseDao().getAllWithDetailsFlow().first()
            .first { it.expense.categoryId == testCatId }
        assertEquals(listOf(tagA), original.tags.map { it.id })

        // Edit: replace tag A with tag B
        val editVm = AddEditExpenseViewModel(app)
        editVm.loadExisting(original)
        editVm.selectedTagIds.value = setOf(tagB)
        assertTrue(save(editVm))

        val updated = db.expenseDao().getAllWithDetailsFlow().first()
            .first { it.expense.categoryId == testCatId }
        assertEquals("old tag should be removed", 1, updated.tags.size)
        assertEquals(tagB, updated.tags[0].id)
    }

    @Test fun save_editPath_clearingAllTags_leavesNoTags() = runBlocking {
        val tagId = db.tagDao().insert(TagEntity(name = "__testtag_c_$suffix"))

        vm.amount.value = "5.0"
        vm.selectedCategoryId.value = testCatId
        vm.selectedTagIds.value = setOf(tagId)
        assertTrue(save(vm))

        val original = db.expenseDao().getAllWithDetailsFlow().first()
            .first { it.expense.categoryId == testCatId }

        val editVm = AddEditExpenseViewModel(app)
        editVm.loadExisting(original)
        editVm.selectedTagIds.value = emptySet()
        assertTrue(save(editVm))

        val updated = db.expenseDao().getAllWithDetailsFlow().first()
            .first { it.expense.categoryId == testCatId }
        assertTrue(updated.tags.isEmpty())
    }
}
