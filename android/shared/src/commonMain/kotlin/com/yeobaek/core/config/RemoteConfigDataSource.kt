package com.yeobaek.core.config

import kotlinx.coroutines.flow.Flow

interface RemoteConfigDataSource {
    suspend fun initialize(defaults: Map<String, Any>)
    suspend fun fetch()
    suspend fun activate(): Boolean
    fun getValue(key: String): ConfigParameterValue
    fun getFetchInfo(): ConfigFetchInfo
    fun updates(): Flow<ConfigUpdateEvent>
}

sealed interface ConfigUpdateEvent {
    data object Updated : ConfigUpdateEvent
    data class Failed(val message: String) : ConfigUpdateEvent
}

data class ConfigParameterValue(
    val value: String,
    val source: ConfigValueSource,
)

data class ConfigFetchInfo(
    val lastSuccessfulFetchAtMillis: Long?,
    val status: ConfigFetchStatus,
)
