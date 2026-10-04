package com.xeg911.shared.data.model

/**
 * Battery snapshot
 *
 * [chargingSource] : "AC" | "USB" | "WIRELESS" | "DOCK" | "NONE"
 * [health]         : "GOOD" | "OVERHEAT" | "DEAD" | "OVER_VOLTAGE" | "COLD" | "UNKNOWN"
 */
data class BatteryInfo(
    val level: Int = -1,
    val charging: Boolean = false,
    val chargingSource: String = "NONE",
    val health: String = "UNKNOWN",
    val temperatureCelsius: Float = 0f,
    val voltageMillivolts: Int = 0,
    val capturedAt: Long = 0L
)
