package com.xeg911.appcontrol.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.paging.PageSource
import com.xeg911.appcontrol.core.util.ConnectivityField
import com.xeg911.appcontrol.core.util.DeviceNode
import com.xeg911.appcontrol.core.util.EventField
import com.xeg911.appcontrol.core.util.FirebaseNode
import com.xeg911.appcontrol.core.util.LocationField
import com.xeg911.appcontrol.core.util.PermissionsField
import com.xeg911.appcontrol.core.util.SortField
import com.xeg911.appcontrol.core.util.UsageField
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.appcontrol.data.mapper.toCallbackRecord
import com.xeg911.appcontrol.data.mapper.toDeviceSnapshot
import com.xeg911.appcontrol.data.paging.FirebasePageSource
import com.xeg911.appcontrol.domain.model.CallbackRecord
import com.xeg911.appcontrol.domain.model.DeviceDetail
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.ConnectivityInfo
import com.xeg911.shared.data.model.DeviceConfig
import com.xeg911.shared.data.model.DeviceContact
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.DeviceStatus
import com.xeg911.shared.data.model.HardwareInfo
import com.xeg911.shared.data.model.InstalledApp
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.PermissionType
import com.xeg911.shared.data.model.WallpaperSnapshot
import com.xeg911.shared.data.model.usage.AppUsageEntry
import com.xeg911.shared.data.model.usage.UsageSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
) : DeviceRepository {

    private val indexRef
        get() = database.reference.child(FirebaseNode.NODE_DEVICE_INDEX)

    private val devicesRef
        get() = database.reference.child(FirebaseNode.NODE_DEVICES)

    private fun nodeRef(deviceId: String, node: String): DatabaseReference =
        devicesRef.child(deviceId).child(node)

    private inline fun <reified T> observeValue(
        deviceId: String,
        node: String
    ): Flow<Result<T?>> = nodeRef(deviceId, node)
        .observeFlow()
        .map { r -> r.map { it.getValueOrNull<T>() } }

    override fun observeDevices(): Flow<Result<List<DeviceSnapshot>>> =
        indexRef.observeFlow().map { result ->
            result.map { snapshot ->
                snapshot.children
                    .mapNotNull { it.toDeviceSnapshot() }
                    .sortedWith(compareByDescending<DeviceSnapshot> { it.isOnline }.thenByDescending { it.lastSeen })
            }
        }

    /**
     * Seven tiny value listeners instead of one on the whole device subtree.
     */
    override fun observeDeviceDetail(deviceId: String): Flow<Result<DeviceDetail>> =
        combine<Result<Any?>, Result<DeviceDetail>>(
            observeValue<DeviceInfo>(deviceId, DeviceNode.NODE_INFO),
            observeValue<DeviceStatus>(deviceId, DeviceNode.NODE_STATUS),
            observeValue<BatteryInfo>(deviceId, DeviceNode.NODE_BATTERY),
            observeValue<HardwareInfo>(deviceId, DeviceNode.NODE_HARDWARE),
            observeValue<ConnectivityInfo>(deviceId, DeviceNode.NODE_CONNECTIVITY),
            observeValue<DeviceConfig>(deviceId, DeviceNode.NODE_CONFIG),
            observeValue<WallpaperSnapshot>(deviceId, DeviceNode.NODE_WALLPAPER),
            nodeRef(deviceId, DeviceNode.NODE_LOCATION)
                .child(LocationField.NODE_STATUS)
                .observeFlow()
                .map { r -> r.map { it.getValueOrNull<LocationSharingStatus>() } },
        ) { parts ->
            parts.firstOrNull { it.isFailure }
                ?.let { return@combine Result.failure(it.exceptionOrNull()!!) }
            val info = parts[0].getOrNull() as? DeviceInfo
                ?: return@combine Result.failure(IllegalStateException("Device '$deviceId' not found or incomplete"))
            val status = parts[1].getOrNull() as? DeviceStatus ?: DeviceStatus()
            Result.success(
                DeviceDetail(
                    deviceId = deviceId,
                    info = info,
                    online = status.online,
                    lastSeen = status.lastSeen,
                    status = status,
                    battery = parts[2].getOrNull() as? BatteryInfo,
                    hardware = parts[3].getOrNull() as? HardwareInfo,
                    connectivity = parts[4].getOrNull() as? ConnectivityInfo,
                    config = parts[5].getOrNull() as? DeviceConfig,
                    wallpaper = parts[6].getOrNull() as? WallpaperSnapshot,
                    locationStatus = parts[7].getOrNull() as? LocationSharingStatus,
                )
            )
        }

    override fun observePermissions(deviceId: String): Flow<Result<List<PermissionStatus>>> =
        nodeRef(deviceId, DeviceNode.NODE_PERMISSIONS)
            .child(PermissionsField.NODE_ITEMS)
            .observeFlow()
            .map { result ->
                result.map { snapshot ->
                    snapshot.children
                        .mapNotNull { it.getValueOrNull<PermissionStatus>() }
                        .sortedWith(
                            compareBy<PermissionStatus> {
                                when (it.type) {
                                    PermissionType.RUNTIME -> 0
                                    PermissionType.SPECIAL -> 1
                                    PermissionType.INSTALL_TIME -> 2
                                }
                            }.thenBy { it.simpleName }
                        )
                }
            }

    override fun observeUsageSummary(deviceId: String): Flow<Result<UsageSummary?>> =
        nodeRef(deviceId, DeviceNode.NODE_USAGE)
            .child(UsageField.NODE_SUMMARY)
            .observeFlow()
            .map { r -> r.map { it.getValueOrNull<UsageSummary>() } }

    override fun appsPage(deviceId: String, namePrefix: String): PageSource<InstalledApp> =
        FirebasePageSource(
            ref = nodeRef(deviceId, DeviceNode.NODE_APPS),
            orderBy = SortField.SORT_KEY,
            prefix = namePrefix.trim().lowercase(),
            decode = { it.getValueOrNull<InstalledApp>() },
        )

    override fun contactsPage(deviceId: String, namePrefix: String): PageSource<DeviceContact> =
        FirebasePageSource(
            ref = nodeRef(deviceId, DeviceNode.NODE_CONTACTS),
            orderBy = SortField.SORT_KEY,
            prefix = namePrefix.trim().lowercase(),
            decode = { it.getValueOrNull<DeviceContact>() },
        )

    override fun usagePage(deviceId: String): PageSource<AppUsageEntry> =
        FirebasePageSource(
            ref = nodeRef(deviceId, DeviceNode.NODE_USAGE).child(UsageField.NODE_APPS),
            orderBy = UsageField.FIELD_FOREGROUND_MS,
            descending = true,
            decode = { it.getValueOrNull<AppUsageEntry>() },
        )

    override fun notificationsPage(deviceId: String): PageSource<CapturedNotification> =
        FirebasePageSource(
            ref = nodeRef(deviceId, DeviceNode.NODE_NOTIFICATIONS),
            orderBy = SortField.TIMESTAMP,
            descending = true,
            decode = { it.getValueOrNull<CapturedNotification>() },
        )

    override fun callbacksPage(deviceId: String): PageSource<CallbackRecord> =
        FirebasePageSource(
            ref = nodeRef(deviceId, DeviceNode.NODE_EVENTS),
            orderBy = SortField.TIMESTAMP,
            descending = true,
            decode = DataSnapshot::toCallbackRecord,
        )

    override fun observeCallbacksFor(
        deviceId: String,
        notificationId: String
    ): Flow<Result<List<CallbackRecord>>> =
        nodeRef(deviceId, DeviceNode.NODE_EVENTS)
            .orderByChild(EventField.FIELD_NOTIFICATION_ID)
            .equalTo(notificationId)
            .observeFlow()
            .map { r -> r.map { it.toCallbackRecords() } }

    override fun observeCallbacksOfType(
        deviceId: String,
        type: String,
        limit: Int
    ): Flow<Result<List<CallbackRecord>>> =
        nodeRef(deviceId, DeviceNode.NODE_EVENTS)
            .orderByChild(EventField.FIELD_TYPE)
            .equalTo(type)
            .limitToLast(limit)
            .observeFlow()
            .map { r -> r.map { it.toCallbackRecords() } }

    private fun DataSnapshot.toCallbackRecords() = children
        .mapNotNull { it.toCallbackRecord() }
        .sortedByDescending { it.event.timestamp }

    override suspend fun clearNotifications(deviceId: String): Result<Unit> = runCatching {
        nodeRef(deviceId, DeviceNode.NODE_NOTIFICATIONS)
            .removeValue()
            .await()
    }

    override suspend fun deleteNotification(
        deviceId: String,
        notificationId: String
    ): Result<Unit> = runCatching {
        nodeRef(deviceId, DeviceNode.NODE_NOTIFICATIONS)
            .child(notificationId)
            .removeValue()
            .await()
    }

    override suspend fun pingDevice(deviceId: String): Result<Unit> = runCatching {
        nodeRef(deviceId, DeviceNode.NODE_CONNECTIVITY)
            .child(ConnectivityField.FIELD_PING_REQUEST)
            .setValue(System.currentTimeMillis())
            .await()
    }

    override suspend fun deleteCallback(deviceId: String, key: String): Result<Unit> = runCatching {
        nodeRef(deviceId, DeviceNode.NODE_EVENTS)
            .child(key)
            .removeValue()
            .await()
    }

    override suspend fun clearCallbacks(deviceId: String): Result<Unit> = runCatching {
        nodeRef(deviceId, DeviceNode.NODE_EVENTS)
            .removeValue()
            .await()
    }
}
