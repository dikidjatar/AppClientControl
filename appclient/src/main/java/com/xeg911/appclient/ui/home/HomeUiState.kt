package com.xeg911.appclient.ui.home

import com.xeg911.appclient.permission.AppPermission

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Offline : HomeUiState

    data class PermissionsRequired(
        val missing: List<AppPermission>,
        val grantedCount: Int,
        val totalCount: Int,
    ) : HomeUiState

    data class Ready(val webviewUrl: String) : HomeUiState
}
