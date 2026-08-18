package com.quicklogger.app.data.export

import android.content.Context
import com.quicklogger.app.domain.repository.CsvExportStore
import com.quicklogger.app.domain.repository.EXPORTS_DIRECTORY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CSV exports under `cacheDir/exports/` (ARCHITECTURE §7.3). Cache, not `filesDir`:
 * the OS is free to reclaim it, and QuickLogger only ever regenerates it on demand.
 */
@Singleton
class CsvFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CsvExportStore {
    private val directory: File
        get() = File(context.cacheDir, EXPORTS_DIRECTORY).apply { mkdirs() }

    override suspend fun write(fileName: String, csv: String): String = withContext(Dispatchers.IO) {
        File(directory, fileName).writeText(csv, Charsets.UTF_8)
        fileName
    }
}
