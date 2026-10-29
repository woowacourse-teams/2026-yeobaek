package com.yeobaek.core.config

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun firebaseConfigUpdates(): Flow<ConfigUpdateEvent> = callbackFlow {
    val registration = FirebaseRemoteConfig.getInstance().addOnConfigUpdateListener(
        object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                trySend(ConfigUpdateEvent.Updated)
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                trySend(ConfigUpdateEvent.Failed(error.message ?: "실시간 원격 설정 수신에 실패했습니다."))
            }
        },
    )
    awaitClose { registration.remove() }
}
