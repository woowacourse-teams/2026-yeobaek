package com.yeobaek.core.config

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.FetchStatus
import dev.gitlive.firebase.remoteconfig.ValueSource
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow

class FirebaseRemoteConfigDataSource : RemoteConfigDataSource {
    private val remoteConfig = Firebase.remoteConfig

    override suspend fun initialize(defaults: Map<String, Any>) {
        remoteConfig.settings {
            minimumFetchInterval = 2.hours
            fetchTimeout = 10.seconds
        }
        remoteConfig.setDefaults(*defaults.toList().toTypedArray())
        remoteConfig.ensureInitialized()
    }

    override suspend fun fetch() {
        remoteConfig.fetch()
    }

    override suspend fun activate(): Boolean = remoteConfig.activate()

    override fun updates(): Flow<ConfigUpdateEvent> = firebaseConfigUpdates()

    override fun getValue(key: String): ConfigParameterValue {
        val value = remoteConfig.getValue(key)
        return ConfigParameterValue(
            value = value.asString(),
            source = if (value.getSource() == ValueSource.Remote) {
                ConfigValueSource.REMOTE
            } else {
                ConfigValueSource.DEFAULT
            },
        )
    }

    override fun getFetchInfo(): ConfigFetchInfo {
        val info = remoteConfig.info
        return ConfigFetchInfo(
            lastSuccessfulFetchAtMillis = info.fetchTime.toEpochMilliseconds().takeIf { it > 0 },
            status = when (info.lastFetchStatus) {
                FetchStatus.NoFetchYet -> ConfigFetchStatus.NOT_FETCHED
                FetchStatus.Success -> ConfigFetchStatus.SUCCESS
                FetchStatus.Failure -> ConfigFetchStatus.FAILURE
                FetchStatus.Throttled -> ConfigFetchStatus.THROTTLED
            },
        )
    }
}
