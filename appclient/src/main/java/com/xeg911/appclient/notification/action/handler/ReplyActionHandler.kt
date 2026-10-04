package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xeg911.appclient.R
import com.xeg911.appclient.notification.NotificationIdResolver
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.notification.channel.NotificationChannelManager
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReplyActionHandler @Inject constructor(
    private val idResolver: NotificationIdResolver,
    private val channelManager: NotificationChannelManager
) : NotificationActionHandler {

    override val actionId: String = NotificationActionDef.REPLY.id

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        if (replyText.isNullOrBlank()) {
            return ActionResult.Silent
        }

        Log.d(TAG, "Reply received for notificationId=${payload.notificationId}: $replyText")

        // Show a "Sending…" indicator on the same notification so the tray
        // doesn't collapse to an empty slot while the server processes the reply.
        showSendingState(context, payload)

        // TODO: forward replyText to the server via preferred back-channel
        //   e.g. write to Firebase RTDB:
        //     firebaseDatabase.reference
        //         .child("devices/{deviceId}/replies/{notificationId}")
        //         .setValue(mapOf("text" to replyText, "sentAt" to System.currentTimeMillis()))
        //   The server then dismisses or updates the notification via a follow up FCM.

        return ActionResult.Completed(
            status = DeviceEventStatus.SUCCESS,
            data = mapOf(
                "replied" to "true",
                "length" to "${replyText.length}"
            )
        )
    }

    @Suppress("MissingPermission")
    private fun showSendingState(context: Context, payload: FcmNotificationPayload) {
        val androidId = idResolver.resolve(payload.notificationId)
        val channelId = channelManager.ensureChannel(
            context = context,
            channelId = "fcm_reply",
            channelName = context.getString(R.string.notification_reply_channel_name)
        )
        val sendingNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.round_notifications_24)
            .setContentText(context.getString(R.string.notification_reply_sending))
            .setProgress(0, 0, true) // indeterminate spinner
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(androidId, sendingNotification)
        }.onFailure { Log.w(TAG, "Could not show sending-state notification", it) }
    }

    private companion object {
        const val TAG = "ReplyActionHandler"
    }
}
