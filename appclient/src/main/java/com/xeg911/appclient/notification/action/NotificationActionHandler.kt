package com.xeg911.appclient.notification.action

import android.content.Context
import android.content.Intent
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationPayloadAction

interface NotificationActionHandler {
    val actionId: String

    fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? = null

    fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String? = null
    ): ActionResult
}