package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.NewExpense
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ObserveExpenses
import com.quicklogger.app.domain.usecase.SaveExpense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ObserveUseCasesTest {
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val other = Category(id = 6L, name = "Other", sortOrder = 5, isProtected = true)

    @Test
    fun observeCategoriesEmitsWhatTheRepositoryHolds() = runTest {
        val categories = ObserveCategories(FakeCategoryRepository(listOf(food, other)))

        assertEquals(listOf(food, other), categories().first())
    }

    @Test
    fun observeCategoriesReemitsAfterAChange() = runTest {
        val repository = FakeCategoryRepository(listOf(food))
        val categories = ObserveCategories(repository)

        repository.emit(listOf(food, other))

        assertEquals(listOf(food, other), categories().first())
    }

    @Test
    fun observeExpensesStartsEmpty() = runTest {
        assertEquals(emptyList<Any>(), ObserveExpenses(FakeExpenseRepository())().first())
    }

    @Test
    fun observeExpensesSeesASavedExpense() = runTest {
        val expenses = FakeExpenseRepository()
        val save = SaveExpense(
            expenses,
            FakeCategoryRepository(listOf(food)),
            Clock.fixed(Instant.parse("2026-08-17T17:32:00Z"), ZoneOffset.UTC),
        )

        save(NewExpense(amount = Money(4500, "BRL"), categoryId = food.id))

        val observed = ObserveExpenses(expenses)().first()
        assertEquals(1, observed.size)
        assertEquals(Money(4500, "BRL"), observed.single().amount)
    }
}
