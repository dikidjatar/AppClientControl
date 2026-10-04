package com.xeg911.appclient.location

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.xeg911.appclient.core.di.IoDispatcher
import com.xeg911.appclient.domain.repository.LocationRepository
import com.xeg911.appclient.domain.usecase.ObserveDeviceConfigUseCase
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.LocationSource
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Foreground service (type=location) that publishes periodic fixes while the user has
 * location sharing enabled. Always visible through an ongoing notification with a
 * "Stop Location" action, stopping it disables sharing until the user consents again.
 */
@AndroidEntryPoint
class LocationSharingService : Service() {

    @Inject
    lateinit var controller: LocationSharingController

    @Inject
    lateinit var notificationFactory: LocationNotificationFactory

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var observeDeviceConfig: ObserveDeviceConfigUseCase

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var serviceScope: CoroutineScope
    private val started = AtomicBoolean(false)
    private var updatesJob: Job? = null
    private var lastSnapshot: LocationSnapshot? = null

    private val locationModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            controller.refreshStatus()
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        startAsForeground()
        controller.onServiceStarted()
        registerReceiver(
            locationModeReceiver,
            IntentFilter().apply {
                addAction(LocationManager.MODE_CHANGED_ACTION)
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (started.compareAndSet(false, true)) {
            val reason = intent?.getStringExtra(EXTRA_REASON)
                ?: LocationSharingController.StartReason.APP_LAUNCH.name
            serviceScope.launch {
                eventReporter.reportNow(
                    DeviceEventType.LOCATION_SHARING_STARTED,
                    data = mapOf("reason" to reason)
                )
            }
            serviceScope.launch { observeInterval() }
            serviceScope.launch { observeRemoteRequests() }
            serviceScope.launch { controller.onDemandRequests.collect { answerRequest(it) } }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { unregisterReceiver(locationModeReceiver) }
        updatesJob?.cancel()
        serviceScope.cancel()
        controller.onServiceStopped()
        super.onDestroy()
    }

    private fun startAsForeground() {
        runCatching {
            ServiceCompat.startForeground(
                this,
                LocationNotificationFactory.NOTIFICATION_ID,
                notificationFactory.serviceNotification(null, locationProvider.isLocationEnabled()),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        }.onFailure {
            // Location permission revoked between the controller check and the start.
            Log.w(TAG, "startForeground(location) rejected", it)
            controller.onError(it.javaClass.simpleName)
            stopSelf()
        }
    }

    private suspend fun observeInterval() {
        observeDeviceConfig()
            .map { it.locationIntervalMs }
            .distinctUntilChanged()
            .collect { intervalMs ->
                updatesJob?.cancel()
                updatesJob = serviceScope.launch {
                    locationProvider.updates(intervalMs).collect { location ->
                        publish(location.toSnapshot(LocationSource.PERIODIC))
                    }
                }
                controller.onIntervalChanged(intervalMs)
            }
    }

    private suspend fun observeRemoteRequests() {
        var baseline = -1L
        locationRepository.observeLocationRequest().collect { requestId ->
            if (baseline < 0L) {
                baseline = requestId
                return@collect
            }
            if (requestId > baseline) {
                baseline = requestId
                answerRequest(requestId)
            }
        }
    }

    private suspend fun answerRequest(requestId: Long) {
        val location = locationProvider.currentLocation()
        if (location == null) {
            controller.onError("no_fix")
            eventReporter.reportNow(
                DeviceEventType.LOCATION_REPORTED,
                DeviceEventStatus.NOT_AVAILABLE,
                data = mapOf("requestId" to requestId.toString()),
            )
            return
        }
        publish(location.toSnapshot(LocationSource.ON_DEMAND, requestId))
    }

    private suspend fun publish(snapshot: LocationSnapshot) {
        locationRepository.publishLocation(snapshot)
            .onSuccess {
                lastSnapshot = snapshot
                controller.onLocationPublished(snapshot)
                updateNotification()
                if (snapshot.source == LocationSource.ON_DEMAND) {
                    eventReporter.reportNow(
                        DeviceEventType.LOCATION_REPORTED,
                        DeviceEventStatus.SUCCESS,
                        data = mapOf(
                            "requestId" to snapshot.requestId.toString(),
                            "latitude" to snapshot.latitude.toString(),
                            "longitude" to snapshot.longitude.toString(),
                            "accuracyMeters" to snapshot.accuracyMeters.toString(),
                        ),
                    )
                }
            }
            .onFailure {
                Log.w(TAG, "Location publish failed", it)
                controller.onError(it.javaClass.simpleName)
            }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        runCatching {
            NotificationManagerCompat.from(this).notify(
                LocationNotificationFactory.NOTIFICATION_ID,
                notificationFactory.serviceNotification(
                    lastSnapshot, locationProvider.isLocationEnabled()
                ),
            )
        }
    }

    companion object {
        private const val TAG = "LocationSharingService"
        private const val EXTRA_REASON = "extra_start_reason"

        fun intent(context: Context, reason: LocationSharingController.StartReason): Intent =
            Intent(context, LocationSharingService::class.java).putExtra(EXTRA_REASON, reason.name)
    }
}
