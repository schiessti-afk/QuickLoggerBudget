package com.quicklogger.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quicklogger.app.R
import com.quicklogger.app.domain.model.Period

/**
 * Day/week/month radio filter (ARCHITECTURE §8.2). A plain `Row`, not `FlowRow` like
 * [CategoryChips]: three fixed labels always fit one line, so wrapping never applies.
 */
@Composable
fun PeriodChips(
    selected: Period,
    onSelected: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Period.entries.forEach { period ->
            val isSelected = period == selected
            FilterChip(
                selected = isSelected,
                onClick = { if (!isSelected) onSelected(period) },
                label = { Text(stringResource(period.labelRes)) },
            )
        }
    }
}

private val Period.labelRes: Int
    get() = when (this) {
        Period.DAY -> R.string.period_day
        Period.WEEK -> R.string.period_week
        Period.MONTH -> R.string.period_month
    }
