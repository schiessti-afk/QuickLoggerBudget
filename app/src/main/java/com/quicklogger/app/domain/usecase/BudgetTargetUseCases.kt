package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.repository.BudgetTargetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Every standing budget target — overall plus per category (ARCHITECTURE §6.5). */
class ObserveBudgetTargets @Inject constructor(
    private val budgetTargets: BudgetTargetRepository,
) {
    operator fun invoke(): Flow<List<BudgetTarget>> = budgetTargets.observeAll()
}

/**
 * Sets or replaces a target. [categoryId] `null` means the overall monthly target.
 *
 * A non-positive amount is refused defensively: the dashboard's target dialog and
 * the Log screen route an empty/zero entry to [ClearBudgetTarget] instead of calling
 * this (ARCHITECTURE §6.5 — "empty or zero amount deletes the row"), so reaching
 * this validation from the UI should be unreachable, the same way [SaveExpense]'s
 * amount check is.
 */
class SetBudgetTarget @Inject constructor(
    private val budgetTargets: BudgetTargetRepository,
) {
    suspend operator fun invoke(categoryId: Long?, amount: Money): Result<Unit> {
        if (amount.minor <= 0L) return Result.failure(BudgetError.InvalidAmount)
        return runCatching {
            if (categoryId == null) {
                budgetTargets.upsertOverall(amount)
            } else {
                budgetTargets.upsertForCategory(categoryId, amount)
            }
        }
    }
}

/**
 * Removes a target. [categoryId] `null` clears the overall monthly target. Absent
 * target = the feature is invisible: no line on Log, no meter on the dashboard.
 */
class ClearBudgetTarget @Inject constructor(
    private val budgetTargets: BudgetTargetRepository,
) {
    suspend operator fun invoke(categoryId: Long?) {
        if (categoryId == null) {
            budgetTargets.deleteOverall()
        } else {
            budgetTargets.deleteForCategory(categoryId)
        }
    }
}
