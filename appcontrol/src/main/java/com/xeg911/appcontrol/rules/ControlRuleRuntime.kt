package com.xeg911.appcontrol.rules

import android.util.Log
import com.xeg911.appcontrol.domain.repository.AuthRepository
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.domain.repository.RuleRepository
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleEngine
import com.xeg911.shared.rules.RuleField
import com.xeg911.shared.rules.RuleScope
import com.xeg911.shared.rules.RuleState
import com.xeg911.shared.rules.RuleStateFlattener
import com.xeg911.shared.rules.RuleStateSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class ControlRuleRuntime @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val deviceRepository: DeviceRepository,
    private val authRepository: AuthRepository,
    private val actionExecutor: ControlRuleActionExecutor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)
    private val rules = MutableStateFlow<List<AutomationRule>>(emptyList())

    private val states = HashMap<String, HashMap<String, RuleState>>()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        authRepository.observeSignedIn()
            .flatMapLatest { signedIn ->
                if (signedIn) ruleRepository.observeEnabledRules(RuleScope.CONTROL) else flowOf(
                    emptyList()
                )
            }
            .onEach { rules.value = it }
            .retryWhen { e, _ ->
                Log.w(
                    TAG,
                    "Rule listener failed, retrying",
                    e
                ); delay(RETRY_MS.milliseconds); true
            }
            .launchIn(scope)

        rules
            .flatMapLatest { active ->
                if (active.none { it.conditions.isNotEmpty() }) flowOf(emptyMap())
                else deviceIds().map { ids -> requirements(active, ids) }
            }
            .distinctUntilChanged()
            .flatMapLatest { required ->
                if (required.isEmpty()) emptyFlow()
                else merge(*required.map { (deviceId, nodes) -> deviceSnapshots(deviceId, nodes) }
                    .toTypedArray())
            }
            .onEach { (deviceId, snapshot) -> evaluate(deviceId, snapshot) }
            .retryWhen { e, _ ->
                Log.w(
                    TAG,
                    "Rule runtime failed, retrying",
                    e
                ); delay(RETRY_MS.milliseconds); true
            }
            .launchIn(scope)
    }

    private fun deviceIds(): Flow<Set<String>> = deviceRepository.observeDevices()
        .map { result -> result.getOrNull().orEmpty().map { it.deviceId }.toSet() }
        .distinctUntilChanged()

    private fun requirements(
        rules: List<AutomationRule>,
        ids: Set<String>
    ): Map<String, Set<String>> {
        val out = HashMap<String, MutableSet<String>>()
        rules.filter { it.conditions.isNotEmpty() }.forEach { rule ->
            val nodes = rule.conditions.map { RuleField.nodeOf(it.field) }
            ids.filter(rule::appliesTo)
                .forEach { id -> out.getOrPut(id) { HashSet() }.addAll(nodes) }
        }
        return out
    }

    private fun deviceSnapshots(
        deviceId: String,
        nodes: Set<String>
    ): Flow<Pair<String, RuleStateSnapshot>> =
        combine(nodes.map { node ->
            ruleRepository.observeDeviceNode(deviceId, node).map { node to it }
        }) { parts ->
            val out = HashMap<String, Any?>()
            parts.forEach { (node, value) -> RuleStateFlattener.flatten(node, value, out) }
            deviceId to out
        }.debounce(DEBOUNCE_MS.milliseconds)

    private suspend fun evaluate(deviceId: String, snapshot: RuleStateSnapshot) {
        val deviceStates = statesFor(deviceId)
        RuleEngine.evaluate(rules.value, deviceId, snapshot, deviceStates).forEach { decision ->
            val previous = deviceStates[decision.rule.id]
            var state = decision.state
            if (decision.fired) {
                Log.i(TAG, "Rule fired: ${decision.rule.name} on $deviceId")
                state = state.copy(lastResult = actionExecutor.execute(decision.rule, deviceId))
            }
            deviceStates[decision.rule.id] = state
            if (decision.fired || previous?.active != state.active) {
                ruleRepository.writeRuleState(deviceId, state)
                    .onFailure { Log.w(TAG, "Rule state write failed", it) }
            }
        }
    }

    private suspend fun statesFor(deviceId: String): HashMap<String, RuleState> =
        states[deviceId] ?: HashMap(
            ruleRepository.loadRuleStates(deviceId).getOrDefault(emptyMap())
        )
            .also { states[deviceId] = it }

    private companion object {
        const val TAG = "ControlRuleRuntime"
        const val DEBOUNCE_MS = 1_000L
        const val RETRY_MS = 30_000L
    }
}
