package com.xeg911.appcontrol.ui.feature.files.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.PullFileRequest
import com.xeg911.appcontrol.domain.model.PushFileOptions
import com.xeg911.appcontrol.domain.model.ReceivedFile
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import com.xeg911.appcontrol.domain.model.TransferProgress
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.CardHeader
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceChipRow
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceOption
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField
import com.xeg911.appcontrol.ui.feature.composer.components.SwitchRow
import com.xeg911.appcontrol.ui.feature.files.FilesUiState
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatBytes
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.appcontrol.ui.util.openFile
import com.xeg911.appcontrol.ui.util.shareFile
import com.xeg911.shared.data.model.transfer.FileInputSource
import com.xeg911.shared.data.model.transfer.FileTransferLimits

@Composable
fun PushCard(
    state: FilesUiState,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onOptions: (PushFileOptions.() -> PushFileOptions) -> Unit,
    onPush: () -> Unit,
    onCancel: () -> Unit,
) {
    val file = state.pendingFile
    val options = state.pushOptions
    val tooLarge = file != null && file.sizeBytes > FileTransferLimits.MAX_DOWNLOAD_BYTES

    AppCard {
        CardHeader(
            title = stringResource(R.string.files_push_section),
            subtitle = stringResource(R.string.files_push_subtitle),
        )
        if (file == null) {
            SecondaryActionButton(
                label = stringResource(R.string.files_choose_file),
                iconRes = R.drawable.folder_open_24px,
                onClick = onPickFile,
            )
        } else {
            FileSummaryRow(
                name = file.name,
                mimeType = file.mimeType,
                localUri = file.uri,
                detail = "${formatBytes(file.sizeBytes)} · ${file.mimeType}",
                trailing = {
                    SmallIconButton(
                        iconRes = R.drawable.close_24px,
                        contentDescription = stringResource(R.string.files_cd_clear_file),
                        onClick = onClearFile,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        enabled = !state.isPushing,
                    )
                },
            )
            if (tooLarge) {
                Text(
                    text = stringResource(R.string.files_push_too_large),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        ComposerTextField(
            value = options.title,
            onValueChange = { v -> onOptions { copy(title = v) } },
            label = stringResource(R.string.files_notification_title),
            placeholder = stringResource(R.string.files_push_default_title),
        )
        ComposerTextField(
            value = options.subDir,
            onValueChange = { v -> onOptions { copy(subDir = v) } },
            label = stringResource(R.string.files_sub_dir),
            placeholder = "invoices/2026",
            supportingText = stringResource(R.string.files_sub_dir_hint),
        )
        SwitchRow(
            label = stringResource(R.string.files_auto_start),
            checked = options.autoStart,
            onCheckedChange = { v -> onOptions { copy(autoStart = v) } },
            description = stringResource(R.string.files_auto_start_hint),
        )
        TransferProgressView(progress = state.pushProgress, onCancel = onCancel)
        PrimaryActionButton(
            label = stringResource(R.string.files_push_button),
            iconRes = R.drawable.send_24px,
            onClick = onPush,
            enabled = file != null && !tooLarge && !state.isPushing && state.storageConfigured == true,
        )
    }
}

@SuppressLint("SdCardPath")
@Composable
fun PullCard(
    request: PullFileRequest,
    isRequesting: Boolean,
    onUpdate: (PullFileRequest.() -> PullFileRequest) -> Unit,
    onSend: () -> Unit,
) {
    AppCard {
        CardHeader(
            title = stringResource(R.string.files_pull_section),
            subtitle = stringResource(R.string.files_pull_subtitle),
        )
        ChoiceChipRow(
            label = stringResource(R.string.files_input_source),
            options = FileInputSource.entries.map { ChoiceOption(it.id, it.id.replace('_', ' ')) },
            selectedId = request.source.id,
            onSelect = { id ->
                FileInputSource.fromId(id)?.let { s -> onUpdate { copy(source = s) } }
            },
        )
        Text(
            text = request.source.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (request.source == FileInputSource.DIRECT_URI) {
            ComposerTextField(
                value = request.directUri,
                onValueChange = { v -> onUpdate { copy(directUri = v) } },
                label = stringResource(R.string.files_direct_uri),
                placeholder = "file:///sdcard/Download/report.pdf",
                supportingText = stringResource(R.string.files_direct_uri_hint),
                isError = request.directUri.isBlank(),
            )
        } else {
            SwitchRow(
                label = stringResource(R.string.files_allow_multiple),
                checked = request.allowMultiple,
                onCheckedChange = { v -> onUpdate { copy(allowMultiple = v) } },
            )
        }
        ComposerTextField(
            value = request.allowedMime,
            onValueChange = { v -> onUpdate { copy(allowedMime = v) } },
            label = stringResource(R.string.files_allowed_mime),
            placeholder = "image/*, application/pdf",
        )
        ComposerTextField(
            value = request.title,
            onValueChange = { v -> onUpdate { copy(title = v) } },
            label = stringResource(R.string.files_notification_title),
            placeholder = stringResource(R.string.files_pull_default_title),
        )
        PrimaryActionButton(
            label = stringResource(if (isRequesting) R.string.files_pull_sending else R.string.files_pull_button),
            iconRes = R.drawable.notification_add_24px,
            onClick = onSend,
            enabled = !isRequesting &&
                    (request.source != FileInputSource.DIRECT_URI || request.directUri.isNotBlank()),
        )
    }
}

/** A file uploaded by AppClient; shows the local copy when one was already downloaded. */
@Composable
fun ReceivedFileCard(
    file: ReceivedFile,
    progress: TransferProgress,
    saved: TransferHistoryItem?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenFailed: () -> Unit,
) {
    val context = LocalContext.current
    val localUri =
        (progress as? TransferProgress.Done)?.savedUri ?: saved?.takeIf { it.isAvailable }?.localUri

    AppCard {
        FileSummaryRow(
            name = file.meta.fileName,
            mimeType = file.meta.mimeType,
            localUri = localUri,
            detail = "${formatBytes(file.meta.sizeBytes)} · ${file.meta.mimeType} · " +
                    "${
                        file.source.replace('_', ' ').lowercase()
                    } · ${formatRelativeTime(file.receivedAt)}",
            trailing = {
                SmallIconButton(
                    iconRes = R.drawable.delete_24px,
                    contentDescription = stringResource(R.string.action_delete),
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
        )
        if (!file.canDownload) {
            Text(
                text = stringResource(R.string.files_received_too_large),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        TransferProgressView(progress = progress, onCancel = onCancel)
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (localUri == null) {
                PrimaryActionButton(
                    label = stringResource(R.string.files_download),
                    iconRes = R.drawable.download_24px,
                    onClick = onDownload,
                    enabled = file.canDownload && progress !is TransferProgress.Running,
                    modifier = Modifier,
                )
            } else {
                PrimaryActionButton(
                    label = stringResource(R.string.files_open),
                    iconRes = R.drawable.open_in_new_24px,
                    onClick = {
                        if (!context.openFile(
                                localUri,
                                file.meta.mimeType
                            )
                        ) onOpenFailed()
                    },
                    modifier = Modifier,
                )
                SecondaryActionButton(
                    label = stringResource(R.string.files_share),
                    iconRes = R.drawable.share_24px,
                    onClick = { context.shareFile(localUri, file.meta.mimeType) },
                )
            }
        }
    }
}
