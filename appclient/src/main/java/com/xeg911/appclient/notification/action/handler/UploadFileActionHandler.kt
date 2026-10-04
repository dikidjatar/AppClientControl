package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.transfer.TransferRequestFactory
import com.xeg911.appclient.transfer.model.TransferRequestCodec
import com.xeg911.appclient.transfer.picker.FilePickerActivity
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import com.xeg911.shared.data.model.transfer.FileTransferParams
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadFileActionHandler @Inject constructor() : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.UPLOAD_FILE.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? {
        val request =
            TransferRequestFactory.upload(action.params, payload.notificationId).getOrNull()
                ?: return null
        return Intent(context, FilePickerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(FilePickerActivity.EXTRA_REQUEST, TransferRequestCodec.encode(request))
        }
    }

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val intent = buildActivityIntent(context, action, payload)
            ?: return ActionResult.Completed(
                DeviceEventStatus.FAILED,
                mapOf(
                    FileTransferParams.ERROR to (TransferRequestFactory.upload(
                        action.params,
                        payload.notificationId
                    )
                        .exceptionOrNull()?.message ?: "Invalid UPLOAD_FILE params"),
                    FileTransferParams.ERROR_CODE to "INVALID_REQUEST",
                ),
            )
        runCatching { context.startActivity(intent) }
        return ActionResult.Delegated
    }
}
