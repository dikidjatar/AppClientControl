package com.xeg911.appclient.monitoring

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.shared.firebase.FirebasePaths.Connectivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
) {
    private data class NetworkState(val transport: String, val ip: String)

    private val connectivityManager get() = context.getSystemService(ConnectivityManager::class.java)
    private val ticks = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var callback: ConnectivityManager.NetworkCallback? = null

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        stop()
        ticks.debounce(DEBOUNCE_MS).map { currentState() }.distinctUntilChanged().onEach { state ->
            deviceRepository.updateConnectivity(
                mapOf(
                    Connectivity.TRANSPORT to state.transport,
                    Connectivity.IP to state.ip,
                    Connectivity.LAST_UPDATED to System.currentTimeMillis(),
                )
            ).onFailure { Log.w(TAG, "Connectivity push failed", it) }
        }.launchIn(scope)

        deviceRepository.observePingRequest().filter { it > 0L }.distinctUntilChanged()
            .onEach { requestedAt ->
                val now = System.currentTimeMillis()
                deviceRepository.updateConnectivity(
                    mapOf(
                        Connectivity.PING_RESPONSE to now,
                        Connectivity.PING_LATENCY_MS to (now - requestedAt),
                    )
                )
            }.catch { Log.w(TAG, "Ping listener failed", it) }.launchIn(scope)

        registerCallback()
        ticks.tryEmit(Unit)
    }

    fun stop() {
        callback?.let { runCatching { connectivityManager?.unregisterNetworkCallback(it) } }
        callback = null
    }

    private fun registerCallback() {
        val cm = connectivityManager ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = tick()
            override fun onLost(network: Network) = tick()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = tick()
            private fun tick() {
                ticks.tryEmit(Unit)
            }
        }
        val request =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        runCatching { cm.registerNetworkCallback(request, cb) }.onSuccess { callback = cb }
            .onFailure { Log.w(TAG, "Could not register network callback", it) }
    }

    private fun currentState(): NetworkState {
        val cm = connectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        return NetworkState(transport = transportOf(caps), ip = ipv4Address())
    }

    private fun transportOf(caps: NetworkCapabilities?): String = when {
        caps == null -> TRANSPORT_NONE
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
        else -> TRANSPORT_NONE
    }

    private fun ipv4Address(): String = runCatching {
        NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress.orEmpty()
    }.getOrDefault("")

    private companion object {
        const val TAG = "ConnectivityMonitor"
        const val TRANSPORT_NONE = "NONE"
        const val DEBOUNCE_MS = 1_500L
    }
}
