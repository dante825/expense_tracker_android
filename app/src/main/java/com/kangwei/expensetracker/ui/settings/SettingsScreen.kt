package com.kangwei.expensetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(innerPadding: PaddingValues, vm: SettingsViewModel = viewModel()) {
    val currencyCode by vm.currencyCode.collectAsStateWithLifecycle()
    val isExporting by vm.isExporting.collectAsStateWithLifecycle()
    val exportIntent by vm.exportShareIntent.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(exportIntent) {
        exportIntent?.let {
            context.startActivity(it)
            vm.clearExportIntent()
        }
    }

    LazyColumn(contentPadding = innerPadding) {
        item {
            // Export section
            Text(
                "Data",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Export to CSV", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Share all expenses as a spreadsheet file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    OutlinedButton(onClick = { vm.exportToCsv() }) {
                        Text("Export")
                    }
                }
            }
            HorizontalDivider()
        }

        item {
            // Currency section
            Text(
                "Currency",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Text(
                "Choose how amounts are displayed throughout the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )
            HorizontalDivider()
        }

        items(SUPPORTED_CURRENCIES) { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.setCurrency(option.code) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currencyCode == option.code,
                    onClick = { vm.setCurrency(option.code) }
                )
                Spacer(Modifier.width(8.dp))
                Text(option.displayName, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}
