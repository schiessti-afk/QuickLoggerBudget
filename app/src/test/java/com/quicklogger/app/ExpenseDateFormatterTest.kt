package com.quicklogger.app

import com.quicklogger.app.domain.model.ExpenseDateFormatter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class ExpenseDateFormatterTest {
    @Test
    fun formatsInTheGivenZoneNotUtc() {
        val instant = Instant.parse("2026-08-17T23:32:00Z")

        val utc = ExpenseDateFormatter.format(instant, ZoneOffset.UTC, Locale.US)
        val minusThree = ExpenseDateFormatter.format(instant, ZoneOffset.ofHours(-3), Locale.US)

        assertTrue("UTC and -3 must render different local times", utc != minusThree)
    }

    @Test
    fun formatsUsingTheGivenLocaleNotTheJvmDefault() {
        val instant = Instant.parse("2026-08-17T14:32:00Z")

        val us = ExpenseDateFormatter.format(instant, ZoneOffset.UTC, Locale.US)
        val brazil = ExpenseDateFormatter.format(instant, ZoneOffset.UTC, Locale.of("pt", "BR"))

        assertTrue("US and pt-BR must render different date formats", us != brazil)
    }
}
