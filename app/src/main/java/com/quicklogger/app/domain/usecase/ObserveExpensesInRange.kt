package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.DateRange
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The expenses History shows for the selected period. */
class ObserveExpensesInRange @Inject constructor(
    private val expenses: ExpenseRepository,
) {
    operator fun invoke(range: DateRange): Flow<List<Expense>> =
        expenses.observeInRange(range.start, range.endExclusive)
}
