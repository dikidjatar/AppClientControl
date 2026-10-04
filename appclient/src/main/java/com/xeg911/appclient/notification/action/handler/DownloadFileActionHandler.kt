package com.xeg911.appclient.notification.action.handler

import android.content.Context
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.transfer.FileTransferLauncher
import com.xeg911.appclient.transfer.FileTransferTracker
import com.xeg911.appclient.transfer.TransferRequestFactory
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import com.xeg911.shared.data.model.transfer.FileTransferParams
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadFileActionHandler @Inject constructor(
    private val launcher: FileTransferLauncher,
    private val tracker: FileTransferTracker,
) : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.DOWNLOAD_FILE.id

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val request =
            TransferRequestFactory.download(action.params, payload.notificationId).getOrElse {
                return ActionResult.Completed(
                    DeviceEventStatus.FAILED,
                    mapOf(
                        FileTransferParams.ERROR to (it.message ?: "Invalid DOWNLOAD_FILE params"),
                        FileTransferParams.ERROR_CODE to "INVALID_REQUEST",
                    ),
                )
            }
        // Already queued/running (e.g. autoStart): ignore the duplicate tap.
        if (!tracker.isTerminal(request.transferId)) return ActionResult.Silent
        launcher.start(request)
        return ActionResult.Delegated
    }
}
