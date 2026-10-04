package com.xeg911.appclient.domain.repository

import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.DeviceConfig
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.PermissionSnapshot
import com.xeg911.shared.data.model.usage.UsageStatsSnapshot
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleState
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    /**
     * Resolves the stable device id (anonymous Firebase uid).
     */
    suspend fun deviceId(): String

    suspend fun registerCurrentDevice(): Result<DeviceInfo>

    suspend fun syncPermissions(snapshot: PermissionSnapshot): Result<Unit>

    suspend fun refreshPermissionGatedData(): Result<Unit>

    suspend fun updateBattery(battery: BatteryInfo): Result<Unit>

    suspend fun updateUsage(usage: UsageStatsSnapshot): Result<Unit>

    suspend fun writeConfigField(key: String, value: Any?): Result<Unit>

    suspend fun updateConnectivity(fields: Map<String, Any?>): Result<Unit>

    suspend fun updateStatus(fields: Map<String, Any?>): Result<Unit>

    fun observePingRequest(): Flow<Long>

    fun observeConnected(): Flow<Boolean>

    fun armDisconnectStatus()
}

interface NotificationRepository {
    suspend fun forward(notification: CapturedNotification)
}

interface DeviceConfigRepository {
    fun observeConfig(): Flow<DeviceConfig>
}

interface LocationRepository {
    suspend fun publishLocation(snapshot: LocationSnapshot): Result<Unit>

    suspend fun publishStatus(status: LocationSharingStatus): Result<Unit>

    fun observeLocationRequest(): Flow<Long>
}

interface RuleRepository {
    fun observeDeviceRules(): Flow<List<AutomationRule>>

    suspend fun publishState(state: RuleState): Result<Unit>

    suspend fun loadStates(): Result<Map<String, RuleState>>
}
