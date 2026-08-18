package com.quicklogger.app.presentation.theme

object BrandColors {
    const val PRIMARY = 0xFF9A4A32
    const val ON_PRIMARY = 0xFFFFF8F3
    const val SURFACE = 0xFFF6F1E8
    const val SURFACE_CONTAINER = 0xFFEFE7D8
    const val ON_SURFACE = 0xFF2A241F
    const val ON_SURFACE_VARIANT = 0xFF6F675E
    const val OUTLINE = 0xFFC9BBA8
    const val ERROR = 0xFF9B2F2F

    /**
     * DESIGN §5.4: budget-under-target only. A brand extension, not a Material role —
     * deliberately not wired into [QuickLoggerColorScheme] (not `tertiary`, no green
     * `ColorScheme`) so it stays reachable only from the budget meter/bars that are
     * allowed to use it, never picked up by an unrelated component reading the theme.
     */
    const val LEDGER_GREEN = 0xFF3F6B45
}
