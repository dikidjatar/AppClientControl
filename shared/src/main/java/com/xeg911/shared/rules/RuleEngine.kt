package com.xeg911.shared.rules

import com.xeg911.shared.util.containsIgnoreCase
import com.xeg911.shared.util.equalsIgnoreCase
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RuleEngine {

    data class Decision(val rule: AutomationRule, val state: RuleState, val fired: Boolean)

    fun evaluate(
        rules: List<AutomationRule>,
        deviceId: String,
        snapshot: RuleStateSnapshot,
        previous: Map<String, RuleState>,
        now: Long = System.currentTimeMillis(),
    ): List<Decision> = rules
        .filter { it.enabled && it.appliesTo(deviceId) && it.conditions.isNotEmpty() }
        .mapNotNull { rule ->
            decide(
                rule = rule,
                snapshot = snapshot,
                prev = previous[rule.id] ?: RuleState(rule.id),
                now = now
            )
        }

    private fun decide(
        rule: AutomationRule,
        snapshot: RuleStateSnapshot,
        prev: RuleState,
        now: Long
    ): Decision? {
        val results = rule.conditions.map { matches(it, snapshot) ?: return null }
        val active = if (rule.match == RuleMatch.ALL) results.all { it } else results.any { it }
        val armed = !rule.edgeTriggered || !prev.active
        val cooled = now - prev.lastFiredAt >= rule.cooldownMs
        val fire = active && armed && cooled
        val state = prev.copy(
            active = active,
            lastEvaluatedAt = now,
            lastFiredAt = if (fire) now else prev.lastFiredAt,
            fireCount = if (fire) prev.fireCount + 1 else prev.fireCount,
        )
        return Decision(rule, state, fire)
    }

    fun matches(condition: RuleCondition, snapshot: RuleStateSnapshot): Boolean? {
        val op = condition.operator
        if (op.geo) return geoMatches(condition, snapshot)
        val actual = snapshot[condition.field] ?: return null
        return when (op) {
            RuleOperator.IS_TRUE -> actual.asBoolean() == true
            RuleOperator.IS_FALSE -> actual.asBoolean() == false
            RuleOperator.EQ -> actual.toString().equalsIgnoreCase(condition.value) ||
                    compareNumbers(actual, condition.value) { it == 0 } == true

            RuleOperator.NEQ -> !(actual.toString().equalsIgnoreCase(condition.value) ||
                    compareNumbers(actual, condition.value) { it == 0 } == true)

            RuleOperator.CONTAINS -> actual.toString().containsIgnoreCase(condition.value)
            RuleOperator.LT -> compareNumbers(actual, condition.value) { it < 0 }
            RuleOperator.LTE -> compareNumbers(actual, condition.value) { it <= 0 }
            RuleOperator.GT -> compareNumbers(actual, condition.value) { it > 0 }
            RuleOperator.GTE -> compareNumbers(actual, condition.value) { it >= 0 }
            else -> null
        }
    }

    private fun geoMatches(condition: RuleCondition, snapshot: RuleStateSnapshot): Boolean? {
        val lat = (snapshot["${condition.field}.latitude"] as? Number)?.toDouble() ?: return null
        val lng = (snapshot["${condition.field}.longitude"] as? Number)?.toDouble() ?: return null
        val center = parseLatLng(condition.value) ?: return null
        val distance = distanceMeters(lat, lng, center.first, center.second)
        return when (condition.operator) {
            RuleOperator.OUTSIDE_RADIUS -> distance > condition.radiusMeters
            RuleOperator.INSIDE_RADIUS -> distance <= condition.radiusMeters
            else -> null
        }
    }

    private fun compareNumbers(actual: Any, expected: String, test: (Int) -> Boolean): Boolean? {
        val a = (actual as? Number)?.toDouble() ?: actual.toString().toDoubleOrNull() ?: return null
        val b = expected.trim().toDoubleOrNull() ?: return null
        return test(a.compareTo(b))
    }

    private fun Any.asBoolean(): Boolean? = when (this) {
        is Boolean -> this
        is String -> when {
            equalsIgnoreCase("true") -> true
            equalsIgnoreCase("false") -> false
            else -> null
        }

        is Number -> toDouble() != 0.0
        else -> null
    }

    fun parseLatLng(text: String): Pair<Double, Double>? {
        val parts = text.split(',').map { it.trim() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        return lat to lng
    }

    fun distanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) *
                sin(dLat / 2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLng / 2) *
                sin(dLng / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private const val EARTH_RADIUS_M = 6_371_000.0
}
