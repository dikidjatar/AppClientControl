package com.xeg911.appcontrol.ui.feature.devicehub.components

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.components.InfoRow
import com.xeg911.appcontrol.ui.components.LabelChip
import com.xeg911.appcontrol.ui.util.formatRelativeWithAbsolute
import com.xeg911.shared.data.model.CapturedNotification

val CapturedNotification.displaySource: String
    get() = source.ifBlank { packageName.substringAfterLast(".") }

fun CapturedNotification.toPlainText(): String = buildString {
    if (title.isNotBlank()) appendLine(title)
    if (text.isNotBlank()) appendLine(text)
    appendLine()
    appendLine("source: $displaySource")
    appendLine("package: $packageName")
    append("timestamp: $timestamp")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailSheet(
    notification: CapturedNotification,
    onDismiss: () -> Unit,
    onDelete: (CapturedNotification) -> Unit,
    onComposeForPackage: (packageName: String, label: String?) -> Unit,
    onMessage: (UiText) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun copy(value: String, messageRes: Int) {
        clipboard.setText(AnnotatedString(value))
        onMessage(UiText.Res(messageRes))
    }

    fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                notification.title.ifBlank { notification.displaySource })
            putExtra(Intent.EXTRA_TEXT, notification.toPlainText())
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    intent,
                    (context as Activity).getString(R.string.notif_action_share)
                )
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabelChip(label = notification.displaySource)
                Text(
                    text = formatRelativeWithAbsolute(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = notification.title.ifBlank { stringResource(R.string.notif_no_title) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = notification.text.ifBlank { stringResource(R.string.notif_no_text) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            InfoRow(
                label = stringResource(R.string.notif_label_package),
                value = notification.packageName
            )
            InfoRow(
                label = stringResource(R.string.notif_label_source),
                value = notification.source
            )
            InfoRow(label = stringResource(R.string.notif_label_id), value = notification.id)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SheetActionButton(
                    label = stringResource(R.string.notif_action_copy_text),
                    iconRes = R.drawable.content_copy_24px,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        copy(
                            listOf(notification.title, notification.text)
                                .filter { it.isNotBlank() }.joinToString("\n"),
                            R.string.notif_copied_text,
                        )
                    },
                )
                SheetActionButton(
                    label = stringResource(R.string.notif_action_copy_all),
                    iconRes = R.drawable.content_copy_24px,
                    modifier = Modifier.weight(1f),
                    onClick = { copy(notification.toPlainText(), R.string.notif_copied_all) },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SheetActionButton(
                    label = stringResource(R.string.notif_action_share),
                    iconRes = R.drawable.send_24px,
                    modifier = Modifier.weight(1f),
                    onClick = ::share,
                )
                SheetActionButton(
                    label = stringResource(R.string.notif_action_compose_app),
                    iconRes = R.drawable.notification_add_24px,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onComposeForPackage(notification.packageName, notification.displaySource)
                        onDismiss()
                    },
                )
            }
            OutlinedButton(
                onClick = {
                    onDelete(notification)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetActionButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1,
        )
    }
}
