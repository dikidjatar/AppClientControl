package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.notification.action.command.AppCommandExecutor
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartCommandActionHandler @Inject constructor(
    executors: Set<@JvmSuppressWildcards AppCommandExecutor>,
) : NotificationActionHandler {

    private val registry: Map<String, AppCommandExecutor> = executors.associateBy { it.commandId }

    override val actionId: String = NotificationActionDef.START_COMMAND.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? = resolve(action)?.activityIntent(context, payload.notificationId)

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val commandId = action.params["command"]?.lowercase()?.trim()
            ?: return ActionResult.Silent

        val executor = registry[commandId] ?: return ActionResult.Completed(
            status = DeviceEventStatus.NOT_AVAILABLE,
            data = mapOf("command" to commandId)
        )

        executor.activityIntent(context, payload.notificationId)?.let { intent ->
            runCatching { context.startActivity(intent) }
            return ActionResult.Delegated
        }
        return executor.execute(context)
    }

    private fun resolve(action: NotificationPayloadAction): AppCommandExecutor? =
        action.params["command"]?.lowercase()?.trim()?.let(registry::get)
}