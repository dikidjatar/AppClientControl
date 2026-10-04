package com.xeg911.appcontrol.ui.feature.composer

import com.xeg911.shared.data.model.notification.ChatMessage
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.MessagingStylePayload
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import com.xeg911.shared.data.model.notification.NotificationTapAction
import com.xeg911.shared.data.model.notification.SupportedActionDef

private fun String.orNull(): String? = trim().takeIf { it.isNotEmpty() }
private fun String.toIntOrNullIfBlank(): Int? = trim().toIntOrNull()
private fun String.toLongOrNullIfBlank(): Long? = trim().toLongOrNull()

fun ComposerForm.toPayload(): FcmNotificationPayload {
    val styleDef = NotificationStyleDef.fromId(style)
    return FcmNotificationPayload(
        notificationId = notificationId.trim(),
        title = title,
        body = body,
        style = style,
        priority = priority,
        icon = icon.orNull(),
        largeIcon = largeIcon.orNull(),
        image = image.orNull().takeIf { styleDef == NotificationStyleDef.IMAGE },
        color = color.orNull(),
        ticker = ticker.orNull(),
        subText = subText.orNull(),
        summaryText = summaryText.orNull(),
        bigText = bigText.orNull(),
        progress = progress.toIntOrNullIfBlank()
            .takeIf { styleDef == NotificationStyleDef.PROGRESS },
        progressMax = progressMax.toIntOrNullIfBlank() ?: 100,
        indeterminate = indeterminate && styleDef == NotificationStyleDef.PROGRESS,
        messagingStyle = messaging.toPayload()
            .takeIf { styleDef == NotificationStyleDef.MESSAGING },
        autoCancel = autoCancel,
        ongoing = ongoing,
        localOnly = localOnly,
        silent = silent,
        timestamp = timestamp.toLongOrNullIfBlank(),
        visibility = visibility,
        badge = badge.toIntOrNullIfBlank(),
        group = group.orNull(),
        groupSummary = groupSummary,
        sortKey = sortKey.orNull(),
        channelId = channelId.orNull() ?: ComposerOptions.CHANNEL_ID_DEFAULT,
        channelName = channelName.orNull() ?: ComposerOptions.CHANNEL_NAME_DEFAULT,
        tapAction = if (tapActionEnabled) NotificationTapAction(
            tapAction,
            tapParams.toParamMap()
        ) else null,
        extras = extras.toParamMap(),
        actions = actions.map { it.toPayload() },
        expiresAt = expiresAt.toLongOrNullIfBlank(),
        cancelAfterMs = cancelAfterMs.toLongOrNullIfBlank(),
        startMonitoring = startMonitoring,
        cancelIds = cancelIds.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        cancelOnly = cancelOnly,
    )
}

private fun ActionForm.toPayload() = NotificationPayloadAction(
    id = id.orNull() ?: action.lowercase(),
    label = label,
    action = action,
    params = params.toParamMap(),
    icon = icon.orNull(),
    semanticAction = semantic.toIntOrNullIfBlank() ?: 0,
    allowGeneratedReplies = allowReplies,
)

private fun MessagingForm.toPayload() = MessagingStylePayload(
    senderName = senderName,
    senderAvatar = senderAvatar.orNull(),
    conversationTitle = conversationTitle.orNull(),
    isGroupConversation = isGroupConversation,
    sourceBadge = sourceBadge.orNull(),
    messages = messages.filter { it.text.isNotBlank() }.map {
        ChatMessage(
            text = it.text,
            timestamp = it.timestamp.toLongOrNullIfBlank(),
            senderName = it.senderName.orNull(),
            senderAvatar = it.senderAvatar.orNull(),
        )
    },
)

fun FcmNotificationPayload.toForm(actionDefs: List<SupportedActionDef>): ComposerForm {
    fun defOf(actionId: String) =
        actionDefs.firstOrNull { it.id.equals(actionId, ignoreCase = true) }

    val tap = tapAction
    return ComposerForm(
        notificationId = notificationId,
        title = title,
        body = body,
        style = style,
        priority = priority,
        visibility = visibility,
        icon = icon.orEmpty(),
        largeIcon = largeIcon.orEmpty(),
        image = image.orEmpty(),
        color = color.orEmpty(),
        ticker = ticker.orEmpty(),
        subText = subText.orEmpty(),
        summaryText = summaryText.orEmpty(),
        bigText = bigText.orEmpty(),
        progress = progress?.toString().orEmpty(),
        progressMax = progressMax.toString(),
        indeterminate = indeterminate,
        messaging = messagingStyle?.toForm() ?: MessagingForm(),
        autoCancel = autoCancel,
        ongoing = ongoing,
        localOnly = localOnly,
        silent = silent,
        timestamp = timestamp?.toString().orEmpty(),
        badge = badge?.toString().orEmpty(),
        group = group.orEmpty(),
        groupSummary = groupSummary,
        sortKey = sortKey.orEmpty(),
        channelId = channelId,
        channelName = channelName,
        tapActionEnabled = tap != null,
        tapAction = tap?.action ?: ComposerForm().tapAction,
        tapParams = tap?.let { it.params.toEntries(defOf(it.action)) } ?: emptyList(),
        extras = extras.toEntries(),
        actions = actions.map { it.toForm(defOf(it.action)) },
        expiresAt = expiresAt?.toString().orEmpty(),
        cancelAfterMs = cancelAfterMs?.toString().orEmpty(),
        startMonitoring = startMonitoring,
        cancelIds = cancelIds.joinToString(", "),
        cancelOnly = cancelOnly,
    )
}

private fun NotificationPayloadAction.toForm(def: SupportedActionDef?) = ActionForm(
    id = id,
    label = label,
    action = action,
    params = params.toEntries(def),
    icon = icon.orEmpty(),
    semantic = semanticAction.toString(),
    allowReplies = allowGeneratedReplies,
)

private fun MessagingStylePayload.toForm() = MessagingForm(
    senderName = senderName,
    senderAvatar = senderAvatar.orEmpty(),
    conversationTitle = conversationTitle.orEmpty(),
    isGroupConversation = isGroupConversation,
    sourceBadge = sourceBadge.orEmpty(),
    messages = messages.map {
        MessageForm(
            text = it.text,
            senderName = it.senderName.orEmpty(),
            senderAvatar = it.senderAvatar.orEmpty(),
            timestamp = it.timestamp?.toString().orEmpty(),
        )
    },
)
