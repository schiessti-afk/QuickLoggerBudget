package com.quicklogger.app.presentation.expenseedit

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.ExpenseError
import java.time.Instant

data class ExpenseEditUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val amountDigits: String = "",
    val amountFormatted: String = "",
    val currencyCode: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val occurredAt: Instant = Instant.EPOCH,
    val occurredAtFormatted: String = "",
    val receiptRelativePath: String? = null,
    val isAttachingReceipt: Boolean = false,
    val receiptError: ReceiptError? = null,
    val isSaving: Boolean = false,
    val saveError: ExpenseError? = null,
) {
    val canSave: Boolean
        get() = !isLoading &&
            !notFound &&
            !isSaving &&
            !isAttachingReceipt &&
            selectedCategoryId != null &&
            amountDigits.isNotEmpty() &&
            amountDigits.toLong() > 0L
}
