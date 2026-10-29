package com.yeobaek.core.config

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.ios
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalForeignApi::class)
actual fun firebaseConfigUpdates(): Flow<ConfigUpdateEvent> = callbackFlow {
    val registration = Firebase.remoteConfig.ios.addOnConfigUpdateListener { update, error ->
        when {
            error != null -> trySend(ConfigUpdateEvent.Failed(error.localizedDescription))
            update != null -> trySend(ConfigUpdateEvent.Updated)
        }
    }
    awaitClose { registration.remove() }
}
