package com.xeg911.appclient.transfer.source

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.xeg911.appclient.transfer.TransferRequestFactory
import com.xeg911.appclient.transfer.model.TransferError
import com.xeg911.appclient.transfer.model.TransferException
import com.xeg911.shared.data.model.transfer.FileTransferParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class LocalFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
)

/** Reads, validates and stages local content:// and file:// URIs. */
@Singleton
class UriFileResolver @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    fun parse(raw: String): Uri? {
        val uri = runCatching { raw.trim().toUri() }.getOrNull() ?: return null
        return when (uri.scheme?.lowercase()) {
            ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE -> uri
            null -> Uri.fromFile(File(raw.trim())) // bare absolute path
            else -> null
        }
    }

    fun describe(uri: Uri): LocalFile = when (uri.scheme?.lowercase()) {
        ContentResolver.SCHEME_FILE -> describeFile(uri)
        ContentResolver.SCHEME_CONTENT -> describeContent(uri)
        else -> throw TransferException(
            TransferError(
                TransferError.Code.INVALID_REQUEST,
                "Unsupported URI scheme: ${uri.scheme}"
            )
        )
    }

    fun open(uri: Uri): InputStream = when (uri.scheme?.lowercase()) {
        ContentResolver.SCHEME_FILE -> FileInputStream(File(uri.path.orEmpty()))
        else -> resolver.openInputStream(uri) ?: throw FileNotFoundException("Cannot open $uri")
    }

    /** Returns null when [file] passes the request limits, otherwise the reason. */
    fun validate(file: LocalFile, allowedMime: String, maxSizeBytes: Long): TransferError? {
        if (file.sizeBytes <= 0L) {
            return TransferError(
                TransferError.Code.FILE_NOT_FOUND,
                "${file.displayName} is empty or unreadable"
            )
        }
        if (file.sizeBytes > maxSizeBytes) {
            return TransferError(
                TransferError.Code.FILE_TOO_LARGE,
                "${file.displayName} is ${file.sizeBytes / 1024 / 1024} MB; limit is ${maxSizeBytes / 1024 / 1024} MB",
            )
        }
        if (!mimeMatches(file.mimeType, allowedMime)) {
            return TransferError(
                TransferError.Code.INVALID_REQUEST,
                "${file.displayName} (${file.mimeType}) does not match $allowedMime",
            )
        }
        return null
    }

    /** Runtime permissions still missing to read a file:// URI; empty for content:// URIs. */
    fun missingPermissions(uri: Uri): List<String> {
        if (uri.scheme?.lowercase() != ContentResolver.SCHEME_FILE) return emptyList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) return emptyList()
        val path = uri.path.orEmpty()
        val internal = context.filesDir.parent?.let { path.startsWith(it) } == true ||
                path.startsWith(context.cacheDir.path)
        if (internal) return emptyList()
        val candidates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return candidates.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    /** Copies [uri] into the app cache so the upload survives picker grant revocation. */
    @SuppressLint("UsableSpace")
    suspend fun stage(uri: Uri, transferId: String, file: LocalFile): File =
        withContext(Dispatchers.IO) {
            val dir = stagingDir(transferId).apply { mkdirs() }
            val target = File(dir, "${dir.listFiles()?.size ?: 0}_${file.displayName}")
            if (dir.usableSpace < file.sizeBytes + MIN_FREE_BYTES) {
                throw TransferException(
                    TransferError(
                        TransferError.Code.INSUFFICIENT_STORAGE,
                        "Not enough space to prepare the upload"
                    )
                )
            }
            runCatching {
                open(uri).use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }.onFailure { target.delete() }.getOrThrow()
            target
        }

    fun stagingDir(transferId: String): File = File(context.cacheDir, "$STAGING_ROOT/$transferId")

    fun clearStaging(transferId: String) {
        stagingDir(transferId).deleteRecursively()
    }

    private fun describeFile(uri: Uri): LocalFile {
        val file = File(uri.path.orEmpty())
        if (!file.isFile) throw FileNotFoundException("${file.path} does not exist")
        if (!file.canRead()) throw SecurityException("No permission to read ${file.path}")
        return LocalFile(
            uri = uri,
            displayName = TransferRequestFactory.sanitizeFileName(file.name),
            mimeType = guessMime(file.name),
            sizeBytes = file.length(),
        )
    }

    private fun describeContent(uri: Uri): LocalFile {
        var name: String? = null
        var size = -1L
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        if (size < 0) {
            size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }
        if (size < 0) {
            size = open(uri).use { input -> input.drainCount() }
        }
        val mime = resolver.getType(uri) ?: guessMime(name ?: uri.lastPathSegment.orEmpty())
        val displayName = TransferRequestFactory.sanitizeFileName(
            name ?: uri.lastPathSegment?.let { "$it${extensionFor(mime)}" }
        )
        return LocalFile(uri = uri, displayName = displayName, mimeType = mime, sizeBytes = size)
    }

    private fun InputStream.drainCount(): Long {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read == -1) return total
            total += read
        }
    }

    private fun guessMime(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: FileTransferParams.DEFAULT_MIME
    }

    private fun extensionFor(mime: String): String =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { ".$it" }.orEmpty()

    private fun mimeMatches(actual: String, allowed: String): Boolean {
        if (allowed.isBlank() || allowed == "*/*") return true
        return allowed.split(',').map { it.trim() }.any { pattern ->
            when {
                pattern == "*/*" -> true
                pattern.endsWith("/*") -> actual.startsWith(
                    pattern.removeSuffix("*"),
                    ignoreCase = true
                )

                else -> actual.equals(pattern, ignoreCase = true)
            }
        }
    }

    private companion object {
        const val STAGING_ROOT = "transfers"
        const val MIN_FREE_BYTES = 16L * 1024 * 1024
    }
}
