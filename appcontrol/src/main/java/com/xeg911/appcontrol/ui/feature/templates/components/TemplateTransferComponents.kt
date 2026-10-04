package com.xeg911.appcontrol.ui.feature.templates.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField

enum class TransferChoice(val labelRes: Int, val iconRes: Int, val isExport: Boolean) {
    IMPORT_TEXT(R.string.templates_import_text, R.drawable.description_24px, isExport = false),
    IMPORT_FILE(R.string.templates_import_file, R.drawable.folder_open_24px, isExport = false),
    EXPORT_FILE(R.string.templates_export_file, R.drawable.save_24px, isExport = true),
    EXPORT_SHARE(R.string.templates_export_share, R.drawable.share_24px, isExport = true),
}

@Composable
fun TemplateTransferMenu(
    onChoice: (TransferChoice) -> Unit,
    enabled: Boolean = true,
    exportEnabled: Boolean = true,
    exportOnly: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val choices = TransferChoice.entries.filter { !exportOnly || it.isExport }

    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(
            painter = painterResource(if (exportOnly) R.drawable.share_24px else R.drawable.swap_vert_24px),
            contentDescription = stringResource(R.string.templates_transfer_cd),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        choices.forEachIndexed { index, choice ->
            if (index > 0 && choices[index - 1].isExport != choice.isExport) HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(choice.labelRes)) },
                enabled = !choice.isExport || exportEnabled,
                leadingIcon = {
                    Icon(
                        painterResource(choice.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    expanded = false
                    onChoice(choice)
                },
            )
        }
    }
}

@Composable
fun ImportJsonDialog(onImport: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.templates_import_text)) },
        text = {
            Column {
                ComposerTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = stringResource(R.string.templates_import_json_label),
                    supportingText = stringResource(R.string.templates_import_json_hint),
                    singleLine = false,
                    minLines = 6,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onImport(text)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.templates_import)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
