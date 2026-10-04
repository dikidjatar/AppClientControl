package com.xeg911.appcontrol.data.template

import android.Manifest
import android.annotation.SuppressLint
import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import com.xeg911.shared.data.model.notification.NotificationTapAction

object DefaultTemplates {

    private const val ID_PREFIX = "tpl_default_"

    @SuppressLint("InlinedApi")
    fun all(): List<NotificationTemplate> = listOf(
        template(
            key = "open_app",
            name = "Open AppClient",
            payload = FcmNotificationPayload(
                title = "You have a new update",
                body = "Tap to open the app and see what's new.",
                priority = "HIGH",
                tapAction = NotificationTapAction(action = NotificationActionDef.OPEN_APP.id),
                actions = listOf(
                    action("open", "Open", NotificationActionDef.OPEN_APP),
                    action("dismiss", "Later", NotificationActionDef.DISMISS),
                ),
            ),
        ),
        template(
            key = "open_url",
            name = "Announcement with link",
            payload = FcmNotificationPayload(
                title = "Important announcement",
                body = "Read the full details on our website.",
                style = NotificationStyleDef.BIG_TEXT.id,
                bigText = "We have published important information. Tap \"Read more\" to open it in your browser.",
                tapAction = NotificationTapAction(
                    action = NotificationActionDef.OPEN_URL.id,
                    params = mapOf("url" to "https://example.com"),
                ),
                actions = listOf(
                    action(
                        "read_more",
                        "Read more",
                        NotificationActionDef.OPEN_URL,
                        mapOf("url" to "https://example.com")
                    ),
                ),
            ),
        ),
        template(
            key = "request_notifications",
            name = "Request notification permission",
            payload = FcmNotificationPayload(
                title = "Permission needed",
                body = "Allow notifications so the app can keep you informed.",
                priority = "HIGH",
                actions = listOf(
                    action(
                        id = "grant",
                        label = "Allow",
                        def = NotificationActionDef.REQUEST_PERMISSION,
                        params = mapOf(
                            "permission" to Manifest.permission.POST_NOTIFICATIONS,
                            "title" to "Permission needed",
                            "rationale" to "Notifications are required to receive updates.",
                        ),
                    ),
                    action("dismiss", "Not now", NotificationActionDef.DISMISS),
                ),
            ),
        ),
        template(
            key = "start_monitoring",
            name = "Start monitoring",
            payload = FcmNotificationPayload(
                title = "Background service",
                body = "Monitoring is being started on this device.",
                style = NotificationStyleDef.MINIMAL.id,
                priority = "LOW",
                silent = true,
                startMonitoring = true,
                cancelAfterMs = 10_000L,
            ),
        ),
        template(
            key = "location_sharing",
            name = "Start location sharing",
            payload = FcmNotificationPayload(
                title = "Share your location",
                body = "Tap to start sharing your location with AppControl.",
                priority = "HIGH",
                actions = listOf(
                    action(
                        id = "start_sharing",
                        label = "Start sharing",
                        def = NotificationActionDef.START_COMMAND,
                        params = mapOf("command" to "start_location_sharing"),
                    ),
                    action("dismiss", "Dismiss", NotificationActionDef.DISMISS),
                ),
            ),
        ),
        template(
            key = "quick_reply",
            name = "Quick reply",
            payload = FcmNotificationPayload(
                title = "Support",
                body = "How can we help you today?",
                priority = "HIGH",
                actions = listOf(
                    action(
                        "reply",
                        "Reply",
                        NotificationActionDef.REPLY,
                        mapOf("hint" to "Type your answer")
                    )
                        .copy(allowGeneratedReplies = true),
                    action(
                        "copy",
                        "Copy",
                        NotificationActionDef.COPY_TO_CLIPBOARD,
                        mapOf("text" to "support@example.com")
                    ),
                ),
            ),
        ),
        template(
            key = "cancel_only",
            name = "Cancel notification",
            payload = FcmNotificationPayload(
                title = "Cancel",
                body = "Removes the notification with the given id without showing anything.",
                silent = true,
                cancelOnly = true,
            ),
        ),
    )

    private fun template(key: String, name: String, payload: FcmNotificationPayload) =
        NotificationTemplate(
            id = ID_PREFIX + key,
            name = name,
            payload = payload,
            updatedAt = System.currentTimeMillis()
        )

    private fun action(
        id: String,
        label: String,
        def: NotificationActionDef,
        params: Map<String, String> = emptyMap(),
    ) = NotificationPayloadAction(
        id = id,
        label = label,
        action = def.id,
        params = params
    )
}
