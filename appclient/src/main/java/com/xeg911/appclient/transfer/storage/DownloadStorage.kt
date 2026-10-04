package com.xeg911.appclient.transfer.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.xeg911.appclient.transfer.model.TransferError
import com.xeg911.appclient.transfer.model.TransferException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun relativePath(subDir: String): String =
        listOf(Environment.DIRECTORY_DOWNLOADS, ROOT_DIR, subDir).filter { it.isNotBlank() }
            .joinToString("/")

    fun ensureFreeSpace(requiredBytes: Long) {
        val available = runCatching {
            StatFs(Environment.getExternalStorageDirectory().path).availableBytes
        }.getOrDefault(Long.MAX_VALUE)
        if (available < requiredBytes + MIN_FREE_BYTES) {
            throw TransferException(
                TransferError(
                    TransferError.Code.INSUFFICIENT_STORAGE,
                    "Not enough free storage for the download"
                )
            )
        }
    }

    fun createPending(fileName: String, mimeType: String, subDir: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath(subDir))
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw TransferException(
                TransferError(
                    TransferError.Code.STORAGE,
                    "MediaStore refused to create the download entry"
                )
            )
    }

    fun openOutput(uri: Uri): OutputStream =
        context.contentResolver.openOutputStream(uri, "w")
            ?: throw TransferException(
                TransferError(
                    TransferError.Code.STORAGE,
                    "Cannot open $uri for writing"
                )
            )

    fun publish(uri: Uri) {
        context.contentResolver.update(
            uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null
        )
    }

    fun discard(uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }

    fun displayPath(uri: Uri, fileName: String, subDir: String): String {
        val resolved = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Downloads.DISPLAY_NAME),
                null,
                null,
                null
            )
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        return "${relativePath(subDir)}/${resolved ?: fileName}"
    }

    private companion object {
        const val ROOT_DIR = "AppClient"
        const val MIN_FREE_BYTES = 32L * 1024 * 1024
    }
}
