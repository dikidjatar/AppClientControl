package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.core.paging.PageSource
import com.xeg911.appcontrol.domain.model.CallbackRecord
import com.xeg911.appcontrol.domain.model.DeviceDetail
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.DeviceContact
import com.xeg911.shared.data.model.InstalledApp
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.usage.AppUsageEntry
import com.xeg911.shared.data.model.usage.UsageSummary
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeDevices(): Flow<Result<List<DeviceSnapshot>>>

    fun observeDeviceDetail(deviceId: String): Flow<Result<DeviceDetail>>

    fun observePermissions(deviceId: String): Flow<Result<List<PermissionStatus>>>

    fun observeUsageSummary(deviceId: String): Flow<Result<UsageSummary?>>

    fun appsPage(deviceId: String, namePrefix: String = ""): PageSource<InstalledApp>
    fun contactsPage(deviceId: String, namePrefix: String = ""): PageSource<DeviceContact>

    fun usagePage(deviceId: String): PageSource<AppUsageEntry>

    fun notificationsPage(deviceId: String): PageSource<CapturedNotification>

    fun callbacksPage(deviceId: String): PageSource<CallbackRecord>

    fun observeCallbacksFor(
        deviceId: String,
        notificationId: String
    ): Flow<Result<List<CallbackRecord>>>

    fun observeCallbacksOfType(
        deviceId: String,
        type: String,
        limit: Int
    ): Flow<Result<List<CallbackRecord>>>

    suspend fun clearNotifications(deviceId: String): Result<Unit>

    suspend fun deleteNotification(deviceId: String, notificationId: String): Result<Unit>

    suspend fun pingDevice(deviceId: String): Result<Unit>

    suspend fun deleteCallback(deviceId: String, key: String): Result<Unit>

    suspend fun clearCallbacks(deviceId: String): Result<Unit>
}
