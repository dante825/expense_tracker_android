package com.kangwei.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.kangwei.expensetracker.ui.categories.CategoriesScreen
import com.kangwei.expensetracker.ui.components.LocalCurrencyCode
import com.kangwei.expensetracker.ui.dashboard.DashboardScreen
import com.kangwei.expensetracker.ui.expenses.ExpensesScreen
import com.kangwei.expensetracker.ui.recurring.RecurringScreen
import com.kangwei.expensetracker.ui.settings.SettingsScreen
import com.kangwei.expensetracker.ui.settings.SettingsViewModel
import com.kangwei.expensetracker.ui.tags.TagsScreen

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Tab("dashboard", "Dashboard", Icons.Filled.PieChart)
    data object Expenses : Tab("expenses", "Expenses", Icons.Filled.List)
    data object Categories : Tab("categories", "Categories", Icons.Filled.Folder)
    data object Recurring : Tab("recurring", "Recurring", Icons.Filled.Repeat)
    data object Tags : Tab("tags", "Tags", Icons.Filled.Label)
    data object Settings : Tab("settings", "Settings", Icons.Filled.Settings)
}

private val tabs = listOf(Tab.Dashboard, Tab.Expenses, Tab.Categories, Tab.Recurring, Tab.Tags, Tab.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val settingsVm: SettingsViewModel = viewModel()
    val currencyCode by settingsVm.currencyCode.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalCurrencyCode provides currencyCode) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Dashboard.route,
            contentAlignment = androidx.compose.ui.Alignment.TopStart
        ) {
            composable(Tab.Dashboard.route) { DashboardScreen(innerPadding) }
            composable(Tab.Expenses.route) { ExpensesScreen(innerPadding) }
            composable(Tab.Categories.route) { CategoriesScreen(innerPadding) }
            composable(Tab.Recurring.route) { RecurringScreen(innerPadding) }
            composable(Tab.Tags.route) { TagsScreen(innerPadding) }
            composable(Tab.Settings.route) { SettingsScreen(innerPadding, settingsVm) }
        }
    }
    } // CompositionLocalProvider
}
