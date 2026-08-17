package com.quicklogger.app

import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Every case passes an explicit locale and currency code. ARCHITECTURE §6.1 stores
 * the currency chosen at save time and renders it later regardless of the device's
 * current locale, so these tests must not inherit the JVM default.
 */
class MoneyFormatterTest {
    @Test
    fun formatsTwoFractionDigitCurrencyFromMinorUnits() {
        val formatted = MoneyFormatter.format(Money(minor = 4500, currencyCode = "USD"), Locale.US)

        assertEquals("$45.00", formatted)
    }

    @Test
    fun formatsSubUnitAmounts() {
        val formatted = MoneyFormatter.format(Money(minor = 5, currencyCode = "USD"), Locale.US)

        assertEquals("$0.05", formatted)
    }

    @Test
    fun zeroFractionDigitCurrencyTreatsMinorAsWholeUnits() {
        val formatted = MoneyFormatter.format(Money(minor = 4500, currencyCode = "JPY"), Locale.US)

        assertTrue(formatted, formatted.contains("4,500"))
        assertTrue("JPY has no minor unit: $formatted", !formatted.contains(".00"))
    }

    @Test
    fun rendersStoredCurrencyNotTheLocaleCurrency() {
        val formatted = MoneyFormatter.format(Money(minor = 4500, currencyCode = "BRL"), Locale.US)

        assertTrue("expected a BRL rendering, got $formatted", formatted.contains("R$"))
    }

    @Test
    fun keepsFullPrecisionBeyondDoubleRange() {
        // 2^53 + 1 minor units cannot round-trip through Double. A Double-based
        // formatter renders ...09.92 here; an exact one renders ...09.93.
        val formatted = MoneyFormatter.format(
            Money(minor = 9_007_199_254_740_993L, currencyCode = "USD"),
            Locale.US,
        )

        assertEquals("$90,071,992,547,409.93", formatted)
    }

    @Test
    fun emptyDigitBufferFormatsToEmptyString() {
        assertEquals("", MoneyFormatter.formatDigits("", "USD", Locale.US))
    }

    @Test
    fun digitBufferIsReadAsMinorUnits() {
        assertEquals("$45.00", MoneyFormatter.formatDigits("4500", "USD", Locale.US))
    }
}
