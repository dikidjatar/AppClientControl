package com.xeg911.appcontrol.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.StatusCard
import com.xeg911.appcontrol.ui.home.HomeUiState
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.shared.data.model.ConfigurablePermission

fun LazyListScope.overviewSection(
    state: HomeUiState,
    onOpenDevice: (String) -> Unit,
    onOpenPermissions: () -> Unit,
) {
    item(key = "overview_header") {
        SectionHeader(title = stringResource(R.string.home_section_overview))
    }
    item(key = "overview_metrics") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            StatusCard(
                iconRes = R.drawable.mobile_24px,
                iconTint = AppTheme.colors.online,
                label = stringResource(R.string.home_metric_online),
                value = "${state.onlineCount}/${state.devices.size}",
                subtitle = stringResource(R.string.home_metric_online_hint),
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                iconRes = R.drawable.verified_user_24px,
                iconTint = AppTheme.colors.info,
                label = stringResource(R.string.home_metric_required),
                value = state.requiredPermissions.size.toString(),
                subtitle = state.requiredPermissions
                    .mapNotNull { ConfigurablePermission.fromKey(it)?.shortLabel() }
                    .joinToString().ifBlank { stringResource(R.string.home_metric_required_none) },
                modifier = Modifier.weight(1f),
                onClick = onOpenPermissions,
                badge = state.devicesMissingPermissions.size.takeIf { it > 0 }?.toString(),
            )
        }
    }
    val alerts = buildAlerts(state)
    if (alerts.isEmpty()) {
        item(key = "overview_ok") {
            AlertCard(
                iconRes = R.drawable.check_circle_24px,
                tint = AppTheme.colors.success,
                title = stringResource(R.string.home_alert_all_good),
                message = stringResource(R.string.home_alert_all_good_hint),
            )
        }
    } else {
        items(alerts, key = { "alert_${it.key}" }) { alert ->
            AlertCard(
                iconRes = alert.iconRes,
                tint = alert.tint(),
                title = alert.title(),
                message = alert.message(),
                onClick = alert.device?.let { d -> { onOpenDevice(d.deviceId) } },
            )
        }
    }
}

private enum class AlertKind { PERMISSION, OFFLINE, MONITORING, BATTERY }

private class Alert(
    val kind: AlertKind,
    val device: DeviceSnapshot?,
    val count: Int,
    val missingKeys: List<String> = emptyList(),
) {
    val key: String get() = "${kind.name}_${device?.deviceId ?: "all"}"
    val iconRes: Int
        get() = when (kind) {
            AlertKind.PERMISSION -> R.drawable.verified_user_24px
            AlertKind.OFFLINE -> R.drawable.mobile_24px
            AlertKind.MONITORING -> R.drawable.warning_24px
            AlertKind.BATTERY -> R.drawable.battery_alert_24px
        }

    @Composable
    fun tint(): Color = when (kind) {
        AlertKind.PERMISSION -> AppTheme.colors.error
        AlertKind.OFFLINE -> AppTheme.colors.neutral
        AlertKind.MONITORING -> AppTheme.colors.warning
        AlertKind.BATTERY -> AppTheme.colors.error
    }

    @Composable
    fun title(): String = device?.info?.deviceName?.ifBlank { device.info.model }
        ?: stringResource(
            when (kind) {
                AlertKind.PERMISSION -> R.string.home_alert_permission_title
                AlertKind.OFFLINE -> R.string.home_alert_offline_title
                AlertKind.MONITORING -> R.string.home_alert_monitoring_title
                AlertKind.BATTERY -> R.string.home_alert_battery_title
            }, count
        )

    @Composable
    fun message(): String = if (kind == AlertKind.PERMISSION && missingKeys.isNotEmpty()) {
        stringResource(
            R.string.home_alert_permission_message,
            missingKeys.mapNotNull { ConfigurablePermission.fromKey(it)?.shortLabel() }
                .joinToString(),
        )
    } else stringResource(
        when (kind) {
            AlertKind.PERMISSION -> R.string.home_alert_permission_message_generic
            AlertKind.OFFLINE -> R.string.home_alert_offline_message
            AlertKind.MONITORING -> R.string.home_alert_monitoring_message
            AlertKind.BATTERY -> R.string.home_alert_battery_message
        }
    )
}

private fun buildAlerts(state: HomeUiState): List<Alert> {
    fun group(kind: AlertKind, devices: List<DeviceSnapshot>): List<Alert> = when {
        devices.isEmpty() -> emptyList()
        devices.size <= 2 -> devices.map { Alert(kind, it, 1) }
        else -> listOf(Alert(kind, null, devices.size))
    }

    val permissionAlerts = state.devicesMissingPermissions.let { devices ->
        if (devices.size <= 2) devices.map {
            Alert(
                AlertKind.PERMISSION,
                it,
                1,
                state.permissionIssues[it.deviceId].orEmpty()
            )
        }
        else listOf(Alert(AlertKind.PERMISSION, null, devices.size))
    }
    return permissionAlerts +
            group(AlertKind.MONITORING, state.monitoringIssues.filter { it.isOnline }) +
            group(AlertKind.BATTERY, state.lowBattery) +
            group(AlertKind.OFFLINE, state.devices.filter { !it.isOnline })
}

@Composable
private fun AlertCard(
    iconRes: Int,
    tint: Color,
    title: String,
    message: String,
    onClick: (() -> Unit)? = null,
) {
    AppCard(
        modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        outlined = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppTheme.spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Icon(painter = painterResource(iconRes), contentDescription = null, tint = tint)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ConfigurablePermission.shortLabel(): String = stringResource(
    when (this) {
        ConfigurablePermission.POST_NOTIFICATIONS -> R.string.req_perm_notifications
        ConfigurablePermission.READ_CONTACTS -> R.string.req_perm_contacts
        ConfigurablePermission.READ_EXTERNAL_STORAGE -> R.string.req_perm_storage_read
        ConfigurablePermission.MANAGE_EXTERNAL_STORAGE -> R.string.req_perm_all_files
        ConfigurablePermission.NOTIFICATION_LISTENER -> R.string.req_perm_listener
        ConfigurablePermission.IGNORE_BATTERY_OPTIMIZATIONS -> R.string.req_perm_battery
        ConfigurablePermission.ACCESS_FINE_LOCATION -> R.string.req_perm_location
        ConfigurablePermission.PACKAGE_USAGE_STATS -> R.string.req_perm_usage_stats
    }
)
