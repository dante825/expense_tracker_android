package com.kangwei.expensetracker.ui.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangwei.expensetracker.ui.components.formatCurrency
import com.kangwei.expensetracker.ui.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(innerPadding: PaddingValues, vm: RecurringViewModel = viewModel()) {
    val recurrings by vm.recurrings.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add recurring")
            }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), scaffoldPadding.calculateBottomPadding()) + 80.dp
            )
        ) {
            if (recurrings.isEmpty()) {
                item {
                    Text(
                        "No recurring expenses yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(recurrings, key = { it.recurring.id }) { item ->
                val r = item.recurring
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (r.isActive) 1f else 0.55f)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            r.description,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (!r.isActive) TextDecoration.LineThrough else null
                        )
                        Text(
                            "${r.frequency.replaceFirstChar { it.uppercase() }} · ${item.category?.name ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Next: ${formatDate(r.nextDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatCurrency(r.amount), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = { vm.toggleActive(r) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (r.isActive) "Pause" else "Resume", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showAdd) {
        AddRecurringSheet(
            categories = categories,
            onDismiss = { showAdd = false },
            onSave = { amount, desc, freq, start, end, catId ->
                vm.add(amount, desc, freq, start, end, catId)
                showAdd = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringSheet(
    categories: List<com.kangwei.expensetracker.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Long, Long?, Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("monthly") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hasEndDate by remember { mutableStateOf(false) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showStartDate by remember { mutableStateOf(false) }
    var showEndDate by remember { mutableStateOf(false) }
    var freqExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val isValid = amount.toDoubleOrNull() != null && selectedCategoryId != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New Recurring", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onSave(amount.toDouble(), desc, frequency, startDate, if (hasEndDate) endDate else null, selectedCategoryId!!) },
                    enabled = isValid
                ) { Text("Add") }
            }

            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // Frequency
            ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = it }) {
                OutlinedTextField(
                    value = frequency.replaceFirstChar { it.uppercase() },
                    onValueChange = {}, readOnly = true, label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(freqExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                    listOf("daily", "weekly", "monthly").forEach { f ->
                        DropdownMenuItem(text = { Text(f.replaceFirstChar { it.uppercase() }) }, onClick = { frequency = f; freqExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = formatDate(startDate), onValueChange = {}, readOnly = true, label = { Text("Start date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { TextButton(onClick = { showStartDate = true }) { Text("Change") } }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Has end date", modifier = Modifier.weight(1f))
                Switch(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
            }
            if (hasEndDate) {
                OutlinedTextField(
                    value = formatDate(endDate), onValueChange = {}, readOnly = true, label = { Text("End date") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { TextButton(onClick = { showEndDate = true }) { Text("Change") } }
                )
            }

            // Category
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(
                    value = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select a category",
                    onValueChange = {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategoryId = cat.id; catExpanded = false })
                    }
                }
            }
        }
    }

    if (showStartDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(onDismissRequest = { showStartDate = false }, confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { startDate = it }; showStartDate = false }) { Text("OK") }
        }) { DatePicker(state = state) }
    }
    if (showEndDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(onDismissRequest = { showEndDate = false }, confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { endDate = it }; showEndDate = false }) { Text("OK") }
        }) { DatePicker(state = state) }
    }
}
