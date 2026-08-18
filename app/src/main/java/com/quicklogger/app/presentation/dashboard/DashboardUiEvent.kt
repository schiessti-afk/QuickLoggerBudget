package com.quicklogger.app.presentation.dashboard

/**
 * One-shot Dashboard effects (ARCHITECTURE §5 rule 4): the period share text, or the
 * relative file name of a freshly written CSV export, each ready for the system
 * chooser. Neither is left sitting in [DashboardUiState] after it fires.
 */
sealed interface DashboardUiEvent {
    data class ShareText(val text: String) : DashboardUiEvent

    data class ShareCsv(val fileName: String) : DashboardUiEvent
}
