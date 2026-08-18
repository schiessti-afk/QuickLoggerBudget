package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.repository.CsvExportStore
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/**
 * Builds the CSV for [expenses] and writes it to `cacheDir/exports/`, named for the
 * **export** date — today, in [zone] — never the period start (ARCHITECTURE §7.3,
 * §9.3, and Sprint.md's exit criterion that this holds even for week/month exports).
 */
class ExportExpensesCsv @Inject constructor(
    private val buildExpensesCsv: BuildExpensesCsv,
    private val csvExportStore: CsvExportStore,
    private val clock: Clock,
) {
    /** Returns the relative file name the CSV was written under. */
    suspend operator fun invoke(
        expenses: List<Expense>,
        categoryNames: Map<Long, String>,
        zone: ZoneId,
    ): String {
        val csv = buildExpensesCsv(expenses, categoryNames, zone)
        val exportDate = clock.instant().atZone(zone).toLocalDate()
        val fileName = "quicklogger-$exportDate.csv"
        return csvExportStore.write(fileName, csv)
    }
}
