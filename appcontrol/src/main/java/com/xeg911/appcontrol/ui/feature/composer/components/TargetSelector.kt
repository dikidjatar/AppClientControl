package com.xeg911.appcontrol.ui.feature.composer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.DeviceTargetOption
import com.xeg911.appcontrol.ui.theme.AppTheme

@Composable
fun TargetSelector(
    devices: List<DeviceTargetOption>,
    selectedDeviceIds: Set<String>,
    manualTokens: List<String>,
    onToggleDevice: (String) -> Unit,
    onAddToken: (String) -> Unit,
    onRemoveToken: (String) -> Unit,
) {
    var tokenInput by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.composer_target_devices),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (devices.isEmpty()) {
            Text(
                text = stringResource(R.string.composer_target_no_devices),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                devices.forEach { device ->
                    FilterChip(
                        selected = device.deviceId in selectedDeviceIds,
                        enabled = device.hasToken,
                        onClick = { onToggleDevice(device.deviceId) },
                        label = {
                            Text(
                                device.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.mobile_24px),
                                contentDescription = null,
                                tint = if (device.isOnline) AppTheme.colors.online else AppTheme.colors.offline,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
            if (devices.any { !it.hasToken }) {
                Text(
                    text = stringResource(R.string.composer_target_missing_token_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ComposerTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = stringResource(R.string.composer_target_manual_token),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = tokenInput.isNotBlank(),
                onClick = {
                    onAddToken(tokenInput)
                    tokenInput = ""
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = stringResource(R.string.composer_target_add_token_cd),
                )
            }
        }

        if (manualTokens.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                manualTokens.forEach { token ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveToken(token) },
                        label = {
                            Text(
                                token.abbreviate(),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close_24px),
                                contentDescription = stringResource(R.string.composer_cd_remove),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun String.abbreviate(): String =
    if (length <= 18) this else "${take(8)}…${takeLast(6)}"
