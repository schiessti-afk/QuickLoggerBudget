package com.quicklogger.app

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.ExpenseDateFormatter
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.usecase.FormatExpenseShareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** ARCHITECTURE §9.1: exact three-line shape, `*star*` markup, no HTML/Markdown. */
class FormatExpenseShareTextTest {
    private val formatter = FormatExpenseShareText()

    private val expense = Expense(
        id = 1L,
        amount = Money(minor = 4500, currencyCode = "USD"),
        categoryId = 1L,
        occurredAt = Instant.parse("2026-08-17T14:32:00Z"),
        receiptRelativePath = null,
        createdAt = Instant.parse("2026-08-17T14:32:00Z"),
        updatedAt = Instant.parse("2026-08-17T14:32:00Z"),
    )

    @Test
    fun buildsTheThreeLineCaption() {
        val text = formatter(expense, "Supplies", Locale.US, ZoneOffset.UTC)

        val amount = MoneyFormatter.format(expense.amount, Locale.US)
        val date = ExpenseDateFormatter.format(expense.occurredAt, ZoneOffset.UTC, Locale.US)
        assertEquals("*QuickLogger*\n$amount — Supplies\n$date", text)
    }

    @Test
    fun titleUsesStarMarkupNotHtmlOrMarkdown() {
        val text = formatter(expense, "Supplies", Locale.US, ZoneOffset.UTC)

        val title = text.lines().first()
        assertEquals("*QuickLogger*", title)
    }

    @Test
    fun rendersTheAmountInTheExpensesStoredCurrencyRegardlessOfLocale() {
        val brlExpense = expense.copy(amount = Money(minor = 4500, currencyCode = "BRL"))

        val text = formatter(brlExpense, "Supplies", Locale.US, ZoneOffset.UTC)

        assertTrue(text.contains("R$"))
    }
}
