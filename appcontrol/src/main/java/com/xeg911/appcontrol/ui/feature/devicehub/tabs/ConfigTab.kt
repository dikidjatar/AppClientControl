package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.util.TelegramConfigField
import com.xeg911.appcontrol.domain.model.DefaultRemoteConfig
import com.xeg911.appcontrol.domain.model.DeviceRemoteConfig
import com.xeg911.appcontrol.domain.model.TelegramChannelConfig
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.RequiredPermissionsEditor
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.components.toggled
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceChipRow
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceOption
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField
import com.xeg911.appcontrol.ui.feature.composer.components.SwitchRow
import com.xeg911.appcontrol.ui.feature.defaults.DefaultConfigViewModel
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatDuration
import com.xeg911.shared.data.model.AppIconStyle
import com.xeg911.shared.data.model.ConfigurablePermission
import com.xeg911.shared.data.model.DeviceConfig

@Composable
fun ConfigTab(
    uiState: UiState<DeviceRemoteConfig>,
    onSaveWebviewUrl: (String) -> Unit,
    onSaveTelegram: (purpose: String, TelegramChannelConfig) -> Unit,
    onSaveLocationInterval: (Long) -> Unit,
    onSaveUsageInterval: (Long) -> Unit,
    onSaveRequiredPermissions: (List<String>?) -> Unit,
    isApplyingIcon: Boolean,
    onApplyClientIcon: (AppIconStyle) -> Unit,
    onSetAppClientVisibility: (Boolean) -> Unit,
    defaultsViewModel: DefaultConfigViewModel = hiltViewModel(),
) {
    val defaults by defaultsViewModel.defaults.collectAsStateWithLifecycle()
    val defaultKeys = (defaults as? UiState.Content<DefaultRemoteConfig>)?.data?.requiredPermissions
        ?: ConfigurablePermission.DEFAULT_REQUIRED
    UiStateContent(uiState = uiState, modifier = Modifier.fillMaxSize()) { config ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "webview") {
                WebviewCard(url = config.webviewUrl, onSave = onSaveWebviewUrl)
            }
            item(key = "app_icon") {
                AppIconCard(isApplying = isApplyingIcon, onApply = onApplyClientIcon)
            }
            item(key = "app_client_visibility") {
                AppClientVisibilityCard(onSetVisibility = onSetAppClientVisibility)
            }
            item(key = "required_permissions") {
                RequiredPermissionsCard(
                    override = config.requiredPermissions,
                    defaultKeys = defaultKeys,
                    onSave = onSaveRequiredPermissions,
                )
            }
            item(key = "location_interval") {
                LocationIntervalCard(
                    intervalMs = config.locationIntervalMs,
                    onSave = onSaveLocationInterval,
                )
            }
            item(key = "usage") {
                UsageIntervalCard(
                    intervalMs = config.usageIntervalMs,
                    onSave = onSaveUsageInterval,
                )
            }
            TelegramConfigField.PURPOSES.forEach { purpose ->
                item(key = "telegram_$purpose") {
                    TelegramCard(
                        purpose = purpose,
                        config = config.telegram[purpose] ?: TelegramChannelConfig(),
                        onSave = { onSaveTelegram(purpose, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RequiredPermissionsCard(
    override: List<String>?,
    defaultKeys: List<String>,
    onSave: (List<String>?) -> Unit,
) {
    var useOverride by remember(override) { mutableStateOf(override != null) }
    var draft by remember(override, defaultKeys) { mutableStateOf(override ?: defaultKeys) }
    val dirty = if (useOverride) draft != override else override != null

    ConfigCard(
        title = stringResource(R.string.tool_required_permissions),
        subtitle = stringResource(
            if (override == null) R.string.req_perm_inherits else R.string.req_perm_overridden
        ),
        trailing = {
            StatusChip(
                label = stringResource(if (override == null) R.string.req_perm_chip_default else R.string.req_perm_chip_custom),
                color = if (override == null) AppTheme.colors.neutral else AppTheme.colors.info,
            )
        },
    ) {
        SwitchRow(
            label = stringResource(R.string.req_perm_use_override),
            checked = useOverride,
            onCheckedChange = { useOverride = it },
        )
        RequiredPermissionsEditor(
            selected = if (useOverride) draft else defaultKeys,
            onToggle = { permission, checked -> draft = draft.toggled(permission, checked) },
            enabled = useOverride,
        )
        SaveButton(enabled = dirty) { onSave(if (useOverride) draft else null) }
    }
}

@Composable
private fun AppClientVisibilityCard(onSetVisibility: (Boolean) -> Unit) {
    var isHidden by remember { mutableStateOf(false) }

    ConfigCard(
        title = stringResource(R.string.config_hide_app_title),
        subtitle = stringResource(R.string.config_hide_app_subtitle),
    ) {
        SwitchRow(
            label = stringResource(R.string.config_hide_app_title),
            checked = isHidden,
            onCheckedChange = {
                isHidden = it
                onSetVisibility(it)
            },
        )
    }
}

/** Picks an AppClient launcher icon and pushes it as an auto-applied SET_APP_ICON command. */
@Composable
private fun AppIconCard(isApplying: Boolean, onApply: (AppIconStyle) -> Unit) {
    var selected by remember { mutableStateOf(AppIconStyle.DEFAULT) }

    ConfigCard(
        title = stringResource(R.string.config_icon_title),
        subtitle = stringResource(R.string.config_icon_subtitle),
    ) {
        ChoiceChipRow(
            options = AppIconStyle.entries.map { ChoiceOption(id = it.id, label = it.label) },
            selectedId = selected.id,
            onSelect = { id -> AppIconStyle.fromId(id)?.let { selected = it } },
        )
        Text(
            text = stringResource(R.string.config_icon_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = { onApply(selected) }, enabled = !isApplying) {
                if (isApplying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.send_24px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.config_icon_apply),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ConfigCard(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.dimens.cardElevation),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(
            painter = painterResource(R.drawable.save_24px),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(text = stringResource(R.string.action_save), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun WebviewCard(url: String, onSave: (String) -> Unit) {
    var draft by remember(url) { mutableStateOf(url) }

    ConfigCard(
        title = stringResource(R.string.config_webview_title),
        subtitle = stringResource(R.string.config_webview_subtitle),
    ) {
        ComposerTextField(
            value = draft,
            onValueChange = { draft = it },
            label = stringResource(R.string.config_webview_url),
            placeholder = stringResource(R.string.composer_hint_url),
            keyboardType = KeyboardType.Uri,
        )
        SaveButton(enabled = draft.trim() != url && draft.isNotBlank()) { onSave(draft) }
    }
}

@Composable
private fun LocationIntervalCard(intervalMs: Long, onSave: (Long) -> Unit) {
    var draft by remember(intervalMs) { mutableLongStateOf(intervalMs) }
    val presets = listOf(1L, 2L, 5L, 10L, 15L, 30L, 60L)

    ConfigCard(
        title = stringResource(R.string.config_location_title),
        subtitle = stringResource(R.string.config_location_subtitle),
        trailing = {
            StatusChip(
                label = formatDuration(intervalMs),
                color = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        ChoiceChipRow(
            options = presets.map { minutes ->
                ChoiceOption(
                    id = (minutes * 60_000L).toString(),
                    label = stringResource(R.string.config_location_preset_minutes, minutes),
                )
            },
            selectedId = draft.toString(),
            onSelect = { draft = it.toLong() },
            label = stringResource(R.string.config_location_interval),
        )
        Text(
            text = stringResource(R.string.config_location_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SaveButton(enabled = draft != intervalMs) { onSave(draft) }
    }
}

@Composable
private fun UsageIntervalCard(intervalMs: Long, onSave: (Long) -> Unit) {
    var draft by remember(intervalMs) { mutableLongStateOf(intervalMs) }
    val presets = listOf(0L, 5L, 15L, 30L, 60L, 180L, 360L)

    ConfigCard(
        title = stringResource(R.string.config_usage_title),
        subtitle = stringResource(R.string.config_usage_subtitle),
        trailing = {
            StatusChip(
                label = if (intervalMs == DeviceConfig.USAGE_DISABLED) stringResource(R.string.config_usage_off)
                else formatDuration(intervalMs),
                color = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        ChoiceChipRow(
            options = presets.map { minutes ->
                ChoiceOption(
                    id = (minutes * 60_000L).toString(),
                    label = if (minutes == 0L) stringResource(R.string.config_usage_off)
                    else stringResource(R.string.config_location_preset_minutes, minutes),
                )
            },
            selectedId = draft.toString(),
            onSelect = { draft = it.toLong() },
            label = stringResource(R.string.config_usage_interval),
        )
        SaveButton(enabled = draft != intervalMs) { onSave(draft) }
    }
}

@Composable
private fun TelegramCard(
    purpose: String,
    config: TelegramChannelConfig,
    onSave: (TelegramChannelConfig) -> Unit,
) {
    var draft by remember(config) { mutableStateOf(config) }
    var showToken by remember { mutableStateOf(false) }
    val isReady = config.enabled && config.botToken.isNotBlank() && config.chatId.isNotBlank()

    ConfigCard(
        title = stringResource(
            when (purpose) {
                TelegramConfigField.PURPOSE_NOTIFICATION -> R.string.config_telegram_notification_title
                else -> R.string.config_telegram_storage_title
            }
        ),
        subtitle = stringResource(R.string.config_telegram_subtitle, purpose),
        trailing = {
            StatusChip(
                label = stringResource(if (isReady) R.string.config_status_ready else R.string.config_status_inactive),
                color = if (isReady) AppTheme.colors.online else AppTheme.colors.offline,
            )
        },
    ) {
        SwitchRow(
            label = stringResource(R.string.config_telegram_enabled),
            checked = draft.enabled,
            onCheckedChange = { draft = draft.copy(enabled = it) },
        )
        ComposerTextField(
            value = draft.botToken,
            onValueChange = { draft = draft.copy(botToken = it) },
            label = stringResource(R.string.config_telegram_bot_token),
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        painter = painterResource(
                            if (showToken) R.drawable.visibility_off_24px else R.drawable.visibility_24px
                        ),
                        contentDescription = stringResource(R.string.config_cd_toggle_token),
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )
        ComposerTextField(
            value = draft.chatId,
            onValueChange = { draft = draft.copy(chatId = it) },
            label = stringResource(R.string.config_telegram_chat_id),
        )
        SaveButton(enabled = draft != config) { onSave(draft) }
    }
}
