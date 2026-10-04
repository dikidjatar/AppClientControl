package com.xeg911.shared.data.model

data class DeviceRegistration(
    val deviceInfo: DeviceInfo,
    val installedApps: List<InstalledApp>,
    val contacts: List<DeviceContact>?,
    val permissions: PermissionSnapshot,
    /**
     * Required permission keys currently not granted.
     */
    val missingRequiredPermissions: List<String> = emptyList(),
    val batteryInfo: BatteryInfo,
    val hardwareInfo: HardwareInfo,
    val wallpaperSnapshot: WallpaperSnapshot?,
    val firstRegistration: Boolean = false
)
