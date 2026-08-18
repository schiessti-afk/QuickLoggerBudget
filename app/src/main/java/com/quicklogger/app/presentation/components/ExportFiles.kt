package com.quicklogger.app.presentation.components

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.quicklogger.app.domain.repository.EXPORTS_DIRECTORY
import java.io.File

/** Mirrors [receiptFile] / [receiptUri] for CSV exports under `cacheDir/exports/`. */
internal fun exportFile(context: Context, fileName: String): File =
    File(File(context.cacheDir, EXPORTS_DIRECTORY), fileName)

internal fun exportUri(context: Context, fileName: String): Uri =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportFile(context, fileName),
    )
