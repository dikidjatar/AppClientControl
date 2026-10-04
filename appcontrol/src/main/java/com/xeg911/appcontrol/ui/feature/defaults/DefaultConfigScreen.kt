package com.xeg911.appcontrol.ui.feature.defaults

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.CardHeader
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceChipRow
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceOption
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField
import com.xeg911.appcontrol.ui.feature.files.components.PrimaryActionButton
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatDuration
import com.xeg911.shared.data.model.DeviceConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: DefaultConfigViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.defaults.collectAsStateWithLifecycle()
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tool_default_config),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        UiStateContent(uiState = state, modifier = Modifier.padding(padding)) { config ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.default_config_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(key = "webview") {
                    var draft by remember(config.webviewUrl) { mutableStateOf(config.webviewUrl) }
                    AppCard {
                        CardHeader(
                            title = stringResource(R.string.config_webview_title),
                            subtitle = stringResource(R.string.default_config_webview_subtitle),
                        )
                        ComposerTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = stringResource(R.string.config_webview_url),
                            placeholder = DeviceConfig.DEFAULT_WEBVIEW_URL,
                            keyboardType = KeyboardType.Uri,
                        )
                        PrimaryActionButton(
                            label = stringResource(R.string.action_save),
                            iconRes = R.drawable.save_24px,
                            onClick = { viewModel.saveDefaultWebviewUrl(draft) },
                            enabled = draft.trim() != config.webviewUrl && draft.isNotBlank(),
                        )
                    }
                }
                item(key = "location") {
                    var draft by remember(config.locationIntervalMs) { mutableLongStateOf(config.locationIntervalMs) }
                    AppCard {
                        CardHeader(
                            title = stringResource(R.string.config_location_title),
                            subtitle = stringResource(R.string.default_config_location_subtitle),
                            trailing = {
                                StatusChip(
                                    label = formatDuration(config.locationIntervalMs),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                        )
                        ChoiceChipRow(
                            options = listOf(1L, 2L, 5L, 10L, 15L, 30L, 60L).map { minutes ->
                                ChoiceOption(
                                    id = (minutes * 60_000L).toString(),
                                    label = stringResource(
                                        R.string.config_location_preset_minutes,
                                        minutes
                                    ),
                                )
                            },
                            selectedId = draft.toString(),
                            onSelect = { draft = it.toLong() },
                            label = stringResource(R.string.config_location_interval),
                        )
                        PrimaryActionButton(
                            label = stringResource(R.string.action_save),
                            iconRes = R.drawable.save_24px,
                            onClick = { viewModel.saveDefaultLocationInterval(draft) },
                            enabled = draft != config.locationIntervalMs,
                        )
                    }
                }
                item(key = "usage") {
                    var draft by remember(config.usageIntervalMs) { mutableLongStateOf(config.usageIntervalMs) }
                    AppCard {
                        CardHeader(
                            title = stringResource(R.string.config_usage_title),
                            subtitle = stringResource(R.string.config_usage_subtitle),
                            trailing = {
                                StatusChip(
                                    label = if (config.usageIntervalMs == 0L) stringResource(R.string.config_usage_off)
                                    else formatDuration(config.usageIntervalMs),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                        )
                        ChoiceChipRow(
                            options = listOf(0L, 5L, 15L, 30L, 60L, 180L, 360L).map { minutes ->
                                ChoiceOption(
                                    id = (minutes * 60_000L).toString(),
                                    label = if (minutes == 0L) stringResource(R.string.config_usage_off)
                                    else stringResource(
                                        R.string.config_location_preset_minutes,
                                        minutes
                                    ),
                                )
                            },
                            selectedId = draft.toString(),
                            onSelect = { draft = it.toLong() },
                            label = stringResource(R.string.config_usage_interval),
                        )
                        PrimaryActionButton(
                            label = stringResource(R.string.action_save),
                            iconRes = R.drawable.save_24px,
                            onClick = { viewModel.saveDefaultUsageInterval(draft) },
                            enabled = draft != config.usageIntervalMs,
                        )
                    }
                }
            }
        }
    }
}
