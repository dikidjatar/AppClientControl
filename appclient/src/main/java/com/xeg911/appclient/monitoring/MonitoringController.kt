package com.xeg911.appclient.monitoring

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place that starts/stops [MonitoringService] and knows whether it runs.
 */
@Singleton
class MonitoringController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
    private val preferences: AppPreferences,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    enum class StartReason { APP_LAUNCH, PERMISSION_GRANTED, BOOT, WATCHDOG, REMOTE_COMMAND, USER }

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val starting = AtomicBoolean(false)

    @Volatile
    private var enabled = false

    init {
        scope.launch { enabled = preferences.monitoringEnabled() }
    }

    fun canStart(): Boolean = permissionChecker.isGranted(AppPermission.POST_NOTIFICATIONS)

    fun startIfAllowed(reason: StartReason) {
        when (reason) {
            StartReason.BOOT, StartReason.WATCHDOG -> if (enabled) start(reason)
            else -> start(reason)
        }
    }

    fun start(reason: StartReason) {
        if (!canStart()) return
        setEnabled(true)
        if (_isRunning.value) return
        if (!starting.compareAndSet(false, true)) return
        runCatching {
            ContextCompat.startForegroundService(context, MonitoringService.intent(context, reason))
        }.onFailure {
            starting.set(false)
            Log.w(TAG, "startForegroundService failed ($reason)", it)
        }
    }

    fun stopByUser() {
        setEnabled(false)
        stopService()
    }

    fun restart(reason: StartReason) {
        scope.launch {
            stopService()
            isRunning.first { !it }
            start(reason)
        }
    }

    fun shouldAutoRestart(): Boolean = enabled && canStart()

    private fun stopService() {
        context.stopService(Intent(context, MonitoringService::class.java))
    }

    private fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        scope.launch { preferences.setMonitoringEnabled(value) }
    }

    internal fun onServiceStarted() {
        _isRunning.value = true
        starting.set(false)
    }

    internal fun onServiceStopped() {
        _isRunning.value = false
        starting.set(false)
    }

    private companion object {
        const val TAG = "MonitoringController"
    }
}
