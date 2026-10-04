package com.xeg911.appcontrol.ui.feature.composer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.feature.composer.ParamPickerKind
import com.xeg911.appcontrol.ui.feature.composer.PermissionPickOption
import com.xeg911.shared.data.model.AppIconStyle
import com.xeg911.shared.data.model.InstalledApp

@Composable
fun ParamPickerSheet(
    kind: ParamPickerKind,
    paramKey: String,
    apps: List<InstalledApp>,
    devicePermissions: List<PermissionPickOption>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (kind) {
        ParamPickerKind.APP -> AppPickerSheet(
            apps = apps,
            onPick = { onPick(it.packageName) },
            onPickPackageName = onPick,
            onDismiss = onDismiss,
        )

        ParamPickerKind.PERMISSION -> PermissionPickerSheet(
            devicePermissions = devicePermissions,
            onPick = onPick,
            onDismiss = onDismiss,
        )

        ParamPickerKind.ICON_STYLE -> IconStylePickerSheet(
            onPick = { onPick(it.id) },
            onDismiss = onDismiss
        )

        ParamPickerKind.BOOLEAN -> BooleanPickerSheet(
            paramKey = paramKey,
            onPick = onPick,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconStylePickerSheet(
    onPick: (AppIconStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.composer_pick_icon_style),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(AppIconStyle.entries, key = { it.id }) { style ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPick(style)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = style.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}
