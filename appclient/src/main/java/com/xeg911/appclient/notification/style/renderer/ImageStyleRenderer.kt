package com.xeg911.appclient.notification.style.renderer

import android.content.Context
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.notification.icon.NotificationIconResolver
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStyleRenderer @Inject constructor(
    private val iconResolver: NotificationIconResolver
) : NotificationStyleRenderer {
    override val styleId = NotificationStyleDef.IMAGE.id
    override suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    ) {
        val bitmap = iconResolver.resolveImage(payload.image) ?: return  // fallback to default look
        val style = NotificationCompat.BigPictureStyle()
            .bigPicture(bitmap)
            .also { style -> payload.summaryText?.let { summary -> style.setSummaryText(summary) } }
        builder.setStyle(style)
    }
}
