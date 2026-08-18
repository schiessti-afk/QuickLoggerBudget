package com.quicklogger.app.presentation.dashboard

import com.quicklogger.app.domain.model.Period

/**
 * Row taps navigate directly (`onEditExpense(id)`, a composable param like
 * `onNavigateUp`); that is not ViewModel-routed state, so there is no event for it.
 */
sealed interface DashboardEvent {
    data class PeriodSelected(val period: Period) : DashboardEvent

    /** Shares the currently filtered period as text (ARCHITECTURE §9.2). */
    data object SharePeriodText : DashboardEvent

    /** Writes the currently filtered period to CSV and shares the file (ARCHITECTURE §9.3). */
    data object ExportCsv : DashboardEvent

    /** Tapping the overall meter or a category bar opens the target dialog (DESIGN §4.2). */
    data class EditBudgetTarget(val categoryId: Long?) : DashboardEvent

    /** Mirrors [com.quicklogger.app.presentation.log.LogEvent.AmountChanged]: same digit-buffer field. */
    data class BudgetTargetAmountChanged(val raw: String) : DashboardEvent

    /** An empty or zero amount clears the target instead of setting it (ARCHITECTURE §6.5). */
    data object ConfirmBudgetTarget : DashboardEvent

    data object DismissBudgetTargetDialog : DashboardEvent
}
