package com.kangwei.expensetracker.ui.addedit

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.kangwei.expensetracker.ExpenseTrackerApp
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.data.db.entity.ExpenseEntity
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests AddEditExpenseSheet composable in isolation using Robolectric.
 * The sheet is a ModalBottomSheet; its content is traversable by the Compose
 * semantics tree used for assertions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ExpenseTrackerApp::class)
class AddEditExpenseSheetTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ── Heading ───────────────────────────────────────────────────────────────

    @Test fun newEntry_showsNewEntryHeading() {
        composeTestRule.setContent { AddEditExpenseSheet(existing = null, onDismiss = {}) }
        composeTestRule.onNodeWithText("New Entry").assertIsDisplayed()
    }

    @Test fun editEntry_showsEditEntryHeading() {
        val existing = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = 1L, amount = 50.0, description = "Dinner",
                date = 0L, isIncome = false, createdAt = 0L, categoryId = 1L
            ),
            category = CategoryEntity(id = 1L, name = "Food"),
            tags = emptyList()
        )
        composeTestRule.setContent { AddEditExpenseSheet(existing = existing, onDismiss = {}) }
        composeTestRule.onNodeWithText("Edit Entry").assertIsDisplayed()
    }

    // ── Form fields ───────────────────────────────────────────────────────────

    @Test fun formFields_amountDescriptionAndIncomeToggleVisible() {
        composeTestRule.setContent { AddEditExpenseSheet(existing = null, onDismiss = {}) }
        composeTestRule.onNodeWithText("Amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is income").assertIsDisplayed()
    }

    @Test fun formFields_saveButtonInHeader_present() {
        composeTestRule.setContent { AddEditExpenseSheet(existing = null, onDismiss = {}) }
        // Two "Save" nodes exist (header TextButton + bottom Button); use onFirst() for the header one
        composeTestRule.onAllNodesWithText("Save")[0].assertIsDisplayed()
    }
}
