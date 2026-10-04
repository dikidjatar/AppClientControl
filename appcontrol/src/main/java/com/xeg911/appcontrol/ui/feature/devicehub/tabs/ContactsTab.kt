package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.paging.PagedState
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.SearchField
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.paging.PagedListContent
import com.xeg911.shared.data.model.DeviceContact
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MS = 300L

@Composable
fun ContactsTab(
    state: PagedState<DeviceContact>,
    query: String,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onCopied: (number: String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(query) }
    LaunchedEffect(draft) {
        delay(SEARCH_DEBOUNCE_MS.milliseconds)
        onSearch(draft)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = stringResource(R.string.contacts_section_title, state.countLabel))
        SearchField(
            query = draft,
            onQueryChange = { draft = it },
            placeholder = stringResource(R.string.contacts_search_placeholder),
            leadingIconRes = R.drawable.person_24px,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        PagedListContent(
            state = state,
            onLoadMore = onLoadMore,
            onRetry = onRefresh,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
            empty = {
                EmptyState(
                    title = if (draft.isBlank()) stringResource(R.string.contacts_empty_title)
                    else stringResource(R.string.notif_search_empty_title, draft),
                    subtitle = if (draft.isBlank()) stringResource(R.string.contacts_empty_subtitle)
                    else stringResource(R.string.paged_search_empty_subtitle),
                    icon = R.drawable.person_24px,
                )
            },
        ) {
            items(state.items, key = { it.contactId }) { contact ->
                ContactRow(contact = contact, onCopied = onCopied)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: DeviceContact,
    onCopied: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val name = contact.displayName.trim()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ContactAvatar(name = name)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = name.ifBlank { stringResource(R.string.contacts_unnamed) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (contact.phoneNumbers.isEmpty()) {
                Text(
                    text = stringResource(R.string.contacts_no_number),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    contact.phoneNumbers.forEach { number ->
                        AssistChip(
                            onClick = {
                                clipboard.setText(AnnotatedString(number))
                                onCopied(number)
                            },
                            label = { Text(number, style = MaterialTheme.typography.labelMedium) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            border = null,
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy_24px),
                                    contentDescription = stringResource(R.string.contacts_cd_copy_number),
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Initials on a hue derived from the name so the list is scannable. */
@Composable
private fun ContactAvatar(name: String) {
    val initials = name.split(' ').filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "#" }
    val hue = (name.hashCode().toUInt() % 360u).toFloat()
    val background = if (name.isBlank()) MaterialTheme.colorScheme.surfaceContainerHigh
    else Color.hsl(hue, 0.45f, 0.82f)
    val foreground = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
    else Color.hsl(hue, 0.5f, 0.28f)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = foreground,
        )
    }
}
