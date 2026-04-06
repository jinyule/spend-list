package com.spendlist.app.domain.usecase.category

import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ManageCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {

    sealed class Result {
        data class Success(val id: Long = 0) : Result()
        data class ValidationError(val message: String) : Result()
    }

    suspend fun add(category: Category): Result {
        if (category.name.isBlank()) {
            return Result.ValidationError("Name is required")
        }
        if (isDuplicateName(category.name, excludeId = null)) {
            return Result.ValidationError("Category name duplicate")
        }
        val id = repository.insert(category)
        return Result.Success(id)
    }

    suspend fun update(category: Category): Result {
        if (category.name.isBlank()) {
            return Result.ValidationError("Name is required")
        }
        if (isDuplicateName(category.name, excludeId = category.id)) {
            return Result.ValidationError("Category name duplicate")
        }
        repository.update(category)
        return Result.Success(category.id)
    }

    suspend fun delete(category: Category): Result {
        if (category.isPreset) {
            return Result.ValidationError("Cannot delete preset category")
        }
        repository.delete(category)
        return Result.Success(category.id)
    }

    private suspend fun isDuplicateName(name: String, excludeId: Long?): Boolean {
        val existing = repository.getAll().first()
        return existing.any {
            it.name.equals(name, ignoreCase = true) && it.id != excludeId
        }
    }
}
