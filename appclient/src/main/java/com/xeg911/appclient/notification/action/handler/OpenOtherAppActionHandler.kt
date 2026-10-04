package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import android.util.Log
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenOtherAppActionHandler @Inject constructor() : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.OPEN_OTHER_APP.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? {
        val packageName = action.params["packageName"]?.takeIf { it.isNotBlank() }
            ?: run { Log.w(TAG, "OPEN_OTHER_APP: missing 'packageName' param"); return null }

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.apply {
                // FLAG_ACTIVITY_NEW_TASK: required when starting from a non-Activity context.
                // FLAG_ACTIVITY_RESET_TASK_IF_NEEDED: if the app's task already exists but
                //   with a different affinity (e.g. was launched from another app), reset it
                //   to its default start state. This mirrors how the system launcher behaves.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }

        if (intent == null) {
            Log.w(
                TAG,
                "OPEN_OTHER_APP: app not installed or has no launcher, packageName=$packageName"
            )
        }
        return intent
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
        const val TAG = "OpenOtherAppActionHandler"
    }
}