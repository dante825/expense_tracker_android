package com.kangwei.expensetracker.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for TagChip, SummaryCard, and ExpenseListItem.
 * Uses Robolectric to run on the JVM, bypassing the Espresso InputManager
 * reflection issue present on Android 16 (API 37) emulators.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedComponentsUiTest {

    @get:Rule val composeTestRule = createComposeRule()

    // ── TagChip ──────────────────────────────────────────────────────────────

    @Test fun tagChip_showsName() {
        composeTestRule.setContent { TagChip(name = "Food", isSelected = false, onClick = {}) }
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
    }

    @Test fun tagChip_clickInvokesCallback() {
        var clicked = false
        composeTestRule.setContent {
            TagChip(name = "Work", isSelected = false, onClick = { clicked = true })
        }
        composeTestRule.onNodeWithText("Work").performClick()
        assertTrue("onClick was not called", clicked)
    }

    @Test fun tagChip_selectedAndUnselected_bothShowName() {
        composeTestRule.setContent {
            TagChip(name = "Travel", isSelected = true, onClick = {})
            TagChip(name = "Other", isSelected = false, onClick = {})
        }
        composeTestRule.onNodeWithText("Travel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Other").assertIsDisplayed()
    }

    // ── SummaryCard ──────────────────────────────────────────────────────────

    @Test fun summaryCard_showsTitleAndFormattedAmount() {
        composeTestRule.setContent {
            SummaryCard(title = "Income", amount = 42.0, color = Color(0xFF2E7D32))
        }
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText(formatCurrency(42.0)).assertIsDisplayed()
    }

    @Test fun summaryCard_zeroAmount_shown() {
        composeTestRule.setContent {
            SummaryCard(title = "Spent", amount = 0.0, color = Color(0xFFC62828))
        }
        composeTestRule.onNodeWithText("Spent").assertIsDisplayed()
        composeTestRule.onNodeWithText(formatCurrency(0.0)).assertIsDisplayed()
    }

    // ── ExpenseListItem ──────────────────────────────────────────────────────

    private fun fakeItem(
        description: String = "Coffee",
        categoryName: String? = "Food",
        amount: Double = 5.5,
        isIncome: Boolean = false,
        tags: List<TagEntity> = emptyList()
    ) = ExpenseWithDetails(
        expense = ExpenseEntity(
            id = 1L, amount = amount, description = description,
            date = 0L, isIncome = isIncome, createdAt = 0L, categoryId = 1L
        ),
        category = categoryName?.let { CategoryEntity(id = 1L, name = it) },
        tags = tags
    )

    @Test fun expenseListItem_showsDescriptionCategoryAndAmount() {
        val item = fakeItem("Lunch", "Food", 12.50)
        composeTestRule.setContent { ExpenseListItem(item) }
        composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText(formatCurrency(12.50)).assertIsDisplayed()
    }

    @Test fun expenseListItem_nullCategory_showsUnknown() {
        val item = fakeItem(categoryName = null)
        composeTestRule.setContent { ExpenseListItem(item) }
        composeTestRule.onNodeWithText("Unknown").assertIsDisplayed()
    }

    @Test fun expenseListItem_tagsShownAlphabetically() {
        val item = fakeItem(tags = listOf(
            TagEntity(id = 2, name = "Work"),
            TagEntity(id = 1, name = "Beta"),
            TagEntity(id = 3, name = "Alpha")
        ))
        composeTestRule.setContent { ExpenseListItem(item) }
        composeTestRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
    }
}
