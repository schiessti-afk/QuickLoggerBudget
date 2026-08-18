package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.DateRange
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.usecase.DeleteExpense
import com.quicklogger.app.domain.usecase.DeleteReceipt
import com.quicklogger.app.domain.usecase.ExpenseError
import com.quicklogger.app.domain.usecase.ObserveExpensesInRange
import com.quicklogger.app.domain.usecase.UpdateExpense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExpenseCorrectionUseCasesTest {
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val createdAt = Instant.parse("2026-08-01T10:00:00Z")
    private val editedAt = Instant.parse("2026-08-17T17:32:00Z")

    private fun stored(occurredAt: Instant = createdAt) = Expense(
        id = 1L,
        amount = Money(4500, "BRL"),
        categoryId = food.id,
        occurredAt = occurredAt,
        receiptRelativePath = "abc.jpg",
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    // --- update ---

    @Test
    fun updatesAmountCategoryReceiptAndOccurredAt() = runTest {
        val expenses = FakeExpenseRepository()
        val transport = Category(id = 2L, name = "Transport", sortOrder = 1, isProtected = false)
        val categories = FakeCategoryRepository(listOf(food, transport))
        val newOccurredAt = Instant.parse("2026-08-10T09:00:00Z")
        val edited = stored().copy(
            amount = Money(999, "BRL"),
            categoryId = transport.id,
            occurredAt = newOccurredAt,
            receiptRelativePath = "new.jpg",
        )

        val result = UpdateExpense(expenses, categories, Clock.fixed(editedAt, ZoneOffset.UTC))(edited)

        val saved = result.getOrThrow()
        assertEquals(999L, saved.amount.minor)
        assertEquals(transport.id, saved.categoryId)
        assertEquals(newOccurredAt, saved.occurredAt)
        assertEquals("new.jpg", saved.receiptRelativePath)
    }

    @Test
    fun stampsUpdatedAtButPreservesCreatedAtAndId() = runTest {
        val expenses = FakeExpenseRepository()
        val categories = FakeCategoryRepository(listOf(food))

        val saved = UpdateExpense(expenses, categories, Clock.fixed(editedAt, ZoneOffset.UTC))(stored())
            .getOrThrow()

        assertEquals(1L, saved.id)
        assertEquals(createdAt, saved.createdAt)
        assertEquals(editedAt, saved.updatedAt)
    }

    @Test
    fun rejectsAZeroAmountWithoutWriting() = runTest {
        val expenses = FakeExpenseRepository()
        val categories = FakeCategoryRepository(listOf(food))

        val result = UpdateExpense(expenses, categories, Clock.fixed(editedAt, ZoneOffset.UTC))(
            stored().copy(amount = Money(0, "BRL")),
        )

        assertEquals(ExpenseError.InvalidAmount, result.exceptionOrNull())
    }

    @Test
    fun rejectsAnUnknownCategoryWithoutWriting() = runTest {
        val expenses = FakeExpenseRepository()
        val categories = FakeCategoryRepository(listOf(food))

        val result = UpdateExpense(expenses, categories, Clock.fixed(editedAt, ZoneOffset.UTC))(
            stored().copy(categoryId = 999L),
        )

        assertEquals(ExpenseError.UnknownCategory, result.exceptionOrNull())
    }

    // --- delete ---

    @Test
    fun deletesTheRowAndItsReceiptFile() = runTest {
        val expenses = FakeExpenseRepository()
        val receipts = FakeReceiptStore()
        receipts.writeBytes("abc.jpg")
        val expense = expenses.insert(stored())

        DeleteExpense(expenses, DeleteReceipt(receipts))(expense)

        assertEquals(listOf(expense.id), expenses.deletedIds)
        assertTrue(receipts.deleted.contains("abc.jpg"))
    }

    @Test
    fun deletingAnExpenseWithNoReceiptDoesNotTouchTheReceiptStore() = runTest {
        val expenses = FakeExpenseRepository()
        val receipts = FakeReceiptStore()
        val expense = expenses.insert(stored().copy(receiptRelativePath = null))

        DeleteExpense(expenses, DeleteReceipt(receipts))(expense)

        assertTrue(receipts.deleted.isEmpty())
    }

    // --- observe in range ---

    @Test
    fun onlyReturnsExpensesInsideTheRange() = runTest {
        val expenses = FakeExpenseRepository()
        val inRange = expenses.insert(stored(Instant.parse("2026-08-17T12:00:00Z")))
        expenses.insert(stored(Instant.parse("2026-08-10T12:00:00Z")))

        val range = DateRange(
            start = Instant.parse("2026-08-17T00:00:00Z"),
            endExclusive = Instant.parse("2026-08-18T00:00:00Z"),
        )
        val result = ObserveExpensesInRange(expenses)(range).first()

        assertEquals(listOf(inRange.id), result.map { it.id })
    }

    @Test
    fun theEndBoundIsExclusive() = runTest {
        val expenses = FakeExpenseRepository()
        expenses.insert(stored(Instant.parse("2026-08-18T00:00:00Z")))

        val range = DateRange(
            start = Instant.parse("2026-08-17T00:00:00Z"),
            endExclusive = Instant.parse("2026-08-18T00:00:00Z"),
        )
        val result = ObserveExpensesInRange(expenses)(range).first()

        assertTrue(result.isEmpty())
    }
}
