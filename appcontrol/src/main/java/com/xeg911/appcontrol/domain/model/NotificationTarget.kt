package com.xeg911.appcontrol.domain.model

data class NotificationTarget(
    val token: String,
    val deviceId: String? = null,
    val label: String = "",
)

/**
 * A registered device that can be picked as a target in the composer.
 */
data class DeviceTargetOption(
    val deviceId: String,
    val label: String,
    val fcmToken: String,
    val isOnline: Boolean,
) {
    val hasToken: Boolean get() = fcmToken.isNotBlank()

    fun toTarget() = NotificationTarget(token = fcmToken, deviceId = deviceId, label = label)
}

data class SendReport(
    val delivered: List<NotificationTarget>,
    val failures: Map<NotificationTarget, String>,
) {
    val total: Int get() = delivered.size + failures.size
    val isFullSuccess: Boolean get() = failures.isEmpty() && delivered.isNotEmpty()
}
