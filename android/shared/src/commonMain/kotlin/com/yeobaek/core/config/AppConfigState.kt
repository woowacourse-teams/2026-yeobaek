package com.yeobaek.core.config

data class AppConfigState(
    val featureFlags: FeatureFlags = FeatureFlags(),
    val sessionMessages: SessionMessages = SessionMessages(),
    val valueSources: Map<String, ConfigValueSource> = mapOf(
        RemoteConfigKeys.COMMENT_LIKE_ENABLED to ConfigValueSource.DEFAULT,
        RemoteConfigKeys.ONBOARDING_TITLE to ConfigValueSource.DEFAULT,
    ),
    val isInitialized: Boolean = false,
    val isFetching: Boolean = false,
    val lastFetchAttemptAtMillis: Long? = null,
    val lastSuccessfulFetchAtMillis: Long? = null,
    val lastFetchStatus: ConfigFetchStatus = ConfigFetchStatus.NOT_FETCHED,
    val lastActivationAtMillis: Long? = null,
    val lastError: String? = null,
)

enum class ConfigValueSource {
    DEFAULT,
    REMOTE,
}

enum class ConfigFetchStatus {
    NOT_FETCHED,
    SUCCESS,
    FAILURE,
    THROTTLED,
}
