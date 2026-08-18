package com.quicklogger.app

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.domain.usecase.BuildPeriodSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** ARCHITECTURE §9.2: title line + one line per expense + one total per currency. */
class BuildPeriodSummaryTest {
    private val build = BuildPeriodSummary()

    private fun expense(id: Long, minor: Long, currency: String, categoryId: Long) = Expense(
        id = id,
        amount = Money(minor, currency),
        categoryId = categoryId,
        occurredAt = Instant.parse("2026-08-17T14:32:00Z"),
        receiptRelativePath = null,
        createdAt = Instant.parse("2026-08-17T14:32:00Z"),
        updatedAt = Instant.parse("2026-08-17T14:32:00Z"),
    )

    @Test
    fun titleNamesThePeriod() {
        val text = build(Period.WEEK, emptyList(), emptyMap(), Locale.US, ZoneOffset.UTC)

        assertEquals("*QuickLogger — Week*", text.lines().first())
    }

    @Test
    fun oneLinePerExpenseCarriesAmountCategoryAndDate() {
        val supplies = expense(1L, 4500, "USD", categoryId = 1L)
        val categories = mapOf(1L to "Supplies")

        val text = build(Period.DAY, listOf(supplies), categories, Locale.US, ZoneOffset.UTC)
        val lines = text.lines()

        assertEquals(3, lines.size) // title, one expense line, one total line
        assertTrue(lines[1].contains("Supplies"))
        assertTrue(lines[1].contains(MoneyFormatter.format(supplies.amount, Locale.US)))
    }

    @Test
    fun expensesKeepTheOrderTheyAreGivenIn() {
        val first = expense(1L, 100, "USD", categoryId = 1L)
        val second = expense(2L, 200, "USD", categoryId = 1L)
        val categories = mapOf(1L to "Food")

        val text = build(Period.DAY, listOf(second, first), categories, Locale.US, ZoneOffset.UTC)
        val expenseLines = text.lines().drop(1).dropLast(1)

        assertTrue(expenseLines[0].contains("$2.00"))
        assertTrue(expenseLines[1].contains("$1.00"))
    }

    @Test
    fun oneTotalLinePerCurrencyNeverCombinesCodes() {
        val usd = expense(1L, 4500, "USD", categoryId = 1L)
        val brl = expense(2L, 1000, "BRL", categoryId = 1L)
        val categories = mapOf(1L to "Food")

        val text = build(Period.DAY, listOf(usd, brl), categories, Locale.US, ZoneOffset.UTC)
        val totalLines = text.lines().takeLast(2)

        assertEquals(2, totalLines.count { it.startsWith("Total: ") })
        assertTrue(totalLines.any { it.contains("$45.00") })
        assertTrue(totalLines.any { it.contains("R$") })
    }

    @Test
    fun emptyPeriodIsJustTheTitle() {
        val text = build(Period.DAY, emptyList(), emptyMap(), Locale.US, ZoneOffset.UTC)

        assertEquals(listOf("*QuickLogger — Day*"), text.lines())
    }
}
