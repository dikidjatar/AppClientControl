package com.xeg911.appclient.monitoring

import com.xeg911.appclient.core.device.UsageStatsProvider
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.domain.usecase.ObserveDeviceConfigUseCase
import com.xeg911.shared.data.model.DeviceConfig
import com.xeg911.shared.data.model.usage.UsageStatsSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsMonitor @Inject constructor(
    private val usageStatsProvider: UsageStatsProvider,
    private val deviceRepository: DeviceRepository,
    private val observeDeviceConfig: ObserveDeviceConfigUseCase,
) {
    private val _latest = MutableStateFlow<UsageStatsSnapshot?>(null)
    val latest: StateFlow<UsageStatsSnapshot?> = _latest.asStateFlow()

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        stop()
        job = scope.launch {
            observeDeviceConfig()
                .map { it.usageIntervalMs }
                .distinctUntilChanged()
                .collectLatest { intervalMs ->
                    if (intervalMs == DeviceConfig.USAGE_DISABLED) return@collectLatest
                    while (true) {
                        syncNow()
                        delay(intervalMs)
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun syncNow(): Result<Unit> {
        val snapshot = usageStatsProvider.collect()
        _latest.value = snapshot
        return deviceRepository.updateUsage(snapshot)
    }
}
