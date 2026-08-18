package com.quicklogger.app.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.DeleteCategory
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.RenameCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rename/delete, reachable from History's overflow (ARCHITECTURE §6.3). Creation is
 * a separate, simpler flow already owned by `LogViewModel`'s `+` chip — this
 * ViewModel only manages existing rows.
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    observeCategories: ObserveCategories,
    private val renameCategory: RenameCategory,
    private val deleteCategory: DeleteCategory,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCategories().collect { categories -> _uiState.update { it.copy(categories = categories) } }
        }
    }

    fun onEvent(event: CategoriesEvent) {
        when (event) {
            is CategoriesEvent.Rename -> viewModelScope.launch {
                renameCategory(event.id, event.name).onFailure(::setError)
            }
            is CategoriesEvent.Delete -> viewModelScope.launch {
                deleteCategory(event.id).onFailure(::setError)
            }
            CategoriesEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun setError(throwable: Throwable) {
        _uiState.update { it.copy(error = throwable as? CategoryError) }
    }
}
