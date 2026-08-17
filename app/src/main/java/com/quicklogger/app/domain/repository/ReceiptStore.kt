package com.quicklogger.app.domain.repository

/**
 * Receipt image storage. Implemented over `filesDir/receipts/` in the data layer.
 *
 * Paths crossing this boundary are always **relative** to that directory, and the
 * gallery source is a plain `String` rather than an `android.net.Uri`, so nothing
 * above this interface needs an Android type (ARCHITECTURE §3.2).
 */
interface ReceiptStore {
    /**
     * Creates an empty `{uuid}.jpg` ready for the camera to write into and returns
     * its relative path. The file must exist before `TakePicture` is launched.
     */
    suspend fun createDraft(): String

    /**
     * Copies the bytes behind [sourceUri] into a new private file and returns its
     * relative path. Throws [ReceiptError] if the source is unreadable or larger
     * than the size cap. The source Uri is never retained.
     */
    suspend fun importFrom(sourceUri: String): String

    /** Best-effort delete. A file that is already gone is not an error. */
    suspend fun delete(relativePath: String)

    /** True when the file exists and holds at least one byte. */
    suspend fun hasContent(relativePath: String): Boolean
}

sealed class ReceiptError(message: String) : Exception(message) {
    data object TooLarge : ReceiptError("Receipt image is larger than the 10 MB limit")
    data object Unreadable : ReceiptError("Receipt image could not be read")
}

/** Sub-directory of `filesDir` that every receipt lives under (ARCHITECTURE §7.2). */
const val RECEIPTS_DIRECTORY = "receipts"

/** Copies larger than this are rejected rather than silently downscaled. */
const val MAX_RECEIPT_BYTES = 10L * 1024 * 1024
