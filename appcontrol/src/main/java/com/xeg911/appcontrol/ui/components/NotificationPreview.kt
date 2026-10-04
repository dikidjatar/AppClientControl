package com.xeg911.appcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import com.xeg911.appcontrol.R
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationStyleDef

private const val MAX_ACTIONS = 3
private const val CLIENT_APP_NAME = "AppClient"

/**
 * Android-shade style mock-up of what [payload] will look like on the device.
 * [compact] trims long content for list cards; the composer uses the full variant.
 */
@Composable
fun NotificationPreview(
    payload: FcmNotificationPayload,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val accent = payload.color?.let { runCatching { Color(it.toColorInt()) }.getOrNull() }
        ?: MaterialTheme.colorScheme.primary
    val style = NotificationStyleDef.fromId(payload.style)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PreviewHeader(payload, accent)
            if (payload.cancelOnly) {
                Text(
                    text = stringResource(R.string.preview_cancel_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PreviewContent(payload, style, compact)
                }
                payload.largeIcon?.takeIf { it.isUrl() }?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
            if (style == NotificationStyleDef.IMAGE) PreviewImage(payload.image, compact)
            PreviewActions(payload, accent)
        }
    }
}

@Composable
private fun PreviewHeader(payload: FcmNotificationPayload, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.notification_add_24px),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = CLIENT_APP_NAME,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        payload.subText?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = "• $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Text(
            text = "• " + stringResource(R.string.preview_now),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (payload.ongoing) LabelChip(label = stringResource(R.string.composer_field_ongoing))
        if (payload.priority.equals("HIGH", true) || payload.priority.equals("MAX", true)) {
            LabelChip(label = payload.priority.uppercase())
        }
        Icon(
            painter = painterResource(R.drawable.unfold_more_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PreviewContent(
    payload: FcmNotificationPayload,
    style: NotificationStyleDef,
    compact: Boolean
) {
    val bodyLines = if (compact) 2 else 6
    val messaging = payload.messagingStyle
    val title = when {
        style == NotificationStyleDef.MESSAGING && messaging != null ->
            messaging.conversationTitle?.takeIf { it.isNotBlank() }
                ?: messaging.senderName.ifBlank { payload.title }

        else -> payload.title
    }
    if (title.isNotBlank()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    when (style) {
        NotificationStyleDef.BIG_TEXT -> BodyText(payload.bigText?.takeIf { it.isNotBlank() }
            ?: payload.body, bodyLines)

        NotificationStyleDef.MESSAGING -> {
            val messages = messaging?.messages.orEmpty().takeLast(if (compact) 2 else 4)
            if (messages.isEmpty()) BodyText(payload.body, bodyLines)
            messages.forEach { message ->
                val sender = message.senderName?.takeIf { it.isNotBlank() }
                    ?: messaging?.senderName.orEmpty()
                BodyText(if (sender.isBlank()) message.text else "$sender: ${message.text}", 2)
            }
        }

        NotificationStyleDef.PROGRESS -> {
            BodyText(payload.body, bodyLines)
            val max = payload.progressMax.coerceAtLeast(1)
            val progress = payload.progress
            if (payload.indeterminate || progress == null) {
                LinearProgressIndicator(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp))
            } else {
                LinearProgressIndicator(
                    progress = { (progress.toFloat() / max).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }

        else -> BodyText(payload.body, bodyLines)
    }
    if (!compact) {
        payload.summaryText?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BodyText(text: String, maxLines: Int) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PreviewImage(url: String?, compact: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    if (url.isUrl()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 72.dp else 140.dp)
                .clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 48.dp else 96.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.image_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewActions(payload: FcmNotificationPayload, accent: Color) {
    val actions = payload.actions.take(MAX_ACTIONS)
    if (actions.isEmpty() && payload.tapAction == null) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (actions.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                actions.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (action.action.equals(NotificationActionDef.REPLY.id, true)) {
                            Icon(
                                painter = painterResource(R.drawable.edit_24px),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = action.label.ifBlank { action.action }.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        payload.tapAction?.let { tap ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.open_in_new_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.preview_tap_action, tap.action),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

private fun String?.isUrl(): Boolean =
    this != null && (startsWith("http://", ignoreCase = true) || startsWith(
        "https://",
        ignoreCase = true
    ))
