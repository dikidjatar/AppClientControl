package com.xeg911.appclient.rules

import android.util.Log
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.domain.repository.RuleRepository
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleEngine
import com.xeg911.shared.rules.RuleState
import com.xeg911.shared.rules.RuleStateSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRuleRuntime @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val deviceRepository: DeviceRepository,
    private val stateProvider: DeviceStateProvider,
    private val actionExecutor: DeviceRuleActionExecutor,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private var states: HashMap<String, RuleState>? = null

    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        combine(
            ruleRepository.observeDeviceRules()
                .retryWhen { e, _ ->
                    Log.w(
                        TAG,
                        "Rule listener failed, retrying",
                        e
                    ); delay(RETRY_MS); true
                },
            stateProvider.snapshots,
        ) { rules, snapshot -> rules to snapshot }
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { (rules, snapshot) -> evaluate(rules, snapshot) }
            .catch { Log.w(TAG, "Rule runtime stopped", it) }
            .launchIn(scope)
    }

    private suspend fun evaluate(rules: List<AutomationRule>, snapshot: RuleStateSnapshot) {
        if (rules.isEmpty()) return
        val deviceId = runCatching { deviceRepository.deviceId() }.getOrNull() ?: return
        val states = restoredStates()
        states.keys.retainAll(rules.map { it.id }.toSet())
        RuleEngine.evaluate(rules, deviceId, snapshot, states).forEach { decision ->
            val previous = states[decision.rule.id]
            var state = decision.state
            if (decision.fired) {
                Log.i(TAG, "Rule fired: ${decision.rule.name}")
                state = state.copy(lastResult = actionExecutor.execute(decision.rule))
            }
            states[decision.rule.id] = state
            // Only write when something observable changed to keep the ruleState node quiet.
            if (decision.fired || previous?.active != state.active) {
                ruleRepository.publishState(state)
                    .onFailure { Log.w(TAG, "Rule state publish failed", it) }
            }
        }
    }

    private suspend fun restoredStates(): HashMap<String, RuleState> =
        states ?: HashMap(ruleRepository.loadStates().getOrDefault(emptyMap())).also { states = it }

    private companion object {
        const val TAG = "DeviceRuleRuntime"
        const val DEBOUNCE_MS = 1_500L
        const val RETRY_MS = 30_000L
    }
}
