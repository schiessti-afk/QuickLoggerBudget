package com.quicklogger.app.presentation.history

import com.quicklogger.app.domain.model.Period

/**
 * Row taps navigate directly (`onEditExpense(id)`, a composable param like
 * `onNavigateUp`); that is not ViewModel-routed state, so there is no event for it.
 */
sealed interface HistoryEvent {
    data class PeriodSelected(val period: Period) : HistoryEvent
}
