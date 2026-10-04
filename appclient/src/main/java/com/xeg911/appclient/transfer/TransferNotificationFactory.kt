package com.xeg911.appclient.transfer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.R
import com.xeg911.appclient.transfer.model.TransferError
import com.xeg911.appclient.transfer.model.TransferRequest
import com.xeg911.appclient.transfer.model.TransferState
import com.xeg911.shared.data.model.transfer.FileTransferParams
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferNotificationFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun idle(): Notification {
        ensureChannel()
        return baseBuilder()
            .setContentTitle(context.getString(R.string.transfer_notification_idle))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()
    }

    fun progress(state: TransferState.Running, queuedCount: Int): Notification {
        ensureChannel()
        val request = state.request
        val title = when (request) {
            is TransferRequest.Download -> context.getString(
                R.string.transfer_notification_downloading,
                request.fileName
            )

            is TransferRequest.Upload -> context.getString(R.string.transfer_notification_uploading)
        }
        val detail = buildString {
            if (state.fileCount > 1) append(
                context.getString(
                    R.string.transfer_notification_file_of,
                    state.fileIndex + 1,
                    state.fileCount
                )
            ).append(" · ")
            append(state.percent?.let { "$it%" }
                ?: context.getString(R.string.transfer_notification_preparing))
            if (state.attempt > 1) append(" · ").append(
                context.getString(
                    R.string.transfer_notification_attempt,
                    state.attempt
                )
            )
            if (queuedCount > 0) append(" · ").append(
                context.getString(
                    R.string.transfer_notification_queued,
                    queuedCount
                )
            )
        }
        return baseBuilder()
            .setContentTitle(title)
            .setContentText(detail)
            .setProgress(100, state.percent ?: 0, state.percent == null)
            .setOngoing(true)
            .addAction(
                R.drawable.close_24px,
                context.getString(R.string.common_action_cancel),
                cancelIntent(request.transferId),
            )
            .build()
    }

    fun success(request: TransferRequest, results: List<Map<String, String>>): Notification {
        ensureChannel()
        val builder = baseBuilder()
            .setOngoing(false)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.verified_user_24px)
        when (request) {
            is TransferRequest.Download -> {
                val saved = results.firstOrNull()?.get(FileTransferParams.SAVED_URI)
                builder.setContentTitle(context.getString(R.string.transfer_notification_download_done))
                    .setContentText(
                        results.firstOrNull()?.get(FileTransferParams.SAVED_PATH)
                            ?: request.fileName
                    )
                saved?.let { builder.setContentIntent(openIntent(Uri.parse(it), request.mimeType)) }
            }

            is TransferRequest.Upload -> builder
                .setContentTitle(context.getString(R.string.transfer_notification_upload_done))
                .setContentText(
                    context.resources.getQuantityString(
                        R.plurals.transfer_notification_files_sent, results.size, results.size
                    )
                )
        }
        return builder.build()
    }

    fun failure(request: TransferRequest, error: TransferError): Notification {
        ensureChannel()
        val builder = baseBuilder()
            .setOngoing(false)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.warning_24px)
            .setContentTitle(context.getString(R.string.transfer_notification_failed))
            .setContentText(error.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(error.message))
        if (error.code != TransferError.Code.INVALID_REQUEST && error.code != TransferError.Code.NOT_CONFIGURED) {
            builder.addAction(
                R.drawable.refresh_24px,
                context.getString(R.string.common_action_retry),
                retryIntent(request),
            )
        }
        return builder.build()
    }

    fun resultId(transferId: String): Int = RESULT_ID_BASE + (transferId.hashCode() and 0x7FFF)

    private fun baseBuilder() = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.storage_24px)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    private fun cancelIntent(transferId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        transferId.hashCode(),
        TransferActionReceiver.cancelIntent(context, transferId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun retryIntent(request: TransferRequest): PendingIntent = PendingIntent.getBroadcast(
        context,
        request.transferId.hashCode() + 1,
        TransferActionReceiver.retryIntent(context, request),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openIntent(uri: Uri, mimeType: String): PendingIntent = PendingIntent.getActivity(
        context,
        uri.hashCode(),
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.transfer_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.transfer_channel_description) }
        )
    }

    companion object {
        const val CHANNEL_ID = "file_transfer_channel"
        const val SERVICE_NOTIFICATION_ID = 1004
        private const val RESULT_ID_BASE = 40_000
    }
}
