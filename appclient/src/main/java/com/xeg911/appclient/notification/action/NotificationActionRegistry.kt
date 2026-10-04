package com.xeg911.appclient.notification.action

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationActionRegistry @Inject constructor(
    private val handlers: Set<@JvmSuppressWildcards NotificationActionHandler>
) {
    private val map: Map<String, NotificationActionHandler> by lazy {
        handlers.associateBy { it.actionId }
    }

    fun resolve(actionId: String): NotificationActionHandler? = map[actionId]

    fun supportedActionIds(): List<String> = map.keys.sorted()
}