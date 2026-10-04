package com.xeg911.appclient.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.xeg911.appclient.notification.action.NotificationActionReceiver
import com.xeg911.appclient.notification.action.NotificationActionRegistry
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTapActionResolver @Inject constructor(
    private val actionRegistry: NotificationActionRegistry
) {

    fun resolve(
        context: Context,
        payload: FcmNotificationPayload,
        androidId: Int,
    ): PendingIntent {
        val tapAction = payload.tapAction
        val actionId = tapAction?.action ?: NotificationActionDef.OPEN_APP.id
        val params = tapAction?.params ?: emptyMap()

        // Build a minimal NotificationPayloadAction for the handler query.
        val payloadAction = NotificationPayloadAction(action = actionId, params = params)

        // If the handler needs to launch an Activity, it provides the Intent here.
        // We wrap it in getActivity() so Android preserves the BAL exemption that
        // comes from the user explicitly tapping the notification.
        //
        // If we used getBroadcast() and then called startActivity() from inside the
        // BroadcastReceiver, Android 12+ would block it (originatingPendingIntent: null,
        // see goo.gle/android-bal).
        val activityIntent = actionRegistry
            .resolve(actionId)
            ?.buildActivityIntent(context, payloadAction, payload)

        if (activityIntent != null) {
            return PendingIntent.getActivity(
                context,
                androidId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Non activity actions, route through the receiver.
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = NotificationActionReceiver.ACTION_NOTIFICATION
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, payload.notificationId)
            putExtra(NotificationActionReceiver.EXTRA_ANDROID_ID, androidId)
            putExtra(NotificationActionReceiver.EXTRA_ACTION_HANDLER_ID, actionId)
            params.forEach { (k, v) ->
                putExtra("${NotificationActionReceiver.PARAM_PREFIX}$k", v)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            androidId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}