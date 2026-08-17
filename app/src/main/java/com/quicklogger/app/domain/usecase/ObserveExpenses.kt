package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Newest-first expenses. Sprint 2 proves the stream; History renders it in sprint 4.
 */
class ObserveExpenses @Inject constructor(
    private val expenses: ExpenseRepository,
) {
    operator fun invoke(): Flow<List<Expense>> = expenses.observeAllNewestFirst()
}
