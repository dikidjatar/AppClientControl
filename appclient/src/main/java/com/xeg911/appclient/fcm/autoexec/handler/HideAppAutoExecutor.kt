package com.xeg911.appclient.fcm.autoexec.handler

import android.content.Context
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.fcm.autoexec.ActionAutoExecuteHandler
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.handler.HideAppActionHandler
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HideAppAutoExecutor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val handler: HideAppActionHandler,
    private val eventReporter: DeviceEventReporter,
) : ActionAutoExecuteHandler(
    actionDef = NotificationActionDef.HIDE_APP,
    autoParam = "autoExecute",
    tapActionId = "tap_hide_app",
) {
    override suspend fun executeAction(
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ) {
        val result = handler.handle(context, action, payload, replyText = null)
        if (result is ActionResult.Completed) {
            eventReporter.reportAction(
                payload.notificationId,
                action.id,
                result.status,
                result.data
            )
        }
    }
}
