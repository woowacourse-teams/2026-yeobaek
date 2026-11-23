package com.yeobaek.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.StateFlow

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val state = MutableNetworkMonitor()
    private val lock = Any()
    private var currentNetwork: Network? = null
    private var closed = false
    override val isConnected: StateFlow<Boolean> = state.isConnected

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(lock) {
                if (closed) return
                currentNetwork = network
                state.updateConnected(false)
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            synchronized(lock) {
                if (closed || network != currentNetwork) return
                state.updateConnected(
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                )
            }
        }

        override fun onLost(network: Network) {
            synchronized(lock) {
                if (closed || network != currentNetwork) return
                currentNetwork = null
                state.updateConnected(false)
            }
        }
    }

    init {
        manager.registerDefaultNetworkCallback(callback)
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            manager.unregisterNetworkCallback(callback)
            currentNetwork = null
            state.close()
        }
    }
}
