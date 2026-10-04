package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.domain.model.DefaultRemoteConfig
import com.xeg911.appcontrol.domain.model.DeviceRemoteConfig
import com.xeg911.appcontrol.domain.model.TelegramChannelConfig
import kotlinx.coroutines.flow.Flow

interface DeviceConfigRepository {
    fun observeConfig(deviceId: String): Flow<Result<DeviceRemoteConfig>>

    suspend fun updateWebviewUrl(deviceId: String, url: String): Result<Unit>

    suspend fun updateTelegram(
        deviceId: String,
        purpose: String,
        config: TelegramChannelConfig,
    ): Result<Unit>

    suspend fun updateLocationInterval(deviceId: String, intervalMs: Long): Result<Unit>

    suspend fun updateUsageInterval(deviceId: String, intervalMs: Long): Result<Unit>

    suspend fun updateDefaultUsageInterval(intervalMs: Long): Result<Unit>

    suspend fun updateRequiredPermissions(deviceId: String, keys: List<String>?): Result<Unit>

    fun observeDefaultConfig(): Flow<Result<DefaultRemoteConfig>>

    suspend fun updateDefaultWebviewUrl(url: String): Result<Unit>

    suspend fun updateDefaultLocationInterval(intervalMs: Long): Result<Unit>

    suspend fun updateDefaultRequiredPermissions(keys: List<String>): Result<Unit>
}
