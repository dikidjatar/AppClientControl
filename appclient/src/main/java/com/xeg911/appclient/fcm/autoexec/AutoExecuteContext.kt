package com.xeg911.appclient.fcm.autoexec

import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import com.xeg911.shared.data.model.notification.withTapActions

class AutoExecuteContext(
    val payload: FcmNotificationPayload,
    val data: Map<String, String> = emptyMap(),
) {
    fun flag(key: String): Boolean = data[key].equals("true", ignoreCase = true)

    fun autoActions(
        def: NotificationActionDef,
        autoParam: String,
        tapActionId: String,
    ): List<NotificationPayloadAction> =
        payload.withTapActions(actionId = tapActionId)
            .filter { it.action.equals(def.id, ignoreCase = true) }
            .filter { it.params[autoParam].equals("true", ignoreCase = true) }
}
