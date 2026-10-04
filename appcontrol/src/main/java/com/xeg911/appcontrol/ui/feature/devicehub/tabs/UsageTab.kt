package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.paging.PagedState
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.paging.PagedListContent
import com.xeg911.appcontrol.ui.util.formatDuration
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.shared.data.model.usage.AppUsageEntry
import com.xeg911.shared.data.model.usage.UsageSummary

@Composable
fun UsageTab(
    summaryState: UiState<UsageSummary?>,
    state: PagedState<AppUsageEntry>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
) {
    val summary = (summaryState as? UiState.Content)?.data
    val total = summary?.totalForegroundMs ?: state.items.maxOfOrNull { it.foregroundMs } ?: 0L

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = stringResource(R.string.usage_section_title, state.countLabel))
        summary?.let { UsageSummaryCard(it) }
        PagedListContent(
            state = state,
            onLoadMore = onLoadMore,
            onRetry = onRefresh,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            empty = {
                EmptyState(
                    title = stringResource(R.string.usage_empty_title),
                    subtitle = stringResource(
                        if (summary?.permissionGranted == false) R.string.usage_empty_permission
                        else R.string.usage_empty_subtitle
                    ),
                )
            },
        ) {
            items(state.items, key = { it.packageName }) { entry ->
                UsageEntryCard(entry = entry, maxMs = total)
            }
        }
    }
}

@Composable
private fun UsageSummaryCard(summary: UsageSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryStat(
                formatDuration(summary.totalForegroundMs),
                stringResource(R.string.usage_total_screen_time)
            )
            SummaryStat(summary.appCount.toString(), stringResource(R.string.usage_apps_used))
            SummaryStat(
                summary.screenUnlockCount.toString(),
                stringResource(R.string.usage_unlocks)
            )
        }
        Text(
            text = stringResource(
                R.string.usage_window_hint,
                formatDuration(summary.windowEndAt - summary.windowStartAt),
                formatRelativeTime(summary.capturedAt),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UsageEntryCard(entry: AppUsageEntry, maxMs: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.appName.ifBlank { entry.packageName },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatDuration(entry.foregroundMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = {
                    if (maxMs > 0) (entry.foregroundMs.toFloat() / maxMs).coerceIn(
                        0f,
                        1f
                    ) else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            Text(
                text = stringResource(
                    R.string.usage_entry_meta,
                    entry.launchCount,
                    formatRelativeTime(entry.lastTimeUsed),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
