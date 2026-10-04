package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.DeviceDetail
import com.xeg911.appcontrol.domain.model.HeartbeatHealth
import com.xeg911.appcontrol.domain.model.heartbeatHealth
import com.xeg911.appcontrol.ui.components.BatteryIndicator
import com.xeg911.appcontrol.ui.components.InfoRow
import com.xeg911.appcontrol.ui.components.InfoSection
import com.xeg911.appcontrol.ui.components.StatusCard
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.batteryColor
import com.xeg911.appcontrol.ui.components.connectivityIconRes
import com.xeg911.appcontrol.ui.components.transportLabel
import com.xeg911.appcontrol.ui.feature.devicehub.components.MonitoringStatusSection
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatBytes
import com.xeg911.appcontrol.ui.util.formatLastSeen
import com.xeg911.appcontrol.ui.util.formatRelativeWithAbsolute
import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.ConnectivityInfo
import com.xeg911.shared.data.model.DeviceStatus
import com.xeg911.shared.data.model.LocationSharingState
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.WallpaperSnapshot

@Composable
fun OverviewTab(
    device: DeviceDetail,
    isPinging: Boolean,
    isStartingMonitoring: Boolean,
    onPing: () -> Unit,
    onStartMonitoring: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        StatusHeader(
            status = device.status,
            battery = device.battery,
            locationStatus = device.locationStatus,
        )

        if (device.battery != null || device.connectivity != null) {
            Spacer(Modifier.height(16.dp))
            MetricsRow(battery = device.battery, connectivity = device.connectivity)
        }

        Spacer(Modifier.height(16.dp))
        MonitoringStatusSection(
            status = device.status,
            connectivity = device.connectivity,
            isPinging = isPinging,
            isStartingMonitoring = isStartingMonitoring,
            onPing = onPing,
            onStartMonitoring = onStartMonitoring,
        )

        InfoSection(title = stringResource(R.string.device_information)) {
            with(device.info) {
                InfoRow(label = stringResource(R.string.label_device_name), value = deviceName)
                InfoRow(label = stringResource(R.string.label_brand), value = brand)
                InfoRow(label = stringResource(R.string.label_model), value = model)
                InfoRow(label = stringResource(R.string.label_manufacturer), value = manufacturer)
                InfoRow(label = stringResource(R.string.label_product), value = product)
                InfoRow(
                    label = stringResource(R.string.label_android_version),
                    value = "$androidVersion (SDK $sdkInt)"
                )
            }
        }

        InfoSection(title = stringResource(R.string.overview_section_appclient)) {
            with(device.info) {
                InfoRow(label = stringResource(R.string.label_app_version), value = appVersionName)
                InfoRow(
                    label = stringResource(R.string.label_app_version_code),
                    value = appVersionCode.toString()
                )
                InfoRow(label = stringResource(R.string.label_device_id), value = device.deviceId)
                InfoRow(
                    label = stringResource(R.string.overview_label_registered_seen),
                    value = formatRelativeWithAbsolute(lastSeen)
                )
            }
        }

        device.config?.let { config ->
            InfoSection(title = stringResource(R.string.overview_section_configuration)) {
                InfoRow(
                    label = stringResource(R.string.config_webview_url),
                    value = config.webviewUrl
                )
            }
        }

        device.wallpaper?.takeIf { it.downloadUrl.isNotBlank() }?.let { wallpaper ->
            WallpaperSection(wallpaper = wallpaper)
        }
    }
}

@Composable
private fun StatusHeader(
    status: DeviceStatus,
    battery: BatteryInfo?,
    locationStatus: LocationSharingStatus?,
) {
    val online = status.online
    val heartbeat = status.heartbeatHealth()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (online) AppTheme.colors.online else AppTheme.colors.offline),
            )
            Text(
                text = stringResource(if (online) R.string.status_online else R.string.status_offline),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (online) AppTheme.colors.online else AppTheme.colors.offline,
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatLastSeen(status.lastSeen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Glanceable chips for the three live flags.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(
                label = stringResource(
                    if (status.appInForeground) R.string.monitoring_value_foreground
                    else R.string.monitoring_value_background
                ),
                color = if (status.appInForeground) AppTheme.colors.online else AppTheme.colors.offline,
            )
            StatusChip(
                label = stringResource(
                    when {
                        status.monitoringRunning && heartbeat == HeartbeatHealth.FRESH ->
                            R.string.monitoring_chip_running

                        status.monitoringRunning -> R.string.monitoring_chip_stale
                        else -> R.string.monitoring_chip_stopped
                    }
                ),
                color = when {
                    status.monitoringRunning && heartbeat == HeartbeatHealth.FRESH -> AppTheme.colors.online
                    status.monitoringRunning -> AppTheme.colors.callbackDenied
                    else -> AppTheme.colors.error
                },
            )
            // Location sharing is user-controlled; show it as clearly as the other flags.
            StatusChip(
                label = stringResource(
                    when (locationStatus?.state) {
                        LocationSharingState.ACTIVE -> R.string.location_chip_active
                        LocationSharingState.STARTING,
                        LocationSharingState.LOCATION_OFF,
                        LocationSharingState.PERMISSION_REQUIRED -> R.string.location_chip_pending

                        else -> R.string.location_chip_off
                    }
                ),
                color = when (locationStatus?.state) {
                    LocationSharingState.ACTIVE -> AppTheme.colors.online
                    LocationSharingState.STARTING,
                    LocationSharingState.LOCATION_OFF,
                    LocationSharingState.PERMISSION_REQUIRED -> AppTheme.colors.callbackDenied

                    else -> AppTheme.colors.offline
                },
            )
        }

        // Battery quick-glance when offline
        if (!online && battery != null && battery.level >= 0) {
            BatteryIndicator(
                level = battery.level,
                charging = battery.charging,
                levelText = stringResource(R.string.status_battery_last_updated, battery.level),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                iconSize = 14.dp
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun MetricsRow(battery: BatteryInfo?, connectivity: ConnectivityInfo?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (battery != null) {
            StatusCard(
                iconRes = if (battery.charging) R.drawable.battery_charging_full_24px else R.drawable.battery_full_24px,
                iconTint = batteryColor(battery.level, battery.charging),
                label = stringResource(R.string.label_battery),
                value = "${battery.level}%",
                subtitle = when {
                    battery.charging -> stringResource(
                        R.string.status_charging_with_source,
                        battery.chargingSource
                    )

                    battery.level <= 20 -> stringResource(R.string.status_battery_critical)
                    else -> "%.1f°C · ${battery.health}".format(battery.temperatureCelsius)
                },
                modifier = Modifier.weight(1f),
            )
        }

        connectivity?.let { conn ->
            StatusCard(
                iconRes = connectivityIconRes(conn.transportType),
                iconTint = if (conn.transportType != "NONE") AppTheme.colors.online else AppTheme.colors.offline,
                label = stringResource(R.string.label_network),
                value = transportLabel(conn.transportType),
                subtitle = conn.ipAddress.takeIf { it.isNotBlank() } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WallpaperSection(wallpaper: WallpaperSnapshot) {
    InfoSection(title = stringResource(R.string.overview_section_wallpaper)) {
        AsyncImage(
            model = wallpaper.downloadUrl,
            contentDescription = stringResource(R.string.overview_section_wallpaper),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium),
        )
        InfoRow(
            label = stringResource(R.string.overview_label_wallpaper_size),
            value = "${wallpaper.widthPx} × ${wallpaper.heightPx} px · ${formatBytes(wallpaper.sizeBytes)}",
        )
        InfoRow(
            label = stringResource(R.string.overview_label_wallpaper_captured),
            value = formatRelativeWithAbsolute(wallpaper.capturedAt),
        )
    }
}
