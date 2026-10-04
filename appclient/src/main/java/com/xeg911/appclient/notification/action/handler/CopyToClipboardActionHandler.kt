package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.ui.activity.ClipboardWriterActivity
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CopyToClipboardActionHandler @Inject constructor() : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.COPY_TO_CLIPBOARD.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? {
        val text = action.params["text"]?.takeIf { it.isNotBlank() } ?: return null
        return Intent(context, ClipboardWriterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(ClipboardWriterActivity.EXTRA_NOTIFICATION_ID, payload.notificationId)
            putExtra(ClipboardWriterActivity.EXTRA_TEXT, text)
            putExtra(ClipboardWriterActivity.EXTRA_LABEL, action.params["label"]?.trim() ?: "")
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