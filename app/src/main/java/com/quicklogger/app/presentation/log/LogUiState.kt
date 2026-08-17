package com.quicklogger.app.presentation.log

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.usecase.SaveExpenseError

/**
 * Everything the Log screen renders. [amountDigits] is the raw buffer (minor units,
 * digits only); [amountFormatted] is what the field shows. Both live here so the
 * composable stays a pure function of state.
 */
data class LogUiState(
    val amountDigits: String = "",
    val amountFormatted: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isSaving: Boolean = false,
    val saveError: SaveExpenseError? = null,
) {
    val canSave: Boolean
        get() = !isSaving &&
            selectedCategoryId != null &&
            amountDigits.isNotEmpty() &&
            amountDigits.toLong() > 0L
}
