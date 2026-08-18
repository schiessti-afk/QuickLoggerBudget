package com.quicklogger.app.domain.model

/**
 * Sums a list of expenses per currency code. This is the only place a "total" is
 * computed — ARCHITECTURE §6.1 forbids adding two currency codes into one number,
 * so callers must never sum [Money] values themselves.
 */
object ExpenseTotals {
    fun byCurrency(expenses: List<Expense>): List<Money> =
        expenses.groupBy { it.amount.currencyCode }
            .map { (code, group) -> Money(group.sumOf { it.amount.minor }, code) }
            .sortedBy { it.currencyCode }
}
