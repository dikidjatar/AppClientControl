package com.xeg911.appclient.data.remote.config

import android.util.Log
import com.xeg911.appclient.core.firebase.getAwait
import com.xeg911.appclient.core.firebase.setValueAwait
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.shared.firebase.FirebasePaths
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies keys from /defaultConfig into devices/{id}/config
 * that the device does not have yet.
 */
@Singleton
class DefaultConfigApplier @Inject constructor(
    private val dataSource: DeviceFirebaseDataSource
) {
    suspend fun applyDefaultConfig(deviceId: String) {
        val applied = withTimeoutOrNull(TIMEOUT_MS) {
            runCatching { merge(deviceId) }
                .onFailure { Log.w(TAG, "applyDefaultConfig failed for $deviceId", it) }
                .isSuccess
        }
        if (applied == null) Log.w(TAG, "applyDefaultConfig timed out for $deviceId")
    }

    private suspend fun merge(deviceId: String) {
        val defaults = dataSource.defaultConfigRef().getAwait()
        if (!defaults.hasChildren()) return
        val deviceConfigRef = dataSource.configRef(deviceId)
        val current = deviceConfigRef.getAwait()
        defaults.children
            .filter { child -> child.key?.let { it !in INHERITED_KEYS && !current.hasChild(it) } == true }
            .forEach { child -> deviceConfigRef.child(child.key!!).setValueAwait(child.value) }
    }

    private companion object {
        const val TAG = "DefaultConfigApplier"
        const val TIMEOUT_MS = 15_000L
        val INHERITED_KEYS = setOf(FirebasePaths.Config.REQUIRED_PERMISSIONS)
    }
}
