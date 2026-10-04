package com.xeg911.appcontrol.domain.usecase

import com.xeg911.appcontrol.domain.model.LocalFileInfo
import com.xeg911.appcontrol.domain.model.NotificationTarget
import com.xeg911.appcontrol.domain.model.PushFileOptions
import com.xeg911.appcontrol.domain.model.StorageEvent
import com.xeg911.appcontrol.domain.model.TransferProgress
import com.xeg911.appcontrol.domain.repository.FileStorageRepository
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationTapAction
import com.xeg911.shared.data.model.transfer.FileTransferLimits
import com.xeg911.shared.data.model.transfer.FileTransferMeta
import com.xeg911.shared.data.model.transfer.FileTransferParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

class PushFileUseCase @Inject constructor(
    private val storageRepository: FileStorageRepository,
    private val notificationRepository: NotificationRepository,
    private val sendNotification: SendNotificationUseCase,
) {
    operator fun invoke(
        deviceId: String,
        file: LocalFileInfo,
        options: PushFileOptions,
    ): Flow<TransferProgress> = flow {
        try {
            if (file.sizeBytes > FileTransferLimits.MAX_DOWNLOAD_BYTES) {
                emit(TransferProgress.Failed("AppClient can only download files up to 20 MB through the Bot API"))
                return@flow
            }
            val token = notificationRepository.getFcmToken(deviceId)
            if (token == null) {
                emit(TransferProgress.Failed("Device has no FCM token registered")); return@flow
            }
            var meta: FileTransferMeta? = null
            storageRepository.upload(deviceId, file, options.caption).collect { event ->
                when (event) {
                    is StorageEvent.Progress -> emit(
                        TransferProgress.Running(
                            event.bytesDone,
                            event.bytesTotal,
                            TransferProgress.Running.Phase.UPLOADING
                        )
                    )

                    is StorageEvent.Uploaded -> meta = event.meta
                    is StorageEvent.Saved -> Unit
                }
            }
            val uploaded = meta?.copy(transferId = newTransferId())
                ?: run { emit(TransferProgress.Failed("Upload finished without a file id")); return@flow }

            emit(
                TransferProgress.Running(
                    file.sizeBytes,
                    file.sizeBytes,
                    TransferProgress.Running.Phase.NOTIFYING
                )
            )
            val report = sendNotification(
                buildPayload(uploaded, options),
                listOf(NotificationTarget(token, deviceId)),
            )
            if (report.isFullSuccess) emit(TransferProgress.Done(uploaded))
            else emit(
                TransferProgress.Failed(
                    report.failures.values.firstOrNull() ?: "FCM send failed"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(TransferProgress.Failed(e.message ?: e.javaClass.simpleName))
        }
    }

    private fun buildPayload(
        meta: FileTransferMeta,
        options: PushFileOptions
    ): FcmNotificationPayload {
        val params = buildMap {
            put(FileTransferParams.TRANSFER_ID, meta.transferId)
            put(FileTransferParams.FILE_ID, meta.fileId)
            put(FileTransferParams.FILE_NAME, meta.fileName)
            put(FileTransferParams.MIME_TYPE, meta.mimeType)
            put(FileTransferParams.FILE_SIZE, meta.sizeBytes.toString())
            if (options.subDir.isNotBlank()) put(FileTransferParams.SUB_DIR, options.subDir.trim())
            put(FileTransferParams.AUTO_START, options.autoStart.toString())
        }
        return FcmNotificationPayload(
            notificationId = "ctrl_file_${meta.transferId}",
            title = options.title.ifBlank { "Incoming file" },
            body = options.body.ifBlank { "${meta.fileName} (${meta.sizeBytes / 1024} KB)" },
            priority = "HIGH",
            channelId = "fcm_files",
            channelName = "File Transfers",
            tapAction = NotificationTapAction(
                action = NotificationActionDef.DOWNLOAD_FILE.id,
                params = params
            ),
        )
    }

    private fun newTransferId(): String = "tr_${UUID.randomUUID().toString().take(8)}"
}
