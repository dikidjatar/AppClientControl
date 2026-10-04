package com.xeg911.appclient.notification.style

import android.content.Context
import androidx.core.app.NotificationCompat
import com.xeg911.shared.data.model.notification.FcmNotificationPayload

interface NotificationStyleRenderer {
    /**
     * Must match a [com.xeg911.shared.data.model.notification.NotificationStyleDef] id value.
     */
    val styleId: String

    suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    )
}