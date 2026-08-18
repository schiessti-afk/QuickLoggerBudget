package com.quicklogger.app.domain.usecase

/** Why a save or update was refused. Shared by [SaveExpense] and [UpdateExpense]. */
sealed class ExpenseError(message: String) : Exception(message) {
    data object InvalidAmount : ExpenseError("Amount must be greater than zero")
    data object UnknownCategory : ExpenseError("Selected category no longer exists")
}
