package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Currency
import javax.inject.Inject

/**
 * A real spreadsheet file, not a second database (ARCHITECTURE §9.3). UTF-8, one
 * header row, RFC 4180-style quoting. `amount` is major units with two decimal
 * places — this is the one place minor-unit [Money] is converted for a human
 * reader, so a spreadsheet never has to know QuickLogger stores integer cents.
 *
 * ```
 * occurred_at,amount,currency,category,has_receipt
 * 2026-08-17T14:32:00-03:00,45.00,BRL,Supplies,true
 * ```
 */
class BuildExpensesCsv @Inject constructor() {
    operator fun invoke(
        expenses: List<Expense>,
        categoryNames: Map<Long, String>,
        zone: ZoneId,
    ): String {
        val header = HEADER
        val rows = expenses.map { row(it, categoryNames[it.categoryId].orEmpty(), zone) }
        return (listOf(header) + rows).joinToString(separator = "\r\n", postfix = "\r\n")
    }

    private fun row(expense: Expense, categoryName: String, zone: ZoneId): String {
        val occurredAt = expense.occurredAt.atZone(zone)
            .truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return listOf(
            occurredAt,
            majorAmount(expense.amount),
            expense.amount.currencyCode,
            categoryName,
            (expense.receiptRelativePath != null).toString(),
        ).joinToString(",") { csvField(it) }
    }

    /** [Money.minor] converted through [BigDecimal] — a `Double` division would round wrong. */
    private fun majorAmount(money: Money): String {
        val scale = Currency.getInstance(money.currencyCode).defaultFractionDigits.coerceAtLeast(0)
        return BigDecimal.valueOf(money.minor, scale)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private companion object {
        const val HEADER = "occurred_at,amount,currency,category,has_receipt"
    }
}
