package com.xeg911.appclient.monitoring

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.core.di.IoDispatcher
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.fcm.FcmTokenManager
import com.xeg911.appclient.monitoring.watchdog.ServiceRestartWorker
import com.xeg911.appclient.permission.PermissionMonitor
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.firebase.FirebasePaths.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject
    lateinit var controller: MonitoringController

    @Inject
    lateinit var notificationFactory: MonitoringNotificationFactory

    @Inject
    lateinit var connectivityMonitor: ConnectivityMonitor

    @Inject
    lateinit var batteryMonitor: BatteryMonitor

    @Inject
    lateinit var usageStatsMonitor: UsageStatsMonitor

    @Inject
    lateinit var permissionMonitor: PermissionMonitor

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private lateinit var serviceScope: CoroutineScope
    private val heartbeatStarted = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        startAsForeground()
        controller.onServiceStarted()
        connectivityMonitor.start(serviceScope)
        batteryMonitor.start(serviceScope)
        usageStatsMonitor.start(serviceScope)
        serviceScope.launch { fcmTokenManager.ensurePublished() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason =
            intent?.getStringExtra(EXTRA_REASON) ?: MonitoringController.StartReason.WATCHDOG.name
        if (heartbeatStarted.compareAndSet(false, true)) {
            serviceScope.launch { publishStarted(reason) }
            serviceScope.launch { heartbeatLoop() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleRestartIfNeeded()
    }

    override fun onDestroy() {
        connectivityMonitor.stop()
        batteryMonitor.stop()
        usageStatsMonitor.stop()
        controller.onServiceStopped()
        serviceScope.cancel()
        appScope.launch {
            withContext(NonCancellable) {
                deviceRepository.updateStatus(mapOf(Status.MONITORING_RUNNING to false))
                eventReporter.reportNow(DeviceEventType.MONITORING_STOPPED)
            }
        }
        scheduleRestartIfNeeded()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val notification = notificationFactory.serviceNotification()
        val id = MonitoringNotificationFactory.SERVICE_NOTIFICATION_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(id, notification)
        }
    }

    private suspend fun publishStarted(reason: String) {
        deviceRepository.updateStatus(
            mapOf(
                Status.MONITORING_RUNNING to true,
                Status.LAST_MONITORING_HEARTBEAT_AT to System.currentTimeMillis()
            )
        )
        eventReporter.reportNow(
            DeviceEventType.MONITORING_STARTED,
            data = mapOf("reason" to reason)
        )
    }

    private suspend fun heartbeatLoop() {
        var beats = 0
        while (serviceScope.isActive) {
            delay(HEARTBEAT_INTERVAL_MS)
            permissionMonitor.refreshNow()
            deviceRepository.updateStatus(
                mapOf(
                    Status.MONITORING_RUNNING to true,
                    Status.LAST_MONITORING_HEARTBEAT_AT to System.currentTimeMillis()
                )
            )
            if (++beats % RUNNING_EVENT_EVERY_N_BEATS == 0) {
                eventReporter.reportNow(
                    DeviceEventType.MONITORING_RUNNING,
                    data = mapOf("uptimeMs" to (beats * HEARTBEAT_INTERVAL_MS).toString()),
                )
            }
        }
    }

    private fun scheduleRestartIfNeeded() {
        if (!controller.shouldAutoRestart()) return
        runCatching {
            ServiceRestartWorker.enqueue(
                applicationContext,
                delayMs = RESTART_DELAY_MS
            )
        }.onFailure { Log.e(TAG, "Failed to enqueue restart", it) }
    }

    companion object {
        private const val TAG = "MonitoringService"
        private const val EXTRA_REASON = "extra_start_reason"
        private const val HEARTBEAT_INTERVAL_MS = 5 * 60_000L
        private const val RUNNING_EVENT_EVERY_N_BEATS = 12  // one MONITORING_RUNNING event per hour
        private const val RESTART_DELAY_MS = 5_000L

        fun intent(context: Context, reason: MonitoringController.StartReason): Intent =
            Intent(context, MonitoringService::class.java).putExtra(EXTRA_REASON, reason.name)
    }
}
