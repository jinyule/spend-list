package com.spendlist.app.domain.usecase.category

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ManageCategoryUseCaseTest {

    private lateinit var repository: CategoryRepository
    private lateinit var useCase: ManageCategoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ManageCategoryUseCase(repository)
    }

    private fun createCategory(
        id: Long = 0,
        name: String = "Mobile Plan",
        iconName: String = "PhoneAndroid",
        color: Long = 0xFFE74C3C,
        isPreset: Boolean = false,
        sortOrder: Int = 100
    ) = Category(
        id = id,
        name = name,
        iconName = iconName,
        color = color,
        isPreset = isPreset,
        sortOrder = sortOrder
    )

    // --- Add ---

    @Test
    fun addCategory_withValidData_returnsSuccess() = runTest {
        coEvery { repository.getAll() } returns flowOf(emptyList())
        coEvery { repository.insert(any()) } returns 9L

        val result = useCase.add(createCategory())

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.Success::class.java)
        assertThat((result as ManageCategoryUseCase.Result.Success).id).isEqualTo(9L)
        coVerify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun addCategory_withEmptyName_returnsValidationError() = runTest {
        val result = useCase.add(createCategory(name = ""))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.ValidationError::class.java)
        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun addCategory_withBlankName_returnsValidationError() = runTest {
        val result = useCase.add(createCategory(name = "   "))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.ValidationError::class.java)
    }

    @Test
    fun addCategory_withDuplicateName_returnsValidationError() = runTest {
        val existing = createCategory(id = 1, name = "Mobile Plan")
        coEvery { repository.getAll() } returns flowOf(listOf(existing))

        val result = useCase.add(createCategory(name = "Mobile Plan"))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.ValidationError::class.java)
        assertThat((result as ManageCategoryUseCase.Result.ValidationError).message)
            .contains("duplicate")
        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun addCategory_withDuplicateNameDifferentCase_returnsValidationError() = runTest {
        val existing = createCategory(id = 1, name = "Mobile Plan")
        coEvery { repository.getAll() } returns flowOf(listOf(existing))

        val result = useCase.add(createCategory(name = "mobile plan"))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.ValidationError::class.java)
    }

    // --- Update ---

    @Test
    fun updateCategory_withValidData_returnsSuccess() = runTest {
        coEvery { repository.getAll() } returns flowOf(emptyList())
        coEvery { repository.update(any()) } returns Unit

        val result = useCase.update(createCategory(id = 9, name = "Updated Name"))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.Success::class.java)
        coVerify(exactly = 1) { repository.update(any()) }
    }

    @Test
    fun updateCategory_withDuplicateNameOfOtherCategory_returnsValidationError() = runTest {
        val other = createCategory(id = 2, name = "Cloud")
        coEvery { repository.getAll() } returns flowOf(listOf(other))

        val result = useCase.update(createCategory(id = 9, name = "Cloud"))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.ValidationError::class.java)
    }

    @Test
    fun updateCategory_withOwnExistingName_returnsSuccess() = runTest {
        val self = createCategory(id = 9, name = "Mobile Plan")
        coEvery { repository.getAll() } returns flowOf(listOf(self))
        coEvery { repository.update(any()) } returns Unit

        val result = useCase.update(createCategory(id = 9, name = "Mobile Plan"))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.Success::class.java)
    }

    // --- Delete ---

    @Test
    fun deleteCategory_customCategory_returnsSuccess() = runTest {
        coEvery { repository.delete(any()) } returns Unit

        val result = useCase.delete(createCategory(id = 9, isPreset = false))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.Success::class.java)
        coVerify(exactly = 1) { repository.delete(any()) }
    }

    @Test
    fun deleteCategory_presetCategory_returnsError() = runTest {
        val result = useCase.delete(createCategory(id = 1, isPreset = true))

        assertThat(result).isInstanceOf(ManageCategoryUseCase.Result.ValidationError::class.java)
        assertThat((result as ManageCategoryUseCase.Result.ValidationError).message)
            .contains("preset")
        coVerify(exactly = 0) { repository.delete(any()) }
    }
}
