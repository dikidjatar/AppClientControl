package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.paging.PagedState
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.components.CountStatItem
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.SearchField
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.matchesAny
import com.xeg911.appcontrol.ui.components.paging.PagedListContent
import com.xeg911.appcontrol.ui.feature.devicehub.components.NotificationDetailSheet
import com.xeg911.appcontrol.ui.feature.devicehub.components.displaySource
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.shared.data.model.CapturedNotification
import java.util.Calendar

private const val ALL_SOURCES = "__all__"
private const val DAY_MS = 24 * 60 * 60_000L

@Composable
fun NotificationsTab(
    state: PagedState<CapturedNotification>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onClearAll: () -> Unit,
    onDelete: (CapturedNotification) -> Unit,
    onComposeForPackage: (packageName: String, label: String?) -> Unit,
    onMessage: (UiText) -> Unit,
) {
    val notifications = state.items
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSource by rememberSaveable { mutableStateOf(ALL_SOURCES) }
    var selected by remember { mutableStateOf<CapturedNotification?>(null) }

    val sources = remember(notifications) {
        notifications.groupingBy { it.displaySource }.eachCount()
            .entries.sortedByDescending { it.value }
    }
    val todayCount = remember(notifications) {
        val startOfDay = startOfToday()
        notifications.count { it.timestamp >= startOfDay }
    }

    val isFiltering = searchQuery.isNotBlank() || selectedSource != ALL_SOURCES
    val filtered = remember(notifications, searchQuery, selectedSource) {
        notifications.filter { n ->
            (selectedSource == ALL_SOURCES || n.displaySource == selectedSource) &&
                    searchQuery.matchesAny(n.title, n.text, n.source, n.packageName)
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { dayKey(it.timestamp) } }

    selected?.let { notification ->
        NotificationDetailSheet(
            notification = notification,
            onDismiss = { selected = null },
            onDelete = onDelete,
            onComposeForPackage = onComposeForPackage,
            onMessage = onMessage,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(
            title = stringResource(R.string.notif_section_title, state.countLabel),
            actionLabel = if (notifications.isNotEmpty()) stringResource(R.string.notif_action_clear_all) else null,
            onAction = if (notifications.isNotEmpty()) onClearAll else null,
        )

        if (notifications.isNotEmpty()) {
            StatsRow(total = notifications.size, today = todayCount, sources = sources.size)
        }

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = stringResource(R.string.notif_search_placeholder),
        )

        if (sources.size > 1) {
            SourceChipRow(
                sources = sources,
                selected = selectedSource,
                total = notifications.size,
                onSelect = { selectedSource = it },
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        if (isFiltering && filtered.isEmpty() && notifications.isNotEmpty()) {
            // Nothing among the loaded batches; let the user pull the next one explicitly.
            EmptyState(
                title = stringResource(
                    R.string.notif_search_empty_title,
                    searchQuery.ifBlank { selectedSource }),
                subtitle = stringResource(R.string.notif_search_empty_subtitle),
                actionLabel = if (state.endReached) null else stringResource(R.string.paged_load_more),
                onAction = if (state.endReached) null else onLoadMore,
            )
        } else {
            PagedListContent(
                state = state,
                onLoadMore = onLoadMore,
                onRetry = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                empty = {
                    EmptyState(
                        title = stringResource(R.string.notif_empty_title),
                        subtitle = stringResource(R.string.notif_empty_subtitle),
                    )
                },
            ) {
                grouped.forEach { (day, dayItems) ->
                    item(key = "day_$day", contentType = "day") { DayHeader(day = day) }
                    items(
                        dayItems,
                        key = { it.id },
                        contentType = { "notification" }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { selected = notification },
                            onDelete = { onDelete(notification) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(total: Int, today: Int, sources: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CountStatItem(
            value = total,
            label = stringResource(R.string.notif_stat_total),
            color = MaterialTheme.colorScheme.primary,
        )
        CountStatItem(
            value = today,
            label = stringResource(R.string.notif_stat_today),
            color = AppTheme.colors.permissionRuntime,
        )
        CountStatItem(
            value = sources,
            label = stringResource(R.string.notif_stat_sources),
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun SourceChipRow(
    sources: List<Map.Entry<String, Int>>,
    selected: String,
    total: Int,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        item(key = ALL_SOURCES) {
            SourceChip(
                label = stringResource(
                    R.string.perm_filter_chip_label,
                    stringResource(R.string.perm_filter_all),
                    total
                ),
                selected = selected == ALL_SOURCES,
                onClick = { onSelect(ALL_SOURCES) },
            )
        }
        items(sources, key = { it.key }) { (source, count) ->
            SourceChip(
                label = stringResource(R.string.perm_filter_chip_label, source, count),
                selected = selected == source,
                onClick = { onSelect(source) },
            )
        }
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun DayHeader(day: Long) {
    val startOfToday = startOfToday()
    val label = when {
        day >= startOfToday -> stringResource(R.string.notif_day_today)
        day >= startOfToday - DAY_MS -> stringResource(R.string.notif_day_yesterday)
        else -> java.text.SimpleDateFormat("EEEE, dd MMM", LocalLocale.current.platformLocale)
            .format(java.util.Date(day))
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun NotificationCard(
    notification: CapturedNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SourceAvatar(source = notification.displaySource)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notification.displaySource,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = formatRelativeTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (notification.title.isNotBlank()) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (notification.text.isNotBlank()) {
                    Text(
                        text = notification.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = stringResource(R.string.notif_cd_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SourceAvatar(source: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = source.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun startOfToday(): Long = dayKey(System.currentTimeMillis())

private fun dayKey(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
