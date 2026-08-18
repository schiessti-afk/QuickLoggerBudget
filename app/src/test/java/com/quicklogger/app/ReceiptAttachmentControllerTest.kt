package com.quicklogger.app

import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CreateReceiptDraft
import com.quicklogger.app.domain.usecase.DeleteReceipt
import com.quicklogger.app.domain.usecase.ImportReceipt
import com.quicklogger.app.domain.usecase.ReceiptHasContent
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentController
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentUiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive coverage of the receipt state machine, extracted from `LogViewModel` in
 * sprint 4 so it is tested once instead of twice (Log and expense-edit both drive it).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptAttachmentControllerTest {
    private fun controller(receipts: FakeReceiptStore) = ReceiptAttachmentController(
        CreateReceiptDraft(receipts),
        ImportReceipt(receipts),
        DeleteReceipt(receipts),
        ReceiptHasContent(receipts),
    )

    /** Collecting on an unconfined dispatcher, per the note in the sprint-3 test this replaces. */
    private fun TestScope.collectEvents(controller: ReceiptAttachmentController): List<ReceiptAttachmentUiEvent> {
        val collected = mutableListOf<ReceiptAttachmentUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            controller.events.collect { collected += it }
        }
        return collected
    }

    // --- seed ---

    @Test
    fun seedingSetsTheStartingReceiptWithoutTouchingStorage() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)

        controller.seed("existing.jpg")

        assertEquals("existing.jpg", controller.state.value.relativePath)
        assertTrue(receipts.files.isEmpty())
    }

    // --- camera ---

    @Test
    fun capturingCreatesTheFileBeforeLaunchingTheCamera() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        val events = collectEvents(controller)

        controller.capture()

        val launch = events.filterIsInstance<ReceiptAttachmentUiEvent.LaunchCamera>().single()
        assertTrue(receipts.files.containsKey(launch.relativePath))
    }

    @Test
    fun noThumbnailAppearsUntilTheCaptureSucceeds() = runTest {
        val controller = controller(FakeReceiptStore())
        collectEvents(controller)

        controller.capture()

        assertNull(controller.state.value.relativePath)
    }

    @Test
    fun aSuccessfulCaptureAttachesTheReceipt() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        val events = collectEvents(controller)
        controller.capture()
        val draft = events.filterIsInstance<ReceiptAttachmentUiEvent.LaunchCamera>().single().relativePath
        receipts.writeBytes(draft)

        controller.captureFinished(success = true)

        assertEquals(draft, controller.state.value.relativePath)
    }

    @Test
    fun aCancelledCaptureDeletesTheEmptyDraft() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        val events = collectEvents(controller)
        controller.capture()
        val draft = events.filterIsInstance<ReceiptAttachmentUiEvent.LaunchCamera>().single().relativePath

        controller.captureFinished(success = false)

        assertFalse(receipts.files.containsKey(draft))
        assertEquals(listOf(draft), receipts.deleted)
        assertNull(controller.state.value.relativePath)
    }

    @Test
    fun aCaptureThatReportsSuccessButWroteNothingIsTreatedAsAFailure() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        val events = collectEvents(controller)
        controller.capture()
        val draft = events.filterIsInstance<ReceiptAttachmentUiEvent.LaunchCamera>().single().relativePath
        // Deliberately no writeBytes: the file is still zero-length.

        controller.captureFinished(success = true)

        assertNull(controller.state.value.relativePath)
        assertEquals(listOf(draft), receipts.deleted)
    }

    // --- gallery ---

    @Test
    fun pickingAnImageAttachesThePrivateCopyNotThePickerUri() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)

        controller.beginPick(); controller.finishPick("content://media/external/images/42")

        val attached = controller.state.value.relativePath
        assertTrue(receipts.files.containsKey(attached))
        assertFalse(attached!!.startsWith("content://"))
    }

    @Test
    fun anOversizedPickSurfacesAnErrorAndAttachesNothing() = runTest {
        val controller = controller(FakeReceiptStore(importFailure = ReceiptError.TooLarge))

        controller.beginPick(); controller.finishPick("content://media/external/images/42")

        assertNull(controller.state.value.relativePath)
        assertEquals(ReceiptError.TooLarge, controller.state.value.error)
        assertFalse(controller.state.value.isAttaching)
    }

    // --- replace and remove ---

    @Test
    fun replacingAReceiptDeletesTheOneItReplaced() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        controller.beginPick(); controller.finishPick("content://first")
        val first = controller.state.value.relativePath!!

        controller.beginPick(); controller.finishPick("content://second")

        val second = controller.state.value.relativePath!!
        assertTrue(first != second)
        assertEquals(listOf(first), receipts.deleted)
    }

    @Test
    fun removingDeletesTheFileAndClearsTheThumbnail() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        controller.beginPick(); controller.finishPick("content://media/external/images/42")
        val attached = controller.state.value.relativePath!!

        controller.remove()

        assertNull(controller.state.value.relativePath)
        assertEquals(listOf(attached), receipts.deleted)
    }

    @Test
    fun removingWhileACaptureIsInFlightDeletesTheInProgressFile() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        val events = collectEvents(controller)
        controller.capture()
        val draft = events.filterIsInstance<ReceiptAttachmentUiEvent.LaunchCamera>().single().relativePath

        controller.remove()

        assertTrue(receipts.deleted.contains(draft))
        assertFalse(receipts.files.containsKey(draft))
    }

    // --- clearAfterSave ---

    @Test
    fun clearAfterSaveDetachesWithoutDeletingTheFile() = runTest {
        val receipts = FakeReceiptStore()
        val controller = controller(receipts)
        controller.beginPick(); controller.finishPick("content://media/external/images/42")
        val attached = controller.state.value.relativePath!!

        controller.clearAfterSave()

        assertNull(controller.state.value.relativePath)
        assertTrue("the caller now owns the file", receipts.files.containsKey(attached))
        assertTrue(receipts.deleted.isEmpty())
    }
}
