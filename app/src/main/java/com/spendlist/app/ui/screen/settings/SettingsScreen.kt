package com.spendlist.app.ui.screen.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendlist.app.R
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.util.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onCategoryManageClick: () -> Unit,
    onExportData: (String, String) -> Unit = { _, _ -> }, // (data, filename) -> save to SAF
    onRequestImport: () -> Unit = {},
    importContent: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Handle imported content from SAF
    LaunchedEffect(importContent) {
        if (importContent != null) {
            if (importContent.trimStart().startsWith("{") || importContent.trimStart().startsWith("[")) {
                viewModel.onImportJson(importContent)
            } else {
                viewModel.onImportCsv(importContent)
            }
        }
    }

    val context = LocalContext.current

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onReminderEnabledChanged(true)
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onReminderEnabledChanged(true)
            }
        } else {
            viewModel.onReminderEnabledChanged(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Theme
            val themeLabel = when (uiState.themeMode) {
                1 -> stringResource(R.string.settings_theme_light)
                2 -> stringResource(R.string.settings_theme_dark)
                else -> stringResource(R.string.settings_theme_system)
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(themeLabel) },
                leadingContent = {
                    Icon(Icons.Default.Palette, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable { showThemeDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Language
            val languageLabel = when (uiState.languageCode) {
                "en" -> stringResource(R.string.settings_language_en)
                "zh-CN" -> stringResource(R.string.settings_language_zh)
                else -> stringResource(R.string.settings_language_system)
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                supportingContent = { Text(languageLabel) },
                leadingContent = {
                    Icon(Icons.Default.Language, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable { showLanguageDialog = true }
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
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                requestNotificationPermissionIfNeeded()
                            } else {
                                viewModel.onReminderEnabledChanged(false)
                            }
                        }
                    )
                }
            )

            // Reminder Days (only show when enabled)
            if (uiState.reminderEnabled) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_remind_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 1, 0).forEach { days ->
                            val label = when (days) {
                                3 -> stringResource(R.string.settings_remind_3days)
                                1 -> stringResource(R.string.settings_remind_1day)
                                else -> stringResource(R.string.settings_remind_today)
                            }
                            FilterChip(
                                selected = uiState.reminderDays.contains(days),
                                onClick = {
                                    val currentDays = uiState.reminderDays.toMutableSet()
                                    if (currentDays.contains(days)) {
                                        currentDays.remove(days)
                                    } else {
                                        currentDays.add(days)
                                    }
                                    viewModel.onReminderDaysChanged(currentDays)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
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

        // Import result message
        if (uiState.importMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val isSuccess = uiState.importMessage!!.startsWith("import_success")
            val messageText = if (isSuccess) {
                val count = uiState.importMessage!!.substringAfter(":").toIntOrNull() ?: 0
                stringResource(R.string.settings_import_success, count)
            } else {
                val error = uiState.importMessage!!.substringAfter(":")
                stringResource(R.string.settings_import_error, error)
            }
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
                    Text(text = messageText, modifier = Modifier.weight(1f))
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

    // Theme picker dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    listOf(
                        0 to R.string.settings_theme_system,
                        1 to R.string.settings_theme_light,
                        2 to R.string.settings_theme_dark
                    ).forEach { (mode, labelRes) ->
                        ListItem(
                            headlineContent = { Text(stringResource(labelRes)) },
                            leadingContent = {
                                RadioButton(
                                    selected = uiState.themeMode == mode,
                                    onClick = {
                                        viewModel.onThemeModeChanged(mode)
                                        showThemeDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onThemeModeChanged(mode)
                                showThemeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Language picker dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    listOf(
                        "" to R.string.settings_language_system,
                        "en" to R.string.settings_language_en,
                        "zh-CN" to R.string.settings_language_zh
                    ).forEach { (code, labelRes) ->
                        ListItem(
                            headlineContent = { Text(stringResource(labelRes)) },
                            leadingContent = {
                                RadioButton(
                                    selected = uiState.languageCode == code,
                                    onClick = {
                                        viewModel.onLanguageChanged(code)
                                        LocaleHelper.setLocale(context, code)
                                        showLanguageDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onLanguageChanged(code)
                                LocaleHelper.setLocale(context, code)
                                showLanguageDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
