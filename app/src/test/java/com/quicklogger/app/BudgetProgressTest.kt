package com.quicklogger.app

import com.quicklogger.app.domain.model.BudgetProgress
import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BudgetProgressTest {
    private fun expense(minor: Long, categoryId: Long, currencyCode: String = "USD") = Expense(
        id = 1L,
        amount = Money(minor, currencyCode),
        categoryId = categoryId,
        occurredAt = Instant.EPOCH,
        receiptRelativePath = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    // --- of(): category targets ---

    @Test
    fun aCategoryTargetOnlyCountsThatCategorysExpenses() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(10_000, "USD"))
        val expenses = listOf(expense(3_000, categoryId = 1L), expense(5_000, categoryId = 2L))

        val progress = BudgetProgress.of(listOf(target), expenses).single()

        assertEquals(3_000L, progress.spent.minor)
        assertEquals(7_000L, progress.remaining.minor)
        assertFalse(progress.isOver)
    }

    @Test
    fun theOverallTargetCountsEveryCategory() {
        val target = BudgetTarget(categoryId = null, amount = Money(10_000, "USD"))
        val expenses = listOf(expense(3_000, categoryId = 1L), expense(5_000, categoryId = 2L))

        val progress = BudgetProgress.of(listOf(target), expenses).single()

        assertEquals(8_000L, progress.spent.minor)
        assertEquals(2_000L, progress.remaining.minor)
    }

    @Test
    fun exactlyAtTargetIsNotOver() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(5_000, "USD"))
        val progress = BudgetProgress.of(listOf(target), listOf(expense(5_000, categoryId = 1L))).single()

        assertEquals(0L, progress.remaining.minor)
        assertFalse(progress.isOver)
    }

    @Test
    fun overTheTargetIsNegativeRemainingAndOver() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(5_000, "USD"))
        val progress = BudgetProgress.of(listOf(target), listOf(expense(7_000, categoryId = 1L))).single()

        assertEquals(-2_000L, progress.remaining.minor)
        assertTrue(progress.isOver)
    }

    @Test
    fun aZeroTargetHasARatioOfZeroRatherThanDividingByZero() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(0, "USD"))
        val progress = BudgetProgress.of(listOf(target), emptyList()).single()

        assertEquals(0.0, progress.ratio, 0.0001)
    }

    @Test
    fun anExpenseInAnotherCurrencyIsExcludedNotConverted() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(10_000, "USD"))
        val expenses = listOf(expense(3_000, categoryId = 1L, currencyCode = "BRL"))

        val progress = BudgetProgress.of(listOf(target), expenses).single()

        assertEquals(0L, progress.spent.minor)
        assertEquals(10_000L, progress.remaining.minor)
    }

    @Test
    fun noExpensesLeavesTheFullTargetRemaining() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(10_000, "USD"))

        val progress = BudgetProgress.of(listOf(target), emptyList()).single()

        assertEquals(0L, progress.spent.minor)
        assertEquals(10_000L, progress.remaining.minor)
    }

    // --- remainingIncludingPending(): the Log screen's live line ---

    @Test
    fun pendingAmountReducesRemainingBeforeItIsSaved() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(10_000, "USD"))

        val remaining = BudgetProgress.remainingIncludingPending(
            target,
            expensesThisMonth = listOf(expense(3_000, categoryId = 1L)),
            pendingMinor = 2_000,
            pendingCurrencyCode = "USD",
        )

        assertEquals(5_000L, remaining.minor)
    }

    @Test
    fun aPendingAmountInAnotherCurrencyDoesNotMoveTheTarget() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(10_000, "USD"))

        val remaining = BudgetProgress.remainingIncludingPending(
            target,
            expensesThisMonth = emptyList(),
            pendingMinor = 5_000,
            pendingCurrencyCode = "BRL",
        )

        assertEquals(10_000L, remaining.minor)
    }

    @Test
    fun aPendingAmountCanPushRemainingNegative() {
        val target = BudgetTarget(categoryId = 1L, amount = Money(4_000, "USD"))

        val remaining = BudgetProgress.remainingIncludingPending(
            target,
            expensesThisMonth = emptyList(),
            pendingMinor = 4_500,
            pendingCurrencyCode = "USD",
        )

        assertEquals(-500L, remaining.minor)
    }
}
