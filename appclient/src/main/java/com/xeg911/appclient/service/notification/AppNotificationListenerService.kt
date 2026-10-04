package com.xeg911.appclient.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.core.di.IoDispatcher
import com.xeg911.appclient.domain.usecase.ForwardNotificationUseCase
import com.xeg911.appclient.permission.PermissionMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationFilter: NotificationFilter

    @Inject
    lateinit var contentFilter: NotificationContentFilter

    @Inject
    lateinit var deviceIdentifier: DeviceIdentifier

    @Inject
    lateinit var forwardNotification: ForwardNotificationUseCase

    @Inject
    lateinit var permissionMonitor: PermissionMonitor

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var serviceScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    override fun onListenerConnected() {
        permissionMonitor.refresh()
    }

    override fun onListenerDisconnected() {
        permissionMonitor.refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val source = notificationFilter.resolveSource(sbn.packageName) ?: return
        if (contentFilter.isNoise(source, NotificationMapper.extractText(sbn))) return
        serviceScope.launch {
            runCatching {
                val deviceId = deviceIdentifier.resolveDeviceId()
                forwardNotification(NotificationMapper.map(sbn, source, deviceId))
            }.onFailure { Log.w(TAG, "Failed to forward notification", it) }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AppNotificationListener"
    }
}
