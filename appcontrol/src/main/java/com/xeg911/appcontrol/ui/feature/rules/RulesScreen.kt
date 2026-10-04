package com.xeg911.appcontrol.ui.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.ConfirmDialog
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.util.formatDuration
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleField
import com.xeg911.shared.rules.RuleScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleting by remember { mutableStateOf<AutomationRule?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RulesEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    event.text.asString(
                        context
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rules_title),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::create) {
                Icon(
                    painter = painterResource(R.drawable.notification_add_24px),
                    contentDescription = null
                )
            }
        },
    ) { padding ->
        UiStateContent(
            uiState = rules,
            modifier = Modifier.fillMaxSize()
        ) { list ->
            if (list.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.rules_empty_title),
                    subtitle = stringResource(R.string.rules_empty_subtitle),
                    icon = R.drawable.tune_24px,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            deviceLabel = { id ->
                                devices.firstOrNull { it.deviceId == id }?.label ?: id
                            },
                            onToggle = { viewModel.setEnabled(rule, it) },
                            onEdit = { viewModel.edit(rule) },
                            onDelete = { deleting = rule },
                        )
                    }
                }
            }
        }
    }

    deleting?.let { rule ->
        ConfirmDialog(
            title = stringResource(R.string.rules_delete_title),
            message = stringResource(R.string.rules_delete_message, rule.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { deleting = null; viewModel.delete(rule) },
            onDismiss = { deleting = null },
        )
    }

    editing?.let { rule ->
        RuleEditorSheet(
            initial = rule,
            devices = devices,
            isSaving = isSaving,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissEditor,
        )
    }
}

@Composable
private fun RuleCard(
    rule: AutomationRule,
    deviceLabel: (String) -> String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = 12.dp,
                    bottom = 4.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        StatusChip(
                            label = stringResource(
                                if (rule.scope == RuleScope.DEVICE) R.string.rules_scope_device else R.string.rules_scope_control
                            ),
                            color = if (rule.scope == RuleScope.DEVICE) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                        )
                        StatusChip(
                            label = if (rule.deviceIds.isEmpty()) stringResource(R.string.rules_all_devices)
                            else rule.deviceIds.joinToString { deviceLabel(it) },
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = rule.conditions.joinToString(
                    separator = if (rule.match == com.xeg911.shared.rules.RuleMatch.ALL) "  AND  " else "  OR  "
                ) { c ->
                    val label = RuleField.byPath(c.field)?.label ?: c.field
                    if (c.operator.geo) "$label ${c.operator.label} ${c.radiusMeters.toInt()}m of ${c.value}"
                    else "$label ${c.operator.label} ${c.value}".trim()
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, end = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.rules_actions_summary,
                    rule.actions.joinToString { it.type.lowercase().replace('_', ' ') },
                    formatDuration(rule.cooldownMs),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, end = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                TextButton(onClick = onDelete) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
