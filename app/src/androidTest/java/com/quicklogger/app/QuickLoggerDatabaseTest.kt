package com.quicklogger.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.data.local.ExpenseEntity
import com.quicklogger.app.data.local.QuickLoggerDatabase
import com.quicklogger.app.data.local.SeedCategoriesCallback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickLoggerDatabaseTest {
    private lateinit var database: QuickLoggerDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QuickLoggerDatabase::class.java,
        ).addCallback(SeedCategoriesCallback).build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun seedsTheSixDefaultCategoriesInOrder() = runTest {
        val categories = database.categoryDao().observeAll().first()

        assertEquals(
            listOf("Food", "Transport", "Supplies", "Utilities", "Personal", "Other"),
            categories.map { it.name },
        )
        assertEquals(listOf(0, 1, 2, 3, 4, 5), categories.map { it.sortOrder })
    }

    @Test
    fun onlyOtherIsProtected() = runTest {
        val categories = database.categoryDao().observeAll().first()

        assertTrue(categories.single { it.name == "Other" }.isProtected)
        assertFalse(categories.filter { it.name != "Other" }.any { it.isProtected })
    }

    @Test
    fun insertingAnExpenseRoundTripsThroughTheNewestFirstStream() = runTest {
        val food = database.categoryDao().observeAll().first().first { it.name == "Food" }

        val id = database.expenseDao().insert(
            ExpenseEntity(
                amountMinor = 4500,
                currencyCode = "BRL",
                categoryId = food.id,
                occurredAtEpochMs = 1_755_452_000_000,
                receiptRelativePath = null,
                createdAtEpochMs = 1_755_452_000_000,
                updatedAtEpochMs = 1_755_452_000_000,
            ),
        )

        val stored = database.expenseDao().observeAllNewestFirst().first().single()
        assertEquals(id, stored.id)
        assertEquals(4500L, stored.amountMinor)
        assertEquals("BRL", stored.currencyCode)
        assertEquals(food.id, stored.categoryId)
        assertEquals(null, stored.receiptRelativePath)
    }

    @Test
    fun expensesComeBackNewestFirst() = runTest {
        val food = database.categoryDao().observeAll().first().first { it.name == "Food" }
        fun expense(occurredAt: Long) = ExpenseEntity(
            amountMinor = occurredAt / 1_000_000,
            currencyCode = "USD",
            categoryId = food.id,
            occurredAtEpochMs = occurredAt,
            receiptRelativePath = null,
            createdAtEpochMs = occurredAt,
            updatedAtEpochMs = occurredAt,
        )

        database.expenseDao().insert(expense(1_000_000_000_000))
        database.expenseDao().insert(expense(3_000_000_000_000))
        database.expenseDao().insert(expense(2_000_000_000_000))

        val stored = database.expenseDao().observeAllNewestFirst().first()
        assertEquals(
            listOf(3_000_000_000_000, 2_000_000_000_000, 1_000_000_000_000),
            stored.map { it.occurredAtEpochMs },
        )
    }

    @Test
    fun anExpenseCannotReferenceAMissingCategory() = runTest {
        val insertOrphan = runCatching {
            database.expenseDao().insert(
                ExpenseEntity(
                    amountMinor = 100,
                    currencyCode = "USD",
                    categoryId = 9_999L,
                    occurredAtEpochMs = 1_755_452_000_000,
                    receiptRelativePath = null,
                    createdAtEpochMs = 1_755_452_000_000,
                    updatedAtEpochMs = 1_755_452_000_000,
                ),
            )
        }

        assertTrue("foreign key should be enforced", insertOrphan.isFailure)
    }
}
