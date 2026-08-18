package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.ExpenseDateFormatter
import com.quicklogger.app.domain.model.MoneyFormatter
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

/**
 * The WhatsApp-friendly caption for a single expense (ARCHITECTURE §9.1). Plain
 * text, no HTML or Markdown; `*star*` is WhatsApp's own bold markup, not ours.
 *
 * ```
 * *QuickLogger*
 * R$ 45.00 — Supplies
 * 17 Aug 2026, 14:32
 * ```
 */
class FormatExpenseShareText @Inject constructor() {
    operator fun invoke(expense: Expense, categoryName: String, locale: Locale, zone: ZoneId): String {
        val amount = MoneyFormatter.format(expense.amount, locale)
        val date = ExpenseDateFormatter.format(expense.occurredAt, zone, locale)
        return "*QuickLogger*\n$amount — $categoryName\n$date"
    }
}
