package com.xeg911.appclient.core.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Cold flow of snapshots, the listener is removed when the collector cancels.
 */
fun Query.observe(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot)
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

suspend fun DatabaseReference.setValueAwait(value: Any?) {
    setValue(value).await()
}

suspend fun DatabaseReference.updateChildrenAwait(update: Map<String, Any?>) {
    updateChildren(update).await()
}

suspend fun DatabaseReference.getAwait(): DataSnapshot = get().await()

inline fun <reified T> DataSnapshot.valueOrNull(): T? =
    runCatching { getValue(T::class.java) }.getOrNull()
