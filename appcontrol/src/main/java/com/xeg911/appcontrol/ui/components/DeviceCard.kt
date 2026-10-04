package com.xeg911.appcontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.domain.model.HeartbeatHealth
import com.xeg911.appcontrol.domain.model.heartbeatHealth
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatLastSeen

@Composable
fun DeviceCard(
    device: DeviceSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        DeviceIdentityRow(device = device)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            MonitoringChip(device = device)
            if (device.status.appInForeground) {
                StatusChip(
                    label = stringResource(R.string.monitoring_value_foreground),
                    color = AppTheme.colors.online,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatLastSeen(device.lastSeen),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DeviceVitals(device = device)
        }
    }
}

/** Single-line variant for dashboards and pickers. */
@Composable
fun CompactDeviceCard(
    device: DeviceSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = AppTheme.spacing.lg,
            vertical = AppTheme.spacing.md,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            DeviceAvatar(online = device.isOnline)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.info.deviceName.ifBlank { device.info.model },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatLastSeen(device.lastSeen),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DeviceVitals(device = device)
            Icon(
                painter = painterResource(R.drawable.chevron_right_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeviceIdentityRow(device: DeviceSnapshot) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        DeviceAvatar(online = device.isOnline)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.info.deviceName.ifBlank { device.info.model },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${device.info.brand} ${device.info.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OnlineStatusBadge(isOnline = device.isOnline)
    }
}

@Composable
private fun DeviceAvatar(online: Boolean) {
    val tint =
        if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val container = if (online) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier
            .size(AppTheme.dimens.avatar)
            .background(container, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.mobile_24px),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(AppTheme.dimens.iconLg),
        )
    }
}

@Composable
fun MonitoringChip(device: DeviceSnapshot) {
    val heartbeat = device.status.heartbeatHealth()
    val healthy = device.status.monitoringRunning && heartbeat == HeartbeatHealth.FRESH
    StatusChip(
        label = stringResource(
            when {
                healthy -> R.string.monitoring_chip_running
                device.status.monitoringRunning -> R.string.monitoring_chip_stale
                else -> R.string.monitoring_chip_stopped
            }
        ),
        color = when {
            healthy -> AppTheme.colors.online
            device.status.monitoringRunning -> AppTheme.colors.warning
            else -> AppTheme.colors.error
        },
    )
}

@Composable
private fun DeviceVitals(device: DeviceSnapshot) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        device.connectivity?.let { TransportChip(transportType = it.transportType) }
        device.battery?.let { BatteryIndicator(level = it.level, charging = it.charging) }
    }
}
