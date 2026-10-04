package com.xeg911.appclient.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xeg911.appclient.core.network.NetworkStatusMonitor
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.domain.usecase.ObserveDeviceConfigUseCase
import com.xeg911.appclient.monitoring.MonitoringController
import com.xeg911.appclient.monitoring.MonitoringLifecycleManager
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionMonitor
import com.xeg911.appclient.permission.PermissionSettingsLauncher
import com.xeg911.appclient.ui.location.LocationConsentActivity
import com.xeg911.shared.data.model.DeviceConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionMonitor: PermissionMonitor,
    private val settingsLauncher: PermissionSettingsLauncher,
    private val monitoringLifecycleManager: MonitoringLifecycleManager,
    private val preferences: AppPreferences,
    networkStatusMonitor: NetworkStatusMonitor,
    observeDeviceConfig: ObserveDeviceConfigUseCase,
) : ViewModel() {

    private val config = observeDeviceConfig()
        .map<DeviceConfig, DeviceConfig?> { it }
        .onStart { emit(null) }

    private val requiredPermissions = combine(
        config,
        preferences.cachedRequiredPermissions(),
    ) { live, cached -> live?.requiredPermissions ?: cached }

    val isOnline: StateFlow<Boolean> = networkStatusMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<HomeUiState> = combine(
        permissionMonitor.state, config, requiredPermissions, isOnline,
    ) { snapshot, deviceConfig, requiredKeys, online ->
        when (requiredKeys) { // Neither Firebase nor the cache knows the gate yet.
            null if !online -> HomeUiState.Offline
            null -> HomeUiState.Loading
            else -> {
                val required = AppPermission.fromKeys(requiredKeys)
                val missing = required.filter { snapshot.items[it.simpleName]?.granted != true }
                when {
                    missing.isNotEmpty() -> HomeUiState.PermissionsRequired(
                        missing = missing,
                        grantedCount = required.size - missing.size,
                        totalCount = required.size,
                    )

                    deviceConfig == null && !online -> HomeUiState.Offline
                    deviceConfig == null -> HomeUiState.Loading
                    else -> HomeUiState.Ready(deviceConfig.webviewUrl)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun refreshPermissions() = permissionMonitor.refresh()

    fun openSettings(permission: AppPermission) = settingsLauncher.open(permission)

    fun ensureMonitoringRunning() =
        monitoringLifecycleManager.startEnabledMonitors(MonitoringController.StartReason.PERMISSION_GRANTED)

    fun promptLocationConsentIfNeeded() {
        viewModelScope.launch {
            if (preferences.locationConsentPrompted()) return@launch
            context.startActivity(LocationConsentActivity.intent(context))
        }
    }
}
