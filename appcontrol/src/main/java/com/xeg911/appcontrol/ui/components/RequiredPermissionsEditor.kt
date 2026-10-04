package com.xeg911.appcontrol.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.home.components.shortLabel
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.shared.data.model.ConfigurablePermission

@Composable
fun RequiredPermissionsEditor(
    selected: List<String>,
    onToggle: (ConfigurablePermission, Boolean) -> Unit,
    enabled: Boolean = true,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ConfigurablePermission.entries.forEach { permission ->
            val checked = selected.any { it.equals(permission.key, ignoreCase = true) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        enabled = enabled,
                        onValueChange = { onToggle(permission, it) })
                    .padding(vertical = AppTheme.spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = permission.shortLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = permission.key,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (permission.isSpecial) {
                    StatusChip(
                        label = stringResource(R.string.req_perm_special),
                        color = AppTheme.colors.permissionSpecial,
                    )
                }
            }
        }
    }
}

private val ConfigurablePermission.isSpecial: Boolean
    get() = this == ConfigurablePermission.MANAGE_EXTERNAL_STORAGE ||
            this == ConfigurablePermission.NOTIFICATION_LISTENER ||
            this == ConfigurablePermission.IGNORE_BATTERY_OPTIMIZATIONS ||
            this == ConfigurablePermission.PACKAGE_USAGE_STATS

/** Immutable toggle helper shared by every editor draft. */
fun List<String>.toggled(permission: ConfigurablePermission, checked: Boolean): List<String> =
    ConfigurablePermission.sanitize(
        if (checked) this + permission.key else filterNot {
            it.equals(
                permission.key,
                ignoreCase = true
            )
        }
    )
