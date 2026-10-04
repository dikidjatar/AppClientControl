package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.ConnectivityInfo
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.DeviceStatus

data class DeviceSnapshot(
    val deviceId: String,
    val info: DeviceInfo,
    val isOnline: Boolean,
    val lastSeen: Long,
    val battery: BatteryInfo?,
    val connectivity: ConnectivityInfo?,
    val status: DeviceStatus,
    val fcmToken: String = "",
    val missingRequiredPermissions: List<String> = emptyList(),
) {
    val label: String get() = info.deviceName.ifBlank { info.model }.ifBlank { deviceId }
}
