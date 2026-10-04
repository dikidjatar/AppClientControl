package com.xeg911.appclient.notification

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps a server defined string notification ID to a stable Android int ID.
 */
@Singleton
class NotificationIdResolver @Inject constructor() {
    fun resolve(serverNotificationId: String): Int =
        serverNotificationId.hashCode()
            .and(Int.MAX_VALUE)
            .coerceAtLeast(1)
}