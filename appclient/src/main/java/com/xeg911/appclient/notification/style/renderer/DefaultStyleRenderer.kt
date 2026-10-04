package com.xeg911.appclient.notification.style.renderer

import android.content.Context
import androidx.core.app.NotificationCompat
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultStyleRenderer @Inject constructor() : NotificationStyleRenderer {
    override val styleId = NotificationStyleDef.DEFAULT.id
    override suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    ) {
        // Base builder already has title, body, icon.
        // Nothing more to add for default style.
    }
}
