package com.xeg911.appcontrol.ui.feature.composer.components

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.LabelChip
import com.xeg911.appcontrol.ui.components.SearchField
import com.xeg911.appcontrol.ui.components.matchesAny
import com.xeg911.appcontrol.ui.feature.composer.PermissionPickOption

/**
 * Fallback catalogue shown under the device-reported permissions,
 * free text covers anything else.
 */
@SuppressLint("InlinedApi")
private val COMMON_PERMISSIONS = listOf(
    PermissionPickOption(Manifest.permission.POST_NOTIFICATIONS, "Notifications"),
    PermissionPickOption(Manifest.permission.READ_CONTACTS, "Contacts"),
    PermissionPickOption(Manifest.permission.ACCESS_FINE_LOCATION, "Precise location"),
    PermissionPickOption(Manifest.permission.ACCESS_COARSE_LOCATION, "Approximate location"),
    PermissionPickOption(Manifest.permission.ACCESS_BACKGROUND_LOCATION, "Background location"),
    PermissionPickOption(Manifest.permission.READ_EXTERNAL_STORAGE, "Read storage"),
    PermissionPickOption(Manifest.permission.MANAGE_EXTERNAL_STORAGE, "Manage all files"),
    PermissionPickOption(Manifest.permission.READ_MEDIA_IMAGES, "Photos"),
    PermissionPickOption(Manifest.permission.READ_MEDIA_VIDEO, "Videos"),
    PermissionPickOption(Manifest.permission.READ_MEDIA_AUDIO, "Audio"),
    PermissionPickOption(Manifest.permission.CAMERA, "Camera"),
    PermissionPickOption(Manifest.permission.RECORD_AUDIO, "Microphone"),
    PermissionPickOption(Manifest.permission.READ_PHONE_STATE, "Phone state"),
    PermissionPickOption(Manifest.permission.READ_CALL_LOG, "Call log"),
    PermissionPickOption(Manifest.permission.READ_SMS, "SMS"),
    PermissionPickOption(
        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
        "Notification access"
    ),
    PermissionPickOption(
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        "Ignore battery optimizations"
    ),
)

private data class PermissionSection(val titleRes: Int, val options: List<PermissionPickOption>)

/**
 * Lists the permissions the target device(s) actually report as missing first, then the
 * common catalogue (minus duplicates). A typed permission name can be used directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionPickerSheet(
    devicePermissions: List<PermissionPickOption>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sections = remember(devicePermissions, query) {
        val known = devicePermissions.map { it.manifestName.lowercase() }.toSet()
        listOf(
            PermissionSection(R.string.composer_pick_permission_missing, devicePermissions),
            PermissionSection(
                R.string.composer_pick_permission_common,
                COMMON_PERMISSIONS.filter { it.manifestName.lowercase() !in known },
            ),
        ).map { section ->
            section.copy(options = section.options.filter {
                query.matchesAny(
                    it.label,
                    it.manifestName
                )
            })
        }.filter { it.options.isNotEmpty() }
    }
    val typed = query.trim()
    val canUseTyped = typed.contains('.') &&
            sections.none { s ->
                s.options.any {
                    it.manifestName.equals(
                        typed,
                        ignoreCase = true
                    )
                }
            }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.composer_pick_permission_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.composer_pick_permission_search),
            leadingIconRes = R.drawable.verified_user_24px,
            trailingIcon = if (canUseTyped) {
                {
                    IconButton(onClick = { onPick(typed); onDismiss() }) {
                        Icon(
                            painter = painterResource(R.drawable.check_24px),
                            contentDescription = stringResource(R.string.composer_pick_use_typed),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else null,
        )
        if (devicePermissions.isEmpty()) {
            Text(
                text = stringResource(R.string.composer_pick_permission_none_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            sections.forEach { section ->
                item(key = "header_${section.titleRes}") {
                    Text(
                        text = stringResource(section.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                items(
                    section.options,
                    key = { "${section.titleRes}_${it.manifestName}" }) { option ->
                    PermissionRow(
                        option = option,
                        onClick = {
                            onPick(option.manifestName)
                            onDismiss()
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(option: PermissionPickOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = option.manifestName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (option.fromDevice) {
            LabelChip(
                label = if (option.deviceCount == 1) stringResource(R.string.composer_pick_permission_missing_badge)
                else stringResource(
                    R.string.composer_pick_permission_missing_count,
                    option.missingOn,
                    option.deviceCount
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooleanPickerSheet(
    paramKey: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.composer_pick_boolean_title, paramKey),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            listOf("true", "false").forEach { value ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPick(value)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}
