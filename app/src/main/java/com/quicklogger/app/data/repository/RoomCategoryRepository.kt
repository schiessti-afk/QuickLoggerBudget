package com.quicklogger.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.quicklogger.app.data.local.CategoryDao
import com.quicklogger.app.data.local.CategoryEntity
import com.quicklogger.app.data.local.ExpenseDao
import com.quicklogger.app.data.local.QuickLoggerDatabase
import com.quicklogger.app.data.local.toDomain
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.CategoryRepository
import com.quicklogger.app.domain.usecase.CategoryError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * The only class allowed to import [SQLiteConstraintException]: it turns Room's
 * unique-index violation into [CategoryError.DuplicateName] — a domain-safe type —
 * before the failure ever reaches a use case.
 */
class RoomCategoryRepository @Inject constructor(
    private val database: QuickLoggerDatabase,
    private val categories: CategoryDao,
    private val expenses: ExpenseDao,
    private val clock: Clock,
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> =
        categories.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? = categories.getById(id)?.toDomain()

    override suspend fun insert(name: String): Category = mappingDuplicateName {
        database.withTransaction {
            val sortOrder = categories.maxSortOrder() + 1
            val id = categories.insert(
                CategoryEntity(name = name, sortOrder = sortOrder, isProtected = false),
            )
            Category(id = id, name = name, sortOrder = sortOrder, isProtected = false)
        }
    }

    override suspend fun rename(id: Long, name: String) = mappingDuplicateName {
        categories.rename(id, name)
    }

    override suspend fun delete(id: Long, reassignExpensesTo: Long) {
        database.withTransaction {
            expenses.reassignCategory(id, reassignExpensesTo, clock.instant().toEpochMilli())
            categories.deleteUnprotected(id)
        }
    }

    private suspend fun <T> mappingDuplicateName(block: suspend () -> T): T = try {
        block()
    } catch (e: SQLiteConstraintException) {
        throw CategoryError.DuplicateName
    }
}
