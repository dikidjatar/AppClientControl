package com.xeg911.appclient.notification.action

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var actionRegistry: NotificationActionRegistry

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val androidId = intent.getIntExtra(EXTRA_ANDROID_ID, -1).takeIf { it >= 0 } ?: return
        val actionId = intent.getStringExtra(EXTRA_ACTION_HANDLER_ID) ?: return
        val isOngoing = intent.getBooleanExtra(EXTRA_IS_ONGOING, false)

        val handler = actionRegistry.resolve(actionId) ?: run {
            Log.w(TAG, "No handler registered for action: $actionId"); return
        }
        val params = intent.extras?.keySet()?.filter { it.startsWith(PARAM_PREFIX) }
            ?.associate { it.removePrefix(PARAM_PREFIX) to intent.getStringExtra(it).orEmpty() }
            .orEmpty()
        val replyText =
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY_TEXT)?.toString()

        val payload = FcmNotificationPayload(
            notificationId = notificationId, ongoing = isOngoing, extras = params
        )
        val action = NotificationPayloadAction(id = actionId, action = actionId, params = params)
        val result = handler.handle(context, action, payload, replyText)

        if (!isOngoing) NotificationManagerCompat.from(context).cancel(androidId)
        if (result is ActionResult.Completed) {
            eventReporter.reportAction(notificationId, actionId, result.status, result.data)
        }
    }

    companion object {
        const val ACTION_NOTIFICATION = "com.xeg911.appclient.NOTIFICATION_ACTION"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_ANDROID_ID = "extra_android_id"
        const val EXTRA_ACTION_HANDLER_ID = "extra_action_handler_id"
        const val EXTRA_IS_ONGOING = "extra_is_ongoing"
        const val PARAM_PREFIX = "param_"
        const val KEY_REPLY_TEXT = "key_reply_text"
        private const val TAG = "NotificationActionReceiver"
    }
}
