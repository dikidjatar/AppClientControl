package com.xeg911.appcontrol.ui.feature.filters.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField
import com.xeg911.appcontrol.ui.feature.composer.components.SwitchRow
import com.xeg911.appcontrol.ui.feature.filters.SourceDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceEditorSheet(
    draft: SourceDraft,
    existingIds: List<String>,
    onSave: (SourceDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var form by remember(draft) { mutableStateOf(draft) }

    val idTaken = form.isNew && form.id in existingIds
    val canSave = form.isValid && !idTaken

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (form.isNew) R.string.filter_editor_title_new else R.string.filter_editor_title_edit
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            ComposerTextField(
                value = form.label,
                onValueChange = { label ->
                    form = form.copy(
                        label = label,
                        id = if (form.isNew) SourceDraft.idFromLabel(label) else form.id,
                    )
                },
                label = stringResource(R.string.filter_field_label),
                placeholder = stringResource(R.string.filter_hint_label),
            )

            ComposerTextField(
                value = form.id,
                onValueChange = { form = form.copy(id = SourceDraft.idFromLabel(it)) },
                label = stringResource(R.string.filter_field_id),
                readOnly = !form.isNew,
                isError = idTaken,
                supportingText = stringResource(
                    when {
                        idTaken -> R.string.filter_error_id_taken
                        form.isNew -> R.string.filter_hint_id_new
                        else -> R.string.filter_hint_id_locked
                    }
                ),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ComposerTextField(
                    value = form.headerEmoji,
                    onValueChange = { form = form.copy(headerEmoji = it) },
                    label = stringResource(R.string.filter_field_header_emoji),
                    modifier = Modifier.weight(1f),
                )
                ComposerTextField(
                    value = form.fromEmoji,
                    onValueChange = { form = form.copy(fromEmoji = it) },
                    label = stringResource(R.string.filter_field_from_emoji),
                    modifier = Modifier.weight(1f),
                )
            }

            SwitchRow(
                label = stringResource(R.string.filter_field_enabled),
                checked = form.enabled,
                onCheckedChange = { form = form.copy(enabled = it) },
            )

            SwitchRow(
                label = stringResource(R.string.filter_field_default_sms),
                description = stringResource(R.string.filter_hint_default_sms),
                checked = form.usesDefaultSms,
                onCheckedChange = { form = form.copy(usesDefaultSms = it) },
            )

            if (!form.usesDefaultSms) {
                StringListEditor(
                    title = stringResource(R.string.filter_field_packages),
                    hint = stringResource(R.string.filter_hint_packages),
                    items = form.packages,
                    onItemsChange = { form = form.copy(packages = it) },
                    keyboardType = KeyboardType.Uri,
                )
            }

            StringListEditor(
                title = stringResource(R.string.filter_field_noise),
                hint = stringResource(R.string.filter_hint_noise),
                items = form.noisePatterns,
                onItemsChange = { form = form.copy(noisePatterns = it) },
            )

            Button(
                onClick = { onSave(form) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.save_24px),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.action_save),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StringListEditor(
    title: String,
    hint: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var input by remember { mutableStateOf("") }

    fun commit() {
        val clean = input.trim()
        if (clean.isNotEmpty() && clean !in items) onItemsChange(items + clean)
        input = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ComposerTextField(
                value = input,
                onValueChange = { input = it },
                label = hint,
                keyboardType = keyboardType,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = ::commit, enabled = input.isNotBlank()) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = stringResource(R.string.composer_add_entry),
                )
            }
        }
        if (items.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEach { item ->
                    InputChip(
                        selected = false,
                        onClick = { onItemsChange(items - item) },
                        label = { Text(item, style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close_24px),
                                contentDescription = stringResource(R.string.composer_cd_remove),
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
