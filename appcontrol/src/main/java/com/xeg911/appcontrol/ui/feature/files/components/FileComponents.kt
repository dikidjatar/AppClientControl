package com.xeg911.appcontrol.ui.feature.files.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.TransferProgress
import com.xeg911.appcontrol.ui.components.FileThumbnail
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatBytes

@Composable
fun FileSummaryRow(
    name: String,
    mimeType: String,
    detail: String,
    localUri: android.net.Uri? = null,
    unavailable: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FileThumbnail(mimeType = mimeType, localUri = localUri, unavailable = unavailable)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun TransferProgressView(progress: TransferProgress, onCancel: () -> Unit) {
    when (progress) {
        is TransferProgress.Running -> {
            val phaseLabel = stringResource(
                when (progress.phase) {
                    TransferProgress.Running.Phase.UPLOADING -> R.string.files_phase_uploading
                    TransferProgress.Running.Phase.DOWNLOADING -> R.string.files_phase_downloading
                    TransferProgress.Running.Phase.NOTIFYING -> R.string.files_phase_notifying
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    Text(
                        text = "$phaseLabel ${progress.percent?.let { "$it%" }.orEmpty()} · " +
                                "${formatBytes(progress.bytesDone)} / ${formatBytes(progress.bytesTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val percent = progress.percent
                    if (percent == null || progress.phase == TransferProgress.Running.Phase.NOTIFYING) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            }
        }

        is TransferProgress.Done -> StatusChip(
            label = stringResource(R.string.files_status_done),
            color = AppTheme.colors.success,
            iconRes = R.drawable.check_24px,
        )

        is TransferProgress.Failed -> Text(
            text = progress.message,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.error,
        )

        TransferProgress.Idle -> Unit
    }
}

@SuppressLint("ModifierParameter")
@Composable
fun PrimaryActionButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(AppTheme.dimens.iconSm)
        )
        Text(text = label, modifier = Modifier.padding(start = AppTheme.spacing.sm))
    }
}

@Composable
fun SecondaryActionButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(onClick = onClick, enabled = enabled) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(AppTheme.dimens.iconSm)
        )
        Text(text = label, modifier = Modifier.padding(start = AppTheme.spacing.sm))
    }
}

@Composable
fun SmallIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(AppTheme.dimens.iconXl)
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(AppTheme.dimens.iconMd),
        )
    }
}
