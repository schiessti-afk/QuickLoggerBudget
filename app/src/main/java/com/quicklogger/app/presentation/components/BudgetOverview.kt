package com.quicklogger.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quicklogger.app.R
import com.quicklogger.app.presentation.dashboard.BudgetBarUiModel
import com.quicklogger.app.presentation.dashboard.BudgetMeterUiModel
import com.quicklogger.app.presentation.theme.budgetStatusColor
import com.quicklogger.app.presentation.theme.categoryStyleFor

/**
 * The overall monthly meter (DESIGN §4.2): a hand-drawn arc, no charting library
 * (DESIGN §6). Tapping it opens the overall target dialog — including when
 * [BudgetMeterUiModel.hasTarget] is false, which is what lets the *first* overall
 * target ever get created: the meter is shown (empty ring, spend total, a "set a
 * budget" prompt) as soon as there's any spend or category target to justify drawing
 * the overview at all, the same "spend-or-target" rule a category bar already uses.
 */
@Composable
fun BudgetMeter(
    meter: BudgetMeterUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = budgetStatusColor(meter.isOver)
    val trackColor = MaterialTheme.colorScheme.outline
    val centerLabel = when {
        !meter.hasTarget -> stringResource(R.string.budget_meter_spent_so_far, meter.spentFormatted)
        meter.isOver -> stringResource(R.string.budget_meter_over_by, meter.remainingFormatted!!)
        else -> stringResource(R.string.budget_meter_remaining_left, meter.remainingFormatted!!)
    }
    val subLabel = if (meter.hasTarget) {
        stringResource(R.string.budget_meter_of_target, meter.targetFormatted!!)
    } else {
        stringResource(R.string.budget_meter_set_prompt)
    }

    Column(
        modifier = modifier
            .size(176.dp)
            .clip(CircleShape)
            .clickable(
                onClickLabel = stringResource(R.string.budget_meter_content_description, centerLabel),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val strokeWidth = 12.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (meter.fillRatio > 0f) {
                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = 360f * meter.fillRatio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.titleMedium,
            color = if (meter.hasTarget && meter.isOver) statusColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One category row (DESIGN §4.2): the fill is the category's own accent — identity
 * beats status here — with the segment past the target tick switching to the error
 * role. No tick, and never over-colored, when [BudgetBarUiModel.targetFraction] is
 * null. Tapping the row opens that category's target dialog.
 */
@Composable
fun BudgetBarRow(
    bar: BudgetBarUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = categoryStyleFor(bar.categoryName).accent
    val overColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val tickColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.budget_bar_content_description, bar.categoryName),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = bar.categoryName,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(84.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Canvas(modifier = Modifier.weight(1f).height(8.dp)) {
            val corner = CornerRadius(size.height / 2f)
            drawRoundRect(color = trackColor, cornerRadius = corner)

            val underFraction = if (bar.targetFraction != null) {
                minOf(bar.fillFraction, bar.targetFraction)
            } else {
                bar.fillFraction
            }
            if (underFraction > 0f) {
                drawRoundRect(
                    color = accent,
                    size = size.copy(width = size.width * underFraction),
                    cornerRadius = corner,
                )
            }
            if (bar.targetFraction != null && bar.fillFraction > bar.targetFraction) {
                drawRoundRect(
                    color = overColor,
                    topLeft = Offset(size.width * bar.targetFraction, 0f),
                    size = size.copy(width = size.width * (bar.fillFraction - bar.targetFraction)),
                    cornerRadius = corner,
                )
            }
            bar.targetFraction?.let { fraction ->
                val x = size.width * fraction
                drawLine(
                    color = tickColor,
                    start = Offset(x, -4.dp.toPx()),
                    end = Offset(x, size.height + 4.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = bar.spentFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = if (bar.isOver) overColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
