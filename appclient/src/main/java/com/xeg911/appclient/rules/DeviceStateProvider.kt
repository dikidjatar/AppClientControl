package com.xeg911.appclient.rules

import com.xeg911.appclient.location.LocationSharingController
import com.xeg911.appclient.monitoring.BatteryMonitor
import com.xeg911.appclient.monitoring.MonitoringController
import com.xeg911.appclient.monitoring.UsageStatsMonitor
import com.xeg911.appclient.permission.PermissionMonitor
import com.xeg911.appclient.session.AppSessionTracker
import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.PermissionSnapshot
import com.xeg911.shared.data.model.usage.UsageStatsSnapshot
import com.xeg911.shared.data.model.usage.UsageSummary
import com.xeg911.shared.firebase.FirebasePaths.Device
import com.xeg911.shared.firebase.FirebasePaths.Permissions
import com.xeg911.shared.firebase.FirebasePaths.Status
import com.xeg911.shared.firebase.FirebasePaths.Usage
import com.xeg911.shared.rules.RuleStateFlattener
import com.xeg911.shared.rules.RuleStateSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStateProvider @Inject constructor(
    private val batteryMonitor: BatteryMonitor,
    private val locationSharingController: LocationSharingController,
    private val monitoringController: MonitoringController,
    private val permissionMonitor: PermissionMonitor,
    private val usageStatsMonitor: UsageStatsMonitor,
    private val sessionTracker: AppSessionTracker,
) {
    val snapshots: Flow<RuleStateSnapshot> = combine<Any?, RuleStateSnapshot>(
        batteryMonitor.latest,
        locationSharingController.latestFix,
        locationSharingController.status,
        monitoringController.isRunning,
        permissionMonitor.state,
        usageStatsMonitor.latest,
        sessionTracker.isInForeground,
    ) { values ->
        val out = HashMap<String, Any?>()
        RuleStateFlattener.flatten(Device.BATTERY, values[0]?.toMap(), out)
        RuleStateFlattener.flatten("${Device.LOCATION}.latest", values[1]?.toMap(), out)
        RuleStateFlattener.flatten("${Device.LOCATION}.status", values[2]?.toMap(), out)
        out["${Device.STATUS}.${Status.MONITORING_RUNNING}"] = values[3]
        out["${Device.STATUS}.${Status.ONLINE}"] = true
        out["${Device.STATUS}.${Status.APP_IN_FOREGROUND}"] = values[6]
        (values[4] as? PermissionSnapshot)?.let {
            out["${Device.PERMISSIONS}.${Permissions.GRANTED_COUNT}"] = it.grantedCount
            out["${Device.PERMISSIONS}.${Permissions.TOTAL_COUNT}"] = it.totalCount
        }
        (values[5] as? UsageStatsSnapshot)?.let {
            RuleStateFlattener.flatten("${Device.USAGE}.${Usage.SUMMARY}", it.summary.toMap(), out)
        }
        out
    }

    private fun Any.toMap(): Map<String, Any?> = when (this) {
        is BatteryInfo -> mapOf(
            "level" to level, "charging" to charging, "chargingSource" to chargingSource,
            "health" to health, "temperatureCelsius" to temperatureCelsius,
        )

        is LocationSnapshot -> mapOf(
            "latitude" to latitude, "longitude" to longitude, "speed" to speedMps,
            "accuracyMeters" to accuracyMeters, "capturedAt" to capturedAt,
        )

        is LocationSharingStatus -> mapOf(
            "serviceRunning" to serviceRunning,
            "state" to state.name,
            "consentGiven" to consentGiven,
        )

        is UsageSummary -> mapOf(
            "totalForegroundMs" to totalForegroundMs, "appCount" to appCount,
            "topPackageName" to topPackageName, "screenUnlockCount" to screenUnlockCount,
        )

        else -> emptyMap()
    }
}
