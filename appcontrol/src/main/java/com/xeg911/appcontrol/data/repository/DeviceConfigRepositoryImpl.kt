package com.xeg911.appcontrol.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.util.DeviceConfigField
import com.xeg911.appcontrol.core.util.DeviceNode
import com.xeg911.appcontrol.core.util.FirebaseNode
import com.xeg911.appcontrol.core.util.TelegramConfigField
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.appcontrol.domain.model.DefaultRemoteConfig
import com.xeg911.appcontrol.domain.model.DeviceRemoteConfig
import com.xeg911.appcontrol.domain.model.TelegramChannelConfig
import com.xeg911.appcontrol.domain.repository.DeviceConfigRepository
import com.xeg911.shared.data.model.ConfigurablePermission
import com.xeg911.shared.data.model.DeviceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceConfigRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
) : DeviceConfigRepository {

    private fun configRef(deviceId: String) = database.reference
        .child(FirebaseNode.NODE_DEVICES)
        .child(deviceId)
        .child(DeviceNode.NODE_CONFIG)

    private val defaultRef: DatabaseReference
        get() = database.reference.child(FirebaseNode.NODE_DEFAULT_CONFIG)

    private fun telegramRef(deviceId: String, purpose: String) =
        configRef(deviceId).child(DeviceConfigField.NODE_TELEGRAM).child(purpose)

    override fun observeConfig(deviceId: String): Flow<Result<DeviceRemoteConfig>> =
        configRef(deviceId).observeFlow().map { result -> result.map { it.toRemoteConfig() } }

    override fun observeDefaultConfig(): Flow<Result<DefaultRemoteConfig>> =
        defaultRef.observeFlow().map { result ->
            result.map { snapshot ->
                DefaultRemoteConfig(
                    webviewUrl = snapshot.child(DeviceConfigField.FIELD_WEBVIEW_URL)
                        .getValueOrNull<String>().orEmpty(),
                    locationIntervalMs = snapshot.locationInterval(),
                    usageIntervalMs = snapshot.usageInterval(),
                    requiredPermissions = snapshot.requiredPermissionsOrNull()
                        ?: ConfigurablePermission.DEFAULT_REQUIRED,
                )
            }
        }

    private fun DataSnapshot.toRemoteConfig(): DeviceRemoteConfig {
        val telegramNode = child(DeviceConfigField.NODE_TELEGRAM)
        return DeviceRemoteConfig(
            webviewUrl = child(DeviceConfigField.FIELD_WEBVIEW_URL).getValueOrNull<String>()
                .orEmpty(),
            telegram = TelegramConfigField.PURPOSES.associateWith { purpose ->
                telegramNode.child(purpose).toTelegramConfig()
            },
            locationIntervalMs = locationInterval(),
            usageIntervalMs = usageInterval(),
            requiredPermissions = requiredPermissionsOrNull(),
        )
    }

    private fun DataSnapshot.usageInterval(): Long =
        DeviceConfig.sanitizeUsageInterval(child(DeviceConfigField.FIELD_USAGE_INTERVAL_MS).getValueOrNull<Long>())

    private fun DataSnapshot.locationInterval(): Long =
        child(DeviceConfigField.FIELD_LOCATION_INTERVAL_MS).getValueOrNull<Long>()
            ?: DeviceConfig.DEFAULT_LOCATION_INTERVAL_MS

    private fun DataSnapshot.requiredPermissionsOrNull(): List<String>? {
        val node = child(DeviceConfigField.FIELD_REQUIRED_PERMISSIONS)
        if (!node.exists()) return null
        return ConfigurablePermission.sanitize(node.children.mapNotNull { it.getValueOrNull<String>() })
    }

    private fun DataSnapshot.toTelegramConfig() = TelegramChannelConfig(
        enabled = child(TelegramConfigField.FIELD_ENABLED).getValueOrNull<Boolean>() ?: false,
        botToken = child(TelegramConfigField.FIELD_BOT_TOKEN).getValueOrNull<String>().orEmpty(),
        chatId = child(TelegramConfigField.FIELD_CHAT_ID).value?.toString().orEmpty(),
    )

    override suspend fun updateWebviewUrl(deviceId: String, url: String): Result<Unit> =
        runCatching {
            configRef(deviceId).child(DeviceConfigField.FIELD_WEBVIEW_URL).setValue(url.trim())
                .await()
        }

    override suspend fun updateTelegram(
        deviceId: String,
        purpose: String,
        config: TelegramChannelConfig,
    ): Result<Unit> = runCatching {
        telegramRef(deviceId, purpose).setValue(
            mapOf(
                TelegramConfigField.FIELD_ENABLED to config.enabled,
                TelegramConfigField.FIELD_BOT_TOKEN to config.botToken.trim(),
                TelegramConfigField.FIELD_CHAT_ID to config.chatId.trim(),
            )
        ).await()
    }

    override suspend fun updateLocationInterval(deviceId: String, intervalMs: Long): Result<Unit> =
        runCatching {
            configRef(deviceId).child(DeviceConfigField.FIELD_LOCATION_INTERVAL_MS)
                .setValue(intervalMs.coerceAtLeast(DeviceConfig.MIN_LOCATION_INTERVAL_MS))
                .await()
        }

    override suspend fun updateUsageInterval(deviceId: String, intervalMs: Long): Result<Unit> =
        runCatching {
            configRef(deviceId).child(DeviceConfigField.FIELD_USAGE_INTERVAL_MS)
                .setValue(DeviceConfig.sanitizeUsageInterval(intervalMs)).await()
        }

    override suspend fun updateDefaultUsageInterval(intervalMs: Long): Result<Unit> = runCatching {
        defaultRef.child(DeviceConfigField.FIELD_USAGE_INTERVAL_MS)
            .setValue(DeviceConfig.sanitizeUsageInterval(intervalMs)).await()
    }

    override suspend fun updateRequiredPermissions(
        deviceId: String,
        keys: List<String>?,
    ): Result<Unit> = writeRequiredPermissions(configRef(deviceId), keys)

    override suspend fun updateDefaultWebviewUrl(url: String): Result<Unit> = runCatching {
        defaultRef.child(DeviceConfigField.FIELD_WEBVIEW_URL).setValue(url.trim()).await()
    }

    override suspend fun updateDefaultLocationInterval(intervalMs: Long): Result<Unit> =
        runCatching {
            defaultRef.child(DeviceConfigField.FIELD_LOCATION_INTERVAL_MS)
                .setValue(intervalMs.coerceAtLeast(DeviceConfig.MIN_LOCATION_INTERVAL_MS))
                .await()
        }

    override suspend fun updateDefaultRequiredPermissions(keys: List<String>): Result<Unit> =
        writeRequiredPermissions(defaultRef, keys)

    private suspend fun writeRequiredPermissions(
        parent: DatabaseReference,
        keys: List<String>?,
    ): Result<Unit> = runCatching {
        val node = parent.child(DeviceConfigField.FIELD_REQUIRED_PERMISSIONS)
        val value: Any? = keys?.let { ConfigurablePermission.sanitize(it).ifEmpty { "" } }
        node.setValue(value).await()
    }
}
