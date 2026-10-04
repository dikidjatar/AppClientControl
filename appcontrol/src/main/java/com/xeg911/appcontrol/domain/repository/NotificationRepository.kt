package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.domain.model.DeviceTargetOption
import com.xeg911.appcontrol.domain.model.SentNotification
import com.xeg911.shared.data.model.notification.DeviceNotificationCapability
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    fun observeCapability(deviceId: String): Flow<Result<DeviceNotificationCapability?>>

    suspend fun getFcmToken(deviceId: String): String?

    fun observeDeviceTargets(): Flow<Result<List<DeviceTargetOption>>>

    fun observeOutbox(deviceId: String): Flow<Result<List<SentNotification>>>

    suspend fun getSent(deviceId: String, notificationId: String): SentNotification?

    suspend fun upsertSent(item: SentNotification): Result<Unit>

    suspend fun deleteSent(deviceId: String, notificationId: String): Result<Unit>

    suspend fun clearOutbox(deviceId: String): Result<Unit>
}
