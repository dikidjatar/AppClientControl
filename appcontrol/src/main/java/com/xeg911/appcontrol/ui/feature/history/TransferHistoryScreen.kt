package com.xeg911.appcontrol.ui.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import com.xeg911.appcontrol.domain.model.TransferProgress
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.ConfirmDialog
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.FullScreenLoading
import com.xeg911.appcontrol.ui.components.SearchField
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceChipRow
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceOption
import com.xeg911.appcontrol.ui.feature.files.components.FileSummaryRow
import com.xeg911.appcontrol.ui.feature.files.components.PrimaryActionButton
import com.xeg911.appcontrol.ui.feature.files.components.SecondaryActionButton
import com.xeg911.appcontrol.ui.feature.files.components.TransferProgressView
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatBytes
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.appcontrol.ui.util.openFile
import com.xeg911.appcontrol.ui.util.shareFile

private const val ALL_DEVICES = "__all__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferHistoryScreen(
    onNavigateBack: () -> Unit,
    onOpenDevice: (deviceId: String) -> Unit,
    viewModel: TransferHistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    event.text.asString(
                        context
                    )
                )
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_title),
            message = stringResource(R.string.history_clear_message),
            confirmLabel = stringResource(R.string.action_clear),
            onConfirm = { showClearDialog = false; viewModel.clear() },
            onDismiss = { showClearDialog = false },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tool_history),
                onNavigateBack = onNavigateBack,
                actions = {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.delete_24px),
                                contentDescription = stringResource(R.string.action_clear_all),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> FullScreenLoading(modifier = Modifier.padding(padding))
            state.items.isEmpty() -> EmptyState(
                title = stringResource(R.string.history_empty_title),
                subtitle = stringResource(R.string.history_empty_subtitle),
                icon = R.drawable.history_24px,
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SearchField(
                    query = state.filter.query,
                    onQueryChange = viewModel::setQuery,
                    placeholder = stringResource(R.string.history_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
                )
                Filters(state = state, viewModel = viewModel)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppTheme.spacing.lg,
                        end = AppTheme.spacing.lg,
                        bottom = AppTheme.spacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    if (state.visible.isEmpty()) {
                        item(key = "no_match") {
                            EmptyState(
                                title = stringResource(R.string.history_no_match),
                                subtitle = "",
                                compact = true,
                                icon = R.drawable.history_24px,
                                modifier = Modifier.padding(vertical = AppTheme.spacing.xxl),
                            )
                        }
                    }
                    items(state.visible, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            progress = state.redownloads[item.id],
                            onRedownload = { viewModel.redownload(item) },
                            onCancel = { viewModel.cancelRedownload(item) },
                            onDelete = { viewModel.delete(item) },
                            onOpenDevice = { onOpenDevice(item.deviceId) },
                            onOpenFailed = { viewModel.showMessage(UiText.Res(R.string.files_open_failed)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Filters(state: HistoryUiState, viewModel: TransferHistoryViewModel) {
    Column(
        modifier = Modifier.padding(
            horizontal = AppTheme.spacing.lg,
            vertical = AppTheme.spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        if (state.devices.size > 1) {
            ChoiceChipRow(
                options = listOf(
                    ChoiceOption(
                        ALL_DEVICES,
                        stringResource(R.string.history_filter_all)
                    )
                ) +
                        state.devices.map { (id, name) -> ChoiceOption(id, name) },
                selectedId = state.filter.deviceId ?: ALL_DEVICES,
                onSelect = { viewModel.setDeviceFilter(it.takeIf { id -> id != ALL_DEVICES }) },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.filter.onlyMissing,
                onClick = viewModel::toggleOnlyMissing,
                label = {
                    Text(
                        stringResource(
                            R.string.history_filter_missing,
                            state.missingCount
                        )
                    )
                },
                enabled = state.missingCount > 0 || state.filter.onlyMissing,
            )
            Text(
                text = stringResource(R.string.history_count, state.visible.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryCard(
    item: TransferHistoryItem,
    progress: TransferProgress?,
    onRedownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenFailed: () -> Unit,
) {
    val context = LocalContext.current
    val uri = item.localUri

    AppCard {
        FileSummaryRow(
            name = item.meta.fileName,
            mimeType = item.meta.mimeType,
            localUri = uri,
            unavailable = !item.isAvailable,
            detail = "${formatBytes(item.meta.sizeBytes)} · ${formatRelativeTime(item.savedAt)}",
            trailing = {
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(
                label = item.deviceName,
                color = MaterialTheme.colorScheme.primary,
                iconRes = R.drawable.mobile_24px,
                modifier = Modifier.clickable(onClick = onOpenDevice),
            )
            if (!item.isAvailable) {
                StatusChip(
                    label = stringResource(R.string.history_file_missing),
                    color = AppTheme.colors.warning,
                    iconRes = R.drawable.warning_24px,
                )
            }
        }
        if (progress != null) TransferProgressView(progress = progress, onCancel = onCancel)
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (item.isAvailable && uri != null) {
                PrimaryActionButton(
                    label = stringResource(R.string.files_open),
                    iconRes = R.drawable.open_in_new_24px,
                    onClick = { if (!context.openFile(uri, item.meta.mimeType)) onOpenFailed() },
                    modifier = Modifier,
                )
                SecondaryActionButton(
                    label = stringResource(R.string.files_share),
                    iconRes = R.drawable.share_24px,
                    onClick = { context.shareFile(uri, item.meta.mimeType) },
                )
            } else {
                PrimaryActionButton(
                    label = stringResource(R.string.history_redownload),
                    iconRes = R.drawable.download_24px,
                    onClick = onRedownload,
                    enabled = progress !is TransferProgress.Running && item.meta.fileId.isNotBlank(),
                    modifier = Modifier,
                )
            }
        }
    }
}
