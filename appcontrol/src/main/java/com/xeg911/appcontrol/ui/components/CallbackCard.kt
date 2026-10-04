package com.xeg911.appcontrol.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType

private val cardCornerRadius = 12.dp

@Composable
fun DeviceEventStatus.toColor(): Color = when (this) {
    DeviceEventStatus.SUCCESS -> AppTheme.colors.callbackSuccess
    DeviceEventStatus.FAILED -> AppTheme.colors.callbackFailed
    DeviceEventStatus.DENIED -> AppTheme.colors.callbackDenied
    DeviceEventStatus.CANCELLED -> AppTheme.colors.callbackCancelled
    DeviceEventStatus.NOT_AVAILABLE -> AppTheme.colors.callbackNotAvailable
    DeviceEventStatus.INFO -> AppTheme.colors.callbackInfo
}

val DeviceEvent.headline: String
    get() = if (type == DeviceEventType.NOTIFICATION_ACTION) actionId.ifBlank { "—" }
    else type.name.lowercase().replace('_', ' ')

@Composable
fun CallbackCard(
    callback: DeviceEvent,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    showNotificationId: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasData = callback.data.isNotEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasData) { expanded = !expanded }
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CallbackInfo(
                    callback = callback,
                    showNotificationId = showNotificationId,
                    modifier = Modifier.weight(1f),
                )
                CallbackControls(
                    timestamp = callback.timestamp,
                    hasData = hasData,
                    expanded = expanded,
                    onDelete = onDelete,
                )
            }
            AnimatedVisibility(
                visible = expanded && hasData,
                enter = expandVertically() + fadeIn(tween(200)),
                exit = shrinkVertically() + fadeOut(tween(150)),
            ) {
                CallbackDataSection(data = callback.data)
            }
        }
    }
}

@Composable
private fun CallbackInfo(
    callback: DeviceEvent,
    showNotificationId: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = callback.headline,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            StatusChip(
                label = callback.status.name,
                color = callback.status.toColor(),
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (showNotificationId && callback.notificationId.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.callback_notif_id_prefix, callback.notificationId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CallbackControls(
    timestamp: Long,
    hasData: Boolean,
    expanded: Boolean,
    onDelete: (() -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = formatRelativeTime(timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasData) {
                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.unfold_less_24px else R.drawable.unfold_more_24px,
                    ),
                    contentDescription = stringResource(
                        if (expanded) R.string.callback_cd_collapse else R.string.callback_cd_expand,
                    ),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = stringResource(R.string.callback_cd_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CallbackDataSection(data: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(
                    bottomStart = cardCornerRadius,
                    bottomEnd = cardCornerRadius
                ),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.callback_data_header),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        data.forEach { (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.35f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.65f),
                )
            }
        }
    }
}
