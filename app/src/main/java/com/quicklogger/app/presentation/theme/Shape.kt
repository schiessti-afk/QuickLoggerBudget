package com.quicklogger.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DESIGN §3: soft paper corners. `small` is pinned to the M3 default (8 dp) so
 * FilterChip — which reads `shapes.small` / `CornerSmall` — stays visually
 * identical to sprint 7. Source:
 * https://developer.android.com/develop/ui/compose/designsystems/material3
 */
internal val QuickLoggerShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

/**
 * M3 buttons ignore [QuickLoggerShapes]: `ButtonDefaults.shape` resolves
 * `CornerFull` → a hard-coded `CircleShape`. Pass this at every `Button` /
 * `OutlinedButton` call site. Source:
 * https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-shapes/-shapes.html
 */
val QuickLoggerButtonShape = RoundedCornerShape(12.dp)
