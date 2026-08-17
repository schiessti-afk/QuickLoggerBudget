package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.NewExpense
import com.quicklogger.app.domain.repository.CategoryRepository
import com.quicklogger.app.domain.repository.ExpenseRepository
import java.time.Clock
import javax.inject.Inject

/** Why a save was refused. Validation is domain work, not Compose work. */
sealed class SaveExpenseError(message: String) : Exception(message) {
    data object InvalidAmount : SaveExpenseError("Amount must be greater than zero")
    data object UnknownCategory : SaveExpenseError("Selected category no longer exists")
}

/**
 * Writes one expense after validating it.
 *
 * The caller supplies the currency code; this use case never reads the device
 * locale (ARCHITECTURE §6.1). `occurredAt` is the save-time instant because the
 * primary path has no date picker.
 */
class SaveExpense @Inject constructor(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(input: NewExpense): Result<Expense> {
        if (input.amount.minor <= 0L) return Result.failure(SaveExpenseError.InvalidAmount)
        if (categories.getById(input.categoryId) == null) {
            return Result.failure(SaveExpenseError.UnknownCategory)
        }

        val now = clock.instant()
        return runCatching {
            expenses.insert(
                Expense(
                    id = 0L,
                    amount = input.amount,
                    categoryId = input.categoryId,
                    occurredAt = now,
                    receiptRelativePath = input.receiptRelativePath,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
}
