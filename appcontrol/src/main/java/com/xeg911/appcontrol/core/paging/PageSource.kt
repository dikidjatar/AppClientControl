package com.xeg911.appcontrol.core.paging

import kotlinx.coroutines.flow.Flow

/**
 * Position after the last item of a page:
 * the ordering value plus the item key as tie-breaker.
 */
data class PageCursor(
    val value: Any?,
    val key: String
)

data class Page<T>(
    val items: List<T>,
    /**
     * null when this was the last page.
     */
    val nextCursor: PageCursor?,
)

interface PageSource<T> {

    /**
     * One-shot read of up to [limit] items positioned after [after] (null = first page).
     */
    suspend fun load(after: PageCursor?, limit: Int): Page<T>

    /**
     * Live view of every item from the start of the ordering up to and including [upTo]
     * (all items when null). Keeps the head of the list fresh while older pages stay static.
     */
    fun observeHead(upTo: PageCursor?): Flow<Result<List<T>>>
}
