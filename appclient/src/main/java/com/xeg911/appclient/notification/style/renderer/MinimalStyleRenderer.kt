package com.xeg911.appclient.notification.style.renderer

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinimalStyleRenderer @Inject constructor() : NotificationStyleRenderer {
    override val styleId = NotificationStyleDef.MINIMAL.id
    override suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    ) {
        builder
            .setLargeIcon(null as Bitmap?)  // strip large icon for compact look
            .setShowWhen(false)             // hide timestamp for truly minimal appearance
    }
}
