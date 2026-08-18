package com.quicklogger.app.presentation.receipt

import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CreateReceiptDraft
import com.quicklogger.app.domain.usecase.DeleteReceipt
import com.quicklogger.app.domain.usecase.ImportReceipt
import com.quicklogger.app.domain.usecase.ReceiptHasContent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * The optional-receipt state machine: create a draft before the camera launches, only
 * confirm a capture that actually wrote bytes, delete whatever a replacement
 * displaces, and never touch a receipt a save has already claimed.
 *
 * Both the Log screen (sprint 3) and the expense-edit screen (sprint 4) need this
 * byte-for-byte — two real, simultaneous consumers, not a hypothetical third one — so
 * it lives here once instead of drifting apart in two ViewModels. Unscoped: each
 * owning ViewModel injects its own instance and drives it from its own
 * `viewModelScope`, projecting [state] into that ViewModel's own `UiState` shape.
 */
class ReceiptAttachmentController @Inject constructor(
    private val createReceiptDraft: CreateReceiptDraft,
    private val importReceipt: ImportReceipt,
    private val deleteReceipt: DeleteReceipt,
    private val receiptHasContent: ReceiptHasContent,
) {
    private val _state = MutableStateFlow(ReceiptAttachmentState())
    val state: StateFlow<ReceiptAttachmentState> = _state.asStateFlow()

    private val _events = Channel<ReceiptAttachmentUiEvent>(Channel.BUFFERED)
    val events: Flow<ReceiptAttachmentUiEvent> = _events.receiveAsFlow()

    /**
     * The file handed to the camera, held until the result comes back. Kept out of
     * [state] so a capture in progress never renders a thumbnail.
     */
    private var pendingCapturePath: String? = null

    /** Preloads an already-attached receipt, e.g. the one on an expense being edited. */
    fun seed(relativePath: String?) {
        _state.value = ReceiptAttachmentState(relativePath = relativePath)
    }

    suspend fun capture() {
        createReceiptDraft()
            .onSuccess { draft ->
                // The file must exist before TakePicture runs (ARCHITECTURE §7.2).
                pendingCapturePath = draft
                _state.update { it.copy(error = null) }
                _events.send(ReceiptAttachmentUiEvent.LaunchCamera(draft))
            }
            .onFailure { fail(ReceiptError.Unreadable) }
    }

    suspend fun captureFinished(success: Boolean) {
        val draft = pendingCapturePath ?: return
        pendingCapturePath = null
        // Some camera apps return OK having written nothing; a zero-length file is a
        // failed capture, not a receipt.
        if (success && receiptHasContent(draft)) {
            attach(draft)
        } else {
            deleteReceipt(draft)
        }
    }

    /**
     * Marks the pick as starting immediately — not a `suspend fun`, so a caller gates
     * on [state] (e.g. disables Save) as soon as this returns, before the async copy
     * even begins. Call [finishPick] right after, inside a coroutine.
     */
    fun beginPick() {
        _state.update { it.copy(isAttaching = true, error = null) }
    }

    suspend fun finishPick(sourceUri: String) {
        importReceipt(sourceUri)
            .onSuccess { attach(it) }
            .onFailure { fail(it as? ReceiptError ?: ReceiptError.Unreadable) }
    }

    suspend fun remove() {
        val attached = _state.value.relativePath
        val pending = pendingCapturePath
        pendingCapturePath = null
        _state.update {
            it.copy(relativePath = null, error = null, isAttaching = false)
        }
        attached?.let { deleteReceipt(it) }
        pending?.let { deleteReceipt(it) }
    }

    /**
     * Detaches the current receipt without deleting its file — the caller (a
     * successful save) now owns it. Does not touch a still-pending capture, matching
     * the same allowance the original Log save had before this was extracted.
     */
    fun clearAfterSave() {
        _state.update { it.copy(relativePath = null, error = null) }
    }

    /** Attaches [relativePath], deleting whatever receipt it replaces. */
    private suspend fun attach(relativePath: String) {
        val replaced = _state.value.relativePath
        _state.update {
            it.copy(relativePath = relativePath, isAttaching = false, error = null)
        }
        if (replaced != null && replaced != relativePath) deleteReceipt(replaced)
    }

    private fun fail(error: ReceiptError) {
        _state.update { it.copy(isAttaching = false, error = error) }
    }
}

/** Only ever a *confirmed* receipt — a capture still waiting on the camera is private. */
data class ReceiptAttachmentState(
    val relativePath: String? = null,
    val isAttaching: Boolean = false,
    val error: ReceiptError? = null,
)

/**
 * One-shot effect. ARCHITECTURE §5 rule 4: consumed once and dropped, never parked
 * in state where it would replay on the next recomposition.
 */
sealed interface ReceiptAttachmentUiEvent {
    /** The draft file already exists; the UI resolves it to a FileProvider Uri. */
    data class LaunchCamera(val relativePath: String) : ReceiptAttachmentUiEvent
}
