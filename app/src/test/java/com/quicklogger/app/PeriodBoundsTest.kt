package com.quicklogger.app

import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.domain.model.PeriodBounds
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class PeriodBoundsTest {
    private val zone: ZoneId = ZoneOffset.UTC

    @Test
    fun dayRunsFromMidnightTodayThroughMidnightTomorrow() {
        val today = LocalDate.of(2026, 8, 18) // a Tuesday
        val range = PeriodBounds.of(Period.DAY, today, zone)

        assertEquals(today.atStartOfDay(zone).toInstant(), range.start)
        assertEquals(today.plusDays(1).atStartOfDay(zone).toInstant(), range.endExclusive)
    }

    @Test
    fun weekStartsOnTheMondayBeforeAMidweekDay() {
        val wednesday = LocalDate.of(2026, 8, 19)
        val range = PeriodBounds.of(Period.WEEK, wednesday, zone)

        val monday = LocalDate.of(2026, 8, 17)
        assertEquals(monday.atStartOfDay(zone).toInstant(), range.start)
        assertEquals(wednesday.plusDays(1).atStartOfDay(zone).toInstant(), range.endExclusive)
    }

    @Test
    fun weekStartsOnItselfWhenTodayIsMonday() {
        val monday = LocalDate.of(2026, 8, 17)
        val range = PeriodBounds.of(Period.WEEK, monday, zone)

        assertEquals(monday.atStartOfDay(zone).toInstant(), range.start)
    }

    @Test
    fun weekStartsOnTheSameMondayWhenTodayIsSunday() {
        // The tricky case the exit criterion calls out: a Sunday-firstDayOfWeek
        // locale must not roll this into "next Monday".
        val sunday = LocalDate.of(2026, 8, 23)
        val range = PeriodBounds.of(Period.WEEK, sunday, zone)

        val precedingMonday = LocalDate.of(2026, 8, 17)
        assertEquals(precedingMonday.atStartOfDay(zone).toInstant(), range.start)
    }

    @Test
    fun monthStartsOnTheFirstOfTheCurrentMonth() {
        val midMonth = LocalDate.of(2026, 8, 19)
        val range = PeriodBounds.of(Period.MONTH, midMonth, zone)

        val firstOfMonth = LocalDate.of(2026, 8, 1)
        assertEquals(firstOfMonth.atStartOfDay(zone).toInstant(), range.start)
        assertEquals(midMonth.plusDays(1).atStartOfDay(zone).toInstant(), range.endExclusive)
    }

    @Test
    fun monthStartsOnItselfWhenTodayIsTheFirst() {
        val firstOfMonth = LocalDate.of(2026, 9, 1)
        val range = PeriodBounds.of(Period.MONTH, firstOfMonth, zone)

        assertEquals(firstOfMonth.atStartOfDay(zone).toInstant(), range.start)
    }

    @Test
    fun boundsRespectTheGivenZoneNotUtc() {
        val nonUtcZone = ZoneId.of("America/Sao_Paulo")
        val today = LocalDate.of(2026, 8, 18)

        val range = PeriodBounds.of(Period.DAY, today, nonUtcZone)

        assertEquals(today.atStartOfDay(nonUtcZone).toInstant(), range.start)
    }
}
