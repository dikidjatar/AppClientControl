package com.xeg911.shared.data.model.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Extra fields for the **messaging** notification style.
 * Sent by the server inside [FcmNotificationPayload.messagingStyle].
 *
 * ## Minimal payload (single message, no history)
 * ```json
 * {
 *   "style": "messaging",
 *   "body": "Halo!",
 *   "messagingStyle": {
 *     "senderName": "Twilio",
 *     "senderAvatar": "https://cdn.example.com/avatar.png",
 *     "sourceBadge": "https://cdn.example.com/whatsapp_badge.png"
 *   }
 * }
 * ```
 *
 * ## Full payload (conversation history, group chat)
 * ```json
 * {
 *   "style": "messaging",
 *   "messagingStyle": {
 *     "senderName": "Alice",
 *     "senderAvatar": "https://...",
 *     "conversationTitle": "Project Team",
 *     "isGroupConversation": true,
 *     "sourceBadge": "https://cdn.example.com/whatsapp_badge.png",
 *     "messages": [
 *       { "text": "Anyone free for a call?", "timestamp": 1725336000000, "senderName": "Bob" },
 *       { "text": "Sure, give me 5 minutes",  "timestamp": 1725336060000, "senderName": "Alice" }
 *     ]
 *   }
 * }
 * ```
 */
@Serializable
data class MessagingStylePayload(
    /**
     * Display name of the primary sender.
     */
    @SerialName("senderName") val senderName: String = "",

    /**
     * Avatar image: drawable name, URL, or base64.
     */
    @SerialName("senderAvatar") val senderAvatar: String? = null,

    /**
     * Shown as the conversation header in group chats.
     * Ignored for 1-on-1 when [isGroupConversation] is false.
     */
    @SerialName("conversationTitle") val conversationTitle: String? = null,

    /**
     * Set true for group conversations so [conversationTitle] is always shown.
     */
    @SerialName("isGroupConversation") val isGroupConversation: Boolean = false,

    /**
     * Source app badge icon overlaid at the bottom right of [senderAvatar].
     * Accepts drawable name, URL, or base64.
     */
    @SerialName("sourceBadge") val sourceBadge: String? = null,

    /**
     * Ordered list of messages to display. The last message is shown in the
     * collapsed notification, expanded view shows all.
     */
    @SerialName("messages") val messages: List<ChatMessage> = emptyList()
)