package com.spendlist.app.ui.screen.addEdit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendlist.app.R
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.ui.component.getIconByName
import com.spendlist.app.ui.component.resolvedCategoryName
import com.spendlist.app.util.DateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showNextRenewalDatePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditMode) R.string.edit_subscription
                            else R.string.add_subscription
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon + Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon preview
                val iconUriValue = uiState.iconUri
                Icon(
                    imageVector = if (iconUriValue != null) getIconByName(iconUriValue)
                    else Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(onClick = { showIconPicker = true }) {
                    Text(stringResource(R.string.field_icon))
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.field_name)) },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Category Dropdown
            CategoryDropdown(
                categories = uiState.categories,
                selectedId = uiState.categoryId,
                onSelect = viewModel::onCategoryChange,
                modifier = Modifier.fillMaxWidth()
            )

            // Amount + Currency
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text(stringResource(R.string.field_amount)) },
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                CurrencyDropdown(
                    selected = uiState.currency,
                    onSelect = viewModel::onCurrencyChange,
                    modifier = Modifier.width(120.dp)
                )
            }

            // Billing Cycle
            Text(
                text = stringResource(R.string.field_billing_cycle),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "MONTHLY" to R.string.cycle_monthly,
                    "QUARTERLY" to R.string.cycle_quarterly,
                    "YEARLY" to R.string.cycle_yearly,
                    "CUSTOM" to R.string.cycle_custom
                ).forEach { (type, labelRes) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.billingCycleType == type,
                            onClick = { viewModel.onBillingCycleTypeChange(type) }
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Custom days input
            if (uiState.billingCycleType == "CUSTOM") {
                OutlinedTextField(
                    value = uiState.customDays,
                    onValueChange = viewModel::onCustomDaysChange,
                    label = { Text(stringResource(R.string.cycle_custom)) },
                    isError = uiState.customDaysError != null,
                    supportingText = uiState.customDaysError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Billing day of month (optional, not for Custom)
            if (uiState.billingCycleType != "CUSTOM") {
                BillingDayDropdown(
                    selectedDay = uiState.billingDayOfMonth,
                    onDaySelect = viewModel::onBillingDayChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Start Date with DatePicker
            OutlinedTextField(
                value = DateFormatter.format(uiState.startDate),
                onValueChange = {},
                label = { Text(stringResource(R.string.field_start_date)) },
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Next Renewal Date — auto-calculated for new subs, manually adjustable in edit mode
            OutlinedTextField(
                value = DateFormatter.format(uiState.nextRenewalDate),
                onValueChange = {},
                label = { Text(stringResource(R.string.field_next_renewal)) },
                supportingText = {
                    Text(
                        stringResource(
                            if (uiState.isEditMode) R.string.field_next_renewal_hint_edit
                            else R.string.field_next_renewal_hint
                        )
                    )
                },
                trailingIcon = if (uiState.isEditMode) {
                    {
                        IconButton(onClick = { showNextRenewalDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select next renewal date")
                        }
                    }
                } else null,
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Manage URL
            OutlinedTextField(
                value = uiState.manageUrl,
                onValueChange = viewModel::onManageUrlChange,
                label = { Text(stringResource(R.string.field_manage_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Note
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.field_note)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Next Renewal DatePicker (edit mode only)
        if (showNextRenewalDatePicker) {
            val nextRenewalPickerState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.nextRenewalDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showNextRenewalDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            nextRenewalPickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                viewModel.onNextRenewalDateChange(date)
                            }
                            showNextRenewalDatePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNextRenewalDatePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            ) {
                DatePicker(state = nextRenewalPickerState)
            }
        }

        // DatePicker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.startDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                viewModel.onStartDateChange(date)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Icon Picker Dialog
        if (showIconPicker) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showIconPicker = false },
                title = { Text(stringResource(R.string.field_icon)) },
                text = {
                    com.spendlist.app.ui.component.IconPicker(
                        selectedIconName = uiState.iconUri ?: "Face",
                        onIconSelected = { iconName ->
                            viewModel.onIconChange(iconName)
                        },
                        modifier = Modifier.height(200.dp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showIconPicker = false }) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: Currency,
    onSelect: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.code,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_currency)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.symbol} ${currency.code}") },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory?.let { resolvedCategoryName(it) }
                ?: stringResource(R.string.home_filter_all),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = {
                if (selectedCategory != null) {
                    Icon(
                        imageVector = getIconByName(selectedCategory.iconName),
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "All" / None option
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_filter_all)) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getIconByName(category.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(resolvedCategoryName(category))
                        }
                    },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingDayDropdown(
    selectedDay: String,
    onDaySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val dayInt = selectedDay.toIntOrNull()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (dayInt != null) dayInt.toString() else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_billing_day)) },
            placeholder = { Text(stringResource(R.string.field_billing_day_hint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.field_billing_day_none)) },
                onClick = {
                    onDaySelect("")
                    expanded = false
                }
            )
            (1..31).forEach { day ->
                DropdownMenuItem(
                    text = { Text(day.toString()) },
                    onClick = {
                        onDaySelect(day.toString())
                        expanded = false
                    }
                )
            }
        }
    }
}
