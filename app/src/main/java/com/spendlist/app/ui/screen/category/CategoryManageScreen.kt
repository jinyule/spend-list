package com.spendlist.app.ui.screen.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendlist.app.R
import com.spendlist.app.domain.model.Category
import com.spendlist.app.ui.component.AVAILABLE_COLORS
import com.spendlist.app.ui.component.AVAILABLE_ICONS
import com.spendlist.app.ui.component.ColorPicker
import com.spendlist.app.ui.component.IconPicker
import com.spendlist.app.ui.component.getIconByName
import com.spendlist.app.ui.component.resolvedCategoryName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(uiState.error) {
        // Error is shown via snackbar below
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_manage_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.category_add))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preset categories section
            item {
                Text(
                    text = stringResource(R.string.category_preset),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(uiState.presetCategories, key = { it.id }) { category ->
                CategoryItem(
                    category = category,
                    onEdit = null,
                    onDelete = null
                )
            }

            // Custom categories section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.category_custom),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.customCategories.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.category_empty_custom),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(uiState.customCategories, key = { it.id }) { category ->
                    CategoryItem(
                        category = category,
                        onEdit = { editingCategory = category },
                        onDelete = { deletingCategory = category }
                    )
                }
            }
        }

        // Error snackbar
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.onClearError() }) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            ) {
                Text(uiState.error!!)
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        CategoryEditDialog(
            title = stringResource(R.string.category_add),
            initialName = "",
            initialIconName = AVAILABLE_ICONS.first().name,
            initialColor = AVAILABLE_COLORS.first(),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, iconName, color ->
                viewModel.onAddCategory(name, iconName, color)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    if (editingCategory != null) {
        val cat = editingCategory!!
        CategoryEditDialog(
            title = stringResource(R.string.category_edit),
            initialName = cat.name,
            initialIconName = cat.iconName,
            initialColor = cat.color,
            onDismiss = { editingCategory = null },
            onConfirm = { name, iconName, color ->
                viewModel.onUpdateCategory(cat.copy(name = name, iconName = iconName, color = color))
                editingCategory = null
            }
        )
    }

    // Delete confirmation dialog
    if (deletingCategory != null) {
        val cat = deletingCategory!!
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text(stringResource(R.string.category_delete_confirm_title)) },
            text = { Text(stringResource(R.string.category_delete_confirm_message, cat.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteCategory(cat)
                    deletingCategory = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color circle with icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(category.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category.iconName),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name (resolved via i18n for preset categories)
            Text(
                text = resolvedCategoryName(category),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // Actions (only for custom categories)
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    title: String,
    initialName: String,
    initialIconName: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconName: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIconName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Icon picker
                Text(
                    text = stringResource(R.string.category_icon),
                    style = MaterialTheme.typography.labelLarge
                )
                IconPicker(
                    selectedIconName = selectedIcon,
                    onIconSelected = { selectedIcon = it },
                    modifier = Modifier.heightIn(max = 200.dp)
                )

                // Color picker
                Text(
                    text = stringResource(R.string.category_color),
                    style = MaterialTheme.typography.labelLarge
                )
                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    modifier = Modifier.heightIn(max = 100.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, selectedIcon, selectedColor) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
