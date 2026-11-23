package com.yeobaek

import com.yeobaek.core.network.MutableNetworkMonitor
import com.yeobaek.core.network.NoOpNetworkMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorTest {
    @Test
    fun reportsRecoveryAfterDisconnectWithoutRepeatingUnchangedConnectivity() = runTest {
        val monitor = MutableNetworkMonitor()
        val states = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            monitor.isConnected.collect { states.add(it) }
        }

        monitor.updateConnected(true)
        monitor.updateConnected(true)
        monitor.updateConnected(false)
        monitor.updateConnected(false)
        monitor.updateConnected(true)

        assertEquals(listOf(false, true, false, true), states)
    }

    @Test
    fun closingRunsPlatformCleanupAndClearsConnectedState() {
        var cleanupCalled = false
        val monitor = MutableNetworkMonitor { cleanupCalled = true }
        monitor.updateConnected(true)

        monitor.close()

        assertTrue(cleanupCalled)
        assertFalse(monitor.isConnected.value)
    }

    @Test
    fun defaultMonitorDoesNotInventAConnection() {
        assertFalse(NoOpNetworkMonitor.isConnected.value)
        NoOpNetworkMonitor.close()
        assertFalse(NoOpNetworkMonitor.isConnected.value)
    }
}
