package com.yeobaek.update

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.yeobaek.core.update.RealtimeUpdatePolicySource
import com.yeobaek.core.update.UpdatePolicy
import com.yeobaek.core.update.UpdatePolicyPlatform
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await

class FirebaseUpdatePolicySource(
    isDebug: Boolean,
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) : RealtimeUpdatePolicySource {
    private val platform = UpdatePolicyPlatform.ANDROID
    private val settingsTask = remoteConfig.setConfigSettingsAsync(
        FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (isDebug) 0 else 7_200)
            .setFetchTimeoutInSeconds(10)
            .build(),
    )
    private val defaultsTask = remoteConfig.setDefaultsAsync(
        mapOf(
            platform.minimumVersionKey to 0,
            UpdatePolicy.FORCE_UPDATE_MESSAGE_KEY to UpdatePolicy.DEFAULT_FORCE_UPDATE_MESSAGE,
        ),
    )

    override suspend fun fetch(): UpdatePolicy {
        settingsTask.await()
        defaultsTask.await()
        remoteConfig.fetchAndActivate().await()
        return readPolicy()
    }

    override suspend fun activate(): UpdatePolicy {
        settingsTask.await()
        defaultsTask.await()
        remoteConfig.activate().await()
        return readPolicy()
    }

    override fun updates(onFailure: (Exception) -> Unit): Flow<Unit> = callbackFlow {
        settingsTask.await()
        defaultsTask.await()
        val registration = remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                if (configUpdate.updatedKeys.any {
                        it == platform.minimumVersionKey || it == UpdatePolicy.FORCE_UPDATE_MESSAGE_KEY
                    }
                ) {
                    trySend(Unit)
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                onFailure(error)
            }
        })
        awaitClose { registration.remove() }
    }.conflate()

    private fun readPolicy(): UpdatePolicy {
        val minimumVersion = remoteConfig.getValue(platform.minimumVersionKey)
        val message = remoteConfig.getValue(UpdatePolicy.FORCE_UPDATE_MESSAGE_KEY)
        require(
            minimumVersion.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE &&
                message.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
        ) { "Both update policy parameters must be published in Remote Config." }

        return UpdatePolicy.fromRemoteValues(
            minimumVersion = minimumVersion.asString(),
            message = message.asString(),
        )
    }
}
