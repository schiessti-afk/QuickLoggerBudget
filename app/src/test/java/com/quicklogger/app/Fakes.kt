package com.quicklogger.app

import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.repository.BudgetTargetRepository
import com.quicklogger.app.domain.repository.CategoryRepository
import com.quicklogger.app.domain.repository.CsvExportStore
import com.quicklogger.app.domain.repository.ExpenseRepository
import com.quicklogger.app.domain.repository.LastCategoryStore
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.repository.ReceiptStore
import com.quicklogger.app.domain.usecase.CategoryError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * In-memory stand-ins for the data layer. ARCHITECTURE §12: use case tests fake
 * repositories, ViewModel tests fake use cases — never both in one test.
 */
class FakeExpenseRepository : ExpenseRepository {
    private val rows = MutableStateFlow<List<Expense>>(emptyList())

    val inserted: List<Expense> get() = rows.value
    val deletedIds = mutableListOf<Long>()

    override fun observeAllNewestFirst(): Flow<List<Expense>> =
        rows.map { list -> list.sortedByDescending { it.occurredAt } }

    override fun observeInRange(from: Instant, to: Instant): Flow<List<Expense>> = rows.map { list ->
        list.filter { it.occurredAt >= from && it.occurredAt < to }
            .sortedByDescending { it.occurredAt }
    }

    override suspend fun insert(expense: Expense): Expense {
        val stored = expense.copy(id = rows.value.size + 1L)
        rows.value = rows.value + stored
        return stored
    }

    override suspend fun getById(id: Long): Expense? = rows.value.firstOrNull { it.id == id }

    override suspend fun update(expense: Expense) {
        rows.value = rows.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
        deletedIds += id
    }
}

class FakeCategoryRepository(categories: List<Category> = emptyList()) : CategoryRepository {
    private val rows = MutableStateFlow(categories)
    private var nextId = (categories.maxOfOrNull { it.id } ?: 0L) + 1

    /** (deletedId, reassignedTo) pairs, so tests can assert the atomic-pair contract. */
    val deletions = mutableListOf<Pair<Long, Long>>()

    fun emit(categories: List<Category>) {
        rows.value = categories
    }

    override fun observeAll(): Flow<List<Category>> = rows.asStateFlow()

    override suspend fun getById(id: Long): Category? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(name: String): Category {
        if (rows.value.any { it.name.equals(name, ignoreCase = true) }) throw CategoryError.DuplicateName
        val sortOrder = (rows.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val category = Category(id = nextId++, name = name, sortOrder = sortOrder, isProtected = false)
        rows.value = rows.value + category
        return category
    }

    override suspend fun rename(id: Long, name: String) {
        if (rows.value.any { it.id != id && it.name.equals(name, ignoreCase = true) }) {
            throw CategoryError.DuplicateName
        }
        rows.value = rows.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun delete(id: Long, reassignExpensesTo: Long) {
        deletions += id to reassignExpensesTo
        rows.value = rows.value.filterNot { it.id == id }
    }
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

/** In-memory `cacheDir/exports/`. [written] maps file name to its last-written content. */
class FakeCsvExportStore : CsvExportStore {
    val written = linkedMapOf<String, String>()

    override suspend fun write(fileName: String, csv: String): String {
        written[fileName] = csv
        return fileName
    }
}

class FakeLastCategoryStore(private var stored: Long? = null) : LastCategoryStore {
    val writes = mutableListOf<Long>()

    override suspend fun lastSelectedId(): Long? = stored

    override suspend fun setLastSelectedId(id: Long) {
        stored = id
        writes += id
    }
}

/** Keyed by `categoryId` (including `null` for the overall row), mirroring the real DAO's upsert. */
class FakeBudgetTargetRepository(targets: List<BudgetTarget> = emptyList()) : BudgetTargetRepository {
    private val rows = MutableStateFlow(targets)

    override fun observeAll(): Flow<List<BudgetTarget>> = rows.asStateFlow()

    override suspend fun upsertOverall(amount: Money) = upsert(null, amount)

    override suspend fun upsertForCategory(categoryId: Long, amount: Money) = upsert(categoryId, amount)

    override suspend fun deleteOverall() {
        rows.value = rows.value.filterNot { it.categoryId == null }
    }

    override suspend fun deleteForCategory(categoryId: Long) {
        rows.value = rows.value.filterNot { it.categoryId == categoryId }
    }

    private fun upsert(categoryId: Long?, amount: Money) {
        rows.value = if (rows.value.any { it.categoryId == categoryId }) {
            rows.value.map { if (it.categoryId == categoryId) BudgetTarget(categoryId, amount) else it }
        } else {
            rows.value + BudgetTarget(categoryId, amount)
        }
    }
}
