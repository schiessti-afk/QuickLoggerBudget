package com.quicklogger.app.presentation.expenseedit

import java.time.Instant

/**
 * Delete confirmation is a local Compose `AlertDialog`; only the destructive action
 * itself, once confirmed, becomes an event.
 */
sealed interface ExpenseEditEvent {
    data class AmountChanged(val raw: String) : ExpenseEditEvent

    data class CategorySelected(val id: Long) : ExpenseEditEvent

    data class OccurredAtChanged(val instant: Instant) : ExpenseEditEvent

    data object Save : ExpenseEditEvent

    data object Delete : ExpenseEditEvent

    data object CaptureReceipt : ExpenseEditEvent

    data class ReceiptCaptured(val success: Boolean) : ExpenseEditEvent

    data class ReceiptPicked(val sourceUri: String) : ExpenseEditEvent

    data object RemoveReceipt : ExpenseEditEvent
}

/** One-shot effects (ARCHITECTURE §5 rule 4). */
sealed interface ExpenseEditUiEvent {
    data class LaunchCamera(val relativePath: String) : ExpenseEditUiEvent

    data object NavigateBack : ExpenseEditUiEvent
}
