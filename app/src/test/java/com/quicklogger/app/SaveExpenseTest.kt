package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.NewExpense
import com.quicklogger.app.domain.usecase.ExpenseError
import com.quicklogger.app.domain.usecase.SaveExpense
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SaveExpenseTest {
    private val savedAt = Instant.parse("2026-08-17T17:32:00Z")
    private val clock = Clock.fixed(savedAt, ZoneOffset.UTC)
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)

    private fun saveExpense(
        expenses: FakeExpenseRepository = FakeExpenseRepository(),
        categories: List<Category> = listOf(food),
    ) = SaveExpense(expenses, FakeCategoryRepository(categories), clock)

    @Test
    fun persistsAPositiveAmount() = runTest {
        val expenses = FakeExpenseRepository()

        val result = saveExpense(expenses)(
            NewExpense(amount = Money(4500, "BRL"), categoryId = food.id),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, expenses.inserted.size)
        assertEquals(4500L, expenses.inserted.single().amount.minor)
        assertEquals(food.id, expenses.inserted.single().categoryId)
    }

    @Test
    fun storesTheCurrencyCodeItWasGiven() = runTest {
        val expenses = FakeExpenseRepository()

        saveExpense(expenses)(NewExpense(amount = Money(4500, "JPY"), categoryId = food.id))

        assertEquals("JPY", expenses.inserted.single().amount.currencyCode)
    }

    @Test
    fun stampsTimestampsFromTheInjectedClock() = runTest {
        val expenses = FakeExpenseRepository()

        saveExpense(expenses)(NewExpense(amount = Money(100, "USD"), categoryId = food.id))

        val saved = expenses.inserted.single()
        assertEquals(savedAt, saved.occurredAt)
        assertEquals(savedAt, saved.createdAt)
        assertEquals(savedAt, saved.updatedAt)
    }

    @Test
    fun rejectsZeroAmountWithoutWriting() = runTest {
        val expenses = FakeExpenseRepository()

        val result = saveExpense(expenses)(
            NewExpense(amount = Money(0, "USD"), categoryId = food.id),
        )

        assertEquals(ExpenseError.InvalidAmount, result.exceptionOrNull())
        assertTrue(expenses.inserted.isEmpty())
    }

    @Test
    fun rejectsNegativeAmountWithoutWriting() = runTest {
        val expenses = FakeExpenseRepository()

        val result = saveExpense(expenses)(
            NewExpense(amount = Money(-1, "USD"), categoryId = food.id),
        )

        assertEquals(ExpenseError.InvalidAmount, result.exceptionOrNull())
        assertTrue(expenses.inserted.isEmpty())
    }

    @Test
    fun rejectsACategoryThatDoesNotExistWithoutWriting() = runTest {
        val expenses = FakeExpenseRepository()

        val result = saveExpense(expenses)(
            NewExpense(amount = Money(4500, "USD"), categoryId = 99L),
        )

        assertEquals(ExpenseError.UnknownCategory, result.exceptionOrNull())
        assertTrue(expenses.inserted.isEmpty())
    }

    @Test
    fun returnsThePersistedExpenseWithItsAssignedId() = runTest {
        val result = saveExpense()(NewExpense(amount = Money(4500, "USD"), categoryId = food.id))

        assertEquals(1L, result.getOrThrow().id)
    }

    @Test
    fun savesWithNoReceiptOnTheSprintTwoPath() = runTest {
        val expenses = FakeExpenseRepository()

        saveExpense(expenses)(NewExpense(amount = Money(4500, "USD"), categoryId = food.id))

        assertEquals(null, expenses.inserted.single().receiptRelativePath)
    }
}
