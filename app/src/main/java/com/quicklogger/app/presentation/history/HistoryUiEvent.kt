package com.quicklogger.app.presentation.history

/**
 * One-shot History effects (ARCHITECTURE §5 rule 4): the period share text, or the
 * relative file name of a freshly written CSV export, each ready for the system
 * chooser. Neither is left sitting in [HistoryUiState] after it fires.
 */
sealed interface HistoryUiEvent {
    data class ShareText(val text: String) : HistoryUiEvent

    data class ShareCsv(val fileName: String) : HistoryUiEvent
}
