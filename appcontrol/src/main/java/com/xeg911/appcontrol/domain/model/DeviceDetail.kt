package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.ConnectivityInfo
import com.xeg911.shared.data.model.DeviceConfig
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.DeviceStatus
import com.xeg911.shared.data.model.HardwareInfo
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.WallpaperSnapshot

data class DeviceDetail(
    val deviceId: String,
    val info: DeviceInfo,
    val online: Boolean,
    val lastSeen: Long,
    val status: DeviceStatus,
    val battery: BatteryInfo?,
    val hardware: HardwareInfo?,
    val connectivity: ConnectivityInfo?,
    val config: DeviceConfig?,
    val wallpaper: WallpaperSnapshot?,
    val permissions: List<PermissionStatus> = emptyList(),
    val locationStatus: LocationSharingStatus? = null,
)
