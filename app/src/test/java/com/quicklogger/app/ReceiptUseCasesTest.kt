package com.quicklogger.app

import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CreateReceiptDraft
import com.quicklogger.app.domain.usecase.DeleteReceipt
import com.quicklogger.app.domain.usecase.ImportReceipt
import com.quicklogger.app.domain.usecase.ReceiptHasContent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptUseCasesTest {
    @Test
    fun creatingADraftReturnsAPathThatExistsButIsEmpty() = runTest {
        val store = FakeReceiptStore()

        val path = CreateReceiptDraft(store)().getOrThrow()

        assertTrue(store.files.containsKey(path))
        assertFalse("a fresh draft holds no bytes", ReceiptHasContent(store)(path))
    }

    @Test
    fun eachDraftGetsItsOwnPath() = runTest {
        val store = FakeReceiptStore()
        val create = CreateReceiptDraft(store)

        val first = create().getOrThrow()
        val second = create().getOrThrow()

        assertTrue(first != second)
    }

    @Test
    fun importingCopiesTheSourceAndReturnsAPrivatePath() = runTest {
        val store = FakeReceiptStore()

        val path = ImportReceipt(store)("content://media/external/images/42").getOrThrow()

        assertTrue(store.files.containsKey(path))
        assertTrue("the picker Uri must not become the stored path", !path.startsWith("content://"))
    }

    @Test
    fun anOversizedImportFails() = runTest {
        val store = FakeReceiptStore(importFailure = ReceiptError.TooLarge)

        val result = ImportReceipt(store)("content://media/external/images/42")

        assertEquals(ReceiptError.TooLarge, result.exceptionOrNull())
        assertTrue(store.files.isEmpty())
    }

    @Test
    fun anUnreadableImportFails() = runTest {
        val store = FakeReceiptStore(importFailure = ReceiptError.Unreadable)

        val result = ImportReceipt(store)("content://media/external/images/42")

        assertEquals(ReceiptError.Unreadable, result.exceptionOrNull())
    }

    @Test
    fun deletingRemovesTheFile() = runTest {
        val store = FakeReceiptStore()
        val path = CreateReceiptDraft(store)().getOrThrow()

        DeleteReceipt(store)(path)

        assertFalse(store.files.containsKey(path))
        assertEquals(listOf(path), store.deleted)
    }

    @Test
    fun deletingAMissingFileIsNotAnError() = runTest {
        val store = FakeReceiptStore()

        DeleteReceipt(store)("never-existed.jpg")

        assertEquals(listOf("never-existed.jpg"), store.deleted)
    }

    @Test
    fun aCapturedFileWithBytesHasContent() = runTest {
        val store = FakeReceiptStore()
        val path = CreateReceiptDraft(store)().getOrThrow()

        store.writeBytes(path)

        assertTrue(ReceiptHasContent(store)(path))
    }
}
