package com.xeg911.appcontrol.data.template

import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TemplateExportDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("payload") val payload: FcmNotificationPayload = FcmNotificationPayload(),
    @SerialName("updatedAt") val updatedAt: Long = 0L,
)

@Serializable
data class TemplateBundleDto(
    @SerialName("format") val format: String = FORMAT,
    @SerialName("version") val version: Int = VERSION,
    @SerialName("exportedAt") val exportedAt: Long = 0L,
    @SerialName("templates") val templates: List<TemplateExportDto> = emptyList(),
) {
    companion object {
        const val FORMAT = "appcontrol.notification-templates"
        const val VERSION = 1
    }
}

/**
 * Serializes templates to a portable JSON bundle and parses it back. Import accepts a bundle,
 * a bare array of templates, a single template, or a single raw [FcmNotificationPayload].
 */
@Singleton
class TemplateJsonCodec @Inject constructor(private val json: Json) {

    private val pretty = Json(from = json) { prettyPrint = true }

    fun export(templates: List<NotificationTemplate>): String = pretty.encodeToString(
        TemplateBundleDto(
            exportedAt = System.currentTimeMillis(),
            templates = templates.map { it.toDto() },
        )
    )

    fun import(text: String): Result<List<NotificationTemplate>> = runCatching {
        val dtos: List<TemplateExportDto> =
            when (val root = json.parseToJsonElement(text.trim())) {
                is JsonArray -> root.map { it.toTemplateDto() }
                is JsonObject if root.containsKey("templates") ->
                    json.decodeFromJsonElement(TemplateBundleDto.serializer(), root).templates

                else -> listOf(root.toTemplateDto())
            }
        dtos.filter { it.name.isNotBlank() }
            .map { it.toDomain() }
            .ifEmpty { error("No templates found in JSON") }
    }

    private fun JsonElement.toTemplateDto(): TemplateExportDto =
        if (this is JsonObject && containsKey("payload")) {
            json.decodeFromJsonElement(TemplateExportDto.serializer(), this)
        } else {
            val payload = json.decodeFromJsonElement(FcmNotificationPayload.serializer(), this)
            TemplateExportDto(
                name = payload.title
                    .ifBlank { payload.body }
                    .ifBlank { "Imported template" },
                payload = payload
            )
        }

    private fun NotificationTemplate.toDto() = TemplateExportDto(id, name, payload, updatedAt)

    private fun TemplateExportDto.toDomain() = NotificationTemplate(
        id = id.ifBlank { newTemplateId() },
        name = name.trim(),
        payload = payload.copy(notificationId = ""),
        updatedAt = updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
    )

    companion object {
        fun newTemplateId(): String =
            "tpl_${System.currentTimeMillis().toString(36)}_${(0..999).random()}"
    }
}
