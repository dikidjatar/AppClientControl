package com.xeg911.appclient.fcm.autoexec.handler

import android.util.Log
import com.xeg911.appclient.fcm.autoexec.ActionAutoExecuteHandler
import com.xeg911.appclient.transfer.FileTransferLauncher
import com.xeg911.appclient.transfer.TransferRequestFactory
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import com.xeg911.shared.data.model.transfer.FileTransferParams
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadFileAutoExecutor @Inject constructor(
    private val launcher: FileTransferLauncher,
) : ActionAutoExecuteHandler(
    actionDef = NotificationActionDef.DOWNLOAD_FILE,
    autoParam = FileTransferParams.AUTO_START,
    tapActionId = "tap_download",
) {
    override suspend fun executeAction(
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ) {
        TransferRequestFactory.download(action.params, payload.notificationId).fold(
            onSuccess = { launcher.start(it) },
            onFailure = {
                Log.w(TAG, "autoStart ignored: ${it.message}")
                launcher.reportInvalid(
                    payload.notificationId,
                    actionDef.id,
                    it.message ?: "Invalid DOWNLOAD_FILE params",
                )
            },
        )
    }

    private companion object {
        const val TAG = "DownloadFileAutoExecutor"
    }
}
