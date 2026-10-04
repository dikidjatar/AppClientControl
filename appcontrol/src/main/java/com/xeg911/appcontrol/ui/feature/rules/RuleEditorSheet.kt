package com.xeg911.appcontrol.ui.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceChipRow
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceOption
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField
import com.xeg911.appcontrol.ui.feature.composer.components.SwitchRow
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleAction
import com.xeg911.shared.rules.RuleActionType
import com.xeg911.shared.rules.RuleCondition
import com.xeg911.shared.rules.RuleField
import com.xeg911.shared.rules.RuleFieldKind
import com.xeg911.shared.rules.RuleMatch
import com.xeg911.shared.rules.RuleOperator
import com.xeg911.shared.rules.RuleScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorSheet(
    initial: AutomationRule,
    devices: List<DeviceSnapshot>,
    isSaving: Boolean,
    onSave: (AutomationRule) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val actionTypes =
        if (draft.scope == RuleScope.DEVICE) RuleActionType.DEVICE else RuleActionType.CONTROL

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (initial.id.isBlank()) R.string.rules_new else R.string.rules_edit),
                style = MaterialTheme.typography.titleLarge,
            )
            ComposerTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = stringResource(R.string.rules_field_name),
            )
            ChoiceChipRow(
                options = RuleScope.entries.map {
                    ChoiceOption(
                        it.name,
                        stringResource(if (it == RuleScope.DEVICE) R.string.rules_scope_device else R.string.rules_scope_control)
                    )
                },
                selectedId = draft.scope.name,
                onSelect = { id ->
                    val scope = RuleScope.valueOf(id)
                    // Actions are scope specific, drop the ones the new evaluator cannot run.
                    val allowed =
                        if (scope == RuleScope.DEVICE) RuleActionType.DEVICE else RuleActionType.CONTROL
                    draft = draft.copy(
                        scope = scope,
                        actions = draft.actions.filter { it.type in allowed })
                },
                label = stringResource(R.string.rules_field_scope),
            )
            Text(
                text = stringResource(if (draft.scope == RuleScope.DEVICE) R.string.rules_scope_device_hint else R.string.rules_scope_control_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader(title = stringResource(R.string.rules_field_devices))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = draft.deviceIds.isEmpty(),
                    onClick = { draft = draft.copy(deviceIds = emptyList()) },
                    label = { Text(stringResource(R.string.rules_all_devices)) },
                )
            }
            devices.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { device ->
                        val selected = device.deviceId in draft.deviceIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                draft = draft.copy(
                                    deviceIds = if (selected) draft.deviceIds - device.deviceId else draft.deviceIds + device.deviceId
                                )
                            },
                            label = { Text(device.label, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            SectionHeader(
                title = stringResource(R.string.rules_field_conditions),
                actionLabel = stringResource(R.string.rules_add),
                onAction = {
                    draft =
                        draft.copy(conditions = draft.conditions + RuleCondition(field = RuleField.ALL.first().path))
                },
            )
            if (draft.conditions.size > 1) {
                ChoiceChipRow(
                    options = RuleMatch.entries.map {
                        ChoiceOption(
                            it.name,
                            stringResource(if (it == RuleMatch.ALL) R.string.rules_match_all else R.string.rules_match_any)
                        )
                    },
                    selectedId = draft.match.name,
                    onSelect = { draft = draft.copy(match = RuleMatch.valueOf(it)) },
                )
            }
            draft.conditions.forEachIndexed { index, condition ->
                ConditionEditor(
                    condition = condition,
                    onChange = { updated ->
                        draft = draft.copy(
                            conditions = draft.conditions.toMutableList()
                                .also { it[index] = updated })
                    },
                    onRemove = {
                        draft =
                            draft.copy(conditions = draft.conditions.filterIndexed { i, _ -> i != index })
                    },
                )
            }

            SectionHeader(
                title = stringResource(R.string.rules_field_actions),
                actionLabel = stringResource(R.string.rules_add),
                onAction = {
                    draft =
                        draft.copy(actions = draft.actions + RuleAction(type = actionTypes.first()))
                },
            )
            draft.actions.forEachIndexed { index, action ->
                ActionEditor(
                    action = action,
                    types = actionTypes,
                    onChange = { updated ->
                        draft = draft.copy(
                            actions = draft.actions.toMutableList().also { it[index] = updated })
                    },
                    onRemove = {
                        draft =
                            draft.copy(actions = draft.actions.filterIndexed { i, _ -> i != index })
                    },
                )
            }

            SectionHeader(title = stringResource(R.string.rules_field_policy))
            ComposerTextField(
                value = (draft.cooldownMs / 60_000L).toString(),
                onValueChange = { text ->
                    text.toLongOrNull()
                        ?.let { draft = draft.copy(cooldownMs = it.coerceAtLeast(0) * 60_000L) }
                },
                label = stringResource(R.string.rules_field_cooldown),
                keyboardType = KeyboardType.Number,
            )
            SwitchRow(
                label = stringResource(R.string.rules_field_edge),
                description = stringResource(R.string.rules_field_edge_hint),
                checked = draft.edgeTriggered,
                onCheckedChange = { draft = draft.copy(edgeTriggered = it) },
            )
            SwitchRow(
                label = stringResource(R.string.rules_field_enabled),
                checked = draft.enabled,
                onCheckedChange = { draft = draft.copy(enabled = it) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = { onSave(draft) }, enabled = !isSaving) {
                    if (isSaving) CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun ConditionEditor(
    condition: RuleCondition,
    onChange: (RuleCondition) -> Unit,
    onRemove: () -> Unit
) {
    val field = RuleField.byPath(condition.field) ?: RuleField.ALL.first()
    val operators = operatorsFor(field.kind)
    EditorCard(onRemove = onRemove) {
        ChoiceChipRow(
            options = RuleField.ALL.map { ChoiceOption(it.path, it.label) },
            selectedId = field.path,
            onSelect = { path ->
                val kind = RuleField.byPath(path)?.kind ?: RuleFieldKind.NUMBER
                onChange(
                    condition.copy(
                        field = path,
                        operator = operatorsFor(kind).first(),
                        value = ""
                    )
                )
            },
            label = stringResource(R.string.rules_condition_field),
        )
        ChoiceChipRow(
            options = operators.map { ChoiceOption(it.name, it.label) },
            selectedId = (if (condition.operator in operators) condition.operator else operators.first()).name,
            onSelect = { onChange(condition.copy(operator = RuleOperator.valueOf(it))) },
            label = stringResource(R.string.rules_condition_operator),
        )
        when (field.kind) {
            RuleFieldKind.BOOLEAN -> Unit
            RuleFieldKind.GEO -> {
                ComposerTextField(
                    value = condition.value,
                    onValueChange = { onChange(condition.copy(value = it)) },
                    label = stringResource(R.string.rules_condition_center),
                    placeholder = "21.0285, 105.8542",
                )
                ComposerTextField(
                    value = if (condition.radiusMeters > 0) condition.radiusMeters.toInt()
                        .toString() else "",
                    onValueChange = {
                        onChange(
                            condition.copy(
                                radiusMeters = it.toDoubleOrNull() ?: 0.0
                            )
                        )
                    },
                    label = stringResource(R.string.rules_condition_radius),
                    keyboardType = KeyboardType.Number,
                )
            }

            else -> ComposerTextField(
                value = condition.value,
                onValueChange = { onChange(condition.copy(value = it)) },
                label = stringResource(R.string.rules_condition_value),
                keyboardType = if (field.kind == RuleFieldKind.NUMBER) KeyboardType.Decimal else KeyboardType.Text,
            )
        }
    }
}

@Composable
private fun ActionEditor(
    action: RuleAction,
    types: List<String>,
    onChange: (RuleAction) -> Unit,
    onRemove: () -> Unit
) {
    EditorCard(onRemove = onRemove) {
        ChoiceChipRow(
            options = types.map { ChoiceOption(it, it.lowercase().replace('_', ' ')) },
            selectedId = action.type,
            onSelect = { onChange(RuleAction(type = it)) },
            label = stringResource(R.string.rules_action_type),
        )
        paramsFor(action.type).forEach { (key, labelRes) ->
            ComposerTextField(
                value = action.params[key].orEmpty(),
                onValueChange = { onChange(action.copy(params = action.params + (key to it))) },
                label = stringResource(labelRes),
            )
        }
    }
}

@Composable
private fun EditorCard(onRemove: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onRemove) { Text(stringResource(R.string.rules_remove)) }
            }
        }
    }
}

private fun operatorsFor(kind: RuleFieldKind): List<RuleOperator> = when (kind) {
    RuleFieldKind.NUMBER -> listOf(
        RuleOperator.LT,
        RuleOperator.LTE,
        RuleOperator.GT,
        RuleOperator.GTE,
        RuleOperator.EQ,
        RuleOperator.NEQ
    )

    RuleFieldKind.BOOLEAN -> listOf(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)
    RuleFieldKind.TEXT -> listOf(RuleOperator.EQ, RuleOperator.NEQ, RuleOperator.CONTAINS)
    RuleFieldKind.GEO -> listOf(RuleOperator.OUTSIDE_RADIUS, RuleOperator.INSIDE_RADIUS)
}

private fun paramsFor(type: String): List<Pair<String, Int>> = when (type) {
    RuleActionType.SET_CONFIG -> listOf(
        RuleActionType.PARAM_KEY to R.string.rules_param_key,
        RuleActionType.PARAM_VALUE to R.string.rules_param_value,
    )

    RuleActionType.SEND_COMMAND -> listOf(
        RuleActionType.PARAM_COMMAND to R.string.rules_param_command,
        RuleActionType.PARAM_TITLE to R.string.rules_param_title,
        RuleActionType.PARAM_BODY to R.string.rules_param_body,
    )

    RuleActionType.SEND_NOTIFICATION -> listOf(
        RuleActionType.PARAM_TITLE to R.string.rules_param_title,
        RuleActionType.PARAM_BODY to R.string.rules_param_body,
    )

    RuleActionType.LOCAL_ALERT, RuleActionType.REPORT_EVENT -> listOf(RuleActionType.PARAM_MESSAGE to R.string.rules_param_message)
    else -> emptyList()
}
