package com.xeg911.appclient.notification.style.renderer

import android.content.Context
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressStyleRenderer @Inject constructor() : NotificationStyleRenderer {

    override val styleId: String = NotificationStyleDef.PROGRESS.id

    override suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    ) {
        val isIndeterminate = payload.indeterminate || payload.progress == null
        val currentProgress = payload.progress ?: 0
        val maxProgress = payload.progressMax.coerceAtLeast(1)   // guard divide-by-zero

        builder
            .setProgress(maxProgress, currentProgress, isIndeterminate)
            // Silence repeated alerts while the server sends update ticks
            .setOnlyAlertOnce(true)
            // Progress notifications are never auto canceled mid-flight;
            // the server explicitly cancels or replaces them.
            .setAutoCancel(false)
    }
}
