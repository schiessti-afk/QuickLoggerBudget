package com.quicklogger.app.presentation.components

import android.content.Context
import android.content.Intent

/**
 * `ACTION_SEND` builders (ARCHITECTURE §9). Always the system chooser — never a
 * hard-coded package — so `Intent.createChooser` wraps every one of these before
 * `startActivity`.
 */
internal fun buildTextShareIntent(text: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

/** With a receipt: the image plus the same caption, and a temporary per-Uri grant. */
internal fun buildReceiptShareIntent(context: Context, text: String, receiptRelativePath: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, receiptUri(context, receiptRelativePath))
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

internal fun buildCsvShareIntent(context: Context, fileName: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, exportUri(context, fileName))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

internal fun Context.launchShareChooser(intent: Intent) {
    startActivity(Intent.createChooser(intent, null))
}
