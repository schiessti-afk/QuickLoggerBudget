package com.quicklogger.app.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicklogger.app.domain.model.BudgetProgress
import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.model.NewExpense
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.domain.model.PeriodBounds
import com.quicklogger.app.domain.repository.LastCategoryStore
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.CreateCategory
import com.quicklogger.app.domain.usecase.ExpenseError
import com.quicklogger.app.domain.usecase.FormatExpenseShareText
import com.quicklogger.app.domain.usecase.ObserveBudgetTargets
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ObserveExpensesInRange
import com.quicklogger.app.domain.usecase.SaveExpense
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentController
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.abs

@HiltViewModel
class LogViewModel @Inject constructor(
    observeCategories: ObserveCategories,
    private val observeBudgetTargets: ObserveBudgetTargets,
    private val observeExpensesInRange: ObserveExpensesInRange,
    private val saveExpense: SaveExpense,
    private val lastCategoryStore: LastCategoryStore,
    private val createCategory: CreateCategory,
    private val receiptAttachment: ReceiptAttachmentController,
    private val formatExpenseShareText: FormatExpenseShareText,
    private val clock: Clock,
    // A Provider, not a Locale: ViewModels survive configuration changes, so a
    // snapshot taken at construction would go stale after a locale switch.
    // ARCHITECTURE §6.1 fixes the currency at save time.
    private val localeProvider: Provider<Locale>,
    private val zoneProvider: Provider<ZoneId>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private val _shareEvents = Channel<LogUiEvent.Share>(Channel.BUFFERED)

    val uiEvents: Flow<LogUiEvent> = merge(
        receiptAttachment.events.map { event ->
            when (event) {
                is ReceiptAttachmentUiEvent.LaunchCamera -> LogUiEvent.LaunchCamera(event.relativePath)
            }
        },
        _shareEvents.receiveAsFlow(),
    )

    /** Backs the remaining-budget line; not part of [LogUiState] itself (ARCHITECTURE §8.1.8). */
    private var budgetTargets: List<BudgetTarget> = emptyList()
    private var expensesThisMonth: List<Expense> = emptyList()

    init {
        viewModelScope.launch {
            val remembered = lastCategoryStore.lastSelectedId()
            observeCategories().collect { categories ->
                _uiState.update { state ->
                    withBudgetLines(
                        state.copy(
                            categories = categories,
                            selectedCategoryId = resolveSelection(
                                current = state.selectedCategoryId,
                                remembered = remembered,
                                categories = categories,
                            ),
                        ),
                    )
                }
            }
        }
        // Unconfined, not the (test-)Main dispatcher: this coroutine only copies
        // values, it does no real async work of its own, so its resumption on each
        // emission should never wait for a dispatcher pump. beginPick()'s synchronous
        // flip must reach canSave before finishPick()'s launch even runs.
        viewModelScope.launch(Dispatchers.Unconfined) {
            receiptAttachment.state.collect { receipt ->
                _uiState.update {
                    it.copy(
                        receiptRelativePath = receipt.relativePath,
                        isAttachingReceipt = receipt.isAttaching,
                        receiptError = receipt.error,
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                observeBudgetTargets(),
                observeExpensesInRange(PeriodBounds.of(Period.MONTH, today(), zoneProvider.get())),
            ) { targets, expenses -> targets to expenses }
                .collect { (targets, expenses) ->
                    budgetTargets = targets
                    expensesThisMonth = expenses
                    _uiState.update { withBudgetLines(it) }
                }
        }
    }

    fun onEvent(event: LogEvent) {
        when (event) {
            is LogEvent.AmountChanged -> updateAmount(event.raw)
            is LogEvent.CategorySelected -> selectCategory(event.id)
            LogEvent.Save -> save(shareAfterSave = false)
            LogEvent.SaveAndShare -> save(shareAfterSave = true)
            LogEvent.CaptureReceipt -> viewModelScope.launch { receiptAttachment.capture() }
            is LogEvent.ReceiptCaptured ->
                viewModelScope.launch { receiptAttachment.captureFinished(event.success) }
            is LogEvent.ReceiptPicked -> {
                // Mirrors save()'s shape: flip the gate synchronously, do the async
                // copy in the launch — so canSave reflects "attaching" immediately.
                receiptAttachment.beginPick()
                viewModelScope.launch { receiptAttachment.finishPick(event.sourceUri) }
            }
            LogEvent.RemoveReceipt -> viewModelScope.launch { receiptAttachment.remove() }
            is LogEvent.CreateCategoryRequested -> submitNewCategory(event.name)
            LogEvent.DismissCategoryError -> _uiState.update { it.copy(categoryError = null) }
        }
    }

    private fun updateAmount(raw: String) {
        val digits = AmountBuffer.sanitize(raw)
        _uiState.update {
            withBudgetLines(
                it.copy(
                    amountDigits = digits,
                    amountFormatted = MoneyFormatter.formatDigits(digits, currencyCode(), locale()),
                    saveError = null,
                ),
            )
        }
    }

    private fun selectCategory(id: Long) {
        _uiState.update { withBudgetLines(it.copy(selectedCategoryId = id, saveError = null)) }
        viewModelScope.launch { lastCategoryStore.setLastSelectedId(id) }
    }

    /**
     * Recomputes [LogUiState.categoryBudgetLine] and [LogUiState.monthBudgetLine]
     * from [budgetTargets]/[expensesThisMonth] plus [state]'s own amount buffer and
     * selection — live, per ARCHITECTURE §8.1.8: it reflects what saving *now* would
     * leave, not the balance before this entry.
     */
    private fun withBudgetLines(state: LogUiState): LogUiState {
        val pendingMinor = state.amountDigits.toLongOrNull() ?: 0L
        val currency = currencyCode()
        val categoryName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name

        val categoryTarget = state.selectedCategoryId?.let { id ->
            budgetTargets.firstOrNull { it.categoryId == id }
        }
        val overallTarget = budgetTargets.firstOrNull { it.categoryId == null }

        return state.copy(
            categoryBudgetLine = categoryTarget?.let { target ->
                budgetLine(categoryName.orEmpty(), target, pendingMinor, currency)
            },
            monthBudgetLine = overallTarget?.let { target ->
                budgetLine(MONTH_LABEL, target, pendingMinor, currency)
            },
        )
    }

    private fun budgetLine(
        label: String,
        target: BudgetTarget,
        pendingMinor: Long,
        pendingCurrencyCode: String,
    ): BudgetLineUiModel {
        val remaining = BudgetProgress.remainingIncludingPending(
            target,
            expensesThisMonth,
            pendingMinor,
            pendingCurrencyCode,
        )
        return BudgetLineUiModel(
            label = label,
            remainingFormatted = MoneyFormatter.format(Money(abs(remaining.minor), remaining.currencyCode), locale()),
            isOver = remaining.minor < 0,
        )
    }

    private fun today() = clock.instant().atZone(zoneProvider.get()).toLocalDate()

    /** [shareAfterSave] is Save & Share (ARCHITECTURE §8.1): identical write, plus a share event. */
    private fun save(shareAfterSave: Boolean) {
        val state = _uiState.value
        if (!state.canSave) return
        val categoryId = state.selectedCategoryId ?: return

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = saveExpense(
                NewExpense(
                    amount = Money(state.amountDigits.toLong(), currencyCode()),
                    categoryId = categoryId,
                    receiptRelativePath = state.receiptRelativePath,
                ),
            )
            result.onSuccess { saved ->
                // The file is NOT deleted — the persisted expense owns it now.
                receiptAttachment.clearAfterSave()
                if (shareAfterSave) {
                    val categoryName = state.categories.firstOrNull { it.id == categoryId }?.name.orEmpty()
                    val text = formatExpenseShareText(saved, categoryName, locale(), zoneProvider.get())
                    _shareEvents.send(LogUiEvent.Share(text, saved.receiptRelativePath))
                }
            }
            _uiState.update {
                if (result.isSuccess) {
                    withBudgetLines(it.copy(amountDigits = "", amountFormatted = "", isSaving = false))
                } else {
                    it.copy(isSaving = false, saveError = result.exceptionOrNull() as? ExpenseError)
                }
            }
        }
    }

    /** From the Log screen's `+` chip. Auto-selects the new chip so it is usable immediately. */
    private fun submitNewCategory(name: String) {
        viewModelScope.launch {
            createCategory(name)
                .onSuccess { selectCategory(it.id) }
                .onFailure { error ->
                    _uiState.update { it.copy(categoryError = error as? CategoryError) }
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

        // Hardcoded like BuildPeriodSummary's Period.label(): a fixed English word for
        // a fixed concept (the overall monthly target), not `strings.xml` UI chrome.
        const val MONTH_LABEL = "Month"
    }
}
