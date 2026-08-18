package com.quicklogger.app.presentation.log

/**
 * One-shot Log-screen effects (ARCHITECTURE §5 rule 4): a camera launch, forwarded
 * from [com.quicklogger.app.presentation.receipt.ReceiptAttachmentController.events],
 * or a completed Save & Share ready for the system chooser. Neither is left sitting
 * in `LogUiState` after it fires.
 */
sealed interface LogUiEvent {
    data class LaunchCamera(val relativePath: String) : LogUiEvent

    /** [receiptRelativePath] is null when the saved expense had no receipt. */
    data class Share(val text: String, val receiptRelativePath: String?) : LogUiEvent
}
