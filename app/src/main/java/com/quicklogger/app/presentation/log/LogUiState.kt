package com.quicklogger.app.presentation.log

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.ExpenseError

/**
 * Everything the Log screen renders. [amountDigits] is the raw buffer (minor units,
 * digits only); [amountFormatted] is what the field shows. Both live here so the
 * composable stays a pure function of state.
 *
 * [receiptRelativePath] is only ever a *confirmed* receipt. A capture still waiting
 * on the camera is tracked privately by [com.quicklogger.app.presentation.receipt.ReceiptAttachmentController],
 * so no thumbnail appears for a photo that was never taken.
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
) {
    val canSave: Boolean
        get() = !isSaving &&
            // Saving mid-copy would hand the receipt to the wrong expense.
            !isAttachingReceipt &&
            selectedCategoryId != null &&
            amountDigits.isNotEmpty() &&
            amountDigits.toLong() > 0L
}
