package com.xeg911.appcontrol.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.util.FirebaseNode
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.appcontrol.data.mapper.toDomain
import com.xeg911.appcontrol.data.mapper.toEntity
import com.xeg911.appcontrol.data.model.TemplateEntity
import com.xeg911.appcontrol.data.template.DefaultTemplates
import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.appcontrol.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val json: Json,
) : TemplateRepository {

    private val templatesRef
        get() = database.reference
            .child(FirebaseNode.NODE_NOTIFICATION_TEMPLATES)

    private val seededRef
        get() = database.reference
            .child(FirebaseNode.NODE_NOTIFICATION_TEMPLATES_META)
            .child(FIELD_DEFAULTS_SEEDED_AT)

    override fun observeTemplates(): Flow<Result<List<NotificationTemplate>>> =
        templatesRef.observeFlow().map { result ->
            result.map { snapshot ->
                snapshot.children
                    .mapNotNull { it.getValueOrNull<TemplateEntity>()?.toDomain(json) }
                    .sortedBy { it.name.lowercase() }
            }
        }

    override suspend fun saveTemplate(template: NotificationTemplate): Result<Unit> = runCatching {
        templatesRef
            .child(template.id)
            .setValue(template.toEntity(json))
            .await()
    }

    override suspend fun saveTemplates(templates: List<NotificationTemplate>): Result<Unit> =
        runCatching {
            if (templates.isEmpty()) return@runCatching
            val updates =
                templates.associate<NotificationTemplate, String, Any> { it.id to it.toEntity(json) }
            templatesRef.updateChildren(updates).await()
        }

    override suspend fun deleteTemplate(templateId: String): Result<Unit> = runCatching {
        templatesRef.child(templateId).removeValue().await()
    }

    override suspend fun seedDefaultsIfNeeded(): Result<Int> = runCatching {
        if (seededRef.get().await().exists()) return@runCatching 0
        val defaults = DefaultTemplates.all()
        saveTemplates(defaults).getOrThrow()
        seededRef.setValue(System.currentTimeMillis()).await()
        defaults.size
    }

    private companion object {
        const val FIELD_DEFAULTS_SEEDED_AT = "defaultsSeededAt"
    }
}
