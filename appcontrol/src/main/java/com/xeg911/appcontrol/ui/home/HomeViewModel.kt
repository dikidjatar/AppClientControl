package com.xeg911.appcontrol.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.domain.model.DefaultRemoteConfig
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.domain.model.HeartbeatHealth
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import com.xeg911.appcontrol.domain.model.heartbeatHealth
import com.xeg911.appcontrol.domain.repository.AuthRepository
import com.xeg911.appcontrol.domain.repository.DeviceConfigRepository
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.domain.repository.TransferHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val devices: List<DeviceSnapshot> = emptyList(),
    val recentTransfers: List<TransferHistoryItem> = emptyList(),
    val requiredPermissions: List<String> = emptyList(),
    val permissionIssues: Map<String, List<String>> = emptyMap(),
) {
    val onlineCount: Int get() = devices.count { it.isOnline }
    val monitoringIssues: List<DeviceSnapshot>
        get() = devices.filter {
            !it.status.monitoringRunning || it.status.heartbeatHealth() != HeartbeatHealth.FRESH
        }
    val lowBattery: List<DeviceSnapshot>
        get() = devices.filter { d -> d.battery?.let { it.level <= LOW_BATTERY && !it.charging } == true }
    val devicesMissingPermissions: List<DeviceSnapshot>
        get() = devices.filter { permissionIssues[it.deviceId].orEmpty().isNotEmpty() }
    val previewDevices: List<DeviceSnapshot> get() = devices.take(MAX_PREVIEW_DEVICES)
    val hasMoreDevices: Boolean get() = devices.size > MAX_PREVIEW_DEVICES

    companion object {
        const val MAX_PREVIEW_DEVICES = 3
        const val LOW_BATTERY = 20
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
    transferHistoryRepository: TransferHistoryRepository,
    deviceConfigRepository: DeviceConfigRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val devices = deviceRepository.observeDevices()
        .map<Result<List<DeviceSnapshot>>, Result<List<DeviceSnapshot>>?> { it }
        .onStart { emit(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val defaults = deviceConfigRepository.observeDefaultConfig()
        .map<Result<DefaultRemoteConfig>, DefaultRemoteConfig?> { it.getOrNull() }
        .onStart { emit(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val permissionIssues: Flow<Map<String, List<String>>> = devices
        .map { result ->
            result?.getOrNull().orEmpty()
                .filter { it.missingRequiredPermissions.isNotEmpty() }
                .associate { it.deviceId to it.missingRequiredPermissions }
        }
        .distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(
        devices,
        transferHistoryRepository.observeAll().onStart { emit(emptyList()) },
        defaults,
        permissionIssues.onStart { emit(emptyMap()) },
    ) { devices, transfers, defaults, issues ->
        HomeUiState(
            isLoading = devices == null,
            errorMessage = devices?.exceptionOrNull()?.localizedMessage,
            devices = devices?.getOrNull().orEmpty(),
            recentTransfers = transfers.take(RECENT_TRANSFERS),
            requiredPermissions = defaults?.requiredPermissions.orEmpty(),
            permissionIssues = issues,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun logout() = authRepository.signOut()

    private companion object {
        const val RECENT_TRANSFERS = 3
    }
}
