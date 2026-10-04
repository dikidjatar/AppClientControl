package com.xeg911.appcontrol.core.paging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedState<T>(
    val items: List<T> = emptyList(),
    /**
     * True until the first page has arrived (also after [Paginator.refresh]).
     */
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = items.isEmpty() && !isRefreshing && error == null

    val countLabel: String
        get() = if (endReached) items.size.toString() else "${items.size}+"
}

/**
 * Generic batch loader. Every batch is a one-shot server read ([PageSource.load]), only the
 * first-page range is kept live through [PageSource.observeHead]. Nothing is fetched until
 * [refresh] (or [setQuery]) is called, and [stop] releases the live listener, so a screen
 * only pays for the list while it is actually visible.
 */
class Paginator<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int,
    private val itemKey: (T) -> Any,
    private val sourceFactory: (query: String) -> PageSource<T>,
) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    val isActive: Boolean get() = source != null

    private var source: PageSource<T>? = null
    private var headJob: Job? = null
    private var loadJob: Job? = null
    private var head: List<T> = emptyList()
    private var tail: List<T> = emptyList()
    private var nextCursor: PageCursor? = null
    private var removed: Set<Any> = emptySet()
    private var generation = 0

    fun setQuery(query: String) {
        val clean = query.trim()
        if (clean == _query.value && source != null) return
        _query.value = clean
        refresh()
    }

    fun refresh() {
        val gen = reset()
        val src = sourceFactory(_query.value).also { source = it }
        _state.value = PagedState(isRefreshing = true)

        loadJob = scope.launch {
            val first = runCatching { src.load(after = null, limit = pageSize) }
                .getOrElse { e -> fail(e); return@launch }
            if (gen != generation) return@launch
            head = first.items
            nextCursor = first.nextCursor
            publish(isRefreshing = false)
            // Everything up to the first-page boundary stays live; older pages are static.
            headJob = src.observeHead(upTo = first.nextCursor)
                .onEach { result ->
                    result.fold(onSuccess = { head = it; publish() }, onFailure = ::fail)
                }
                .launchIn(scope)
        }
    }

    /**
     * Cancels the live listener and drops loaded batches,
     * the next [refresh] starts over.
     */
    fun stop() {
        reset()
        source = null
        _state.value = PagedState(isRefreshing = true)
    }

    fun loadNext() {
        val src = source ?: return
        val cursor = nextCursor ?: return
        val current = _state.value
        if (current.isRefreshing || current.isLoadingMore || loadJob?.isActive == true) return
        val gen = generation
        _state.update { it.copy(isLoadingMore = true, error = null) }
        loadJob = scope.launch {
            runCatching { src.load(after = cursor, limit = pageSize) }.fold(
                onSuccess = { page ->
                    if (gen != generation) return@launch
                    tail = tail + page.items
                    nextCursor = page.nextCursor
                    publish(isLoadingMore = false)
                },
                onFailure = ::fail,
            )
        }
    }

    /**
     * Optimistic local removal, the live head will confirm it, static pages need this.
     */
    fun remove(predicate: (T) -> Boolean) {
        val keys = (head + tail).filter(predicate).map(itemKey)
        if (keys.isEmpty()) return
        removed = removed + keys
        publish()
    }

    private fun reset(): Int {
        headJob?.cancel()
        loadJob?.cancel()
        headJob = null
        loadJob = null
        head = emptyList()
        tail = emptyList()
        nextCursor = null
        removed = emptySet()
        return ++generation
    }

    private fun publish(
        isRefreshing: Boolean = _state.value.isRefreshing,
        isLoadingMore: Boolean = _state.value.isLoadingMore,
    ) {
        val seen = HashSet<Any>()
        val merged = (head + tail).filter { item ->
            val key = itemKey(item)
            key !in removed && seen.add(key)
        }
        _state.update {
            it.copy(
                items = merged,
                isRefreshing = isRefreshing,
                isLoadingMore = isLoadingMore,
                endReached = nextCursor == null,
                error = null,
            )
        }
    }

    private fun fail(e: Throwable) {
        _state.update {
            it.copy(
                isRefreshing = false,
                isLoadingMore = false,
                error = e.localizedMessage ?: e.message ?: e.javaClass.simpleName,
            )
        }
    }
}
