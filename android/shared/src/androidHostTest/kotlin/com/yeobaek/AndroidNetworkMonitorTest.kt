package com.yeobaek

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.yeobaek.core.network.AndroidNetworkMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class AndroidNetworkMonitorTest {
    @Test
    fun waitsForValidatedInternetAndClearsStateWhenDefaultNetworkIsLost() {
        withMonitor { monitor, callback ->
            val network = ShadowNetwork.newInstance(1)
            callback.onAvailable(network)
            assertFalse(monitor.isConnected.value)
            callback.onCapabilitiesChanged(
                network,
                capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            )
            assertFalse(monitor.isConnected.value)
            callback.onCapabilitiesChanged(network, validatedInternet())
            assertTrue(monitor.isConnected.value)
            callback.onLost(network)
            assertFalse(monitor.isConnected.value)
        }
    }

    @Test
    fun oldNetworkCallbacksCannotClearTheNewDefaultConnection() {
        withMonitor { monitor, callback ->
            val oldNetwork = ShadowNetwork.newInstance(1)
            val newNetwork = ShadowNetwork.newInstance(2)
            callback.onAvailable(oldNetwork)
            callback.onCapabilitiesChanged(oldNetwork, validatedInternet())
            callback.onAvailable(newNetwork)
            callback.onCapabilitiesChanged(newNetwork, validatedInternet())

            callback.onLost(oldNetwork)
            callback.onCapabilitiesChanged(oldNetwork, NetworkCapabilities())

            assertTrue(monitor.isConnected.value)
        }
    }

    @Test
    fun closeUnregistersCallbackOnceAndIgnoresLateConnectionEvents() {
        withMonitor { monitor, callback ->
            val manager = RuntimeEnvironment.getApplication().getSystemService(ConnectivityManager::class.java)
            monitor.close()
            monitor.close()
            val network = ShadowNetwork.newInstance(1)
            callback.onAvailable(network)
            callback.onCapabilitiesChanged(network, validatedInternet())

            assertFalse(monitor.isConnected.value)
            assertTrue(shadowOf(manager).networkCallbacks.isEmpty())
        }
    }

    private fun validatedInternet(): NetworkCapabilities = capabilities(
        NetworkCapabilities.NET_CAPABILITY_INTERNET,
        NetworkCapabilities.NET_CAPABILITY_VALIDATED,
    )

    private fun capabilities(vararg capabilities: Int): NetworkCapabilities =
        ShadowNetworkCapabilities.newInstance().also { networkCapabilities ->
            capabilities.forEach { shadowOf(networkCapabilities).addCapability(it) }
        }

    private fun withMonitor(block: (AndroidNetworkMonitor, ConnectivityManager.NetworkCallback) -> Unit) {
        val context = RuntimeEnvironment.getApplication()
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val monitor = AndroidNetworkMonitor(context)
        try {
            assertEquals(1, shadowOf(manager).networkCallbacks.size)
            block(monitor, shadowOf(manager).networkCallbacks.single())
        } finally {
            monitor.close()
        }
    }
}
