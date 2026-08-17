package com.quicklogger.app.domain.model

/**
 * An amount in integer minor units (cents for USD/BRL, whole yen for JPY) plus the
 * ISO 4217 code it was captured in. ARCHITECTURE §6.1: money is never `Double` or
 * `Float`, and [currencyCode] is fixed at save time and never rewritten afterwards.
 */
data class Money(
    val minor: Long,
    val currencyCode: String,
)
