package com.xeg911.appclient.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.xeg911.appclient.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringNotificationFactory @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun serviceNotification(): Notification = base(R.string.service_monitoring_notification_title)
        .setOngoing(true)
        .build()

    fun watchdogForegroundInfo(): ForegroundInfo {
        val notification = base(R.string.service_watchdog_notification_title).build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                WATCHDOG_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            ForegroundInfo(WATCHDOG_NOTIFICATION_ID, notification)
        }
    }

    private fun base(titleRes: Int): NotificationCompat.Builder {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(titleRes))
            .setSmallIcon(R.drawable.round_notifications_24)
            .setSilent(true)
            .setShowWhen(false)
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.service_monitoring_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            )
        )
    }

    companion object {
        const val CHANNEL_ID = "monitoring_service_channel"
        const val SERVICE_NOTIFICATION_ID = 1001
        const val WATCHDOG_NOTIFICATION_ID = 1002
    }
}
