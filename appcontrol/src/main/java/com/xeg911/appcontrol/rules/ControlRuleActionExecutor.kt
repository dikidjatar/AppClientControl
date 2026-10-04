package com.xeg911.appcontrol.rules

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.NotificationTarget
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.appcontrol.domain.repository.RuleRepository
import com.xeg911.appcontrol.domain.usecase.SendNotificationUseCase
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationTapAction
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleAction
import com.xeg911.shared.rules.RuleActionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlRuleActionExecutor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val notificationRepository: NotificationRepository,
    private val sendNotification: SendNotificationUseCase,
) {
    suspend fun execute(rule: AutomationRule, deviceId: String): String {
        val results = rule.actions.map { action ->
            "${action.type}=${
                runCatching {
                    run(
                        rule = rule,
                        deviceId = deviceId,
                        action = action
                    )
                }.getOrElse { it.message ?: it.javaClass.simpleName }
            }"
        }
        val summary = results.joinToString(";")
        ruleRepository.pushEvent(
            deviceId,
            DeviceEvent(
                deviceId = deviceId,
                type = DeviceEventType.RULE_TRIGGERED,
                status = DeviceEventStatus.SUCCESS,
                data = mapOf(
                    "ruleId" to rule.id,
                    "ruleName" to rule.name,
                    "scope" to rule.scope.name,
                    "actions" to summary
                ),
                timestamp = System.currentTimeMillis(),
            ),
        )
        return summary
    }

    private suspend fun run(
        rule: AutomationRule,
        deviceId: String,
        action: RuleAction
    ): String = when (action.type) {
        RuleActionType.SEND_COMMAND -> {
            val command = action.params[RuleActionType.PARAM_COMMAND].orEmpty()
            require(command.isNotBlank()) { "missing command" }
            val payload = if (command == COMMAND_START_MONITORING) {
                FcmNotificationPayload(
                    notificationId = "rule_${rule.id}",
                    cancelOnly = true,
                    silent = true,
                    startMonitoring = true
                )
            } else {
                FcmNotificationPayload(
                    notificationId = "rule_${rule.id}",
                    title = action.params[RuleActionType.PARAM_TITLE].orEmpty()
                        .ifBlank { rule.name },
                    body = action.params[RuleActionType.PARAM_BODY].orEmpty()
                        .ifBlank { context.getString(R.string.rules_command_body, command) },
                    priority = "HIGH",
                    tapAction = NotificationTapAction(
                        action = NotificationActionDef.START_COMMAND.id,
                        params = mapOf("command" to command),
                    ),
                )
            }
            push(deviceId, payload)
        }

        RuleActionType.SEND_NOTIFICATION -> push(
            deviceId,
            FcmNotificationPayload(
                notificationId = "rule_${rule.id}",
                title = action.params[RuleActionType.PARAM_TITLE].orEmpty()
                    .ifBlank { rule.name },
                body = action.params[RuleActionType.PARAM_BODY].orEmpty(),
                priority = "HIGH",
            ),
        )

        RuleActionType.SET_CONFIG -> {
            val key = action.params[RuleActionType.PARAM_KEY].orEmpty()
            require(key.isNotBlank()) { "missing key" }
            ruleRepository.writeDeviceConfig(
                deviceId,
                key,
                action.params[RuleActionType.PARAM_VALUE].toTypedValue()
            ).getOrThrow()
            "ok"
        }

        RuleActionType.LOCAL_ALERT -> {
            localAlert(rule, deviceId, action.params[RuleActionType.PARAM_MESSAGE].orEmpty())
        }

        RuleActionType.REPORT_EVENT -> action.params[RuleActionType.PARAM_MESSAGE].orEmpty()
            .ifBlank { "reported" }

        else -> "unsupported"
    }

    private suspend fun push(deviceId: String, payload: FcmNotificationPayload): String {
        val token = notificationRepository.getFcmToken(deviceId) ?: return "no_token"
        val report = sendNotification(payload, listOf(NotificationTarget(token, deviceId)))
        return if (report.isFullSuccess) "sent" else report.failures.values.firstOrNull()
            ?: "failed"
    }

    private fun localAlert(
        rule: AutomationRule,
        deviceId: String,
        message: String
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return "no_permission"
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.rules_alert_channel)).build()
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.tune_24px)
            .setContentTitle(rule.name)
            .setContentText(message.ifBlank {
                context.getString(
                    R.string.rules_alert_default,
                    deviceId
                )
            })
            .setAutoCancel(true)
            .build()
        manager.notify((rule.id + deviceId).hashCode(), notification)
        return "shown"
    }

    private fun String?.toTypedValue(): Any? = when {
        this == null -> null
        equals("true", true) -> true
        equals("false", true) -> false
        toLongOrNull() != null -> toLong()
        toDoubleOrNull() != null -> toDouble()
        else -> this
    }

    private companion object {
        const val CHANNEL_ID = "rule_alerts"
        const val COMMAND_START_MONITORING = "start_monitoring"
    }
}
