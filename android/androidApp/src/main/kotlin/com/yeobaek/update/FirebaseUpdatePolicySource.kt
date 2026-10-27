package com.yeobaek.update

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.yeobaek.core.update.UpdatePolicy
import com.yeobaek.core.update.UpdatePolicyPlatform
import com.yeobaek.core.update.UpdatePolicySource
import kotlinx.coroutines.tasks.await

class FirebaseUpdatePolicySource(
    isDebug: Boolean,
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) : UpdatePolicySource {
    private val platform = UpdatePolicyPlatform.ANDROID
    private val settingsTask = remoteConfig.setConfigSettingsAsync(
        FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (isDebug) 0 else 3_600)
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
