package com.xeg911.appcontrol.core.util

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

inline fun <reified T> DataSnapshot.getValueOrNull(): T? =
    runCatching { getValue(T::class.java) }.getOrNull()

/**
 * Observes a [Query] (or [DatabaseReference]) as a continuous [Flow] of [DataSnapshot].
 * Emits [Result.success] on every data change, [Result.failure] on error.
 * The listener is removed when the flow collector is canceled.
 */
fun Query.observeFlow(): Flow<Result<DataSnapshot>> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(Result.success(snapshot))
        }

        override fun onCancelled(error: DatabaseError) {
            trySend(Result.failure(error.toException()))
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

/**
 * Maps each child [DataSnapshot] into a non-null [T] using [transform].
 */
inline fun <reified T> DataSnapshot.childrenAs(
    crossinline transform: (DataSnapshot) -> T?,
): List<T> = children.mapNotNull(transform)