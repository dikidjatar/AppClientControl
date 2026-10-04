package com.xeg911.appclient.transfer.model

import com.xeg911.shared.data.model.notification.NotificationActionDef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class TransferRequest {
    abstract val transferId: String
    abstract val notificationId: String
    abstract val actionId: String

    @Serializable
    @SerialName("download")
    data class Download(
        override val transferId: String,
        override val notificationId: String = "",
        val fileId: String,
        val fileName: String,
        val mimeType: String,
        val expectedSize: Long = 0L,
        val sha256: String = "",
        val subDir: String = "",
    ) : TransferRequest() {
        override val actionId: String get() = NotificationActionDef.DOWNLOAD_FILE.id
    }

    @Serializable
    @SerialName("upload")
    data class Upload(
        override val transferId: String,
        override val notificationId: String = "",
        val source: String,
        val uris: List<String> = emptyList(),
        val allowedMime: String = "*/*",
        val maxSizeBytes: Long = 0L,
        val allowMultiple: Boolean = false,
        val caption: String = "",
    ) : TransferRequest() {
        override val actionId: String get() = NotificationActionDef.UPLOAD_FILE.id
    }
}

object TransferRequestCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(request: TransferRequest): String =
        json.encodeToString(TransferRequest.serializer(), request)

    fun decode(raw: String?): TransferRequest? =
        raw?.let {
            runCatching {
                json.decodeFromString(
                    TransferRequest.serializer(),
                    it
                )
            }.getOrNull()
        }
}
