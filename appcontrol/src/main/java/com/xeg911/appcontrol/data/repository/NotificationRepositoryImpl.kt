package com.xeg911.appcontrol.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.util.DeviceNode
import com.xeg911.appcontrol.core.util.FirebaseNode
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.appcontrol.data.mapper.toDeviceSnapshot
import com.xeg911.appcontrol.data.mapper.toDomain
import com.xeg911.appcontrol.data.mapper.toEntity
import com.xeg911.appcontrol.data.model.OutboxEntity
import com.xeg911.appcontrol.domain.model.DeviceTargetOption
import com.xeg911.appcontrol.domain.model.SentNotification
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.shared.data.model.notification.DeviceNotificationCapability
import com.xeg911.shared.util.toSafeFirebaseKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val json: Json,
) : NotificationRepository {

    private val devicesRef get() = database.reference.child(FirebaseNode.NODE_DEVICES)

    private fun capabilityRef(deviceId: String) = devicesRef
        .child(deviceId)
        .child(DeviceNode.NODE_CAPABILITIES)
        .child(DeviceNode.NODE_NOTIFICATION_CAPABILITY)

    private fun outboxRef(deviceId: String) = devicesRef
        .child(deviceId)
        .child(DeviceNode.NODE_OUTBOX)

    private fun outboxItemRef(deviceId: String, notificationId: String) =
        outboxRef(deviceId).child(notificationId.toSafeFirebaseKey())

    override fun observeCapability(deviceId: String): Flow<Result<DeviceNotificationCapability?>> =
        capabilityRef(deviceId).observeFlow().map { result ->
            result.map { it.getValueOrNull<DeviceNotificationCapability>() }
        }

    override suspend fun getFcmToken(deviceId: String): String? =
        runCatching {
            capabilityRef(deviceId).child(FIELD_FCM_TOKEN).get().await()
                .getValue(String::class.java)
        }.getOrNull()?.takeIf { it.isNotBlank() }

    override fun observeDeviceTargets(): Flow<Result<List<DeviceTargetOption>>> =
        database.reference
            .child(FirebaseNode.NODE_DEVICE_INDEX)
            .observeFlow()
            .map { result ->
                result.map { snapshot ->
                    snapshot.children
                        .mapNotNull { it.toDeviceSnapshot() }
                        .map { DeviceTargetOption(it.deviceId, it.label, it.fcmToken, it.isOnline) }
                        .sortedWith(compareByDescending<DeviceTargetOption> { it.isOnline }.thenBy { it.label })
                }
            }

    override fun observeOutbox(deviceId: String): Flow<Result<List<SentNotification>>> =
        outboxRef(deviceId)
            .orderByChild(FIELD_UPDATED_AT)
            .limitToLast(OUTBOX_LIMIT)
            .observeFlow()
            .map { result ->
                result.map { snapshot ->
                    snapshot.children
                        .mapNotNull { it.getValueOrNull<OutboxEntity>()?.toDomain(json) }
                        .sortedByDescending { it.updatedAt }
                }
            }

    override suspend fun getSent(
        deviceId: String,
        notificationId: String
    ): SentNotification? = runCatching {
        outboxItemRef(deviceId, notificationId).get().await()
            .getValueOrNull<OutboxEntity>()?.toDomain(json)
    }.getOrNull()

    override suspend fun upsertSent(item: SentNotification): Result<Unit> = runCatching {
        outboxItemRef(item.deviceId, item.id)
            .setValue(item.toEntity(json))
            .await()
    }

    override suspend fun deleteSent(deviceId: String, notificationId: String): Result<Unit> =
        runCatching { outboxItemRef(deviceId, notificationId).removeValue().await() }

    override suspend fun clearOutbox(deviceId: String): Result<Unit> =
        runCatching { outboxRef(deviceId).removeValue().await() }

    private companion object {
        const val FIELD_FCM_TOKEN = "fcmToken"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val OUTBOX_LIMIT = 100
    }
}