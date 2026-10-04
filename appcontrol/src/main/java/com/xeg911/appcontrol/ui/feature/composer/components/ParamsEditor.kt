package com.xeg911.appcontrol.ui.feature.composer.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xeg911.appcontrol.ui.feature.composer.ActionUiSpec
import com.xeg911.appcontrol.ui.feature.composer.KeyValueEntry
import com.xeg911.appcontrol.ui.feature.composer.ParamPickerKind
import com.xeg911.appcontrol.ui.feature.composer.PermissionPickOption
import com.xeg911.shared.data.model.InstalledApp

@Composable
fun ParamsEditor(
    actionId: String,
    entries: List<KeyValueEntry>,
    onChange: (List<KeyValueEntry>) -> Unit,
    apps: List<InstalledApp>,
    devicePermissions: List<PermissionPickOption>,
    modifier: Modifier = Modifier,
    emptyHint: String? = null,
) {
    var picking by remember { mutableStateOf<Pair<KeyValueEntry, ParamPickerKind>?>(null) }

    KeyValueEditor(
        entries = entries,
        onChange = onChange,
        modifier = modifier,
        emptyHint = emptyHint,
        pickerIconFor = { entry -> ActionUiSpec.pickerFor(actionId, entry.key)?.iconRes },
        onPick = { entry ->
            ActionUiSpec.pickerFor(actionId, entry.key)?.let { picking = entry to it }
        },
    )

    picking?.let { (entry, kind) ->
        ParamPickerSheet(
            kind = kind,
            paramKey = entry.key,
            apps = apps,
            devicePermissions = devicePermissions,
            onPick = { value -> onChange(entries.map { if (it.uid == entry.uid) it.copy(value = value) else it }) },
            onDismiss = { picking = null },
        )
    }
}
