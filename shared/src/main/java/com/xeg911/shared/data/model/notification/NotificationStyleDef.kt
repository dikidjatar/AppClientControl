package com.xeg911.shared.data.model.notification

/**
 * All notification display styles supported by this AppClient.
 * The server picks a style by its [id] string in the FCM payload.
 */
enum class NotificationStyleDef(
    val id: String,
    val description: String,
    val usedFields: List<String>
) {
    DEFAULT(
        id = "default",
        description = "Standard title + body notification",
        usedFields = listOf("title", "body", "icon", "largeIcon", "color")
    ),
    MINIMAL(
        id = "minimal",
        description = "Compact, no large icon, low visual weight",
        usedFields = listOf("title", "body")
    ),
    BIG_TEXT(
        id = "big_text",
        description = "Expandable notification with long text body",
        usedFields = listOf("title", "body", "bigText", "summaryText")
    ),
    IMAGE(
        id = "image",
        description = "Notification with a large inline image",
        usedFields = listOf("title", "body", "image", "summaryText")
    ),
    PROGRESS(
        id = "progress",
        description = "Notification with a determinate or indeterminate progress bar",
        usedFields = listOf("title", "body", "progress", "progressMax", "indeterminate")
    ),
    ONGOING(
        id = "ongoing",
        description = "Persistent notification the user cannot swipe away",
        usedFields = listOf("title", "body", "bigText")
    ),
    MESSAGING(
        id = "messaging",
        description = "WhatsApp-style MessagingStyle with avatar, badge, and conversation",
        usedFields = listOf(
            "title",          // optional override for the notification header
            "body",           // fallback when messagingStyle.messages is empty
            "messagingStyle"  // senderName, senderAvatar, sourceBadge,
            // conversationTitle, isGroupConversation, messages[]
        )
    );

    companion object {
        fun fromId(id: String): NotificationStyleDef =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
