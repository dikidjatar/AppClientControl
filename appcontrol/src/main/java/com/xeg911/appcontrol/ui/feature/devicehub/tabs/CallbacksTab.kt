package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.paging.PagedState
import com.xeg911.appcontrol.domain.model.CallbackRecord
import com.xeg911.appcontrol.ui.components.CallbackCard
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.paging.PagedListContent
import com.xeg911.appcontrol.ui.components.toColor
import com.xeg911.appcontrol.ui.theme.labelXSmall
import com.xeg911.shared.data.model.event.DeviceEventStatus

@Composable
fun CallbacksTab(
    state: PagedState<CallbackRecord>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onClearAll: () -> Unit,
    onDelete: (callbackId: String) -> Unit,
) {
    val callbacks = state.items
    val hasCallbacks = callbacks.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(
            title = stringResource(R.string.callback_section_title, state.countLabel),
            actionLabel = if (hasCallbacks) stringResource(R.string.callback_action_clear_all) else null,
            onAction = if (hasCallbacks) onClearAll else null,
        )

        if (hasCallbacks) {
            CallbackStatusSummary(callbacks = callbacks)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        PagedListContent(
            state = state,
            onLoadMore = onLoadMore,
            onRetry = onRefresh,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            empty = {
                EmptyState(
                    title = stringResource(R.string.callback_empty_title),
                    subtitle = stringResource(R.string.callback_empty_subtitle),
                )
            },
        ) {
            items(callbacks, key = { it.key }) { record ->
                CallbackCard(
                    callback = record.event,
                    onDelete = { onDelete(record.key) },
                )
            }
        }
    }
}

@Composable
private fun CallbackStatusSummary(callbacks: List<CallbackRecord>) {
    val countByStatus = remember(callbacks) { callbacks.groupingBy { it.event.status }.eachCount() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        DeviceEventStatus.entries.forEach { status ->
            val count = countByStatus[status] ?: 0
            if (count > 0) {
                CallbackStatusCount(
                    label = status.name,
                    count = count,
                    color = status.toColor(),
                )
            }
        }
    }
}

@Composable
private fun CallbackStatusCount(
    label: String,
    count: Int,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelXSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
