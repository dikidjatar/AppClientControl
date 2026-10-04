package com.xeg911.appcontrol.domain.usecase

import com.xeg911.appcontrol.domain.model.NotificationTarget
import com.xeg911.appcontrol.domain.model.SendReport
import com.xeg911.appcontrol.domain.model.SentNotification
import com.xeg911.appcontrol.domain.model.SentStatus
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.appcontrol.domain.repository.PushSender
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import javax.inject.Inject

/**
 * Sends [FcmNotificationPayload] to every target and records the result in
 * the outbox of each known device. Sending the same notificationId again
 * updates the existing outbox record (and the notification on the device).
 */
class SendNotificationUseCase @Inject constructor(
    private val pushSender: PushSender,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(
        payload: FcmNotificationPayload,
        targets: List<NotificationTarget>,
    ): SendReport {
        val delivered = mutableListOf<NotificationTarget>()
        val failures = linkedMapOf<NotificationTarget, String>()

        targets.forEach { target ->
            pushSender.send(target.token, payload)
                .onSuccess { delivered += target }
                .onFailure { failures[target] = it.message ?: it.javaClass.simpleName }
        }

        val now = System.currentTimeMillis()
        targets.mapNotNull { it.deviceId }.distinct().forEach { deviceId ->
            val error = failures.entries.firstOrNull { it.key.deviceId == deviceId }?.value
            val existing = notificationRepository.getSent(deviceId, payload.notificationId)
            notificationRepository.upsertSent(
                SentNotification(
                    id = payload.notificationId,
                    deviceId = deviceId,
                    payload = payload,
                    status = if (error == null) SentStatus.SENT else SentStatus.FAILED,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    sendCount = (existing?.sendCount ?: 0) + 1,
                    lastError = error,
                )
            )
        }
        return SendReport(delivered, failures)
    }
}