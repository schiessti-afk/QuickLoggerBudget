package com.quicklogger.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * DESIGN §5.4: the only two colors a budget surface may take on. Ledger green is a
 * fixed [BrandColors] constant (not a `ColorScheme` role); over-target reuses the
 * theme's own `error` role, the same red already used for validation and delete.
 */
@Composable
fun budgetStatusColor(isOver: Boolean): Color =
    if (isOver) MaterialTheme.colorScheme.error else Color(BrandColors.LEDGER_GREEN)
