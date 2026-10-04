package com.xeg911.shared.rules

enum class RuleScope { DEVICE, CONTROL }

enum class RuleMatch { ALL, ANY }

enum class RuleOperator(
    val label: String,
    val geo: Boolean = false,
    val unary: Boolean = false
) {
    LT("<"),
    LTE("<="),
    GT(">"),
    GTE(">="),
    EQ("="),
    NEQ("!="),
    CONTAINS("contains"),
    IS_TRUE("is true", unary = true),
    IS_FALSE("is false", unary = true),
    OUTSIDE_RADIUS("outside radius", geo = true),
    INSIDE_RADIUS("inside radius", geo = true);
}

data class RuleCondition(
    val field: String = "",
    val operator: RuleOperator = RuleOperator.LT,
    val value: String = "",
    val radiusMeters: Double = 0.0,
)

object RuleActionType {
    const val STOP_LOCATION_SHARING = "STOP_LOCATION_SHARING"
    const val START_LOCATION_SHARING = "START_LOCATION_SHARING"
    const val SET_CONFIG = "SET_CONFIG"
    const val START_MONITORING = "START_MONITORING"
    const val REPORT_EVENT = "REPORT_EVENT"
    const val SEND_COMMAND = "SEND_COMMAND"
    const val SEND_NOTIFICATION = "SEND_NOTIFICATION"
    const val LOCAL_ALERT = "LOCAL_ALERT"

    const val PARAM_KEY = "key"
    const val PARAM_VALUE = "value"
    const val PARAM_MESSAGE = "message"
    const val PARAM_COMMAND = "command"
    const val PARAM_TITLE = "title"
    const val PARAM_BODY = "body"

    val DEVICE = listOf(
        STOP_LOCATION_SHARING,
        START_LOCATION_SHARING,
        SET_CONFIG,
        START_MONITORING,
        REPORT_EVENT
    )
    val CONTROL = listOf(
        SEND_COMMAND,
        SEND_NOTIFICATION,
        SET_CONFIG,
        LOCAL_ALERT,
        REPORT_EVENT
    )
}

data class RuleAction(
    val type: String = RuleActionType.REPORT_EVENT,
    val params: Map<String, String> = emptyMap(),
)

data class AutomationRule(
    val id: String = "",
    val name: String = "",
    val enabled: Boolean = true,
    val scope: RuleScope = RuleScope.DEVICE,
    val deviceIds: List<String> = emptyList(),
    val match: RuleMatch = RuleMatch.ALL,
    val conditions: List<RuleCondition> = emptyList(),
    val actions: List<RuleAction> = emptyList(),
    /**
     * Minimum time between two firings for the same device.
     */
    val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    /**
     * Fire again only after the condition became false in between.
     */
    val edgeTriggered: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun appliesTo(deviceId: String) = deviceIds.isEmpty() || deviceId in deviceIds

    companion object {
        const val DEFAULT_COOLDOWN_MS = 5 * 60_000L
    }
}

data class RuleState(
    val ruleId: String = "",
    val active: Boolean = false,
    val lastEvaluatedAt: Long = 0L,
    val lastFiredAt: Long = 0L,
    val fireCount: Int = 0,
    val lastResult: String = "",
)
