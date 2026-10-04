package com.xeg911.appclient.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.MainActivity
import com.xeg911.appclient.R
import com.xeg911.shared.data.model.LocationSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ongoing notification shown for the whole time location sharing is active,
 * so the user is always aware of it and can stop it with one tap.
 */
@Singleton
class LocationNotificationFactory @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun serviceNotification(last: LocationSnapshot?, locationEnabled: Boolean): Notification {
        ensureChannel()
        val text = when {
            !locationEnabled -> context.getString(R.string.location_notification_gps_off)
            last == null -> context.getString(R.string.location_notification_waiting)
            else -> context.getString(
                R.string.location_notification_last_update,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(last.capturedAt)),
                last.accuracyMeters.toInt(),
            )
        }
        val contentIntent = if (locationEnabled) openAppIntent() else locationSettingsIntent()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.location_on_24px)
            .setContentTitle(context.getString(R.string.location_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.stop_circle_24px,
                context.getString(R.string.location_notification_action_stop),
                stopIntent(),
            )
            .build()
    }

    private fun stopIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_STOP,
        LocationSharingActionReceiver.stopIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun locationSettingsIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_SETTINGS,
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.location_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.location_channel_description) }
        )
    }

    companion object {
        const val CHANNEL_ID = "location_sharing_channel"
        const val NOTIFICATION_ID = 1003
        private const val REQUEST_STOP = 3001
        private const val REQUEST_OPEN = 3002
        private const val REQUEST_SETTINGS = 3003
    }
}
