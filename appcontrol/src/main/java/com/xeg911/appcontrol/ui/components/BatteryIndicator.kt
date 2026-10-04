package com.xeg911.appcontrol.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BatteryIndicator(
    level: Int,
    charging: Boolean,
    modifier: Modifier = Modifier,
    levelText: String? = null,
    iconSize: Dp = 16.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(2.dp)
) {
    val color = batteryColor(level, charging)
    val icon = batteryIconIndicatorRes(level, charging)

    Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = color,
        )
        Text(
            text = levelText ?: "$level%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun TransportChip(
    transportType: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = transportLabel(transportType),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}