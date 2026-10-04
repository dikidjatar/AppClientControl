package com.xeg911.shared.rules

import com.xeg911.shared.firebase.FirebasePaths.Device

enum class RuleFieldKind { NUMBER, BOOLEAN, TEXT, GEO }

/**
 * Catalog of state paths a condition may reference. [node] is the device child that holds
 * the value, so evaluators subscribe only to the nodes the active rules actually use.
 */
data class RuleField(
    val path: String,
    val kind: RuleFieldKind,
    val label: String,
    val node: String
) {
    companion object {
        // @formatter:off
        val ALL = listOf(
            RuleField("battery.level", RuleFieldKind.NUMBER, "Battery level (%)", Device.BATTERY),
            RuleField("battery.charging", RuleFieldKind.BOOLEAN, "Battery charging", Device.BATTERY),
            RuleField("battery.temperatureCelsius", RuleFieldKind.NUMBER, "Battery temperature (°C)", Device.BATTERY),
            RuleField("battery.health", RuleFieldKind.TEXT, "Battery health", Device.BATTERY),
            RuleField("location.latest", RuleFieldKind.GEO, "Device location", Device.LOCATION),
            RuleField("location.latest.speed", RuleFieldKind.NUMBER, "Speed (m/s)", Device.LOCATION),
            RuleField("location.status.serviceRunning", RuleFieldKind.BOOLEAN, "Location sharing running", Device.LOCATION),
            RuleField("status.online", RuleFieldKind.BOOLEAN, "Online", Device.STATUS),
            RuleField("status.monitoringRunning", RuleFieldKind.BOOLEAN, "Monitoring running", Device.STATUS),
            RuleField("status.appInForeground", RuleFieldKind.BOOLEAN, "App in foreground", Device.STATUS),
            RuleField("connectivity.transportType", RuleFieldKind.TEXT, "Network transport", Device.CONNECTIVITY),
            RuleField("permissions.grantedCount", RuleFieldKind.NUMBER, "Granted permissions", Device.PERMISSIONS),
            RuleField("usage.summary.totalForegroundMs", RuleFieldKind.NUMBER, "Screen time (ms)", Device.USAGE),
            RuleField("usage.summary.topPackageName", RuleFieldKind.TEXT, "Top used package", Device.USAGE),
        )
        // @formatter:on

        fun byPath(path: String): RuleField? = ALL.firstOrNull { it.path == path }

        fun nodeOf(path: String): String = byPath(path)?.node ?: path.substringBefore('.')
    }
}

typealias RuleStateSnapshot = Map<String, Any?>

object RuleStateFlattener {
    fun flatten(
        prefix: String,
        value: Any?,
        out: MutableMap<String, Any?> = HashMap()
    ): MutableMap<String, Any?> {
        when (value) {
            is Map<*, *> -> value.forEach { (k, v) -> flatten("$prefix.$k", v, out) }
            null -> Unit
            else -> out[prefix] = value
        }
        return out
    }
}
