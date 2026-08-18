package com.quicklogger.app.domain.model

import java.time.Instant

/** A half-open interval: [start, endExclusive). */
data class DateRange(val start: Instant, val endExclusive: Instant)

/** The three summary windows History filters by (ARCHITECTURE §6.4). */
enum class Period { DAY, WEEK, MONTH }
