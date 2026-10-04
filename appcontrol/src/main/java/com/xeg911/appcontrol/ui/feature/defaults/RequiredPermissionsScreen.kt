package com.xeg911.appcontrol.ui.feature.defaults

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.DefaultRemoteConfig
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.CardHeader
import com.xeg911.appcontrol.ui.components.RequiredPermissionsEditor
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.toggled
import com.xeg911.appcontrol.ui.feature.composer.components.SwitchRow
import com.xeg911.appcontrol.ui.feature.files.components.PrimaryActionButton
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.shared.data.model.ConfigurablePermission

@Composable
@SuppressLint("ModifierParameter")
fun DeviceOverrideCard(
    entry: DevicePermissionOverride,
    defaultKeys: List<String>,
    onSave: (List<String>?) -> Unit,
    modifier: Modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
) {
    val override = entry.override
    var useOverride by remember(override) { mutableStateOf(override != null) }
    var draft by remember(override, defaultKeys) { mutableStateOf(override ?: defaultKeys) }
    val dirty = if (useOverride) draft != override else override != null

    AppCard(modifier = modifier) {
        CardHeader(
            title = entry.device.info.deviceName.ifBlank { entry.device.info.model },
            subtitle = stringResource(
                if (override == null) R.string.req_perm_inherits else R.string.req_perm_overridden
            ),
            trailing = {
                StatusChip(
                    label = stringResource(if (override == null) R.string.req_perm_chip_default else R.string.req_perm_chip_custom),
                    color = if (override == null) AppTheme.colors.neutral else AppTheme.colors.info,
                )
            },
        )
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
        PrimaryActionButton(
            label = stringResource(R.string.action_save),
            iconRes = R.drawable.save_24px,
            onClick = { onSave(if (useOverride) draft else null) },
            enabled = dirty,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequiredPermissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DefaultConfigViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val defaults by viewModel.defaults.collectAsStateWithLifecycle()
    val overrides by viewModel.overrides.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DefaultsEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    event.text.asString(
                        context
                    )
                )
            }
        }
    }

    val defaultKeys = (defaults as? UiState.Content<DefaultRemoteConfig>)?.data?.requiredPermissions
        ?: ConfigurablePermission.DEFAULT_REQUIRED

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tool_required_permissions),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = AppTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            item(key = "default") {
                DefaultGateCard(
                    current = defaultKeys,
                    isLoading = defaults is UiState.Loading,
                    onSave = viewModel::saveDefaultRequiredPermissions,
                )
            }
            item(key = "devices_header") {
                SectionHeader(title = stringResource(R.string.req_perm_devices_section))
            }
            if (overrides.isEmpty()) {
                item(key = "devices_empty") {
                    Text(
                        text = stringResource(R.string.device_list_empty_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
                    )
                }
            }
            items(overrides, key = { it.device.deviceId }) { entry ->
                DeviceOverrideCard(
                    entry = entry,
                    defaultKeys = defaultKeys,
                    onSave = { keys -> viewModel.saveDeviceOverride(entry.device.deviceId, keys) },
                )
            }
        }
    }
}

@Composable
private fun DefaultGateCard(
    current: List<String>,
    isLoading: Boolean,
    onSave: (List<String>) -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    AppCard(
        modifier = Modifier.padding(
            start = AppTheme.spacing.lg,
            end = AppTheme.spacing.lg,
            top = AppTheme.spacing.lg
        )
    ) {
        CardHeader(
            title = stringResource(R.string.req_perm_default_title),
            subtitle = stringResource(R.string.req_perm_default_subtitle),
            trailing = {
                StatusChip(
                    label = draft.size.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
            },
        )
        RequiredPermissionsEditor(
            selected = draft,
            onToggle = { permission, checked -> draft = draft.toggled(permission, checked) },
            enabled = !isLoading,
        )
        if (draft.isEmpty()) {
            Text(
                text = stringResource(R.string.req_perm_none_warning),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.warning,
            )
        }
        PrimaryActionButton(
            label = stringResource(R.string.action_save),
            iconRes = R.drawable.save_24px,
            onClick = { onSave(draft) },
            enabled = !isLoading && draft != current,
        )
    }
}
