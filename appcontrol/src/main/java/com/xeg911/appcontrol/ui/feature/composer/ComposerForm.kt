package com.xeg911.appcontrol.ui.feature.composer

import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import com.xeg911.shared.data.model.notification.SupportedActionDef
import java.util.UUID

private fun newUid(): String = UUID.randomUUID().toString()

data class KeyValueEntry(
    val key: String = "",
    val value: String = "",
    val required: Boolean = false,
    val uid: String = newUid(),
)

data class ActionForm(
    val uid: String = newUid(),
    val id: String = "",
    val label: String = "",
    val action: String = "",
    val params: List<KeyValueEntry> = emptyList(),
    val icon: String = "",
    val semantic: String = "0",
    val allowReplies: Boolean = false,
)

data class MessageForm(
    val uid: String = newUid(),
    val text: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val timestamp: String = "",
)

data class MessagingForm(
    val senderName: String = "",
    val senderAvatar: String = "",
    val conversationTitle: String = "",
    val isGroupConversation: Boolean = false,
    val sourceBadge: String = "",
    val messages: List<MessageForm> = emptyList(),
)

data class ComposerForm(
    val notificationId: String = "",
    val title: String = "",
    val body: String = "",

    val style: String = NotificationStyleDef.DEFAULT.id,
    val priority: String = ComposerOptions.PRIORITY_DEFAULT,
    val visibility: String = ComposerOptions.VISIBILITY_DEFAULT,
    val icon: String = "",
    val largeIcon: String = "",
    val image: String = "",
    val color: String = "",
    val ticker: String = "",
    val subText: String = "",
    val summaryText: String = "",
    val bigText: String = "",

    val progress: String = "",
    val progressMax: String = "100",
    val indeterminate: Boolean = false,
    val messaging: MessagingForm = MessagingForm(),

    val autoCancel: Boolean = true,
    val ongoing: Boolean = false,
    val localOnly: Boolean = false,
    val silent: Boolean = false,
    val timestamp: String = "",
    val badge: String = "",

    val group: String = "",
    val groupSummary: Boolean = false,
    val sortKey: String = "",
    val channelId: String = ComposerOptions.CHANNEL_ID_DEFAULT,
    val channelName: String = ComposerOptions.CHANNEL_NAME_DEFAULT,

    val tapActionEnabled: Boolean = false,
    val tapAction: String = NotificationActionDef.OPEN_APP.id,
    val tapParams: List<KeyValueEntry> = emptyList(),

    val extras: List<KeyValueEntry> = emptyList(),
    val actions: List<ActionForm> = emptyList(),

    val expiresAt: String = "",
    val cancelAfterMs: String = "",
    val startMonitoring: Boolean = false,
    val cancelIds: String = "",
    val cancelOnly: Boolean = false,
)

object ComposerOptions {
    const val PRIORITY_DEFAULT = "DEFAULT"
    const val VISIBILITY_DEFAULT = "PRIVATE"
    const val CHANNEL_ID_DEFAULT = "fcm_default"
    const val CHANNEL_NAME_DEFAULT = "Remote Notification"
    const val MAX_ACTIONS = 3

    val priorities = listOf("LOW", "DEFAULT", "HIGH", "MAX")
    val visibilities = listOf("PUBLIC", "PRIVATE", "SECRET")

    fun generateNotificationId(): String =
        "ctrl_${System.currentTimeMillis().toString(36)}"
}

/**
 * Rebuilds the param list for [def], keeping values already typed for
 * matching keys. Required params come first and cannot be removed.
 */
fun List<KeyValueEntry>.reconcileWith(def: SupportedActionDef?): List<KeyValueEntry> {
    if (def == null) return filterNot { it.required }
    val existing = associateBy { it.key }
    val required = def.requiredParams.map { key ->
        KeyValueEntry(key = key, value = existing[key]?.value.orEmpty(), required = true)
    }
    val optional = def.optionalParams.map { key ->
        KeyValueEntry(key = key, value = existing[key]?.value.orEmpty())
    }
    val known = (def.requiredParams + def.optionalParams).toSet()
    val custom = filter { it.key !in known && !it.required }
    return required + optional + custom
}

fun List<KeyValueEntry>.toParamMap(): Map<String, String> =
    filter { it.key.isNotBlank() && it.value.isNotBlank() }
        .associate { it.key.trim() to it.value }

fun Map<String, String>.toEntries(def: SupportedActionDef? = null): List<KeyValueEntry> =
    map { (key, value) -> KeyValueEntry(key = key, value = value) }.reconcileWith(def)
