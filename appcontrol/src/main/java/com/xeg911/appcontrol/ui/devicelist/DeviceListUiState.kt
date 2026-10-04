package com.xeg911.appcontrol.ui.devicelist

import com.xeg911.appcontrol.domain.model.DeviceSnapshot

data class DeviceListUiState(
    val isLoading: Boolean = true,
    val devices: List<DeviceSnapshot> = emptyList(),
    val errorMessage: String? = null,
)