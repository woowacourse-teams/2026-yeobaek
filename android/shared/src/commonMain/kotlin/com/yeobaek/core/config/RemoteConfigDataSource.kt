package com.yeobaek.core.config

interface RemoteConfigDataSource {
    suspend fun initialize(defaults: Map<String, Any>)
    suspend fun fetch()
    suspend fun activate(): Boolean
    fun getValue(key: String): ConfigParameterValue
    fun getFetchInfo(): ConfigFetchInfo
}

data class ConfigParameterValue(
    val value: String,
    val source: ConfigValueSource,
)

data class ConfigFetchInfo(
    val lastSuccessfulFetchAtMillis: Long?,
    val status: ConfigFetchStatus,
)
