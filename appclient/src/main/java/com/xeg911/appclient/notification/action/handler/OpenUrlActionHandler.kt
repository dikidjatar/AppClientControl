package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenUrlActionHandler @Inject constructor() : NotificationActionHandler {
    override val actionId = NotificationActionDef.OPEN_URL.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? {
        val url = action.params["url"]?.takeIf { it.isNotBlank() } ?: return null
        return Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val intent = buildActivityIntent(context, action, payload)
            ?: return ActionResult.Silent
        runCatching { context.startActivity(intent) }
        return ActionResult.Delegated
    }
}