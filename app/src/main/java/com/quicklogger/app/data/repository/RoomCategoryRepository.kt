package com.quicklogger.app.data.repository

import com.quicklogger.app.data.local.CategoryDao
import com.quicklogger.app.data.local.toDomain
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomCategoryRepository @Inject constructor(
    private val categories: CategoryDao,
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> =
        categories.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? = categories.getById(id)?.toDomain()
}
