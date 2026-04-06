package com.spendlist.app.ui.screen.category

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.usecase.category.ManageCategoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var manageCategoryUseCase: ManageCategoryUseCase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categoriesFlow: MutableStateFlow<List<Category>>

    private val presetCategory = Category(
        id = 1, name = "AI Tools", nameResKey = "category_ai",
        iconName = "SmartToy", color = 0xFFFF6B35, isPreset = true, sortOrder = 0
    )
    private val customCategory = Category(
        id = 9, name = "Mobile Plan",
        iconName = "PhoneAndroid", color = 0xFFE74C3C, isPreset = false, sortOrder = 100
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        manageCategoryUseCase = mockk()
        categoryRepository = mockk()
        categoriesFlow = MutableStateFlow(listOf(presetCategory, customCategory))
        coEvery { categoryRepository.getAll() } returns categoriesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CategoryManageViewModel {
        return CategoryManageViewModel(categoryRepository, manageCategoryUseCase)
    }

    @Test
    fun initialState_loadsAllCategories() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.presetCategories).hasSize(1)
        assertThat(state.customCategories).hasSize(1)
        assertThat(state.presetCategories[0].name).isEqualTo("AI Tools")
        assertThat(state.customCategories[0].name).isEqualTo("Mobile Plan")
    }

    @Test
    fun addCategory_withValidData_updatesListViaFlow() = runTest {
        coEvery { manageCategoryUseCase.add(any()) } returns ManageCategoryUseCase.Result.Success(10L)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddCategory("Gaming", "SportsEsports", 0xFFFF6B6B)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { manageCategoryUseCase.add(any()) }
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun addCategory_withEmptyName_showsError() = runTest {
        coEvery { manageCategoryUseCase.add(any()) } returns
            ManageCategoryUseCase.Result.ValidationError("Name is required")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddCategory("", "SportsEsports", 0xFFFF6B6B)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNotNull()
    }

    @Test
    fun updateCategory_success() = runTest {
        coEvery { manageCategoryUseCase.update(any()) } returns ManageCategoryUseCase.Result.Success(9L)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUpdateCategory(customCategory.copy(name = "Updated"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { manageCategoryUseCase.update(any()) }
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun deleteCategory_customCategory_success() = runTest {
        coEvery { manageCategoryUseCase.delete(any()) } returns ManageCategoryUseCase.Result.Success(9L)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteCategory(customCategory)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { manageCategoryUseCase.delete(any()) }
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun deleteCategory_presetCategory_showsError() = runTest {
        coEvery { manageCategoryUseCase.delete(any()) } returns
            ManageCategoryUseCase.Result.ValidationError("Cannot delete preset category")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteCategory(presetCategory)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNotNull()
    }

    @Test
    fun clearError_resetsErrorState() = runTest {
        coEvery { manageCategoryUseCase.add(any()) } returns
            ManageCategoryUseCase.Result.ValidationError("error")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddCategory("", "Icon", 0xFF000000)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.error).isNotNull()

        viewModel.onClearError()
        assertThat(viewModel.uiState.value.error).isNull()
    }
}
