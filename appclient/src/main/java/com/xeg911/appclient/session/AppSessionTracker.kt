package com.xeg911.appclient.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.firebase.FirebasePaths.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionTracker @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val eventReporter: DeviceEventReporter,
    @param:ApplicationScope private val scope: CoroutineScope,
) : DefaultLifecycleObserver {

    private val started = AtomicBoolean(false)
    private var openedAt = 0L

    private val _isInForeground = MutableStateFlow(false)
    val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@AppSessionTracker)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        openedAt = System.currentTimeMillis()
        _isInForeground.value = true
        scope.launch {
            deviceRepository.updateStatus(
                mapOf(Status.APP_IN_FOREGROUND to true, Status.LAST_APP_OPENED_AT to openedAt)
            )
            eventReporter.reportNow(DeviceEventType.APP_OPENED)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        _isInForeground.value = false
        val closedAt = System.currentTimeMillis()
        val sessionMs = (closedAt - openedAt).coerceAtLeast(0L)
        scope.launch {
            deviceRepository.updateStatus(
                mapOf(Status.APP_IN_FOREGROUND to false, Status.LAST_APP_CLOSED_AT to closedAt)
            )
            eventReporter.reportNow(
                DeviceEventType.APP_CLOSED,
                data = mapOf("sessionDurationMs" to sessionMs.toString()),
            )
        }
    }
}
