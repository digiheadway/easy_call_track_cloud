package com.miniclick.calltrackmanage.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Unified Network Connectivity Observer.
 * Provides a Flow-based API to observe network connectivity changes.
 * 
 * This is the canonical location - other references should use this class.
 */
class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Observe network connectivity as a Flow of Boolean values.
     * Emits true when connected, false when disconnected.
     */
    fun observe(): Flow<Boolean> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(true) }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch { send(false) }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(false) }
                }
            }

            // Send initial state
            val currentNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
            val isConnected = caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
            launch { send(isConnected) }

            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }

    /**
     * Check current connectivity status synchronously.
     */
    fun isConnected(): Boolean {
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps != null && (
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }
}
