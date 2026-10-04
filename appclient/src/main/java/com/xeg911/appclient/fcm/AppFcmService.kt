package com.xeg911.appclient.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.fcm.autoexec.AutoExecuteContext
import com.xeg911.appclient.fcm.autoexec.AutoExecuteCoordinator
import com.xeg911.appclient.notification.NotificationDispatcher
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class AppFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationDispatcher: NotificationDispatcher

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var autoExecuteCoordinator: AutoExecuteCoordinator

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val payloadJson = data[KEY_PAYLOAD] ?: return
        appScope.launch {
            runCatching {
                val payload = json.decodeFromString<FcmNotificationPayload>(payloadJson)
                autoExecuteCoordinator.executeAll(AutoExecuteContext(payload, data))
                notificationDispatcher.dispatch(applicationContext, payload)
            }.onFailure { Log.e(TAG, "Failed to dispatch FCM notification", it) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onNewToken(token: String) {
        appScope.launch { fcmTokenManager.onTokenRefreshed(token) }
    }

    private companion object {
        const val TAG = "AppFcmService"
        const val KEY_PAYLOAD = "payload"
    }
}
