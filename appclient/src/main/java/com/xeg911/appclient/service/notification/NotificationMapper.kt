package com.xeg911.appclient.service.notification

import android.service.notification.StatusBarNotification
import com.xeg911.shared.data.model.CapturedNotification
import java.util.UUID

object NotificationMapper {

    fun map(
        statusBarNotification: StatusBarNotification,
        source: String,
        deviceId: String
    ): CapturedNotification {
        return CapturedNotification(
            id = UUID.randomUUID().toString(),
            deviceId = deviceId,
            source = source,
            packageName = statusBarNotification.packageName,
            title = extractTitle(statusBarNotification),
            text = extractText(statusBarNotification),
            timestamp = statusBarNotification.postTime
        )
    }

    fun extractTitle(statusBarNotification: StatusBarNotification): String {
        return statusBarNotification.notification.extras
            .getCharSequence("android.title")
            ?.toString()
            .orEmpty()
    }

    fun extractText(statusBarNotification: StatusBarNotification): String {
        return statusBarNotification.notification.extras
            .getCharSequence("android.text")
            ?.toString()
            .orEmpty()
    }
}
