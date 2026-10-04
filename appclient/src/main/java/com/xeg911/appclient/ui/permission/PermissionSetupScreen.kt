package com.xeg911.appclient.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.xeg911.appclient.R
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.ui.components.ActionCard
import com.xeg911.appclient.ui.components.SectionHeader
import com.xeg911.appclient.ui.components.StatusBanner
import com.xeg911.appclient.ui.home.HomeUiState
import com.xeg911.appclient.ui.theme.Spacing
import com.xeg911.shared.data.model.PermissionType

@Composable
fun PermissionSetupScreen(
    state: HomeUiState.PermissionsRequired,
    onRequestRuntime: (AppPermission) -> Unit,
    onOpenSettings: (AppPermission) -> Unit,
) {
    val runtime = state.missing.filter { it.type == PermissionType.RUNTIME }
    val special = state.missing.filter { it.type == PermissionType.SPECIAL }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = padding.calculateTopPadding() + Spacing.lg,
                bottom = padding.calculateBottomPadding() + Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    StatusBanner(
                        title = stringResource(R.string.permission_screen_title),
                        message = stringResource(R.string.permission_screen_subtitle),
                    )
                    ProgressRow(granted = state.grantedCount, total = state.totalCount)
                }
            }
            if (runtime.isNotEmpty()) {
                item(key = "runtime_header") { SectionHeader(stringResource(R.string.permission_screen_group_runtime)) }
                items(runtime, key = { it.name }) { permission ->
                    PermissionCard(
                        permission = permission,
                        onClick = { onRequestRuntime(permission) })
                }
            }
            if (special.isNotEmpty()) {
                item(key = "special_header") { SectionHeader(stringResource(R.string.permission_screen_group_special)) }
                items(special, key = { it.name }) { permission ->
                    PermissionCard(
                        permission = permission,
                        onClick = { onOpenSettings(permission) })
                }
            }
            item(key = "footer") {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.permission_screen_notice_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(granted: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.permission_screen_progress, granted, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else granted.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionCard(permission: AppPermission, onClick: () -> Unit) {
    val isSpecial = permission.type == PermissionType.SPECIAL
    ActionCard(
        title = stringResource(permission.titleRes),
        description = stringResource(permission.descriptionRes),
        iconRes = permission.iconRes,
        onClick = onClick,
        accentColor = if (isSpecial) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        trailing = {
            Icon(
                painter = painterResource(if (isSpecial) R.drawable.settings_24px else R.drawable.arrow_forward_24px),
                contentDescription = stringResource(
                    if (isSpecial) R.string.common_action_open_settings else R.string.common_action_grant
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
