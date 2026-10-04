package com.xeg911.appcontrol.ui.feature.rules

import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.domain.repository.RuleRepository
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.common.toUiState
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleCondition
import com.xeg911.shared.rules.RuleField
import com.xeg911.shared.rules.RuleOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RulesEvent {
    data class ShowMessage(val text: UiText) : RulesEvent
}

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    deviceRepository: DeviceRepository,
) : BaseViewModel<RulesEvent>() {

    val rules: StateFlow<UiState<List<AutomationRule>>> = ruleRepository.observeRules()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val devices: StateFlow<List<DeviceSnapshot>> = deviceRepository.observeDevices()
        .map { it.getOrDefault(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editing = MutableStateFlow<AutomationRule?>(null)
    val editing: StateFlow<AutomationRule?> = _editing.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun create() {
        _editing.value = AutomationRule()
    }

    fun edit(rule: AutomationRule) {
        _editing.value = rule
    }

    fun dismissEditor() {
        _editing.value = null
    }

    fun save(rule: AutomationRule) {
        validate(rule)?.let { trySendEvent(RulesEvent.ShowMessage(UiText.Res(it))); return }
        _isSaving.value = true
        viewModelScope.launch {
            ruleRepository.save(rule).fold(
                onSuccess = { _editing.value = null; message(UiText.Res(R.string.rules_saved)) },
                onFailure = { message(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) },
            )
            _isSaving.value = false
        }
    }

    fun setEnabled(rule: AutomationRule, enabled: Boolean) = viewModelScope.launch {
        ruleRepository.setEnabled(rule.id, enabled)
            .onFailure { message(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) }
    }

    fun delete(rule: AutomationRule) = viewModelScope.launch {
        ruleRepository.delete(rule.id).fold(
            onSuccess = { message(UiText.Res(R.string.rules_deleted)) },
            onFailure = { message(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) },
        )
    }

    private fun validate(rule: AutomationRule): Int? = when {
        rule.name.isBlank() -> R.string.rules_error_name
        rule.conditions.isEmpty() -> R.string.rules_error_conditions
        rule.conditions.any { !it.isValid() } -> R.string.rules_error_condition_value
        rule.actions.isEmpty() -> R.string.rules_error_actions
        else -> null
    }

    private fun RuleCondition.isValid(): Boolean = RuleField.byPath(field) != null && when {
        operator.geo -> radiusMeters > 0 && com.xeg911.shared.rules.RuleEngine.parseLatLng(value) != null
        operator.unary -> true
        operator == RuleOperator.CONTAINS || operator == RuleOperator.EQ || operator == RuleOperator.NEQ -> value.isNotBlank()
        else -> value.trim().toDoubleOrNull() != null
    }

    private suspend fun message(text: UiText) = sendEvent(RulesEvent.ShowMessage(text))
}
