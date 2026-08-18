package com.quicklogger.app.presentation.dashboard

import com.quicklogger.app.domain.model.Period

data class DashboardRowUiModel(
    val id: Long,
    val amountFormatted: String,
    val categoryName: String,
    val occurredAtFormatted: String,
    val hasReceipt: Boolean,
)

/**
 * The overall monthly meter (DESIGN §4.2). [fillRatio] is clamped to `0f..1f` for the
 * arc sweep — the wedge never overdraws past a full circle — while [isOver] (which is
 * not clamped) is what actually drives the "over by" wording and the error color.
 *
 * [hasTarget] is false when no overall target is set yet but the meter is still
 * shown — spend-or-target is what earns a category its bar, and the overall total
 * needs the same "tap here to set one" affordance, or there would be no way to ever
 * create the *first* overall target. In that state [remainingFormatted] /
 * [targetFormatted] are null and [spentFormatted] carries the total instead.
 */
data class BudgetMeterUiModel(
    val hasTarget: Boolean,
    val fillRatio: Float,
    val spentFormatted: String,
    val remainingFormatted: String?,
    val targetFormatted: String?,
    val isOver: Boolean,
)

/**
 * One row of the per-category breakdown (DESIGN §4.2). [fillFraction] and
 * [targetFraction] share one scale across every bar in the list — the largest
 * spend-or-target among them — so the bars are visually comparable, not each
 * independently normalized to its own target. [targetFraction] is null when this
 * category has no target: no tick is drawn, and the bar never turns over-red.
 */
data class BudgetBarUiModel(
    val categoryId: Long,
    val categoryName: String,
    val spentFormatted: String,
    val fillFraction: Float,
    val targetFraction: Float?,
    val isOver: Boolean,
)

/** Absent (both null/empty) means "draw nothing" — the dashboard falls back to plain History (DESIGN §4.2). */
data class BudgetOverviewUiModel(
    val meter: BudgetMeterUiModel? = null,
    val bars: List<BudgetBarUiModel> = emptyList(),
) {
    val isEmpty: Boolean get() = meter == null && bars.isEmpty()
}

/**
 * The target dialog opened by tapping the meter or a bar. [categoryId] `null` is the
 * overall target; [categoryName] is that category's name, or null for the overall
 * target — the composable supplies the "Monthly budget" title text for that case
 * from `strings.xml` rather than the ViewModel hardcoding it. Reuses the Log
 * digit-buffer field, hence [amountDigits] / [amountFormatted] instead of a plain
 * `String` amount.
 */
data class BudgetTargetDialogUiState(
    val categoryId: Long?,
    val categoryName: String?,
    val amountDigits: String = "",
    val amountFormatted: String = "",
)

data class DashboardUiState(
    val period: Period = Period.DAY,
    val rows: List<DashboardRowUiModel> = emptyList(),
    val totalsFormatted: List<String> = emptyList(),
    val overview: BudgetOverviewUiModel = BudgetOverviewUiModel(),
    val targetDialog: BudgetTargetDialogUiState? = null,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}
