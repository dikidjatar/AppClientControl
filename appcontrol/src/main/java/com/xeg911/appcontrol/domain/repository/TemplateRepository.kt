package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.domain.model.NotificationTemplate
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<Result<List<NotificationTemplate>>>

    suspend fun saveTemplate(template: NotificationTemplate): Result<Unit>

    suspend fun saveTemplates(templates: List<NotificationTemplate>): Result<Unit>

    suspend fun deleteTemplate(templateId: String): Result<Unit>

    suspend fun seedDefaultsIfNeeded(): Result<Int>
}
