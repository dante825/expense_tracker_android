package com.kangwei.expensetracker.ui.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kangwei.expensetracker.ExpenseTrackerApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests DashboardScreen static UI structure using Robolectric.
 * @Config(application = ExpenseTrackerApp::class) makes Robolectric create the
 * real application singleton so viewModel() inside DashboardScreen can access
 * ExpenseTrackerApp.database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ExpenseTrackerApp::class)
class DashboardScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ── Period selector ──────────────────────────────────────────────────────

    @Test fun periodSelector_showsAllFourLabels() {
        composeTestRule.setContent { DashboardScreen(innerPadding = PaddingValues()) }
        composeTestRule.onNodeWithText("Week").assertIsDisplayed()
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Year").assertIsDisplayed()
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
    }

    // ── Summary cards ────────────────────────────────────────────────────────

    @Test fun summaryCards_showIncomeSpentNetLabels() {
        composeTestRule.setContent { DashboardScreen(innerPadding = PaddingValues()) }
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net").assertIsDisplayed()
    }

}
