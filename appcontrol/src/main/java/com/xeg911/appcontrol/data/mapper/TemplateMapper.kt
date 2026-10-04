package com.xeg911.appcontrol.data.mapper

import com.xeg911.appcontrol.data.model.TemplateEntity
import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import kotlinx.serialization.json.Json

fun NotificationTemplate.toEntity(json: Json) = TemplateEntity(
    id = id,
    name = name,
    payloadJson = json.encodeToString(payload),
    updatedAt = updatedAt,
)

fun TemplateEntity.toDomain(json: Json): NotificationTemplate? {
    val payload = runCatching {
        json.decodeFromString<FcmNotificationPayload>(payloadJson)
    }.getOrNull() ?: return null
    return NotificationTemplate(id = id, name = name, payload = payload, updatedAt = updatedAt)
}
