package com.quicklogger.app.presentation.history

import com.quicklogger.app.domain.model.Period

data class HistoryRowUiModel(
    val id: Long,
    val amountFormatted: String,
    val categoryName: String,
    val occurredAtFormatted: String,
    val hasReceipt: Boolean,
)

data class HistoryUiState(
    val period: Period = Period.DAY,
    val rows: List<HistoryRowUiModel> = emptyList(),
    val totalsFormatted: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}
