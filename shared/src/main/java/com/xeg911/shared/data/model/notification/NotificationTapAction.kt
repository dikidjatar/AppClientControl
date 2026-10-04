package com.xeg911.shared.data.model.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Defines what happens when the user taps the notification body
 */
@Serializable
data class NotificationTapAction(
    @SerialName("action") val action: String = NotificationActionDef.OPEN_APP.id,
    @SerialName("params") val params: Map<String, String> = emptyMap()
)
