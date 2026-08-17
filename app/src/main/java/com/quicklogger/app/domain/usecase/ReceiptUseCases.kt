package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.repository.ReceiptStore
import javax.inject.Inject

/**
 * Creates the empty file the camera will write into. The caller is responsible for
 * discarding it if the capture does not succeed.
 */
class CreateReceiptDraft @Inject constructor(
    private val receipts: ReceiptStore,
) {
    suspend operator fun invoke(): Result<String> = runCatching { receipts.createDraft() }
}

/** Copies a picked image into private storage. Fails on unreadable or oversized sources. */
class ImportReceipt @Inject constructor(
    private val receipts: ReceiptStore,
) {
    suspend operator fun invoke(sourceUri: String): Result<String> =
        runCatching { receipts.importFrom(sourceUri) }
}

/**
 * Discards a receipt file. Best-effort: a receipt that is already gone is not an
 * error, so this never fails the flow that called it.
 */
class DeleteReceipt @Inject constructor(
    private val receipts: ReceiptStore,
) {
    suspend operator fun invoke(relativePath: String) {
        runCatching { receipts.delete(relativePath) }
    }
}

/**
 * A capture that reported success but wrote nothing is a failed capture. Some camera
 * apps return `RESULT_OK` after writing zero bytes.
 */
class ReceiptHasContent @Inject constructor(
    private val receipts: ReceiptStore,
) {
    suspend operator fun invoke(relativePath: String): Boolean =
        runCatching { receipts.hasContent(relativePath) }.getOrDefault(false)
}
