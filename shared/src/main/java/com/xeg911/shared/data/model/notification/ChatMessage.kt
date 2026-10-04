package com.xeg911.shared.data.model.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single message in a [MessagingStylePayload] conversation.
 *
 * [senderName] and [senderAvatar] are optional, when null they inherit
 * the parent [MessagingStylePayload.senderName] and [MessagingStylePayload.senderAvatar],
 * which is the common case for 1-on-1 chats where every message is from the same person.
 * Populate them per message for group conversations where different people send messages.
 */
@Serializable
data class ChatMessage(
    @SerialName("text") val text: String = "",
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("senderAvatar") val senderAvatar: String? = null
)
