package com.yeobaek.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface NetworkMonitor {
    val isConnected: StateFlow<Boolean>

    fun close()
}

class MutableNetworkMonitor(private val onClose: () -> Unit) : NetworkMonitor {
    constructor() : this({})

    private val connected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = connected.asStateFlow()

    fun updateConnected(connected: Boolean) {
        this.connected.value = connected
    }

    override fun close() {
        onClose()
        connected.value = false
    }
}

object NoOpNetworkMonitor : NetworkMonitor {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun close() = Unit
}
