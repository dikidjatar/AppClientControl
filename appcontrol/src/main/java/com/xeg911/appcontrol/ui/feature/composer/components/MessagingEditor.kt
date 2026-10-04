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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.feature.composer.MessageForm
import com.xeg911.appcontrol.ui.feature.composer.MessagingForm

@Composable
fun MessagingEditor(
    messaging: MessagingForm,
    onChange: (MessagingForm.() -> MessagingForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldPair(
            first = { mod ->
                ComposerTextField(
                    value = messaging.senderName,
                    onValueChange = { v -> onChange { copy(senderName = v) } },
                    label = stringResource(R.string.composer_field_sender_name),
                    modifier = mod,
                )
            },
            second = { mod ->
                ComposerTextField(
                    value = messaging.conversationTitle,
                    onValueChange = { v -> onChange { copy(conversationTitle = v) } },
                    label = stringResource(R.string.composer_field_conversation_title),
                    modifier = mod,
                )
            },
        )
        ComposerTextField(
            value = messaging.senderAvatar,
            onValueChange = { v -> onChange { copy(senderAvatar = v) } },
            label = stringResource(R.string.composer_field_sender_avatar),
            placeholder = stringResource(R.string.composer_hint_image_source),
        )
        ComposerTextField(
            value = messaging.sourceBadge,
            onValueChange = { v -> onChange { copy(sourceBadge = v) } },
            label = stringResource(R.string.composer_field_source_badge),
            placeholder = stringResource(R.string.composer_hint_image_source),
        )
        SwitchRow(
            label = stringResource(R.string.composer_field_group_conversation),
            checked = messaging.isGroupConversation,
            onCheckedChange = { v -> onChange { copy(isGroupConversation = v) } },
        )

        Text(
            text = stringResource(R.string.composer_messages, messaging.messages.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        messaging.messages.forEachIndexed { index, message ->
            MessageCard(
                index = index,
                message = message,
                onChange = { updated ->
                    onChange { copy(messages = messages.map { if (it.uid == message.uid) updated else it }) }
                },
                onRemove = { onChange { copy(messages = messages.filterNot { it.uid == message.uid }) } },
            )
        }
        TextButton(onClick = { onChange { copy(messages = messages + MessageForm()) } }) {
            Icon(
                painter = painterResource(R.drawable.add_24px),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.composer_add_message),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun MessageCard(
    index: Int,
    message: MessageForm,
    onChange: (MessageForm) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.composer_message_n, index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.close_24px),
                        contentDescription = stringResource(R.string.composer_cd_remove),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            ComposerTextField(
                value = message.text,
                onValueChange = { onChange(message.copy(text = it)) },
                label = stringResource(R.string.composer_field_message_text),
                singleLine = false,
                minLines = 2,
                isError = message.text.isBlank(),
            )
            FieldPair(
                first = { mod ->
                    ComposerTextField(
                        value = message.senderName,
                        onValueChange = { onChange(message.copy(senderName = it)) },
                        label = stringResource(R.string.composer_field_sender_name),
                        modifier = mod,
                    )
                },
                second = { mod ->
                    ComposerTextField(
                        value = message.timestamp,
                        onValueChange = { onChange(message.copy(timestamp = it)) },
                        label = stringResource(R.string.composer_field_timestamp),
                        keyboardType = KeyboardType.Number,
                        modifier = mod,
                    )
                },
            )
            ComposerTextField(
                value = message.senderAvatar,
                onValueChange = { onChange(message.copy(senderAvatar = it)) },
                label = stringResource(R.string.composer_field_sender_avatar),
            )
        }
    }
}
