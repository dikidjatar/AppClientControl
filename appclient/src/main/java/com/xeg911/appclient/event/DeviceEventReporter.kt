package com.xeg911.appclient.event

import android.util.Log
import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.data.remote.channel.RemoteChannelRegistry
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single entry point for reporting anything that happened on the device.
 */
@Singleton
class DeviceEventReporter @Inject constructor(
    private val deviceIdentifier: DeviceIdentifier,
    private val channelRegistry: RemoteChannelRegistry,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    fun report(
        type: DeviceEventType,
        status: DeviceEventStatus = DeviceEventStatus.INFO,
        data: Map<String, String> = emptyMap(),
    ) = enqueue(type, status, notificationId = "", actionId = "", data = data)

    fun reportAction(
        notificationId: String,
        actionId: String,
        status: DeviceEventStatus = DeviceEventStatus.SUCCESS,
        data: Map<String, String> = emptyMap(),
        type: DeviceEventType = DeviceEventType.NOTIFICATION_ACTION,
    ) = enqueue(type, status, notificationId, actionId, data)

    suspend fun reportNow(
        type: DeviceEventType,
        status: DeviceEventStatus = DeviceEventStatus.INFO,
        notificationId: String = "",
        actionId: String = "",
        data: Map<String, String> = emptyMap(),
    ) {
        val deviceId = runCatching { deviceIdentifier.resolveDeviceId() }
            .onFailure { Log.w(TAG, "Cannot report $type: no deviceId", it) }
            .getOrNull() ?: return
        channelRegistry.sendEvent(
            DeviceEvent(
                deviceId = deviceId,
                type = type,
                status = status,
                notificationId = notificationId,
                actionId = actionId,
                data = data,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    private fun enqueue(
        type: DeviceEventType,
        status: DeviceEventStatus,
        notificationId: String,
        actionId: String,
        data: Map<String, String>,
    ) {
        scope.launch { reportNow(type, status, notificationId, actionId, data) }
    }

    private companion object {
        const val TAG = "DeviceEventReporter"
    }
}
