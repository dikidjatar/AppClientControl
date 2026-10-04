package com.xeg911.appclient.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.notification.NotificationCapabilityProvider
import com.xeg911.shared.data.model.event.DeviceEventType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val deviceIdentifier: DeviceIdentifier,
    private val preferences: AppPreferences,
    private val dataSource: DeviceFirebaseDataSource,
    private val capabilityProvider: NotificationCapabilityProvider,
    private val eventReporter: DeviceEventReporter,
) {
    private val mutex = Mutex()

    suspend fun currentToken(): String? {
        preferences.fcmToken()?.let { return it }
        return runCatching { firebaseMessaging.token.await() }
            .onSuccess { preferences.setFcmToken(it) }
            .onFailure { Log.w(TAG, "Failed to fetch FCM token", it) }
            .getOrNull()
    }

    /**
     * Publishes the current token if it is not yet known remotely or has changed.
     */
    suspend fun ensurePublished() {
        val token = currentToken() ?: return
        publish(token, refreshed = false)
    }

    suspend fun onTokenRefreshed(token: String) {
        val previous = preferences.fcmToken()
        preferences.setFcmToken(token)
        publish(token, refreshed = previous != token)
    }

    private suspend fun publish(token: String, refreshed: Boolean): Unit = mutex.withLock {
        val deviceId = runCatching { deviceIdentifier.resolveDeviceId() }
            .onFailure { Log.w(TAG, "Cannot publish token: no deviceId", it) }
            .getOrNull() ?: return@withLock
        runCatching {
            dataSource.writeCapability(deviceId, capabilityProvider.build(token))
        }.onSuccess {
            if (refreshed) {
                eventReporter.report(
                    DeviceEventType.FCM_TOKEN_REFRESHED,
                    data = mapOf("tokenSuffix" to token.takeLast(TOKEN_SUFFIX_LENGTH)),
                )
            }
        }.onFailure { Log.w(TAG, "Failed to publish FCM token", it) }
    }

    private companion object {
        const val TAG = "FcmTokenManager"
        const val TOKEN_SUFFIX_LENGTH = 8
    }
}
