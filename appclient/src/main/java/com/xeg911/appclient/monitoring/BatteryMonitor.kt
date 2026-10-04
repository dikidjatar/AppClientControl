package com.xeg911.appclient.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.xeg911.appclient.core.device.BatteryInfoProvider
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.shared.data.model.BatteryInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val batteryInfoProvider: BatteryInfoProvider,
    private val deviceRepository: DeviceRepository,
) {
    private val ticks = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var receiver: BroadcastReceiver? = null

    private val _latest = MutableStateFlow<BatteryInfo?>(null)

    val latest: StateFlow<BatteryInfo?> = _latest.asStateFlow()

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        stop()
        ticks
            .debounce(DEBOUNCE_MS)
            .map { batteryInfoProvider.collect() }
            .distinctUntilChanged { old, new -> old.copy(capturedAt = 0) == new.copy(capturedAt = 0) }
            .onEach { info ->
                _latest.value = info
                deviceRepository.updateBattery(info)
            }
            .launchIn(scope)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                ticks.tryEmit(Unit)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ticks.tryEmit(Unit)
    }

    fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }

    private companion object {
        const val DEBOUNCE_MS = 2_000L
    }
}
