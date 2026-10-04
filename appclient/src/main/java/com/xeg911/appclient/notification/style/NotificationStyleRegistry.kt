package com.xeg911.appclient.notification.style

import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStyleRegistry @Inject constructor(
    private val renderers: Set<@JvmSuppressWildcards NotificationStyleRenderer>
) {
    private val map: Map<String, NotificationStyleRenderer> by lazy {
        renderers.associateBy { it.styleId }
    }

    fun resolve(styleId: String): NotificationStyleRenderer =
        map[styleId] ?: map[NotificationStyleDef.DEFAULT.id]
        ?: error("DefaultStyleRenderer not registered")

    fun supportedStyleIds(): List<String> = map.keys.sorted()
}
