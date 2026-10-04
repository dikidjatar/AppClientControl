package com.xeg911.appcontrol.ui.feature.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.TransferProgress
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceChipRow
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceOption
import com.xeg911.appcontrol.ui.feature.files.components.PullCard
import com.xeg911.appcontrol.ui.feature.files.components.PushCard
import com.xeg911.appcontrol.ui.feature.files.components.ReceivedFileCard
import com.xeg911.appcontrol.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    initialDeviceId: String,
    onNavigateBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: FileTransferViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialDeviceId) { viewModel.start(initialDeviceId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FilesEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    event.text.asString(
                        context
                    )
                )
            }
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.onFilePicked(it)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tool_files),
                onNavigateBack = onNavigateBack,
                actions = {
                    androidx.compose.material3.TextButton(onClick = onOpenHistory) {
                        Text(stringResource(R.string.tool_history))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.devices.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.device_list_empty_title),
                subtitle = stringResource(R.string.device_list_empty_subtitle),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ChoiceChipRow(
                options = state.devices.map {
                    ChoiceOption(it.deviceId, it.info.deviceName.ifBlank { it.info.model })
                },
                selectedId = state.selectedDeviceId,
                onSelect = viewModel::selectDevice,
                modifier = Modifier.padding(
                    horizontal = AppTheme.spacing.lg,
                    vertical = AppTheme.spacing.sm
                ),
            )
            SecondaryTabRow(selectedTabIndex = state.section.ordinal) {
                FilesSection.entries.forEach { section ->
                    Tab(
                        selected = state.section == section,
                        onClick = { viewModel.selectSection(section) },
                        text = {
                            Text(
                                text = when (section) {
                                    FilesSection.PUSH -> stringResource(R.string.files_tab_push)
                                    FilesSection.PULL -> stringResource(R.string.files_tab_pull)
                                    FilesSection.RECEIVED -> stringResource(
                                        R.string.files_tab_received, state.received.size
                                    )
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                if (state.storageConfigured == false) {
                    item(key = "storage_warning") { StorageWarningCard() }
                }
                when (state.section) {
                    FilesSection.PUSH -> item(key = "push") {
                        PushCard(
                            state = state,
                            onPickFile = { pickFile.launch(arrayOf("*/*")) },
                            onClearFile = viewModel::clearPendingFile,
                            onOptions = viewModel::updatePushOptions,
                            onPush = viewModel::push,
                            onCancel = { viewModel.cancelPush() },
                        )
                    }

                    FilesSection.PULL -> item(key = "pull") {
                        PullCard(
                            request = state.pullRequest,
                            isRequesting = state.isRequesting,
                            onUpdate = viewModel::updatePullRequest,
                            onSend = viewModel::sendPullRequest,
                        )
                    }

                    FilesSection.RECEIVED -> {
                        if (state.received.isEmpty()) {
                            item(key = "received_empty") {
                                EmptyState(
                                    title = stringResource(R.string.files_received_empty_title),
                                    subtitle = stringResource(R.string.files_received_empty),
                                    icon = R.drawable.insert_drive_file_24px,
                                    compact = true,
                                    modifier = Modifier.padding(vertical = AppTheme.spacing.xxl),
                                )
                            }
                        }
                        items(state.received, key = { it.callbackKey }) { file ->
                            ReceivedFileCard(
                                file = file,
                                progress = state.downloads[file.callbackKey]
                                    ?: TransferProgress.Idle,
                                saved = state.saved[file.meta.fileId],
                                onDownload = { viewModel.download(file) },
                                onCancel = { viewModel.cancelDownload(file) },
                                onDelete = { viewModel.deleteReceived(file) },
                                onOpenFailed = { viewModel.showMessage(UiText.Res(R.string.files_open_failed)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageWarningCard() {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = stringResource(R.string.files_storage_not_configured),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
