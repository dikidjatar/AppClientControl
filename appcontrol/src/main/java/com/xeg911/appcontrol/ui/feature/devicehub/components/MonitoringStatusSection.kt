package com.xeg911.appcontrol.ui.feature.devicehub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.HEARTBEAT_INTERVAL_MS
import com.xeg911.appcontrol.domain.model.HeartbeatHealth
import com.xeg911.appcontrol.domain.model.heartbeatHealth
import com.xeg911.appcontrol.domain.model.isMonitoringHealthy
import com.xeg911.appcontrol.ui.components.InfoRow
import com.xeg911.appcontrol.ui.components.InfoSection
import com.xeg911.appcontrol.ui.components.StatusIndicatorRow
import com.xeg911.appcontrol.ui.components.transportLabel
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatDuration
import com.xeg911.appcontrol.ui.util.formatRelativeWithAbsolute
import com.xeg911.shared.data.model.ConnectivityInfo
import com.xeg911.shared.data.model.DeviceStatus

@Composable
fun MonitoringStatusSection(
    status: DeviceStatus,
    connectivity: ConnectivityInfo?,
    isPinging: Boolean,
    isStartingMonitoring: Boolean,
    onPing: () -> Unit,
    onStartMonitoring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heartbeat = status.heartbeatHealth()
    val heartbeatColor = when (heartbeat) {
        HeartbeatHealth.FRESH -> AppTheme.colors.online
        HeartbeatHealth.STALE -> AppTheme.colors.callbackDenied
        HeartbeatHealth.NONE -> AppTheme.colors.offline
    }
    val monitoringHealthy = status.isMonitoringHealthy()

    InfoSection(title = stringResource(R.string.monitoring_section_title), modifier = modifier) {
        StatusIndicatorRow(
            label = stringResource(R.string.monitoring_label_presence),
            statusLabel = stringResource(if (status.online) R.string.status_online else R.string.status_offline),
            color = if (status.online) AppTheme.colors.online else AppTheme.colors.offline,
            detail = stringResource(
                R.string.monitoring_detail_last_seen,
                formatRelativeWithAbsolute(status.lastSeen)
            ),
        )
        StatusIndicatorRow(
            label = stringResource(R.string.monitoring_label_foreground),
            statusLabel = stringResource(
                if (status.appInForeground) R.string.monitoring_value_foreground
                else R.string.monitoring_value_background
            ),
            color = if (status.appInForeground) AppTheme.colors.online else AppTheme.colors.offline,
            detail = if (status.appInForeground) stringResource(
                R.string.monitoring_detail_opened,
                formatRelativeWithAbsolute(status.lastAppOpenedAt)
            ) else stringResource(
                R.string.monitoring_detail_closed,
                formatRelativeWithAbsolute(status.lastAppClosedAt)
            ),
        )
        StatusIndicatorRow(
            label = stringResource(R.string.monitoring_label_service),
            statusLabel = stringResource(
                when {
                    monitoringHealthy -> R.string.monitoring_value_running
                    status.monitoringRunning -> R.string.monitoring_value_stale
                    else -> R.string.monitoring_value_stopped
                }
            ),
            color = when {
                monitoringHealthy -> AppTheme.colors.online
                status.monitoringRunning -> AppTheme.colors.callbackDenied
                else -> AppTheme.colors.error
            },
            detail = stringResource(
                R.string.monitoring_detail_heartbeat,
                formatRelativeWithAbsolute(status.lastMonitoringHeartbeatAt)
            ),
        )
        StatusIndicatorRow(
            label = stringResource(R.string.monitoring_label_heartbeat),
            statusLabel = stringResource(
                when (heartbeat) {
                    HeartbeatHealth.FRESH -> R.string.monitoring_heartbeat_fresh
                    HeartbeatHealth.STALE -> R.string.monitoring_heartbeat_stale
                    HeartbeatHealth.NONE -> R.string.monitoring_heartbeat_none
                }
            ),
            color = heartbeatColor,
            detail = stringResource(
                R.string.monitoring_detail_heartbeat_interval,
                formatDuration(HEARTBEAT_INTERVAL_MS)
            ),
        )

        if (connectivity != null) {
            val transportOnline = connectivity.transportType != "NONE"
            StatusIndicatorRow(
                label = stringResource(R.string.monitoring_label_network),
                statusLabel = transportLabel(connectivity.transportType),
                color = if (transportOnline) AppTheme.colors.online else AppTheme.colors.offline,
                detail = connectivity.ipAddress.ifBlank { null },
            )
            InfoRow(
                label = stringResource(R.string.monitoring_label_network_updated),
                value = formatRelativeWithAbsolute(connectivity.lastUpdated),
            )
            InfoRow(
                label = stringResource(R.string.monitoring_label_ping_latency),
                value = when {
                    isPinging -> stringResource(R.string.monitoring_ping_waiting)
                    connectivity.pingResponse <= 0L -> "—"
                    else -> stringResource(
                        R.string.monitoring_ping_result,
                        connectivity.pingLatencyMs,
                        formatRelativeWithAbsolute(connectivity.pingResponse),
                    )
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPing,
                enabled = !isPinging && status.online,
                modifier = Modifier.weight(1f),
            ) {
                if (isPinging) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.replay_24px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.monitoring_action_ping),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            FilledTonalButton(
                onClick = onStartMonitoring,
                enabled = !isStartingMonitoring && !monitoringHealthy,
                modifier = Modifier.weight(1f),
            ) {
                if (isStartingMonitoring) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.send_24px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.monitoring_action_start),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.monitoring_actions_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
