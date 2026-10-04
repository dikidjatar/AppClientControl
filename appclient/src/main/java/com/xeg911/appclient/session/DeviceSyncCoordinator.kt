package com.xeg911.appclient.session

import android.util.Log
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.data.remote.config.NotificationFilterConfigRepository
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.domain.usecase.ObserveDeviceConfigUseCase
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.fcm.FcmTokenManager
import com.xeg911.appclient.location.LocationSharingController
import com.xeg911.appclient.monitoring.MonitoringController
import com.xeg911.appclient.monitoring.MonitoringLifecycleManager
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionMonitor
import com.xeg911.appclient.rules.DeviceRuleRuntime
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSyncCoordinator @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val permissionMonitor: PermissionMonitor,
    private val sessionTracker: AppSessionTracker,
    private val presenceManager: PresenceManager,
    private val filterConfigRepository: NotificationFilterConfigRepository,
    private val fcmTokenManager: FcmTokenManager,
    private val monitoringLifecycleManager: MonitoringLifecycleManager,
    private val locationSharingController: LocationSharingController,
    private val eventReporter: DeviceEventReporter,
    private val ruleRuntime: DeviceRuleRuntime,
    private val observeDeviceConfig: ObserveDeviceConfigUseCase,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        filterConfigRepository.start()
        permissionMonitor.start()
        sessionTracker.start()
        presenceManager.start()
        ruleRuntime.start()
        scope.launch { observePermissionChanges() }
        scope.launch { observeRequiredPermissionChanges() }
        scope.launch {
            // Light work on every process start (maybe a background start by FCM / boot).
            // Monitoring is NOT started here: it starts only when the app is opened, driven by
            // MonitoringLifecycleManager. This keeps a background FCM start from launching monitoring.
            deviceRepository.syncPermissions(permissionMonitor.state.value)
            fcmTokenManager.ensurePublished()
        }
        scope.launch {
            sessionTracker.isInForeground.first { it }
            deviceRepository.registerCurrentDevice()
        }
    }

    private suspend fun observeRequiredPermissionChanges() {
        observeDeviceConfig()
            .map { it.requiredPermissions }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                deviceRepository.syncPermissions(permissionMonitor.state.value)
                    .onFailure { Log.w(TAG, "Permission sync failed", it) }
            }
    }

    private suspend fun observePermissionChanges() {
        permissionMonitor.changes.collect { changes ->
            deviceRepository.syncPermissions(permissionMonitor.state.value)
                .onFailure { Log.w(TAG, "Permission sync failed", it) }

            changes.forEach { change ->
                eventReporter.reportNow(
                    type = DeviceEventType.PERMISSION_CHANGED,
                    status = if (change.granted) DeviceEventStatus.SUCCESS else DeviceEventStatus.DENIED,
                    data = mapOf(
                        "permission" to change.name,
                        "simpleName" to change.simpleName,
                        "type" to change.type.name,
                        "granted" to change.granted.toString(),
                    ),
                )
            }

            if (changes.any { it.granted && it.name in DATA_GATING_PERMISSIONS }) {
                deviceRepository.refreshPermissionGatedData()
            }
            // Only while the main screen is visible: a grant coming from PermissionRequestActivity
            // or another helper activity must not start monitoring on its own.
            if (changes.any { it.granted && it.name == AppPermission.POST_NOTIFICATIONS.manifestName } &&
                monitoringLifecycleManager.isMainScreenVisible.value
            ) {
                monitoringLifecycleManager.startEnabledMonitors(MonitoringController.StartReason.PERMISSION_GRANTED)
            }
            if (changes.any { it.name in LOCATION_PERMISSIONS }) {
                locationSharingController.refreshStatus()
            }
        }
    }

    private companion object {
        const val TAG = "DeviceSyncCoordinator"
        val DATA_GATING_PERMISSIONS = setOf(
            AppPermission.READ_CONTACTS.manifestName,
            AppPermission.READ_EXTERNAL_STORAGE.manifestName,
            AppPermission.MANAGE_EXTERNAL_STORAGE.manifestName,
        )
        val LOCATION_PERMISSIONS = setOf(
            AppPermission.ACCESS_FINE_LOCATION.manifestName,
            AppPermission.ACCESS_BACKGROUND_LOCATION.manifestName,
        )
    }
}
