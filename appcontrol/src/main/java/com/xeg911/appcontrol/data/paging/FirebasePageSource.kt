package com.xeg911.appcontrol.data.paging

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import com.xeg911.appcontrol.core.paging.Page
import com.xeg911.appcontrol.core.paging.PageCursor
import com.xeg911.appcontrol.core.paging.PageSource
import com.xeg911.appcontrol.core.util.observeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Realtime Database batch source ordered by the child field [orderBy].
 *
 * - Ascending: `startAfter(cursor).limitToFirst(n)`; [prefix] narrows to values starting
 *   with it (case-sensitive, so callers should order on a lower-cased field).
 * - Descending: `endBefore(cursor).limitToLast(n)` and the page is reversed.
 */
class FirebasePageSource<T>(
    private val ref: Query,
    private val orderBy: String,
    private val descending: Boolean = false,
    private val prefix: String? = null,
    private val decode: (DataSnapshot) -> T?,
) : PageSource<T> {

    override suspend fun load(after: PageCursor?, limit: Int): Page<T> {
        val query = if (descending) {
            (after?.let { ordered().endBefore(it) } ?: ordered()).limitToLast(limit)
        } else {
            (after?.let { ordered().startAfter(it) } ?: ordered().withPrefixStart())
                .withPrefixEnd()
                .limitToFirst(limit)
        }
        val children = query.get().await().children.toList().oriented()
        val last = children.lastOrNull()
        val nextCursor = last?.takeIf { children.size >= limit }?.let { cursorOf(it) }
        return Page(items = children.mapNotNull(decode), nextCursor = nextCursor)
    }

    override fun observeHead(upTo: PageCursor?): Flow<Result<List<T>>> {
        val query = if (descending) {
            upTo?.let { ordered().startAt(it) } ?: ordered()
        } else {
            val start = ordered().withPrefixStart()
            upTo?.let { start.endAt(it) } ?: start.withPrefixEnd()
        }
        return query.observeFlow().map { result ->
            result.map { snapshot -> snapshot.children.toList().oriented().mapNotNull(decode) }
        }
    }

    private fun ordered() = ref.orderByChild(orderBy)

    private fun List<DataSnapshot>.oriented() = if (descending) asReversed() else this

    private fun cursorOf(snapshot: DataSnapshot) =
        PageCursor(value = snapshot.child(orderBy).value, key = snapshot.key.orEmpty())

    private fun Query.withPrefixStart() = if (prefix.isNullOrEmpty()) this else startAt(prefix)
    private fun Query.withPrefixEnd() =
        if (prefix.isNullOrEmpty()) this else endAt(prefix + PREFIX_END)

    // Realtime Database has typed overloads, dispatch on the cursor's runtime type.
    private fun Query.startAfter(c: PageCursor): Query = when (val v = c.value) {
        is Number -> startAfter(v.toDouble(), c.key)
        is Boolean -> startAfter(v, c.key)
        else -> startAfter(v as? String, c.key)
    }

    private fun Query.startAt(c: PageCursor): Query = when (val v = c.value) {
        is Number -> startAt(v.toDouble(), c.key)
        is Boolean -> startAt(v, c.key)
        else -> startAt(v as? String, c.key)
    }

    private fun Query.endAt(c: PageCursor): Query = when (val v = c.value) {
        is Number -> endAt(v.toDouble(), c.key)
        is Boolean -> endAt(v, c.key)
        else -> endAt(v as? String, c.key)
    }

    private fun Query.endBefore(c: PageCursor): Query = when (val v = c.value) {
        is Number -> endBefore(v.toDouble(), c.key)
        is Boolean -> endBefore(v, c.key)
        else -> endBefore(v as? String, c.key)
    }

    private companion object {
        /**
         * Highest code point Firebase orders after any prefix match.
         */
        const val PREFIX_END = "\uf8ff"
    }
}
