package com.xeg911.shared.data.model.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPayloadAction(
    @SerialName("id") val id: String = "",
    @SerialName("label") val label: String = "",
    @SerialName("action") val action: String = "",
    @SerialName("params") val params: Map<String, String> = emptyMap(),
    @SerialName("icon") val icon: String? = null,
    @SerialName("semantic") val semanticAction: Int = 0,
    @SerialName("allowReplies") val allowGeneratedReplies: Boolean = false
)
