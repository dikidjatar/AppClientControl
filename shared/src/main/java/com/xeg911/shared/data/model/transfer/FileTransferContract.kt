package com.xeg911.shared.data.model.transfer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How AppClient obtains the file for an UPLOAD_FILE action.
 */
enum class FileInputSource(val id: String, val description: String) {
    MEDIA_PICKER("MEDIA_PICKER", "Android Photo Picker: images and videos"),
    PHOTO_PICKER("PHOTO_PICKER", "Android Photo Picker: images only"),
    FILE_PICKER("FILE_PICKER", "System file chooser filtered by MIME type"),
    SAF("SAF", "Storage Access Framework document picker (persistable access)"),
    DIRECT_URI("DIRECT_URI", "No picker: uploads the content:// or file:// URI given in params");

    companion object {
        fun fromId(id: String?): FileInputSource? =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }
    }
}

/**
 * Telegram Bot API hard limits.
 */
object FileTransferLimits {
    const val MAX_UPLOAD_BYTES = 50L * 1024 * 1024
    const val MAX_DOWNLOAD_BYTES = 20L * 1024 * 1024
    const val MAX_FILES_PER_REQUEST = 10
}

/**
 * Param or event-data keys shared by DOWNLOAD_FILE and UPLOAD_FILE.
 */
object FileTransferParams {
    const val TRANSFER_ID = "transferId"
    const val FILE_ID = "fileId"
    const val FILE_NAME = "fileName"
    const val MIME_TYPE = "mimeType"
    const val FILE_SIZE = "fileSize"
    const val SHA256 = "sha256"
    const val SUB_DIR = "subDir"
    const val AUTO_START = "autoStart"

    const val SOURCE = "source"
    const val URI = "uri"
    const val ALLOWED_MIME = "allowedMime"
    const val MAX_SIZE = "maxSize"
    const val ALLOW_MULTIPLE = "allowMultiple"
    const val CAPTION = "caption"

    const val DOWNLOAD_URL = "downloadUrl"
    const val SAVED_URI = "savedUri"
    const val SAVED_PATH = "savedPath"
    const val FILE_INDEX = "fileIndex"
    const val FILE_COUNT = "fileCount"
    const val ERROR = "error"
    const val ERROR_CODE = "errorCode"
    const val RETRYABLE = "retryable"

    const val DEFAULT_MIME = "application/octet-stream"
}

/**
 * Metadata of a file stored in Telegram Storage, exchanged in params and event data.
 */
@Serializable
data class FileTransferMeta(
    @SerialName("transferId") val transferId: String = "",
    @SerialName("fileId") val fileId: String = "",
    @SerialName("fileName") val fileName: String = "",
    @SerialName("mimeType") val mimeType: String = FileTransferParams.DEFAULT_MIME,
    @SerialName("sizeBytes") val sizeBytes: Long = 0L,
    @SerialName("sha256") val sha256: String = "",
    @SerialName("downloadUrl") val downloadUrl: String = "",
) {
    fun toParams(): Map<String, String> = buildMap {
        put(FileTransferParams.TRANSFER_ID, transferId)
        put(FileTransferParams.FILE_ID, fileId)
        put(FileTransferParams.FILE_NAME, fileName)
        put(FileTransferParams.MIME_TYPE, mimeType)
        put(FileTransferParams.FILE_SIZE, sizeBytes.toString())
        if (sha256.isNotBlank()) put(FileTransferParams.SHA256, sha256)
        if (downloadUrl.isNotBlank()) put(FileTransferParams.DOWNLOAD_URL, downloadUrl)
    }

    companion object {
        fun fromParams(params: Map<String, String>): FileTransferMeta? {
            val fileId = params[FileTransferParams.FILE_ID]?.trim().orEmpty()
            if (fileId.isBlank()) return null
            return FileTransferMeta(
                transferId = params[FileTransferParams.TRANSFER_ID].orEmpty(),
                fileId = fileId,
                fileName = params[FileTransferParams.FILE_NAME]?.trim().orEmpty()
                    .ifBlank { "file" },
                mimeType = params[FileTransferParams.MIME_TYPE]?.trim().orEmpty()
                    .ifBlank { FileTransferParams.DEFAULT_MIME },
                sizeBytes = params[FileTransferParams.FILE_SIZE]?.trim()?.toLongOrNull() ?: 0L,
                sha256 = params[FileTransferParams.SHA256]?.trim().orEmpty(),
                downloadUrl = params[FileTransferParams.DOWNLOAD_URL]?.trim().orEmpty(),
            )
        }
    }
}
