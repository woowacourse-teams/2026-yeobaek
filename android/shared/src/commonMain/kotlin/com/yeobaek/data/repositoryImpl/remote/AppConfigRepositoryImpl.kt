package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.core.config.AppConfigState
import com.yeobaek.core.config.ConfigFetchStatus
import com.yeobaek.core.config.ConfigValueSource
import com.yeobaek.core.config.FeatureFlags
import com.yeobaek.core.config.RemoteConfigDataSource
import com.yeobaek.core.config.RemoteConfigKeys
import com.yeobaek.core.config.SessionMessages
import com.yeobaek.data.repository.AppConfigRepository
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppConfigRepositoryImpl(
    private val dataSource: RemoteConfigDataSource,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : AppConfigRepository {
    private val _state = MutableStateFlow(AppConfigState())
    override val state = _state.asStateFlow()
    private val mutex = Mutex()

    override suspend fun initialize(): Result<Unit> = mutex.withLock {
        initializeIfNeeded()
    }

    private suspend fun initializeIfNeeded(): Result<Unit> {
        if (_state.value.isInitialized) return Result.success(Unit)

        return try {
            dataSource.initialize(
                mapOf(
                    RemoteConfigKeys.COMMENT_LIKE_ENABLED to FeatureFlags().commentLikeEnabled,
                    RemoteConfigKeys.ONBOARDING_TITLE to SessionMessages().onboardingTitle,
                ),
            )
            val activated = dataSource.activate()
            currentCoroutineContext().ensureActive()
            val flags = readFeatureFlags()
            val title = dataSource.getValue(RemoteConfigKeys.ONBOARDING_TITLE)
            val fetchInfo = dataSource.getFetchInfo()
            val hasTitle = title.value.isNotBlank()
            val messageSources = if (hasTitle) {
                mapOf(RemoteConfigKeys.ONBOARDING_TITLE to title.source)
            } else {
                emptyMap()
            }
            val oldState = _state.value
            _state.value = oldState.copy(
                featureFlags = flags.featureFlags,
                sessionMessages = SessionMessages(
                    onboardingTitle = title.value.takeIf { hasTitle } ?: SessionMessages().onboardingTitle,
                ),
                valueSources = oldState.valueSources + flags.sources + messageSources,
                isInitialized = true,
                lastSuccessfulFetchAtMillis = fetchInfo.lastSuccessfulFetchAtMillis,
                lastFetchStatus = fetchInfo.status,
                lastActivationAtMillis = if (activated) nowMillis() else oldState.lastActivationAtMillis,
                lastError = null,
            )
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(lastError = exception.message ?: "원격 설정 초기화에 실패했습니다.")
            Result.failure(exception)
        }
    }

    override suspend fun fetch(): Result<Unit> = mutex.withLock {
        initializeIfNeeded().exceptionOrNull()?.let { return@withLock Result.failure(it) }

        _state.value = _state.value.copy(isFetching = true, lastFetchAttemptAtMillis = nowMillis())
        try {
            dataSource.fetch()
            currentCoroutineContext().ensureActive()
            val fetchInfo = dataSource.getFetchInfo()
            _state.value = _state.value.copy(
                lastSuccessfulFetchAtMillis = fetchInfo.lastSuccessfulFetchAtMillis,
                lastFetchStatus = ConfigFetchStatus.SUCCESS,
                lastError = null,
            )
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            val fetchInfo = runCatching { dataSource.getFetchInfo() }.getOrNull()
            _state.value = _state.value.copy(
                lastFetchStatus = if (fetchInfo?.status == ConfigFetchStatus.THROTTLED) {
                    ConfigFetchStatus.THROTTLED
                } else {
                    ConfigFetchStatus.FAILURE
                },
                lastError = exception.message ?: "원격 설정을 가져오지 못했습니다.",
            )
            Result.failure(exception)
        } finally {
            _state.value = _state.value.copy(isFetching = false)
        }
    }

    override suspend fun activate(): Result<Boolean> = mutex.withLock {
        initializeIfNeeded().exceptionOrNull()?.let { return@withLock Result.failure(it) }

        try {
            val activated = dataSource.activate()
            currentCoroutineContext().ensureActive()
            val flags = readFeatureFlags()
            _state.value = _state.value.copy(
                featureFlags = flags.featureFlags,
                valueSources = _state.value.valueSources + flags.sources,
                lastActivationAtMillis = if (activated) nowMillis() else _state.value.lastActivationAtMillis,
                lastError = null,
            )
            Result.success(activated)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(lastError = exception.message ?: "원격 설정 활성화에 실패했습니다.")
            Result.failure(exception)
        }
    }

    private fun readFeatureFlags(): FeatureFlagValues {
        val like = dataSource.getValue(RemoteConfigKeys.COMMENT_LIKE_ENABLED)
        val enabled = like.value.trim().lowercase().toBooleanStrictOrNull()
        return FeatureFlagValues(
            featureFlags = FeatureFlags(commentLikeEnabled = enabled ?: _state.value.featureFlags.commentLikeEnabled),
            sources = if (enabled != null) mapOf(RemoteConfigKeys.COMMENT_LIKE_ENABLED to like.source) else emptyMap(),
        )
    }

    private data class FeatureFlagValues(
        val featureFlags: FeatureFlags,
        val sources: Map<String, ConfigValueSource>,
    )
}
