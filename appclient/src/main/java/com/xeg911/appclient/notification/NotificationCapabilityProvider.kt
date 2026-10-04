package com.xeg911.appclient.notification

import com.xeg911.appclient.notification.action.NotificationActionRegistry
import com.xeg911.appclient.notification.style.NotificationStyleRegistry
import com.xeg911.shared.data.model.notification.DeviceNotificationCapability
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import com.xeg911.shared.data.model.notification.SupportedActionDef
import com.xeg911.shared.data.model.notification.SupportedStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationCapabilityProvider @Inject constructor(
    private val styleRegistry: NotificationStyleRegistry,
    private val actionRegistry: NotificationActionRegistry
) {

    fun build(fcmToken: String): DeviceNotificationCapability = DeviceNotificationCapability(
        schemaVersion = SCHEMA_VERSION,
        supportedStyles = buildStyleDefs(),
        supportedActions = buildActionDefs(),
        fcmToken = fcmToken,
        updatedAt = System.currentTimeMillis()
    )

    private fun buildStyleDefs(): List<SupportedStyleDef> =
        styleRegistry.supportedStyleIds().map { id ->
            val def = NotificationStyleDef.fromId(id)
            SupportedStyleDef(
                id = def.id,
                description = def.description,
                usedFields = def.usedFields
            )
        }.sortedBy { it.id }

    private fun buildActionDefs(): List<SupportedActionDef> =
        actionRegistry.supportedActionIds().mapNotNull { id ->
            val def = NotificationActionDef.fromId(id) ?: return@mapNotNull null
            SupportedActionDef(
                id = def.id,
                description = def.description,
                requiredParams = def.requiredParams,
                optionalParams = def.optionalParams
            )
        }.sortedBy { it.id }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}
