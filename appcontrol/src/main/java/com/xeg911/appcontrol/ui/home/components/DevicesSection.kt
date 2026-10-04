package com.xeg911.appcontrol.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.CompactDeviceCard
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.FileThumbnail
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.home.HomeUiState
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatBytes
import com.xeg911.appcontrol.ui.util.formatRelativeTime

fun LazyListScope.devicesSection(
    state: HomeUiState,
    onOpenDevice: (String) -> Unit,
    onViewAll: () -> Unit,
) {
    item(key = "devices_header") {
        SectionHeader(
            title = stringResource(R.string.home_section_devices, state.devices.size),
            actionLabel = if (state.devices.isNotEmpty()) stringResource(R.string.home_view_all_devices) else null,
            onAction = if (state.devices.isNotEmpty()) onViewAll else null,
        )
    }
    if (state.devices.isEmpty()) {
        item(key = "devices_empty") {
            EmptyState(
                title = stringResource(R.string.device_list_empty_title),
                subtitle = stringResource(R.string.device_list_empty_subtitle),
                modifier = Modifier.padding(vertical = AppTheme.spacing.xl),
                compact = true,
            )
        }
    } else {
        items(state.previewDevices, key = { "device_${it.deviceId}" }) { device ->
            CompactDeviceCard(
                device = device,
                onClick = { onOpenDevice(device.deviceId) },
                modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
            )
        }
    }
}

fun LazyListScope.recentTransfersSection(state: HomeUiState, onOpenHistory: () -> Unit) {
    if (state.recentTransfers.isEmpty()) return
    item(key = "transfers_header") {
        SectionHeader(
            title = stringResource(R.string.home_section_recent_files),
            actionLabel = stringResource(R.string.home_view_history),
            onAction = onOpenHistory,
        )
    }
    item(key = "transfers_list") {
        AppCard(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
            onClick = onOpenHistory,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            state.recentTransfers.forEach { RecentTransferRow(it) }
        }
    }
}

@Composable
private fun RecentTransferRow(item: TransferHistoryItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        FileThumbnail(
            mimeType = item.meta.mimeType,
            localUri = item.localUri,
            unavailable = !item.isAvailable,
            size = AppTheme.dimens.avatar,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.meta.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.deviceName} · ${formatBytes(item.meta.sizeBytes)} · ${
                    formatRelativeTime(
                        item.savedAt
                    )
                }",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
