package com.quicklogger.app.domain.model

/**
 * Progress against one [BudgetTarget], for a fixed reporting window (the current
 * calendar month — callers pass expenses already scoped with [PeriodBounds] MONTH).
 * Always derived, never stored: there is no persisted "progress" row.
 */
data class Progress(
    val categoryId: Long?,
    val target: Money,
    val spent: Money,
    /** Target minus spent. Negative when over. */
    val remaining: Money,
) {
    val isOver: Boolean get() = remaining.minor < 0

    /** `spent / target`, 0 when the target itself is zero (never divides by zero). */
    val ratio: Double
        get() = if (target.minor == 0L) 0.0 else spent.minor.toDouble() / target.minor.toDouble()
}

/**
 * The one place mixed-currency exclusion is decided for budgets (ARCHITECTURE
 * §6.5), the same way [ExpenseTotals] is the one place currencies are summed.
 * Callers must not compute "spent" or "remaining" themselves.
 */
object BudgetProgress {
    /**
     * [expensesThisMonth] should already be scoped to the current calendar month.
     * An expense counts toward a target only if its `currencyCode` matches the
     * target's, and — for a category target — only if its `categoryId` matches.
     * The overall target (`categoryId == null`) counts every expense in that
     * currency, regardless of category.
     */
    fun of(targets: List<BudgetTarget>, expensesThisMonth: List<Expense>): List<Progress> =
        targets.map { target -> progressFor(target, expensesThisMonth) }

    /**
     * Remaining budget for [target] as if [pendingMinor] (captured in
     * [pendingCurrencyCode]) were added to what's already spent this month. Backs
     * the Log screen's live remaining line (ARCHITECTURE §8.1.8): the digit buffer
     * has not been saved, so it is not in [expensesThisMonth], but it should still
     * count against the target as the user types. A pending amount in a different
     * currency does not move the target, mirroring [of]'s exclusion rule.
     */
    fun remainingIncludingPending(
        target: BudgetTarget,
        expensesThisMonth: List<Expense>,
        pendingMinor: Long,
        pendingCurrencyCode: String,
    ): Money {
        val spent = progressFor(target, expensesThisMonth).spent.minor
        val pending = if (pendingCurrencyCode == target.amount.currencyCode) pendingMinor else 0L
        return Money(target.amount.minor - spent - pending, target.amount.currencyCode)
    }

    private fun progressFor(target: BudgetTarget, expensesThisMonth: List<Expense>): Progress {
        val spentMinor = expensesThisMonth
            .asSequence()
            .filter { it.amount.currencyCode == target.amount.currencyCode }
            .filter { target.categoryId == null || it.categoryId == target.categoryId }
            .sumOf { it.amount.minor }
        val spent = Money(spentMinor, target.amount.currencyCode)
        val remaining = Money(target.amount.minor - spentMinor, target.amount.currencyCode)
        return Progress(target.categoryId, target.amount, spent, remaining)
    }
}
