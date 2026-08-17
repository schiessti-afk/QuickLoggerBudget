package com.quicklogger.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun QuickLoggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuickLoggerColorScheme,
        typography = QuickLoggerTypography,
        content = content,
    )
}
