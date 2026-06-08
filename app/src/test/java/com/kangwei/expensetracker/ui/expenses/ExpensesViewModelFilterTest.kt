package com.kangwei.expensetracker.ui.expenses

import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the filter predicate inside ExpensesViewModel.filtered.
 * The combine() lambda is duplicated here so it can be tested on the JVM
 * without needing an Application context or Room.
 */
class ExpensesViewModelFilterTest {

    // Mirrors the combine() lambda in ExpensesViewModel.filtered exactly.
    private fun applyFilter(
        expenses: List<ExpenseWithDetails>,
        q: String = "",
        catId: Long? = null,
        tagId: Long? = null,
        typeFilter: String = "all"
    ) = expenses.filter { e ->
        val matchSearch = q.isBlank() || e.expense.description.contains(q, ignoreCase = true)
        val matchCat = catId == null || e.expense.categoryId == catId
        val matchTag = tagId == null || e.tags.any { t -> t.id == tagId }
        val matchType = when (typeFilter) {
            "income" -> e.expense.isIncome
            "expense" -> !e.expense.isIncome
            else -> true
        }
        matchSearch && matchCat && matchTag && matchType
    }

    private fun expense(
        id: Long = 1L,
        description: String = "test",
        categoryId: Long = 1L,
        isIncome: Boolean = false,
        tags: List<TagEntity> = emptyList()
    ) = ExpenseWithDetails(
        expense = ExpenseEntity(
            id = id, amount = 10.0, description = description,
            date = 0L, isIncome = isIncome, createdAt = 0L, categoryId = categoryId
        ),
        category = CategoryEntity(id = categoryId, name = "Cat"),
        tags = tags
    )

    // ── search filter ────────────────────────────────────────────────────────

    @Test fun blankSearch_returnsAllExpenses() {
        val data = listOf(expense(1, "coffee"), expense(2, "taxi"))
        assertEquals(2, applyFilter(data, q = "").size)
    }

    @Test fun search_matchesDescriptionCaseInsensitive() {
        val data = listOf(expense(1, "Coffee"), expense(2, "taxi"))
        val result = applyFilter(data, q = "coffee")
        assertEquals(1, result.size)
        assertEquals("Coffee", result[0].expense.description)
    }

    @Test fun search_partialMatch_included() {
        val data = listOf(expense(1, "kopitiam breakfast"), expense(2, "lunch"))
        assertEquals(1, applyFilter(data, q = "kopi").size)
    }

    @Test fun search_noMatch_returnsEmpty() {
        val data = listOf(expense(1, "coffee"), expense(2, "taxi"))
        assertTrue(applyFilter(data, q = "hotel").isEmpty())
    }

    // ── category filter ──────────────────────────────────────────────────────

    @Test fun categoryFilter_null_returnsAll() {
        val data = listOf(expense(1, categoryId = 1), expense(2, categoryId = 2))
        assertEquals(2, applyFilter(data, catId = null).size)
    }

    @Test fun categoryFilter_specificId_excludesOtherCategories() {
        val data = listOf(expense(1, categoryId = 1), expense(2, categoryId = 2))
        val result = applyFilter(data, catId = 1L)
        assertEquals(1, result.size)
        assertEquals(1L, result[0].expense.categoryId)
    }

    // ── tag filter ───────────────────────────────────────────────────────────

    @Test fun tagFilter_null_returnsAll() {
        val data = listOf(
            expense(1, tags = listOf(TagEntity(id = 10, name = "work"))),
            expense(2, tags = emptyList())
        )
        assertEquals(2, applyFilter(data, tagId = null).size)
    }

    @Test fun tagFilter_specificId_excludesExpensesWithoutTag() {
        val data = listOf(
            expense(1, tags = listOf(TagEntity(id = 10, name = "work"))),
            expense(2, tags = emptyList())
        )
        val result = applyFilter(data, tagId = 10L)
        assertEquals(1, result.size)
        assertEquals(1L, result[0].expense.id)
    }

    @Test fun tagFilter_expenseWithMultipleTags_matchesIfAnyTagMatches() {
        val data = listOf(
            expense(1, tags = listOf(TagEntity(id = 10, name = "work"), TagEntity(id = 11, name = "travel")))
        )
        assertEquals(1, applyFilter(data, tagId = 11L).size)
    }

    // ── type filter ──────────────────────────────────────────────────────────

    @Test fun typeFilter_all_returnsBothIncomeAndExpense() {
        val data = listOf(expense(1, isIncome = false), expense(2, isIncome = true))
        assertEquals(2, applyFilter(data, typeFilter = "all").size)
    }

    @Test fun typeFilter_income_excludesExpenses() {
        val data = listOf(expense(1, isIncome = true), expense(2, isIncome = false))
        val result = applyFilter(data, typeFilter = "income")
        assertEquals(1, result.size)
        assertTrue(result[0].expense.isIncome)
    }

    @Test fun typeFilter_expense_excludesIncome() {
        val data = listOf(expense(1, isIncome = false), expense(2, isIncome = true))
        val result = applyFilter(data, typeFilter = "expense")
        assertEquals(1, result.size)
        assertFalse(result[0].expense.isIncome)
    }

    @Test fun typeFilter_unknownValue_treatedAsAll() {
        val data = listOf(expense(1, isIncome = false), expense(2, isIncome = true))
        assertEquals(2, applyFilter(data, typeFilter = "unknown").size)
    }

    // ── combined filters ─────────────────────────────────────────────────────

    @Test fun combinedFilters_allConditionsMustMatch() {
        val tag = TagEntity(id = 5, name = "work")
        val data = listOf(
            expense(1, "lunch", categoryId = 1, isIncome = false, tags = listOf(tag)), // passes all
            expense(2, "lunch", categoryId = 2, isIncome = false, tags = listOf(tag)), // wrong category
            expense(3, "lunch", categoryId = 1, isIncome = true,  tags = listOf(tag)), // wrong type
            expense(4, "dinner", categoryId = 1, isIncome = false, tags = listOf(tag)) // wrong search
        )
        val result = applyFilter(data, q = "lunch", catId = 1L, tagId = 5L, typeFilter = "expense")
        assertEquals(1, result.size)
        assertEquals(1L, result[0].expense.id)
    }

    @Test fun emptyExpenseList_returnsEmpty() {
        assertTrue(applyFilter(emptyList(), q = "x", catId = 1L, tagId = 1L).isEmpty())
    }
}
