package com.quicklogger.app

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.ExpenseTotals
import com.quicklogger.app.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ExpenseTotalsTest {
    private fun expense(minor: Long, currencyCode: String, id: Long = 1L) = Expense(
        id = id,
        amount = Money(minor, currencyCode),
        categoryId = 1L,
        occurredAt = Instant.EPOCH,
        receiptRelativePath = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun emptyListHasNoTotals() {
        assertEquals(emptyList<Money>(), ExpenseTotals.byCurrency(emptyList()))
    }

    @Test
    fun oneCurrencyProducesOneTotal() {
        val totals = ExpenseTotals.byCurrency(
            listOf(expense(4500, "USD"), expense(199, "USD")),
        )

        assertEquals(listOf(Money(4699, "USD")), totals)
    }

    @Test
    fun mixedCurrenciesProduceOneLinePerCurrency() {
        val totals = ExpenseTotals.byCurrency(
            listOf(expense(4500, "BRL"), expense(1000, "JPY"), expense(500, "BRL")),
        )

        assertEquals(listOf(Money(5000, "BRL"), Money(1000, "JPY")), totals)
    }

    @Test
    fun neverCombinesTwoCurrencyCodesIntoOneNumber() {
        val totals = ExpenseTotals.byCurrency(listOf(expense(100, "USD"), expense(100, "EUR")))

        assertEquals(2, totals.size)
        assertEquals(100L, totals.single { it.currencyCode == "USD" }.minor)
        assertEquals(100L, totals.single { it.currencyCode == "EUR" }.minor)
    }
}
