package com.kangwei.expensetracker.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import com.kangwei.expensetracker.ui.addedit.AddEditExpenseSheet
import com.kangwei.expensetracker.ui.components.ExpenseListItem
import com.kangwei.expensetracker.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(innerPadding: PaddingValues, vm: ExpensesViewModel = viewModel()) {
    val filtered by vm.filtered.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val allTags by vm.allTags.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val selectedCategoryId by vm.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedTagId by vm.selectedTagId.collectAsStateWithLifecycle()
    val entryType by vm.entryTypeFilter.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ExpenseWithDetails?>(null) }

    val filteredIncome = filtered.filter { it.expense.isIncome }.sumOf { it.expense.amount }
    val filteredSpent = filtered.filter { !it.expense.isIncome }.sumOf { it.expense.amount }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), scaffoldPadding.calculateBottomPadding()) + 80.dp
            )
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { vm.search.value = it },
                    label = { Text("Search expenses") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    listOf("All" to "all", "Expenses" to "expense", "Income" to "income").forEachIndexed { i, (label, value) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, 3),
                            onClick = { vm.entryTypeFilter.value = value },
                            selected = entryType == value
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBoxCategoryPicker(
                        categories = categories.map { Pair(it.id, it.name) },
                        selectedId = selectedCategoryId,
                        onSelect = { vm.selectedCategoryId.value = it },
                        modifier = Modifier.weight(1f)
                    )
                    if (allTags.isNotEmpty()) {
                        ExposedDropdownMenuBoxCategoryPicker(
                            categories = allTags.map { Pair(it.id, it.name) },
                            selectedId = selectedTagId,
                            onSelect = { vm.selectedTagId.value = it },
                            modifier = Modifier.weight(1f),
                            allLabel = "All Tags"
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (filtered.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Income: ${formatCurrency(filteredIncome)}", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelMedium)
                        Text("Spent: ${formatCurrency(filteredSpent)}", color = Color(0xFFC62828), style = MaterialTheme.typography.labelMedium)
                    }
                    HorizontalDivider()
                }
            }

            items(filtered, key = { it.expense.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            vm.delete(item)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                ) {
                    Surface(
                        onClick = { editTarget = item },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                            ExpenseListItem(item)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showAdd) {
        AddEditExpenseSheet(onDismiss = { showAdd = false })
    }
    editTarget?.let { target ->
        AddEditExpenseSheet(existing = target, onDismiss = { editTarget = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxCategoryPicker(
    categories: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = "All Categories"
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.first == selectedId }?.second ?: allLabel
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); expanded = false })
            categories.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}
