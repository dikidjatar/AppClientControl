package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.DeviceStatus

enum class HeartbeatHealth { FRESH, STALE, NONE }

/**
 * Must stay in sync with `MonitoringService.HEARTBEAT_INTERVAL_MS` in AppClient.
 */
const val HEARTBEAT_INTERVAL_MS = 5 * 60_000L

/**
 * Two missed beats plus a minute of slack before we call the service stale.
 */
private const val HEARTBEAT_STALE_AFTER_MS = HEARTBEAT_INTERVAL_MS * 2 + 60_000L

fun DeviceStatus.heartbeatHealth(now: Long = System.currentTimeMillis()): HeartbeatHealth = when {
    lastMonitoringHeartbeatAt <= 0L -> HeartbeatHealth.NONE
    now - lastMonitoringHeartbeatAt > HEARTBEAT_STALE_AFTER_MS -> HeartbeatHealth.STALE
    else -> HeartbeatHealth.FRESH
}

/**
 * Monitoring counts as healthy only when AppClient reports it running AND the heartbeat is fresh.
 */
fun DeviceStatus.isMonitoringHealthy(now: Long = System.currentTimeMillis()): Boolean =
    monitoringRunning && heartbeatHealth(now) == HeartbeatHealth.FRESH
