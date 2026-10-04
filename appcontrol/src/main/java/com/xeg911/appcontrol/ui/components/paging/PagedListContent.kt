package com.xeg911.appcontrol.ui.components.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.paging.PagedState
import com.xeg911.appcontrol.ui.components.ErrorState
import com.xeg911.appcontrol.ui.components.FullScreenLoading
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Items from the end at which the next batch is requested.
 */
private const val PREFETCH_DISTANCE = 6

/**
 * Generic batched list: shows loading / error / empty states, renders [content] in a
 * [LazyColumn] and asks for the next batch when the user scrolls near the end.
 * Any list that is backed by a [com.xeg911.appcontrol.core.paging.Paginator] can use it.
 */
@Composable
fun <T> PagedListContent(
    state: PagedState<T>,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    empty: @Composable () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    when {
        state.isRefreshing && state.items.isEmpty() -> FullScreenLoading(modifier = modifier)
        state.error != null && state.items.isEmpty() ->
            ErrorState(message = state.error, modifier = modifier, onRetry = onRetry)

        state.items.isEmpty() -> Box(modifier = modifier) { empty() }
        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
            ) {
                content()
                pagedFooter(state = state, onRetry = onLoadMore)
            }
            LoadMoreEffect(
                listState = listState,
                enabled = !state.endReached && !state.isLoadingMore && state.error == null,
                onLoadMore = onLoadMore,
            )
        }
    }
}

/**
 * Fires [onLoadMore] once each time the last visible item enters the prefetch window.
 */
@Composable
fun LoadMoreEffect(
    listState: LazyListState,
    enabled: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - PREFETCH_DISTANCE
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}

fun LazyListScope.pagedFooter(state: PagedState<*>, onRetry: () -> Unit) {
    when {
        state.isLoadingMore -> item(key = PAGED_FOOTER_KEY, contentType = PAGED_FOOTER_KEY) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        state.error != null -> item(key = PAGED_FOOTER_KEY, contentType = PAGED_FOOTER_KEY) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}

private const val PAGED_FOOTER_KEY = "__paged_footer"
