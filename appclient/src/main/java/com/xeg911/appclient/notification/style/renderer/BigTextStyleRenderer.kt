package com.xeg911.appclient.notification.style.renderer

import android.content.Context
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BigTextStyleRenderer @Inject constructor() : NotificationStyleRenderer {
    override val styleId = NotificationStyleDef.BIG_TEXT.id
    override suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    ) {
        val expandedText = payload.bigText ?: payload.body
        val style = NotificationCompat.BigTextStyle()
            .bigText(expandedText)
            .also { style -> payload.summaryText?.let { summary -> style.setSummaryText(summary) } }
        builder.setStyle(style)
    }
}
