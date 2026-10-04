package com.xeg911.appcontrol.domain.usecase

import android.content.Context
import android.net.Uri
import com.xeg911.appcontrol.data.template.TemplateJsonCodec
import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.appcontrol.domain.repository.TemplateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TemplateImportReport(val imported: Int, val replaced: Int)

class ImportTemplatesUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: TemplateRepository,
    private val codec: TemplateJsonCodec,
) {
    suspend fun fromText(
        text: String,
        existing: List<NotificationTemplate>
    ): Result<TemplateImportReport> =
        codec.import(text).mapCatching { parsed ->
            val byId = existing.associateBy { it.id }
            val byName = existing.associateBy { it.name.lowercase() }
            var replaced = 0
            val resolved = parsed.map { template ->
                val match = byId[template.id] ?: byName[template.name.lowercase()]
                if (match != null) replaced++
                template.copy(id = match?.id ?: template.id, updatedAt = System.currentTimeMillis())
            }.distinctBy { it.id }
            repository.saveTemplates(resolved).getOrThrow()
            TemplateImportReport(imported = resolved.size, replaced = replaced)
        }

    suspend fun fromUri(
        uri: Uri,
        existing: List<NotificationTemplate>
    ): Result<TemplateImportReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error("Cannot open file")
            }
        }.fold(onSuccess = { fromText(it, existing) }, onFailure = { Result.failure(it) })
}

class ExportTemplatesUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val codec: TemplateJsonCodec,
) {
    fun toJson(templates: List<NotificationTemplate>): String = codec.export(templates)

    suspend fun toUri(uri: Uri, templates: List<NotificationTemplate>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()
                    ?.use { it.write(toJson(templates)) }
                    ?: error("Cannot write file")
            }
        }

    companion object {
        const val MIME_JSON = "application/json"

        fun fileName(templateName: String? = null): String {
            val base = templateName?.replace(Regex("[^A-Za-z0-9._-]+"), "_")?.trim('_')
                ?.takeIf { it.isNotBlank() } ?: "notification_templates"
            return "$base.json"
        }
    }
}
