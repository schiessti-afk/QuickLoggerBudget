package com.quicklogger.app.domain.model

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Renders [Money] for display and turns the Log screen's digit buffer into a
 * formatted string.
 *
 * Locale and currency are always arguments, never read from the environment: an
 * expense is displayed in the currency it was saved with even after the device
 * moves to another locale (ARCHITECTURE §6.1). `java.text` / `java.util` are JVM
 * types, so this stays inside the domain layer's import rules.
 *
 * Conversion goes through [BigDecimal] rather than a division — `Double` cannot
 * represent every `Long` minor value exactly.
 */
object MoneyFormatter {
    fun format(money: Money, locale: Locale): String {
        val currency = Currency.getInstance(money.currencyCode)
        val scale = currency.defaultFractionDigits.coerceAtLeast(0)
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            minimumFractionDigits = scale
            maximumFractionDigits = scale
        }
        return format.format(BigDecimal.valueOf(money.minor, scale))
    }

    /**
     * Formats a digit-only buffer read as minor units: `"4500"` is `$45.00` under
     * USD and `¥4,500` under JPY. A blank buffer formats to `""` so the amount field
     * shows its label instead of a zero the user did not type.
     */
    fun formatDigits(digits: String, currencyCode: String, locale: Locale): String =
        if (digits.isEmpty()) "" else format(Money(digits.toLong(), currencyCode), locale)
}
