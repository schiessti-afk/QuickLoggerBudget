package com.quicklogger.app.domain.repository

/**
 * CSV export storage. Implemented over `cacheDir/exports/` in the data layer — a
 * cache, not `filesDir`: an export is regenerated on demand, never a second
 * database (ARCHITECTURE §7.3, §9.3).
 *
 * Like [ReceiptStore], the path crossing this boundary is a relative file name, not
 * a `java.io.File` or `android.net.Uri`.
 */
interface CsvExportStore {
    /** Writes [csv] to `cacheDir/exports/{fileName}`, overwriting any prior file of that name. */
    suspend fun write(fileName: String, csv: String): String
}

/** Sub-directory of `cacheDir` every CSV export lives under (ARCHITECTURE §7.3). */
const val EXPORTS_DIRECTORY = "exports"
