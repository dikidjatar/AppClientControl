package com.xeg911.appclient.transfer

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.transfer.model.TransferRequest
import com.xeg911.appclient.transfer.model.TransferRequestCodec
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.model.transfer.FileTransferParams
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val eventReporter: DeviceEventReporter,
) {
    fun start(request: TransferRequest): Boolean {
        val intent = Intent(context, FileTransferService::class.java)
            .setAction(FileTransferService.ACTION_START)
            .putExtra(FileTransferService.EXTRA_REQUEST, TransferRequestCodec.encode(request))
        return runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { error ->
                Log.w(TAG, "Cannot start FileTransferService", error)
                val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    error is ForegroundServiceStartNotAllowedException
                ) {
                    "Device refused to start the transfer in background; tap the notification to retry"
                } else error.message ?: error.javaClass.simpleName
                eventReporter.reportAction(
                    type = DeviceEventType.FILE_TRANSFER,
                    notificationId = request.notificationId,
                    actionId = request.actionId,
                    status = DeviceEventStatus.FAILED,
                    data = mapOf(
                        FileTransferParams.TRANSFER_ID to request.transferId,
                        FileTransferParams.ERROR to message,
                        FileTransferParams.ERROR_CODE to "SERVICE_START_REJECTED",
                    ),
                )
            }.isSuccess
    }

    fun reportInvalid(notificationId: String, actionId: String, reason: String) {
        eventReporter.reportAction(
            type = DeviceEventType.FILE_TRANSFER,
            notificationId = notificationId,
            actionId = actionId,
            status = DeviceEventStatus.FAILED,
            data = mapOf(
                FileTransferParams.ERROR to reason,
                FileTransferParams.ERROR_CODE to "INVALID_REQUEST",
            ),
        )
    }

    private companion object {
        const val TAG = "FileTransferLauncher"
    }
}
