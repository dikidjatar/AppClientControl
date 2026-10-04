package com.xeg911.appclient.notification.style.renderer

import android.content.Context
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OngoingStyleRenderer @Inject constructor() : NotificationStyleRenderer {
    override val styleId = NotificationStyleDef.ONGOING.id
    override suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    ) {
        builder
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .apply {
                payload.bigText?.let {
                    setStyle(NotificationCompat.BigTextStyle().bigText(it))
                }
            }
    }
}
