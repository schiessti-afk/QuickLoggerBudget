package com.quicklogger.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.data.local.BudgetTargetEntity
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

    // --- sprint 4: range, update, delete ---

    private suspend fun foodCategory() = database.categoryDao().observeAll().first().first { it.name == "Food" }

    private fun expenseAt(categoryId: Long, occurredAt: Long) = ExpenseEntity(
        amountMinor = 100,
        currencyCode = "USD",
        categoryId = categoryId,
        occurredAtEpochMs = occurredAt,
        receiptRelativePath = null,
        createdAtEpochMs = occurredAt,
        updatedAtEpochMs = occurredAt,
    )

    @Test
    fun observeInRangeExcludesTheUpperBound() = runTest {
        val food = foodCategory()
        database.expenseDao().insert(expenseAt(food.id, 1_000L))
        database.expenseDao().insert(expenseAt(food.id, 2_000L))

        val inRange = database.expenseDao().observeInRange(1_000L, 2_000L).first()

        assertEquals(listOf(1_000L), inRange.map { it.occurredAtEpochMs })
    }

    @Test
    fun updateOverwritesTheStoredRow() = runTest {
        val food = foodCategory()
        val id = database.expenseDao().insert(expenseAt(food.id, 1_000L))
        val original = database.expenseDao().getById(id)!!

        database.expenseDao().update(original.copy(amountMinor = 999, currencyCode = "BRL"))

        val updated = database.expenseDao().getById(id)!!
        assertEquals(999L, updated.amountMinor)
        assertEquals("BRL", updated.currencyCode)
    }

    @Test
    fun deleteRemovesTheRow() = runTest {
        val food = foodCategory()
        val id = database.expenseDao().insert(expenseAt(food.id, 1_000L))

        database.expenseDao().delete(id)

        assertEquals(null, database.expenseDao().getById(id))
    }

    @Test
    fun maxSortOrderIsMinusOneWhenEmpty() = runTest {
        val empty = Room.inMemoryDatabaseBuilder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            QuickLoggerDatabase::class.java,
        ).build()

        assertEquals(-1, empty.categoryDao().maxSortOrder())
        empty.close()
    }

    @Test
    fun maxSortOrderReflectsTheSeededRows() = runTest {
        assertEquals(5, database.categoryDao().maxSortOrder())
    }

    @Test
    fun renameChangesOnlyTheTargetedRow() = runTest {
        val food = foodCategory()

        database.categoryDao().rename(food.id, "Groceries")

        assertEquals("Groceries", database.categoryDao().getById(food.id)!!.name)
    }

    @Test
    fun deleteUnprotectedRefusesToDeleteTheProtectedRow() = runTest {
        val other = database.categoryDao().observeAll().first().first { it.name == "Other" }

        database.categoryDao().deleteUnprotected(other.id)

        assertTrue(
            "Other must survive an attempted delete",
            database.categoryDao().observeAll().first().any { it.id == other.id },
        )
    }

    @Test
    fun reassignCategoryMovesEveryMatchingExpense() = runTest {
        val food = foodCategory()
        val other = database.categoryDao().observeAll().first().first { it.name == "Other" }
        database.expenseDao().insert(expenseAt(food.id, 1_000L))
        database.expenseDao().insert(expenseAt(food.id, 2_000L))

        database.expenseDao().reassignCategory(food.id, other.id, 5_000L)

        val all = database.expenseDao().observeAllNewestFirst().first()
        assertTrue(all.all { it.categoryId == other.id })
        assertTrue(all.all { it.updatedAtEpochMs == 5_000L })
    }

    @Test
    fun deletingACategoryAfterReassignmentDoesNotViolateTheForeignKey() = runTest {
        val food = foodCategory()
        val other = database.categoryDao().observeAll().first().first { it.name == "Other" }
        database.expenseDao().insert(expenseAt(food.id, 1_000L))

        // Mirrors the transaction RoomCategoryRepository.delete runs: reassign, then delete.
        database.expenseDao().reassignCategory(food.id, other.id, 5_000L)
        database.categoryDao().deleteUnprotected(food.id)

        assertEquals(null, database.categoryDao().getById(food.id))
        assertTrue(database.expenseDao().observeAllNewestFirst().first().all { it.categoryId == other.id })
    }

    // --- sprint 7: budget targets (ARCHITECTURE §6.5, §7.1) ---

    @Test
    fun insertingAnOverallTargetRoundTrips() = runTest {
        val id = database.budgetTargetDao().insert(
            BudgetTargetEntity(categoryId = null, amountMinor = 50_000, currencyCode = "USD"),
        )

        val stored = database.budgetTargetDao().getOverall()!!
        assertEquals(id, stored.id)
        assertEquals(50_000L, stored.amountMinor)
        assertEquals(null, stored.categoryId)
    }

    @Test
    fun insertingACategoryTargetRoundTrips() = runTest {
        val food = foodCategory()
        database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = food.id, amountMinor = 5_000, currencyCode = "USD"))

        val stored = database.budgetTargetDao().getForCategory(food.id)!!
        assertEquals(5_000L, stored.amountMinor)
    }

    @Test
    fun aSecondTargetForTheSameCategoryViolatesTheUniqueIndex() = runTest {
        val food = foodCategory()
        database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = food.id, amountMinor = 5_000, currencyCode = "USD"))

        val secondInsert = runCatching {
            database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = food.id, amountMinor = 9_000, currencyCode = "USD"))
        }

        assertTrue("the unique index on categoryId should reject a second row", secondInsert.isFailure)
    }

    @Test
    fun deletingACategoryCascadesToItsTarget() = runTest {
        val food = foodCategory()
        database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = food.id, amountMinor = 5_000, currencyCode = "USD"))

        // Mirrors RoomCategoryRepository.delete: reassign expenses, then delete the row.
        // The target has no expenses to reassign — it is simply gone with the category.
        database.categoryDao().deleteUnprotected(food.id)

        assertEquals(null, database.budgetTargetDao().getForCategory(food.id))
        assertTrue(database.budgetTargetDao().observeAll().first().none { it.categoryId == food.id })
    }

    @Test
    fun deletingTheOverallTargetLeavesCategoryTargetsAlone() = runTest {
        val food = foodCategory()
        database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = null, amountMinor = 50_000, currencyCode = "USD"))
        database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = food.id, amountMinor = 5_000, currencyCode = "USD"))

        database.budgetTargetDao().deleteOverall()

        assertEquals(null, database.budgetTargetDao().getOverall())
        assertTrue(database.budgetTargetDao().getForCategory(food.id) != null)
    }

    @Test
    fun deletingACategoryTargetLeavesTheOverallTargetAlone() = runTest {
        val food = foodCategory()
        database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = null, amountMinor = 50_000, currencyCode = "USD"))
        database.budgetTargetDao().insert(BudgetTargetEntity(categoryId = food.id, amountMinor = 5_000, currencyCode = "USD"))

        database.budgetTargetDao().deleteForCategory(food.id)

        assertTrue(database.budgetTargetDao().getOverall() != null)
        assertEquals(null, database.budgetTargetDao().getForCategory(food.id))
    }
}
