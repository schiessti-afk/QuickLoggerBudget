package com.quicklogger.app.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.model.NewExpense
import com.quicklogger.app.domain.repository.LastCategoryStore
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.CreateCategory
import com.quicklogger.app.domain.usecase.ExpenseError
import com.quicklogger.app.domain.usecase.FormatExpenseShareText
import com.quicklogger.app.domain.usecase.ObserveCategories
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class LogViewModel @Inject constructor(
    observeCategories: ObserveCategories,
    private val saveExpense: SaveExpense,
    private val lastCategoryStore: LastCategoryStore,
    private val createCategory: CreateCategory,
    private val receiptAttachment: ReceiptAttachmentController,
    private val formatExpenseShareText: FormatExpenseShareText,
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
                    it.copy(amountDigits = "", amountFormatted = "", isSaving = false)
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
    }
}
