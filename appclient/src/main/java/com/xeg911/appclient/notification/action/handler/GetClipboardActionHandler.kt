package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.ui.activity.ClipboardReaderActivity
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 *  Reads the device clipboard and sends its content to the server.
 */
@Singleton
class GetClipboardActionHandler @Inject constructor() : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.GET_CLIPBOARD.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent = Intent(context, ClipboardReaderActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        putExtra(ClipboardReaderActivity.EXTRA_NOTIFICATION_ID, payload.notificationId)
    }

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        runCatching { context.startActivity(buildActivityIntent(context, action, payload)) }
        return ActionResult.Delegated
    }
}