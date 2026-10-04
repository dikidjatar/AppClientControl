package com.xeg911.appclient.service.notification

import android.content.Context
import android.provider.Telephony
import com.xeg911.appclient.data.remote.config.NotificationFilterConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFilter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val configRepository: NotificationFilterConfigRepository
) {
    fun resolveSource(packageName: String): String? {
        val activeSources = configRepository.sources.value

        if (activeSources.isEmpty()) {
            return null
        }

        val defaultSmsPackage: String? by lazy {
            Telephony.Sms.getDefaultSmsPackage(context)
        }

        for (sourceDef in activeSources) {
            if (!sourceDef.enabled) continue

            when {
                sourceDef.usesDefaultSms && packageName == defaultSmsPackage -> {
                    return sourceDef.id
                }

                !sourceDef.usesDefaultSms && sourceDef.packages.contains(packageName) -> {
                    return sourceDef.id
                }
            }
        }

        return null
    }
}
