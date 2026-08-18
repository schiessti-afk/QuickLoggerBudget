package com.quicklogger.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.ExpenseDateFormatter
import com.quicklogger.app.domain.model.ExpenseTotals
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.domain.model.PeriodBounds
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ObserveExpensesInRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider

/**
 * Display formatting happens here, not in Compose (mirrors [com.quicklogger.app.presentation.log.LogViewModel]'s
 * `Provider<Locale>` pattern), so `HistoryScreen` stays a pure function of [uiState].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeExpensesInRange: ObserveExpensesInRange,
    observeCategories: ObserveCategories,
    private val clock: Clock,
    private val zoneProvider: Provider<ZoneId>,
    private val localeProvider: Provider<Locale>,
) : ViewModel() {
    private val period = MutableStateFlow(Period.DAY)

    val uiState: StateFlow<HistoryUiState> = combine(
        period,
        period.flatMapLatest { observeExpensesInRange(PeriodBounds.of(it, today(), zoneProvider.get())) },
        observeCategories(),
    ) { selectedPeriod, expenses, categories ->
        buildState(selectedPeriod, expenses, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.PeriodSelected -> period.update { event.period }
        }
    }

    private fun buildState(
        selectedPeriod: Period,
        expenses: List<Expense>,
        categories: List<Category>,
    ): HistoryUiState {
        val locale = localeProvider.get()
        val zone = zoneProvider.get()
        val categoryNames = categories.associate { it.id to it.name }

        return HistoryUiState(
            period = selectedPeriod,
            rows = expenses.map { expense ->
                HistoryRowUiModel(
                    id = expense.id,
                    amountFormatted = MoneyFormatter.format(expense.amount, locale),
                    categoryName = categoryNames[expense.categoryId].orEmpty(),
                    occurredAtFormatted = ExpenseDateFormatter.format(expense.occurredAt, zone, locale),
                    hasReceipt = expense.receiptRelativePath != null,
                )
            },
            totalsFormatted = ExpenseTotals.byCurrency(expenses).map { MoneyFormatter.format(it, locale) },
        )
    }

    private fun today() = clock.instant().atZone(zoneProvider.get()).toLocalDate()
}
