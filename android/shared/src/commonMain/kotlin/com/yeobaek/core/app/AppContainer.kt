package com.yeobaek.core.app

import com.yeobaek.core.network.NetworkProvider

class AppContainer {
    val networkProvider = NetworkProvider()

    fun close() {
        networkProvider.close()
    }
}
