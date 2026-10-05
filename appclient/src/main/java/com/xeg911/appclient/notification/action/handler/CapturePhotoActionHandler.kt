package com.xeg911.appclient.notification.action.handler

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.xeg911.appclient.camera.CameraCaptureService
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapturePhotoActionHandler @Inject constructor() : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.CAPTURE_PHOTO.id

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ActionResult.Completed(
                DeviceEventStatus.FAILED,
                mapOf("error" to "CAMERA permission denied")
            )
        }

        val intent = Intent(context, CameraCaptureService::class.java).apply {
            putExtra(CameraCaptureService.EXTRA_NOTIFICATION_ID, payload.notificationId)
            putExtra(CameraCaptureService.EXTRA_LENS_FACING, action.params["lensFacing"])
        }

        ContextCompat.startForegroundService(context, intent)
        return ActionResult.Delegated
    }
}
