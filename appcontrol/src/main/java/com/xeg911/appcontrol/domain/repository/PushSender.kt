package com.xeg911.appcontrol.domain.repository

import com.xeg911.shared.data.model.notification.FcmNotificationPayload

interface PushSender {
    suspend fun send(token: String, payload: FcmNotificationPayload): Result<Unit>
}