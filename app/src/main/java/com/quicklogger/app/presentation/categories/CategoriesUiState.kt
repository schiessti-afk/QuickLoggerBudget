package com.quicklogger.app.presentation.categories

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.usecase.CategoryError

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val error: CategoryError? = null,
)

sealed interface CategoriesEvent {
    data class Rename(val id: Long, val name: String) : CategoriesEvent
    data class Delete(val id: Long) : CategoriesEvent
    data object DismissError : CategoriesEvent
}
