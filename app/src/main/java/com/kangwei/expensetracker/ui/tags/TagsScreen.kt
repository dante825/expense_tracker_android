package com.kangwei.expensetracker.ui.tags

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangwei.expensetracker.data.db.entity.TagEntity
import com.kangwei.expensetracker.data.db.relation.TagWithExpenses
import com.kangwei.expensetracker.ui.components.formatCurrency

@Composable
fun TagsScreen(innerPadding: PaddingValues, vm: TagsViewModel = viewModel()) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var tagToRename by remember { mutableStateOf<TagEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var tagToDelete by remember { mutableStateOf<TagWithExpenses?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add tag")
            }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), scaffoldPadding.calculateBottomPadding()) + 80.dp
            )
        ) {
            if (tags.isEmpty()) {
                item {
                    Text(
                        "No tags yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(tags, key = { it.tag.id }) { item ->
                val totalSpend = item.expenses.filter { !it.isIncome }.sumOf { it.amount }
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            tagToDelete = item; true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                            Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                ) {
                    Surface(
                        onClick = { tagToRename = item.tag; renameText = item.tag.name },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.tag.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${item.expenses.size} expense${if (item.expenses.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (totalSpend > 0) {
                                Text(
                                    formatCurrency(totalSpend),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; newTagName = "" },
            title = { Text("New Tag") },
            text = { OutlinedTextField(value = newTagName, onValueChange = { newTagName = it }, label = { Text("Tag name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { vm.add(newTagName); newTagName = ""; showAdd = false }, enabled = newTagName.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; newTagName = "" }) { Text("Cancel") } }
        )
    }

    tagToRename?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = { Text("Rename Tag") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("Tag name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { vm.rename(tag, renameText); tagToRename = null }, enabled = renameText.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { tagToRename = null }) { Text("Cancel") } }
        )
    }

    tagToDelete?.let { item ->
        val count = item.expenses.size
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete \"${item.tag.name}\"?") },
            text = {
                if (count > 0) Text("This tag is attached to $count expense${if (count == 1) "" else "s"}.")
                else Text("This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = { vm.delete(item.tag); tagToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("Cancel") } }
        )
    }
}
