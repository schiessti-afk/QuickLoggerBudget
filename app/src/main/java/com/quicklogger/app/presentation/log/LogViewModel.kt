package com.quicklogger.app.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.model.NewExpense
import com.quicklogger.app.domain.repository.LastCategoryStore
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.SaveExpense
import com.quicklogger.app.domain.usecase.SaveExpenseError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class LogViewModel @Inject constructor(
    observeCategories: ObserveCategories,
    private val saveExpense: SaveExpense,
    private val lastCategoryStore: LastCategoryStore,
    // A Provider, not a Locale: ViewModels survive configuration changes, so a
    // snapshot taken at construction would go stale after a locale switch.
    // ARCHITECTURE §6.1 fixes the currency at save time.
    private val localeProvider: Provider<Locale>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val remembered = lastCategoryStore.lastSelectedId()
            observeCategories().collect { categories ->
                _uiState.update { state ->
                    state.copy(
                        categories = categories,
                        selectedCategoryId = resolveSelection(
                            current = state.selectedCategoryId,
                            remembered = remembered,
                            categories = categories,
                        ),
                    )
                }
            }
        }
    }

    fun onEvent(event: LogEvent) {
        when (event) {
            is LogEvent.AmountChanged -> updateAmount(event.raw)
            is LogEvent.CategorySelected -> selectCategory(event.id)
            LogEvent.Save -> save()
        }
    }

    private fun updateAmount(raw: String) {
        val digits = AmountBuffer.sanitize(raw)
        _uiState.update {
            it.copy(
                amountDigits = digits,
                amountFormatted = MoneyFormatter.formatDigits(digits, currencyCode(), locale()),
                saveError = null,
            )
        }
    }

    private fun selectCategory(id: Long) {
        _uiState.update { it.copy(selectedCategoryId = id, saveError = null) }
        viewModelScope.launch { lastCategoryStore.setLastSelectedId(id) }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val categoryId = state.selectedCategoryId ?: return

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = saveExpense(
                NewExpense(
                    amount = Money(state.amountDigits.toLong(), currencyCode()),
                    categoryId = categoryId,
                ),
            )
            _uiState.update {
                if (result.isSuccess) {
                    // Amount clears, category stays: the next log needs no setup.
                    it.copy(amountDigits = "", amountFormatted = "", isSaving = false)
                } else {
                    it.copy(isSaving = false, saveError = result.exceptionOrNull() as? SaveExpenseError)
                }
            }
        }
    }

    /**
     * ARCHITECTURE §6.3: keep the current chip if it still exists, else the
     * remembered one, else the lowest `sortOrder`. `Other` is reached through that
     * last tier when it is the only row left, since it always exists.
     */
    private fun resolveSelection(
        current: Long?,
        remembered: Long?,
        categories: List<Category>,
    ): Long? {
        if (categories.isEmpty()) return null
        val existing = { id: Long? -> categories.firstOrNull { it.id == id }?.id }
        return existing(current)
            ?: existing(remembered)
            ?: categories.minByOrNull { it.sortOrder }?.id
    }

    private fun locale(): Locale = localeProvider.get()

    /** A locale with no country has no currency; fall back rather than crash the save. */
    private fun currencyCode(): String =
        runCatching { Currency.getInstance(locale()).currencyCode }
            .getOrDefault(FALLBACK_CURRENCY_CODE)

    private companion object {
        const val FALLBACK_CURRENCY_CODE = "USD"
    }
}
