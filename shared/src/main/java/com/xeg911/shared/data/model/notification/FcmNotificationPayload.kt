package com.xeg911.shared.data.model.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FcmNotificationPayload(
    // Identity
    @SerialName("notificationId") val notificationId: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",

    // Appearance
    @SerialName("style") val style: String = "default",
    @SerialName("priority") val priority: String = "DEFAULT",   // LOW|DEFAULT|HIGH|MAX
    @SerialName("icon") val icon: String? = null,               // drawable name or URL
    @SerialName("largeIcon") val largeIcon: String? = null,     // drawable name or URL
    @SerialName("image") val image: String? = null,             // URL (image style)
    @SerialName("color") val color: String? = null,             // "#RRGGBB"
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("subText") val subText: String? = null,
    @SerialName("summaryText") val summaryText: String? = null,

    // Big-text style
    @SerialName("bigText") val bigText: String? = null,       // falls back to body

    // Progress style
    @SerialName("progress") val progress: Int? = null,        // 0–100, null = indeterminate
    @SerialName("progressMax") val progressMax: Int = 100,
    @SerialName("indeterminate") val indeterminate: Boolean = false,

    // Messaging style
    @SerialName("messagingStyle") val messagingStyle: MessagingStylePayload? = null,

    // Behavior
    @SerialName("autoCancel") val autoCancel: Boolean = true,
    @SerialName("ongoing") val ongoing: Boolean = false,
    @SerialName("localOnly") val localOnly: Boolean = false,
    @SerialName("silent") val silent: Boolean = false,
    @SerialName("timestamp") val timestamp: Long? = null,          // epoch ms, null = now

    // Visibility & badge
    @SerialName("visibility") val visibility: String = "PRIVATE",  // PUBLIC|PRIVATE|SECRET
    @SerialName("badge") val badge: Int? = null,

    // Grouping
    @SerialName("group") val group: String? = null,
    @SerialName("groupSummary") val groupSummary: Boolean = false,
    @SerialName("sortKey") val sortKey: String? = null,

    // Android channel
    @SerialName("channelId") val channelId: String = "fcm_default",
    @SerialName("channelName") val channelName: String = "Remote Notification",

    // Tap actions
    // What happens when the user taps the notification body
    @SerialName("tapAction") val tapAction: NotificationTapAction? = null,

    // Extra data
    @SerialName("extras") val extras: Map<String, String> = emptyMap(),

    // Action buttons (max 3 on Android)
    @SerialName("actions") val actions: List<NotificationPayloadAction> = emptyList(),

    // Lifecycle
    @SerialName("expiresAt") val expiresAt: Long? = null,          // epoch ms, skip if past
    @SerialName("cancelAfterMs") val cancelAfterMs: Long? = null,  // auto-dismiss delay ms

    @SerialName("startMonitoring") val startMonitoring: Boolean = false,

    // ID management
    /**
     * Cancel these server IDs BEFORE showing this notification.
     */
    @SerialName("cancelIds") val cancelIds: List<String> = emptyList(),

    /**
     * When true the client only cancels [notificationId] and [cancelIds]
     * without displaying anything.
     */
    @SerialName("cancelOnly") val cancelOnly: Boolean = false
)

fun FcmNotificationPayload.withTapActions(
    actionId: String = "tap_action"
): List<NotificationPayloadAction> {
    val mappedTapAction = tapAction?.let {
        NotificationPayloadAction(
            id = actionId,
            action = it.action,
            params = it.params
        )
    }
    return actions + listOfNotNull(mappedTapAction)
}