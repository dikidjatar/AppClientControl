package com.xeg911.appcontrol.data.mapper

import com.xeg911.appcontrol.data.model.OutboxEntity
import com.xeg911.appcontrol.domain.model.SentNotification
import com.xeg911.appcontrol.domain.model.SentStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import kotlinx.serialization.json.Json

fun SentNotification.toEntity(json: Json) = OutboxEntity(
    notificationId = id,
    deviceId = deviceId,
    payloadJson = json.encodeToString(payload),
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sendCount = sendCount,
    lastError = lastError,
)

fun OutboxEntity.toDomain(json: Json): SentNotification? {
    val payload = runCatching {
        json.decodeFromString<FcmNotificationPayload>(payloadJson)
    }.getOrNull() ?: return null
    return SentNotification(
        id = notificationId,
        deviceId = deviceId,
        payload = payload,
        status = SentStatus.entries.firstOrNull { it.name == status } ?: SentStatus.SENT,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sendCount = sendCount,
        lastError = lastError,
    )
}
