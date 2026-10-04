package com.xeg911.appcontrol.domain.model

import android.net.Uri
import com.xeg911.shared.data.model.transfer.FileInputSource
import com.xeg911.shared.data.model.transfer.FileTransferLimits
import com.xeg911.shared.data.model.transfer.FileTransferMeta

data class LocalFileInfo(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
)

sealed interface TransferProgress {
    data object Idle : TransferProgress

    data class Running(val bytesDone: Long, val bytesTotal: Long, val phase: Phase) :
        TransferProgress {
        val percent: Int?
            get() = if (bytesTotal > 0) ((bytesDone * 100) / bytesTotal).toInt()
                .coerceIn(0, 100) else null

        enum class Phase { UPLOADING, DOWNLOADING, NOTIFYING }
    }

    data class Done(val meta: FileTransferMeta, val savedUri: Uri? = null) : TransferProgress

    data class Failed(val message: String) : TransferProgress
}

data class PushFileOptions(
    val subDir: String = "",
    val autoStart: Boolean = true,
    val title: String = "",
    val body: String = "",
    val caption: String = "",
)

data class PullFileRequest(
    val source: FileInputSource = FileInputSource.PHOTO_PICKER,
    val allowedMime: String = "*/*",
    val directUri: String = "",
    val allowMultiple: Boolean = false,
    val maxSizeBytes: Long = FileTransferLimits.MAX_UPLOAD_BYTES,
    val title: String = "",
    val body: String = "",
    val caption: String = "",
)

data class ReceivedFile(
    val callbackKey: String,
    val notificationId: String,
    val meta: FileTransferMeta,
    val source: String,
    val receivedAt: Long,
) {
    val canDownload: Boolean get() = meta.sizeBytes <= FileTransferLimits.MAX_DOWNLOAD_BYTES
}

sealed interface StorageEvent {
    data class Progress(val bytesDone: Long, val bytesTotal: Long) : StorageEvent
    data class Uploaded(val meta: FileTransferMeta) : StorageEvent
    data class Saved(val uri: Uri) : StorageEvent
}
