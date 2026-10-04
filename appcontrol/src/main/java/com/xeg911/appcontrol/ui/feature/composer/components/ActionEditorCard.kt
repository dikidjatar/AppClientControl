package com.xeg911.appcontrol.ui.feature.composer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.feature.composer.ActionForm
import com.xeg911.appcontrol.ui.feature.composer.ActionUiSpec
import com.xeg911.appcontrol.ui.feature.composer.PermissionPickOption
import com.xeg911.shared.data.model.InstalledApp
import com.xeg911.shared.data.model.notification.SupportedActionDef

@Composable
fun ActionEditorCard(
    index: Int,
    action: ActionForm,
    actionDefs: List<SupportedActionDef>,
    apps: List<InstalledApp>,
    devicePermissions: List<PermissionPickOption>,
    onChange: (ActionForm.() -> ActionForm) -> Unit,
    onSelectType: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.composer_action_n, index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = stringResource(R.string.composer_cd_remove),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            ActionTypePicker(
                selectedId = action.action,
                actionDefs = actionDefs,
                onSelect = onSelectType,
            )

            FieldPair(
                first = { mod ->
                    ComposerTextField(
                        value = action.label,
                        onValueChange = { v -> onChange { copy(label = v) } },
                        label = stringResource(R.string.composer_field_action_label),
                        isError = action.label.isBlank(),
                        modifier = mod,
                    )
                },
                second = { mod ->
                    ComposerTextField(
                        value = action.id,
                        onValueChange = { v -> onChange { copy(id = v) } },
                        label = stringResource(R.string.composer_field_action_id),
                        modifier = mod,
                    )
                },
            )

            FieldPair(
                first = { mod ->
                    ComposerTextField(
                        value = action.icon,
                        onValueChange = { v -> onChange { copy(icon = v) } },
                        label = stringResource(R.string.composer_field_action_icon),
                        modifier = mod,
                    )
                },
                second = { mod ->
                    ComposerTextField(
                        value = action.semantic,
                        onValueChange = { v -> onChange { copy(semantic = v) } },
                        label = stringResource(R.string.composer_field_semantic),
                        keyboardType = KeyboardType.Number,
                        modifier = mod,
                    )
                },
            )

            if (ActionUiSpec.supportsInlineReply(action.action)) {
                SwitchRow(
                    label = stringResource(R.string.composer_field_allow_replies),
                    checked = action.allowReplies,
                    onCheckedChange = { v -> onChange { copy(allowReplies = v) } },
                )
            }

            Text(
                text = stringResource(R.string.composer_params),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ParamsEditor(
                actionId = action.action,
                entries = action.params,
                onChange = { v -> onChange { copy(params = v) } },
                apps = apps,
                devicePermissions = devicePermissions,
                emptyHint = stringResource(R.string.composer_params_empty),
            )
        }
    }
}
