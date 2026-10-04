package com.xeg911.appclient.data.repository

import com.xeg911.appclient.data.remote.channel.RemoteChannelRegistry
import com.xeg911.appclient.domain.repository.NotificationRepository
import com.xeg911.shared.data.model.CapturedNotification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val channelRegistry: RemoteChannelRegistry
) : NotificationRepository {
    override suspend fun forward(notification: CapturedNotification) =
        channelRegistry.sendNotification(notification)
}
