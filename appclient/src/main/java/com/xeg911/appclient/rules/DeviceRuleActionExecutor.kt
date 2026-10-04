package com.xeg911.appclient.rules

import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.location.LocationSharingController
import com.xeg911.appclient.monitoring.MonitoringController
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleAction
import com.xeg911.shared.rules.RuleActionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRuleActionExecutor @Inject constructor(
    private val locationSharingController: LocationSharingController,
    private val monitoringController: MonitoringController,
    private val deviceRepository: DeviceRepository,
    private val eventReporter: DeviceEventReporter,
) {
    suspend fun execute(rule: AutomationRule): String {
        val results = rule.actions.map { action ->
            "${action.type}=${
                runCatching {
                    run(
                        rule,
                        action
                    )
                }.getOrElse { it.javaClass.simpleName }
            }"
        }
        val summary = results.joinToString(";")
        eventReporter.reportNow(
            type = DeviceEventType.RULE_TRIGGERED,
            status = if (results.any { it.endsWith("Exception") }) DeviceEventStatus.FAILED else DeviceEventStatus.SUCCESS,
            data = mapOf(
                "ruleId" to rule.id,
                "ruleName" to rule.name,
                "scope" to rule.scope.name,
                "actions" to summary
            ),
        )
        return summary
    }

    private suspend fun run(rule: AutomationRule, action: RuleAction): String = when (action.type) {
        RuleActionType.STOP_LOCATION_SHARING -> {
            if (locationSharingController.isRunning()) {
                locationSharingController.stopByRule(rule.name); "stopped"
            } else "not_running"
        }

        RuleActionType.START_LOCATION_SHARING -> {
            locationSharingController.startIfEnabled(LocationSharingController.StartReason.REMOTE_COMMAND)
            if (locationSharingController.isEnabled()) "started" else "consent_missing"
        }

        RuleActionType.START_MONITORING -> {
            monitoringController.startIfAllowed(MonitoringController.StartReason.REMOTE_COMMAND); "requested"
        }

        RuleActionType.SET_CONFIG -> {
            val key = action.params[RuleActionType.PARAM_KEY].orEmpty()
            require(key.isNotBlank()) { "missing key" }
            deviceRepository.writeConfigField(
                key,
                action.params[RuleActionType.PARAM_VALUE].toTypedValue()
            ).getOrThrow()
            "ok"
        }

        RuleActionType.REPORT_EVENT -> action.params[RuleActionType.PARAM_MESSAGE].orEmpty()
            .ifBlank { "reported" }

        else -> "unsupported"
    }

    private fun String?.toTypedValue(): Any? = when {
        this == null -> null
        equals("true", true) -> true
        equals("false", true) -> false
        toLongOrNull() != null -> toLong()
        toDoubleOrNull() != null -> toDouble()
        else -> this
    }
}
