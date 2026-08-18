package com.quicklogger.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Computes the [DateRange] for a [Period], anchored on `today` (ARCHITECTURE §6.4).
 * Every period runs through the end of today, so they share one `end`; only the
 * start differs.
 *
 * Deliberately takes no [java.util.Locale]: the week always starts on the ISO
 * `DayOfWeek.MONDAY` constant, never on the device's `firstDayOfWeek`. A Sunday
 * locale must not shift this by a day, and not taking a Locale parameter at all is
 * what guarantees that rather than merely intending it.
 */
object PeriodBounds {
    fun of(period: Period, today: LocalDate, zone: ZoneId): DateRange {
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val start = when (period) {
            Period.DAY -> today
            Period.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Period.MONTH -> today.withDayOfMonth(1)
        }.atStartOfDay(zone).toInstant()
        return DateRange(start, end)
    }
}
