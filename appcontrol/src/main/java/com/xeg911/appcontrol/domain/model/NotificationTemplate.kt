package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.notification.FcmNotificationPayload

data class NotificationTemplate(
    val id: String,
    val name: String,
    val payload: FcmNotificationPayload,
    val updatedAt: Long,
)
