package com.xeg911.appcontrol.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.theme.AppTheme

@Composable
fun batteryColor(level: Int, charging: Boolean) = when {
    charging -> AppTheme.colors.online
    level <= 20 -> AppTheme.colors.batteryLow
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun connectivityIconRes(transport: String): Int = when (transport) {
    "WIFI" -> R.drawable.mobile_24px // TODO: replace with wifi icon
    "MOBILE" -> R.drawable.mobile_24px
    else -> R.drawable.mobile_24px
}

@Composable
fun transportLabel(transport: String): String = when (transport) {
    "WIFI" -> stringResource(R.string.transport_wifi)
    "MOBILE" -> stringResource(R.string.transport_mobile)
    "ETHERNET" -> stringResource(R.string.transport_ethernet)
    "VPN" -> stringResource(R.string.transport_vpn)
    else -> transport
}

fun batteryIconIndicatorRes(level: Int, charging: Boolean): Int {
    return when {
        charging -> when {
            level < 30 -> R.drawable.battery_charging_20_24px
            level < 50 -> R.drawable.battery_charging_30_24px
            level < 80 -> R.drawable.battery_charging_50_24px
            level < 90 -> R.drawable.battery_charging_80_24px
            level < 100 -> R.drawable.battery_charging_90_24px
            else -> R.drawable.battery_charging_full_24px
        }

        level < 10 -> R.drawable.battery_alert_24px
        level < 30 -> R.drawable.battery_1_bar_24px
        level < 50 -> R.drawable.battery_2_bar_24px
        level < 80 -> R.drawable.battery_3_bar_24px
        level < 90 -> R.drawable.battery_4_bar_24px
        level < 100 -> R.drawable.battery_5_bar_24px
        else -> R.drawable.battery_full_24px
    }
}

/**
 * Returns a color that reflects usage severity.
 */
@Composable
fun progressColor(ratio: Float): Color = when {
    ratio > 0.90f -> MaterialTheme.colorScheme.error
    ratio > 0.75f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}