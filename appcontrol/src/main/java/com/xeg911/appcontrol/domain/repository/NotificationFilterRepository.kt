package com.xeg911.appcontrol.domain.repository

import com.xeg911.shared.data.model.notification.NotificationSourceDef
import kotlinx.coroutines.flow.Flow

interface NotificationFilterRepository {
    fun observeSources(): Flow<Result<List<NotificationSourceDef>>>

    suspend fun upsertSource(source: NotificationSourceDef): Result<Unit>

    suspend fun setEnabled(sourceId: String, enabled: Boolean): Result<Unit>

    suspend fun deleteSource(sourceId: String): Result<Unit>
}
