package com.spendlist.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendlist.app.R
import com.spendlist.app.domain.model.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onCategoryManageClick: () -> Unit,
    onExportData: (String, String) -> Unit = { _, _ -> }, // (data, filename) -> save to SAF
    onRequestImport: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCurrencyPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // General section
        Text(
            text = stringResource(R.string.settings_general),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            // Primary Currency
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_primary_currency)) },
                supportingContent = { Text("${uiState.primaryCurrency.symbol} ${uiState.primaryCurrency.displayName}") },
                leadingContent = {
                    Icon(Icons.Default.Paid, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable { showCurrencyPicker = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reminders section
        Text(
            text = stringResource(R.string.settings_reminders),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reminder_enabled)) },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = { viewModel.onReminderEnabledChanged(it) }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data Management section
        Text(
            text = stringResource(R.string.settings_data),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            // Category Management
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_categories)) },
                leadingContent = {
                    Icon(Icons.Default.Category, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable { onCategoryManageClick() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Sync Exchange Rates
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_exchange_rates)) },
                supportingContent = {
                    if (uiState.isSyncingRates) {
                        Text(stringResource(R.string.settings_syncing_rates))
                    }
                },
                leadingContent = {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null)
                },
                trailingContent = {
                    if (uiState.isSyncingRates) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { viewModel.onSyncRates() }) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.clickable(enabled = !uiState.isSyncingRates) {
                    viewModel.onSyncRates()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Export JSON
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export_json)) },
                leadingContent = {
                    Icon(Icons.Default.Upload, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    viewModel.onExportJson { data ->
                        onExportData(data, "subscriptions.json")
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Export CSV
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export_csv)) },
                leadingContent = {
                    Icon(Icons.Default.Upload, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    viewModel.onExportCsv { data ->
                        onExportData(data, "subscriptions.csv")
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Import
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import)) },
                leadingContent = {
                    Icon(Icons.Default.Download, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable { onRequestImport() }
            )
        }

        // Sync result message
        if (uiState.rateSyncMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val isSuccess = uiState.rateSyncMessage == "success"
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSuccess) stringResource(R.string.settings_rates_synced)
                        else stringResource(R.string.settings_rates_sync_failed),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.onClearMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            }
        }
    }

    // Currency picker dialog
    if (showCurrencyPicker) {
        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            title = { Text(stringResource(R.string.settings_primary_currency)) },
            text = {
                Column {
                    Currency.entries.forEach { currency ->
                        ListItem(
                            headlineContent = { Text("${currency.symbol} ${currency.displayName}") },
                            supportingContent = { Text(currency.code) },
                            leadingContent = {
                                RadioButton(
                                    selected = currency == uiState.primaryCurrency,
                                    onClick = {
                                        viewModel.onPrimaryCurrencyChanged(currency)
                                        showCurrencyPicker = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onPrimaryCurrencyChanged(currency)
                                showCurrencyPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
