package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.CallbackRecord
import com.xeg911.appcontrol.domain.model.SentNotification
import com.xeg911.appcontrol.domain.model.SentStatus
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.CallbackCard
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.LabelChip
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.appcontrol.ui.util.standardFadeTransition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
private fun SentStatus.toColor(): Color = when (this) {
    SentStatus.SENT -> AppTheme.colors.callbackSuccess
    SentStatus.FAILED -> AppTheme.colors.callbackFailed
    SentStatus.CANCELLED -> AppTheme.colors.callbackCancelled
}

@Composable
fun OutboxTab(
    uiState: UiState<List<SentNotification>>,
    busyIds: Set<String>,
    callbacksFor: (notificationId: String) -> Flow<List<CallbackRecord>>,
    onCompose: () -> Unit,
    onEdit: (SentNotification) -> Unit,
    onResend: (SentNotification) -> Unit,
    onCancel: (SentNotification) -> Unit,
    onDelete: (SentNotification) -> Unit,
    onClearAll: () -> Unit,
) {
    UiStateContent(uiState = uiState, modifier = Modifier.fillMaxSize()) { items ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SectionHeader(
                    title = stringResource(R.string.outbox_section_title, items.size),
                    actionLabel = if (items.isNotEmpty()) stringResource(R.string.action_clear_all) else null,
                    onAction = if (items.isNotEmpty()) onClearAll else null,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                AnimatedContent(
                    targetState = items,
                    transitionSpec = { standardFadeTransition() },
                    modifier = Modifier.fillMaxSize(),
                    label = "outbox_list",
                ) { list ->
                    if (list.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.outbox_empty_title),
                            subtitle = stringResource(R.string.outbox_empty_subtitle),
                            icon = R.drawable.outbox_24px,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(list, key = { it.id }) { item ->
                                SentNotificationCard(
                                    item = item,
                                    busy = item.id in busyIds,
                                    callbacksFor = callbacksFor,
                                    onEdit = { onEdit(item) },
                                    onResend = { onResend(item) },
                                    onCancel = { onCancel(item) },
                                    onDelete = { onDelete(item) },
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onCompose,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.notification_add_24px),
                    contentDescription = stringResource(R.string.notif_compose_fab_cd),
                )
            }
        }
    }
}

@Composable
private fun SentNotificationCard(
    item: SentNotification,
    busy: Boolean,
    callbacksFor: (notificationId: String) -> Flow<List<CallbackRecord>>,
    onEdit: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    var showCallbacks by rememberSaveable(item.id) { mutableStateOf(false) }
    // One indexed listener per expanded card, collapsed cards cost nothing.
    val callbacks by remember(item.id, showCallbacks) {
        if (showCallbacks) callbacksFor(item.id) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabelChip(label = item.payload.style)
                    StatusChip(label = item.status.name, color = item.status.toColor())
                }
                Text(
                    text = formatRelativeTime(item.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            Text(
                text = item.payload.title.ifBlank { item.id },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (item.payload.body.isNotBlank()) {
                Text(
                    text = item.payload.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.outbox_meta, item.id, item.sendCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            item.lastError?.takeIf { it.isNotBlank() }?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showCallbacks = !showCallbacks }) {
                    Text(
                        text = if (showCallbacks) stringResource(
                            R.string.outbox_callbacks_count,
                            callbacks.size
                        )
                        else stringResource(R.string.outbox_callbacks_show),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Icon(
                        painter = painterResource(
                            if (showCallbacks) R.drawable.unfold_less_24px else R.drawable.unfold_more_24px
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    OutboxIconButton(R.drawable.edit_24px, R.string.outbox_cd_edit, onEdit)
                    OutboxIconButton(R.drawable.replay_24px, R.string.outbox_cd_resend, onResend)
                    if (item.status != SentStatus.CANCELLED) {
                        OutboxIconButton(R.drawable.block_24px, R.string.outbox_cd_cancel, onCancel)
                    }
                }
                OutboxIconButton(
                    iconRes = R.drawable.delete_24px,
                    cdRes = R.string.outbox_cd_delete,
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                    enabled = !busy,
                )
            }

            AnimatedVisibility(visible = showCallbacks && callbacks.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    callbacks.forEach { record ->
                        CallbackCard(
                            callback = record.event,
                            showNotificationId = false,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutboxIconButton(
    iconRes: Int,
    cdRes: Int,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(cdRes),
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}
