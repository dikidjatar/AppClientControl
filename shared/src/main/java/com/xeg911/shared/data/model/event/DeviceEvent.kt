package com.xeg911.shared.data.model.event

enum class DeviceEventType {
    DEVICE_REGISTERED,
    APP_OPENED,
    APP_CLOSED,
    MONITORING_STARTED,
    MONITORING_RUNNING,
    MONITORING_STOPPED,
    PERMISSION_CHANGED,
    NOTIFICATION_ACTION,
    CLIPBOARD_CAPTURED,
    BOOT_COMPLETED,
    FCM_TOKEN_REFRESHED,
    LOCATION_SHARING_STARTED,
    LOCATION_SHARING_STOPPED,
    LOCATION_REPORTED,
    FILE_TRANSFER,
    RULE_TRIGGERED
}

enum class DeviceEventStatus {
    SUCCESS,
    FAILED,
    DENIED,
    CANCELLED,
    NOT_AVAILABLE,
    INFO
}

/**
 * A single fact reported by a device.
 * [notificationId] and [actionId] are only populated for [DeviceEventType.NOTIFICATION_ACTION].
 */
data class DeviceEvent(
    val deviceId: String = "",
    val type: DeviceEventType = DeviceEventType.NOTIFICATION_ACTION,
    val status: DeviceEventStatus = DeviceEventStatus.INFO,
    val notificationId: String = "",
    val actionId: String = "",
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L
)
