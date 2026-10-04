package com.xeg911.appclient.notification.action.handler

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.xeg911.appclient.notification.NotificationIdResolver
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DismissActionHandler @Inject constructor(
    private val idResolver: NotificationIdResolver
) : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.DISMISS.id
    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        NotificationManagerCompat
            .from(context)
            .cancel(idResolver.resolve(payload.notificationId))
        return ActionResult.Completed(
            status = DeviceEventStatus.SUCCESS,
            data = mapOf("dismissed" to "true")
        )
    }
}