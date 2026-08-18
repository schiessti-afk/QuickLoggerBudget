package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.repository.CategoryRepository
import com.quicklogger.app.domain.repository.ExpenseRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Persists edits to an existing expense: amount, category, receipt, `occurredAt`
 * (ARCHITECTURE §6.2 — edit is the one path allowed to change `occurredAt`). Same
 * validation as [SaveExpense]; `id` and `createdAt` come from the expense being
 * edited, `updatedAt` is stamped fresh.
 */
class UpdateExpense @Inject constructor(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(expense: Expense): Result<Expense> {
        if (expense.amount.minor <= 0L) return Result.failure(ExpenseError.InvalidAmount)
        if (categories.getById(expense.categoryId) == null) {
            return Result.failure(ExpenseError.UnknownCategory)
        }

        val updated = expense.copy(updatedAt = clock.instant())
        return runCatching { expenses.update(updated); updated }
    }
}
