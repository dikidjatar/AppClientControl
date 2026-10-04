package com.xeg911.appclient.session

import android.util.Log
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.monitoring.MonitoringController
import com.xeg911.shared.firebase.FirebasePaths.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceManager @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val monitoringController: MonitoringController,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            deviceRepository.observeConnected()
                .onEach { connected -> if (connected) onConnected() }
                .catch { Log.w(TAG, "Presence listener failed", it) }
                .collect()
        }
    }

    private suspend fun onConnected() {
        deviceRepository.deviceId()
        deviceRepository.armDisconnectStatus()
        deviceRepository.updateStatus(
            mapOf(
                Status.ONLINE to true,
                Status.MONITORING_RUNNING to monitoringController.isRunning.value,
            )
        )
    }

    private companion object {
        const val TAG = "PresenceManager"
    }
}
