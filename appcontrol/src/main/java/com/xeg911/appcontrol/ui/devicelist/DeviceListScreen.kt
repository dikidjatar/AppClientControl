package com.xeg911.appcontrol.ui.devicelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.DeviceCard
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.ErrorState
import com.xeg911.appcontrol.ui.components.FullScreenLoading
import com.xeg911.appcontrol.ui.components.SearchField
import com.xeg911.appcontrol.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onDeviceClick: (deviceId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DeviceListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.device_list_title, uiState.devices.size),
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.devices.size > 1) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.device_list_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
                )
            }
            val filtered = uiState.devices.filter { device ->
                query.isBlank() || listOf(
                    device.info.deviceName,
                    device.info.model,
                    device.info.brand
                )
                    .any { it.contains(query, ignoreCase = true) }
            }
            when {
                uiState.isLoading -> FullScreenLoading()
                uiState.errorMessage != null -> ErrorState(message = uiState.errorMessage.orEmpty())
                filtered.isEmpty() -> EmptyState(
                    title = stringResource(R.string.device_list_empty_title),
                    subtitle = stringResource(
                        if (query.isBlank()) R.string.device_list_empty_subtitle
                        else R.string.device_list_no_match
                    ),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = AppTheme.spacing.lg,
                        vertical = AppTheme.spacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    items(filtered, key = { it.deviceId }) { device ->
                        DeviceCard(device = device, onClick = { onDeviceClick(device.deviceId) })
                    }
                }
            }
        }
    }
}
