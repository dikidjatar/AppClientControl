package com.xeg911.shared.data.model

/**
 * Live state of a device.
 */
data class DeviceStatus(
    val online: Boolean = false,
    val appInForeground: Boolean = false,
    val monitoringRunning: Boolean = false,
    val lastAppOpenedAt: Long = 0L,
    val lastAppClosedAt: Long = 0L,
    val lastMonitoringHeartbeatAt: Long = 0L,
    val lastSeen: Long = 0L
)
