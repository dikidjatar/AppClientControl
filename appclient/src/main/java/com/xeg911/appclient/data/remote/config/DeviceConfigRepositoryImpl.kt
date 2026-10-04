package com.xeg911.appclient.data.remote.config

import com.google.firebase.database.DataSnapshot
import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.core.firebase.observe
import com.xeg911.appclient.core.firebase.valueOrNull
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.appclient.domain.repository.DeviceConfigRepository
import com.xeg911.shared.data.model.ConfigurablePermission
import com.xeg911.shared.data.model.DeviceConfig
import com.xeg911.shared.firebase.FirebasePaths.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceConfigRepositoryImpl @Inject constructor(
    private val dataSource: DeviceFirebaseDataSource,
    private val deviceIdentifier: DeviceIdentifier,
    private val preferences: AppPreferences,
    @param:ApplicationScope private val scope: CoroutineScope,
) : DeviceConfigRepository {

    private val shared: Flow<DeviceConfig> = flow {
        val deviceId = deviceIdentifier.resolveDeviceId()
        val defaults = dataSource.defaultConfigRef().observe()
            .map<DataSnapshot, DataSnapshot?> { it }
            .onStart { emit(null) }
            .catch { emit(null) }
        emitAll(
            combine(dataSource.configRef(deviceId).observe(), defaults) { device, default ->
                DeviceConfig(
                    webviewUrl = device.child(Config.WEBVIEW_URL).valueOrNull<String>()
                        ?.takeIf { it.isNotBlank() }
                        ?: DeviceConfig.DEFAULT_WEBVIEW_URL,
                    locationIntervalMs = device.child(Config.LOCATION_INTERVAL_MS)
                        .valueOrNull<Long>()
                        ?.coerceAtLeast(DeviceConfig.MIN_LOCATION_INTERVAL_MS)
                        ?: DeviceConfig.DEFAULT_LOCATION_INTERVAL_MS,
                    usageIntervalMs = DeviceConfig.sanitizeUsageInterval(
                        device.child(Config.USAGE_INTERVAL_MS).valueOrNull<Long>()
                            ?: default?.child(Config.USAGE_INTERVAL_MS)?.valueOrNull<Long>()
                    ),
                    // Device override wins, then the global default, then the built-in default.
                    requiredPermissions = device.requiredPermissionsOrNull()
                        ?: default?.requiredPermissionsOrNull()
                        ?: ConfigurablePermission.DEFAULT_REQUIRED,
                )
            }
                .distinctUntilChanged()
                .onEach { preferences.setCachedRequiredPermissions(it.requiredPermissions) }
        )
    }
        .catch { emit(DeviceConfig()) }
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    override fun observeConfig(): Flow<DeviceConfig> = shared

    private fun DataSnapshot.requiredPermissionsOrNull(): List<String>? {
        val node = child(Config.REQUIRED_PERMISSIONS)
        if (!node.exists()) return null
        val keys = node.children.mapNotNull { it.valueOrNull<String>() }
        return ConfigurablePermission.sanitize(keys)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 30_000L
    }
}
