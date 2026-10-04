package com.xeg911.appclient.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.toColorInt
import com.xeg911.appclient.R
import com.xeg911.appclient.notification.action.NotificationActionReceiver
import com.xeg911.appclient.notification.action.NotificationActionRegistry
import com.xeg911.appclient.notification.channel.NotificationChannelManager
import com.xeg911.appclient.notification.icon.NotificationIconResolver
import com.xeg911.appclient.notification.style.NotificationStyleRegistry
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationDispatcher @Inject constructor(
    private val styleRegistry: NotificationStyleRegistry,
    private val actionRegistry: NotificationActionRegistry,
    private val idResolver: NotificationIdResolver,
    private val iconResolver: NotificationIconResolver,
    private val channelManager: NotificationChannelManager,
    private val tapActionResolver: NotificationTapActionResolver
) {
    // Scope for cancelAfterMs auto dismiss jobs, survives across FCM deliveries.
    private val dispatchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @SuppressLint("MissingPermission")
    suspend fun dispatch(context: Context, payload: FcmNotificationPayload) {
        if (!isValid(payload)) {
            Log.d(TAG, "Payload invalid or expired. Skipping: ${payload.notificationId}")
            return
        }

        val androidId = idResolver.resolve(payload.notificationId)

        // Pre cancel any IDs the server flagged
        payload.cancelIds.forEach { serverId ->
            NotificationManagerCompat.from(context).cancel(idResolver.resolve(serverId))
        }

        if (payload.cancelOnly) {
            NotificationManagerCompat.from(context).cancel(androidId)
            Log.d(TAG, "Cancel-only payload handled: ${payload.notificationId}")
            return
        }

        // Ensure channel exists
        val channelId = channelManager.ensureChannel(
            context = context,
            channelId = payload.channelId,
            channelName = payload.channelName,
            priority = payload.priority
        )

        // Base builder (suspend: may download large/small icon bitmaps)
        val builder = buildBaseBuilder(context, channelId, payload, androidId)

        // Style renderer (suspend: image style may download bitmap)
        styleRegistry.resolve(payload.style).render(context, payload, builder)

        // Action buttons (max 3, Android platform limit)
        addActionButtons(context, payload, builder, androidId)

        // Post notification
        NotificationManagerCompat.from(context).notify(androidId, builder.build())
        Log.d(TAG, "Notification dispatched: id=${payload.notificationId} style=${payload.style}")

        // Schedule auto dismiss if requested
        payload.cancelAfterMs?.let { delayMs ->
            scheduleCancelAfter(context, androidId, delayMs)
        }
    }

    private fun isValid(payload: FcmNotificationPayload): Boolean {
        if (payload.notificationId.isBlank()) {
            Log.w(TAG, "notificationId is blank"); return false
        }
        payload.expiresAt?.let { expiry ->
            if (System.currentTimeMillis() > expiry) {
                Log.w(TAG, "Payload expired (expiresAt=$expiry)"); return false
            }
        }
        return true
    }

    private suspend fun buildBaseBuilder(
        context: Context,
        channelId: String,
        payload: FcmNotificationPayload,
        androidId: Int
    ): NotificationCompat.Builder {
        val smallIcon: IconCompat = iconResolver.resolveSmallIcon(payload.icon)
        val largeIcon = iconResolver.resolveLargeIcon(payload.largeIcon)
        val colorInt = payload.color?.let { runCatching { it.toColorInt() }.getOrNull() }
        val contentIntent: PendingIntent = tapActionResolver.resolve(context, payload, androidId)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setPriority(mapPriority(payload.priority))
            .setContentIntent(contentIntent)
            .setAutoCancel(payload.autoCancel)
            .setOngoing(payload.ongoing)
            .setLocalOnly(payload.localOnly)
            .setSilent(payload.silent)
            .setVisibility(mapVisibility(payload.visibility))
            .setShowWhen(true)
            .setWhen(payload.timestamp ?: System.currentTimeMillis())
            .setOnlyAlertOnce(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .apply {
                payload.ticker?.let { setTicker(it) }
                payload.subText?.let { setSubText(it) }
                payload.badge?.let { setNumber(it) }
                payload.sortKey?.let { setSortKey(it) }
                payload.group?.let {
                    setGroup(it)
                    setGroupSummary(payload.groupSummary)
                }
                largeIcon?.let { setLargeIcon(it) }
                colorInt?.let { setColor(it) }
            }
    }

    private fun addActionButtons(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder,
        androidId: Int
    ) {
        payload.actions.take(MAX_ACTIONS).forEachIndexed { index, actionDef ->
            val handler = actionRegistry.resolve(actionDef.action)
            if (handler == null) {
                Log.w(TAG, "Skipping unknown action: ${actionDef.action}"); return@forEachIndexed
            }
            val pendingIntent =
                buildActionPendingIntent(context, payload, actionDef, androidId, index)
            val iconResId = resolveActionIcon(context, actionDef)

            if (actionDef.action == NotificationActionDef.REPLY.id) {
                // Inline reply: attach a RemoteInput so the user can type directly
                // in the notification shade without opening the app.
                val hint = actionDef.params["hint"] ?: actionDef.label
                val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY_TEXT)
                    .setLabel(hint)
                    .build()
                builder.addAction(
                    NotificationCompat.Action.Builder(iconResId, actionDef.label, pendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(actionDef.allowGeneratedReplies)
                        .setSemanticAction(actionDef.semanticAction)
                        .build()
                )
            } else {
                builder.addAction(
                    NotificationCompat.Action.Builder(iconResId, actionDef.label, pendingIntent)
                        .setSemanticAction(actionDef.semanticAction)
                        .build()
                )
            }
        }
    }

    private fun buildActionPendingIntent(
        context: Context,
        payload: FcmNotificationPayload,
        action: NotificationPayloadAction,
        androidId: Int,
        index: Int
    ): PendingIntent {
        // Use a unique request code per (notification, action slot) to prevent
        // PendingIntent collisions when multiple notifications with actions are live.
        val requestCode = androidId * 100 + index
        val isReply = action.action == NotificationActionDef.REPLY.id

        // If the handler needs to launch an Activity, use getActivity() so Android
        // preserves the BAL exemption from the user's notification tap.
        // Calling startActivity() from inside a BroadcastReceiver triggered by a
        // notification is blocked on Android 12+ (notification trampoline restriction).
        val handler = actionRegistry.resolve(action.action)
        val activityIntent = handler?.buildActivityIntent(context, action, payload)

        if (activityIntent != null) {
            return PendingIntent.getActivity(
                context,
                requestCode,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Non activity actions, broadcast receiver as before.
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = NotificationActionReceiver.ACTION_NOTIFICATION
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, payload.notificationId)
            putExtra(NotificationActionReceiver.EXTRA_ANDROID_ID, androidId)
            putExtra(NotificationActionReceiver.EXTRA_ACTION_HANDLER_ID, action.action)
            putExtra(NotificationActionReceiver.EXTRA_IS_ONGOING, payload.ongoing)
            action.params.forEach { (k, v) ->
                putExtra("${NotificationActionReceiver.PARAM_PREFIX}$k", v)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (isReply) mutableFlag() else immutableFlag()
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    @SuppressLint("MissingPermission")
    private fun scheduleCancelAfter(context: Context, androidId: Int, delayMs: Long) {
        if (delayMs <= 0) return
        dispatchScope.launch {
            delay(delayMs)
            runCatching {
                NotificationManagerCompat.from(context).cancel(androidId)
                Log.d(TAG, "Auto-cancelled notification androidId=$androidId after ${delayMs}ms")
            }.onFailure { Log.w(TAG, "Failed to auto-cancel androidId=$androidId", it) }
        }
    }

    private fun resolveActionIcon(context: Context, action: NotificationPayloadAction): Int {
        if (action.icon != null) {
            val resId =
                context.resources.getIdentifier(action.icon, "drawable", context.packageName)
            if (resId != 0) return resId
        }
        return R.drawable.round_notifications_24
    }

    private fun mapPriority(p: String): Int = when (p.uppercase()) {
        "MAX" -> NotificationCompat.PRIORITY_MAX
        "HIGH" -> NotificationCompat.PRIORITY_HIGH
        "LOW" -> NotificationCompat.PRIORITY_LOW
        "MIN" -> NotificationCompat.PRIORITY_MIN
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

    private fun mapVisibility(v: String): Int = when (v.uppercase()) {
        "PUBLIC" -> NotificationCompat.VISIBILITY_PUBLIC
        "SECRET" -> NotificationCompat.VISIBILITY_SECRET
        else -> NotificationCompat.VISIBILITY_PRIVATE
    }

    private fun immutableFlag() = PendingIntent.FLAG_IMMUTABLE

    private fun mutableFlag() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    private companion object {
        const val TAG = "NotificationDispatcher"
        const val MAX_ACTIONS = 3   // Android platform limit for notification action buttons
    }
}
