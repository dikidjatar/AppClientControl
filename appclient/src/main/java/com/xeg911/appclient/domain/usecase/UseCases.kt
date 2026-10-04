package com.xeg911.appclient.domain.usecase

import com.xeg911.appclient.domain.repository.DeviceConfigRepository
import com.xeg911.appclient.domain.repository.NotificationRepository
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.DeviceConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ForwardNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(notification: CapturedNotification) =
        notificationRepository.forward(notification)
}

class ObserveDeviceConfigUseCase @Inject constructor(
    private val repository: DeviceConfigRepository
) {
    operator fun invoke(): Flow<DeviceConfig> = repository.observeConfig()
}
