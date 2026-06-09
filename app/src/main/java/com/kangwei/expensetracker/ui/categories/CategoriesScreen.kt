package com.kangwei.expensetracker.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangwei.expensetracker.data.db.entity.CategoryEntity
import com.kangwei.expensetracker.ui.components.LocalCurrencyCode
import com.kangwei.expensetracker.ui.components.formatCurrency

@Composable
fun CategoriesScreen(innerPadding: PaddingValues, vm: CategoriesViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.seed() }
    val categories by vm.categories.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add category")
            }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), scaffoldPadding.calculateBottomPadding()) + 80.dp
            )
        ) {
            items(categories, key = { it.id }) { cat ->
                CategoryItem(cat, vm)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showAdd) {
        AddCategoryDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, budget -> vm.addCategory(name, budget); showAdd = false }
        )
    }
}

@Composable
fun CategoryItem(category: CategoryEntity, vm: CategoriesViewModel) {
    val currencyCode = LocalCurrencyCode.current
    var monthSpent by remember { mutableDoubleStateOf(0.0) }
    var editingBudget by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }

    LaunchedEffect(category.id) {
        vm.monthSpent(category.id) { monthSpent = it }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge)
        }

        if (category.budgetLimit > 0) {
            Spacer(Modifier.height(4.dp))
            val pct = (monthSpent / category.budgetLimit).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    pct >= 1f -> MaterialTheme.colorScheme.error
                    pct >= 0.8f -> Color(0xFFE65100)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spent: ${formatCurrency(monthSpent, currencyCode)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("of ${formatCurrency(category.budgetLimit, currencyCode)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(4.dp))

        if (editingBudget) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Budget limit") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    singleLine = true
                )
                TextButton(onClick = {
                    vm.updateBudget(category, budgetText.toDoubleOrNull() ?: 0.0)
                    editingBudget = false
                }) { Text("Set") }
                TextButton(onClick = { editingBudget = false }) { Text("Cancel") }
            }
        } else {
            TextButton(
                onClick = { budgetText = if (category.budgetLimit > 0) category.budgetLimit.toString() else ""; editingBudget = true },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    if (category.budgetLimit > 0) "Edit budget" else "Set budget limit",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onAdd: (String, Double?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category name") }, singleLine = true)
                OutlinedTextField(
                    value = budget, onValueChange = { budget = it }, label = { Text("Budget limit (optional)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name.trim(), budget.toDoubleOrNull()) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
