package com.quicklogger.app.presentation.log

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.ExpenseError

/**
 * One half of the remaining-budget line (ARCHITECTURE §8.1.8): either the selected
 * category's target or the overall monthly target. [label] is the category name, or
 * "Month" for the overall line — a fixed word, not a `strings.xml` chrome label, the
 * same way [com.quicklogger.app.domain.usecase.BuildPeriodSummary] hardcodes
 * "Day"/"Week"/"Month". [remainingFormatted] is already absolute-valued; the
 * composable picks the "left" or "over by" template from [isOver].
 */
data class BudgetLineUiModel(
    val label: String,
    val remainingFormatted: String,
    val isOver: Boolean,
)

/**
 * Everything the Log screen renders. [amountDigits] is the raw buffer (minor units,
 * digits only); [amountFormatted] is what the field shows. Both live here so the
 * composable stays a pure function of state.
 *
 * [receiptRelativePath] is only ever a *confirmed* receipt. A capture still waiting
 * on the camera is tracked privately by [com.quicklogger.app.presentation.receipt.ReceiptAttachmentController],
 * so no thumbnail appears for a photo that was never taken.
 *
 * [categoryBudgetLine] and [monthBudgetLine] are each null when that target does not
 * exist — absent target = the feature is invisible (ARCHITECTURE §6.5). Both are
 * live against [amountDigits]: they reflect what saving *would* leave, not the
 * balance before this entry.
 */
data class LogUiState(
    val amountDigits: String = "",
    val amountFormatted: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isSaving: Boolean = false,
    val saveError: ExpenseError? = null,
    val receiptRelativePath: String? = null,
    val isAttachingReceipt: Boolean = false,
    val receiptError: ReceiptError? = null,
    val categoryError: CategoryError? = null,
    val categoryBudgetLine: BudgetLineUiModel? = null,
    val monthBudgetLine: BudgetLineUiModel? = null,
) {
    val canSave: Boolean
        get() = !isSaving &&
            // Saving mid-copy would hand the receipt to the wrong expense.
            !isAttachingReceipt &&
            selectedCategoryId != null &&
            amountDigits.isNotEmpty() &&
            amountDigits.toLong() > 0L
}
