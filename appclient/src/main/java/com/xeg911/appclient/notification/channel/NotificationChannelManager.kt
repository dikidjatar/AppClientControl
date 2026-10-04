package com.xeg911.appclient.notification.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor() {

    fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        priority: String = "DEFAULT"
    ): String {
        if (NotificationManagerCompat.from(context)
                .getNotificationChannelCompat(channelId) == null
        ) {
            val channel =
                NotificationChannel(channelId, channelName, mapImportance(priority)).apply {
                    description = "FCM remote channel: $channelName"
                }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
        return channelId
    }

    fun deleteChannel(context: Context, channelId: String) {
        context.getSystemService(NotificationManager::class.java)
            ?.deleteNotificationChannel(channelId)
    }

    private fun mapImportance(priority: String): Int = when (priority.uppercase()) {
        "MAX" -> NotificationManager.IMPORTANCE_MAX
        "HIGH" -> NotificationManager.IMPORTANCE_HIGH
        "LOW" -> NotificationManager.IMPORTANCE_LOW
        "MIN" -> NotificationManager.IMPORTANCE_MIN
        else -> NotificationManager.IMPORTANCE_DEFAULT
    }
}