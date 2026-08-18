package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Deletes an expense row, then best-effort deletes its receipt file
 * (ARCHITECTURE §7.2 item 4). [DeleteReceipt] already swallows a missing file, so a
 * receipt that is somehow already gone does not fail this delete.
 */
class DeleteExpense @Inject constructor(
    private val expenses: ExpenseRepository,
    private val deleteReceipt: DeleteReceipt,
) {
    suspend operator fun invoke(expense: Expense) {
        expenses.delete(expense.id)
        expense.receiptRelativePath?.let { deleteReceipt(it) }
    }
}
