package com.xeg911.appcontrol.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.ConfirmDialog
import com.xeg911.appcontrol.ui.components.ErrorState
import com.xeg911.appcontrol.ui.components.FullScreenLoading
import com.xeg911.appcontrol.ui.home.components.devicesSection
import com.xeg911.appcontrol.ui.home.components.overviewSection
import com.xeg911.appcontrol.ui.home.components.recentTransfersSection
import com.xeg911.appcontrol.ui.home.components.toolsSection
import com.xeg911.appcontrol.ui.theme.AppTheme

class HomeActions(
    val onOpenTool: (Tool) -> Unit,
    val onOpenAllTools: () -> Unit,
    val onOpenDevice: (deviceId: String) -> Unit,
    val onOpenAllDevices: () -> Unit,
    val onLogout: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    actions: HomeActions,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { actions.onOpenTool(Tool.SETTINGS) }) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            contentDescription = stringResource(R.string.tool_settings),
                        )
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.logout_24px),
                            contentDescription = stringResource(R.string.logout_dialog_btn_confirm),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> FullScreenLoading(modifier = Modifier.padding(padding))
            state.errorMessage != null -> ErrorState(
                message = state.errorMessage.orEmpty(),
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = AppTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                toolsSection(onOpenTool = actions.onOpenTool, onOpenAll = actions.onOpenAllTools)
                overviewSection(
                    state = state,
                    onOpenDevice = actions.onOpenDevice,
                    onOpenPermissions = { actions.onOpenTool(Tool.REQUIRED_PERMISSIONS) },
                )
                devicesSection(
                    state = state,
                    onOpenDevice = actions.onOpenDevice,
                    onViewAll = actions.onOpenAllDevices,
                )
                recentTransfersSection(
                    state = state,
                    onOpenHistory = { actions.onOpenTool(Tool.TRANSFER_HISTORY) },
                )
            }
        }
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.logout_dialog_title),
            message = stringResource(R.string.logout_dialog_message),
            confirmLabel = stringResource(R.string.logout_dialog_btn_confirm),
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
                actions.onLogout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}
