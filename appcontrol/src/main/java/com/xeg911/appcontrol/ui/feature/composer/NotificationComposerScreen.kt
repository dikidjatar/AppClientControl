package com.xeg911.appcontrol.ui.feature.composer

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.FullScreenLoading
import com.xeg911.appcontrol.ui.components.NotificationPreview
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerSection
import com.xeg911.appcontrol.ui.feature.composer.components.ParamPickerSheet
import com.xeg911.appcontrol.ui.feature.composer.components.PayloadPreview
import com.xeg911.appcontrol.ui.feature.composer.components.SaveTemplateDialog
import com.xeg911.appcontrol.ui.feature.composer.components.TargetSelector
import com.xeg911.appcontrol.ui.feature.composer.components.TemplatePickerSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationComposerScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationComposerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSaveTemplate by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }

    if (showSaveTemplate) {
        SaveTemplateDialog(
            onSave = viewModel::saveTemplate,
            onDismiss = { showSaveTemplate = false },
        )
    }
    if (showTemplates) {
        TemplatePickerSheet(
            templates = state.templates,
            onLoad = viewModel::loadTemplate,
            onDelete = viewModel::deleteTemplate,
            onDismiss = { showTemplates = false },
        )
    }
    // Picker opened automatically for the action just selected (app, permission, icon style).
    state.pendingPick?.let { pick ->
        ParamPickerSheet(
            kind = pick.kind,
            paramKey = pick.key,
            apps = state.apps,
            devicePermissions = state.missingPermissions,
            onPick = viewModel::applyPick,
            onDismiss = viewModel::dismissPick,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ComposerEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    event.text.asString(
                        context
                    )
                )

                ComposerEvent.Finished -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(
                    if (state.isEditing) R.string.composer_title_edit else R.string.composer_title
                ),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showTemplates = true }, enabled = !state.isLoading) {
                        Icon(
                            painter = painterResource(R.drawable.folder_open_24px),
                            contentDescription = stringResource(R.string.template_cd_load),
                        )
                    }
                    IconButton(onClick = { showSaveTemplate = true }, enabled = !state.isLoading) {
                        Icon(
                            painter = painterResource(R.drawable.save_24px),
                            contentDescription = stringResource(R.string.template_cd_save),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SendBar(
                errors = state.errors,
                isSending = state.isSending,
                isEditing = state.isEditing,
                targetCount = state.targets.size,
                onSend = viewModel::send,
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            FullScreenLoading(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "targets") {
                ComposerSection(
                    title = stringResource(R.string.composer_section_targets),
                    subtitle = stringResource(
                        R.string.composer_section_targets_count,
                        state.targets.size
                    ),
                ) {
                    TargetSelector(
                        devices = state.devices,
                        selectedDeviceIds = state.selectedDeviceIds,
                        manualTokens = state.manualTokens,
                        onToggleDevice = viewModel::toggleDevice,
                        onAddToken = viewModel::addManualToken,
                        onRemoveToken = viewModel::removeManualToken,
                    )
                }
            }
            item(key = "content") { ContentSection(state, viewModel) }
            // Cancel-only payloads render nothing on the device: only id + lifecycle matter.
            if (!state.form.cancelOnly) {
                item(key = "style") { StyleSection(state, viewModel) }
                item(key = "appearance") { AppearanceSection(state.form, viewModel) }
                item(key = "behavior") { BehaviorSection(state.form, viewModel) }
                item(key = "grouping") { GroupingSection(state.form, viewModel) }
                item(key = "tap") { TapActionSection(state, viewModel) }
                item(key = "actions") { ActionButtonsSection(state, viewModel) }
                item(key = "extras") { ExtrasSection(state.form, viewModel) }
            }
            item(key = "lifecycle") { LifecycleSection(state.form, viewModel) }
            item(key = "preview") {
                ComposerSection(
                    title = stringResource(R.string.composer_section_preview),
                    subtitle = stringResource(R.string.composer_section_preview_hint),
                ) {
                    val previewPayload = remember(state.form) { state.form.toPayload() }
                    NotificationPreview(payload = previewPayload)
                    PayloadPreview(
                        json = state.payloadPreview,
                        onCopied = {
                            scope.launch {
                                snackbarHostState.showSnackbar((context as Activity).getString(R.string.composer_json_copied))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SendBar(
    errors: List<UiText>,
    isSending: Boolean,
    isEditing: Boolean,
    targetCount: Int,
    onSend: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            errors.forEach { error ->
                Text(
                    text = error.asString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onSend,
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.send_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (isEditing) stringResource(
                                R.string.composer_btn_update,
                                targetCount
                            )
                            else stringResource(R.string.composer_btn_send, targetCount),
                        )
                    }
                }
            }
        }
    }
}
