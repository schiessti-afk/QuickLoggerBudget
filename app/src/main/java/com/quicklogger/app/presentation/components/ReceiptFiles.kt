package com.quicklogger.app.presentation.components

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.quicklogger.app.domain.repository.RECEIPTS_DIRECTORY
import java.io.File

/**
 * Resolves receipt paths for the UI. ARCHITECTURE §3.1 puts `FileProvider` and
 * Activity Result plumbing in a small UI-side helper rather than in the ViewModel,
 * which must stay free of `Context` and `Uri`.
 *
 * The directory name comes from domain so presentation never has to import data.
 */
internal fun receiptFile(context: Context, relativePath: String): File =
    File(File(context.filesDir, RECEIPTS_DIRECTORY), relativePath)

/**
 * Wraps a private receipt in a temporary, per-Uri grant. The authority matches the
 * manifest's `${applicationId}.fileprovider`; at runtime `packageName` *is* the
 * applicationId, which avoids turning on the `buildConfig` feature just to read it.
 */
internal fun receiptUri(context: Context, relativePath: String): Uri =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        receiptFile(context, relativePath),
    )
