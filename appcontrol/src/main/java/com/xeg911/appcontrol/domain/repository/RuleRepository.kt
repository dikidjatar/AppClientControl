package com.xeg911.appcontrol.domain.repository

import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleScope
import com.xeg911.shared.rules.RuleState
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun observeRules(): Flow<Result<List<AutomationRule>>>

    fun observeEnabledRules(scope: RuleScope): Flow<List<AutomationRule>>

    suspend fun save(rule: AutomationRule): Result<AutomationRule>

    suspend fun setEnabled(ruleId: String, enabled: Boolean): Result<Unit>

    suspend fun delete(ruleId: String): Result<Unit>

    fun observeDeviceNode(deviceId: String, node: String): Flow<Any?>

    suspend fun writeRuleState(deviceId: String, state: RuleState): Result<Unit>

    suspend fun loadRuleStates(deviceId: String): Result<Map<String, RuleState>>

    suspend fun writeDeviceConfig(deviceId: String, key: String, value: Any?): Result<Unit>

    suspend fun pushEvent(deviceId: String, event: DeviceEvent): Result<Unit>
}
