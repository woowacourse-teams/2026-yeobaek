package com.yeobaek.core.config

import com.yeobaek.data.repositoryImpl.remote.AppConfigRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AppConfigRefreshTest {
    @Test
    fun foregroundRefreshesAgainButDoesNotRegisterAnotherListener() = runTest {
        val source = RefreshConfigDataSource()
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)

        startup.onForeground()
        runCurrent()
        assertTrue(startup.isReady.value)
        assertEquals(1, source.fetchCount)
        assertEquals(1, source.listenerCount)

        source.server = remoteValues("false", "다음 실행 문구")
        startup.onForeground()
        runCurrent()

        assertEquals(2, source.fetchCount)
        assertEquals(1, source.listenerCount)
        assertFalse(repository.state.value.featureFlags.commentLikeEnabled)
        assertEquals("이전 문구", repository.state.value.sessionMessages.onboardingTitle)
    }

    @Test
    fun overlappingForegroundEventsDoNotDuplicateAnInFlightFetch() = runTest {
        val source = RefreshConfigDataSource().apply { fetchGate = CompletableDeferred() }
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)
        startup.onForeground()
        runCurrent()

        repeat(3) { startup.onForeground() }
        runCurrent()
        assertEquals(1, source.fetchCount)

        source.fetchGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, source.fetchCount)
        assertEquals(1, source.listenerCount)
    }

    @Test
    fun realtimeUpdateActivatesFlagsWithoutFetchingAgainOrChangingSessionMessages() = runTest {
        val source = RefreshConfigDataSource()
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)
        startup.onForeground()
        runCurrent()

        source.publish("false", "새 문구", fetchedAt = 2_000)
        runCurrent()

        assertEquals(1, source.fetchCount)
        assertFalse(repository.state.value.featureFlags.commentLikeEnabled)
        assertEquals("이전 문구", repository.state.value.sessionMessages.onboardingTitle)
        assertEquals(2_000L, repository.state.value.lastSuccessfulFetchAtMillis)
        assertEquals(ConfigFetchStatus.SUCCESS, repository.state.value.lastFetchStatus)

        val nextSession = AppConfigRepositoryImpl(source)
        nextSession.initialize()
        assertEquals("새 문구", nextSession.state.value.sessionMessages.onboardingTitle)
    }

    @Test
    fun listenerErrorKeepsValuesAndLaterUpdateRecovers() = runTest {
        val source = RefreshConfigDataSource()
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)
        startup.onForeground()
        runCurrent()

        source.events.send(ConfigUpdateEvent.Failed("connection unavailable"))
        runCurrent()
        assertTrue(repository.state.value.featureFlags.commentLikeEnabled)
        assertEquals("connection unavailable", repository.state.value.lastError)
        assertEquals(1, source.listenerCount)

        source.publish("false", "새 문구")
        runCurrent()
        assertFalse(repository.state.value.featureFlags.commentLikeEnabled)
        assertNull(repository.state.value.lastError)
    }

    @Test
    fun foregroundRetriesListenerAfterRegistrationFailure() = runTest {
        val source = RefreshConfigDataSource().apply {
            registrationFailure = IllegalStateException("registration failed")
        }
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)
        startup.onForeground()
        runCurrent()
        assertEquals(0, source.listenerCount)

        source.registrationFailure = null
        startup.onForeground()
        runCurrent()
        assertEquals(1, source.listenerCount)
        assertEquals(2, source.registrationCount)

        source.publish("false", "새 문구")
        runCurrent()
        assertFalse(repository.state.value.featureFlags.commentLikeEnabled)
    }

    @Test
    fun fetchFailureDoesNotStopRealtimeListeningOrLoseCachedValues() = runTest {
        val source = RefreshConfigDataSource().apply {
            fetchFailure = IllegalStateException("offline")
        }
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)
        startup.onForeground()
        runCurrent()

        assertEquals(ConfigFetchStatus.FAILURE, repository.state.value.lastFetchStatus)
        assertTrue(repository.state.value.featureFlags.commentLikeEnabled)
        assertEquals("이전 문구", repository.state.value.sessionMessages.onboardingTitle)
        assertEquals(1, source.listenerCount)

        source.publish("false", "새 문구")
        runCurrent()
        assertFalse(repository.state.value.featureFlags.commentLikeEnabled)
        assertEquals(ConfigFetchStatus.SUCCESS, repository.state.value.lastFetchStatus)
    }

    @Test
    fun activationFailureKeepsFlagsAndFollowingUpdateRecovers() = runTest {
        val source = RefreshConfigDataSource()
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)
        startup.onForeground()
        runCurrent()

        source.activationFailure = IllegalStateException("activation failed")
        source.publish("false", "새 문구")
        runCurrent()
        assertTrue(repository.state.value.featureFlags.commentLikeEnabled)
        assertEquals("activation failed", repository.state.value.lastError)

        source.activationFailure = null
        source.publish("false", "새 문구")
        runCurrent()
        assertFalse(repository.state.value.featureFlags.commentLikeEnabled)
        assertNull(repository.state.value.lastError)
    }

    @Test
    fun closingAppScopeRemovesListenerAndCancelsFetch() = runTest {
        val source = RefreshConfigDataSource().apply { fetchGate = CompletableDeferred() }
        val repository = AppConfigRepositoryImpl(source)
        val startup = AppConfigStartup(repository, backgroundScope)
        startup.onForeground()
        runCurrent()
        assertEquals(1, source.listenerCount)
        assertTrue(repository.state.value.isFetching)

        backgroundScope.cancel()
        runCurrent()

        assertEquals(0, source.listenerCount)
        assertFalse(repository.state.value.isFetching)
    }
}

private fun remoteValues(enabled: String, title: String) = mapOf(
    RemoteConfigKeys.COMMENT_LIKE_ENABLED to ConfigParameterValue(enabled, ConfigValueSource.REMOTE),
    RemoteConfigKeys.ONBOARDING_TITLE to ConfigParameterValue(title, ConfigValueSource.REMOTE),
)

private class RefreshConfigDataSource : RemoteConfigDataSource {
    val events = Channel<ConfigUpdateEvent>(Channel.UNLIMITED)
    var active = remoteValues("true", "이전 문구")
    var server = active
    var fetchCount = 0
    var listenerCount = 0
    var registrationCount = 0
    var fetchGate: CompletableDeferred<Unit>? = null
    var fetchFailure: Exception? = null
    var registrationFailure: Exception? = null
    var activationFailure: Exception? = null
    private var fetched: Map<String, ConfigParameterValue>? = null
    private var fetchInfo = ConfigFetchInfo(null, ConfigFetchStatus.NOT_FETCHED)

    override suspend fun initialize(defaults: Map<String, Any>) = Unit

    override suspend fun fetch() {
        fetchCount++
        fetchGate?.await()
        fetchFailure?.let { throw it }
        fetched = server
        fetchInfo = ConfigFetchInfo(1_000, ConfigFetchStatus.SUCCESS)
    }

    override suspend fun activate(): Boolean {
        activationFailure?.let { throw it }
        val values = fetched ?: return false
        val changed = values != active
        active = values
        fetched = null
        return changed
    }

    override fun getValue(key: String): ConfigParameterValue = active.getValue(key)

    override fun getFetchInfo(): ConfigFetchInfo = fetchInfo

    override fun updates(): Flow<ConfigUpdateEvent> = flow {
        registrationCount++
        registrationFailure?.let { throw it }
        listenerCount++
        try {
            for (event in events) emit(event)
        } finally {
            listenerCount--
        }
    }

    suspend fun publish(enabled: String, title: String, fetchedAt: Long = 2_000) {
        fetched = remoteValues(enabled, title)
        fetchInfo = ConfigFetchInfo(fetchedAt, ConfigFetchStatus.SUCCESS)
        events.send(ConfigUpdateEvent.Updated)
    }
}
