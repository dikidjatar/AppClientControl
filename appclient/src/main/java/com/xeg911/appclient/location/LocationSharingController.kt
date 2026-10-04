package com.xeg911.appclient.location

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.domain.repository.LocationRepository
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionChecker
import com.xeg911.shared.data.model.DeviceConfig
import com.xeg911.shared.data.model.LocationSharingState
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.event.DeviceEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single owner of the location-sharing lifecycle. Sharing only becomes enabled through
 * [grantConsentAndStart], which is called exclusively after the user accepted the consent
 * dialog in [com.xeg911.appclient.ui.location.LocationConsentActivity].
 */
@Singleton
class LocationSharingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val permissionChecker: PermissionChecker,
    private val locationProvider: LocationProvider,
    private val locationRepository: LocationRepository,
    private val eventReporter: DeviceEventReporter,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    enum class StartReason { CONSENT, APP_LAUNCH, BOOT, REMOTE_COMMAND }

    private val _status = MutableStateFlow(LocationSharingStatus())
    val status: StateFlow<LocationSharingStatus> = _status.asStateFlow()

    private val _onDemandRequests = MutableSharedFlow<Long>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Request ids the running service must answer with a fresh fix. */
    val onDemandRequests: SharedFlow<Long> = _onDemandRequests.asSharedFlow()

    private val _latestFix = MutableStateFlow<LocationSnapshot?>(null)

    /** Last published fi, consumed by the device-side rule engine (geofencing). */
    val latestFix: StateFlow<LocationSnapshot?> = _latestFix.asStateFlow()

    @Volatile
    private var enabled = false

    @Volatile
    private var consentAt = 0L

    @Volatile
    private var serviceRunning = false

    @Volatile
    private var intervalMs = DeviceConfig.DEFAULT_LOCATION_INTERVAL_MS

    @Volatile
    private var lastUpdateAt = 0L

    @Volatile
    private var lastError = ""

    init {
        scope.launch {
            enabled = preferences.locationSharingEnabled()
            consentAt = preferences.locationConsentAt()
            refreshStatus()
        }
    }

    fun isEnabled(): Boolean = enabled

    fun isRunning(): Boolean = serviceRunning

    /**
     * Foreground service needs a visible notification and a location permission.
     */
    fun canStart(): Boolean =
        locationProvider.hasForegroundPermission() &&
                permissionChecker.isGranted(AppPermission.POST_NOTIFICATIONS)

    fun grantConsentAndStart() {
        val now = System.currentTimeMillis()
        enabled = true
        consentAt = now
        lastError = ""
        scope.launch {
            preferences.setLocationSharingEnabled(true)
            preferences.setLocationConsentAt(now)
        }
        start(StartReason.CONSENT)
    }

    /**
     * Restarts the service after boot / process start,
     * only if the user enabled sharing earlier.
     */
    fun startIfEnabled(reason: StartReason) {
        if (!enabled) return
        start(reason)
    }

    fun stopByUser() = stop("USER", disable = true)

    fun stopByRule(ruleName: String) = stop("RULE:$ruleName", disable = false)

    private fun stop(reason: String, disable: Boolean) {
        if (disable) {
            enabled = false
            scope.launch { preferences.setLocationSharingEnabled(false) }
        }
        context.stopService(Intent(context, LocationSharingService::class.java))
        eventReporter.report(
            DeviceEventType.LOCATION_SHARING_STOPPED, data = mapOf("reason" to reason)
        )
        refreshStatus()
    }

    fun requestCurrentLocation(requestId: Long = System.currentTimeMillis()): Boolean {
        if (!serviceRunning) return false
        return _onDemandRequests.tryEmit(requestId)
    }

    fun refreshStatus() {
        val fg = locationProvider.hasForegroundPermission()
        val gps = locationProvider.isLocationEnabled()
        val state = when {
            !enabled -> LocationSharingState.DISABLED
            !fg -> LocationSharingState.PERMISSION_REQUIRED
            !gps -> LocationSharingState.LOCATION_OFF
            serviceRunning -> LocationSharingState.ACTIVE
            else -> LocationSharingState.STARTING
        }
        val status = LocationSharingStatus(
            state = state,
            consentGiven = enabled,
            consentAt = consentAt,
            foregroundPermission = fg,
            backgroundPermission = locationProvider.hasBackgroundPermission(),
            locationServicesEnabled = gps,
            serviceRunning = serviceRunning,
            intervalMs = intervalMs,
            lastUpdateAt = lastUpdateAt,
            lastError = lastError,
            updatedAt = System.currentTimeMillis(),
        )
        _status.value = status
        scope.launch {
            locationRepository.publishStatus(status)
                .onFailure { Log.w(TAG, "Status publish failed", it) }
        }
    }

    private fun start(reason: StartReason) {
        if (!canStart()) {
            lastError = "permission_missing"
            Log.i(TAG, "Cannot start location sharing ($reason): permission missing")
            refreshStatus()
            return
        }
        if (serviceRunning) return
        runCatching {
            ContextCompat.startForegroundService(
                context, LocationSharingService.intent(context, reason)
            )
        }.onFailure {
            lastError = it.javaClass.simpleName
            Log.w(TAG, "startForegroundService failed ($reason)", it)
            refreshStatus()
        }
    }

    internal fun onServiceStarted() {
        serviceRunning = true
        lastError = ""
        refreshStatus()
    }

    internal fun onServiceStopped() {
        serviceRunning = false
        refreshStatus()
    }

    internal fun onIntervalChanged(ms: Long) {
        intervalMs = ms
        refreshStatus()
    }

    internal fun onLocationPublished(snapshot: LocationSnapshot) {
        _latestFix.value = snapshot
        lastUpdateAt = snapshot.capturedAt
        lastError = ""
        refreshStatus()
    }

    internal fun onError(message: String) {
        lastError = message
        refreshStatus()
    }

    private companion object {
        const val TAG = "LocationSharingController"
    }
}
