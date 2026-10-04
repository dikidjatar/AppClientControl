package com.xeg911.appclient.data.remote.config

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.core.firebase.observe
import com.xeg911.appclient.core.firebase.valueOrNull
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.shared.data.model.notification.NotificationSourceDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFilterConfigRepository @Inject constructor(
    private val dataSource: DeviceFirebaseDataSource,
    private val preferences: AppPreferences,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val json = Json { ignoreUnknownKeys = true }

    private val _sources = MutableStateFlow<List<NotificationSourceDef>>(emptyList())
    val sources: StateFlow<List<NotificationSourceDef>> = _sources.asStateFlow()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            loadCache()
            dataSource.notificationSourcesRef().observe()
                .onEach { snapshot ->
                    val parsed = snapshot.children.mapNotNull(::parseSource)
                    _sources.value = parsed
                    runCatching {
                        preferences.setCachedNotificationSources(json.encodeToString(parsed))
                    }
                }
                .catch { Log.w(TAG, "Source listener failed", it) }
                .collect()
        }
    }

    private suspend fun loadCache() {
        val cached = preferences.cachedNotificationSources() ?: return
        runCatching { json.decodeFromString<List<NotificationSourceDef>>(cached) }
            .onSuccess { if (_sources.value.isEmpty()) _sources.value = it }
    }

    private fun parseSource(child: DataSnapshot): NotificationSourceDef? = runCatching {
        val id = child.key ?: return null
        val defaults = NotificationSourceDef()
        NotificationSourceDef(
            id = id,
            label = child.child("label").valueOrNull<String>() ?: id,
            enabled = child.child("enabled").valueOrNull<Boolean>() ?: defaults.enabled,
            headerEmoji = child.child("headerEmoji").valueOrNull<String>() ?: defaults.headerEmoji,
            fromEmoji = child.child("fromEmoji").valueOrNull<String>() ?: defaults.fromEmoji,
            packages = child.child("packages").stringList(),
            usesDefaultSms = child.child("usesDefaultSms").valueOrNull<Boolean>() ?: false,
            noisePatterns = child.child("noisePatterns").stringList(),
        )
    }.onFailure { Log.w(TAG, "Failed to parse source '${child.key}'", it) }.getOrNull()

    private fun DataSnapshot.stringList(): List<String> =
        (value as? List<*>)?.filterIsInstance<String>().orEmpty()

    private companion object {
        const val TAG = "NotificationFilterConfig"
    }
}
