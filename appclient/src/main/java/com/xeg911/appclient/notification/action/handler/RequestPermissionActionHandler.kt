package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import android.util.Log
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionChecker
import com.xeg911.appclient.ui.activity.PermissionRequestActivity
import com.xeg911.shared.data.model.PermissionType
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestPermissionActionHandler @Inject constructor(
    private val permissionChecker: PermissionChecker,
) : NotificationActionHandler {
    override val actionId = NotificationActionDef.REQUEST_PERMISSION.id

    override fun buildActivityIntent(
        context: Context, action: NotificationPayloadAction, payload: FcmNotificationPayload
    ): Intent? {
        val permission =
            action.params["permission"]?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val knownSpecial = AppPermission.fromManifestName(permission)
            ?.takeIf { it.type == PermissionType.SPECIAL } != null

        if (!knownSpecial && !permissionChecker.isDeclaredInManifest(permission)) {
            Log.w(
                TAG,
                "REQUEST_PERMISSION: '$permission' not declared in AndroidManifest, ignoring"
            )
            return null
        }

        return Intent(context, PermissionRequestActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(PermissionRequestActivity.EXTRA_NOTIFICATION_ID, payload.notificationId)
            putExtra(PermissionRequestActivity.EXTRA_PERMISSION, permission)
            action.params["title"]?.let { putExtra(PermissionRequestActivity.EXTRA_TITLE, it) }
            action.params["rationale"]?.let {
                putExtra(PermissionRequestActivity.EXTRA_RATIONALE, it)
            }
        }
    }

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val intent = buildActivityIntent(context, action, payload)
            ?: return ActionResult.Silent   // unknown / not declared
        runCatching { context.startActivity(intent) }
        return ActionResult.Delegated       // PermissionRequestActivity sends the real callback
    }

    private companion object {
        const val TAG = "RequestPermissionHandler"
    }
}
