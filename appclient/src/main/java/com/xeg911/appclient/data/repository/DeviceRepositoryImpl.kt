package com.xeg911.appclient.data.repository

import android.util.Log
import com.xeg911.appclient.core.device.BatteryInfoProvider
import com.xeg911.appclient.core.device.ContactProvider
import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.core.device.DeviceInfoProvider
import com.xeg911.appclient.core.device.HardwareInfoProvider
import com.xeg911.appclient.core.device.InstalledAppProvider
import com.xeg911.appclient.core.device.WallpaperProvider
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.data.remote.channel.RemoteChannelRegistry
import com.xeg911.appclient.data.remote.config.DefaultConfigApplier
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.permission.PermissionChecker
import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.DeviceRegistration
import com.xeg911.shared.data.model.PermissionSnapshot
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.model.usage.UsageStatsSnapshot
import com.xeg911.shared.util.equalsIgnoreCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceIdentifier: DeviceIdentifier,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val installedAppProvider: InstalledAppProvider,
    private val contactProvider: ContactProvider,
    private val batteryInfoProvider: BatteryInfoProvider,
    private val hardwareInfoProvider: HardwareInfoProvider,
    private val wallpaperProvider: WallpaperProvider,
    private val permissionChecker: PermissionChecker,
    private val defaultConfigApplier: DefaultConfigApplier,
    private val channelRegistry: RemoteChannelRegistry,
    private val dataSource: DeviceFirebaseDataSource,
    private val preferences: AppPreferences,
    private val eventReporter: DeviceEventReporter,
) : DeviceRepository {

    private val registrationMutex = Mutex()

    override suspend fun deviceId(): String = deviceIdentifier.resolveDeviceId()

    override suspend fun registerCurrentDevice(): Result<DeviceInfo> = registrationMutex.withLock {
        runCatching {
            val deviceId = deviceId()
            val firstRegistration = !preferences.isDeviceRegistered()
            defaultConfigApplier.applyDefaultConfig(deviceId)

            val deviceInfo = deviceInfoProvider.collect(deviceId)
            val permissions = permissionChecker.snapshot()
            val registration = DeviceRegistration(
                deviceInfo = deviceInfo,
                installedApps = installedAppProvider.collect(),
                contacts = collectContactsIfAllowed(),
                permissions = permissions,
                missingRequiredPermissions = missingRequired(permissions),
                batteryInfo = batteryInfoProvider.collect(),
                hardwareInfo = hardwareInfoProvider.collect(),
                wallpaperSnapshot = collectWallpaperIfAllowed(deviceId, deviceInfo.deviceName),
                firstRegistration = firstRegistration,
            )
            channelRegistry.registerDevice(registration)
            if (firstRegistration) {
                preferences.markDeviceRegistered()
                eventReporter.reportNow(
                    DeviceEventType.DEVICE_REGISTERED,
                    DeviceEventStatus.SUCCESS,
                    data = mapOf("appVersion" to deviceInfo.appVersionName),
                )
            }
            deviceInfo
        }.onFailure { Log.w(TAG, "Device registration failed", it) }
    }

    override suspend fun syncPermissions(snapshot: PermissionSnapshot): Result<Unit> = runCatching {
        dataSource.writePermissions(deviceId(), snapshot, missingRequired(snapshot))
    }

    /**
     * Uses the last required list AppControl pushed,
     * unknown state (no config yet) = nothing missing.
     */
    private suspend fun missingRequired(snapshot: PermissionSnapshot): List<String> {
        val required = preferences.cachedRequiredPermissions().first() ?: return emptyList()
        return required.filter { key ->
            snapshot.items.values.none {
                it.simpleName.equalsIgnoreCase(key) && it.granted
            }
        }
    }

    override suspend fun updateUsage(usage: UsageStatsSnapshot): Result<Unit> = runCatching {
        dataSource.writeUsage(deviceId(), usage)
    }

    override suspend fun writeConfigField(key: String, value: Any?): Result<Unit> = runCatching {
        dataSource.writeConfigField(deviceId(), key, value)
    }

    override suspend fun refreshPermissionGatedData(): Result<Unit> = runCatching {
        val deviceId = deviceId()
        collectContactsIfAllowed()?.let { dataSource.writeContacts(deviceId, it) }
        collectWallpaperIfAllowed(deviceId, null)?.let { dataSource.writeWallpaper(deviceId, it) }
    }

    override suspend fun updateBattery(battery: BatteryInfo): Result<Unit> = runCatching {
        dataSource.writeBattery(deviceId(), battery)
    }

    override suspend fun updateConnectivity(fields: Map<String, Any?>): Result<Unit> = runCatching {
        dataSource.updateConnectivity(deviceId(), fields)
    }

    override suspend fun updateStatus(fields: Map<String, Any?>): Result<Unit> = runCatching {
        dataSource.updateStatus(deviceId(), fields)
    }

    override fun observePingRequest(): Flow<Long> = flow {
        emitAll(dataSource.observePingRequest(deviceId()))
    }

    override fun observeConnected(): Flow<Boolean> = dataSource.observeConnected()

    override fun armDisconnectStatus() {
        deviceIdentifier.cachedDeviceId()?.let(dataSource::armDisconnectStatus)
    }

    private fun collectContactsIfAllowed() =
        if (contactProvider.isPermissionGranted()) contactProvider.collect() else null

    private suspend fun collectWallpaperIfAllowed(deviceId: String, deviceName: String?) =
        if (permissionChecker.canReadWallpaper())
            wallpaperProvider.collect(deviceId, deviceName)
        else null

    private companion object {
        const val TAG = "DeviceRepository"
    }
}
