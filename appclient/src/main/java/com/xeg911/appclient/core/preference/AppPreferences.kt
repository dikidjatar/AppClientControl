package com.xeg911.appclient.core.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore("app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val data: Flow<Preferences> = context.appDataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    suspend fun isDeviceRegistered(): Boolean = read(KEY_DEVICE_REGISTERED, false)
    suspend fun markDeviceRegistered() = write(KEY_DEVICE_REGISTERED, true)

    suspend fun fcmToken(): String? = read(KEY_FCM_TOKEN, "").ifBlank { null }
    suspend fun setFcmToken(token: String) = write(KEY_FCM_TOKEN, token)

    suspend fun cachedNotificationSources(): String? =
        read(KEY_NOTIFICATION_SOURCES, "").ifBlank { null }

    suspend fun setCachedNotificationSources(json: String) = write(KEY_NOTIFICATION_SOURCES, json)

    fun cachedRequiredPermissions(): Flow<List<String>?> = data.map { prefs ->
        prefs[KEY_REQUIRED_PERMISSIONS]?.split(',')?.filter { it.isNotBlank() }
    }

    suspend fun setCachedRequiredPermissions(keys: List<String>) =
        write(KEY_REQUIRED_PERMISSIONS, keys.joinToString(","))

    suspend fun permissionRequestCount(permission: String): Int =
        read(intPreferencesKey("$PERMISSION_REQUEST_PREFIX$permission"), 0)

    suspend fun incrementPermissionRequestCount(permission: String) {
        val key = intPreferencesKey("$PERMISSION_REQUEST_PREFIX$permission")
        context.appDataStore.edit { it[key] = (it[key] ?: 0) + 1 }
    }

    /**
     * True once monitoring has been legitimately started (app opened, remote command, user),
     * cleared when the user stops it. Boot/watchdog only resurrect monitoring when this is true.
     */
    suspend fun monitoringEnabled(): Boolean = read(KEY_MONITORING_ENABLED, false)
    suspend fun setMonitoringEnabled(enabled: Boolean) = write(KEY_MONITORING_ENABLED, enabled)

    /**
     * True only after the user accepted the consent dialog and has not stopped sharing since.
     */
    suspend fun locationSharingEnabled(): Boolean = read(KEY_LOCATION_SHARING_ENABLED, false)
    suspend fun setLocationSharingEnabled(enabled: Boolean) =
        write(KEY_LOCATION_SHARING_ENABLED, enabled)

    suspend fun locationConsentAt(): Long = read(KEY_LOCATION_CONSENT_AT, 0L)
    suspend fun setLocationConsentAt(at: Long) = write(KEY_LOCATION_CONSENT_AT, at)

    suspend fun locationConsentPrompted(): Boolean = read(KEY_LOCATION_CONSENT_PROMPTED, false)
    suspend fun markLocationConsentPrompted() = write(KEY_LOCATION_CONSENT_PROMPTED, true)

    /**
     * [com.xeg911.shared.data.model.AppIconStyle] id last applied, null = default.
     */
    suspend fun launcherIconStyle(): String? = read(KEY_LAUNCHER_ICON_STYLE, "").ifBlank { null }
    suspend fun setLauncherIconStyle(styleId: String) = write(KEY_LAUNCHER_ICON_STYLE, styleId)

    private suspend fun <T> read(key: Preferences.Key<T>, default: T): T =
        data.map { it[key] ?: default }.first()

    private suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        context.appDataStore.edit { it[key] = value }
    }

    private companion object {
        val KEY_DEVICE_REGISTERED = booleanPreferencesKey("device_registered")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_registration_token")
        val KEY_NOTIFICATION_SOURCES = stringPreferencesKey("notification_filter_sources_v1")
        val KEY_REQUIRED_PERMISSIONS = stringPreferencesKey("required_permissions_v1")
        val KEY_MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val KEY_LOCATION_SHARING_ENABLED = booleanPreferencesKey("location_sharing_enabled")
        val KEY_LOCATION_CONSENT_AT = longPreferencesKey("location_consent_at")
        val KEY_LOCATION_CONSENT_PROMPTED = booleanPreferencesKey("location_consent_prompted")
        val KEY_LAUNCHER_ICON_STYLE = stringPreferencesKey("launcher_icon_style")
        const val PERMISSION_REQUEST_PREFIX = "permission_request_count_"
    }
}
