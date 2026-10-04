package com.xeg911.appcontrol.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.shared.firebase.FirebasePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connection state of the AppControl device itself.
 *
 * [NetworkState.online] is the validated internet flag from the system,
 * [NetworkState.realtimeDbConnected] mirrors Firebase's `.info/connected`,
 * so the UI can distinguish "no internet" from "internet, but Realtime Database
 * is unreachable / reconnecting".
 */
data class NetworkState(
    val online: Boolean = true,
    val realtimeDbConnected: Boolean = true,
) {
    val isFullyConnected: Boolean
        get() = online && realtimeDbConnected
}

@Singleton
class NetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: FirebaseDatabase,
) {
    private val systemOnline: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emit()
            override fun onLost(network: Network) = emit()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = emit()
            private fun emit() {
                trySend(cm.hasValidatedInternet())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { cm.registerNetworkCallback(request, callback) }
        trySend(cm.hasValidatedInternet())
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()

    private val realtimeDbConnected: Flow<Boolean> =
        database.getReference(FirebasePaths.INFO_CONNECTED).observeFlow()
            .map { result -> result.getOrNull()?.getValue(Boolean::class.java) ?: false }
            .onStart { emit(true) }
            .distinctUntilChanged()

    val state: Flow<NetworkState> =
        combine(systemOnline, realtimeDbConnected) { online, db ->
            NetworkState(online = online, realtimeDbConnected = db)
        }.distinctUntilChanged()

    private fun ConnectivityManager.hasValidatedInternet(): Boolean {
        val caps = getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
