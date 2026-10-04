package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.shared.data.model.InstalledApp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MS = 300L

@Composable
fun AppsTab(
    state: PagedState<InstalledApp>,
    query: String,
    sendingPackages: Set<String>,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onPickForAction: (InstalledApp) -> Unit,
    onSend: (InstalledApp) -> Unit,
    onCopied: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(query) }
    LaunchedEffect(draft) {
        delay(SEARCH_DEBOUNCE_MS.milliseconds)
        onSearch(draft)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = stringResource(R.string.apps_section_title, state.countLabel))
        SearchField(
            query = draft,
            onQueryChange = { draft = it },
            placeholder = stringResource(R.string.apps_search_placeholder),
            leadingIconRes = R.drawable.apps_24px,
        )
        Text(
            text = stringResource(R.string.apps_pick_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                    title = if (draft.isBlank()) stringResource(R.string.apps_empty_title)
                    else stringResource(R.string.notif_search_empty_title, draft),
                    subtitle = if (draft.isBlank()) stringResource(R.string.apps_empty_subtitle)
                    else stringResource(R.string.paged_search_empty_subtitle),
                    icon = R.drawable.apps_24px,
                )
            },
        ) {
            items(state.items, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    isSending = app.packageName in sendingPackages,
                    onClick = { onPickForAction(app) },
                    onSend = { onSend(app) },
                    onCopied = onCopied,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    isSending: Boolean,
    onClick: () -> Unit,
    onSend: () -> Unit,
    onCopied: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val label = app.appName.ifBlank { app.packageName }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppAvatar(label = label)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(app.packageName))
                        onCopied()
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.content_copy_24px),
                        contentDescription = stringResource(R.string.apps_cd_copy_package),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.apps_meta,
                    app.versionName.ifBlank { "—" },
                    app.versionCode,
                    formatRelativeTime(app.lastUpdateTime),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        FilledTonalButton(
            onClick = onSend,
            enabled = !isSending,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp),
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(R.drawable.send_24px),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = stringResource(R.string.apps_action_send),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun AppAvatar(label: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
