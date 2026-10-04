package com.xeg911.appcontrol.ui.feature.composer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.feature.composer.KeyValueEntry

/**
 * Editable key/value list used for params and extras. Required entries keep
 * their key read-only and cannot be removed.
 */
@Composable
fun KeyValueEditor(
    entries: List<KeyValueEntry>,
    onChange: (List<KeyValueEntry>) -> Unit,
    modifier: Modifier = Modifier,
    emptyHint: String? = null,
    pickerIconFor: (KeyValueEntry) -> Int? = { null },
    onPick: (KeyValueEntry) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (entries.isEmpty() && emptyHint != null) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        entries.forEach { entry ->
            KeyValueRow(
                entry = entry,
                onChange = { updated -> onChange(entries.map { if (it.uid == entry.uid) updated else it }) },
                onRemove = { onChange(entries.filterNot { it.uid == entry.uid }) },
                pickerIconRes = pickerIconFor(entry),
                onPick = { onPick(entry) },
            )
        }
        TextButton(onClick = { onChange(entries + KeyValueEntry()) }) {
            Icon(
                painter = painterResource(R.drawable.add_24px),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.composer_add_entry),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun KeyValueRow(
    entry: KeyValueEntry,
    onChange: (KeyValueEntry) -> Unit,
    onRemove: () -> Unit,
    pickerIconRes: Int?,
    onPick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComposerTextField(
            value = entry.key,
            onValueChange = { onChange(entry.copy(key = it)) },
            label = if (entry.required) stringResource(R.string.composer_key_required)
            else stringResource(R.string.composer_key),
            readOnly = entry.required,
            modifier = Modifier.weight(0.4f),
        )
        ComposerTextField(
            value = entry.value,
            onValueChange = { onChange(entry.copy(value = it)) },
            label = stringResource(R.string.composer_value),
            isError = entry.required && entry.value.isBlank(),
            modifier = Modifier.weight(0.6f),
            trailingIcon = pickerIconRes?.let { iconRes ->
                {
                    IconButton(onClick = onPick) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = stringResource(R.string.composer_cd_pick_value),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )
        if (!entry.required) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.close_24px),
                    contentDescription = stringResource(R.string.composer_cd_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
