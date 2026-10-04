package com.xeg911.shared.data.model

data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val model: String = "",
    val brand: String = "",
    val manufacturer: String = "",
    val product: String = "",
    val androidVersion: String = "",
    val sdkInt: Int = 0,
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val lastSeen: Long = 0L
)
