package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAppActionHandler @Inject constructor() : NotificationActionHandler {
    override val actionId = NotificationActionDef.OPEN_APP.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? {
        val pm = context.packageManager
        val target = action.params[PARAM_PACKAGE_NAME]?.trim()?.takeIf { it.isNotBlank() }
        val intent = target?.let { pm.getLaunchIntentForPackage(it) }
            ?: pm.getLaunchIntentForPackage(context.packageName)
        return intent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

    private companion object {
        const val PARAM_PACKAGE_NAME = "packageName"
    }
}