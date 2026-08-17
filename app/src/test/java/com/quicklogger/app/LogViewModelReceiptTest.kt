package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CreateReceiptDraft
import com.quicklogger.app.domain.usecase.DeleteReceipt
import com.quicklogger.app.domain.usecase.ImportReceipt
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ReceiptHasContent
import com.quicklogger.app.domain.usecase.SaveExpense
import com.quicklogger.app.presentation.log.LogEvent
import com.quicklogger.app.presentation.log.LogUiEvent
import com.quicklogger.app.presentation.log.LogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelReceiptTest {
    private val dispatcher = StandardTestDispatcher()
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)

    private lateinit var receipts: FakeReceiptStore
    private lateinit var expenses: FakeExpenseRepository

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(importFailure: ReceiptError? = null): LogViewModel {
        receipts = FakeReceiptStore(importFailure)
        expenses = FakeExpenseRepository()
        val categories = FakeCategoryRepository(listOf(food))
        return LogViewModel(
            observeCategories = ObserveCategories(categories),
            saveExpense = SaveExpense(
                expenses,
                categories,
                Clock.fixed(Instant.parse("2026-08-17T17:32:00Z"), ZoneOffset.UTC),
            ),
            lastCategoryStore = FakeLastCategoryStore(),
            createReceiptDraft = CreateReceiptDraft(receipts),
            importReceipt = ImportReceipt(receipts),
            deleteReceipt = DeleteReceipt(receipts),
            receiptHasContent = ReceiptHasContent(receipts),
            localeProvider = Provider { Locale.US },
        )
    }

    /**
     * Collects on an unconfined dispatcher so a `send` resumes the collector at the
     * point of the send. Under [StandardTestDispatcher] the resumption is queued as
     * background work that `advanceUntilIdle()` does not drain, and every assertion
     * here would see an empty list.
     */
    private fun TestScope.collectUiEvents(viewModel: LogViewModel): List<LogUiEvent> {
        val collected = mutableListOf<LogUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { collected += it }
        }
        return collected
    }

    // --- camera ---

    @Test
    fun capturingCreatesTheFileBeforeAskingTheUiToLaunchTheCamera() = runTest {
        val viewModel = viewModel()
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.CaptureReceipt)
        advanceUntilIdle()

        val launch = events.filterIsInstance<LogUiEvent.LaunchCamera>().single()
        assertTrue("the draft must exist before the camera starts", receipts.files.containsKey(launch.relativePath))
    }

    @Test
    fun noThumbnailAppearsUntilTheCaptureSucceeds() = runTest {
        val viewModel = viewModel()
        collectUiEvents(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.CaptureReceipt)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptRelativePath)
    }

    @Test
    fun aSuccessfulCaptureAttachesTheReceipt() = runTest {
        val viewModel = viewModel()
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.CaptureReceipt)
        advanceUntilIdle()
        val draft = events.filterIsInstance<LogUiEvent.LaunchCamera>().single().relativePath
        receipts.writeBytes(draft)

        viewModel.onEvent(LogEvent.ReceiptCaptured(success = true))
        advanceUntilIdle()

        assertEquals(draft, viewModel.uiState.value.receiptRelativePath)
    }

    @Test
    fun aCancelledCaptureDeletesTheEmptyDraft() = runTest {
        val viewModel = viewModel()
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.CaptureReceipt)
        advanceUntilIdle()
        val draft = events.filterIsInstance<LogUiEvent.LaunchCamera>().single().relativePath

        viewModel.onEvent(LogEvent.ReceiptCaptured(success = false))
        advanceUntilIdle()

        assertFalse(receipts.files.containsKey(draft))
        assertEquals(listOf(draft), receipts.deleted)
        assertNull(viewModel.uiState.value.receiptRelativePath)
    }

    @Test
    fun aCaptureThatReportsSuccessButWroteNothingIsTreatedAsAFailure() = runTest {
        val viewModel = viewModel()
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.CaptureReceipt)
        advanceUntilIdle()
        val draft = events.filterIsInstance<LogUiEvent.LaunchCamera>().single().relativePath
        // Deliberately no writeBytes: the file is still zero-length.

        viewModel.onEvent(LogEvent.ReceiptCaptured(success = true))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptRelativePath)
        assertEquals(listOf(draft), receipts.deleted)
    }

    // --- gallery ---

    @Test
    fun pickingAnImageCopiesItAndAttachesThePrivateCopy() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()

        val attached = viewModel.uiState.value.receiptRelativePath
        assertTrue(receipts.files.containsKey(attached))
        assertFalse("the picker Uri must never be stored", attached!!.startsWith("content://"))
    }

    @Test
    fun anOversizedPickSurfacesAnErrorAndAttachesNothing() = runTest {
        val viewModel = viewModel(importFailure = ReceiptError.TooLarge)
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptRelativePath)
        assertEquals(ReceiptError.TooLarge, viewModel.uiState.value.receiptError)
        assertFalse(viewModel.uiState.value.isAttachingReceipt)
    }

    // --- replace and remove ---

    @Test
    fun replacingAReceiptDeletesTheOneItReplaced() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.ReceiptPicked("content://first"))
        advanceUntilIdle()
        val first = viewModel.uiState.value.receiptRelativePath!!

        viewModel.onEvent(LogEvent.ReceiptPicked("content://second"))
        advanceUntilIdle()

        val second = viewModel.uiState.value.receiptRelativePath!!
        assertTrue(first != second)
        assertEquals(listOf(first), receipts.deleted)
        assertFalse(receipts.files.containsKey(first))
    }

    @Test
    fun removingAReceiptDeletesTheFileAndClearsTheThumbnail() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()
        val attached = viewModel.uiState.value.receiptRelativePath!!

        viewModel.onEvent(LogEvent.RemoveReceipt)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptRelativePath)
        assertEquals(listOf(attached), receipts.deleted)
    }

    @Test
    fun removingWhileACaptureIsInFlightDeletesTheInProgressFile() = runTest {
        val viewModel = viewModel()
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.CaptureReceipt)
        advanceUntilIdle()
        val draft = events.filterIsInstance<LogUiEvent.LaunchCamera>().single().relativePath

        viewModel.onEvent(LogEvent.RemoveReceipt)
        advanceUntilIdle()

        assertTrue(receipts.deleted.contains(draft))
        assertFalse(receipts.files.containsKey(draft))
    }

    // --- save ---

    @Test
    fun savingPersistsTheReceiptPathOnTheExpense() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()
        val attached = viewModel.uiState.value.receiptRelativePath!!
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertEquals(attached, expenses.inserted.single().receiptRelativePath)
    }

    @Test
    fun savingClearsTheReceiptFromTheFormWithoutDeletingTheFile() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()
        val attached = viewModel.uiState.value.receiptRelativePath!!
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptRelativePath)
        assertTrue("the saved expense still owns its file", receipts.files.containsKey(attached))
        assertTrue(receipts.deleted.isEmpty())
    }

    @Test
    fun savingWithNoReceiptStillWrites() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertNull(expenses.inserted.single().receiptRelativePath)
    }

    @Test
    fun saveIsBlockedWhileACopyIsStillRunning() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        // No advanceUntilIdle: the import is still in flight.

        assertFalse(viewModel.uiState.value.canSave)
    }
}
