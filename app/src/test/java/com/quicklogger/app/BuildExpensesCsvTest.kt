package com.quicklogger.app

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.usecase.BuildExpensesCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/** ARCHITECTURE §9.3: UTF-8, header row, RFC-style quoting, major units with two decimals. */
class BuildExpensesCsvTest {
    private val build = BuildExpensesCsv()

    private fun expense(
        id: Long = 1L,
        minor: Long = 4500,
        currency: String = "USD",
        categoryId: Long = 1L,
        receipt: String? = null,
        occurredAt: Instant = Instant.parse("2026-08-17T14:32:00Z"),
    ) = Expense(id, Money(minor, currency), categoryId, occurredAt, receipt, occurredAt, occurredAt)

    @Test
    fun headerRowIsExact() {
        val csv = build(emptyList(), emptyMap(), ZoneOffset.UTC)

        assertEquals("occurred_at,amount,currency,category,has_receipt", csv.lines().first())
    }

    @Test
    fun oneDataRowPerExpense() {
        val csv = build(
            listOf(expense(id = 1L), expense(id = 2L)),
            mapOf(1L to "Supplies"),
            ZoneOffset.UTC,
        )

        // header + 2 data rows + trailing blank line from the final line break
        assertEquals(4, csv.split("\r\n").size)
    }

    @Test
    fun amountIsMajorUnitsWithTwoDecimals() {
        val csv = build(listOf(expense(minor = 4500, currency = "USD")), mapOf(1L to "Food"), ZoneOffset.UTC)

        assertTrue(csv.contains(",45.00,USD,"))
    }

    @Test
    fun zeroFractionDigitCurrencyStillGetsTwoDecimalPlaces() {
        val csv = build(listOf(expense(minor = 4500, currency = "JPY")), mapOf(1L to "Food"), ZoneOffset.UTC)

        // JPY has no minor unit, so 4500 minor IS 4500 major.
        assertTrue(csv.contains(",4500.00,JPY,"))
    }

    @Test
    fun hasReceiptReflectsWhetherAReceiptPathIsSet() {
        val csv = build(
            listOf(expense(id = 1L, receipt = "abc.jpg"), expense(id = 2L, receipt = null)),
            mapOf(1L to "Food"),
            ZoneOffset.UTC,
        )
        val rows = csv.lines().drop(1)

        assertTrue(rows[0].endsWith("true"))
        assertTrue(rows[1].endsWith("false"))
    }

    @Test
    fun occurredAtIsFormattedInTheGivenZoneWithAnOffset() {
        val csv = build(
            listOf(expense(occurredAt = Instant.parse("2026-08-17T17:32:00Z"))),
            mapOf(1L to "Food"),
            ZoneOffset.ofHours(-3),
        )

        assertTrue(csv.contains("2026-08-17T14:32:00-03:00"))
    }

    @Test
    fun aCategoryNameContainingACommaIsQuoted() {
        val csv = build(listOf(expense()), mapOf(1L to "Food, drinks"), ZoneOffset.UTC)

        assertTrue(csv.contains("\"Food, drinks\""))
    }

    @Test
    fun aCategoryNameContainingAQuoteIsEscapedByDoubling() {
        val csv = build(listOf(expense()), mapOf(1L to "Bob\"s"), ZoneOffset.UTC)

        assertTrue(csv.contains("\"Bob\"\"s\""))
    }

    @Test
    fun missingCategoryNameLeavesAnEmptyField() {
        val csv = build(listOf(expense(categoryId = 404L)), emptyMap(), ZoneOffset.UTC)

        val row = csv.lines()[1]
        assertEquals(5, row.split(",").size)
    }
}
