package com.xeg911.appclient.monitoring.watchdog

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xeg911.appclient.monitoring.MonitoringController
import com.xeg911.appclient.monitoring.MonitoringNotificationFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WatchdogEntryPoint {
    fun monitoringController(): MonitoringController
    fun notificationFactory(): MonitoringNotificationFactory
}

private fun Context.watchdogEntryPoint(): WatchdogEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WatchdogEntryPoint::class.java)

/**
 * Periodic guard: re launches the monitoring service if it died and is allowed to run.
 */
class ServiceWatchdogWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val controller = applicationContext.watchdogEntryPoint().monitoringController()
        if (controller.shouldAutoRestart() && !controller.isRunning.value) {
            ServiceRestartWorker.enqueue(applicationContext, delayMs = 0L)
        }
        return Result.success()
    }
}

/**
 * One shot expedited restart, used after onDestroy/onTaskRemoved and by the watchdog.
 */
class ServiceRestartWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = applicationContext.watchdogEntryPoint()
        val controller = entryPoint.monitoringController()
        if (!controller.shouldAutoRestart()) return Result.success()
        runCatching { setForeground(entryPoint.notificationFactory().watchdogForegroundInfo()) }
        if (controller.isRunning.value) return Result.success()
        controller.start(MonitoringController.StartReason.WATCHDOG)
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        applicationContext.watchdogEntryPoint().notificationFactory().watchdogForegroundInfo()

    companion object {
        private const val WORK_NAME = "service_one_time_restart"

        fun enqueue(context: Context, delayMs: Long) {
            val request = OneTimeWorkRequestBuilder<ServiceRestartWorker>()
                .apply { if (delayMs > 0) setInitialDelay(delayMs, TimeUnit.MILLISECONDS) }
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}

@Singleton
class ServiceWatchdogScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedule() {
        val request =
            PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_MINUTES, TimeUnit.MINUTES)
                .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "service_watchdog_v2"
        const val INTERVAL_MINUTES = 15L
        const val BACKOFF_MINUTES = 5L
    }
}
