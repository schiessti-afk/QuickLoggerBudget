package com.quicklogger.app.domain.repository

import com.quicklogger.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>

    suspend fun getById(id: Long): Category?

    /**
     * Appends a new custom category (`sortOrder` after every existing row) and
     * returns it. Throws [com.quicklogger.app.domain.usecase.CategoryError.DuplicateName]
     * for a case-insensitive name collision — the implementation is what turns the
     * Room unique-index violation into this domain-safe type.
     */
    suspend fun insert(name: String): Category

    /** Same duplicate-name behavior as [insert]. */
    suspend fun rename(id: Long, name: String)

    /**
     * Deletes [id] after reassigning its expenses to [reassignExpensesTo], both in
     * one transaction. Never deletes a protected row, defensively, even if called
     * with a protected [id].
     */
    suspend fun delete(id: Long, reassignExpensesTo: Long)
}
