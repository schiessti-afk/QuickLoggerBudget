package com.quicklogger.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.data.local.ExpenseEntity
import com.quicklogger.app.data.local.QuickLoggerDatabase
import com.quicklogger.app.data.local.SeedCategoriesCallback
import com.quicklogger.app.data.repository.RoomCategoryRepository
import com.quicklogger.app.domain.usecase.CategoryError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Exercises the pieces `QuickLoggerDatabaseTest` deliberately runs by hand
 * (reassign-then-delete) through the real repository, so the constraint-mapping and
 * the transaction boundary are proven, not just each statement individually.
 */
@RunWith(AndroidJUnit4::class)
class RoomCategoryRepositoryTest {
    private lateinit var database: QuickLoggerDatabase
    private lateinit var repository: RoomCategoryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QuickLoggerDatabase::class.java,
        ).addCallback(SeedCategoriesCallback).build()
        repository = RoomCategoryRepository(
            database,
            database.categoryDao(),
            database.expenseDao(),
            Clock.fixed(Instant.parse("2026-08-17T17:32:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun insertAppendsAfterTheSeededRows() = runTest {
        val created = repository.insert("Groceries")

        assertEquals(6, created.sortOrder)
    }

    @Test
    fun insertingADuplicateNameThrowsADomainError() = runTest {
        val thrown = runCatching { repository.insert("food") }.exceptionOrNull()

        assertEquals(CategoryError.DuplicateName, thrown)
    }

    @Test
    fun renamingToADuplicateNameThrowsADomainError() = runTest {
        val food = database.categoryDao().observeAll().first().first { it.name == "Food" }

        val thrown = runCatching { repository.rename(food.id, "other") }.exceptionOrNull()

        assertEquals(CategoryError.DuplicateName, thrown)
    }

    @Test
    fun deleteReassignsAndRemovesAtomicallyThroughTheRepository() = runTest {
        val food = database.categoryDao().observeAll().first().first { it.name == "Food" }
        val other = database.categoryDao().observeAll().first().first { it.name == "Other" }
        database.expenseDao().insert(
            ExpenseEntity(
                amountMinor = 100,
                currencyCode = "USD",
                categoryId = food.id,
                occurredAtEpochMs = 1_000L,
                receiptRelativePath = null,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
        )

        repository.delete(food.id, other.id)

        assertEquals(null, database.categoryDao().getById(food.id))
        val expenses = database.expenseDao().observeAllNewestFirst().first()
        assertTrue(expenses.all { it.categoryId == other.id })
    }
}
