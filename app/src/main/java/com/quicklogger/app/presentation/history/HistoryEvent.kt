package com.quicklogger.app.presentation.history

import com.quicklogger.app.domain.model.Period

/**
 * Row taps navigate directly (`onEditExpense(id)`, a composable param like
 * `onNavigateUp`); that is not ViewModel-routed state, so there is no event for it.
 */
sealed interface HistoryEvent {
    data class PeriodSelected(val period: Period) : HistoryEvent

    /** Shares the currently filtered period as text (ARCHITECTURE §9.2). */
    data object SharePeriodText : HistoryEvent

    /** Writes the currently filtered period to CSV and shares the file (ARCHITECTURE §9.3). */
    data object ExportCsv : HistoryEvent
}
