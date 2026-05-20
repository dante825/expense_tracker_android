package com.kangwei.expensetracker.ui.addedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseSheet(
    existing: ExpenseWithDetails? = null,
    onDismiss: () -> Unit,
    vm: AddEditExpenseViewModel = viewModel()
) {
    LaunchedEffect(existing) {
        existing?.let { vm.loadExisting(it) }
    }

    val categories by vm.categories.collectAsStateWithLifecycle()
    val allTags by vm.allTags.collectAsStateWithLifecycle()
    val amount by vm.amount.collectAsStateWithLifecycle()
    val description by vm.description.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val isIncome by vm.isIncome.collectAsStateWithLifecycle()
    val selectedCategoryId by vm.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedTagIds by vm.selectedTagIds.collectAsStateWithLifecycle()
    val receiptData by vm.receiptData.collectAsStateWithLifecycle()
    val isValid by vm.isValid.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            vm.receiptData.value = bytes
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (existing != null) "Edit Entry" else "New Entry",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { vm.save { onDismiss() } }, enabled = isValid) {
                    Text("Save")
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { vm.amount.value = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { vm.description.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date picker
            OutlinedTextField(
                value = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(date)),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("This is income", modifier = Modifier.weight(1f))
                Switch(checked = isIncome, onCheckedChange = { vm.isIncome.value = it })
            }

            // Category dropdown
            var catExpanded by remember { mutableStateOf(false) }
            val catName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select a category"
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(
                    value = catName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.name) }, onClick = {
                            vm.selectedCategoryId.value = cat.id; catExpanded = false
                        })
                    }
                }
            }

            // Tags section
            Text("Tags", style = MaterialTheme.typography.labelLarge)
            if (selectedTagIds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    allTags.filter { it.id in selectedTagIds }.sortedBy { it.name }.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { vm.selectedTagIds.value = selectedTagIds - tag.id },
                            label = { Text(tag.name) },
                            trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            var tagMenuExpanded by remember { mutableStateOf(false) }
            val unselectedTags = allTags.filter { it.id !in selectedTagIds }
            ExposedDropdownMenuBox(expanded = tagMenuExpanded, onExpandedChange = { tagMenuExpanded = it }) {
                OutlinedButton(onClick = { tagMenuExpanded = true }, modifier = Modifier.menuAnchor()) {
                    Text("Add Tag")
                }
                ExposedDropdownMenu(expanded = tagMenuExpanded, onDismissRequest = { tagMenuExpanded = false }) {
                    unselectedTags.forEach { tag ->
                        DropdownMenuItem(text = { Text(tag.name) }, onClick = {
                            vm.selectedTagIds.value = selectedTagIds + tag.id; tagMenuExpanded = false
                        })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("New Tag…") }, onClick = {
                        tagMenuExpanded = false; showNewTagDialog = true
                    })
                }
            }

            // Receipt
            Text("Receipt", style = MaterialTheme.typography.labelLarge)
            val bitmap = receiptData?.let {
                android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Receipt",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                )
                OutlinedButton(
                    onClick = { vm.receiptData.value = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove Receipt") }
            } else {
                OutlinedButton(onClick = {
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Attach Receipt Photo") }
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save { onDismiss() } }, enabled = isValid, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.date.value = it }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false; newTagName = "" },
            title = { Text("New Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addNewTag(newTagName)
                    newTagName = ""
                    showNewTagDialog = false
                }, enabled = newTagName.isNotBlank()) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false; newTagName = "" }) { Text("Cancel") }
            }
        )
    }
}
