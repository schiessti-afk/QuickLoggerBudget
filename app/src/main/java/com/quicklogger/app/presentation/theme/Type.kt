package com.quicklogger.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.quicklogger.app.R

/**
 * Inter 4.1 (OFL), static Regular / Medium / SemiBold from
 * https://github.com/rsms/inter/releases/tag/v4.1 — license at
 * `third_party/inter/OFL.txt`. M3 `Typography` has no `defaultFontFamily`;
 * each role must set `fontFamily` itself. Source:
 * https://developer.android.com/develop/ui/compose/designsystems/material3
 */
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private fun TextStyle.withInter() = copy(fontFamily = Inter)

internal val QuickLoggerTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.withInter(),
        displayMedium = displayMedium.withInter(),
        displaySmall = displaySmall.withInter(),
        headlineLarge = headlineLarge.withInter(),
        headlineMedium = headlineMedium.withInter(),
        headlineSmall = headlineSmall.withInter(),
        titleLarge = titleLarge.withInter(),
        titleMedium = titleMedium.withInter(),
        titleSmall = titleSmall.withInter(),
        bodyLarge = bodyLarge.withInter(),
        bodyMedium = bodyMedium.withInter(),
        bodySmall = bodySmall.withInter(),
        labelLarge = labelLarge.withInter(),
        labelMedium = labelMedium.withInter(),
        labelSmall = labelSmall.withInter(),
    )
}
