package com.quicklogger.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.data.receipt.ReceiptFileStore
import com.quicklogger.app.domain.repository.MAX_RECEIPT_BYTES
import com.quicklogger.app.domain.repository.RECEIPTS_DIRECTORY
import com.quicklogger.app.domain.repository.ReceiptError
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReceiptFileStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: ReceiptFileStore
    private lateinit var receiptsDir: File
    private lateinit var sourceDir: File

    @Before
    fun setUp() {
        store = ReceiptFileStore(context)
        receiptsDir = File(context.filesDir, RECEIPTS_DIRECTORY)
        sourceDir = File(context.cacheDir, "receipt-test-sources").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        receiptsDir.deleteRecursively()
        sourceDir.deleteRecursively()
    }

    /** A `file://` Uri exercises the same ContentResolver path as the photo picker. */
    private fun sourceUriOf(bytes: ByteArray): String {
        val file = File(sourceDir, "source-${bytes.size}-${System.nanoTime()}.jpg")
        file.writeBytes(bytes)
        return android.net.Uri.fromFile(file).toString()
    }

    @Test
    fun aDraftLandsInTheReceiptsDirectoryAndIsEmpty() = runTest {
        val path = store.createDraft()

        val file = File(receiptsDir, path)
        assertTrue("draft should exist at ${file.absolutePath}", file.isFile)
        assertEquals(0L, file.length())
        assertTrue(path.endsWith(".jpg"))
    }

    @Test
    fun draftsDoNotCollide() = runTest {
        val first = store.createDraft()
        val second = store.createDraft()

        assertTrue(first != second)
        assertEquals(2, receiptsDir.listFiles()!!.size)
    }

    @Test
    fun everyReceiptStaysUnderFilesDir() = runTest {
        val path = store.createDraft()

        val canonical = File(receiptsDir, path).canonicalPath
        assertTrue(
            "receipts must not escape filesDir: $canonical",
            canonical.startsWith(context.filesDir.canonicalPath),
        )
    }

    @Test
    fun importingCopiesTheBytesIntoPrivateStorage() = runTest {
        val bytes = ByteArray(2_048) { it.toByte() }

        val path = store.importFrom(sourceUriOf(bytes))

        val copied = File(receiptsDir, path)
        assertTrue(copied.isFile)
        assertTrue(bytes.contentEquals(copied.readBytes()))
    }

    @Test
    fun importedFilesDoNotKeepTheSourceName() = runTest {
        val path = store.importFrom(sourceUriOf(ByteArray(16)))

        assertFalse(path.contains("source-"))
        assertTrue(path.endsWith(".jpg"))
    }

    @Test
    fun anOversizedSourceIsRejectedAndLeavesNoPartialFile() = runTest {
        val tooBig = ByteArray((MAX_RECEIPT_BYTES + 1_024).toInt())

        val thrown = runCatching { store.importFrom(sourceUriOf(tooBig)) }.exceptionOrNull()

        assertEquals(ReceiptError.TooLarge, thrown)
        assertTrue(
            "a rejected import must not leave bytes behind",
            receiptsDir.listFiles().isNullOrEmpty(),
        )
    }

    @Test
    fun anUnreadableSourceFails() = runTest {
        val thrown = runCatching {
            store.importFrom("file://${sourceDir.absolutePath}/does-not-exist.jpg")
        }.exceptionOrNull()

        assertEquals(ReceiptError.Unreadable, thrown)
        assertTrue(receiptsDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun deletingRemovesTheFile() = runTest {
        val path = store.createDraft()

        store.delete(path)

        assertFalse(File(receiptsDir, path).exists())
    }

    @Test
    fun deletingAMissingFileIsNotAnError() = runTest {
        store.delete("never-existed.jpg")
    }

    @Test
    fun deleteCannotEscapeTheReceiptsDirectory() = runTest {
        val outsider = File(context.filesDir, "not-a-receipt.txt").apply { writeText("keep me") }

        store.delete("../not-a-receipt.txt")

        assertTrue("path traversal must not delete app files", outsider.exists())
        outsider.delete()
    }

    @Test
    fun anEmptyDraftHasNoContent() = runTest {
        val path = store.createDraft()

        assertFalse(store.hasContent(path))
    }

    @Test
    fun aWrittenFileHasContent() = runTest {
        val path = store.importFrom(sourceUriOf(ByteArray(512) { 1 }))

        assertTrue(store.hasContent(path))
    }

    @Test
    fun aMissingFileHasNoContent() = runTest {
        assertFalse(store.hasContent("never-existed.jpg"))
    }
}
