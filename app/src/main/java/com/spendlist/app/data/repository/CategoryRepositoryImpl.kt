package com.spendlist.app.data.repository

import com.spendlist.app.data.local.converter.toDomain
import com.spendlist.app.data.local.converter.toEntity
import com.spendlist.app.data.local.dao.CategoryDao
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> {
        return categoryDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): Category? {
        return categoryDao.getById(id)?.toDomain()
    }

    override suspend fun insert(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun update(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun delete(category: Category) {
        categoryDao.delete(category.toEntity())
    }
}
