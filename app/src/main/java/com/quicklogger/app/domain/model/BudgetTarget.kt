package com.quicklogger.app.domain.model

/**
 * An optional ceiling for the current calendar month (ARCHITECTURE §6.5). A standing
 * value, not a per-month record: editing it changes the current month and every
 * month after it.
 *
 * [categoryId] is `null` for the one overall monthly target; otherwise it names the
 * one target for that category. [amount]'s `currencyCode` is fixed at the moment the
 * target is set, the same rule as [Expense] — it is never rewritten afterwards.
 */
data class BudgetTarget(
    val categoryId: Long?,
    val amount: Money,
)
