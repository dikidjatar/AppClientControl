package com.xeg911.appclient.monitoring

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.xeg911.appclient.location.LocationSharingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringLifecycleManager @Inject constructor(
    private val monitoringController: MonitoringController,
    private val locationSharingController: LocationSharingController,
) : DefaultLifecycleObserver {

    private val _isMainScreenVisible = MutableStateFlow(false)

    val isMainScreenVisible: StateFlow<Boolean> = _isMainScreenVisible.asStateFlow()

    fun bindMainScreen(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        _isMainScreenVisible.value = true
        startEnabledMonitors(MonitoringController.StartReason.APP_LAUNCH)
    }

    override fun onStop(owner: LifecycleOwner) {
        _isMainScreenVisible.value = false
    }

    fun startEnabledMonitors(reason: MonitoringController.StartReason) {
        monitoringController.startIfAllowed(reason)
        locationSharingController.startIfEnabled(reason.toLocationReason())
    }

    private fun MonitoringController.StartReason.toLocationReason(): LocationSharingController.StartReason =
        when (this) {
            MonitoringController.StartReason.BOOT -> LocationSharingController.StartReason.BOOT
            else -> LocationSharingController.StartReason.APP_LAUNCH
        }
}
