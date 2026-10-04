package com.xeg911.appclient.service.notification

import com.xeg911.appclient.data.remote.config.NotificationFilterConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationContentFilter @Inject constructor(
    private val configRepository: NotificationFilterConfigRepository
) {

    fun isNoise(sourceId: String, text: String): Boolean {
        val sourceDef = configRepository.sources.value.find { it.id == sourceId }
            ?: return false

        if (sourceDef.noisePatterns.isEmpty()) return false

        val normalizedText = text.trim()

        return sourceDef.noisePatterns.any { pattern ->
            runCatching {
                Regex(pattern, RegexOption.IGNORE_CASE).matches(normalizedText)
            }.getOrDefault(false)
        }
    }
}
