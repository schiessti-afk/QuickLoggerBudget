package com.quicklogger.app.presentation.expenseedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.ExpenseDateFormatter
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.repository.ExpenseRepository
import com.quicklogger.app.domain.usecase.DeleteExpense
import com.quicklogger.app.domain.usecase.ExpenseError
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.UpdateExpense
import com.quicklogger.app.presentation.log.AmountBuffer
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider

/**
 * Not on the fast path (ARCHITECTURE §8): amount, category, receipt, and — uniquely
 * to this screen — `occurredAt` are all editable. Receipt handling delegates to the
 * same [ReceiptAttachmentController] the Log screen uses.
 */
@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenses: ExpenseRepository,
    observeCategories: ObserveCategories,
    private val updateExpense: UpdateExpense,
    private val deleteExpense: DeleteExpense,
    private val receiptAttachment: ReceiptAttachmentController,
    private val zoneProvider: Provider<ZoneId>,
    private val localeProvider: Provider<Locale>,
) : ViewModel() {
    private val expenseId: Long = checkNotNull(savedStateHandle[ARG_ID]) { "Missing '$ARG_ID' nav argument" }

    /** The persisted row, kept so save/delete preserve `id`, `createdAt`, and currency. */
    private var loaded: Expense? = null

    private val _uiState = MutableStateFlow(ExpenseEditUiState())
    val uiState: StateFlow<ExpenseEditUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<ExpenseEditUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<ExpenseEditUiEvent> = _uiEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            val expense = expenses.getById(expenseId)
            if (expense == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            loaded = expense
            receiptAttachment.seed(expense.receiptRelativePath)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    amountDigits = expense.amount.minor.toString(),
                    amountFormatted = MoneyFormatter.format(expense.amount, locale()),
                    currencyCode = expense.amount.currencyCode,
                    selectedCategoryId = expense.categoryId,
                    occurredAt = expense.occurredAt,
                    occurredAtFormatted = ExpenseDateFormatter.format(expense.occurredAt, zone(), locale()),
                )
            }
        }
        viewModelScope.launch {
            observeCategories().collect { cats -> _uiState.update { it.copy(categories = cats) } }
        }
        // Unconfined: see the identical note in LogViewModel — this only mirrors
        // values, it must never wait on a dispatcher pump to do so.
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

    fun onEvent(event: ExpenseEditEvent) {
        when (event) {
            is ExpenseEditEvent.AmountChanged -> updateAmount(event.raw)
            is ExpenseEditEvent.CategorySelected ->
                _uiState.update { it.copy(selectedCategoryId = event.id, saveError = null) }
            is ExpenseEditEvent.OccurredAtChanged -> updateOccurredAt(event.instant)
            ExpenseEditEvent.Save -> save()
            ExpenseEditEvent.Delete -> delete()
            ExpenseEditEvent.CaptureReceipt -> viewModelScope.launch { receiptAttachment.capture() }
            is ExpenseEditEvent.ReceiptCaptured ->
                viewModelScope.launch { receiptAttachment.captureFinished(event.success) }
            is ExpenseEditEvent.ReceiptPicked -> {
                receiptAttachment.beginPick()
                viewModelScope.launch { receiptAttachment.finishPick(event.sourceUri) }
            }
            ExpenseEditEvent.RemoveReceipt -> viewModelScope.launch { receiptAttachment.remove() }
        }
    }

    private fun updateAmount(raw: String) {
        val digits = AmountBuffer.sanitize(raw)
        _uiState.update {
            it.copy(
                amountDigits = digits,
                amountFormatted = MoneyFormatter.formatDigits(digits, it.currencyCode, locale()),
                saveError = null,
            )
        }
    }

    private fun updateOccurredAt(instant: Instant) {
        _uiState.update {
            it.copy(occurredAt = instant, occurredAtFormatted = ExpenseDateFormatter.format(instant, zone(), locale()))
        }
    }

    private fun save() {
        val state = _uiState.value
        val original = loaded ?: return
        if (!state.canSave) return
        val categoryId = state.selectedCategoryId ?: return

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = updateExpense(
                original.copy(
                    amount = Money(state.amountDigits.toLong(), state.currencyCode),
                    categoryId = categoryId,
                    occurredAt = state.occurredAt,
                    receiptRelativePath = state.receiptRelativePath,
                ),
            )
            if (result.isSuccess) {
                receiptAttachment.clearAfterSave()
                _uiEvents.send(ExpenseEditUiEvent.NavigateBack)
            } else {
                _uiState.update { it.copy(isSaving = false, saveError = result.exceptionOrNull() as? ExpenseError) }
            }
        }
    }

    /** Deletes the persisted row and its file — not whatever is mid-edit in the form. */
    private fun delete() {
        val original = loaded ?: return
        viewModelScope.launch {
            deleteExpense(original)
            _uiEvents.send(ExpenseEditUiEvent.NavigateBack)
        }
    }

    private fun zone(): ZoneId = zoneProvider.get()

    private fun locale(): Locale = localeProvider.get()

    companion object {
        const val ARG_ID = "id"
    }
}
