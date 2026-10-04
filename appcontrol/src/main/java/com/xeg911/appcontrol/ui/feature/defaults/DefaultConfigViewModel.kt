package com.xeg911.appcontrol.ui.feature.defaults

import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.domain.model.DefaultRemoteConfig
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.domain.repository.DeviceConfigRepository
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DefaultsEvent {
    data class ShowMessage(val text: UiText) : DefaultsEvent
}

data class DevicePermissionOverride(
    val device: DeviceSnapshot,
    val override: List<String>?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DefaultConfigViewModel @Inject constructor(
    private val configRepository: DeviceConfigRepository,
    deviceRepository: DeviceRepository,
) : BaseViewModel<DefaultsEvent>() {

    val defaults: StateFlow<UiState<DefaultRemoteConfig>> = configRepository.observeDefaultConfig()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val overrides: StateFlow<List<DevicePermissionOverride>> = deviceRepository.observeDevices()
        .map { it.getOrDefault(emptyList()) }
        .flatMapLatest { devices ->
            if (devices.isEmpty()) flowOf(emptyList())
            else combine(
                devices.map { device ->
                    configRepository.observeConfig(device.deviceId).map { result ->
                        DevicePermissionOverride(device, result.getOrNull()?.requiredPermissions)
                    }
                }
            ) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveDefaultWebviewUrl(url: String) =
        runAction(R.string.config_saved) { configRepository.updateDefaultWebviewUrl(url) }

    fun saveDefaultLocationInterval(intervalMs: Long) =
        runAction(R.string.config_saved) { configRepository.updateDefaultLocationInterval(intervalMs) }

    fun saveDefaultUsageInterval(intervalMs: Long) =
        runAction(R.string.config_saved) { configRepository.updateDefaultUsageInterval(intervalMs) }

    fun saveDefaultRequiredPermissions(keys: List<String>) =
        runAction(R.string.req_perm_saved) { configRepository.updateDefaultRequiredPermissions(keys) }

    fun saveDeviceOverride(deviceId: String, keys: List<String>?) =
        runAction(R.string.req_perm_saved) {
            configRepository.updateRequiredPermissions(
                deviceId,
                keys
            )
        }

    private fun runAction(successRes: Int, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().fold(
                onSuccess = { sendEvent(DefaultsEvent.ShowMessage(UiText.Res(successRes))) },
                onFailure = {
                    sendEvent(
                        DefaultsEvent.ShowMessage(
                            UiText.Dynamic(
                                it.message ?: it.javaClass.simpleName
                            )
                        )
                    )
                },
            )
        }
    }
}
