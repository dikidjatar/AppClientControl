package com.xeg911.appclient.transfer

import com.xeg911.appclient.transfer.model.TransferRequest
import com.xeg911.shared.data.model.transfer.FileInputSource
import com.xeg911.shared.data.model.transfer.FileTransferLimits
import com.xeg911.shared.data.model.transfer.FileTransferParams
import java.util.UUID

object TransferRequestFactory {

    fun download(
        params: Map<String, String>,
        notificationId: String
    ): Result<TransferRequest.Download> {
        val fileId = params[FileTransferParams.FILE_ID]?.trim().orEmpty()
        if (fileId.isBlank()) return Result.failure(IllegalArgumentException("fileId is required"))
        val fileName = sanitizeFileName(params[FileTransferParams.FILE_NAME])
        val size = params[FileTransferParams.FILE_SIZE]?.trim()?.toLongOrNull() ?: 0L
        if (size > FileTransferLimits.MAX_DOWNLOAD_BYTES) {
            return Result.failure(IllegalArgumentException("fileSize exceeds the 20 MB Bot API download limit"))
        }
        return Result.success(
            TransferRequest.Download(
                transferId = params[FileTransferParams.TRANSFER_ID]?.trim().orEmpty()
                    .ifBlank { newId() },
                notificationId = notificationId,
                fileId = fileId,
                fileName = fileName,
                mimeType = params[FileTransferParams.MIME_TYPE]?.trim().orEmpty()
                    .ifBlank { FileTransferParams.DEFAULT_MIME },
                expectedSize = size,
                sha256 = params[FileTransferParams.SHA256]?.trim()?.lowercase().orEmpty(),
                subDir = sanitizeSubDir(params[FileTransferParams.SUB_DIR]),
            )
        )
    }

    fun upload(
        params: Map<String, String>,
        notificationId: String
    ): Result<TransferRequest.Upload> {
        val source = FileInputSource.fromId(params[FileTransferParams.SOURCE])
            ?: return Result.failure(
                IllegalArgumentException(
                    "source must be one of ${FileInputSource.entries.joinToString { it.id }}"
                )
            )
        val uris = params[FileTransferParams.URI]?.split(',', '\n')
            ?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
        if (source == FileInputSource.DIRECT_URI && uris.isEmpty()) {
            return Result.failure(IllegalArgumentException("uri is required for DIRECT_URI"))
        }
        val maxSize = (params[FileTransferParams.MAX_SIZE]?.trim()?.toLongOrNull() ?: 0L)
            .takeIf { it > 0 }
            ?.coerceAtMost(FileTransferLimits.MAX_UPLOAD_BYTES)
            ?: FileTransferLimits.MAX_UPLOAD_BYTES
        return Result.success(
            TransferRequest.Upload(
                transferId = params[FileTransferParams.TRANSFER_ID]?.trim().orEmpty()
                    .ifBlank { newId() },
                notificationId = notificationId,
                source = source.id,
                uris = uris.take(FileTransferLimits.MAX_FILES_PER_REQUEST),
                allowedMime = params[FileTransferParams.ALLOWED_MIME]?.trim().orEmpty()
                    .ifBlank { "*/*" },
                maxSizeBytes = maxSize,
                allowMultiple = params[FileTransferParams.ALLOW_MULTIPLE].equals(
                    "true",
                    ignoreCase = true
                ),
                caption = params[FileTransferParams.CAPTION]?.trim().orEmpty(),
            )
        )
    }

    fun isAutoStart(params: Map<String, String>): Boolean =
        params[FileTransferParams.AUTO_START].equals("true", ignoreCase = true)

    fun newId(): String = "tr_${UUID.randomUUID().toString().take(8)}"

    fun sanitizeFileName(raw: String?): String {
        val name = raw?.trim()?.substringAfterLast('/')?.substringAfterLast('\\').orEmpty()
            .replace(INVALID_NAME_CHARS, "_")
            .trim('.', ' ')
        return name.ifBlank { "file_${System.currentTimeMillis()}" }.take(MAX_NAME_LENGTH)
    }

    fun sanitizeSubDir(raw: String?): String = raw?.split('/', '\\')
        ?.map { it.trim().replace(INVALID_NAME_CHARS, "_") }
        ?.filter { it.isNotBlank() && it != "." && it != ".." }
        ?.take(MAX_SUBDIR_DEPTH)
        ?.joinToString("/")
        .orEmpty()

    private val INVALID_NAME_CHARS = Regex("[\\x00-\\x1F\"*:<>?|]")
    private const val MAX_NAME_LENGTH = 120
    private const val MAX_SUBDIR_DEPTH = 3
}
