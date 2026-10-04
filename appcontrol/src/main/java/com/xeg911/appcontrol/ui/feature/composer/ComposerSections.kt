package com.xeg911.appcontrol.ui.feature.composer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.LabelChip
import com.xeg911.appcontrol.ui.feature.composer.components.ActionEditorCard
import com.xeg911.appcontrol.ui.feature.composer.components.ActionTypePicker
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceChipRow
import com.xeg911.appcontrol.ui.feature.composer.components.ChoiceOption
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerSection
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField
import com.xeg911.appcontrol.ui.feature.composer.components.FieldPair
import com.xeg911.appcontrol.ui.feature.composer.components.KeyValueEditor
import com.xeg911.appcontrol.ui.feature.composer.components.MessagingEditor
import com.xeg911.appcontrol.ui.feature.composer.components.ParamsEditor
import com.xeg911.appcontrol.ui.feature.composer.components.SwitchRow

@Composable
internal fun ContentSection(state: ComposerUiState, viewModel: NotificationComposerViewModel) {
    val form = state.form
    ComposerSection(title = stringResource(R.string.composer_section_content)) {
        ComposerTextField(
            value = form.notificationId,
            onValueChange = { v -> viewModel.update { copy(notificationId = v) } },
            label = stringResource(R.string.composer_field_notification_id),
            supportingText = stringResource(
                if (state.isEditing) R.string.composer_hint_id_edit else R.string.composer_hint_id
            ),
            readOnly = state.isEditing,
            isError = form.notificationId.isBlank(),
            trailingIcon = if (state.isEditing) null else {
                {
                    IconButton(onClick = viewModel::regenerateId) {
                        Icon(
                            painter = painterResource(R.drawable.replay_24px),
                            contentDescription = stringResource(R.string.composer_cd_regenerate_id),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )
        if (form.cancelOnly) {
            Text(
                text = stringResource(R.string.composer_cancel_only_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@ComposerSection
        }
        ComposerTextField(
            value = form.title,
            onValueChange = { v -> viewModel.update { copy(title = v) } },
            label = stringResource(R.string.composer_field_title),
        )
        ComposerTextField(
            value = form.body,
            onValueChange = { v -> viewModel.update { copy(body = v) } },
            label = stringResource(R.string.composer_field_body),
            singleLine = false,
            minLines = 2,
        )
    }
}

@Composable
internal fun StyleSection(state: ComposerUiState, viewModel: NotificationComposerViewModel) {
    val form = state.form
    val fields = state.selectedStyleFields
    val selectedStyle = state.styles.firstOrNull { it.id.equals(form.style, ignoreCase = true) }

    ComposerSection(
        title = stringResource(R.string.composer_section_style),
        subtitle = selectedStyle?.description,
        badge = if (state.capability == null) {
            { LabelChip(label = stringResource(R.string.composer_capability_fallback)) }
        } else null,
    ) {
        ChoiceChipRow(
            options = state.styles.map { ChoiceOption(it.id) },
            selectedId = form.style,
            onSelect = { v -> viewModel.update { copy(style = v) } },
        )
        if ("bigText" in fields) {
            ComposerTextField(
                value = form.bigText,
                onValueChange = { v -> viewModel.update { copy(bigText = v) } },
                label = stringResource(R.string.composer_field_big_text),
                singleLine = false,
                minLines = 3,
            )
        }
        if ("summaryText" in fields) {
            ComposerTextField(
                value = form.summaryText,
                onValueChange = { v -> viewModel.update { copy(summaryText = v) } },
                label = stringResource(R.string.composer_field_summary_text),
            )
        }
        if ("image" in fields) {
            ComposerTextField(
                value = form.image,
                onValueChange = { v -> viewModel.update { copy(image = v) } },
                label = stringResource(R.string.composer_field_image),
                placeholder = stringResource(R.string.composer_hint_url),
                keyboardType = KeyboardType.Uri,
            )
        }
        if ("progress" in fields) {
            FieldPair(
                first = { mod ->
                    ComposerTextField(
                        value = form.progress,
                        onValueChange = { v -> viewModel.update { copy(progress = v) } },
                        label = stringResource(R.string.composer_field_progress),
                        keyboardType = KeyboardType.Number,
                        modifier = mod,
                    )
                },
                second = { mod ->
                    ComposerTextField(
                        value = form.progressMax,
                        onValueChange = { v -> viewModel.update { copy(progressMax = v) } },
                        label = stringResource(R.string.composer_field_progress_max),
                        keyboardType = KeyboardType.Number,
                        modifier = mod,
                    )
                },
            )
            SwitchRow(
                label = stringResource(R.string.composer_field_indeterminate),
                checked = form.indeterminate,
                onCheckedChange = { v -> viewModel.update { copy(indeterminate = v) } },
            )
        }
        if ("messagingStyle" in fields) {
            MessagingEditor(
                messaging = form.messaging,
                onChange = { transform -> viewModel.update { copy(messaging = messaging.transform()) } },
            )
        }
    }
}

@Composable
internal fun AppearanceSection(form: ComposerForm, viewModel: NotificationComposerViewModel) {
    ComposerSection(
        title = stringResource(R.string.composer_section_appearance),
        initiallyExpanded = false,
    ) {
        FieldPair(
            first = { mod ->
                ComposerTextField(
                    value = form.icon,
                    onValueChange = { v -> viewModel.update { copy(icon = v) } },
                    label = stringResource(R.string.composer_field_icon),
                    modifier = mod,
                )
            },
            second = { mod ->
                ComposerTextField(
                    value = form.largeIcon,
                    onValueChange = { v -> viewModel.update { copy(largeIcon = v) } },
                    label = stringResource(R.string.composer_field_large_icon),
                    modifier = mod,
                )
            },
        )
        Text(
            text = stringResource(R.string.composer_hint_image_source),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FieldPair(
            first = { mod ->
                ComposerTextField(
                    value = form.color,
                    onValueChange = { v -> viewModel.update { copy(color = v) } },
                    label = stringResource(R.string.composer_field_color),
                    placeholder = "#RRGGBB",
                    modifier = mod,
                )
            },
            second = { mod ->
                ComposerTextField(
                    value = form.ticker,
                    onValueChange = { v -> viewModel.update { copy(ticker = v) } },
                    label = stringResource(R.string.composer_field_ticker),
                    modifier = mod,
                )
            },
        )
        ComposerTextField(
            value = form.subText,
            onValueChange = { v -> viewModel.update { copy(subText = v) } },
            label = stringResource(R.string.composer_field_sub_text),
        )
    }
}

@Composable
internal fun BehaviorSection(form: ComposerForm, viewModel: NotificationComposerViewModel) {
    ComposerSection(
        title = stringResource(R.string.composer_section_behavior),
        initiallyExpanded = false,
    ) {
        ChoiceChipRow(
            label = stringResource(R.string.composer_field_priority),
            options = ComposerOptions.priorities.map { ChoiceOption(it) },
            selectedId = form.priority,
            onSelect = { v -> viewModel.update { copy(priority = v) } },
        )
        ChoiceChipRow(
            label = stringResource(R.string.composer_field_visibility),
            options = ComposerOptions.visibilities.map { ChoiceOption(it) },
            selectedId = form.visibility,
            onSelect = { v -> viewModel.update { copy(visibility = v) } },
        )
        SwitchRow(
            label = stringResource(R.string.composer_field_auto_cancel),
            checked = form.autoCancel,
            onCheckedChange = { v -> viewModel.update { copy(autoCancel = v) } },
        )
        SwitchRow(
            label = stringResource(R.string.composer_field_ongoing),
            checked = form.ongoing,
            onCheckedChange = { v -> viewModel.update { copy(ongoing = v) } },
        )
        SwitchRow(
            label = stringResource(R.string.composer_field_silent),
            checked = form.silent,
            onCheckedChange = { v -> viewModel.update { copy(silent = v) } },
        )
        SwitchRow(
            label = stringResource(R.string.composer_field_local_only),
            checked = form.localOnly,
            onCheckedChange = { v -> viewModel.update { copy(localOnly = v) } },
        )
        SwitchRow(
            label = stringResource(R.string.composer_field_start_monitoring),
            description = stringResource(R.string.composer_hint_start_monitoring),
            checked = form.startMonitoring,
            onCheckedChange = { v -> viewModel.update { copy(startMonitoring = v) } },
        )
        FieldPair(
            first = { mod ->
                ComposerTextField(
                    value = form.timestamp,
                    onValueChange = { v -> viewModel.update { copy(timestamp = v) } },
                    label = stringResource(R.string.composer_field_timestamp),
                    keyboardType = KeyboardType.Number,
                    modifier = mod,
                )
            },
            second = { mod ->
                ComposerTextField(
                    value = form.badge,
                    onValueChange = { v -> viewModel.update { copy(badge = v) } },
                    label = stringResource(R.string.composer_field_badge),
                    keyboardType = KeyboardType.Number,
                    modifier = mod,
                )
            },
        )
    }
}

@Composable
internal fun GroupingSection(form: ComposerForm, viewModel: NotificationComposerViewModel) {
    ComposerSection(
        title = stringResource(R.string.composer_section_grouping),
        initiallyExpanded = false,
    ) {
        FieldPair(
            first = { mod ->
                ComposerTextField(
                    value = form.group,
                    onValueChange = { v -> viewModel.update { copy(group = v) } },
                    label = stringResource(R.string.composer_field_group),
                    modifier = mod,
                )
            },
            second = { mod ->
                ComposerTextField(
                    value = form.sortKey,
                    onValueChange = { v -> viewModel.update { copy(sortKey = v) } },
                    label = stringResource(R.string.composer_field_sort_key),
                    modifier = mod,
                )
            },
        )
        SwitchRow(
            label = stringResource(R.string.composer_field_group_summary),
            checked = form.groupSummary,
            onCheckedChange = { v -> viewModel.update { copy(groupSummary = v) } },
        )
        FieldPair(
            first = { mod ->
                ComposerTextField(
                    value = form.channelId,
                    onValueChange = { v -> viewModel.update { copy(channelId = v) } },
                    label = stringResource(R.string.composer_field_channel_id),
                    modifier = mod,
                )
            },
            second = { mod ->
                ComposerTextField(
                    value = form.channelName,
                    onValueChange = { v -> viewModel.update { copy(channelName = v) } },
                    label = stringResource(R.string.composer_field_channel_name),
                    modifier = mod,
                )
            },
        )
    }
}

@Composable
internal fun TapActionSection(state: ComposerUiState, viewModel: NotificationComposerViewModel) {
    val form = state.form
    ComposerSection(
        title = stringResource(R.string.composer_section_tap_action),
        subtitle = stringResource(R.string.composer_section_tap_action_hint),
        initiallyExpanded = false,
    ) {
        SwitchRow(
            label = stringResource(R.string.composer_field_tap_enabled),
            checked = form.tapActionEnabled,
            onCheckedChange = { v -> viewModel.update { copy(tapActionEnabled = v) } },
        )
        if (form.tapActionEnabled) {
            ActionTypePicker(
                selectedId = form.tapAction,
                actionDefs = state.actionDefs,
                onSelect = viewModel::selectTapAction,
            )
            ParamsEditor(
                actionId = form.tapAction,
                entries = form.tapParams,
                onChange = { v -> viewModel.update { copy(tapParams = v) } },
                apps = state.apps,
                devicePermissions = state.missingPermissions,
                emptyHint = stringResource(R.string.composer_params_empty),
            )
        }
    }
}

@Composable
internal fun ActionButtonsSection(
    state: ComposerUiState,
    viewModel: NotificationComposerViewModel
) {
    val actions = state.form.actions
    ComposerSection(
        title = stringResource(R.string.composer_section_actions),
        subtitle = stringResource(
            R.string.composer_section_actions_count,
            actions.size,
            ComposerOptions.MAX_ACTIONS
        ),
    ) {
        actions.forEachIndexed { index, action ->
            ActionEditorCard(
                index = index,
                action = action,
                actionDefs = state.actionDefs,
                apps = state.apps,
                devicePermissions = state.missingPermissions,
                onChange = { transform -> viewModel.updateAction(action.uid, transform) },
                onSelectType = { viewModel.selectActionType(action.uid, it) },
                onRemove = { viewModel.removeAction(action.uid) },
            )
        }
        if (actions.size < ComposerOptions.MAX_ACTIONS) {
            TextButton(onClick = viewModel::addAction) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.composer_add_action),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun ExtrasSection(form: ComposerForm, viewModel: NotificationComposerViewModel) {
    ComposerSection(
        title = stringResource(R.string.composer_section_extras),
        initiallyExpanded = false,
    ) {
        KeyValueEditor(
            entries = form.extras,
            onChange = { v -> viewModel.update { copy(extras = v) } },
            emptyHint = stringResource(R.string.composer_extras_empty),
        )
    }
}

@Composable
internal fun LifecycleSection(form: ComposerForm, viewModel: NotificationComposerViewModel) {
    ComposerSection(
        title = stringResource(R.string.composer_section_lifecycle),
        initiallyExpanded = form.cancelOnly,
    ) {
        SwitchRow(
            label = stringResource(R.string.composer_field_cancel_only),
            description = stringResource(R.string.composer_hint_cancel_only),
            checked = form.cancelOnly,
            onCheckedChange = { v -> viewModel.update { copy(cancelOnly = v) } },
        )
        FieldPair(
            first = { mod ->
                ComposerTextField(
                    value = form.expiresAt,
                    onValueChange = { v -> viewModel.update { copy(expiresAt = v) } },
                    label = stringResource(R.string.composer_field_expires_at),
                    keyboardType = KeyboardType.Number,
                    modifier = mod,
                )
            },
            second = { mod ->
                ComposerTextField(
                    value = form.cancelAfterMs,
                    onValueChange = { v -> viewModel.update { copy(cancelAfterMs = v) } },
                    label = stringResource(R.string.composer_field_cancel_after),
                    keyboardType = KeyboardType.Number,
                    modifier = mod,
                )
            },
        )
        ComposerTextField(
            value = form.cancelIds,
            onValueChange = { v -> viewModel.update { copy(cancelIds = v) } },
            label = stringResource(R.string.composer_field_cancel_ids),
            supportingText = stringResource(R.string.composer_hint_cancel_ids),
        )
    }
}
