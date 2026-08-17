package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.repository.CategoryRepository
import com.quicklogger.app.domain.repository.ExpenseRepository
import com.quicklogger.app.domain.repository.LastCategoryStore
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.repository.ReceiptStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-ins for the data layer. ARCHITECTURE §12: use case tests fake
 * repositories, ViewModel tests fake use cases — never both in one test.
 */
class FakeExpenseRepository : ExpenseRepository {
    private val rows = MutableStateFlow<List<Expense>>(emptyList())

    val inserted: List<Expense> get() = rows.value

    override fun observeAllNewestFirst(): Flow<List<Expense>> =
        rows.map { list -> list.sortedByDescending { it.occurredAt } }

    override suspend fun insert(expense: Expense): Expense {
        val stored = expense.copy(id = rows.value.size + 1L)
        rows.value = rows.value + stored
        return stored
    }
}

class FakeCategoryRepository(categories: List<Category> = emptyList()) : CategoryRepository {
    private val rows = MutableStateFlow(categories)

    fun emit(categories: List<Category>) {
        rows.value = categories
    }

    override fun observeAll(): Flow<List<Category>> = rows.asStateFlow()

    override suspend fun getById(id: Long): Category? = rows.value.firstOrNull { it.id == id }
}

/**
 * In-memory receipt storage. [files] maps relative path to its byte length, so a
 * zero-length entry models a camera that reported success but wrote nothing.
 */
class FakeReceiptStore(
    private val importFailure: ReceiptError? = null,
) : ReceiptStore {
    val files = linkedMapOf<String, Long>()
    val deleted = mutableListOf<String>()
    private var nextId = 0

    /** Simulates the camera writing bytes into a draft it was handed. */
    fun writeBytes(relativePath: String, length: Long = 1_024) {
        files[relativePath] = length
    }

    override suspend fun createDraft(): String {
        val path = "draft-${nextId++}.jpg"
        files[path] = 0L
        return path
    }

    override suspend fun importFrom(sourceUri: String): String {
        importFailure?.let { throw it }
        val path = "imported-${nextId++}.jpg"
        files[path] = 2_048L
        return path
    }

    override suspend fun delete(relativePath: String) {
        files.remove(relativePath)
        deleted += relativePath
    }

    override suspend fun hasContent(relativePath: String): Boolean =
        (files[relativePath] ?: 0L) > 0L
}

class FakeLastCategoryStore(private var stored: Long? = null) : LastCategoryStore {
    val writes = mutableListOf<Long>()

    override suspend fun lastSelectedId(): Long? = stored

    override suspend fun setLastSelectedId(id: Long) {
        stored = id
        writes += id
    }
}
