package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.LabelChip
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.StatusCard
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.shared.data.model.notification.DeviceNotificationCapability
import com.xeg911.shared.data.model.notification.SupportedActionDef
import com.xeg911.shared.data.model.notification.SupportedStyleDef

private const val STALE_AFTER_MS = 7L * 24 * 60 * 60 * 1000

@Composable
fun CapabilitiesTab(
    uiState: UiState<DeviceNotificationCapability?>,
    onTokenCopied: () -> Unit,
) {
    UiStateContent(uiState = uiState, modifier = Modifier.fillMaxSize()) { capability ->
        if (capability == null) {
            EmptyState(
                title = stringResource(R.string.capability_empty_title),
                subtitle = stringResource(R.string.capability_empty_subtitle),
            )
        } else {
            CapabilitiesContent(capability = capability, onTokenCopied = onTokenCopied)
        }
    }
}

@Composable
private fun CapabilitiesContent(
    capability: DeviceNotificationCapability,
    onTokenCopied: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "summary") { CapabilitySummary(capability) }
        item(key = "token") { TokenCard(capability = capability, onTokenCopied = onTokenCopied) }

        item(key = "styles_header") {
            SectionHeader(
                title = stringResource(
                    R.string.capability_styles_title,
                    capability.supportedStyles.size
                )
            )
        }
        items(capability.supportedStyles, key = { "style_${it.id}" }) { style ->
            StyleCard(style = style)
        }

        item(key = "actions_header") {
            SectionHeader(
                title = stringResource(
                    R.string.capability_actions_title,
                    capability.supportedActions.size
                )
            )
        }
        items(capability.supportedActions, key = { "action_${it.id}" }) { action ->
            ActionCard(action = action)
        }
    }
}

@Composable
private fun CapabilitySummary(capability: DeviceNotificationCapability) {
    val isStale = capability.updatedAt <= 0L ||
            System.currentTimeMillis() - capability.updatedAt > STALE_AFTER_MS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusCard(
            iconRes = R.drawable.mobile_24px,
            iconTint = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.capability_schema_version),
            value = "v${capability.schemaVersion}",
            modifier = Modifier.weight(1f),
        )
        StatusCard(
            iconRes = if (isStale) R.drawable.error_24px else R.drawable.check_24px,
            iconTint = if (isStale) AppTheme.colors.batteryCharging else AppTheme.colors.online,
            label = stringResource(R.string.capability_updated),
            value = formatRelativeTime(capability.updatedAt),
            subtitle = if (isStale) stringResource(R.string.capability_stale_hint) else null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TokenCard(
    capability: DeviceNotificationCapability,
    onTokenCopied: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val hasToken = capability.fcmToken.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.capability_fcm_token),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (hasToken) capability.fcmToken else stringResource(R.string.capability_token_missing),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (hasToken) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hasToken) {
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(capability.fcmToken))
                        onTokenCopied()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.content_copy_24px),
                        contentDescription = stringResource(R.string.capability_cd_copy_token),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleCard(style: SupportedStyleDef) {
    CapabilityCard(id = style.id, description = style.description) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            style.usedFields.forEach { LabelChip(label = it) }
        }
    }
}

@Composable
private fun ActionCard(action: SupportedActionDef) {
    CapabilityCard(id = action.id, description = action.description) {
        if (action.requiredParams.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.composer_required_params,
                    action.requiredParams.joinToString()
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (action.optionalParams.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.composer_optional_params,
                    action.optionalParams.joinToString()
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun CapabilityCard(
    id: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = id,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
