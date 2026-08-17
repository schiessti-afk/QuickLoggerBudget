package com.quicklogger.app.presentation.log

/**
 * The amount field is a digit buffer, not free text: whatever the IME produces is
 * reduced to ASCII digits before it reaches state. That makes paste, locale
 * separators, and the currency symbol the field itself renders all harmless.
 */
internal object AmountBuffer {
    /** 12 digits is ~10 billion major units — past any cash purchase, short of `Long` overflow. */
    const val MAX_DIGITS = 12

    fun sanitize(raw: String): String =
        raw.filter { it in '0'..'9' }.trimStart('0').take(MAX_DIGITS)
}
