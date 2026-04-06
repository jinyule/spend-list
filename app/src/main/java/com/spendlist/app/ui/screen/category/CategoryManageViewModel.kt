package com.spendlist.app.ui.screen.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.usecase.category.ManageCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManageUiState(
    val presetCategories: List<Category> = emptyList(),
    val customCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val manageCategoryUseCase: ManageCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManageUiState())
    val uiState: StateFlow<CategoryManageUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAll()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { categories ->
                    _uiState.value = _uiState.value.copy(
                        presetCategories = categories.filter { it.isPreset },
                        customCategories = categories.filter { !it.isPreset },
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun onAddCategory(name: String, iconName: String, color: Long) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                iconName = iconName,
                color = color,
                isPreset = false,
                sortOrder = (_uiState.value.presetCategories.size + _uiState.value.customCategories.size)
            )
            when (val result = manageCategoryUseCase.add(category)) {
                is ManageCategoryUseCase.Result.Success -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is ManageCategoryUseCase.Result.ValidationError -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun onUpdateCategory(category: Category) {
        viewModelScope.launch {
            when (val result = manageCategoryUseCase.update(category)) {
                is ManageCategoryUseCase.Result.Success -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is ManageCategoryUseCase.Result.ValidationError -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun onDeleteCategory(category: Category) {
        viewModelScope.launch {
            when (val result = manageCategoryUseCase.delete(category)) {
                is ManageCategoryUseCase.Result.Success -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is ManageCategoryUseCase.Result.ValidationError -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun onClearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
