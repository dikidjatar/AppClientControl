package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.notification.FcmNotificationPayload

enum class SentStatus { SENT, FAILED, CANCELLED }

data class SentNotification(
    val id: String,
    val deviceId: String,
    val payload: FcmNotificationPayload,
    val status: SentStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val sendCount: Int = 0,
    val lastError: String? = null,
)
