package com.xeg911.appclient.fcm.autoexec

import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction

abstract class ActionAutoExecuteHandler(
    protected val actionDef: NotificationActionDef,
    private val autoParam: String,
    private val tapActionId: String,
) : AutoExecuteHandler {

    override val id: String get() = "${actionDef.id}:$autoParam"

    override fun shouldExecute(context: AutoExecuteContext): Boolean =
        applicableActions(context).isNotEmpty()

    override suspend fun execute(context: AutoExecuteContext) {
        applicableActions(context).forEach { executeAction(it, context.payload) }
    }

    protected abstract suspend fun executeAction(
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
    )

    private fun applicableActions(context: AutoExecuteContext) =
        context.autoActions(actionDef, autoParam, tapActionId)
}
