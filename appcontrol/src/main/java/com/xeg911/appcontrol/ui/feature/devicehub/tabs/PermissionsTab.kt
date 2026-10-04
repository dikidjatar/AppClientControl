package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.CountStatItem
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.standardFadeTransition
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.PermissionType

private const val ANDROID_PERMISSION_PREFIX = "android.permission."

private enum class PermissionFilter(
    @param:StringRes val labelRes: Int,
    val typeKey: PermissionType?,
) {
    ALL(R.string.perm_filter_all, null),
    RUNTIME(R.string.perm_filter_runtime, PermissionType.RUNTIME),
    SPECIAL(R.string.perm_filter_special, PermissionType.SPECIAL),
    INSTALL(R.string.perm_filter_install, PermissionType.INSTALL_TIME),
}

private val permissionSortComparator = compareBy<PermissionStatus>(
    { it.granted },
    { it.type == PermissionType.INSTALL_TIME },
    { it.simpleName },
)

private val PermissionStatus.shortName: String
    get() = name.removePrefix(ANDROID_PERMISSION_PREFIX)

/**
 * True for permissions AppControl can ask AppClient to request remotely.
 */
val PermissionStatus.isRequestable: Boolean
    get() = !granted && type != PermissionType.INSTALL_TIME

@Composable
fun PermissionsTab(
    uiState: UiState<List<PermissionStatus>>,
    onRequest: (PermissionStatus) -> Unit,
) {
    UiStateContent(
        uiState = uiState,
        modifier = Modifier.fillMaxSize(),
    ) { permissions ->
        PermissionsContent(permissions = permissions, onRequest = onRequest)
    }
}

@Composable
private fun PermissionsContent(
    permissions: List<PermissionStatus>,
    onRequest: (PermissionStatus) -> Unit,
) {
    var activeFilter by remember { mutableStateOf(PermissionFilter.ALL) }

    val countByFilter = remember(permissions) {
        mapOf(
            PermissionFilter.ALL to permissions.size,
            PermissionFilter.RUNTIME to permissions.count { it.type == PermissionType.RUNTIME },
            PermissionFilter.SPECIAL to permissions.count { it.type == PermissionType.SPECIAL },
            PermissionFilter.INSTALL to permissions.count { it.type == PermissionType.INSTALL_TIME },
        )
    }

    val grantedCount = remember(permissions) { permissions.count { it.granted } }
    val deniedCount = remember(permissions) {
        permissions.count { !it.granted && it.type != PermissionType.INSTALL_TIME }
    }

    val filteredPermissions = remember(permissions, activeFilter) {
        val base = if (activeFilter.typeKey == null) permissions
        else permissions.filter { it.type == activeFilter.typeKey }
        base.sortedWith(permissionSortComparator)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PermissionsSummaryHeader(
            total = permissions.size,
            granted = grantedCount,
            denied = deniedCount,
            runtimeCount = countByFilter.getValue(PermissionFilter.RUNTIME),
            specialCount = countByFilter.getValue(PermissionFilter.SPECIAL),
        )

        PermissionsFilterRow(
            activeFilter = activeFilter,
            countByFilter = countByFilter,
            onFilterSelected = { activeFilter = it },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        AnimatedContent(
            targetState = filteredPermissions,
            transitionSpec = { standardFadeTransition() },
            modifier = Modifier.fillMaxSize(),
            label = "permissions_list_transition",
        ) { list ->
            if (list.isEmpty()) {
                EmptyState(
                    title = stringResource(
                        R.string.perm_empty_title,
                        stringResource(activeFilter.labelRes)
                    ),
                    subtitle = stringResource(R.string.perm_empty_subtitle),
                )
            } else {
                PermissionsList(permissions = list, onRequest = onRequest)
            }
        }
    }
}

@Composable
private fun PermissionsSummaryHeader(
    total: Int,
    granted: Int,
    denied: Int,
    runtimeCount: Int,
    specialCount: Int,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CountStatItem(
                value = total,
                label = stringResource(R.string.perm_stat_total),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
            )
            CountStatItem(
                value = granted,
                label = stringResource(R.string.perm_stat_granted),
                color = AppTheme.colors.success,
                style = MaterialTheme.typography.titleLarge,
            )
            CountStatItem(
                value = denied,
                label = stringResource(R.string.perm_stat_denied),
                color = AppTheme.colors.error,
                style = MaterialTheme.typography.titleLarge,
            )
            CountStatItem(
                value = runtimeCount,
                label = stringResource(R.string.perm_filter_runtime),
                color = AppTheme.colors.permissionRuntime,
                style = MaterialTheme.typography.titleLarge,
            )
            CountStatItem(
                value = specialCount,
                label = stringResource(R.string.perm_filter_special),
                color = AppTheme.colors.permissionSpecial,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun PermissionsFilterRow(
    activeFilter: PermissionFilter,
    countByFilter: Map<PermissionFilter, Int>,
    onFilterSelected: (PermissionFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        items(PermissionFilter.entries) { filter ->
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = stringResource(
                            R.string.perm_filter_chip_label,
                            stringResource(filter.labelRes),
                            countByFilter[filter] ?: 0,
                        ),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun PermissionsList(
    permissions: List<PermissionStatus>,
    onRequest: (PermissionStatus) -> Unit,
) {
    val requestable = permissions.filter { it.isRequestable }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (requestable.isNotEmpty()) {
            item(key = "request_hint") { RequestHint(count = requestable.size) }
        }
        items(permissions, key = { it.name }) { permission ->
            PermissionItem(permission = permission, onRequest = { onRequest(permission) })
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun RequestHint(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.perm_request_hint, count),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PermissionItem(permission: PermissionStatus, onRequest: () -> Unit) {
    val isInstallTime = permission.type == PermissionType.INSTALL_TIME

    val statusColor = when {
        permission.granted -> AppTheme.colors.success
        isInstallTime -> AppTheme.colors.offline
        else -> AppTheme.colors.error
    }

    val typeColor = when (permission.type) {
        PermissionType.RUNTIME -> AppTheme.colors.permissionRuntime
        PermissionType.SPECIAL -> AppTheme.colors.permissionSpecial
        else -> AppTheme.colors.permissionInstall
    }

    val typeLabel = when (permission.type) {
        PermissionType.RUNTIME -> stringResource(R.string.perm_filter_runtime)
        PermissionType.SPECIAL -> stringResource(R.string.perm_filter_special)
        else -> stringResource(R.string.perm_filter_install)
    }

    val statusLabel = when {
        permission.granted -> stringResource(R.string.perm_status_granted)
        isInstallTime -> stringResource(R.string.perm_status_install_time)
        else -> stringResource(R.string.perm_status_denied)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.simpleName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = permission.shortName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        StatusChip(label = typeLabel, color = typeColor)
        StatusChip(label = statusLabel, color = statusColor)
        if (permission.isRequestable) {
            FilledTonalButton(
                onClick = onRequest,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.send_24px),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.perm_action_request),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}