package com.quicklogger.app.domain.usecase

/** Why a budget target could not be set. */
sealed class BudgetError(message: String) : Exception(message) {
    data object InvalidAmount : BudgetError("Amount must be greater than zero")
}
