package com.xeg911.appcontrol.domain.usecase

import com.xeg911.appcontrol.domain.model.SentNotification
import com.xeg911.appcontrol.domain.model.SentStatus
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.appcontrol.domain.repository.PushSender
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import javax.inject.Inject

class DeviceTokenMissingException : Exception("Device has no FCM token registered")

class CancelNotificationUseCase @Inject constructor(
    private val pushSender: PushSender,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(item: SentNotification): Result<Unit> {
        val token = notificationRepository.getFcmToken(item.deviceId)
            ?: return Result.failure(DeviceTokenMissingException())

        val cancelPayload = FcmNotificationPayload(notificationId = item.id, cancelOnly = true)
        val now = System.currentTimeMillis()

        return pushSender.send(token, cancelPayload)
            .onSuccess {
                notificationRepository.upsertSent(
                    item.copy(status = SentStatus.CANCELLED, updatedAt = now, lastError = null)
                )
            }
            .onFailure {
                notificationRepository.upsertSent(
                    item.copy(
                        updatedAt = now,
                        lastError = it.message
                    )
                )
            }
    }
}
