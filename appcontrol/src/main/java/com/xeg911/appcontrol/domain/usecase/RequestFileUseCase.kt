package com.xeg911.appcontrol.domain.usecase

import com.xeg911.appcontrol.domain.model.NotificationTarget
import com.xeg911.appcontrol.domain.model.PullFileRequest
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationTapAction
import com.xeg911.shared.data.model.transfer.FileInputSource
import com.xeg911.shared.data.model.transfer.FileTransferParams
import java.util.UUID
import javax.inject.Inject

class RequestFileUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val sendNotification: SendNotificationUseCase,
) {
    suspend operator fun invoke(deviceId: String, request: PullFileRequest): Result<String> {
        if (request.source == FileInputSource.DIRECT_URI && request.directUri.isBlank()) {
            return Result.failure(IllegalArgumentException("A URI is required for DIRECT_URI"))
        }
        val token = notificationRepository.getFcmToken(deviceId)
            ?: return Result.failure(IllegalStateException("Device has no FCM token registered"))

        val transferId = "tr_${UUID.randomUUID().toString().take(8)}"
        val params = buildMap {
            put(FileTransferParams.TRANSFER_ID, transferId)
            put(FileTransferParams.SOURCE, request.source.id)
            put(FileTransferParams.ALLOWED_MIME, request.allowedMime.trim().ifBlank { "*/*" })
            put(FileTransferParams.MAX_SIZE, request.maxSizeBytes.toString())
            put(FileTransferParams.ALLOW_MULTIPLE, request.allowMultiple.toString())
            if (request.directUri.isNotBlank()) put(
                FileTransferParams.URI,
                request.directUri.trim()
            )
            if (request.caption.isNotBlank()) put(
                FileTransferParams.CAPTION,
                request.caption.trim()
            )
        }
        val payload = FcmNotificationPayload(
            notificationId = "ctrl_pull_$transferId",
            title = request.title.ifBlank { "File requested" },
            body = request.body.ifBlank { defaultBody(request.source) },
            priority = "HIGH",
            channelId = "fcm_files",
            channelName = "File Transfers",
            tapAction = NotificationTapAction(
                action = NotificationActionDef.UPLOAD_FILE.id,
                params = params,
            ),
        )
        val report = sendNotification(payload, listOf(NotificationTarget(token, deviceId)))
        return if (report.isFullSuccess) Result.success(transferId)
        else Result.failure(
            IllegalStateException(
                report.failures.values.firstOrNull() ?: "FCM send failed"
            )
        )
    }

    private fun defaultBody(source: FileInputSource) = when (source) {
        FileInputSource.PHOTO_PICKER -> "Tap to choose a photo to share"
        FileInputSource.MEDIA_PICKER -> "Tap to choose a photo or video to share"
        FileInputSource.DIRECT_URI -> "Tap to send the requested file"
        else -> "Tap to choose a file to share"
    }
}
