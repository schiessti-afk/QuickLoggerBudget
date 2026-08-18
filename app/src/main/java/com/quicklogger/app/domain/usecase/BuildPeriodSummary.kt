package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.ExpenseDateFormatter
import com.quicklogger.app.domain.model.ExpenseTotals
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.model.Period
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

/**
 * Dashboard's period share (ARCHITECTURE §9.2): a title line, one line per expense,
 * then one total line per currency ([ExpenseTotals] is still the only place a total
 * is computed). [expenses] is used in the order it is given — Dashboard already hands
 * this newest-first.
 */
class BuildPeriodSummary @Inject constructor() {
    operator fun invoke(
        period: Period,
        expenses: List<Expense>,
        categoryNames: Map<Long, String>,
        locale: Locale,
        zone: ZoneId,
    ): String {
        val title = "*QuickLogger — ${period.label()}*"
        val lines = expenses.map { expense ->
            val amount = MoneyFormatter.format(expense.amount, locale)
            val category = categoryNames[expense.categoryId].orEmpty()
            val date = ExpenseDateFormatter.format(expense.occurredAt, zone, locale)
            "$amount — $category — $date"
        }
        val totals = ExpenseTotals.byCurrency(expenses).map { "Total: ${MoneyFormatter.format(it, locale)}" }
        return (listOf(title) + lines + totals).joinToString("\n")
    }

    private fun Period.label(): String = when (this) {
        Period.DAY -> "Day"
        Period.WEEK -> "Week"
        Period.MONTH -> "Month"
    }
}
