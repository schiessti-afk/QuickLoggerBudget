package com.quicklogger.app.domain.repository

import com.quicklogger.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Category reads for the chip row. Create/rename/delete arrive in sprint 4.
 */
interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>

    suspend fun getById(id: Long): Category?
}
