package com.xeg911.shared.data.model.notification

/**
 * Metadata for a single notification style that app supports.
 *
 * Written to Firebase as part of [DeviceNotificationCapability] so AppControl
 * knows not only which styles exist but also which payload fields each style
 * actually uses. This prevents AppControl from sending fields the current
 * renderer ignores.
 */
data class SupportedStyleDef(
    /**
     * Must match [NotificationStyleDef.id].
     */
    val id: String = "",
    val description: String = "",
    /**
     * Payload fields consumed by this renderer.
     */
    val usedFields: List<String> = emptyList()
)

/**
 * Metadata for a single notification action that AppClient supports.
 *
 * Written to Firebase as part of [DeviceNotificationCapability].
 * [requiredParams] and [optionalParams] tell AppControl which params to include
 * in the FCM payload when constructing an action of this type.
 */
data class SupportedActionDef(
    /**
     * Must match [NotificationActionDef.id].
     */
    val id: String = "",
    val description: String = "",
    /**
     * Params that MUST be present for the action to work.
     */
    val requiredParams: List<String> = emptyList(),
    /**
     * Params that are optional but recognized by the handler.
     */
    val optionalParams: List<String> = emptyList()
)

data class DeviceNotificationCapability(
    val schemaVersion: Int = 1,
    val supportedStyles: List<SupportedStyleDef> = emptyList(),
    val supportedActions: List<SupportedActionDef> = emptyList(),
    val fcmToken: String = "",
    val updatedAt: Long = 0L
)
