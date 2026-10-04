package com.xeg911.appcontrol.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.util.DeviceNode
import com.xeg911.appcontrol.core.util.FirebaseNode
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.appcontrol.domain.repository.RuleRepository
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.firebase.FirebasePaths
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleScope
import com.xeg911.shared.rules.RuleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
) : RuleRepository {

    private val rulesRef
        get() = database.reference
            .child(FirebaseNode.NODE_AUTOMATION_RULES)

    private fun deviceNode(deviceId: String, node: String) =
        database.reference
            .child(FirebaseNode.NODE_DEVICES)
            .child(deviceId).child(node)

    override fun observeRules(): Flow<Result<List<AutomationRule>>> =
        rulesRef.observeFlow().map { result ->
            result.map { snapshot ->
                snapshot.children.mapNotNull { it.getValueOrNull<AutomationRule>() }
                    .sortedBy { it.name.lowercase() }
            }
        }

    override fun observeEnabledRules(scope: RuleScope): Flow<List<AutomationRule>> =
        rulesRef.orderByChild(FirebasePaths.Rules.SCOPE).equalTo(scope.name).observeFlow()
            .map { result ->
                result.getOrNull()?.children
                    ?.mapNotNull { it.getValueOrNull<AutomationRule>() }
                    ?.filter { it.enabled }
                    .orEmpty()
            }

    override suspend fun save(rule: AutomationRule): Result<AutomationRule> = runCatching {
        val now = System.currentTimeMillis()
        val id = rule.id.ifBlank { rulesRef.push().key ?: error("No key") }
        val stored = rule.copy(
            id = id,
            createdAt = if (rule.createdAt == 0L) now else rule.createdAt,
            updatedAt = now,
        )
        rulesRef.child(id).setValue(stored).await()
        stored
    }

    override suspend fun setEnabled(
        ruleId: String,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        rulesRef
            .child(ruleId)
            .updateChildren(
                mapOf(
                    FirebasePaths.Rules.ENABLED to enabled,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
    }

    override suspend fun delete(ruleId: String): Result<Unit> = runCatching {
        rulesRef.child(ruleId).removeValue().await()
    }

    override fun observeDeviceNode(deviceId: String, node: String): Flow<Any?> =
        deviceNode(deviceId, node)
            .observeFlow()
            .map { it.getOrNull()?.value }

    override suspend fun writeRuleState(deviceId: String, state: RuleState): Result<Unit> =
        runCatching {
            deviceNode(deviceId, DeviceNode.NODE_RULE_STATE)
                .child(state.ruleId)
                .setValue(state)
                .await()
        }

    override suspend fun loadRuleStates(deviceId: String): Result<Map<String, RuleState>> =
        runCatching {
            deviceNode(deviceId, DeviceNode.NODE_RULE_STATE).get().await().children
                .mapNotNull { it.getValueOrNull<RuleState>() }
                .associateBy { it.ruleId }
        }

    override suspend fun writeDeviceConfig(
        deviceId: String,
        key: String,
        value: Any?
    ): Result<Unit> = runCatching {
        deviceNode(deviceId, DeviceNode.NODE_CONFIG)
            .child(key)
            .setValue(value).await()
    }

    override suspend fun pushEvent(deviceId: String, event: DeviceEvent): Result<Unit> =
        runCatching {
            deviceNode(deviceId, DeviceNode.NODE_EVENTS)
                .push()
                .setValue(event)
                .await()
        }
}
