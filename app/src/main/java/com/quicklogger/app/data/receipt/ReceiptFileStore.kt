package com.quicklogger.app.data.receipt

import android.content.Context
import android.net.Uri
import com.quicklogger.app.domain.repository.MAX_RECEIPT_BYTES
import com.quicklogger.app.domain.repository.RECEIPTS_DIRECTORY
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.repository.ReceiptStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Receipt images under `filesDir/receipts/` (ARCHITECTURE §7.2).
 *
 * This is the only class that turns a picker `content://` string into an actual
 * stream. Nothing here touches `MediaStore`, so a capture or import never adds an
 * image to the device gallery. Uninstalling the app removes the directory with it.
 */
@Singleton
class ReceiptFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReceiptStore {
    private val directory: File
        get() = File(context.filesDir, RECEIPTS_DIRECTORY).apply { mkdirs() }

    override suspend fun createDraft(): String = withContext(Dispatchers.IO) {
        val name = "${UUID.randomUUID()}.jpg"
        val file = File(directory, name)
        if (!file.createNewFile() && !file.exists()) throw IOException("Could not create $name")
        name
    }

    override suspend fun importFrom(sourceUri: String): String = withContext(Dispatchers.IO) {
        val name = "${UUID.randomUUID()}.jpg"
        val destination = File(directory, name)

        val copied = runCatching {
            context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                if (input == null) throw ReceiptError.Unreadable
                destination.outputStream().use { output -> input.copyCapped(output) }
            }
        }

        copied.fold(
            onSuccess = { name },
            onFailure = { cause ->
                // Never leave a partial copy behind.
                destination.delete()
                throw if (cause is ReceiptError) cause else ReceiptError.Unreadable
            },
        )
    }

    override suspend fun delete(relativePath: String) {
        withContext(Dispatchers.IO) { resolve(relativePath)?.delete() }
    }

    override suspend fun hasContent(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolve(relativePath)
        file != null && file.isFile && file.length() > 0L
    }

    /**
     * Rejects anything that would escape the receipts directory. Relative paths come
     * from our own database, but a traversal here would read or delete arbitrary app
     * files, so it is checked rather than assumed.
     */
    private fun resolve(relativePath: String): File? {
        val directory = directory
        val file = File(directory, relativePath)
        return file.takeIf { it.canonicalPath.startsWith(directory.canonicalPath + File.separator) }
    }
}

/**
 * Streams with a running byte count instead of trusting the source's reported size:
 * a `content://` provider is free to under-report or not report at all.
 */
private fun java.io.InputStream.copyCapped(output: java.io.OutputStream) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        if (total > MAX_RECEIPT_BYTES) throw ReceiptError.TooLarge
        output.write(buffer, 0, read)
    }
}
