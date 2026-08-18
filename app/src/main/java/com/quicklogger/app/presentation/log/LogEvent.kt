package com.quicklogger.app.presentation.log

/**
 * Save & Share arrives in sprint 5; it is not stubbed here. Camera-launch is a
 * one-shot effect owned by `ReceiptAttachmentController.events`, not a `LogEvent`.
 */
sealed interface LogEvent {
    data class AmountChanged(val raw: String) : LogEvent

    data class CategorySelected(val id: Long) : LogEvent

    data object Save : LogEvent

    /** User tapped the camera action. The draft file is created before launching. */
    data object CaptureReceipt : LogEvent

    /** Result of `TakePicture`, reported back by the UI. */
    data class ReceiptCaptured(val success: Boolean) : LogEvent

    /**
     * Result of the photo picker. Carries the source as a `String`, not an
     * `android.net.Uri`, so the ViewModel stays free of Android types — the data
     * layer parses it (ARCHITECTURE §3.1).
     */
    data class ReceiptPicked(val sourceUri: String) : LogEvent

    data object RemoveReceipt : LogEvent

    /** Submitted from the `+` chip dialog. Dialog visibility itself is local Compose state. */
    data class CreateCategoryRequested(val name: String) : LogEvent

    data object DismissCategoryError : LogEvent
}
